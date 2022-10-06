package ServerInternals;

import DataInternals.Answer;
import DataInternals.Data;
import DataInternals.DataHandler;
import DataInternals.Request;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;

public final class ClientThread {
    private boolean DEBUG;
    private final Socket socket;
    public final UUID id;
    public final int RETRY_COUNTER = 5;
    public volatile Request request;

    public final DataHandler dataHandler;

    private volatile boolean connected, handling = false;

    public volatile String name;

    public volatile long ping;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            debug("start loop");
            while (connected && socket.isConnected()) {
                if (handling) {
                    handle();
                } else {
                    receive();
                }
            }
            debug("end of loop");
        }
    });

    public ClientThread(boolean DEBUG, Socket socket, UUID id, DataHandler dataHandler) {
        setDEBUG(DEBUG);

        this.socket = socket;
        this.id = id;
        this.dataHandler = dataHandler;

        initDataHandler();

        if (DEBUG) {
            dataHandler.printDataTypes();
        }

        init();
    }

    private void initDataHandler() {
        dataHandler.addDataType(new Data("Connect") {
            @Override
            public void handle(Object input) {
                name = (String) input;
            }
        });
        dataHandler.addDataType(new Data("Ping") {
            @Override
            public void handle(Object input) {
                if(input == null){
                    input = 0;
                }
                long currentTime = System.currentTimeMillis();
                ping = (long) input - currentTime;
                debug("ping: " + ping + "ms");
                send(new Answer(TAG, currentTime));
            }
        });
        dataHandler.addDataType(new Data("ChangeName") {
            @Override
            public void handle(Object input) {
                name = (String) input;
            }
        });
        //TODO
    }

    private void init() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            debug("object stream created");
        } catch (Exception e) {
            close();
            debug("object stream error", e);
        }
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

    private void receive() {
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

    private void handle() {
        dataHandler.handle(request.TAG(), request.request());
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

    public boolean connecting() {
        connected = true;
        send(new Answer("Connect", id));

        debug("initial connect");
        for (int counter = 0; connected && counter < RETRY_COUNTER; counter++) {
            debug("try: " + counter);
            receive();

            if (request != null && request.TAG() instanceof String TAG) {
                if (TAG.equals("Connect")) {
                    handle();
                    return true;
                }
            }

            handling = false;
        }
        return false;
    }

    public UUID reconnecting() {
        send(new Answer("Reconnect", null));

        debug("reconnect");
        for (int counter = 0; connected && counter < RETRY_COUNTER; counter++) {
            debug("try: " + counter);
            receive();

            if (request != null && request.TAG() instanceof String TAG) {
                if (TAG.equals("Reconnect")) {
                    handling = false;
                    return (UUID) request.request();
                }
            }

            handling = false;
        }
        return null;
    }


    //DEBUG
    public void setDEBUG(boolean DEBUG) {
        if (this.DEBUG != DEBUG) {
            this.DEBUG = DEBUG;
            debug("debug: " + DEBUG);
        }
    }

    private void print(String toPrint){
        System.out.println("Client[" + id + "] >> " + toPrint + " <<");
    }

    private void debug(String toPrint) {
        if (DEBUG){
            print(toPrint);
        }
    }

    private void debug(String toPrint, Exception e){
        debug(toPrint);
        if(DEBUG) {
            e.printStackTrace();
        }
    }
}
