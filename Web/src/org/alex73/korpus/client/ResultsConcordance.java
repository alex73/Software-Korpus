package org.alex73.korpus.client;

import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.shared.dto.SearchResults;
import org.alex73.korpus.shared.dto.WordResult;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ResultsConcordance extends VerticalPanel {
    Korpus korpus;
    Grid grid;

    public ResultsConcordance(Korpus korpus) {
        this.korpus = korpus;
    }

    public void showResults(int pageIndex, SearchResults[] sentences, int wordsCount) {
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

        add(PagesIndexPanel.createPagesIndexPanel(korpus, pageIndex, korpus.pages.size(), pageShowCallback));

        grid = new Grid(0, 4);

        for (SearchResults s : sentences) {
            for (int i = 0; i < s.text.words.length; i++) {
                for (int j = 0; j < s.text.words[i].length; j++) {
                    // Window.alert(s.text.words[i][j].value+"  req="+s.text.words[i][j].requestedWord+" j="+j);
                    if (s.text.words[i][j].requestedWord && j + wordsCount < s.text.words[i].length) {
                        // show
                        int wordsTo = showRow(s, s.text.words[i], j, wordsCount);
                        j = wordsTo;
                    }
                }
            }
        }

        add(grid);

        add(PagesIndexPanel.createPagesIndexPanel(korpus, pageIndex, korpus.pages.size(), pageShowCallback));
    }

    PagesIndexPanel.IPageRequest pageShowCallback = new PagesIndexPanel.IPageRequest() {
        @Override
        public void showPage(int pageIndex) {
            korpus.requestPageDetails(pageIndex);
        }
    };

    private int showRow(SearchResults text, WordResult[] sentence, int wordsFrom, int wordsCount) {
        int row = grid.insertRow(grid.getRowCount());

        Anchor doclabel = new Anchor("падрабязней... ");
        korpus.widgetsInfoDoc.put(doclabel, text);
        doclabel.addClickHandler(korpus.handlerShowInfoDoc);
        grid.setWidget(row, 0, doclabel);

        HTMLPanel line = new HTMLPanel("");
        line.setStyleName("text-right");
        List<InlineLabel> back = new ArrayList<>();
        for (int i = wordsFrom - 1, count = 0; i >= 0 && count < 8; i--) {
            InlineLabel w = new InlineLabel(sentence[i].orig);
            if (sentence[i].isWord) {
                korpus.widgetsInfoWord.put(w, sentence[i]);
                w.addMouseDownHandler(korpus.handlerShowInfoWord);
                count++;
            }
            back.add(w);
        }
        for (int i = back.size() - 1; i >= 0; i--) {
            line.add(back.get(i));
        }
        grid.setWidget(row, 1, line);

        int wordsTo = wordsFrom;
        line = new HTMLPanel("");
        line.setStyleName("text-center");
        for (int i = wordsFrom, count = 0; i < sentence.length && count < wordsCount; i++) {
            InlineLabel w = new InlineLabel(sentence[i].orig);
            if (sentence[i].isWord) {
                korpus.widgetsInfoWord.put(w, sentence[i]);
                w.addMouseDownHandler(korpus.handlerShowInfoWord);
                count++;
                wordsTo = i;
            }
            line.add(w);
            w.setStyleName("wordFound");
        }
        grid.setWidget(row, 2, line);

        line = new HTMLPanel("");
        line.setStyleName("text-left");
        for (int i = wordsTo + 1, count = 0; i < sentence.length && count < 8; i++) {
            InlineLabel w = new InlineLabel(sentence[i].orig);
            if (sentence[i].isWord) {
                korpus.widgetsInfoWord.put(w, sentence[i]);
                w.addMouseDownHandler(korpus.handlerShowInfoWord);
                count++;
            }
            line.add(w);
        }
        grid.setWidget(row, 3, line);

        return wordsTo;
    }
}
