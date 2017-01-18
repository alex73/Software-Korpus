package org.alex73.korpus.client;

import org.alex73.korpus.client.utils.TextPos;
import org.alex73.korpus.shared.dto.ResultText;
import org.alex73.korpus.shared.dto.SearchResults;
import org.alex73.korpus.shared.dto.WordResult;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ResultsSearch extends VerticalPanel {
    Korpus korpus;

    public ResultsSearch(Korpus korpus) {
        this.korpus = korpus;
    }

    public void showResults(int pageIndex, SearchResults[] sentences) {
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

        for (SearchResults s : sentences) {
            HTMLPanel p = new HTMLPanel("");
            Anchor doclabel = new Anchor("падрабязней... ");
            korpus.widgetsInfoDoc.put(doclabel, s);
            doclabel.addClickHandler(korpus.handlerShowInfoDoc);
            // doclabel.addMouseOutHandler(handlerHideInfoDoc);

            p.add(doclabel);

            int num = getRequestedWordsCountInResult(s.text) / korpus.currentSearchParams.words.size();
            int wordsCount;
            switch (num) {
            case 0:
            case 1:
                wordsCount = 7;
                break;
            case 2:
                wordsCount = 5;
                break;
            case 3:
                wordsCount = 4;
                break;
            default:
                wordsCount = 3;
                break;
            }
            outputText(s, p, korpus, true, wordsCount, 0, " ... ");

            add(p);
        }

        add(PagesIndexPanel.createPagesIndexPanel(korpus, pageIndex, korpus.pages.size(), pageShowCallback));
    }

    PagesIndexPanel.IPageRequest pageShowCallback = new PagesIndexPanel.IPageRequest() {
        @Override
        public void showPage(int pageIndex) {
            korpus.requestPageDetails(pageIndex);
        }
    };

    int getRequestedWordsCountInResult(ResultText text) {
        int count = 0;

        for (int i = 0; i < text.words.length; i++) {
            for (int j = 0; j < text.words[i].length; j++) {
                if (text.words[i][j].requestedWord) {
                    count++;
                }
            }
        }
        return count;
    }

    public static void outputText(SearchResults s, HTMLPanel p, Korpus korpus, boolean addWordInfoHandler,
            int wordAround, int sentencesAround, String separatorText) {
        TextPos begin = new TextPos(s.text, 0, 0);
        TextPos end = new TextPos(s.text, s.text.words.length - 1,
                s.text.words[s.text.words.length - 1].length - 1);

        TextPos pos = getNextRequestedWordPosAfter(s.text, null);
        TextPos currentAroundFrom = pos.addWords(-wordAround);
        if (sentencesAround != 0) {
            currentAroundFrom = currentAroundFrom.addSequences(-sentencesAround);
        }
        TextPos currentAroundTo = pos.addWords(wordAround);
        if (sentencesAround != 0) {
            currentAroundTo = currentAroundTo.addSequences(sentencesAround);
        }

        if (currentAroundFrom.after(begin)) {
            p.add(new InlineLabel(separatorText));
        }

        while (true) {
            TextPos next = getNextRequestedWordPosAfter(s.text, pos);
            if (next == null) {
                break;
            }
            TextPos nextAroundFrom = next.addWords(-wordAround);
            if (sentencesAround != 0) {
                nextAroundFrom = nextAroundFrom.addSequences(-sentencesAround);
            }
            TextPos nextAroundTo = next.addWords(wordAround);
            if (sentencesAround != 0) {
                nextAroundTo = nextAroundTo.addSequences(sentencesAround);
            }

            if (currentAroundTo.addWords(2).after(nextAroundFrom)) {
                // merge
                currentAroundTo = nextAroundTo;
            } else {
                output(p, korpus, s.text, currentAroundFrom, currentAroundTo);
                p.add(new InlineLabel(separatorText));
                currentAroundFrom = nextAroundFrom;
                currentAroundTo = nextAroundTo;
            }
            pos = next;
        }
        output(p, korpus, s.text, currentAroundFrom, currentAroundTo);

        if (end.after(currentAroundTo)) {
            p.add(new InlineLabel(separatorText));
        }
    }

    static void output(HTMLPanel p, Korpus korpus, ResultText text, TextPos from, TextPos to) {
        TextPos curr = from;
        while (true) {
            WordResult w = text.words[curr.getSentence()][curr.getWord()];
            if (!w.isWord && !w.orig.isEmpty() && w.orig.charAt(0) == '\n') {
                p.add(new InlineHTML("<br/>"));
            } else {
                InlineLabel wlabel = new InlineLabel(w.orig);
                if (w.requestedWord) {
                    wlabel.setStyleName("wordFound");
                }
                if (w.isWord) {
                    wlabel.addMouseDownHandler(korpus.handlerShowInfoWord);
                    korpus.widgetsInfoWord.put(wlabel, w);
                }
                p.add(wlabel);
            }
            TextPos next = curr.addPos(1);
            if (curr.equals(to)) {
                break;
            }
            curr = next;
        }
    }

    static TextPos getNextRequestedWordPosAfter(ResultText text, TextPos currentPos) {
        int startI, startJ;
        if (currentPos == null) {
            startI = 0;
            startJ = 0;
        } else {
            TextPos next = currentPos.addWords(1);
            if (next.equals(currentPos)) {
                return null;
            }
            startI = next.getSentence();
            startJ = next.getWord();
        }
        int j = startJ;
        for (int i = startI; i < text.words.length; i++) {
            for (; j < text.words[i].length; j++) {
                if (text.words[i][j].requestedWord) {
                    return new TextPos(text, i, j);
                }
            }
            j = 0;
        }
        return null;
    }
}
