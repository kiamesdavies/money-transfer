package io.kiamesdavies.revolut.services;

import akka.testkit.javadsl.TestKit;
import io.kiamesdavies.revolut.Bootstrap;
import io.kiamesdavies.revolut.exceptions.AccountNotFoundException;
import io.kiamesdavies.revolut.exceptions.InsufficientFundsException;
import io.kiamesdavies.revolut.models.AccountBalance;
import io.kiamesdavies.revolut.models.MoneyTransfer;
import io.kiamesdavies.revolut.models.TransactionResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CompletionStage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class AccountTest {

    private static Account account;
    private static Bootstrap instance;

    @BeforeAll
    static void setup() {
        instance = Bootstrap.getInstance();
        account = instance.account;
    }

    @AfterAll
    static void teardown() {
        instance.terminate();
        TestKit.shutdownActorSystem(instance.actorSystem);
    }

    @Test
    void shouldTransferIfAccountsAreAvailableAndAmountIsSufficient() {
        BigDecimal amount = BigDecimal.valueOf(100);
        MoneyTransfer transfer = new MoneyTransfer(amount);
        AccountBalance accountBalance1 = account.getBalance("1").toCompletableFuture().join();
        AccountBalance accountBalance2 = account.getBalance("2").toCompletableFuture().join();
        CompletionStage<TransactionResult> response = account.transferMoney("1", "2", transfer);
        TransactionResult result = response.toCompletableFuture().join();
        assertThat(result, instanceOf(TransactionResult.Success.class));
        AccountBalance newAccountBalance1 = account.getBalance("1").toCompletableFuture().join();
        AccountBalance newAccountBalance2 = account.getBalance("2").toCompletableFuture().join();
        assertThat(newAccountBalance1.getBalance(), equalTo(accountBalance1.getBalance().subtract(amount)));
        assertThat(newAccountBalance2.getBalance(), equalTo(accountBalance2.getBalance().add(amount)));

    }

    @Test
    void shouldFailIfSenderAccountIsUnavailable() {
        BigDecimal amount = BigDecimal.valueOf(100);
        MoneyTransfer transfer = new MoneyTransfer(amount);
        AccountBalance accountBalance2 = account.getBalance("2").toCompletableFuture().join();
        CompletionStage<TransactionResult> response = account.transferMoney("wrongId", "2", transfer);
        TransactionResult result = response.toCompletableFuture().join();
        assertThat(result, instanceOf(TransactionResult.Failure.class));
        assertThat(((TransactionResult.Failure) result).exception, instanceOf(AccountNotFoundException.class));
        AccountBalance newAccountBalance2 = account.getBalance("2").toCompletableFuture().join();
        assertThat(newAccountBalance2.getBalance(), equalTo(accountBalance2.getBalance()));
    }

    @Test
    void shouldFailIfRecipientAccountIsUnavailable() {
        BigDecimal amount = BigDecimal.valueOf(100);
        MoneyTransfer transfer = new MoneyTransfer(amount);
        AccountBalance accountBalance1 = account.getBalance("1").toCompletableFuture().join();
        CompletionStage<TransactionResult> response = account.transferMoney("1", "wrongId", transfer);
        TransactionResult result = response.toCompletableFuture().join();
        assertThat(result, instanceOf(TransactionResult.Failure.class));
        assertThat(((TransactionResult.Failure) result).exception, instanceOf(AccountNotFoundException.class));
        AccountBalance newAccountBalance1 = account.getBalance("1").toCompletableFuture().join();
        assertThat(newAccountBalance1.getBalance(), equalTo(accountBalance1.getBalance()));
    }

    @Test
    void shouldFailIfSenderAccountIsNotSufficient() {


        AccountBalance accountBalance1 = account.getBalance("1").toCompletableFuture().join();
        AccountBalance accountBalance2 = account.getBalance("2").toCompletableFuture().join();

        BigDecimal amount = accountBalance1.getBalance().add(BigDecimal.TEN);
        MoneyTransfer transfer = new MoneyTransfer(amount);

        CompletionStage<TransactionResult> response = account.transferMoney("1", "2", transfer);
        TransactionResult result = response.toCompletableFuture().join();
        assertThat(result, instanceOf(TransactionResult.Failure.class));
        assertThat(((TransactionResult.Failure) result).exception, instanceOf(InsufficientFundsException.class));
        AccountBalance newAccountBalance1 = account.getBalance("1").toCompletableFuture().join();
        AccountBalance newAccountBalance2 = account.getBalance("2").toCompletableFuture().join();
        assertThat(newAccountBalance1.getBalance(), equalTo(accountBalance1.getBalance()));
        assertThat(newAccountBalance2.getBalance(), equalTo(accountBalance2.getBalance()));
    }

    @Test
    void shouldFailIfSenderAndRecipientAccountAreEqual() {
        BigDecimal amount = BigDecimal.valueOf(100);
        MoneyTransfer transfer = new MoneyTransfer(amount);

        AccountBalance accountBalance1 = account.getBalance("1").toCompletableFuture().join();

        CompletionStage<TransactionResult> response = account.transferMoney("1", "1", transfer);
        TransactionResult result = response.toCompletableFuture().join();
        assertThat(result, instanceOf(TransactionResult.Failure.class));
        assertThat(((TransactionResult.Failure) result).exception, instanceOf(IllegalArgumentException.class));
        AccountBalance newAccountBalance1 = account.getBalance("1").toCompletableFuture().join();
        assertThat(newAccountBalance1.getBalance(), equalTo(accountBalance1.getBalance()));
    }

    @Test
    void shouldFailIfAmountIsLessThanOne() {


        MoneyTransfer transfer = new MoneyTransfer(BigDecimal.ZERO);

        AccountBalance accountBalance1 = account.getBalance("1").toCompletableFuture().join();
        AccountBalance accountBalance2 = account.getBalance("2").toCompletableFuture().join();


        CompletionStage<TransactionResult> response = account.transferMoney("1", "2", transfer);
        TransactionResult result = response.toCompletableFuture().join();
        assertThat(result, instanceOf(TransactionResult.Failure.class));
        assertThat(((TransactionResult.Failure) result).exception, instanceOf(IllegalArgumentException.class));
        AccountBalance newAccountBalance1 = account.getBalance("1").toCompletableFuture().join();
        AccountBalance newAccountBalance2 = account.getBalance("2").toCompletableFuture().join();
        assertThat(newAccountBalance1.getBalance(), equalTo(accountBalance1.getBalance()));
        assertThat(newAccountBalance2.getBalance(), equalTo(accountBalance2.getBalance()));
    }

    @Test
    void shouldRollbackIfCanNotDeposit() throws InterruptedException {
        MoneyTransfer transfer = new MoneyTransfer(BigDecimal.TEN);

        AccountBalance accountBalance1 = account.getBalance("3").toCompletableFuture().join();
        CompletionStage<TransactionResult> response = account.transferMoney("3", "10", transfer);
        TransactionResult result = response.toCompletableFuture().join();
        assertThat(result, instanceOf(TransactionResult.Success.class));
        //so that rollback can take place
        Thread.sleep(5000);
        AccountBalance newAccountBalance1 = account.getBalance("3").toCompletableFuture().join();
        assertThat(newAccountBalance1.getBalance(), equalTo(accountBalance1.getBalance()));
    }
}
