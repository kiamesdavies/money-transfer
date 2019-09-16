package io.kiamesdavies.revolut.account;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

public class BadBankAccount extends AbstractLoggingActor {
    public static Props props() {
        return Props.create(BadBankAccount.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(f -> log().error("Unattended Message {}", f))
                .build();
    }
}
