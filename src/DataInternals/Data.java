package DataInternals;

public abstract class Data {
    public final Object TAG;

    public Data(Object TAG) {
        this.TAG = TAG;
    }

    public abstract void handle(Object input);
}