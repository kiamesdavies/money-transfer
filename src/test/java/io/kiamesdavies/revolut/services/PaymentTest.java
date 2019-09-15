package io.kiamesdavies.revolut.services;

import io.kiamesdavies.revolut.models.AccountBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class PaymentTest {

    Payment payment;

    @Test
    public  void shouldTransferIfAccountsAreAvailableAndAmountIsSufficient(){
        BigDecimal amount = BigDecimal.valueOf(100);
        MoneyTransfer transfer = new MoneyTransfer(amount);
        AccountBalance accountBalance1 = payment.getBalance("1").join();
        AccountBalance accountBalance2 = payment.getBalance("2").join();
        CompletableFuture<TransferResult> response = payment.transferMoney("1","2",transfer);
        TransferResult result = response.join();
        assertThat(result, instanceOf(Sucess.class));
        AccountBalance newAccountBalance1 = payment.getBalance("1").join();
        AccountBalance newAccountBalance2 = payment.getBalance("2").join();
        assertThat(newAccountBalance1.getBalance(),equalTo(accountBalance1.getBalance().subtract(amount)));
        assertThat(newAccountBalance2.getBalance(),equalTo(accountBalance2.getBalance().add(amount)));

    }

    @Test
    public void shouldFailIfSenderAccountIsUnavailable(){
        BigDecimal amount = BigDecimal.valueOf(100);
        MoneyTransfer transfer = new MoneyTransfer(amount);
        AccountBalance accountBalance2 = payment.getBalance("2").join();
        CompletableFuture<TransferResult> response = payment.transferMoney("wrongId","2",transfer);
        TransferResult result = response.join();
        assertThat(result, instanceOf(Failure.class));
        assertThat(((Failure)result).exception, instanceOf(AccountNotFoundException.class));
        AccountBalance newAccountBalance2 = payment.getBalance("2").join();
        assertThat(newAccountBalance2.getBalance(),equalTo(accountBalance2.getBalance()));
    }

    @Test
    public void shouldFailIfRecipientAccountIsUnavailable(){
        BigDecimal amount = BigDecimal.valueOf(100);
        MoneyTransfer transfer = new MoneyTransfer(amount);
        AccountBalance accountBalance1 = payment.getBalance("1").join();
        CompletableFuture<TransferResult> response = payment.transferMoney("1","wrongId",transfer);
        TransferResult result = response.join();
        assertThat(result, instanceOf(Failure.class));
        assertThat(((Failure)result).exception, instanceOf(AccountNotFoundException.class));
        AccountBalance newAccountBalance1 = payment.getBalance("1").join();
        assertThat(newAccountBalance1.getBalance(),equalTo(accountBalance1.getBalance()));
    }

    @Test
    public void shouldFailIfSenderAccountIsNotSufficient(){


        AccountBalance accountBalance1 = payment.getBalance("1").join();
        AccountBalance accountBalance2 = payment.getBalance("2").join();

        BigDecimal amount = accountBalance1.getBalance().add(BigDecimal.TEN);
        MoneyTransfer transfer = new MoneyTransfer(amount);

        CompletableFuture<TransferResult> response = payment.transferMoney("1","2",transfer);
        TransferResult result = response.join();
        assertThat(result, instanceOf(Failure.class));
        assertThat(((Failure)result).exception, instanceOf(InsufficientFundsException.class));
        AccountBalance newAccountBalance1 = payment.getBalance("1").join();
        AccountBalance newAccountBalance2 = payment.getBalance("2").join();
        assertThat(newAccountBalance1.getBalance(),equalTo(accountBalance1.getBalance()));
        assertThat(newAccountBalance2.getBalance(),equalTo(accountBalance2.getBalance()));
    }

    @Test
    public void shouldFailIfSenderAndRecipientAccountAreEqual(){
        BigDecimal amount = BigDecimal.valueOf(100);
        MoneyTransfer transfer = new MoneyTransfer(amount);

        AccountBalance accountBalance1 = payment.getBalance("1").join();

        CompletableFuture<TransferResult> response = payment.transferMoney("1","1",transfer);
        TransferResult result = response.join();
        assertThat(result, instanceOf(Failure.class));
        assertThat(((Failure)result).exception, instanceOf(IllegalArgumentException.class));
        AccountBalance newAccountBalance1 = payment.getBalance("1").join();
        assertThat(newAccountBalance1.getBalance(),equalTo(accountBalance1.getBalance()));
    }

    @Test
    public void shouldFailIfAmountIsLessThanOne(){


        MoneyTransfer transfer = new MoneyTransfer(BigDecimal.ZERO);

        AccountBalance accountBalance1 = payment.getBalance("1").join();
        AccountBalance accountBalance2 = payment.getBalance("2").join();


        CompletableFuture<TransferResult> response = payment.transferMoney("1","2",transfer);
        TransferResult result = response.join();
        assertThat(result, instanceOf(Failure.class));
        assertThat(((Failure)result).exception, instanceOf(IllegalArgumentException.class));
        AccountBalance newAccountBalance1 = payment.getBalance("1").join();
        AccountBalance newAccountBalance2 = payment.getBalance("2").join();
        assertThat(newAccountBalance1.getBalance(),equalTo(accountBalance1.getBalance()));
        assertThat(newAccountBalance2.getBalance(),equalTo(accountBalance2.getBalance()));
    }
}
