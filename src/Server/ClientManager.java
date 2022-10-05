package Server;

import DataTypes.Answer;
import DataTypes.Data;
import DataTypes.DataHandler;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientManager {
    public final boolean OFFLINE;
    private final HashMap<UUID, ClientThread> clientThreadHashMap = new HashMap<>();
    private boolean DEBUG = false;
    public final DataHandler dataHandler;

    public ClientManager(boolean DEBUG, boolean OFFLINE, DataHandler dataHandler) {
        setDEBUG(DEBUG);
        this.OFFLINE = OFFLINE;

        this.dataHandler = dataHandler;
        initDataHandler();
    }

    private void initDataHandler() {
        dataHandler.addDataType(new Data("Chat") {
            @Override
            public void handle(Object input) {
                broadcast(new Answer("Chat", input));
            }
        });
        dataHandler.addDataType(new Data("Disconnect") {
            @Override
            public void handle(Object input) {
                if(input instanceof UUID uuid) {
                    clientThreadHashMap.get(uuid).close();
                    clientThreadHashMap.remove(uuid);
                    print(uuid, "disconnected");
                    print("connected clients: " + getConnectionAmount() + " / " + clientThreadHashMap.size());
                }
            }
        });
        //TODO
    }

    public int getConnectionAmount() {
        AtomicInteger ret = new AtomicInteger(0);
        clientThreadHashMap.forEach((key, value) -> {
            if (value.isConnected()) {
                ret.getAndIncrement();
            } else {
                debug(key, "is not connected");
            }
        });

        debug("connected clients: " + ret + " / " + clientThreadHashMap.size());

        return ret.get();
    }

    public void connectionCheck(Socket client) {
        InetSocketAddress socketAddress = (InetSocketAddress) client.getRemoteSocketAddress();
        String clientIP = socketAddress.getAddress().getHostAddress();
        debug("client ip: " + clientIP);

        if (OFFLINE) {
            if (clientIP.equals("127.0.0.1")) {
                creatingClient(client);
            } else {
                debug("client blocked. this is a offline server");
            }
        } else {
            creatingClient(client);
        }
    }

    public void creatingClient(Socket client) {
        UUID id = UUID.randomUUID();
        ClientThread clientThread = new ClientThread(DEBUG, client, id, dataHandler);

        reconnectionCheckAndConnect(clientThread, id);
    }

    private void reconnectionCheckAndConnect(ClientThread clientThread, UUID id) {
        boolean connected = clientThread.connecting();
        debug(id, "initial connect: " + connected);
        if (connected) {

            UUID reconnectionID = clientThread.reconnecting();
            boolean reconnected = reconnectionID != null && clientThreadHashMap.containsKey(reconnectionID);

            if (reconnected) {
                id = reconnectionID;
                clientThreadHashMap.get(id).close();
                clientThreadHashMap.remove(id);
                debug("removed old thread: " + id);
            }

            debug("client id: " + id);
            clientThreadHashMap.put(id, clientThread);

            print(id, (reconnected ? "re" : "") + "connected");
            print("connected clients: " + getConnectionAmount() + " / " + clientThreadHashMap.size());

            clientThreadHashMap.get(id).start();
        }
    }

    public void send(Answer answer, UUID id) {
        if (isClient(id)) {
            clientThreadHashMap.get(id).send(answer);
        }
    }

    public void broadcast(Answer answer) {
        int currentConnections = getConnectionAmount();
        if (currentConnections > 0) {

            print("broadcasting to " + currentConnections + " client" + (currentConnections > 1 ? "s " : " ") + answer);

            clientThreadHashMap.forEach((key, value) -> {
                if (value.isConnected()) {
                    value.send(answer);
                }
            });

            debug("broadcast: " + answer);
        }
    }

    private boolean isClient(UUID client) {
        return clientThreadHashMap.get(client) != null;
    }

    public void closeConnections() {
        clientThreadHashMap.forEach((key, value) -> closeClient(key));
    }

    public void closeClient(UUID id) {
        clientThreadHashMap.get(id).close();
    }

    public void removeClient(UUID id) {
        closeClient(id);
        clientThreadHashMap.remove(id);
    }


    //DEBUG
    public void setDEBUG(boolean DEBUG) {
        if (this.DEBUG != DEBUG) {
            this.DEBUG = DEBUG;
            debug("debug: " + DEBUG);
        }
    }

    private void print(String toPrint) {
        System.out.println("Client Manager >> " + toPrint + " <<");

    }

    private void print(UUID id, String toPrint) {
        print("client[" + id + "] " + toPrint);
    }

    private void debug(String toPrint) {
        if (DEBUG) {
            print(toPrint);
        }
    }

    private void debug(UUID id, String toPrint) {
        debug("client[" + id + "] " + toPrint);
    }
}
