package DataInternals;

import java.util.HashMap;

public final class DataHandler {
    private boolean DEBUG = false;
    private final HashMap<Object, Data> dataTypeHashMap = new HashMap<>();

    public DataHandler(boolean DEBUG) {
        setDEBUG(DEBUG);
    }

    public DataHandler(DataHandler dataHandler) {
        this(dataHandler.DEBUG);
        dataTypeHashMap.putAll(dataHandler.dataTypeHashMap);
    }

    public void addData(Data data) {
        if (dataTypeHashMap.containsKey(data.TAG)) {
            debug("'" + data.TAG + "' already existed");
            return;
        }
        dataTypeHashMap.put(data.TAG, data);
        debug("'" + data.TAG + "' was added");
    }

    public boolean contain(Object TAG) {
        if (dataTypeHashMap.get(TAG) != null) return true;
        debug("'" + TAG + "' was not found");
        return false;
    }

    public void handle(Object TAG, Object input) {
        if (!contain(TAG)) return;
        debug("handling '" + TAG + "'");
        dataTypeHashMap.get(TAG).handle(input);
    }

    @Override
    public String toString(){
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