package org.alex73.korpus.custom;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.base.TextInfo.Subtext;
import org.alex73.korpus.compiler.ITextsPreprocessor;
import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessTexts;

public class ApplyKolas implements ITextsPreprocessor {
    static final Collator BE = Collator.getInstance(new Locale("be"));

    @Override
    public void preprocess(MessageParsedText text) {
        if (text.paragraphs != null) {
            ITextsPreprocessor.shuffle(text.paragraphs);
        }
        for (TextInfo.Subtext st : text.textInfo.subtexts) {
            if (st.lang == null) {
                st.lang = "bel";
            }
        }
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
            int c = Long.compare(earliestCreationPublication(o1, Long.MAX_VALUE), earliestCreationPublication(o2, Long.MAX_VALUE));
            if (c == 0) {
                c = BE.compare(o1.subtexts[0].label, o2.subtexts[0].label);
            }
            return c;
        };
    }

    @Override
    public void constructTextPassport(TextInfo textInfo, Subtext subText) {
        StringBuilder s = new StringBuilder();

        subText.label = subText.headers.get("Title");
        addHeader(s, "Аўтары", String.join(",", Arrays.asList(subText.authors)));
        addHeader(s, "Перакладчык", subText.headers.get("Translators"));
        addHeader(s, "Пераклад з", subText.headers.get("LangOrig"));
        addHeader(s, "Назва", subText.headers.get("Title") + "{{page}}");
        addHeader(s, "Стыль/жанр", subText.headers.get("StyleGenre"));
        addHeader(s, "Выданне", subText.headers.get("Edition"));
        addHeader(s, "Час стварэння", subText.headers.get("CreationYear"));
        addHeader(s, "Час публікацыі", subText.headers.get("PublicationYear"));

        subText.passport = s.toString();
    }

    private void addHeader(StringBuilder s, String title, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        s.append("<div><b>" + title + ":</b> " + value + "</div>");
    }

    static long earliestCreationPublication(TextInfo ti, long defaultValue) {
        if (ti.subtexts[0].creationTimeEarliest() != null) {
            return ti.subtexts[0].creationTimeEarliest();
        } else if (ti.subtexts[0].publicationTimeEarliest() != null) {
            return ti.subtexts[0].publicationTimeEarliest();
        } else {
            return defaultValue;
        }
    }

    public static void main(String[] args) throws Exception {
        ProcessTexts.preprocessor = new ApplyKolas();
        PrepareCache3.main(args);
    }
}
