package org.alex73.korpus.client;

import org.alex73.korpus.shared.dto.ClusterResults;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ResultsCluster extends VerticalPanel {
    public static final int CLUSTER_PAGE_SIZE = 50;

    Korpus korpus;
    Grid grid;

    public ResultsCluster(Korpus korpus) {
        this.korpus = korpus;
    }

    public void showResults(int pageIndex) {
        korpus.searchControls.errorMessage.setText("Знойдзена: " + korpus.clusterResults.rows.length);

        korpus.widgetsInfoDoc.clear();
        korpus.widgetsInfoWord.clear();
        clear();

        int pagesCount = (korpus.clusterResults.rows.length + ResultsCluster.CLUSTER_PAGE_SIZE - 1)
                / ResultsCluster.CLUSTER_PAGE_SIZE;

        add(PagesIndexPanel.createPagesIndexPanel(korpus, pageIndex, pagesCount, pageShowCallback));

        grid = new Grid(0, 4);

        for (int i = pageIndex * CLUSTER_PAGE_SIZE; i < Math.min((pageIndex + 1) * CLUSTER_PAGE_SIZE,
                korpus.clusterResults.rows.length); i++) {
            showRow(korpus.clusterResults.rows[i]);
        }

        add(grid);

        add(PagesIndexPanel.createPagesIndexPanel(korpus, pageIndex, pagesCount, pageShowCallback));
    }

    PagesIndexPanel.IPageRequest pageShowCallback = new PagesIndexPanel.IPageRequest() {
        @Override
        public void showPage(int pageIndex) {
            korpus.requestPageDetails(pageIndex);
        }
    };

    private void showRow(ClusterResults.Row rowData) {
        int row = grid.insertRow(grid.getRowCount());

        HTMLPanel line = new HTMLPanel("");
        line.setStyleName("text-right");
        for (String w : rowData.wordsBefore) {
            if (w != null) {
                line.add(new InlineLabel(w + " "));
            }
        }
        grid.setWidget(row, 0, line);

        InlineLabel wc = new InlineLabel(rowData.word);
        wc.setStyleName("text-center");
        wc.addStyleName("wordFound");
        grid.setWidget(row, 1, wc);

        line = new HTMLPanel("");
        line.setStyleName("text-left");
        for (String w : rowData.wordsAfter) {
            if (w != null) {
                line.add(new InlineLabel(" " + w));
            }
        }
        grid.setWidget(row, 2, line);

        wc = new InlineLabel(Integer.toString(rowData.count));
        wc.setStyleName("text-right");
        grid.setWidget(row, 3, wc);
    }
}
