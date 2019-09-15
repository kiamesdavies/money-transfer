package io.kiamesdavies.revolut.models;

import akka.actor.ActorRef;



public class Query {
    public final String bankAccountId;


    public Query( String bankAccountId) {
        this.bankAccountId = bankAccountId;
    }


    public static  class QueryAckNotFound{
        public final String bankAccountId;
        public QueryAckNotFound( String bankAccountId) {
            this.bankAccountId = bankAccountId;
        }
    }



    public  static  class BankAccount{
        public final String bankAccountId;
        public final ActorRef bankAccount;

        public BankAccount(String bankAccountId, ActorRef bankAccount) {
            this.bankAccountId = bankAccountId;
            this.bankAccount = bankAccount;
        }
    }

}
