package io.kiamesdavies.revolut.account;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.BackoffOpts;
import akka.pattern.BackoffSupervisor;
import akka.persistence.AbstractPersistentActorWithTimers;
import akka.persistence.SnapshotOffer;
import io.kiamesdavies.revolut.models.*;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.kiamesdavies.revolut.models.Evt.FailedEvent.Type.INSUFFICIENT_FUNDS;
import static io.kiamesdavies.revolut.models.Evt.FailedEvent.Type.INVALID_AMOUNT;

/**
 * Represents a single user bank account
 */
public class BankAccount extends AbstractPersistentActorWithTimers {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final String bankAccountId;
    private final int snapShotInterval = 100;
    private AccountBalance state;
    private Map<String, LocalDateTime> receivedCmds = new HashMap<>();


    public BankAccount(String bankAccountId) {
        this.bankAccountId = bankAccountId;
        state = new AccountBalance(bankAccountId, BigDecimal.valueOf(10000));
        timers().startPeriodicTimer(new ReceivedCmdCleanUp(), new ReceivedCmdCleanUp(), Duration.ofHours(6));
    }



    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(Evt.BaseAccountEvt.class, this::update)
                .match(SnapshotOffer.class, ss -> state = (AccountBalance) ss.snapshot())
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Query.Single.class, s -> sender().tell(new QueryAck(s.deliveryId, state.copy()), self()))
                .match(Cmd.BaseAccountCmd.class, s -> s.amount.compareTo(BigDecimal.ZERO) < 1,
                        f -> sender().tell(CmdAck.from(f, new Evt.FailedEvent(bankAccountId, INVALID_AMOUNT, "Amount is too small")), self()))
                .match(Cmd.WithdrawCmd.class, s -> s.amount.compareTo(state.getBalance()) > 0,
                        f -> sender().tell(CmdAck.from(f, new Evt.FailedEvent(bankAccountId, INSUFFICIENT_FUNDS)), self()))
                .match(Cmd.BaseAccountCmd.class, this::handleCmd)
                .match(ReceivedCmdCleanUp.class, f -> {
                    //the map was re-created to reset the bucket size instead of just removing the entries
                    receivedCmds = receivedCmds.entrySet().stream()
                            .filter(g -> g.getValue().isAfter(LocalDateTime.now().minusHours(6)))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                })
                .matchAny(f -> log.error("Unattended Message {}", f))
                .build();
    }



    private void update(Evt.BaseAccountEvt evt) {
        receivedCmds.put(evt.getTransactionId(), LocalDateTime.now());
        if (evt instanceof Evt.DepositEvent) {
            AccountBalance.deposit(state, evt.getAmount());
        } else if (evt instanceof Evt.WithdrawEvent) {
            AccountBalance.withdraw(state, evt.getAmount());
        }
    }


    private void handleCmd(Cmd.BaseAccountCmd c) {
        final Evt.BaseAccountEvt evt = c instanceof Cmd.DepositCmd ? new Evt.DepositEvent((Cmd.DepositCmd) c) : new Evt.WithdrawEvent((Cmd.WithdrawCmd) c);
        if (receivedCmds.containsKey(c.transactionId)) {
            sender().tell(CmdAck.from(c, evt), self());
        } else {
            persist(evt,
                    (Evt.BaseAccountEvt e) -> {

                        this.update(e);
                        sender().tell(CmdAck.from(c, e), self());
                        if (lastSequenceNr() % snapShotInterval == 0 && lastSequenceNr() != 0) {
                            saveSnapshot(state.copy());
                        }
                    });
        }
    }

    private static class ReceivedCmdCleanUp {
    }


    @Override
    public String persistenceId() {
        return String.format("bank-account-%s",bankAccountId);
    }

    /**
     * Creates a BackoffSupervisor  incase the database is not available, and we need to give it some time to start-up again
     *
     * @param bankAccountId the account id
     * @return BankAccount  configuration object
     */
    public static Props props(String bankAccountId) {
        return BackoffSupervisor.props(
                BackoffOpts.onStop(
                        Props.create(BankAccount.class,bankAccountId), bankAccountId,
                        FiniteDuration.create(1, TimeUnit.SECONDS),
                        FiniteDuration.create(5, TimeUnit.SECONDS),
                        0.2)
        );
    }


    @Override
    public void preStart() {
        log.info("Starting bank account {}", bankAccountId);
    }
}
