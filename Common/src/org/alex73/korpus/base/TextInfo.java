/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)
               Home page: https://sourceforge.net/projects/korpus/

 This file is part of Korpus.

 Korpus is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Korpus is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.korpus.base;

import java.io.Serializable;

import org.alex73.korpus.utils.KorpusDateTime;

@SuppressWarnings("serial")
public class TextInfo implements Serializable {
    public transient String sourceFilePath;
    public String subcorpus;
    public String source;
    public String url;
    public String[] authors;
    public String title;
    public String[] translators;
    public String lang, langOrig;
    public String[] styleGenres;
    public String edition;
    public String details;
    public String file;
    public String creationTime, publicationTime;

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
