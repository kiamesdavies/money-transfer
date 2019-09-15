package io.kiamesdavies.revolut;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import io.kiamesdavies.revolut.account.Bank;
import io.kiamesdavies.revolut.account.BankAccount;
import io.kiamesdavies.revolut.services.Payment;
import io.kiamesdavies.revolut.services.impl.DefaultPayment;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Bootstrap {

    private static Bootstrap ourInstance;
    public final Payment payment;
    public final ActorRef bank;
    public final ActorSystem actorSystem;


    private Bootstrap() {
        actorSystem = ActorSystem.create("system");

        //set up five bankAccounts with ids 1, 2, 3, 4, 5
        Map<String, ActorRef> bankAccounts = makeBankAccounts(actorSystem);

        //create a bad bank account to simulate rollback
        bankAccounts.put("10", ActorRef.noSender());

        bank = actorSystem.actorOf(Bank.props(bankAccounts), "bank");
        payment = new DefaultPayment(actorSystem, bank);

    }

    public static Map<String, ActorRef> makeBankAccounts(ActorSystem actorSystem) {
        return IntStream.range(1, 6).mapToObj(String::valueOf).collect(Collectors.toMap(f -> f, f ->
                actorSystem.actorOf(BankAccount.props(f), String.format("supervisor-for-%s", f))));
    }

    public static Bootstrap getInstance() {
        if (ourInstance == null) {
            ourInstance = new Bootstrap();
        }
        return ourInstance;
    }
}
