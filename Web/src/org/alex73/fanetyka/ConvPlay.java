package org.alex73.fanetyka;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/fanplay")
public class ConvPlay extends HttpServlet {
    private final String apiKey;
    static final Pattern RE_AUDIO_CONTENT = Pattern.compile("\"audioContent\": \"([^\"]+)\"");

    public ConvPlay() {
        apiKey = System.getProperty("GOOGLE_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("GOOGLE_API_KEY was not defined");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String text;
            try (BufferedReader reader = req.getReader()) {
                text = IOUtils.toString(reader);
            }

            String template;
            try (InputStream in = this.getClass().getResourceAsStream("req.template")) {
                template = IOUtils.toString(in);
            }

            text = "<speak><phoneme alphabet=\"ipa\" ph=\"" + text + "\">невядомыя фанемы</phoneme></speak>";
            System.out.println("text: "+text);

            byte[] data = json(template, text).getBytes();
            System.out.println("data: "+new String(data));

            URL url = new URL("https://texttospeech.googleapis.com/v1beta1/text:synthesize?key=" + apiKey);
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST"); // PUT is another valid option
            http.setDoOutput(true);

            http.setFixedLengthStreamingMode(data.length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.connect();
            try (OutputStream os = http.getOutputStream()) {
                os.write(data);
            }
            int code = http.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                System.err.println("Error " + code + ": " + http.getResponseMessage());
            }
            if (http.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(http.getInputStream()));
                String strCurrentLine;
                String out = null;
                while ((strCurrentLine = br.readLine()) != null) {
                    Matcher m = RE_AUDIO_CONTENT.matcher(strCurrentLine);
                    if (m.find()) {
                        out = m.group(1);
                    }
                    System.out.println(strCurrentLine);
                }
                resp.setContentType("text/plain");
                resp.getOutputStream().write(out.getBytes());
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(http.getErrorStream()));
                String strCurrentLine;
                while ((strCurrentLine = br.readLine()) != null) {
                    System.out.println(strCurrentLine);
                }
            }
        } catch (Exception ex) {
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getOutputStream().write(("Памылка: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    static String json(String template, String ssml) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map v = objectMapper.readValue(template, Map.class);
        ((Map)v.get("input")).put("ssml", ssml);
        return objectMapper.writeValueAsString(v);
    }
}
