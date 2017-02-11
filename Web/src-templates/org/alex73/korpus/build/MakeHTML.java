package org.alex73.korpus.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

public class MakeHTML {

    static Configuration cfg;

    public static void main(String[] args) throws Exception {
        cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setDirectoryForTemplateLoading(new File("src-templates/org/alex73/korpus/build/"));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        out("index.fm", "web-angular/src/index.html");
        out("korpus.fm", "web-angular/src/korpus.html");
        out("grammar.fm", "web-angular/src/grammar.html");
        out("download.fm", "web-angular/src/download.html");
    }

    static void out(String templateFile, String outFile) throws Exception {
        Template t = cfg.getTemplate(templateFile);
        Map<String, Object> context = new TreeMap<>();
        try (OutputStreamWriter wr = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8")) {
            t.process(context, wr);
        }
    }
}
