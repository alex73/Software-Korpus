package org.alex73.korpus.client;

import org.alex73.korpus.shared.ResultSentence;
import org.alex73.korpus.shared.ResultText;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ResultsSearch extends VerticalPanel {

    public void showResults(Korpus korpus, int pageIndex, ResultSentence[] sentences) {
        int c = 0;
        for (int[] p : korpus.pages) {
            c += p.length;
        }
        if (korpus.hasMore) {
            korpus.searchControls.errorMessage.setText("Знойдзена больш за " + c);
        } else {
            korpus.searchControls.errorMessage.setText("Знойдзена: " + c);
        }

        korpus.widgetsInfoDoc.clear();
        korpus.widgetsInfoWord.clear();
        clear();

        add(PagesIndexPanel.createPagesIndexPanel(korpus, pageIndex));

        for (ResultSentence s : sentences) {
            HTMLPanel p = new HTMLPanel("");
            Anchor doclabel = new Anchor("падрабязней... ");
            korpus.widgetsInfoDoc.put(doclabel, s);
            doclabel.addClickHandler(korpus.handlerShowInfoDoc);
            // doclabel.addMouseOutHandler(handlerHideInfoDoc);

            p.add(doclabel);

            boolean alreadyFound = false;
            for (int i = 0; i < s.text.words.length; i++) {
                int firstFoundWord = -1;
                for (int j = 0; j < s.text.words[i].length; j++) {
                    if (s.text.words[i][j].requestedWord) {
                        firstFoundWord = j;
                        break;
                    }
                }
                if (firstFoundWord >= 0) {
                    if (alreadyFound) {
                        p.add(new InlineLabel(" ... "));
                    } else {
                        alreadyFound = true;
                    }
                    int begWord = Math.max(firstFoundWord - 6, 0);
                    int endWord = Math.min(s.text.words[i].length - 1, firstFoundWord + 6);
                    for (int j = begWord; j <= endWord; j++) {
                        ResultText.Word w = s.text.words[i][j];
                        InlineLabel wlabel = new InlineLabel(wordToText(w));
                        if (w.requestedWord) {
                            wlabel.setStyleName("wordFound");
                        }
                        wlabel.addMouseDownHandler(korpus.handlerShowInfoWord);
                        p.add(wlabel);
                        korpus.widgetsInfoWord.put(wlabel, w);
                    }
                }
            }

            add(p);
        }

        add(PagesIndexPanel.createPagesIndexPanel(korpus, pageIndex));
    }

    public static String wordToText(ResultText.Word w) {
        if (w.value.equals(",") || w.value.equals(".")) {
            return w.value;
        } else {
            return " " + w.value;
        }
    }
}
