package io.kiamesdavies.revolut.services.impl;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.BackoffOpts;
import akka.pattern.BackoffSupervisor;
import akka.persistence.AbstractPersistentActorWithAtLeastOnceDelivery;
import akka.persistence.AtLeastOnceDelivery;
import io.kiamesdavies.revolut.exceptions.AccountNotFoundException;
import io.kiamesdavies.revolut.exceptions.InsufficientFundsException;
import io.kiamesdavies.revolut.models.*;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class TransferHandler extends AbstractPersistentActorWithAtLeastOnceDelivery {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final String transactionId;

    private final ActorRef bank;

    /**
     * Define behaviour of the handler while deducting money from the sender's account
     */
    private final AbstractActor.Receive debtor;

    /**
     * Define behaviour of the handler while crediting the receiver's account
     */
    private final AbstractActor.Receive creditor;

    /**
     * Define behaviour of the handler while performing a roll back
     */
    private final AbstractActor.Receive rollback;

    /**
     * Reference to api call that initiated the transfer
     */
    private ActorRef initiator;
    private Evt.TransactionEvent state;

    /**
     * Reference to the sender's account
     */
    private ActorPath accountFrom;

    /**
     * Reference to the receiver's account
     */
    private ActorPath accountTo;

    public TransferHandler(String transactionId, ActorRef bank) {
        this.transactionId = transactionId;
        this.bank = bank;

        rollback = receiveBuilder()
                .match(CmdAck.class, f -> f.event instanceof Evt.DepositEvent, j -> {
                    log.info("Rolling back transaction {}", state);
                    persist(state.with(TransactionStatus.ROLLBACK), a -> {
                                confirmDelivery(j.deliveryId);
                                self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                            }
                    );
                })
                .match(AtLeastOnceDelivery.UnconfirmedWarning.class, j -> {
                    log.error("Rollback to account {} failed to respond after {} trials for {} will try again in 5 minutes ", accountTo, warnAfterNumberOfUnconfirmedAttempts(), state);
                    getContext().system().scheduler().scheduleOnce(
                            Duration.ofMinutes(5),
                            () -> deliver(accountFrom, deliveryId ->
                                    new Cmd.DepositCmd(deliveryId, transactionId + "-rollback", state.accountFromId, state.amount)), getContext().getDispatcher());
                })
                .matchAny(f -> log.error("Unattended Message {} at state {}", f, state))
                .build();

        creditor = receiveBuilder()
                .match(AtLeastOnceDelivery.UnconfirmedWarning.class, j -> {
                    log.error("Deposit account {} failed to respond after {} trials for {} will attempt rollback", accountTo, warnAfterNumberOfUnconfirmedAttempts(), state);
                    //rollback
                    persist(state.with(TransactionStatus.DEPOSIT_FAILED), a -> {
                        j.getUnconfirmedDeliveries().forEach(g -> confirmDelivery(g.deliveryId()));
                        state = a;
                        getContext().become(rollback);
                        //the transactionId has been marked as worked on by the account so "-rollback" is attached to differentiate it
                        //note that the read side is required to remove the  "-rollback" text before saving it
                        deliver(accountFrom, deliveryId ->
                                new Cmd.DepositCmd(deliveryId, transactionId + "-rollback", state.accountFromId, state.amount));
                    });
                })
                .match(CmdAck.class, f -> f.event instanceof Evt.FailedEvent, j -> {
                    log.error("Failed to creditor {} due to {} will attempt rollback", state, j.event);
                    //rollback
                    persist(state.with(TransactionStatus.DEPOSIT_FAILED), a -> {
                        confirmDelivery(j.deliveryId);
                        state = a;
                        getContext().become(rollback);
                        //the transactionId has been flagged by the sender's account so "-rollback" is attached to differentiate it
                        //note that the read side is required to remove the  "-rollback" text before saving to its own store
                        deliver(accountFrom, deliveryId ->
                                new Cmd.DepositCmd(deliveryId, transactionId + "-rollback", state.accountFromId, state.amount));
                    });


                })
                .match(CmdAck.class, f -> f.event instanceof Evt.DepositEvent, j ->
                        persist(state.with(TransactionStatus.COMPLETED), a -> {
                            confirmDelivery(j.deliveryId);
                            self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                        })
                )
                .matchAny(f -> log.error("Unattended Message {} at state {}", f, state))
                .build();


        debtor = receiveBuilder()
                .match(AtLeastOnceDelivery.UnconfirmedWarning.class, j -> {
                    log.error("Withdraw account {} failed to respond after {} trials for {}", accountFrom, warnAfterNumberOfUnconfirmedAttempts(), state);
                    persist(state.with(TransactionStatus.FAILED), (a) -> {
                        j.getUnconfirmedDeliveries().forEach(g -> confirmDelivery(g.deliveryId()));
                        if (initiator != null) {
                            initiator.tell(new TransactionResult.Failure(new IllegalArgumentException("Bank account not responding")), self());
                        }
                        self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                    });

                })
                .match(CmdAck.class, f -> f.event instanceof Evt.FailedEvent, j -> {
                    log.error("Failed for debtor {} due to {}", state, j.event);
                    persist(state.with(TransactionStatus.FAILED), a -> {
                        confirmDelivery(j.deliveryId);
                        if (initiator != null) {
                            Evt.FailedEvent failedEvent = (Evt.FailedEvent) j.event;
                            initiator.tell(
                                    new TransactionResult.Failure(failedEvent.type.equals(Evt.FailedEvent.Type.INSUFFICIENT_FUNDS) ? new InsufficientFundsException(failedEvent.additionalDescription) : new IllegalArgumentException(failedEvent.additionalDescription)), self());
                        }
                        self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                    });
                })
                .match(CmdAck.class, f -> f.event instanceof Evt.WithdrawEvent, j ->
                        persist(state.with(TransactionStatus.WITHDRAWN), g -> {
                            confirmDelivery(j.deliveryId);
                            state = g;
                            getContext().become(creditor);
                            deliver(accountTo, deliveryId ->
                                    new Cmd.DepositCmd(deliveryId, state.transactionId, state.accountToId, state.amount));
                            if (initiator != null) {
                                initiator.tell(new TransactionResult.Success(transactionId), self());
                            }
                        }))
                .matchAny(f -> log.error("Unattended Message {} at state {}", f, state))
                .build();

    }



    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(Evt.TransactionEvent.class, a -> this.startTransfer(a, true))
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Cmd.TransferCmd.class, h -> {
                    initiator = sender();
                    if (h.accountFromId.equalsIgnoreCase(h.accountToId)) {
                        initiator.tell(new TransactionResult.Failure(new IllegalArgumentException("Can't transfer to same account")), self());
                        self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                    } else {
                        persist(new Evt.TransactionEvent(transactionId, h, TransactionStatus.NEW), a ->
                                startTransfer(a, false));
                    }

                })
                .match(QueryAck.class, d -> d.response instanceof Map, f -> {
                    confirmDelivery(f.deliveryId);
                    Map<String, Query.BankReference> response = (Map<String, Query.BankReference>) f.response;
                    accountFrom = response.get(state.accountFromId).bankAccount.path();
                    accountTo = response.get(state.accountToId).bankAccount.path();
                    if (TransactionStatus.NEW.equals(state.status)) {
                        getContext().become(debtor);
                        deliver(accountFrom, deliveryId -> new Cmd.WithdrawCmd(deliveryId, state.transactionId, state.accountFromId, state.amount));
                    } else if (TransactionStatus.WITHDRAWN.equals(state.status) || TransactionStatus.DEPOSIT_FAILED.equals(state.status)) {
                        getContext().become(creditor);
                        deliver(accountTo, deliveryId -> new Cmd.DepositCmd(deliveryId, state.transactionId, state.accountToId, state.amount));
                    } else {
                        log.warning("A completed transaction {} try to restart, am going to kill myself now", state);
                        self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                    }
                })
                .match(QueryAck.class, d -> d.response instanceof Query.QueryAckNotFound, j -> {
                    if (TransactionStatus.NEW.equals(state.status)) {
                        persist(state.with(TransactionStatus.FAILED), a -> {
                            confirmDelivery(j.deliveryId);
                            if (initiator != null) {
                                Query.QueryAckNotFound queryAckNotFound = (Query.QueryAckNotFound) j.response;
                                initiator.tell(new TransactionResult.Failure(new AccountNotFoundException(String.format("%s not found", queryAckNotFound.bankAccountId))), self());
                            }
                            self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                        });
                    } else {
                        confirmDelivery(j.deliveryId);
                        if (initiator != null) {
                            Query.QueryAckNotFound queryAckNotFound = (Query.QueryAckNotFound) j.response;
                            initiator.tell(new TransactionResult.Failure(new IllegalArgumentException(String.format("%s not found", queryAckNotFound.bankAccountId))), self());
                        }
                        self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                    }
                })
                .match(AtLeastOnceDelivery.UnconfirmedWarning.class, j -> {
                    log.error("Bank reference {} failed to respond after {} trials for {}", bank, warnAfterNumberOfUnconfirmedAttempts(), state);
                    if (TransactionStatus.NEW.equals(state.status)) {
                        persist(state.with(TransactionStatus.FAILED), a -> {
                            j.getUnconfirmedDeliveries().forEach(g -> confirmDelivery(g.deliveryId()));
                            if (initiator != null) {
                                initiator.tell(new TransactionResult.Failure(new IllegalArgumentException("Bank ")), self());
                            }
                            self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                        });
                    } else {
                        j.getUnconfirmedDeliveries().forEach(g -> confirmDelivery(g.deliveryId()));
                        if (initiator != null) {
                            initiator.tell(new TransactionResult.Failure(new IllegalStateException("Bank not responding")), self());
                        }
                        self().tell(PoisonPill.getInstance(), ActorRef.noSender());
                    }


                })
                .matchAny(f -> log.error("Unattended Message {}", f))
                .build();
    }



    /**
     * Start the process of transfer
     * @param transactionEvent state of event
     * @param checkForRecovery true if this was from a recovery process
     */
    private void startTransfer(Evt.TransactionEvent transactionEvent, boolean checkForRecovery) {
        state = transactionEvent;
        //get bank references
        final Runnable run  = () ->
                deliver(bank.path(), (deliveryId) ->
                        new Query.Multiple(deliveryId, transactionEvent.accountFromId, transactionEvent.accountToId));

        if(!checkForRecovery){
            run.run();
        }
        else if (checkForRecovery && recoveryFinished()) {
            //wait for other messages that were sent before starting the process of transfer
            getContext().system().scheduler().scheduleOnce(
                    Duration.ofSeconds(redeliverInterval().toSeconds() * warnAfterNumberOfUnconfirmedAttempts()),
                    run,
                    getContext().getDispatcher());
        }
    }

    public static Props props(String transactionId, ActorRef bank) {
        return BackoffSupervisor.props(
                BackoffOpts.onStop(
                        Props.create(TransferHandler.class, transactionId, bank), transactionId,
                        FiniteDuration.create(1, TimeUnit.SECONDS),
                        FiniteDuration.create(10, TimeUnit.SECONDS),
                        0.2)
        );
    }

    @Override
    public String persistenceId() {
        return String.format("transaction-%s", transactionId);
    }

}
