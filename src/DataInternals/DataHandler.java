package DataInternals;

import java.util.HashMap;

public final class DataHandler {
    private boolean DEBUG = false;
    private final HashMap<Object, Data> dataTypeHashMap = new HashMap<>();

    public DataHandler(boolean DEBUG) {
        setDEBUG(DEBUG);
    }

    public void addDataType(Data data) {
        if (!dataTypeHashMap.containsKey(data.TAG)) {
            dataTypeHashMap.put(data.TAG, data);
            debug("'" + data.TAG + "' was added");
        } else {
            debug("'" + data.TAG + "' already existed");
        }
    }

    public boolean exist(Object TAG) {
        Data dataDecoder = dataTypeHashMap.get(TAG);
        if (dataDecoder == null) {
            debug("'" + TAG + "' was not found");
            return false;
        }
        return true;
    }

    public void handle(Object TAG, Object input) {
        if (exist(TAG)) {
            debug("handling '" + TAG + "'");
            dataTypeHashMap.get(TAG).handle(input);
        }
    }

    public void printDataTypes() {
        StringBuilder stringBuilder = new StringBuilder();
        dataTypeHashMap.forEach((key, value) -> stringBuilder.append("'").append(key).append("'").append(", "));

        debug("can handle following data types: " + stringBuilder.substring(0, stringBuilder.length() - 2));
    }


    //DEBUG
    public void setDEBUG(boolean DEBUG) {
        if (this.DEBUG != DEBUG) {
            this.DEBUG = DEBUG;
            debug("debug: " + DEBUG);
        }
    }

    private void debug(String toPrint) {
        if (DEBUG) System.out.println("Data Handler >> " + toPrint + " <<");
    }
}