package DataInternals;

public abstract class OnReceive {
    public final Object tag;

    public OnReceive(Object tag) {
        this.tag = tag;
    }

    public abstract void doUponReceipt(Object input);
}