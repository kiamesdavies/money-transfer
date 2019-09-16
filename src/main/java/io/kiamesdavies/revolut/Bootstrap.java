package io.kiamesdavies.revolut;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import io.kiamesdavies.revolut.account.BadBankAccount;
import io.kiamesdavies.revolut.account.Bank;
import io.kiamesdavies.revolut.account.BankAccount;
import io.kiamesdavies.revolut.controllers.PaymentController;
import io.kiamesdavies.revolut.services.Payment;
import io.kiamesdavies.revolut.services.impl.DefaultPayment;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Bootstrap {

    private static Bootstrap ourInstance;
    public final Payment payment;
    public final ActorRef bank;
    public final ActorSystem actorSystem;
    public final CompletionStage<ServerBinding> binding;


    private Bootstrap() {
        actorSystem = ActorSystem.create("system");


        Map<String, ActorRef> bankAccounts = makeBankAccounts(actorSystem);

        bank = actorSystem.actorOf(Bank.props(bankAccounts), "bank");
        payment = new DefaultPayment(actorSystem, bank);


        ActorMaterializer materializer = ActorMaterializer.create(actorSystem);

        //In order to access all directives we need an instance where the routes are define.
        PaymentController app = new PaymentController(payment);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(actorSystem, materializer);
        binding = Http.get(actorSystem).bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 8080), materializer);

        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");

    }

    /**
     * Set up five bankAccounts with ids 1, 2, 3, 4, 5 and 10 as a bad one
     * @param actorSystem
     * @return
     */
    public static Map<String, ActorRef> makeBankAccounts(ActorSystem actorSystem) {
        Map<String, ActorRef> bankAccounts = IntStream.range(1, 6).mapToObj(String::valueOf).collect(Collectors.toMap(f -> f, f ->
                actorSystem.actorOf(BankAccount.props(f), String.format("supervisor-for-%s", f))));

        //create a bad bank account to demonstrate rollback
        bankAccounts.put("10", actorSystem.actorOf(BadBankAccount.props(),"bad-account"));

        return bankAccounts;
    }

    public static Bootstrap getInstance() {
        if (ourInstance == null) {
            ourInstance = new Bootstrap();
        }
        return ourInstance;
    }
}
