package org.alex73.korpus.server.html;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@SuppressWarnings("serial")
@WebServlet(name="HTML", urlPatterns = { "*.html" })
public class MakeHTML extends HttpServlet {
    private Configuration cfg;

    @Override
    public void init(ServletConfig config) throws ServletException {
        cfg = new Configuration(Configuration.VERSION_2_3_23);
        try {
            cfg.setDirectoryForTemplateLoading(new File(System.getProperty("CONFIG_DIR")));
        } catch (IOException ex) {
            throw new ServletException(ex);
        }
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String templateFile = "templates/" + req.getServletPath();
        Template t = cfg.getTemplate(templateFile);
        Map<String, Object> context = new TreeMap<>();
        resp.setContentType("text/html; charset=UTF-8");
        try (OutputStreamWriter wr = new OutputStreamWriter(resp.getOutputStream(), "UTF-8")) {
            t.process(context, wr);
        } catch (TemplateException ex) {
            ex.printStackTrace();
            throw new ServletException(ex);
        }
    }
}
