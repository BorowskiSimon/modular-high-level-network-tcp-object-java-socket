package DataInternals;

public abstract class OnReceive {
    public final Object TAG;

    public OnReceive(Object TAG) {
        this.TAG = TAG;
    }

    public abstract void doUponReceipt(Object input);
}