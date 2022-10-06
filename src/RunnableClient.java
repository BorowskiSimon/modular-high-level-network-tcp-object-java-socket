import ClientInternals.Client;
import DataInternals.Data;
import DataInternals.DataHandler;
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

        DataHandler dataHandler = new DataHandler(false);
        dataHandler.addDataType(new Data("Chat") {
            @Override
            public void handle(Object input) {
                System.out.println(input);
            }
        });
        /*
        dataHandler.addDataType(new Data(//TODO) {
            @Override
            public void handle(Object input) {
                //TODO
                // example:
                // System.out.println("Test Print: " + input);
            }
        });
         */

        Client client = new Client(false, name, ip, 25565, false);

        /*
        dataHandler.addDataType(new Data(//TODO) {
            @Override
            public void handle(Object input) {
                //TODO
                // example:
                // client.close();
            }
        });
         */

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
                System.out.println(client.getPing());
            } else {
                client.send(new Request("Chat", client.name + ": " + input));
            }
        } while (client.isOn());


        client.stop();
    }
}
