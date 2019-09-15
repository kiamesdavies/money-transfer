package io.kiamesdavies.revolut.account;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.BackoffOpts;
import akka.pattern.BackoffSupervisor;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import io.kiamesdavies.revolut.models.*;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static io.kiamesdavies.revolut.models.Evt.FailedEvent.Type.INSUFFICIENT_FUNDS;
import static io.kiamesdavies.revolut.models.Evt.FailedEvent.Type.INVALID_AMOUNT;

/**
 * Represents a single user bank account
 */
public class BankAccount extends AbstractPersistentActor {

    private  final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final String bankAccountId;
    private AccountBalance state;

    private final int snapShotInterval = 100;

    public BankAccount(String bankAccountId) {
        this.bankAccountId = bankAccountId;
        state = new AccountBalance(bankAccountId, BigDecimal.valueOf(10000));
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(Evt.class, this::update)
                .match(SnapshotOffer.class, ss -> state = (AccountBalance) ss.snapshot())
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Query.class, s -> {
                    log.info("Received Query {}", s.bankAccountId);
                    sender().tell(state.copy(), self());
                })
                .match(Cmd.class, s -> s.amount.compareTo(BigDecimal.ZERO) < 1,
                        f -> sender().tell(CmdAck.from(f, new Evt.FailedEvent(bankAccountId, INVALID_AMOUNT,"Amount is too small")), self()))
                .match(Cmd.WithdrawCmd.class, s -> s.amount.compareTo(state.getBalance()) == 1,
                        f -> sender().tell(CmdAck.from(f, new Evt.FailedEvent(bankAccountId, INSUFFICIENT_FUNDS)), self()))
                .match(
                        Cmd.class,
                        c -> {

                            final Evt evt = c instanceof Cmd.DepositCmd ? new Evt.DepositEvent((Cmd.DepositCmd) c): new Evt.WithdrawEvent((Cmd.WithdrawCmd) c);
                            persist(evt,
                                    (Evt e) -> {
                                        this.update(e);
                                        sender().tell(CmdAck.from(c, e), self());
                                        if (lastSequenceNr() % snapShotInterval == 0 && lastSequenceNr() != 0) {
                                            saveSnapshot(state.copy());
                                        }
                                    });
                        })
                .build();
    }

    @Override
    public String persistenceId() {
        return bankAccountId;
    }


    private void update(Evt evt) {
        if (evt instanceof Evt.DepositEvent) {
            AccountBalance.deposit(state, evt.amount);
        } else if (evt instanceof Evt.WithdrawEvent) {
            AccountBalance.withdraw(state, evt.amount);
        }
    }

    @Override
    public void preStart() {
        log.info("Starting bank account {}", bankAccountId);
    }




    /**
     * Creates a BackoffSupervisor  incase the database is not available, and we need to give it some time to start-up again
     * @param bankAccountId
     * @return
     */
    public static Props props(String bankAccountId){
        return  BackoffSupervisor.props(
                BackoffOpts.onStop(
                        Props.create(BankAccount.class, bankAccountId),String.format("bank-account-%s",bankAccountId),
                        FiniteDuration.create(1, TimeUnit.SECONDS),
                        FiniteDuration.create(30, TimeUnit.SECONDS),
                        0.2)
        );
    }
}
