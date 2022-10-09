package ClientInternals;

import DataInternals.Answer;
import DataInternals.Data;
import DataInternals.DataHandler;
import DataInternals.Request;
import Utility.Helper;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;

public final class Client {
    private boolean DEBUG = false;
    private Socket socket;
    private UUID id = null;
    public final String serverIP;
    public String ip;
    public final int port;
    private final boolean IPv6;
    private volatile Answer answer;
    private DataHandler dataHandler;
    private volatile boolean on = false;
    private volatile boolean handling = false;
    public volatile String name;
    private volatile long ping = 0;
    private volatile long timestamp;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            debug("start loop");
            while (on && socket.isConnected()) {
                if (handling) {
                    handle();
                } else {
                    receive();
                }
            }
            debug("end of loop");
        }
    });

    public Client(boolean DEBUG, String name, String serverIP, int port, boolean IPv6) {
        //TODO timeout für z.b. ping nach 3 sekunden (evtl je nach Data)

        setDEBUG(DEBUG);

        changeName(name);

        this.serverIP = serverIP;
        debug("ip: " + serverIP);
        this.port = port;
        debug("port: " + port);

        this.IPv6 = IPv6;
        setIPv6();
    }

    public Client(String name, String serverIP, int port) {
        this(false, name, serverIP, port, false);
    }

    public void setDataHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        initDataHandler();
        if (!DEBUG) return;
        debug(dataHandler.toString());
    }

    private void initDataHandler() {
        dataHandler.addDataType(new Data("PingTask") {
            @Override
            public void handle(Object input) {
                send(new Request(TAG, null));
            }
        });
        dataHandler.addDataType(new Data("Ping") {
            @Override
            public void handle(Object input) {
                ping = System.currentTimeMillis() - timestamp;
                debug("ping: " + ping + "ms");
                System.out.println("ping: " + ping + "ms");
            }
        });
        dataHandler.addDataType(new Data("Connect") {
            @Override
            public void handle(Object input) {
                id = (UUID) input;
                Object[] objects = new Object[]{id, name};
                send(new Request(TAG, objects));
                debug("connected");
            }
        });
        dataHandler.addDataType(new Data("Chat") {
            @Override
            public void handle(Object input) {
                //TODO make javadoc for overwriting ;)
                System.out.println(input);
            }
        });
        //TODO
    }

    private void changeName(String name) {
        this.name = name;
        send(new Request("ChangeName", name));
        print("name: " + name);
    }

    private void setIPv6() {
        try {
            System.setProperty("java.net.preferIPv6Addresses", "" + IPv6);
            debug("ip format: IPv" + (IPv6 ? "6" : "4"));

            if (IPv6) {
                ip = String.valueOf(Helper.getPublicIPv6());
            } else {
                ip = String.valueOf(Helper.getPublicIPv4());
            }
        } catch (Exception e) {
            debug("public ip error", e);
        }
    }

    public void start() {
        on = true;
        try {
            socket = new Socket(serverIP, port);
            debug("socket created");

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            debug("object stream created");

            thread.start();
        } catch (Exception e) {
            debug("init error", e);
            close();
        }
    }

    private void receive() {
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

    private void handle() {
        dataHandler.handle(answer.TAG(), answer.answer());
        handling = false;
    }

    public void send(Request request) {
        if (!on || !socket.isConnected() || request == null) return;
        try {
            out.writeObject(request);
            out.flush();
            debug("sent: " + request);
        } catch (Exception e) {
            debug("send error", e);
        }
    }

    public void stop() {
        if (socket != null) {
            print("active disconnect");
            send(new Request("Disconnect", id));
        }
        close();
    }

    public void close() {
        debug("disconnecting");
        on = false;
        try {
            if (in != null) {
                in.close();
                debug("input stream closed");
            }
            if (out != null) {
                out.close();
                debug("output stream closed");
            }
            if (socket != null) {
                socket.close();
                debug("socket closed");
            }
        } catch (Exception e) {
            debug("close error", e);
        }
        print("stopped");
    }

    public boolean isOn() {
        return on;
    }

    public UUID getId() {
        return id;
    }

    public void ping() {
        timestamp = System.currentTimeMillis();
        send(new Request("Ping", null));
    }


    //DEBUG
    public void setDEBUG(boolean DEBUG) {
        if (this.DEBUG == DEBUG) return;
        this.DEBUG = DEBUG;
        debug("debug: " + DEBUG);
    }

    private void print(String toPrint) {
        System.out.println("Client[" + id + "] >> " + toPrint + " <<");
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
