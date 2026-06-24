package org.alex73.korpus.future;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.alex73.korpus.server.ApplicationOther;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class FutureBaseServlet implements Handler {
    private final String templatePath;
    private Configuration cfg;

    public FutureBaseServlet(String templatePath) {
        this.templatePath = templatePath;
        try {
            cfg = new Configuration(Configuration.VERSION_2_3_30);
            List<TemplateLoader> loadersList = new ArrayList<>();

            // Dev templates fallback
            File devTemplates = new File("src/main/webapp/WEB-INF/templates");
            if (!devTemplates.exists()) {
                devTemplates = new File("WebService-Other/src/main/webapp/WEB-INF/templates");
            }
            if (devTemplates.exists()) {
                loadersList.add(new FileTemplateLoader(devTemplates));
            }

            // Classpath fallback
            loadersList.add(new ClassTemplateLoader(FutureBaseServlet.class, "/WEB-INF/templates"));

            cfg.setTemplateLoader(new MultiTemplateLoader(loadersList.toArray(new TemplateLoader[0])));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    abstract protected Object process(Map<String, String> pathParams) throws Exception;

    @Override
    public void handle(Context ctx) throws Exception {
        var data = process(ctx.pathParamMap());

        Template t = cfg.getTemplate(templatePath);
        Map<String, Object> context = new TreeMap<>();
        context.put("data", data);
        ctx.contentType("text/html; charset=UTF-8");
        try (StringWriter wr = new StringWriter()) {
            t.process(context, wr);
            ctx.result(wr.toString());
        } catch (TemplateException ex) {
            ex.printStackTrace();
            ctx.status(500).result("Error processing request");
        }
    }
}
