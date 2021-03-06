package io.kiamesdavies.revolut.controllers;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import io.kiamesdavies.revolut.commons.Utility;
import io.kiamesdavies.revolut.exceptions.AccountNotFoundException;
import io.kiamesdavies.revolut.models.MoneyTransfer;
import io.kiamesdavies.revolut.models.TransactionResult;
import io.kiamesdavies.revolut.services.Account;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.PathMatchers.integerSegment;
import static akka.http.javadsl.server.PathMatchers.segment;

public class AccountController extends AllDirectives {

    private final Account account;
    private final LoggingAdapter log;

    public AccountController(ActorSystem actorSystem, Account account) {
        this.account = account;
        log = Logging.getLogger(actorSystem, this);
    }

    public Route createRoute() {
        return pathPrefix(segment("account").slash(segment()), accountFromId ->
                pathEndOrSingleSlash(() ->
                        get(() -> completeWithFuture(this.getBalance(accountFromId))))
                        .orElse(pathPrefix(segment("transfer").slash(segment()), accountToId ->

                                        pathEndOrSingleSlash(() -> post(() ->
                                                entity(Jackson.unmarshaller(MoneyTransfer.class),
                                                        mock -> completeWithFuture(this.transfer(accountFromId, accountToId, mock)))
                                        ))
                                )
                        )
        ).orElse(get(() -> complete("welcome")));
    }

    private CompletionStage<HttpResponse> transfer(String accountFromId, String accountToId, MoneyTransfer transfer) {

        return account.transferMoney(accountFromId, accountToId, transfer).thenApply(g -> {
            HttpResponse response = HttpResponse.create();
            if (g instanceof TransactionResult.Success) {
                TransactionResult.Success result = (TransactionResult.Success) g;
                return response.withStatus(StatusCodes.OK).withEntity(ContentTypes.APPLICATION_JSON, Utility.toBytes(result));
            } else {
                Throwable f = ((TransactionResult.Failure) g).exception;
                response = response.withEntity(Objects.toString(f.getMessage(), "")).withStatus(StatusCodes.BAD_REQUEST);
                if (f instanceof AccountNotFoundException) {
                    response = response.withStatus(StatusCodes.NOT_FOUND);
                }
            }
            return response;
        });

    }

    private CompletionStage<HttpResponse> getBalance(String accountFromId) {
        return account.getBalance(accountFromId)
                .thenApply(h -> HttpResponse.create().withStatus(StatusCodes.OK).withEntity(ContentTypes.APPLICATION_JSON, Utility.toBytes(h)))
                .exceptionally(g -> HttpResponse.create()
                        .withStatus(StatusCodes.NOT_FOUND)
                        .withEntity(Objects.toString((g.getCause() != null ? g.getCause() : g).getMessage(), "")));
    }


    @Override
    public String toString() {
        return "AccountController";
    }
}
