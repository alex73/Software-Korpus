package org.alex73.korpus.custom;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.ITextsPreprocessor;
import org.alex73.korpus.compiler.MessageParsedText;

public class ApplyKolas implements ITextsPreprocessor {
    static final Collator BE = Collator.getInstance(new Locale("be"));

    @Override
    public void preprocess(MessageParsedText text) {
        ITextsPreprocessor.shuffle(text.paragraphs);
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
                c = BE.compare(o1.subtexts[0].title, o2.subtexts[0].title);
            }
            return c;
        };
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
}
