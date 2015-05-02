package org.alex73.korpus.client;

import org.alex73.korpus.shared.dto.ResultSentence;
import org.alex73.korpus.shared.dto.WordResult;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ResultsConcordance extends VerticalPanel {
    Grid grid;

    public void showResults(Korpus korpus, int pageIndex, ResultSentence[] sentences, int wordsCount) {
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

        grid = new Grid(0, 4);

        for (ResultSentence s : sentences) {
            for (int i = 0; i < s.text.words.length; i++) {
                for (int j = 0; j < s.text.words[i].length; j++) {
               //     Window.alert(s.text.words[i][j].value+"  req="+s.text.words[i][j].requestedWord+" j="+j);
                    if (s.text.words[i][j].requestedWord && j + wordsCount < s.text.words[i].length) {
                        // show
                        showRow(korpus, s, s.text.words[i], j, j + wordsCount - 1);
                        j += wordsCount - 1;
                    }
                }
            }
        }

        add(grid);

        add(PagesIndexPanel.createPagesIndexPanel(korpus, pageIndex));
    }

    private void showRow(Korpus korpus, ResultSentence text, WordResult[] sentence, int wordsFrom,
            int wordsTo) {
        int row = grid.insertRow(grid.getRowCount());

        Anchor doclabel = new Anchor("падрабязней... ");
        korpus.widgetsInfoDoc.put(doclabel, text);
        doclabel.addClickHandler(korpus.handlerShowInfoDoc);
        grid.setWidget(row, 0, doclabel);

        HTMLPanel line = new HTMLPanel("");
        line.setStyleName("text-right");
        for (int i = Math.max(0, wordsFrom - 8); i < wordsFrom; i++) {
            InlineLabel w = new InlineLabel(ResultsSearch.wordToText(sentence[i]));
            korpus.widgetsInfoWord.put(w, sentence[i]);
            w.addMouseDownHandler(korpus.handlerShowInfoWord);
            line.add(w);
        }
        grid.setWidget(row, 1, line);

        line = new HTMLPanel("");
        line.setStyleName("text-center");
        for (int i = wordsFrom; i <= wordsTo; i++) {
            InlineLabel w = new InlineLabel(ResultsSearch.wordToText(sentence[i]));
            korpus.widgetsInfoWord.put(w, sentence[i]);
            w.addMouseDownHandler(korpus.handlerShowInfoWord);
            line.add(w);
            w.setStyleName("wordFound");
        }
        grid.setWidget(row, 2, line);

        line = new HTMLPanel("");
        line.setStyleName("text-left");
        for (int i = wordsTo + 1; i < Math.min(sentence.length, wordsTo + 9); i++) {
            InlineLabel w = new InlineLabel(ResultsSearch.wordToText(sentence[i]));
            korpus.widgetsInfoWord.put(w, sentence[i]);
            w.addMouseDownHandler(korpus.handlerShowInfoWord);
            line.add(w);
        }
        grid.setWidget(row, 3, line);

    }
}
