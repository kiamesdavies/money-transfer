package io.kiamesdavies.revolut.account;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import io.kiamesdavies.revolut.models.Query;

import java.util.Map;

/**
 * This is responsible for managing the bank accounts in the application
 */
public class Bank extends AbstractLoggingActor {


    private final   Map<String, ActorRef> bankAccounts;


    public Bank(Map<String, ActorRef> bankAccounts) {
        this.bankAccounts = bankAccounts;
    }

    @Override
    public void preStart() {
        log().info("Starting bank with {} accounts", bankAccounts.size() );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Query.class, s -> !bankAccounts.containsKey(s.bankAccountId),
                        s -> sender().tell(new Query.QueryAckNotFound(s.bankAccountId), self()))
                .match(Query.class , s -> bankAccounts.containsKey(s.bankAccountId),
                        s -> sender().tell(new Query.BankAccount(s.bankAccountId, bankAccounts.get(s.bankAccountId)), self()))
                .build();
    }

    public static Props props( Map<String, ActorRef> bankAccounts){
        return Props.create(Bank.class,bankAccounts);
    }
}
