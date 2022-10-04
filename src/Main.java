import Server.Server;
import Utility.Helper;

import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        testing();
    }

    public static void testing(){
        for (int i = 0; i < 100; i++) {
            //System.out.println(UUID.randomUUID());
        }

        //System.out.println(Helper.getPublicIPv4());

        //System.out.println(Helper.getPublicIPv6());

        Server server = new Server(true,25565, 3, false, false);
    }
}
