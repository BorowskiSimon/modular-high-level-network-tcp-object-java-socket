package ServerInternals;

import DataInternals.Answer;
import DataInternals.OnReceive;
import DataInternals.OnReceiveHandler;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class ClientManager {
    public final boolean OFFLINE;
    private boolean on = true;
    private final HashMap<UUID, ClientThread> clientThreadHashMap = new HashMap<>();
    private boolean DEBUG = false;
    private final OnReceiveHandler onReceiveHandler;
    private volatile ClientThread newClientThread = null;
    private final int max;
    private final Thread heartbeatThread = new Thread(this::heartbeat);

    public ClientManager(boolean DEBUG, boolean OFFLINE, int max) {
        setDEBUG(DEBUG);
        this.OFFLINE = OFFLINE;
        this.max = max;

        this.onReceiveHandler = new OnReceiveHandler(DEBUG);
        init();

        heartbeatThread.start();
    }

    private void init() {
        onReceiveHandler.add(new OnReceive("Disconnect") {
            @Override
            public void doUponReceipt(Object input) {
                if (input instanceof UUID uuid) {
                    broadcast(new Answer("Chat", clientThreadHashMap.get(uuid).name + " disconnected."));
                    clientThreadHashMap.get(uuid).close();
                    clientThreadHashMap.remove(uuid);
                    print(uuid, "disconnected");
                    printStatus();
                }
            }
        });
        onReceiveHandler.add(new OnReceive("Chat") {
            @Override
            public void doUponReceipt(Object input) {
                broadcast(new Answer(TAG, input));
            }
        });
        onReceiveHandler.add(new OnReceive("Connect") {
            @Override
            public void doUponReceipt(Object input) {
                if (input instanceof Object[] objects) {
                    if (objects[0] instanceof UUID id) {
                        String name = (String) objects[1];

                        boolean readyForReconnect = false;
                        if (clientThreadHashMap.containsKey(id)) {
                            if (clientThreadHashMap.get(id).isWaiting()) {
                                readyForReconnect = true;
                            } else {
                                send(new Answer("Chat", "You have been removed. Illegal connect"), id);
                                debug("blocked. illegal try to reconnect");
                                newClientThread = null;
                                return;
                            }
                        }
                        if (readyForReconnect) {
                            clientThreadHashMap.get(id).close();
                            clientThreadHashMap.remove(id);
                            debug("removed old thread: " + id);
                        }

                        newClientThread.name = name;
                        clientThreadHashMap.put(id, newClientThread);
                        newClientThread = null;

                        broadcast(new Answer("Chat", clientThreadHashMap.get(id).name + (readyForReconnect ? " reconnected. Welcome back!" : " connected. Welcome!")));
                        printStatus();
                    }
                }
            }
        });
    }

    public void addOnReceive(OnReceive onReceive) {
        debug("adding " + onReceive.TAG + " for all new clients");
        onReceiveHandler.add(onReceive);
        clientThreadHashMap.forEach(((uuid, clientThread) -> {
            debug(uuid, "adding " + onReceive.TAG);
            clientThread.addOnReceive(onReceive);
        }));
    }

    private void heartbeat() {
        debug("start \"still connected\" loop");
        AtomicBoolean changedActivity = new AtomicBoolean(false);
        AtomicInteger printCounter = new AtomicInteger();
        final int heartbeatPrint = 5;
        final int heartbeatDelay = 5000;
        while (on) {
            changedActivity.set(false);
            clientThreadHashMap.forEach((key, value) -> {
                if (!value.isWaiting() && !value.isConnected()) {
                    printCounter.set(0);
                    print(key, "disconnected");
                    debug("heartbeat: client[" + key + "] changed to waiting (for reconnect)");
                    printStatus();
                    value.setWaiting(true);
                    changedActivity.set(true);
                }
            });

            if (!changedActivity.get()) {
                if (printCounter.get() == heartbeatPrint) {
                    printCounter.set(0);
                    print("heartbeat: nothing happened");
                } else {
                    printCounter.getAndIncrement();
                    debug("heartbeat: nothing happened");
                }
            }

            try {
                Thread.sleep(heartbeatDelay);
            } catch (Exception e) {
                debug("wait error", e);
            }
        }
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

        if (!OFFLINE) {
            creatingClient(client);
            return;
        }
        if (!clientIP.equals("127.0.0.1")) {
            debug("client blocked. this is a offline server");
            return;
        }
        creatingClient(client);
    }

    private void creatingClient(Socket client) {
        if (newClientThread != null) return;
        newClientThread = new ClientThread(DEBUG, client, UUID.randomUUID(), new OnReceiveHandler(onReceiveHandler));
        newClientThread.start();
        newClientThread.send(new Answer("Connect", newClientThread.id));
    }

    public void send(Answer answer, UUID id) {
        if (!isClient(id)) return;
        clientThreadHashMap.get(id).send(answer);
    }

    public void broadcast(Answer answer) {
        int currentConnections = getConnectionAmount();
        if (currentConnections == 0) return;
        print("broadcasting to " + currentConnections + " client" + (currentConnections > 1 ? "s " : " ") + answer);

        clientThreadHashMap.forEach((key, value) -> {
            if (value.isConnected()) {
                value.send(answer);
            }
        });

        debug("broadcast: " + answer);
    }

    private boolean isClient(UUID client) {
        return clientThreadHashMap.get(client) != null;
    }

    public void closeConnections() {
        on = false;
        newClientThread = null;

        closeHeartbeat();
        clientThreadHashMap.forEach((key, value) -> closeClient(key));

        debug("all connection closed");
        printStatus();
    }

    private void closeHeartbeat() {
        if (!heartbeatThread.isAlive()) return;
        try {
            heartbeatThread.join();
            debug("heartbeat thread stopped");
        } catch (Exception e) {
            debug("heartbeat thread stop error", e);
        }
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
        if (this.DEBUG == DEBUG) return;
        this.DEBUG = DEBUG;
        debug("debug: " + DEBUG);
    }

    private void print(String toPrint) {
        System.out.println("Client Manager >> " + toPrint + " <<");

    }

    private void print(UUID id, String toPrint) {
        print("client[" + id + "] " + toPrint);
    }

    private void debug(String toPrint) {
        if (!DEBUG) return;
        print(toPrint);
    }

    private void debug(UUID id, String toPrint) {
        debug("client[" + id + "] " + toPrint);
    }

    private void debug(String toPrint, Exception e) {
        debug(toPrint);
        if (!DEBUG) return;
        e.printStackTrace();
    }
}
