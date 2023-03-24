package ClientInternals;

import DataInternals.Answer;
import DataInternals.OnReceive;
import DataInternals.OnReceiveHandler;
import DataInternals.Request;
import Utility.Helper;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;

public final class Client {
    private boolean DEBUG = false;
    private Socket clientSocket;
    private UUID clientID = null;
    public final String serverIP;
    private String ipAddress;
    public final int port;
    public final boolean IPv6;
    private volatile Answer answer;
    private final OnReceiveHandler onReceiveHandler;
    private volatile boolean on = false;
    private volatile boolean handling = false;
    private volatile String clientName;
    private final String unchangedClientName;
    private int uniqueNameCounter = 2;
    private volatile long ping = 0;
    private volatile long timestamp;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            debug("start loop");
            while (on && clientSocket.isConnected()) {
                if (handling) {
                    onReceive();
                } else {
                    read();
                }
            }
            debug("end of loop");
        }
    });

    public Client(boolean DEBUG, String clientName, String serverIP, int port, boolean IPv6) {
        setDEBUG(DEBUG);

        this.unchangedClientName = clientName;
        changeName(clientName);

        this.serverIP = serverIP;
        debug("server ip: " + serverIP);
        this.port = port;
        debug("port: " + port);

        this.IPv6 = IPv6;
        setIPv6();

        onReceiveHandler = new OnReceiveHandler(DEBUG);
        init();
        if (!DEBUG) return;
        debug(onReceiveHandler.toString());
    }

    public Client(String clientName, String serverIP, int port) {
        this(false, clientName, serverIP, port, false);
    }

    public void addOnReceive(OnReceive onReceive) {
        onReceiveHandler.add(onReceive);
    }

    private void init() {
        onReceiveHandler.add(new OnReceive("PingTask") {
            @Override
            public void doUponReceipt(Object input) {
                send(new Request(tag, null));
            }
        });
        onReceiveHandler.add(new OnReceive("Ping") {
            @Override
            public void doUponReceipt(Object input) {
                handlePing();
            }
        });
        onReceiveHandler.add(new OnReceive("Connect") {
            @Override
            public void doUponReceipt(Object input) {
                handleConnect(input);
            }
        });
        onReceiveHandler.add(new OnReceive("ConnectSuccessful") {
            @Override
            public void doUponReceipt(Object input) {
                debug("connected");
            }
        });
        onReceiveHandler.add(new OnReceive("Chat") {
            @Override
            public void doUponReceipt(Object input) {
                System.out.println(input);
            }
        });
        onReceiveHandler.add(new OnReceive("UniqueName") {
            @Override
            public void doUponReceipt(Object input) {
                handleUniqueName();
            }
        });
    }

    private void handlePing() {
        ping = System.currentTimeMillis() - timestamp;
        debug("ping: " + ping + "ms");
        System.out.println("ping: " + ping + "ms");
    }

    private void handleConnect(Object input) {
        clientID = (UUID) input;
        Object[] connectionData = new Object[]{clientID, clientName};
        send(new Request("Connect", connectionData));
    }

    private void handleUniqueName() {
        changeName(unchangedClientName + "_" + uniqueNameCounter);
        uniqueNameCounter++;

        debug("changing name iteratively: " + clientName);

        Object[] connectionData = new Object[]{clientID, clientName};
        send(new Request("Connect", connectionData));
    }

    private void changeName(String name) {
        this.clientName = name;
        send(new Request("ChangeName", name));
        print("name: " + name);
    }

    private void setIPv6() {
        try {
            System.setProperty("java.net.preferIPv6Addresses", "" + IPv6);
            debug("ip format: IPv" + (IPv6 ? "6" : "4"));

            if (IPv6) {
                ipAddress = String.valueOf(Helper.getPublicIPv6());
            } else {
                ipAddress = String.valueOf(Helper.getPublicIPv4());
            }
            ipAddress = ipAddress.substring(1);

            debug("ip: " + ipAddress);
        } catch (Exception e) {
            debug("public ip error", e);
        }
    }

    public void start() {
        on = true;
        try {
            clientSocket = new Socket(serverIP, port);
            debug("socket created");

            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            debug("object stream created");

            thread.start();
        } catch (Exception e) {
            debug("init error", e);
            close();
        }
    }

    private void read() {
        try {
            Object tmp = in.readObject();
            if (tmp instanceof Answer answer) {
                this.answer = answer;
            }
            handling = true;
            debug("read: " + answer);
        } catch (SocketException e) {
            debug("reading socket error", e);
            close();
        } catch (Exception e) {
            close();
            debug("read error", e);
        }
    }

    private void onReceive() {
        onReceiveHandler.onReceive(answer.tag(), answer.answer());
        handling = false;
    }

    public void send(Request request) {
        if (!on || !clientSocket.isConnected() || request == null) return;
        try {
            out.writeObject(request);
            out.flush();
            debug("sent: " + request);
        } catch (Exception e) {
            debug("send error", e);
        }
    }

    public void stop() {
        if (clientSocket != null) {
            print("active disconnect");
            send(new Request("Disconnect", clientID));
        }
        close();
    }

    public void close() {
        debug("disconnecting");
        on = false;
        try {
            if (thread.isAlive()) {
                thread.join();
                debug("thread stopped");
            }
            if (in != null) {
                in.close();
                debug("input stream closed");
            }
            if (out != null) {
                out.close();
                debug("output stream closed");
            }
            if (clientSocket != null) {
                clientSocket.close();
                debug("socket closed");
            }
        } catch (Exception e) {
            debug("close error", e);
        }
        print("stopped");
    }

    public void ping() {
        timestamp = System.currentTimeMillis();
        send(new Request("Ping", null));
    }

    public boolean isOn() {
        return on;
    }

    public UUID getClientID() {
        return clientID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getClientName() {
        return clientName;
    }


    //DEBUG
    public void setDEBUG(boolean DEBUG) {
        if (this.DEBUG == DEBUG) return;
        this.DEBUG = DEBUG;
        debug("debug: " + DEBUG);
    }

    private void print(String toPrint) {
        System.out.println("Client[" + clientID + "] >> " + toPrint + " <<");
    }

    private void debug(String toPrint) {
        if (!DEBUG) return;
        print(toPrint);
    }

    private void debug(String toPrint, Exception e) {
        debug(toPrint);
        if (!DEBUG) return;
        e.printStackTrace();
    }
}
