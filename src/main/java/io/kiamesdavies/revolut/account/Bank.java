package io.kiamesdavies.revolut.account;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import io.kiamesdavies.revolut.models.Query;
import io.kiamesdavies.revolut.models.QueryAck;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This is responsible for managing the bank accounts in the application.
 */
public class Bank extends AbstractLoggingActor {


    private final Map<String, ActorRef> bankAccounts;


    public Bank(final Map<String, ActorRef> bankAccounts) {
        this.bankAccounts = bankAccounts;
    }

    public static Props props(Map<String, ActorRef> bankAccounts) {
        return Props.create(Bank.class, bankAccounts);
    }

    @Override
    public void preStart() {
        log().info("Starting bank with {} accounts", bankAccounts.size());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Query.Single.class, s -> !bankAccounts.containsKey(s.bankAccountId),
                        s -> sender().tell(new QueryAck(s.deliveryId, new Query.QueryAckNotFound(s.bankAccountId)), self()))
                .match(Query.Single.class, s -> bankAccounts.containsKey(s.bankAccountId),
                        s -> sender().tell(
                                new QueryAck(s.deliveryId, new Query.BankReference(s.bankAccountId, bankAccounts.get(s.bankAccountId))),
                                self())
                )
                .match(Query.Multiple.class, g -> {
                    Optional<String> notFoundBank = Arrays.stream(g.bankAccountIds).filter(f -> !bankAccounts.containsKey(f)).findFirst();
                    if (notFoundBank.isPresent()) {
                        sender().tell(new QueryAck(g.deliveryId, new Query.QueryAckNotFound(notFoundBank.get())),
                                self());
                    } else {
                        Map<String, Query.BankReference> response = Arrays.stream(g.bankAccountIds)
                                .collect(Collectors.toMap(j -> j, j -> new Query.BankReference(j, bankAccounts.get(j)), (p, q) -> p));
                        sender().tell(new QueryAck(g.deliveryId, response), self());
                    }
                })
                .matchAny(f -> log().error("Unattended Message {}", f))
                .build();
    }
}
