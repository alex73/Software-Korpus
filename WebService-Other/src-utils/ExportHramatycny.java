import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.grammardb.FormsReadyFilter;
import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.future.Hramatycny.Out;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

public class ExportHramatycny {

    public static void main(String[] args) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setDirectoryForTemplateLoading(new File("war/WEB-INF/templates/future/"));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        GrammarDB2 db = GrammarDB2.initializeFromDir("/data/gits/GrammarDB");

        List<Out> data = Collections.synchronizedList(new ArrayList<>());
        db.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> forms = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
                if (forms == null || forms.isEmpty()) {
                    continue;
                }
                data.add(new Out(p, v, forms));
            }
        });
        Collections.sort(data);

        Template t = cfg.getTemplate("hramatycny.html");
        Map<String, Object> context = new TreeMap<>();
        context.put("data", data);
        try (OutputStreamWriter wr = new OutputStreamWriter(new FileOutputStream("/tmp/hram.html"), "UTF-8")) {
            t.process(context, wr);
        }

        try (BufferedReader rd = Files.newBufferedReader(Paths.get("/tmp/hram.html"))) {
            try (BufferedWriter wr = Files.newBufferedWriter(Paths.get("/tmp/hram.txt"))) {
                String s;
                while ((s = rd.readLine()) != null) {
                    s = s.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
                    if (!s.isEmpty()) {
                        wr.write(s);
                        wr.write("\n");
                    }
                }
            }
        }
    }
}
