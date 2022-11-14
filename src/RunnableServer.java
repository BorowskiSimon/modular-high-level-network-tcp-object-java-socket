import DataInternals.Answer;
import DataInternals.OnReceive;
import ServerInternals.Server;

public final class RunnableServer {
    public static void main(String[] args) {
        System.out.println("\n\nStart Runnable Server");
        Server server = new Server(false, 25565, 3, false, false, 1000);
        server.addOnReceive(new OnReceive("Chat") {
            @Override
            public void doUponReceipt(Object input) {
                server.broadcast(new Answer("Chat", input));
            }
        });
        server.start();
    }
}
