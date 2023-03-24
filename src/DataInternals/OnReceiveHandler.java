package DataInternals;

import java.util.HashMap;

public final class OnReceiveHandler {
    private boolean DEBUG = false;
    private final HashMap<Object, OnReceive> dataTypeHashMap = new HashMap<>();

    public OnReceiveHandler(boolean DEBUG) {
        setDEBUG(DEBUG);
    }

    public OnReceiveHandler(OnReceiveHandler onReceiveHandler) {
        this(onReceiveHandler.DEBUG);
        dataTypeHashMap.putAll(onReceiveHandler.dataTypeHashMap);
    }

    public void add(OnReceive onReceive) {
        if (dataTypeHashMap.containsKey(onReceive.tag)) {
            debug("'" + onReceive.tag + "' already existed");
            return;
        }
        dataTypeHashMap.put(onReceive.tag, onReceive);
        debug("'" + onReceive.tag + "' was added");
    }

    public boolean contain(Object TAG) {
        if (dataTypeHashMap.get(TAG) != null) return true;
        debug("'" + TAG + "' was not found");
        return false;
    }

    public void onReceive(Object TAG, Object input) {
        if (!contain(TAG)) return;
        debug("handling '" + TAG + "'");
        dataTypeHashMap.get(TAG).doUponReceipt(input);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        dataTypeHashMap.forEach((key, value) -> stringBuilder.append("'").append(key).append("'").append(", "));

        return "Data Handler >> " + "can handle following data types: " + stringBuilder.substring(0, stringBuilder.length() - 2) + " <<";
    }


    //DEBUG
    public void setDEBUG(boolean DEBUG) {
        if (this.DEBUG == DEBUG) return;
        this.DEBUG = DEBUG;
        debug("debug: " + DEBUG);
    }

    private void debug(String toPrint) {
        if (!DEBUG) return;
        System.out.println("Data Handler >> " + toPrint + " <<");
    }
}