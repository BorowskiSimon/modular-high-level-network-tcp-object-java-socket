package Server;

import Utility.Helper;

import java.net.ServerSocket;

public class Server {
    public final int port, max;
    public final boolean OFFLINE, IPv6;
    private int connections = 0;
    private boolean on = false, DEBUG = false;
    private ServerSocket socket = null;
    private String ipAddress = "localhost";

    public Server(boolean DEBUG, int port, int max, boolean OFFLINE, boolean IPv6) {
        setDEBUG(DEBUG);

        //FINALS
        this.port = port;
        debug("port: " + port);
        this.max = max;
        debug("max connections: " + max);
        this.OFFLINE = OFFLINE;
        debug("runs offline", OFFLINE);
        this.IPv6 = IPv6;
        setIPv6();

        init();
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
            debug("ip: " + ipAddress);
        } catch (Exception e) {
            debug("init error");
            e.printStackTrace();
        }
    }

    public void start() {
        setOn(true);

        try {
            //TODO
        } catch (Exception e){
            debug("start error");
            e.printStackTrace();
        } finally {
            close();
        }

        debug("Server started.");
    }

    private void close() {
        if(!socket.isClosed()){
            cutConnections();

            if(socket != null){
                try {
                    socket.close();
                    debug("socket closed");
                } catch (Exception e) {
                    debug("socket close error");
                    e.printStackTrace();
                }
            }
        }
    }

    private void cutConnections() {
        //TODO
    }

    public boolean isOn() {
        return on;
    }

    protected void setOn(boolean on) {
        this.on = on;
    }


    //DEBUG
    public void setDEBUG(boolean DEBUG) {
        if (this.DEBUG != DEBUG) {
            this.DEBUG = DEBUG;
            debug("debug: " + DEBUG);
        }
    }

    private void debug(String toPrint) {
        if (DEBUG) System.out.println("Server >> " + toPrint + " <<");
    }

    private void debug(String toPrint, boolean condition) {
        if (condition) debug(toPrint);
    }
}
