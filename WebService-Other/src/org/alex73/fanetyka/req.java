package org.alex73.fanetyka;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

public class req {

    public static void main(String[] args) throws Exception {

        URL url = new URL("https://texttospeech.googleapis.com/v1beta1/text:synthesize?key=AIzaSyD60s7duQa4gIdGBO_Gqp1SKZ1NCcoyBWo");
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);

        byte[] out = Files.readAllBytes(Paths.get("/home/alex/gits/1"));
        int length = out.length;

        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(out);
        }
        int code = http.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            System.err.println("Error " + code + ": " + http.getResponseMessage());
        }
        if (http.getResponseCode() == 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String strCurrentLine;
            while ((strCurrentLine = br.readLine()) != null) {
                System.out.println(strCurrentLine);
            }
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(http.getErrorStream()));
            String strCurrentLine;
            while ((strCurrentLine = br.readLine()) != null) {
                System.out.println(strCurrentLine);
            }
        }
    }
}
