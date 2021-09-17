package org.alex73.korpus.future;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/freq/*" })
public class Freq extends FutureBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int count = Integer.parseInt(req.getPathInfo().substring(1));

        List<String> frequences = Files.readAllLines(Paths.get(getApp().korpusCache + "/stat.formsfreq.tab"));
        frequences = frequences.subList(0, Math.min(frequences.size(), count));
        List<Pair> data = frequences.stream().map(line -> new Pair(line)).collect(Collectors.toList());

        output("future/freq.html", data, resp);
    }

    public static class Pair {
        private final String word;
        private final int count;

        public Pair(String line) {
            int p = line.indexOf('=');
            word = line.substring(0, p);
            count = Integer.parseInt(line.substring(p + 1));
        }

        public String getWord() {
            return word;
        }

        public int getCount() {
            return count;
        }
    }
}
