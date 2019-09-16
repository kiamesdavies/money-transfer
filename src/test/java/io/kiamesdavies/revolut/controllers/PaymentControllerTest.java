package io.kiamesdavies.revolut.controllers;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import akka.stream.ActorMaterializer;
import io.kiamesdavies.revolut.Bootstrap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PaymentControllerTest extends JUnitRouteTest {


    private static Bootstrap instance;
    private TestRoute appRoute = testRoute(instance.route);

    @Test
    void shouldReturn200AndTransactionIdIfAccountsAreAvailableAndAmountIsSufficient(){
        appRoute.run(HttpRequest.POST("/account/1/transfer/2").withEntity("{\"amount\":100}"))
                .assertStatusCode(200);
    }

    @Test
    void shouldReturn404IfSenderOrRecipientAccountIsUnavailable(){
        appRoute.run(HttpRequest.POST("/account/100/transfer/2").withEntity("{\"amount\":100}"))
                .assertStatusCode(404);

        appRoute.run(HttpRequest.POST("/account/1/transfer/200").withEntity("{\"amount\":100}"))
                .assertStatusCode(404);
    }

    @Test
    void shouldReturn400IfSenderAccountIsNotSufficient(){
        appRoute.run(HttpRequest.POST("/account/1/transfer/2").withEntity("{\"amount\":10000000}"))
                .assertStatusCode(404);
    }

    @Test
    void shouldReturn400IfSenderAndRecipientAccountAreEqual(){
        appRoute.run(HttpRequest.POST("/account/1/transfer/1").withEntity("{\"amount\":100}"))
                .assertStatusCode(200);
    }

    @Test
    void shouldReturn400IfAmountIsLessThanOne(){
        appRoute.run(HttpRequest.POST("/account/1/transfer/2").withEntity("{\"amount\":0}"))
                .assertStatusCode(200);
    }

    @Test
    void shouldReturn200IfBalanceIsRequested(){
        appRoute.run(HttpRequest.GET("/account/5/"))
                .assertStatusCode(200);
    }

    @Test
    void shouldReturn404IfWrongAccountBalanceIsRequested(){
        appRoute.run(HttpRequest.GET("/account/100/"))
                .assertStatusCode(200);
    }

    @Test
    void shouldReturn404IfBadAccountBalanceIsRequested(){
        appRoute.run(HttpRequest.GET("/account/10/"))
                .assertStatusCode(200);
    }




    public ActorMaterializer materializer(){
        return  instance.materializer;
    }
    public ActorSystem system(){
        return instance.actorSystem;
    }

    @BeforeAll
    static void setup() {
        instance = Bootstrap.getInstance();

    }

    @AfterAll
    static void teardown() {
        instance.terminate();
    }
}
