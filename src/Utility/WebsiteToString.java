package Utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class WebsiteToString {
    public static String extract(String websiteHttpsURL) throws IOException {
        URL url = new URL(websiteHttpsURL);

        InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        //readLine is not modular. could be done but not worth
        String websiteContent = bufferedReader.readLine();

        bufferedReader.close();
        inputStreamReader.close();

        return websiteContent;
    }
}
