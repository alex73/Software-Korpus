package org.alex73.korpus.server;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("rest")
public class ApplicationWeb extends Application {
    public Map<String, Map<String, String>> localization;
    public ResourceBundle messagesEn, messagesBe;

    public static ApplicationWeb instance;

    public ApplicationWeb() {
        instance = this;

        System.out.println("Starting...");
        try {
            messagesBe = ResourceBundle.getBundle("messages", new Locale("be"));
            messagesEn = ResourceBundle.getBundle("messages", new Locale("en"));

            localization = new TreeMap<>();
            prepareLocalization("be", messagesBe);
            prepareLocalization("en", messagesEn);

            System.out.println("Initialization finished");
        } catch (Throwable ex) {
            System.err.println("Startup error");
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    void prepareLocalization(String lang, ResourceBundle messages) {
        Map<String, String> lines = new HashMap<>();
        messages.getKeys().asIterator().forEachRemaining(key -> lines.put(key, messages.getString(key)));
        localization.put(lang, lines);
    }
}
