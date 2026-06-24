package org.alex73.korpus.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.*;
import io.javalin.config.JavalinConfig;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

import java.io.StringWriter;
import java.util.*;

public class ApplicationWeb {

    public Map<String, Map<String, String>> localization;
    public ResourceBundle messagesEn, messagesBe;

    public static ApplicationWeb instance;
    private Configuration cfg;

    public ApplicationWeb() throws Exception {
        instance = this;

        messagesBe = ResourceBundle.getBundle("messages", Locale.of("be"));
        messagesEn = ResourceBundle.getBundle("messages", Locale.of("en"));

        localization = new TreeMap<>();
        prepareLocalization("be", messagesBe);
        prepareLocalization("en", messagesEn);

        initFreemarker();
    }

    void prepareLocalization(String lang, ResourceBundle messages) {
        Map<String, String> lines = new HashMap<>();
        messages.getKeys().asIterator().forEachRemaining(key -> lines.put(key, messages.getString(key)));
        localization.put(lang, lines);
    }

    private void initFreemarker() throws Exception {
        cfg = new Configuration(Configuration.VERSION_2_3_23);
        List<TemplateLoader> loadersList = List.of(new ClassTemplateLoader(ApplicationWeb.class, "/templates"));
        cfg.setTemplateLoader(new MultiTemplateLoader(loadersList.toArray(new TemplateLoader[0])));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public void registerRoutes(JavalinConfig config) {
        RoutesConfig routes = config.routes;
        config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
            mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        }));

        // Server static files from resources
        config.staticFiles.add(staticFiles -> {
            staticFiles.directory = "/public";
            staticFiles.location = Location.CLASSPATH;
        });

        // Core routes mapping
        routes.get("/rest/localization", ctx -> {
            ctx.json(localization);
        });

        Handler htmlHandler = ctx -> {
            String path = ctx.path();
            if ("/".equals(path)) {
                path = "/index.html";
            }
            try {
                String templatePath = path.replaceAll("\\..+$", "") + "-base.html";
                if (templatePath.startsWith("/")) {
                    templatePath = templatePath.substring(1);
                }

                Map<String, Object> context = new TreeMap<>();
                Template t = cfg.getTemplate(templatePath);

                String lang;
                ResourceBundle messages;
                if (path.endsWith(".en.html")) {
                    lang = "en";
                    messages = messagesEn;
                } else {
                    lang = "be";
                    messages = messagesBe;
                }
                context.put("lang", lang);
                context.put("messages", messages);
                ctx.contentType("text/html; charset=UTF-8");

                StringWriter wr = new StringWriter();
                t.process(context, wr);
                ctx.result(wr.toString());
            } catch (TemplateNotFoundException ex) {
                ctx.status(404);
            } catch (TemplateException ex) {
                ex.printStackTrace();
                ctx.status(500).result(ex.getMessage());
            }
        };
        routes.get("/", htmlHandler);
        routes.get("/*.html", htmlHandler);
    }
}
