package Utility;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;

public final class PublicIP {
    public static final String IP_V4_WEBSITE = "https://4.ident.me";
    public static final String IP_V6_WEBSITE = "https://6.ident.me";

    public static Inet4Address getV4() throws IOException {
        return (Inet4Address) Inet4Address.getByName(WebsiteToString.extract(IP_V4_WEBSITE));
    }

    public static Inet6Address getV6() throws IOException {
        return (Inet6Address) Inet6Address.getByName(WebsiteToString.extract(IP_V6_WEBSITE));
    }
}
