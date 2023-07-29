import DataInternals.Answer;
import DataInternals.OnReceive;
import ServerInternals.Server;

import java.util.UUID;

public final class ExampleServer {
    public static void main(String[] args) {
        System.out.println("\n\nStart Example Server");


        Server server = new Server(false, 25565, 3, false, false, 1000);
        server.addOnReceive(new OnReceive("Chat") {
            @Override
            public void doUponReceipt(Object input) {
                server.broadcast(new Answer("Chat", input));
            }
        });
        server.addOnReceive(new OnReceive("ListClients") {
            @Override
            public void doUponReceipt(Object input) {
                StringBuilder clientIDNameList = new StringBuilder();

                for (UUID clientID : server.getConnectedClientList()) {
                    clientIDNameList
                            .append(server.getClientName(clientID))
                            .append("[")
                            .append(clientID)
                            .append("]")
                            .append(", ");
                }
                clientIDNameList.deleteCharAt(clientIDNameList.lastIndexOf(","));
                clientIDNameList.deleteCharAt(clientIDNameList.lastIndexOf(" "));

                server.broadcast(new Answer("Chat", clientIDNameList.toString()));

                server.send(new Answer("Chat", "Your ID: is " + getClientID()), getClientID());
            }
        });


        System.out.println("Server IP: " + server.getIpAddress());


        server.start();


        try {
            System.out.println("SERVER WILL CLOSE AUTOMATICALLY AFTER 1 HOUR!");
            Thread.sleep(3600000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        server.close();
    }
}
