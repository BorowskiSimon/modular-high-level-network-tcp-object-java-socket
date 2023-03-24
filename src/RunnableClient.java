import ClientInternals.Client;
import DataInternals.OnReceive;
import DataInternals.Request;

import java.util.Scanner;

public final class RunnableClient {
    public static void main(String[] args) {
        String ip = "localhost";
        String name = "Default Mum";
        if (args.length > 0) {
            ip = args[0];
        }
        if (args.length > 1) {
            name = args[1];
        }


        System.out.println("\n\nStart Runnable Client");


        Client client = new Client(false, name, ip, 25565, false);
        client.addOnReceive(new OnReceive("Chat") {
            @Override
            public void doUponReceipt(Object input) {
                System.out.println(input);
            }
        });
        client.start();


        Scanner scanner = new Scanner(System.in);
        String input;
        while(client.isOn()) {
            input = scanner.nextLine();
            if (input.equals("exit")) {
                break;
            } else if (input.equals("ping")) {
                client.ping();
            } else {
                client.send(new Request("Chat", client.getName() + ": " + input));
            }
        }


        client.stop();
    }
}
