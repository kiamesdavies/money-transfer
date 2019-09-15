package io.kiamesdavies.revolut.services;

public interface Payment {

    void transferMoney(String accountFromId, String accountToId);
}
