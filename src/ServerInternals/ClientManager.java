package ServerInternals;

import DataInternals.Answer;
import DataInternals.Data;
import DataInternals.DataHandler;

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
    private volatile ClientThread currentClientThread;
    private final int max;

    public ClientManager(boolean DEBUG, boolean OFFLINE, int max, DataHandler dataHandler) {
        setDEBUG(DEBUG);
        this.OFFLINE = OFFLINE;
        this.max = max;

        this.dataHandler = dataHandler;
        initDataHandler();
    }

    private void initDataHandler() {
        dataHandler.addDataType(new Data("Disconnect") {
            @Override
            public void handle(Object input) {
                if (input instanceof UUID uuid) {
                    broadcast(new Answer("Chat", clientThreadHashMap.get(uuid).name + " disconnected."));
                    clientThreadHashMap.get(uuid).close();
                    clientThreadHashMap.remove(uuid);
                    print(uuid, "disconnected");
                    printStatus();
                }
            }
        });
        dataHandler.addDataType(new Data("Chat") {
            @Override
            public void handle(Object input) {
                broadcast(new Answer(TAG, input));
            }
        });
        dataHandler.addDataType(new Data("Connect") {
            @Override
            public void handle(Object input) {
                if (input instanceof Object[] objects) {
                    if (objects[0] instanceof UUID id) {
                        String name = (String) objects[1];
                        boolean reconnected = clientThreadHashMap.containsKey(id);
                        if (reconnected) {
                            clientThreadHashMap.get(id).close();
                            clientThreadHashMap.remove(id);
                            debug("removed old thread: " + id);
                        }
                        currentClientThread.name = name;
                        clientThreadHashMap.put(id, currentClientThread);

                        broadcast(new Answer("Chat", clientThreadHashMap.get(id).name + (reconnected ? " reconnected. Welcome back!" : " connected. Welcome!")));
                        printStatus();
                    }
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

        debug("current clients: " + ret + " / " + clientThreadHashMap.size());

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
        currentClientThread = new ClientThread(DEBUG, client, UUID.randomUUID(), dataHandler);
        currentClientThread.start();
        currentClientThread.send(new Answer("Connect", currentClientThread.id));
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

    private void printStatus() {
        broadcast(new Answer("Chat", "Connected clients: " + getConnectionAmount() + " / " + max));
        print("connected clients: " + getConnectionAmount() + " / " + max);
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
