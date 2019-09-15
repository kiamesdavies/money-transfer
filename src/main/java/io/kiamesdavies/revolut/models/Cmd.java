package io.kiamesdavies.revolut.models;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A command class
 */
public abstract class Cmd implements Serializable {
    static final long serialVersionUID = 42L;

    public final  String deliveryId;
    public final String bankAccountId;
    public final BigDecimal amount;

    protected Cmd(String deliveryId, String bankAccountId, BigDecimal amount) {
        this.deliveryId = deliveryId;
        this.amount = amount;
        this.bankAccountId = bankAccountId;
    }


    public  static  class  DepositCmd extends  Cmd{

        public DepositCmd(String deliveryId,String bankAccountId, BigDecimal amount){
            super(deliveryId,bankAccountId,amount);
        }
    }

    public  static  class  WithdrawCmd extends  Cmd{

        public WithdrawCmd(String deliveryId,String bankAccountId, BigDecimal amount){
            super(deliveryId,bankAccountId,amount);
        }
    }

}
