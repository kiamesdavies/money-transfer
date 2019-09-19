package io.kiamesdavies.revolut.models;

public class QueryAck {

    public final long deliveryId;
    public final Object response;

    public QueryAck(long deliveryId, Object response) {
        this.deliveryId = deliveryId;
        this.response = response;
    }
}
