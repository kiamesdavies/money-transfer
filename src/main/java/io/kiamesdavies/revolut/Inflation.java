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

/**
 * Replaces the need for Guice DI and glue all the application code together.
 */
public final class Inflation {

    private static Inflation ourInstance;
    private final Account account;
    private final ActorRef bank;
    private final ActorSystem actorSystem;
    private final ActorMaterializer materializer;
    private final Route route;


    private Inflation() {
        actorSystem = ActorSystem.create("system");

        bank = getActorSystem().actorOf(Bank.props(initiateDemoBankAccounts(getActorSystem())), "bank");
        account = new DefaultAccount(getActorSystem(), getBank());
        materializer = ActorMaterializer.create(getActorSystem());
        route = new AccountController(getActorSystem(), getAccount()).createRoute();

        getActorSystem()
                .scheduler()
                .scheduleOnce(
                        Duration.ofMinutes(getActorSystem().settings().config().getInt("server.minutes-to-recreate-hanging-transactions")),
                        () -> getAccount().walkBackInTime(), getActorSystem().dispatcher()
                );

    }

    /**
     * Set up five bankAccounts with ids 1, 2, 3, 4, 5 and 10 as a bad one
     *
     * @param actorSystem akka actor system
     * @return array of demo accounts
     */
    public static Map<String, ActorRef> initiateDemoBankAccounts(ActorSystem actorSystem) {
        Map<String, ActorRef> bankAccounts = IntStream.range(1, 6).mapToObj(String::valueOf)
                .collect(Collectors.toMap(f -> f, f ->
                        actorSystem.actorOf(BankAccount.props(f), String.format("supervisor-for-%s", f))));

        //create a bad bank account to demonstrate rollback
        bankAccounts.put("10", actorSystem.actorOf(UnavailableBankAccount.props(), "bad-account"));

        return bankAccounts;
    }

    /**
     *
     * @return this inflation object
     */
    public static Inflation getInstance() {
        if (getOurInstance() == null) {
            ourInstance = new Inflation();
        }
        return getOurInstance();
    }


    /**
     * Shutdown actor system and set the instance to null
     * @return
     */
    public static Future<Terminated> terminate() {
        getOurInstance().getMaterializer().shutdown();
        Future<Terminated> terminate = getOurInstance().getActorSystem().terminate();
        ourInstance = null;
        return terminate;
    }

    public static Inflation getOurInstance() {
        return ourInstance;
    }

    public Account getAccount() {
        return account;
    }

    public ActorRef getBank() {
        return bank;
    }

    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    public ActorMaterializer getMaterializer() {
        return materializer;
    }

    public Route getRoute() {
        return route;
    }
}
