import Utility.Helper;

import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(UUID.randomUUID());
        }


        try {
            System.out.println(Helper.getPublicIPv4());
            System.out.println(Helper.getPublicIPv6());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
