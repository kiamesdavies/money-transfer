package io.kiamesdavies.revolut.services.impl;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActorWithAtLeastOnceDelivery;
import akka.persistence.AtLeastOnceDelivery;
import io.kiamesdavies.revolut.exceptions.AccountNotFoundException;
import io.kiamesdavies.revolut.exceptions.InsufficientFundsException;
import io.kiamesdavies.revolut.models.*;

import java.util.Map;

public class TransferHandler extends AbstractPersistentActorWithAtLeastOnceDelivery {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final String transactionId;
    private final ActorRef bank;
    private final AbstractActor.Receive withdraw;
    private final AbstractActor.Receive deposit;
    private final AbstractActor.Receive rollback;
    private ActorRef initiator;
    private Evt.TransactionEvent state;

    private ActorRef accountFrom;
    private ActorRef accountTo;

    public TransferHandler(String transactionId, ActorRef bank) {
        this.transactionId = transactionId;
        this.bank = bank;

        rollback = receiveBuilder()
                .match(CmdAck.class, f -> f.event instanceof Evt.DepositEvent, j ->{
                    log.info("Rolling back transaction {}",state);
                    //save the roll back as another transaction
                    persist(state.with(TransactionStatus.ROLLBACK),
                            (a) -> {
                                confirmDelivery(Long.valueOf(j.deliveryId));
                                self().tell(PoisonPill.getInstance(), self());
                            }
                    );
                })
                .match(AtLeastOnceDelivery.UnconfirmedWarning.class, j -> {
                    log.error("Rollback to account {} failed to respond after {} trials for {}", accountTo, warnAfterNumberOfUnconfirmedAttempts(), state);
                    persist(state.with(TransactionStatus.ROLLBACK_FAILED), (a) -> {
                        j.getUnconfirmedDeliveries().forEach(g -> confirmDelivery(g.deliveryId()));
                        self().tell(PoisonPill.getInstance(), self());
                    });
                })
                .matchAny(f -> log.error("Unattended Message {}", f))
                .build();
        deposit = receiveBuilder()
                .match(AtLeastOnceDelivery.UnconfirmedWarning.class, j -> {
                    log.error("Deposit account {} failed to respond after {} trials for {}", accountTo, warnAfterNumberOfUnconfirmedAttempts(), state);
                    //rollback
                    persist(state.with(TransactionStatus.DEPOSIT_FAILED), (a) -> {
                        j.getUnconfirmedDeliveries().forEach(g -> confirmDelivery(g.deliveryId()));
                        getContext().become(rollback);
                        deliver(accountFrom.path(), deliveryId -> new Cmd.DepositCmd(deliveryId.toString(), transactionId + "-rollback", state.getAccountFromId(), state.getAmount()));
                    });
                })
                .match(CmdAck.class, f -> f.event instanceof Evt.FailedEvent, j -> {
                    log.error("Failed to deposit {} due to {}", state, j.event);
                    //rollback
                    persist(state.with(TransactionStatus.DEPOSIT_FAILED), (a) -> {
                        confirmDelivery(Long.valueOf(j.deliveryId));
                        getContext().become(rollback);
                        deliver(accountFrom.path(), deliveryId -> new Cmd.DepositCmd(deliveryId.toString(), transactionId + "-rollback", state.getAccountFromId(), state.getAmount()));
                    });


                })
                .match(CmdAck.class, f -> f.event instanceof Evt.DepositEvent, j ->
                        persist(state.with(TransactionStatus.COMPLETED),
                                (a) -> {
                                    confirmDelivery(Long.valueOf(j.deliveryId));
                                    self().tell(PoisonPill.getInstance(), self());
                                }
                        ))
                .matchAny(f -> log.error("Unattended Message {}", f))
                .build();


        withdraw = receiveBuilder()
                .match(AtLeastOnceDelivery.UnconfirmedWarning.class, j -> {
                    log.error("Withdraw account {} failed to respond after {} trials for {}", accountFrom, warnAfterNumberOfUnconfirmedAttempts(), state);
                    persist(state.with(TransactionStatus.WITHDRAW_FAILED), (a) -> {
                        j.getUnconfirmedDeliveries().forEach(g -> confirmDelivery(g.deliveryId()));
                        if (initiator != null) {
                            initiator.tell(new TransactionResult.Failure(new IllegalArgumentException("Bank account not responding")), self());
                        }
                        self().tell(PoisonPill.getInstance(), self());
                    });

                })
                .match(CmdAck.class, f -> f.event instanceof Evt.FailedEvent, j -> {
                    log.error("Failed to withdraw {} due to {}", state, j.event);
                    persist(state.with(TransactionStatus.WITHDRAW_FAILED), (a) -> {
                        confirmDelivery(Long.valueOf(j.deliveryId));
                        if (initiator != null) {
                            Evt.FailedEvent failedEvent = (Evt.FailedEvent) j.event;
                            initiator.tell(
                                    new TransactionResult.Failure(failedEvent.getType().equals(Evt.FailedEvent.Type.INSUFFICIENT_FUNDS) ? new InsufficientFundsException(failedEvent.getAdditionalDescription()) : new IllegalArgumentException(failedEvent.getAdditionalDescription())), self());
                        }
                        self().tell(PoisonPill.getInstance(), self());
                    });


                })
                .match(CmdAck.class, f -> f.event instanceof Evt.WithdrawEvent, j ->
                        persist(state.with(TransactionStatus.WITHDRAWN), (Evt.TransactionEvent g) -> {
                            confirmDelivery(Long.valueOf(j.deliveryId));
                            getContext().become(deposit);
                            deliver(accountTo.path(), deliveryId -> new Cmd.DepositCmd(deliveryId.toString(), state.getTransactionId(), state.getAccountToId(), state.getAmount()));
                            if (initiator != null) {
                                initiator.tell(new TransactionResult.Success(transactionId), self());
                            }
                        }))
                .matchAny(f -> log.error("Unattended Message {}", f))
                .build();

    }


