package org.alex73.korpus.future;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/naciski" })
public class Naciski extends FutureBaseServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringWriter s = new StringWriter();
        IOUtils.copy(req.getReader(), s);

        resp.setContentType("text/plain");
        resp.getWriter().write(s.toString());
    }
}
