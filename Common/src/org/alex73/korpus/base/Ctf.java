package org.alex73.korpus.base;

import java.util.List;

public class Ctf {
    public String[] styleGenres;

    public Language[] languages;

    public static class Language {
        public String lang;
        public String label, title;
        public String[] authors;
        public String source;
        public String creationTime, publicationTime;
        public String[] headers;

        public Page pages[];
    }

    public static class Page {
        public String pageNum;
        public String audioPreview;
        public String paragraphs[];
    }

    public void setPages(String lang, List<Page> pages) {
        languages = new Language[1];
        languages[0] = new Language();
        languages[0].lang = lang;
        languages[0].pages = pages.toArray(new Page[0]);
    }
}