    public static Props props(String transactionId, ActorRef bank) {
        return Props.create(TransferHandler.class, transactionId, bank);
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(Evt.TransactionEvent.class, this::handleEvent)
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Cmd.TransferCmd.class, h -> {
                    initiator = sender();
                    if (h.accountFromId.equalsIgnoreCase(h.accountToId)) {
                        initiator.tell(new TransactionResult.Failure(new IllegalArgumentException("You can transfer to same account")), self());
                        self().tell(PoisonPill.getInstance(), self());
                    } else {
                        persist(new Evt.TransactionEvent(transactionId, h, TransactionStatus.NEW), (Evt.TransactionEvent a) -> handleEvent(a));
                    }

                })
                .match(QueryAck.class, d -> d.response instanceof Map, f -> {
                    confirmDelivery(Long.valueOf(f.deliveryId));
                    Map<String, Query.BankReference> response = (Map<String, Query.BankReference>) f.response;
                    accountFrom = response.get(state.getAccountFromId()).bankAccount;
                    accountTo = response.get(state.getAccountToId()).bankAccount;
                    if (TransactionStatus.NEW.equals(state.getStatus())) {
                        getContext().become(withdraw);
                        deliver(accountFrom.path(), deliveryId -> new Cmd.WithdrawCmd(deliveryId.toString(), state.getTransactionId(), state.getAccountFromId(), state.getAmount()));
                    } else if (TransactionStatus.WITHDRAWN.equals(state.getStatus())) {
                        getContext().become(deposit);
                        deliver(accountTo.path(), deliveryId -> new Cmd.DepositCmd(deliveryId.toString(), state.getTransactionId(), state.getAccountToId(), state.getAmount()));
                    } else if (TransactionStatus.DEPOSIT_FAILED.equals(state.getStatus()) || TransactionStatus.ROLLBACK_FAILED.equals(state.getStatus())) {
                        getContext().become(rollback);
                        deliver(accountFrom.path(), deliveryId -> new Cmd.DepositCmd(deliveryId.toString(), state.getTransactionId() + "-rollback", state.getAccountFromId(), state.getAmount()));
                    } else {
                        log.warning("A completed transaction {} try to restart, am going to kill myself now", state);
                        self().tell(PoisonPill.getInstance(), self());
                    }
                })
                .match(QueryAck.class, d -> d.response instanceof Query.QueryAckNotFound, j -> {
                    if (TransactionStatus.NEW.equals(state.getStatus())) {
                        persist(state.with(TransactionStatus.FAILED), (a) -> {
                            confirmDelivery(Long.valueOf(j.deliveryId));
                            if (initiator != null) {
                                Query.QueryAckNotFound queryAckNotFound = (Query.QueryAckNotFound) j.response;
                                initiator.tell(new TransactionResult.Failure(new AccountNotFoundException(String.format("%s not found", queryAckNotFound.bankAccountId))), self());
                            }
                            self().tell(PoisonPill.getInstance(), self());
                        });
                    } else {
                        confirmDelivery(Long.valueOf(j.deliveryId));
                        if (initiator != null) {
                            Query.QueryAckNotFound queryAckNotFound = (Query.QueryAckNotFound) j.response;
                            initiator.tell(new TransactionResult.Failure(new IllegalArgumentException(String.format("%s not found", queryAckNotFound.bankAccountId))), self());
                        }
                        self().tell(PoisonPill.getInstance(), self());
                    }
                })
                .match(AtLeastOnceDelivery.UnconfirmedWarning.class, j -> {
                    log.error("Bank reference {} failed to respond after {} trials for {}", bank, warnAfterNumberOfUnconfirmedAttempts(), state);
                    if (TransactionStatus.NEW.equals(state.getStatus())) {
                        persist(state.with(TransactionStatus.FAILED), (a) -> {
                            j.getUnconfirmedDeliveries().forEach(g -> confirmDelivery(g.deliveryId()));
                            if (initiator != null) {
                                initiator.tell(new TransactionResult.Failure(new IllegalArgumentException("Bank ")), self());
                            }
                            self().tell(PoisonPill.getInstance(), self());
                        });
                    } else {
                        j.getUnconfirmedDeliveries().forEach(g -> confirmDelivery(g.deliveryId()));
                        if (initiator != null) {
                            initiator.tell(new TransactionResult.Failure(new IllegalStateException("Bank not responding")), self());
                        }
                        self().tell(PoisonPill.getInstance(), self());
                    }


                })
                .matchAny(f -> log.error("Unattended Message {}", f))
                .build();
    }

    private void handleEvent(Evt.TransactionEvent transactionEvent) {
        state = transactionEvent;
        if (recoveryFinished()) {
            //get reference to the bank accounts
            deliver(bank.path(), (deliveryId) -> new Query.Multiple(deliveryId.toString(), transactionEvent.getAccountFromId(), transactionEvent.getAccountToId()));
        }
    }

    @Override
    public String persistenceId() {
        return transactionId;
    }


}
