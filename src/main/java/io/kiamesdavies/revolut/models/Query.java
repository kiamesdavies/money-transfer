package io.kiamesdavies.revolut.models;

import akka.actor.ActorRef;


public class Query {


    public static class Single {
        public final long deliveryId;
        public final String bankAccountId;


        public Single(long deliveryId, String bankAccountId) {
            this.deliveryId = deliveryId;
            this.bankAccountId = bankAccountId;
        }

    }

    public static class Multiple {
        public final long deliveryId;
        public final String[] bankAccountIds;


        public Multiple(long deliveryId, String... bankAccountIds) {
            this.deliveryId = deliveryId;
            this.bankAccountIds = bankAccountIds;
        }
    }

    public static class QueryAckNotFound {
        public final String bankAccountId;

        public QueryAckNotFound(String bankAccountId) {
            this.bankAccountId = bankAccountId;
        }
    }

    public static class BankReference {
        public final String bankAccountId;
        public final ActorRef bankAccount;

        public BankReference(String bankAccountId, ActorRef bankAccount) {
            this.bankAccountId = bankAccountId;
            this.bankAccount = bankAccount;
        }
    }

}
