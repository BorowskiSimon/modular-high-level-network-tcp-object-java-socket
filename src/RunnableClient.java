import Client.Client;
import DataTypes.Data;
import DataTypes.DataHandler;
import DataTypes.Request;

import java.util.Scanner;

public final class RunnableClient {
    public static void main(String[] args) {
        System.out.println("\n\nStart Runnable Client");

        DataHandler dataHandler = new DataHandler(false);
        /*
        dataHandler.addDataType(new Data(//TODO) {
            @Override
            public void handle(Object input) {
                //TODO
            }
        });
         */

        Client client = new Client(true, "**Deine** Mum", "localhost", 25565, false);
        client.setDataHandler(dataHandler);
        client.start();


        Scanner scanner = new Scanner(System.in);
        String input;
        do {
            input = scanner.next();
            if(input.equals("exit")){
                break;
            }
            client.send(new Request("Chat", client.name + ": " + input));
        } while (true);

        client.close();
    }
}
