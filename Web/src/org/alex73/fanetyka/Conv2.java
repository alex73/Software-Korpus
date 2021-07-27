package org.alex73.fanetyka;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alex73.fanetyka.impl.FanetykaText;
import org.alex73.korpus.server.KorpusApplication;
import org.apache.commons.io.IOUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/fan")
public class Conv2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String text;
            try (BufferedReader reader = req.getReader()) {
                text = IOUtils.toString(reader);
            }

            FanetykaText f = new FanetykaText(KorpusApplication.instance.grFinder, text.replace('+', '´').replaceAll("[-‒‒–]", "-"));

            resp.setContentType("text/html; charset=UTF-8");
            String o ="<div>Вынікі канвертавання (IPA):</div><div style='font-size: 150%'>"+ f.ipa.replace("\n", "<br/>") + "</div><br/><div>Школьная транскрпцыя:</div><div style='font-size: 150%'>" + f.skola.replace("\n", "<br/>")+"</div>";
            resp.getOutputStream().write(o.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getOutputStream().write(("Памылка: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}
