package org.alex73.korpus.custom;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.ITextsPreprocessor;
import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessTexts;

public class ApplyParalelny implements ITextsPreprocessor {
    static final Collator BE = Collator.getInstance(new Locale("be"));

    @Override
    public void preprocess(MessageParsedText text) {
    }

    @Override
    public Comparator<TextInfo> getTextsComparator() {
        return (o1, o2) -> {
            if (o1.subtexts.length != 1) {
                throw new RuntimeException("Не адзін варыянт тэкста " + o1.sourceFilePath);
            }
            if (o2.subtexts.length != 1) {
                throw new RuntimeException("Не адзін варыянт тэкста " + o2.sourceFilePath);
            }
            if (o1 == o2) {
                return 0;
            }
            return BE.compare(getBelarusianTitle(o1), getBelarusianTitle(o2));
        };
    }

    private String getBelarusianTitle(TextInfo ti) {
        String r = null;
        for (TextInfo.Subtext st : ti.subtexts) {
            if ("bel".equals(st.lang)) {
                if (r != null) {
                    throw new RuntimeException("Too many belarusian texts for " + ti.sourceFilePath);
                }
                r = st.title;
            }
        }
        if (r == null) {
            throw new RuntimeException("No belarusian text for " + ti.sourceFilePath);
        }
        return r;
    }

    public static void main(String[] args) throws Exception {
        ProcessTexts.preprocessor = new ApplyParalelny();
        PrepareCache3.main(args);
    }
}
