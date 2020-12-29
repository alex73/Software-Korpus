package org.alex73.spellchecker;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;
import org.languagetool.tools.RuleMatchAsXmlSerializer;

@WebServlet(name = "GrammarChecker", urlPatterns = { "/check" })
public class GrammarCheckServlet extends javax.servlet.http.HttpServlet {
    static final Charset UTF8 = Charset.forName("UTF-8");

    Language langOfficial;
    JLanguageTool ltOfficial;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            Languages.getOrAddLanguageByClassName(Belarusian.class.getName());
            ltOfficial = new JLanguageTool(new Belarusian());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String[] params = IOUtils.toString(req.getInputStream(), UTF8).split("&");
            String text = null;

            for (String p : params) {
                if (p.startsWith("text=")) {
                    text = URLDecoder.decode(p.substring(5), "UTF-8");
                }
            }
            process(text, resp);
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.sendError(500);
        }
    }

    void process(String text, HttpServletResponse resp) throws IOException {
        if (text == null) {
            System.err.println("There is no text");
        }
        List<RuleMatch> matches = ltOfficial.check(text);
        RuleAsXmlSerializer ser = new RuleAsXmlSerializer();
        String xml = ser.ruleMatchesToXml(matches, text, 40, langOfficial);

        resp.setContentType("text/xml; charset=UTf-8");
        byte[] data = xml.getBytes(UTF8);
        resp.setContentLength(data.length);
        resp.getOutputStream().write(data);
    }
}
