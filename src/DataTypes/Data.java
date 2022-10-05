package DataTypes;

public abstract class Data {
    private final Object TAG;

    public Data(Object TAG) {
        this.TAG = TAG;
    }

    public abstract void handle(Object input);

    public Object getTAG(){
        return TAG;
    }
}