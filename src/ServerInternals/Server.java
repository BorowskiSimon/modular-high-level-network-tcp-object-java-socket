package ServerInternals;

import DataInternals.Answer;
import DataInternals.OnReceive;
import DataInternals.OnReceiveHandler;
import Utility.Helper;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public final class Server {
    private boolean DEBUG = false;
    private ServerSocket socket = null;
    public final int port;
    public final int max;
    public final int DELAY_IF_LIMIT_REACHED;
    public final boolean OFFLINE;
    public final boolean IPv6;
    private final ClientManager clientManager;
    private volatile boolean on = false;
    private String ipAddress = "localhost";

    private final Thread connectionThread = new Thread(this::connectionLoop);

    public Server(boolean DEBUG, int port, int max, boolean OFFLINE, boolean IPv6, int DELAY_IF_LIMIT_REACHED) {
        setDEBUG(DEBUG);

        //FINALS
        this.port = port;
        this.max = max;
        debug("max connections: " + max);
        this.OFFLINE = OFFLINE;
        debug("runs offline");
        this.DELAY_IF_LIMIT_REACHED = DELAY_IF_LIMIT_REACHED;
        debug("connection limit delay: " + DELAY_IF_LIMIT_REACHED);

        this.IPv6 = IPv6;
        setIPv6();

        init();

        clientManager = new ClientManager(DEBUG, OFFLINE, max);
    }

    public Server(int port, int max) {
        this(false, port, max, false, false, 1000);
    }

    public void addOnReceive(OnReceive onReceive) {
        clientManager.addOnReceive(onReceive);
    }

    private void setIPv6() {
        if (!IPv6) {
            debug("ip format: IPv4");
            return;
        }
        System.setProperty("java.net.preferIPv6Addresses", "true");
        debug("ip format: IPv6");
    }

    private void init() {
        try {
            socket = new ServerSocket(port);
            debug("socket created");
        } catch (Exception e) {
            debug("socket init error", e);
        }

        try {
            ipAddress = initIP();

            print("ip: " + ipAddress);
            print("port: " + port);
        } catch (Exception e) {
            debug("ip init error", e);
        }
    }

    private String initIP() throws Exception {
        if (!OFFLINE) {
            if (IPv6) {
                return Helper.getPublicIPv6().getHostAddress();
            } else {
                return Helper.getPublicIPv4().getHostAddress();
            }
        }
        return String.valueOf(socket.getInetAddress().getHostAddress());
    }

    public void start() {
        if (socket == null) {
            debug("socket still null. maybe server not online");
            return;
        }
        if (clientManager == null) {
            debug("client manager still null. maybe data handler missing");
            return;
        }
        setOn();
        connectionThread.start();
    }

    private void connectionLoop() {
        debug("starting connection loop");
        while (on && !socket.isClosed()) {
            if (clientManager.getConnectionAmount() < max) {
                try {
                    debug("waiting for new client");
                    Socket client = socket.accept();
                    if (!on) break;
                    print("client connecting");

                    clientManager.connectionCheck(client);
                } catch (Exception e) {
                    if (!on) break;
                    debug("client connection error", e);
                }
            } else {
                debug("client connection limit reached");
                try {
                    Thread.sleep(DELAY_IF_LIMIT_REACHED);
                } catch (Exception e) {
                    debug("wait error", e);
                }
            }
        }
        debug("stopped connection loop");
    }

    public void send(Answer answer, UUID id) {
        if (!on || socket.isClosed() || answer == null) return;
        clientManager.send(answer, id);
    }

    public void broadcast(Answer answer) {
        if (!on || socket.isClosed() || answer == null) return;
        clientManager.broadcast(answer);
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
        if (!connectionThread.isAlive()) return;
        try {
            connectionThread.join();
            debug("connection thread stopped");
        } catch (Exception e) {
            debug("connection thread stop error", e);
        }
    }

    private void closeSocket() {
        if (socket.isClosed()) return;
        if (socket == null) return;
        try {
            socket.close();
            debug("socket closed");
        } catch (Exception e) {
            debug("socket close error", e);
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
        if (this.DEBUG == DEBUG) return;
        this.DEBUG = DEBUG;
        debug("debug: " + DEBUG);
    }

    private void print(String toPrint) {
        System.out.println("Server >> " + toPrint + " <<");
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
