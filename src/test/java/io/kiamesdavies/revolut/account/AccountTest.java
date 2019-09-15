package io.kiamesdavies.revolut.account;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import io.kiamesdavies.revolut.models.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Unit test for a single account
 */
public class AccountTest

{
    static ActorSystem  system;
    static ActorRef bank;
    static ActorRef sampleBankAccount;
    private static final String bankAccountId = "2";

    static TestKit testProbe;

    @Test
    public void shouldWithdrawMoneyIfBalanceIsGreaterThanAmount()
    {
        BigDecimal amount = new BigDecimal(10);


        sampleBankAccount.tell(new Query(bankAccountId), testProbe.getRef());
        AccountBalance accountBalance = testProbe.expectMsgClass(AccountBalance.class);
        sampleBankAccount.tell(new Cmd.WithdrawCmd(java.util.UUID.randomUUID().toString(), bankAccountId, amount), testProbe.getRef());
        CmdAck cmdAck = testProbe.expectMsgClass(CmdAck.class);
        assertThat(cmdAck.event, instanceOf(Evt.WithdrawEvent.class));
        sampleBankAccount.tell(new Query(bankAccountId), testProbe.getRef());
        AccountBalance newAccountBalance = testProbe.expectMsgClass(AccountBalance.class);
        assertThat(newAccountBalance.getBalance(),  equalTo(accountBalance.getBalance().subtract(amount)));
    }


    @Test
    public void shouldFailToWithdrawMoneyIfBalanceIsLessThanAmount(){


        sampleBankAccount.tell(new Query(bankAccountId), testProbe.getRef());
        AccountBalance accountBalance = testProbe.expectMsgClass(AccountBalance.class);
        sampleBankAccount.tell(new Cmd.WithdrawCmd(java.util.UUID.randomUUID().toString(), bankAccountId, accountBalance.getBalance().add(BigDecimal.ONE)), testProbe.getRef());
        CmdAck cmdAck = testProbe.expectMsgClass(CmdAck.class);
        assertThat(cmdAck.event, instanceOf(Evt.FailedEvent.class));
        assertThat(((Evt.FailedEvent)cmdAck.event).type, equalTo(Evt.FailedEvent.Type.INSUFFICIENT_FUNDS));
    }

    @Test
    public void shouldCreditBalanceIfAmountIsGreaterThanZero(){
        BigDecimal amount = new BigDecimal(10);
        sampleBankAccount.tell(new Query(bankAccountId), testProbe.getRef());
        AccountBalance accountBalance = testProbe.expectMsgClass(AccountBalance.class);
        sampleBankAccount.tell(new Cmd.DepositCmd(java.util.UUID.randomUUID().toString(), bankAccountId,amount), testProbe.getRef());
        CmdAck cmdAck = testProbe.expectMsgClass(CmdAck.class);
        assertThat(cmdAck.event, instanceOf(Evt.DepositEvent.class));
        sampleBankAccount.tell(new Query(bankAccountId), testProbe.getRef());
        AccountBalance newAccountBalance = testProbe.expectMsgClass(AccountBalance.class);
        assertThat(newAccountBalance.getBalance(),  equalTo(accountBalance.getBalance().add(amount)));
    }

    @Test
    public void shouldFailToCreditBalanceIfAmountIsLessThanOne(){
        sampleBankAccount.tell(new Cmd.DepositCmd(java.util.UUID.randomUUID().toString(), bankAccountId,BigDecimal.ZERO), testProbe.getRef());
        CmdAck cmdAck = testProbe.expectMsgClass(CmdAck.class);
        assertThat(cmdAck.event, instanceOf(Evt.FailedEvent.class));
        assertThat(((Evt.FailedEvent)cmdAck.event).type, equalTo(Evt.FailedEvent.Type.INVALID_AMOUNT));
    }

    @Test
    public void shouldReturnAccountGivenACorrectAccountId(){

        bank.tell(new Query(bankAccountId), testProbe.getRef());

        Query.BankAccount bankAccount = testProbe.expectMsgClass(Query.BankAccount.class);
        assertNotNull(bankAccount.bankAccount);
        assertThat(bankAccount.bankAccountId, equalTo(bankAccountId));
    }

    @Test
    public void shouldFailToReturnAccountGivenWrongAccountId(){

        bank.tell(new Query("wrongBankId"), testProbe.getRef());
        testProbe.expectMsgClass(Query.QueryAckNotFound.class);

    }



    @BeforeAll
    public static void setup() {
        system = ActorSystem.create();
        testProbe = new TestKit(system);
        Map<String, ActorRef> bankAccounts = IntStream.range(1, 6).mapToObj(String::valueOf).collect(Collectors.toMap(f -> f, f ->
                system.actorOf(BankAccount.props(f), String.format("supervisor-%s", f))));
        bank = system.actorOf(Bank.props(bankAccounts),"bank");

        sampleBankAccount = bankAccounts.get(bankAccountId);


    }

    @AfterAll
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }
}
