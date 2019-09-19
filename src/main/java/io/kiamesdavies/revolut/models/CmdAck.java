package io.kiamesdavies.revolut.models;

/**
 * Command Acknowledgement.
 */
public class CmdAck {
    public final long deliveryId;
    public final Evt event;

    public CmdAck(long deliveryId, Evt event) {
        this.deliveryId = deliveryId;
        this.event = event;
    }

    public static CmdAck from(Cmd.BaseAccountCmd cmd, Evt evt) {

        return new CmdAck(cmd.deliveryId, evt);
    }

}
