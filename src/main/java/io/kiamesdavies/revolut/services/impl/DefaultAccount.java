package io.kiamesdavies.revolut.services.impl;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import io.kiamesdavies.revolut.exceptions.AccountNotFoundException;
import io.kiamesdavies.revolut.models.*;
import io.kiamesdavies.revolut.services.Account;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class DefaultAccount implements Account {

    private final LoggingAdapter log;

    private final ActorRef bank;
    private final ActorSystem actorSystem;
    private final static  Random RANDOM = new Random();

    public DefaultAccount(ActorSystem actorSystem, ActorRef bank) {
        this.actorSystem = actorSystem;
        this.bank = bank;
        log = Logging.getLogger(actorSystem, this);
    }

    /**
     * Transfer money between two accounts
     *
     * @param accountFromId sender's account
     * @param accountToId   recipient's accouny
     * @param moneyTransfer containing amount to transfer
     * @return Returns {@link TransactionResult.Success} if transfer was successful
     * otherwise returns  {@link TransactionResult.Failure}
     */
    @Override
    public CompletionStage<TransactionResult> transferMoney(String accountFromId, String accountToId, MoneyTransfer moneyTransfer) {

        String transactionId = UUID.randomUUID().toString();
        ActorRef transferHandler = actorSystem.actorOf(TransferHandler.props(transactionId, bank), String.format("transaction-%s", transactionId));
        return Patterns.ask(transferHandler, new Cmd.TransferCmd(accountFromId, accountToId, moneyTransfer.getAmount(), moneyTransfer.getRemarks(), moneyTransfer.getSource()), Duration.ofSeconds(60)).exceptionally(ex -> {
            log.error("Failed to transfer", ex);
            return new TransactionResult.Failure(ex);
        }).thenApply(g -> {
            if (g instanceof TransactionResult) {
                return (TransactionResult) g;
            }
            log.error("Unknown type {}", g);
            return new TransactionResult.Failure(new IllegalStateException(String.format("Unknown type %s", Objects.toString(g))));
        });

    }

    /**
     * Get the balance of an account
     *
     * @param bankAccountId
     * @return Returns the account balance or a completion exception
     */
    @Override
    public CompletionStage<AccountBalance> getBalance(String bankAccountId) {

        return Patterns.ask(bank, new Query.Single(RANDOM.nextLong(), bankAccountId), Duration.ofSeconds(3))
                .thenCompose(f -> {
                    if (f instanceof Query.QueryAckNotFound) {
                        throw new CompletionException(new AccountNotFoundException(String.format("bank account %s not found", bankAccountId)));
                    }
                    Query.BankReference bankReference = (Query.BankReference) ((QueryAck) f).response;
                    return Patterns.ask(bankReference.bankAccount, new Query.Single(RANDOM.nextLong(), bankAccountId), Duration.ofSeconds(5)).thenApply(g -> {
                        if (g instanceof Query.QueryAckNotFound) {
                            log.error("Account {} failed to respond bank account = id {}", bankReference, bankAccountId);
                            throw new CompletionException(new IllegalStateException(String.format("bank account %s not found", bankAccountId)));
                        }
                        return (AccountBalance) ((QueryAck) g).response;
                    });
                });
    }

    /**
     * This is meant to be called once when the server starts,
     * it queries the read side of the application and recreates transaction handler for transactions {@link TransactionStatus} that are in NEW, WITHDRAWN , DEPOSIT_FAILED status.
     */
    @Override
    public void walkBackInTime() {
        //TODO Queries the read side for transactions with the aforementioned status
        List<String> hangingTransactions = new ArrayList<>();
        hangingTransactions.forEach(transactionId -> {
            //automatically it will create its state from previous event and continue from where it stopped
            actorSystem.actorOf(TransferHandler.props(transactionId, bank), String.format("transaction-%s", transactionId));
        });
    }

    @Override
    public String toString() {
        return "DefaultAccount";
    }
}
