package io.kiamesdavies.revolut.services;

import io.kiamesdavies.revolut.models.AccountBalance;
import io.kiamesdavies.revolut.models.MoneyTransfer;
import io.kiamesdavies.revolut.models.TransactionResult;

import java.util.concurrent.CompletionStage;

public interface Account {

    /**
     * Transfer money between two accounts
     *
     * @param accountFromId sender's account
     * @param accountToId   recipient's accouny
     * @param moneyTransfer containing amount to transfer
     * @return Returns {@link TransactionResult.Success} if transfer was successful
     * otherwise returns  {@link TransactionResult.Failure}
     */
    CompletionStage<TransactionResult> transferMoney(String accountFromId, String accountToId, MoneyTransfer moneyTransfer);

    /**
     * Get the balance of an account
     *
     * @param bankAccountId
     * @return Returns the account balance or a completion exception
     */
    CompletionStage<AccountBalance> getBalance(String bankAccountId);


    /**
     * This is meant to be called once when the server starts,
     * it queries the read side of the application and recreates transaction handler for transactions {@link io.kiamesdavies.revolut.models.TransactionStatus} that are in NEW, WITHDRAWN , DEPOSIT_FAILED or ROLLBACK_FAILED status.
     */
    void walkBackInTime();


}
