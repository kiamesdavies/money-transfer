package io.kiamesdavies.revolut.account;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import io.kiamesdavies.revolut.Inflation;
import io.kiamesdavies.revolut.models.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Unit test for a single account
 */
public class BankAccountTest {
    private static final String bankAccountId = "2";
    private static ActorSystem system;
    private static ActorRef bank;
    private static ActorRef sampleBankAccount;
    private static TestKit testProbe;

    private static final Random RANDOM = new Random();

    @BeforeAll
    static void setup() throws InterruptedException {
        system = ActorSystem.create();
        testProbe = new TestKit(system);
        Map<String, ActorRef> bankAccounts = Inflation.initiateDemoBankAccounts(system);
        bank = system.actorOf(Bank.props(bankAccounts), "bank");

        sampleBankAccount = bankAccounts.get(bankAccountId);
    }

    @AfterAll
    static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    void shouldWithdrawMoneyIfBalanceIsGreaterThanAmount() {
        BigDecimal amount = new BigDecimal(10);


        sampleBankAccount.tell(new Query.Single(RANDOM.nextLong(), bankAccountId), testProbe.getRef());
        QueryAck queryAck = testProbe.expectMsgClass(QueryAck.class);
        AccountBalance accountBalance = (AccountBalance) queryAck.response;

        sampleBankAccount.tell(new Cmd.WithdrawCmd(RANDOM.nextLong(), UUID.randomUUID().toString(), bankAccountId, amount), testProbe.getRef());
        CmdAck cmdAck = testProbe.expectMsgClass(CmdAck.class);
        assertThat(cmdAck.event, instanceOf(Evt.WithdrawEvent.class));
        sampleBankAccount.tell(new Query.Single(RANDOM.nextLong(), bankAccountId), testProbe.getRef());
        queryAck = testProbe.expectMsgClass(QueryAck.class);
        AccountBalance newAccountBalance = (AccountBalance) queryAck.response;
        assertThat(newAccountBalance.getBalance(), equalTo(accountBalance.getBalance().subtract(amount)));
    }

    @Test
    void shouldFailToWithdrawMoneyIfBalanceIsLessThanAmount() {

        
        sampleBankAccount.tell(new Query.Single(RANDOM.nextLong(), bankAccountId), testProbe.getRef());
        QueryAck queryAck = testProbe.expectMsgClass(QueryAck.class);
        AccountBalance accountBalance = (AccountBalance) queryAck.response;
        sampleBankAccount.tell(new Cmd.WithdrawCmd(RANDOM.nextLong(), UUID.randomUUID().toString(), bankAccountId, accountBalance.getBalance().add(BigDecimal.ONE)), testProbe.getRef());
        CmdAck cmdAck = testProbe.expectMsgClass(CmdAck.class);
        assertThat(cmdAck.event, instanceOf(Evt.FailedEvent.class));
        assertThat(((Evt.FailedEvent) cmdAck.event).type, equalTo(Evt.FailedEvent.Type.INSUFFICIENT_FUNDS));
    }

    @Test
    void shouldCreditBalanceIfAmountIsGreaterThanZero() {
        BigDecimal amount = new BigDecimal(10);
        sampleBankAccount.tell(new Query.Single(RANDOM.nextLong(), bankAccountId), testProbe.getRef());
        QueryAck queryAck = testProbe.expectMsgClass(QueryAck.class);
        AccountBalance accountBalance = (AccountBalance) queryAck.response;
        sampleBankAccount.tell(new Cmd.DepositCmd(RANDOM.nextLong(), UUID.randomUUID().toString(), bankAccountId, amount), testProbe.getRef());
        CmdAck cmdAck = testProbe.expectMsgClass(CmdAck.class);
        assertThat(cmdAck.event, instanceOf(Evt.DepositEvent.class));
        sampleBankAccount.tell(new Query.Single(RANDOM.nextLong(), bankAccountId), testProbe.getRef());
        queryAck = testProbe.expectMsgClass(QueryAck.class);
        AccountBalance newAccountBalance = (AccountBalance) queryAck.response;
        assertThat(newAccountBalance.getBalance(), equalTo(accountBalance.getBalance().add(amount)));
    }

    @Test
    void shouldFailToCreditBalanceIfAmountIsLessThanOne() {
        sampleBankAccount.tell(new Cmd.DepositCmd(RANDOM.nextLong(), UUID.randomUUID().toString(), bankAccountId, BigDecimal.ZERO), testProbe.getRef());
        CmdAck cmdAck = testProbe.expectMsgClass(CmdAck.class);
        assertThat(cmdAck.event, instanceOf(Evt.FailedEvent.class));
        assertThat(((Evt.FailedEvent) cmdAck.event).type, equalTo(Evt.FailedEvent.Type.INVALID_AMOUNT));
    }

    @Test
    void shouldReturnAccountGivenACorrectAccountId() {

        bank.tell(new Query.Single(RANDOM.nextLong(), bankAccountId), testProbe.getRef());
        QueryAck queryAck = testProbe.expectMsgClass(QueryAck.class);
        Query.BankReference bankReference = (Query.BankReference) queryAck.response;
        assertNotNull(bankReference.bankAccount);
        assertThat(bankReference.bankAccountId, equalTo(bankAccountId));
    }

    @Test
    void shouldFailToReturnAccountGivenWrongAccountId() {

        bank.tell(new Query.Single(RANDOM.nextLong(), "wrongBankId"), testProbe.getRef());
        QueryAck queryAck = testProbe.expectMsgClass(QueryAck.class);
        assertThat(queryAck.response, instanceOf(Query.QueryAckNotFound.class));

    }

    @Test
    void shouldReturnMultipleAccountGivenMultipleCorrectAccountId() {

        bank.tell(new Query.Multiple(RANDOM.nextLong(), "3", "4", "5"), testProbe.getRef());
        QueryAck queryAck = testProbe.expectMsgClass(QueryAck.class);
        Map<String, Query.BankReference> bankReferences = (Map<String, Query.BankReference>) queryAck.response;
        assertThat(bankReferences.keySet(), hasItems("3", "4", "5"));
    }

    @Test
    void shouldFailToReturnAccountGivenWrongAccountIdWithinMultipleIds() {

        bank.tell(new Query.Multiple(RANDOM.nextLong(), "3", "wrongBankId", "5"), testProbe.getRef());
        QueryAck queryAck = testProbe.expectMsgClass(QueryAck.class);
        assertThat(queryAck.response, instanceOf(Query.QueryAckNotFound.class));
        Query.QueryAckNotFound notFound = (Query.QueryAckNotFound) queryAck.response;
        assertThat(notFound.bankAccountId, equalTo("wrongBankId"));

    }
}
