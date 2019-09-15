package io.kiamesdavies.revolut.models;

public class CmdAck {
    public final String deliveryId;
    public final Evt event;

    public CmdAck(String deliveryId, Evt event) {
        this.deliveryId = deliveryId;
        this.event = event;
    }

    public static CmdAck from(Cmd cmd, Evt evt){
        return new CmdAck(cmd.deliveryId, evt);
    }

}
