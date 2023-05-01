package DataInternals;

import java.util.HashMap;
import java.util.UUID;

public final class OnReceiveHandler {
    private boolean DEBUG = false;
    private final HashMap<Object, OnReceive> dataTypeHashMap = new HashMap<>();

    private UUID clientID;

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

        updateClientID();
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

    private void updateClientID() {
        dataTypeHashMap.forEach((key, value) -> {
            value.setClientID(clientID);
            debug("'" + clientID + "' was updated for " + "'" + key + "'");
        });
        debug("'" + clientID + "' was updated");
    }

    public void setClientID(UUID clientID) {
        this.clientID = clientID;
    }

    public UUID getClientID() {
        return clientID;
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