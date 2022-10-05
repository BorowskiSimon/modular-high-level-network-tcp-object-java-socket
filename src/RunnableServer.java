import DataTypes.Data;
import DataTypes.Answer;
import DataTypes.DataHandler;
import Server.Server;

import java.util.UUID;

public final class RunnableServer {
    public static void main(String[] args) {
        System.out.println("\n\nStart Runnable Server");

        Server server = new Server(false, 25565, 3, false, false, 1000);

        DataHandler dataHandler = new DataHandler(false);
        /*
        dataHandler.addDataType(new Data(//TODO) {
            @Override
            public void handle(Object input) {
                //TODO
            }
        });
         */

        server.setDataHandler(dataHandler);
        server.start();
    }
}
