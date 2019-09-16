package io.kiamesdavies.revolut.controllers;

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import io.kiamesdavies.revolut.models.MoneyTransfer;
import io.kiamesdavies.revolut.models.TransactionResult;
import io.kiamesdavies.revolut.services.Payment;

import java.math.BigDecimal;

public class PaymentController  extends AllDirectives {

    private final Payment payment;

    public PaymentController(Payment payment) {
        this.payment = payment;
    }

    public Route createRoute() {
        final Marshaller<TransactionResult, HttpResponse> marshaller = Marshaller.entityToOKResponse(Jackson.marshaller());

        return concat(
                path("hello", () ->
                        get(() ->
                                complete("hello"))));
    }
}
