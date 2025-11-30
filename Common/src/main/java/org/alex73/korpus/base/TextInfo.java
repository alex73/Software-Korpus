package org.alex73.korpus.base;

import java.io.Serializable;

import org.alex73.korpus.utils.KorpusDateTime;

@SuppressWarnings("serial")
public class TextInfo implements Serializable {
    public String subcorpus;
    public transient String sourceFilePath; // source file ID
    public String[] styleGenres; // styles and genres
    public transient int globalTextOrder;
    public Subtext[] subtexts;

    public TextInfo() {
    }

    public static class Subtext {
        public String sourceName; // крыніца: толькі фільтраў
        public String sourceLinkBase; // крыніца: толькі для сайтаў, неразабраных і аўдыё
        public String previewLinkBase; // пачатак адрасу preview, толькі для audio
        //URL public String url; // спасылка на знешні сайт, калі ёсць
        public String[] authors; // аўтары, у перавёрнутым выглядзе, афіцыйным правапісам
        //public String title; // назва: заўсёды
        //Translators public String[] translators; // перакладчыкі
        public String lang, langOrig; // мова тэксту, мова зыходнага тэксту
        //public String edition; // выданне
       // public String details; // дэталі
        //public String file; // файл на зыходнай старонцы
        public String creationTime, publicationTime; // дата стварэння і публікацыі
        public String label, passport, title; // пазнака, поўны пашпарт і загаловак тэкста

        private transient Long creationTimeLatest, creationTimeEarliest;
        private transient Long publicationTimeLatest, publicationTimeEarliest;

        public Long creationTimeLatest() {
            if (creationTime == null) {
                return null;
            }
            if (creationTimeLatest == null) {
                KorpusDateTime dt = new KorpusDateTime(creationTime);
                creationTimeEarliest = dt.earliest();
                creationTimeLatest = dt.latest();
            }
            return creationTimeLatest;
        }

        public Long creationTimeEarliest() {
            if (creationTime == null) {
                return null;
            }
            if (creationTimeEarliest == null) {
                KorpusDateTime dt = new KorpusDateTime(creationTime);
                creationTimeEarliest = dt.earliest();
                creationTimeLatest = dt.latest();
            }
            return creationTimeEarliest;
        }

        public Long publicationTimeLatest() {
            if (publicationTime == null) {
                return null;
            }
            if (publicationTimeLatest == null) {
                KorpusDateTime dt = new KorpusDateTime(publicationTime);
                publicationTimeEarliest = dt.earliest();
                publicationTimeLatest = dt.latest();
            }
            return publicationTimeLatest;
        }

        public Long publicationTimeEarliest() {
            if (publicationTime == null) {
                return null;
            }
            if (publicationTimeEarliest == null) {
                KorpusDateTime dt = new KorpusDateTime(publicationTime);
                publicationTimeEarliest = dt.earliest();
                publicationTimeLatest = dt.latest();
            }
            return publicationTimeEarliest;
        }
    }
}
