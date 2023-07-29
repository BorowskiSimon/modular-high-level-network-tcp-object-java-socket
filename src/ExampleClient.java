import ClientInternals.Client;
import DataInternals.OnReceive;
import DataInternals.Request;

import java.util.Scanner;

public final class ExampleClient {
    public static void main(String[] args) {
        String serverIP = "localhost";
        String clientName = "Default Mum";
        if (args.length > 0) {
            serverIP = args[0];
        }
        if (args.length > 1) {
            clientName = args[1];
        }


        System.out.println("\n\nStart Example Client");


        Client client = new Client(false, clientName, serverIP, 25565, false);
        client.addOnReceive(new OnReceive("Chat") {
            @Override
            public void doUponReceipt(Object input) {
                System.out.println(input);
            }
        });
        client.start();


        Scanner scanner = new Scanner(System.in);
        String input;
        label:
        while (client.isOn()) {
            input = scanner.nextLine();
            switch (input) {
                case "exit":
                    break label;
                case "ping":
                    client.ping();
                    break;
                case "list":
                    client.send(new Request("ListClients", null));
                    break;
                default:
                    client.send(new Request("Chat", client.getClientName() + ": " + input));
                    break;
            }
        }


        client.stop();
    }
}
