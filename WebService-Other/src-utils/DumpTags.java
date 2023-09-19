import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.alex73.grammardb.tags.TagLetter;
import org.alex73.korpus.languages.DBTagsFactory;
import org.alex73.korpus.languages.DBTagsFactory.DBTagsGroup;
import org.alex73.korpus.languages.LanguageFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

public class DumpTags {

    public static void main(String[] args) throws Exception {
        TagLetter root = LanguageFactory.get("bel").getTags().getRoot();

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setDirectoryForTemplateLoading(new File("src-utils"));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        Template t = cfg.getTemplate("DumpTags.template");
        Map<String, Object> context = new TreeMap<>();
        try (OutputStreamWriter wr = new OutputStreamWriter(new FileOutputStream("/tmp/tags.html"), "UTF-8")) {
            for (TagLetter.OneLetterInfo li : root.letters) {
                Table table = new Table();
                DBTagsGroup groups = LanguageFactory.get("bel").getDbTags().getTagGroupsByWordType().get(li.letter);
                for (DBTagsFactory.Group g : groups.groups) {
                    table.columns.add(g.name);
                }
                Block b = new Block(li);
                table.rows.add(new Row(table.columns.size()));
                b.apply(table);
                context.put("table", table);
                context.put("title", li.letter+": "+li.description);
                t.process(context, wr);
            }
        }
    }

    static class Block {
        TagLetter.OneLetterInfo li;
        List<Block> children = new ArrayList<>();

        public Block(TagLetter.OneLetterInfo li) {
            this.li = li;
            for (TagLetter.OneLetterInfo li2 : li.nextLetters.letters) {
                children.add(new Block(li2));
            }
        }

        public int height() {
            if (children.isEmpty()) {
                return 1;
            } else {
                return children.stream().mapToInt(c -> c.height()).sum();
            }
        }

        public void apply(Table t) {
            Row latest = null;
            int columnIndex = -1;
            for (int i = 0; i < children.size(); i++) {
                Block c = children.get(i);
                if (i > 0) {
                    if (children.get(i - 1).childrenString().equals(children.get(i).childrenString())) {
                        // the same children - just add name to previous
                        String prev = latest.getValue(columnIndex);
                        prev += "<br/>" + c.li.letter + ":" + c.li.description;
                        latest.setValue(columnIndex, prev);
                        continue;
                    } else {
                        t.rows.add(new Row(t.columns.size()));
                    }
                }
                latest = t.rows.get(t.rows.size() - 1);
                int rowAfter = t.rows.size();
                columnIndex = t.columns.indexOf(c.li.groupName);
                latest.setValue(columnIndex, c.li.letter + ":" + c.li.description, 0);
                c.apply(t);
                int rowSpan = t.rows.size() - rowAfter+1;
                latest.setRowSpan(columnIndex, rowSpan);
                for (int r = rowAfter; r < t.rows.size(); r++) {
                    t.rows.get(r).setColSpan(columnIndex, 0);
                }
            }
        }

        public String childrenString() {
            return children.stream().map(c -> c.li.letter + ":" + c.li.description + "/" + c.childrenString())
                    .collect(Collectors.joining(";"));
        }
    }

    public static class Table {
        List<String> columns = new ArrayList<>();
        List<Row> rows = new ArrayList<>();

        public List<String> getColumns() {
            return columns;
        }

        public List<Row> getRows() {
            return rows;
        }
    }

    public static class Row {
        public List<Value> columns = new ArrayList<>();

        public Row(int columnsCount) {
            while (columns.size() < columnsCount) {
                columns.add(new Value());
            }
        }

        public String getValue(int columnIndex) {
            return columns.get(columnIndex).v;
        }

        public void setValue(int columnIndex, String value) {
            columns.get(columnIndex).v = value;
        }

        public void setValue(int columnIndex, String value, int rs) {
            columns.get(columnIndex).v = value;
            columns.get(columnIndex).cspan = 1;
            columns.get(columnIndex).rspan = rs;
        }

        public void setColSpan(int columnIndex, int cs) {
            columns.get(columnIndex).cspan = cs;
        }

        public void setRowSpan(int columnIndex, int rs) {
            columns.get(columnIndex).rspan = rs;
        }

        public List<Value> getColumns() {
            return columns;
        }
    }

    public static class Value {
        String v = "";
        int cspan = 1;
        int rspan = 1;

        public String getV() {
            return v;
        }

        public int getCspan() {
            return cspan;
        }

        public int getRspan() {
            return rspan;
        }
    }
}
