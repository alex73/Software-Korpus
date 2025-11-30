package org.alex73.korpus.server.html;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alex73.korpus.server.ApplicationWeb;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;

@SuppressWarnings("serial")
@WebServlet(name = "HTML", urlPatterns = { "*.html" })
public class MakeHTML extends HttpServlet {
    private Configuration cfg;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            InitialContext context = new InitialContext();
            Context xmlNode = (Context) context.lookup("java:comp/env");
            String paths = (String) xmlNode.lookup("TEMPLATE_PATHS");
            String[] ps = paths.split(":");
            TemplateLoader[] loaders = new TemplateLoader[ps.length];
            for (int i = 0; i < ps.length; i++) {
                loaders[i] = new WebappTemplateLoader(getServletContext(), ps[i]);
            }

            cfg = new Configuration(Configuration.VERSION_2_3_23);
            cfg.setTemplateLoader(new MultiTemplateLoader(loaders));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String path = req.getServletPath();

            String templatePath = path.replaceAll("\\..+$", "") + "-base.html";
            Map<String, Object> context = new TreeMap<>();
            Template t = cfg.getTemplate(templatePath);
            if (path.endsWith(".txt")) {
                resp.setContentType("text/plain; charset=UTF-8");
            } else {
                String lang;
                ResourceBundle messages;
                if (path.endsWith(".en.html")) {
                    lang = "en";
                    messages = ApplicationWeb.instance.messagesEn;
                } else {
                    lang = "be";
                    messages = ApplicationWeb.instance.messagesBe;
                }
                context.put("lang", lang);
                context.put("messages", messages);
                resp.setContentType("text/html; charset=UTF-8");
            }
            try (OutputStreamWriter wr = new OutputStreamWriter(resp.getOutputStream(), "UTF-8")) {
                t.process(context, wr);
            } catch (TemplateException ex) {
                ex.printStackTrace();
                throw new ServletException(ex);
            }
        } catch (TemplateNotFoundException ex) {
            resp.sendError(404);
        }
    }
}
