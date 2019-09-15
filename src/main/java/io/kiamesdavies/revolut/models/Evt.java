package io.kiamesdavies.revolut.models;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * An Event class
 */
public abstract class Evt implements Serializable {
    static final long serialVersionUID = 42L;

    public final String bankAccountId;
    public final BigDecimal amount;

    protected Evt(String bankAccountId, BigDecimal amount) {
        this.bankAccountId = bankAccountId;
        this.amount = amount;
    }


    public  static class  DepositEvent extends  Evt{

        public DepositEvent(String bankAccountId, BigDecimal amount){
            super(bankAccountId,amount);
        }

        public  DepositEvent(Cmd.DepositCmd depositCmd){
            this(depositCmd.bankAccountId, depositCmd.amount);
        }
    }

    public  static class  WithdrawEvent extends  Evt{

        public WithdrawEvent(String bankAccountId, BigDecimal amount){
            super(bankAccountId,amount);
        }

        public  WithdrawEvent(Cmd.WithdrawCmd withdrawCmd){
            this(withdrawCmd.bankAccountId, withdrawCmd.amount);
        }
    }

    public  static  class FailedEvent extends  Evt{


        public enum Type{
            INSUFFICIENT_FUNDS,
            INVALID_AMOUNT
        }

        public final Type  type;
        public final String additionalDescription;

        public FailedEvent(String bankAccountId, Type type, String additionalDescription){
            super(bankAccountId, BigDecimal.ZERO);
            this.type=type;
            this.additionalDescription = additionalDescription;
        }

        public FailedEvent(String bankAccountId, Type type){
            this(bankAccountId, type, null);
        }
    }




}
