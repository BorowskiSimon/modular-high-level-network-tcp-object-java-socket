package ServerInternals;

import DataInternals.Answer;
import DataInternals.Data;
import DataInternals.DataHandler;
import Utility.Helper;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public final class Server {
    //FINALS
    public final int port, max, CONNECTION_LIMIT_DELAY;
    public final boolean OFFLINE, IPv6;
    private ClientManager clientManager;

    //VARS
    private volatile boolean on = false;
    private boolean DEBUG = false;
    private ServerSocket socket = null;
    private String ipAddress = "localhost";

    private final Thread connectionThread = new Thread(this::connectionLoop);

    public Server(boolean DEBUG, int port, int max, boolean OFFLINE, boolean IPv6, int CONNECTION_LIMIT_DELAY) {
        setDEBUG(DEBUG);

        //FINALS
        this.port = port;
        this.max = max;
        debug("max connections: " + max);
        this.OFFLINE = OFFLINE;
        debug("runs offline", OFFLINE);
        this.CONNECTION_LIMIT_DELAY = CONNECTION_LIMIT_DELAY;
        debug("connection limit delay: " + CONNECTION_LIMIT_DELAY);

        this.IPv6 = IPv6;
        setIPv6();

        init();
    }

    public Server(int port, int max) {
        this(false, port, max, false, false, 1000);
    }

    public void setDataHandler(DataHandler dataHandler) {
        clientManager = new ClientManager(DEBUG, OFFLINE, max, dataHandler);
    }

    private void setIPv6() {
        if (IPv6) {
            System.setProperty("java.net.preferIPv6Addresses", "true");
        }
        debug("ip format: IPv6", IPv6);
        debug("ip format: IPv4", !IPv6);
    }

    private void init() {
        try {
            socket = new ServerSocket(port);
            debug("socket created");

            if (!OFFLINE) {
                if (IPv6) {
                    ipAddress = Helper.getPublicIPv6().getHostAddress();
                } else {
                    ipAddress = Helper.getPublicIPv4().getHostAddress();
                }
            }
            print("ip: " + ipAddress);
            print("port: " + port);
        } catch (Exception e) {
            debug("init error", e);
        }
    }

    public void start() {
        if (clientManager != null) {
            setOn();
            connectionThread.start();
        } else {
            debug("client manager still null. maybe data handler missing");
        }
    }

    private void connectionLoop() {
        debug("starting connection loop");
        while (on && !socket.isClosed()) {
            if (clientManager.getConnectionAmount() < max) {
                try {
                    Socket client = socket.accept();
                    if (!on) {
                        break;
                    }
                    print("client connecting");

                    clientManager.connectionCheck(client);
                } catch (Exception e) {
                    if (!on) {
                        break;
                    }
                    debug("client connection error", e);
                }
            } else {
                debug("client connection limit reached");
                try {
                    Thread.sleep(CONNECTION_LIMIT_DELAY);
                } catch (Exception e) {
                    debug("wait error", e);
                }
            }
        }
        debug("stopped connection loop");
    }

    public void send(Answer answer, UUID id) {
        if (on && !socket.isClosed() && answer != null) {
            clientManager.send(answer, id);
        }
    }

    public void broadcast(Answer answer) {
        if (on && !socket.isClosed() && answer != null) {
            clientManager.broadcast(answer);
        }
    }

    public void close() {
        setOff();
        debug("stopping now");

        clientManager.closeConnections();
        closeConnectionThread();
        closeSocket();
        print("stopped");
    }

    private void closeConnectionThread() {
        if (connectionThread.isAlive()) {
            try {
                connectionThread.join();
                debug("connection thread stopped");
            } catch (Exception e) {
                debug("connection thread stop error", e);
            }
        }
    }

    private void closeSocket() {
        if (!socket.isClosed()) {
            if (socket != null) {
                try {
                    socket.close();
                    debug("socket closed");
                } catch (Exception e) {
                    debug("socket close error", e);
                }
            }
        }
    }

    public boolean isOn() {
        return on;
    }

    private void setOn() {
        this.on = true;
    }

    private void setOff() {
        this.on = false;
    }


    //DEBUG
    public void setDEBUG(boolean DEBUG) {
        if (this.DEBUG != DEBUG) {
            this.DEBUG = DEBUG;
            debug("debug: " + DEBUG);
        }
    }

    private void print(String toPrint) {
        System.out.println("Server >> " + toPrint + " <<");
    }

    private void debug(String toPrint) {
        if (DEBUG) {
            print(toPrint);
        }
    }

    private void debug(String toPrint, boolean condition) {
        if (condition) debug(toPrint);
    }

    private void debug(String toPrint, Exception e) {
        debug(toPrint);
        if (DEBUG) {
            e.printStackTrace();
        }
    }
}
