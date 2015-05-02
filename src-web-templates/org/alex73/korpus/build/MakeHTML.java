package org.alex73.korpus.build;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.URLResourceLoader;

public class MakeHTML {
    static Template template;

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.setProperty("resource.loader", "class");
        props.setProperty("class.resource.loader.description", "Velocity Classpath Resource Loader");
        props.setProperty("class.resource.loader.class", URLResourceLoader.class.getName());
        props.setProperty("class.resource.loader.root", MakeHTML.class.getResource("").toExternalForm());
        Velocity.init(props);

        template = Velocity.getTemplate("search.velocity", "UTF-8");
        out("search", "korpus", "war/korpusSearch.html");
        out("search", "other", "war/otherSearch.html");

        out("conco", "korpus", "war/korpusConcordance.html");
        out("conco", "other", "war/otherConcordance.html");

        out("cluster", "korpus", "war/korpusCluster.html");
        out("cluster", "other", "war/otherCluster.html");
    }

    static void out(String mode, String db, String outFile) throws Exception {
        VelocityContext context = new VelocityContext();
        context.put("mode", mode);
        context.put("db", db);
        try (OutputStreamWriter wr = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8")) {
            template.merge(context, wr);
        }
    }
}
