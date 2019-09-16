package io.kiamesdavies.revolut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kiamesdavies.revolut.controllers.AccountController;
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


}
