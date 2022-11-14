package Utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URL;

public final class Helper {
    private static String getPublicIP(String urlWithPlainIP) throws Exception {
        String publicAddress = "localhost";

        URL url = null;
        url = new URL(urlWithPlainIP);

        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(url.openStream()));
        publicAddress = reader.readLine();
        reader.close();

        return publicAddress;
    }

    public static Inet4Address getPublicIPv4() throws Exception {
        return (Inet4Address) Inet4Address.getByName(getPublicIP("https://4.ident.me"));
    }

    public static Inet6Address getPublicIPv6() throws Exception {
        return (Inet6Address) Inet6Address.getByName(getPublicIP("https://6.ident.me"));
    }
}
