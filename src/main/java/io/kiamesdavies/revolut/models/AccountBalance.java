package io.kiamesdavies.revolut.models;

import java.io.Serializable;
import java.math.BigDecimal;

public class AccountBalance implements Serializable {
    static final long serialVersionUID = 42L;

    private String bankAccountId;
    private BigDecimal balance;

    public AccountBalance(String bankAccountId, BigDecimal balance) {
        this.balance = balance;
        this.bankAccountId = bankAccountId;
    }

    public static AccountBalance deposit(AccountBalance account, BigDecimal amount) {
        account.balance = account.balance.add(amount);
        return account;
    }

    public static AccountBalance withdraw(AccountBalance account, BigDecimal amount) {
        account.balance = account.balance.subtract(amount);
        return account;
    }

    public String getBankAccountId() {
        return bankAccountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountBalance copy() {
        return new AccountBalance(bankAccountId, balance);
    }

}