import ClientInternals.Client;
import DataInternals.Data;
import DataInternals.DataHandler;
import DataInternals.Request;

import java.nio.charset.StandardCharsets;
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


        DataHandler dataHandler = new DataHandler(false);
        dataHandler.addData(new Data("Chat") {
            @Override
            public void handle(Object input) {
                System.out.println(input);
            }
        });


        client.setDataHandler(dataHandler);
        client.start();


        Scanner scanner = new Scanner(System.in);
        String input;
        do {
            input = scanner.nextLine();
            if (input.equals("exit")) {
                break;
            } else if (input.equals("ping")) {
                client.ping();
            } else {
                client.send(new Request("Chat", client.name + ": " + input));
            }
        } while (client.isOn());


        client.stop();
    }
}
