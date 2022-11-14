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
    private Socket socket;
    private UUID id = null;
    public final String serverIP;
    private String ip;
    public final int port;
    public final boolean IPv6;
    private volatile Answer answer;
    private OnReceiveHandler onReceiveHandler;
    private volatile boolean on = false;
    private volatile boolean handling = false;
    private volatile String name;
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
                    onReceive();
                } else {
                    read();
                }
            }
            debug("end of loop");
        }
    });

    public Client(boolean DEBUG, String name, String serverIP, int port, boolean IPv6) {
        setDEBUG(DEBUG);

        changeName(name);

        this.serverIP = serverIP;
        debug("server ip: " + serverIP);
        this.port = port;
        debug("port: " + port);

        this.IPv6 = IPv6;
        setIPv6();

        init();
        if (!DEBUG) return;
        debug(onReceiveHandler.toString());
    }

    public Client(String name, String serverIP, int port) {
        this(false, name, serverIP, port, false);
    }

    public void addOnReceive(OnReceive onReceive) {
        onReceiveHandler.add(onReceive);
    }

    private void init() {
        onReceiveHandler.add(new OnReceive("PingTask") {
            @Override
            public void doUponReceipt(Object input) {
                send(new Request(TAG, null));
            }
        });
        onReceiveHandler.add(new OnReceive("Ping") {
            @Override
            public void doUponReceipt(Object input) {
                ping = System.currentTimeMillis() - timestamp;
                debug("ping: " + ping + "ms");
                System.out.println("ping: " + ping + "ms");
            }
        });
        onReceiveHandler.add(new OnReceive("Connect") {
            @Override
            public void doUponReceipt(Object input) {
                id = (UUID) input;
                Object[] objects = new Object[]{id, name};
                send(new Request(TAG, objects));
                debug("connected");
            }
        });
        onReceiveHandler.add(new OnReceive("Chat") {
            @Override
            public void doUponReceipt(Object input) {
                System.out.println(input);
            }
        });
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
            ip = ip.substring(1);

            debug("ip: " + ip);
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
        onReceiveHandler.onReceive(answer.TAG(), answer.answer());
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

    public void ping() {
        timestamp = System.currentTimeMillis();
        send(new Request("Ping", null));
    }

    public boolean isOn() {
        return on;
    }

    public UUID getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public String getName() {
        return name;
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
