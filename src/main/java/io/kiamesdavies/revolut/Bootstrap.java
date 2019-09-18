package io.kiamesdavies.revolut;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import io.kiamesdavies.revolut.account.Bank;
import io.kiamesdavies.revolut.account.BankAccount;
import io.kiamesdavies.revolut.account.UnavailableBankAccount;
import io.kiamesdavies.revolut.controllers.AccountController;
import io.kiamesdavies.revolut.services.Account;
import io.kiamesdavies.revolut.services.impl.DefaultAccount;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Bootstrap {

    private static Bootstrap ourInstance;
    public final Account account;
    public final ActorRef bank;
    public final ActorSystem actorSystem;
    public final ActorMaterializer materializer;
    public final Route route;


    private Bootstrap() {
        actorSystem = ActorSystem.create("system");

        bank = actorSystem.actorOf(Bank.props(makeBankAccounts(actorSystem)), "bank");
        account = new DefaultAccount(actorSystem, bank);
        materializer = ActorMaterializer.create(actorSystem);
        route = new AccountController(actorSystem, account).createRoute();

        actorSystem.scheduler().scheduleOnce(Duration.ofMinutes(30), () -> account.walkBackInTime(), actorSystem.dispatcher());

    }

    /**
     * Set up five bankAccounts with ids 1, 2, 3, 4, 5 and 10 as a bad one
     *
     * @param actorSystem
     * @return
     */
    public static Map<String, ActorRef> makeBankAccounts(ActorSystem actorSystem) {
        Map<String, ActorRef> bankAccounts = IntStream.range(1, 6).mapToObj(String::valueOf).collect(Collectors.toMap(f -> f, f ->
                actorSystem.actorOf(BankAccount.props(f), String.format("supervisor-for-%s", f))));

        //create a bad bank account to demonstrate rollback
        bankAccounts.put("10", actorSystem.actorOf(UnavailableBankAccount.props(), "bad-account"));

        return bankAccounts;
    }

    public static Bootstrap getInstance() {
        if (ourInstance == null) {
            ourInstance = new Bootstrap();
        }
        return ourInstance;
    }

    public Future<Terminated> terminate() {
        ourInstance = null;
        materializer.shutdown();
        return actorSystem.terminate();
    }
}
