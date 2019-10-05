package io.kiamesdavies.revolut.account;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

/**
 * A no action bank account
 */
public final class UnavailableBankAccount extends AbstractLoggingActor {
    public static Props props() {
        return Props.create(UnavailableBankAccount.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(f -> log().error("Unattended Message {}", f))
                .build();
    }
}
