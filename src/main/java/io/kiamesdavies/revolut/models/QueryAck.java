package io.kiamesdavies.revolut.models;

public class QueryAck {

    public final String deliveryId;
    public final Object response;

    public QueryAck(String deliveryId, Object response) {
        this.deliveryId = deliveryId;
        this.response = response;
    }
}
