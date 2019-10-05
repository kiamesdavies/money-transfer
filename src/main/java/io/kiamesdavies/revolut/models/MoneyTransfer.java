package io.kiamesdavies.revolut.models;

import java.math.BigDecimal;

public class MoneyTransfer {

    private BigDecimal amount;
    private String remarks;
    private String source;

    public MoneyTransfer() {
    }

    public MoneyTransfer(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "MoneyTransfer{" +
                "amount=" + amount +
                ", remarks='" + remarks + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
