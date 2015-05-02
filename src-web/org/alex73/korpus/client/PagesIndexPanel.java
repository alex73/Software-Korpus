package org.alex73.korpus.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;

public class PagesIndexPanel {

    public static HTMLPanel createPagesIndexPanel(final Korpus korpus, int currentPageIndex) {
        HTMLPanel panelPages = new HTMLPanel("<br/><br/>");
        for (int i = 0; i < korpus.pages.size(); i++) {
            if (i == currentPageIndex) {
                InlineLabel next = new InlineLabel(Integer.toString(i + 1));
                next.setStyleName("pageIndex");
                panelPages.add(next);
            } else {
                Anchor next = new Anchor(Integer.toString(i + 1));
                next.setStyleName("pageIndex");
                final int pi = i;
                next.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        korpus.requestPageDetails(pi);
                    }
                });
                panelPages.add(next);
            }
        }
        if (korpus.hasMore) {
            Anchor next = new Anchor("Наступная старонка...");
            next.setStyleName("pageIndex");
            next.addClickHandler(korpus.nextPageHandler);
            panelPages.add(next);
        }
        panelPages.add(new HTMLPanel("<br/><br/>"));

        return panelPages;
    }
}
