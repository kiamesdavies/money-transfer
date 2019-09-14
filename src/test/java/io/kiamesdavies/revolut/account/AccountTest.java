package io.kiamesdavies.revolut.account;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * Unit test for a single account
 */
public class AccountTest

{
    ActorSystem system;
    ActorRef bank;
    ActorRef sampleBankAccount;
    private static final String bankACCOUNTId = "12345";

    final TestKit testProbe = new TestKit(system);

    @Test
    public void shouldWithdrawMoneyIfBalanceIsGreaterThanAmount()
    {
        double amount = 10;


        sampleBankAccount.tell(new GetBalance(), ActorRef.noSender());
        AccountBalance accountBalance = testProbe.expectMsgClass(AccountBalance.class);
        sampleBankAccount.tell(new Withdraw(amount), ActorRef.noSender());
        MoneyWithdrawn moneyWithdrawn = testProbe.expectMsgClass(MoneyWithdrawn.class);
        assertThat(moneyWithdrawn.balance,  equalTo(accountBalance.balance-10));
    }


    @Test
    public void shouldFailToWithdrawMoneyIfBalanceIsLessThanAmount(){
        sampleBankAccount.tell(new GetBalance(), ActorRef.noSender());
        AccountBalance accountBalance = testProbe.expectMsgClass(AccountBalance.class);
        sampleBankAccount.tell(new Withdraw(accountBalance.balance + 10), ActorRef.noSender());
        testProbe.expectMsgClass(FailedToWithdraw.class);
    }

    @Test
    public void shouldCreditBalanceIfAmountIsGreaterThanZero(){
        double amount = 10;
        sampleBankAccount.tell(new GetBalance(), ActorRef.noSender());
        AccountBalance accountBalance = testProbe.expectMsgClass(AccountBalance.class);
        sampleBankAccount.tell(new Deposit(amount), ActorRef.noSender());
        AccountBalance newAccountBalance = testProbe.expectMsgClass(AccountBalance.class);
        assertThat(newAccountBalance.balance,  equalTo(accountBalance.balance+10));
    }

    @Test
    public void shouldFailToCreditBalanceIfAmountIsLessThanOne(){
        sampleBankAccount.tell(new Deposit(0), ActorRef.noSender());
        testProbe.expectMsgClass(FailedToDeposit.class);
    }

    @Test
    public void shouldReturnAccountGivenACorrectAccountId(){
        final TestKit testProbe = new TestKit(system);

        bank.tell(new GetBankAccount(bankACCOUNTId), ActorRef.noSender());

        BankAccountResponse bankAccountResponse = testProbe.expectMsgClass(BankAccountResponse.class);
        assertNotNull(bankAccountResponse.balance);
        assertThat(bankAccountResponse.account, equalTo(bankACCOUNTId));
    }

    @Test
    public void shouldFailToReturnAccountGivenWrongAccountId(){
        final TestKit testProbe = new TestKit(system);
        bank.tell(new GetBankAccount("wrongBankId"), ActorRef.noSender());
        testProbe.expectMsgClass(BankNotFound.class);

    }
}
