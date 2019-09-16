package io.kiamesdavies.revolut.controllers;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import akka.stream.ActorMaterializer;
import io.kiamesdavies.revolut.Bootstrap;
import io.kiamesdavies.revolut.commons.Utility;
import io.kiamesdavies.revolut.models.MoneyTransfer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class AccountControllerTest extends JUnitRouteTest {


    private static Bootstrap instance;
    private TestRoute appRoute = testRoute(instance.route);

    @BeforeAll
    static void setup() {
        instance = Bootstrap.getInstance();

    }

    @AfterAll
    static void teardown() {
        instance.terminate();
    }

    @Test
    void testhome() {

        appRoute.run(HttpRequest.GET("/"))
                .assertStatusCode(200)
                .assertEntity("welcome");
    }

    @Test
    void shouldReturn200AndTransactionIdIfAccountsAreAvailableAndAmountIsSufficient() {

        appRoute.run(HttpRequest.POST("/account/1/transfer/2").withEntity(ContentTypes.APPLICATION_JSON, Utility.toBytes(new MoneyTransfer(BigDecimal.valueOf(100)))))
                .assertStatusCode(200)
                .assertContentType(ContentTypes.APPLICATION_JSON);
    }

    @Test
    void shouldReturn404IfSenderOrRecipientAccountIsUnavailable() {
        appRoute.run(HttpRequest.POST("/account/100/transfer/2").withEntity(ContentTypes.APPLICATION_JSON, Utility.toBytes(new MoneyTransfer(BigDecimal.valueOf(100)))))
                .assertStatusCode(404);

        appRoute.run(HttpRequest.POST("/account/1/transfer/200").withEntity(ContentTypes.APPLICATION_JSON, Utility.toBytes(new MoneyTransfer(BigDecimal.valueOf(100)))))
                .assertStatusCode(404);
    }

    @Test
    void shouldReturn400IfSenderAccountIsNotSufficient() {
        appRoute.run(HttpRequest.POST("/account/1/transfer/2").withEntity(ContentTypes.APPLICATION_JSON, Utility.toBytes(new MoneyTransfer(BigDecimal.valueOf(1000000)))))
                .assertStatusCode(400);
    }

    @Test
    void shouldReturn400IfSenderAndRecipientAccountAreEqual() {
        appRoute.run(HttpRequest.POST("/account/1/transfer/1/").withEntity(ContentTypes.APPLICATION_JSON, Utility.toBytes(new MoneyTransfer(BigDecimal.valueOf(1000000)))))
                .assertStatusCode(400);
    }

    @Test
    void shouldReturn400IfAmountIsLessThanOne() {
        appRoute.run(HttpRequest.POST("/account/1/transfer/2").withEntity(ContentTypes.APPLICATION_JSON, Utility.toBytes(new MoneyTransfer(BigDecimal.ZERO))))
                .assertStatusCode(400);
    }

    @Test
    void shouldReturn200IfBalanceIsRequested() {
        appRoute.run(HttpRequest.GET("/account/5/"))
                .assertStatusCode(200)
                .assertContentType(ContentTypes.APPLICATION_JSON);
    }

    @Test
    void shouldReturn404IfWrongAccountBalanceIsRequested() {
        appRoute.run(HttpRequest.GET("/account/100"))
                .assertStatusCode(404);
    }

    @Test
    void shouldReturn404IfBadAccountBalanceIsRequested() {
        appRoute.run(HttpRequest.GET("/account/10"))
                .assertStatusCode(404);
    }

    public ActorMaterializer materializer() {
        return instance.materializer;
    }

    public ActorSystem system() {
        return instance.actorSystem;
    }

    @Override
    public FiniteDuration awaitDuration() {
        return FiniteDuration.create(10, TimeUnit.SECONDS);
    }
}
