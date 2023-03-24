package ServerInternals;

import DataInternals.Answer;
import DataInternals.OnReceive;
import DataInternals.OnReceiveHandler;
import DataInternals.Request;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;

final class ClientThread {
    private boolean DEBUG = false;
    private boolean waiting = false;
    private final Socket socket;
    public final UUID id;
    public volatile Request request;

    public final OnReceiveHandler onReceiveHandler;

    private volatile boolean connected;
    private volatile boolean handling = false;

    public volatile String name;

    public volatile long ping = 0;
    private volatile long timestamp;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            debug("start loop");
            while (connected && socket.isConnected()) {
                if (handling) {
                    onReceive();
                } else {
                    read();
                }
            }
            debug("end of loop");
        }
    });

    public ClientThread(boolean DEBUG, Socket socket, UUID id, OnReceiveHandler onReceiveHandler) {
        setDEBUG(DEBUG);

        this.socket = socket;
        this.id = id;
        this.onReceiveHandler = onReceiveHandler;

        init();

        debug(onReceiveHandler.toString());

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            debug("object stream created");
        } catch (Exception e) {
            close();
            debug("object stream error", e);
        }

        connected = true;
    }

    private void init() {
        onReceiveHandler.add(new OnReceive("Ping") {
            @Override
            public void doUponReceipt(Object input) {
                System.out.println("currently in: " + id + " (data handler: " + onReceiveHandler + ")");
                send(new Answer(TAG, null));
            }
        });
        onReceiveHandler.add(new OnReceive("PingTask") {
            @Override
            public void doUponReceipt(Object input) {
                ping = System.currentTimeMillis() - timestamp;
                debug("ping: " + ping + "ms");
            }
        });
        onReceiveHandler.add(new OnReceive("ChangeName") {
            @Override
            public void doUponReceipt(Object input) {
                name = (String) input;
            }
        });
    }

    public void addOnReceive(OnReceive onReceive) {
        onReceiveHandler.add(onReceive);
    }

    public void start() {
        thread.start();
    }

    public void send(Answer answer) {
        try {
            out.writeObject(answer);
            out.flush();

            print("sent: " + answer);
        } catch (Exception e) {
            debug("send error", e);
        }
    }

    private void read() {
        try {
            Object tmp = in.readObject();
            if (tmp instanceof Request request) {
                this.request = request;
            }
            handling = true;

            print("read: " + request);
        } catch (SocketException e) {
            debug("reading socket error", e);
            close();
        } catch (Exception e) {
            close();
            debug("read error", e);
        }
    }

    private void onReceive() {
        onReceiveHandler.onReceive(request.TAG(), request.request());
        handling = false;
    }

    public void close() {
        connected = false;
        debug("disconnecting");
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
    }

    public boolean isConnected() {
        return connected;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public void pingTask() {
        timestamp = System.currentTimeMillis();
        send(new Answer("PingTask", null));
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
