import DataInternals.Answer;
import DataInternals.Data;
import DataInternals.DataHandler;
import ServerInternals.Server;

public final class RunnableServer {
    public static void main(String[] args) {
        System.out.println("\n\nStart Runnable Server");
        Server server = new Server(false, 25565, 3, false, false, 1000);


        DataHandler dataHandler = new DataHandler(false);
        dataHandler.addData(new Data("Chat") {
            @Override
            public void handle(Object input) {
                server.broadcast(new Answer("Chat", input));
            }
        });


        server.setDataHandler(dataHandler);
        server.start();
    }
}
