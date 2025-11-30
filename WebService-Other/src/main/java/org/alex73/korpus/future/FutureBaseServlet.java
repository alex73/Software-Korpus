package org.alex73.korpus.future;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.alex73.korpus.server.ApplicationOther;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@SuppressWarnings("serial")
public class FutureBaseServlet extends HttpServlet {
    private Configuration cfg;

    protected ApplicationOther getApp() {
        return ApplicationOther.instance;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setServletContextForTemplateLoading(config.getServletContext(), "/WEB-INF/templates/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    protected synchronized void output(String template, Object data, HttpServletResponse resp)
            throws ServletException, IOException {
        Template t = cfg.getTemplate(template);
        Map<String, Object> context = new TreeMap<>();
        context.put("data", data);
        resp.setContentType("text/html; charset=UTF-8");
        try (OutputStreamWriter wr = new OutputStreamWriter(resp.getOutputStream(), "UTF-8")) {
            t.process(context, wr);
        } catch (TemplateException ex) {
            ex.printStackTrace();
            throw new ServletException(ex);
        }
    }
}
