package DataInternals;

import java.util.UUID;

public abstract class OnReceive {
    public final Object tag;

    private UUID clientID;

    public OnReceive(Object tag) {
        this.tag = tag;
    }

    public abstract void doUponReceipt(Object input);

    public void setClientID(UUID clientID) {
        this.clientID = clientID;
    }

    public UUID getClientID() {
        return clientID;
    }
}