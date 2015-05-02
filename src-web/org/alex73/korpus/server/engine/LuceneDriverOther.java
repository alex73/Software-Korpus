/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)
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

package org.alex73.korpus.server.engine;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.utils.WordNormalizer;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Field;

import alex73.corpus.paradigm.P;
import alex73.corpus.paradigm.S;
import alex73.corpus.paradigm.W;

/**
 * Lucene driver for other document's database.
 */
public class LuceneDriverOther extends LuceneDriverBase {
    public Field fieldSentenceTextURL;
    public Field fieldSentenceTextVolume;

    public LuceneDriverOther(String rootDir, boolean write) throws Exception {
        super(rootDir, write);

        docSentence.add(fieldSentenceTextVolume = new Field("textVolume", "", TYPE_NOTSTORED_INDEXED));
        docSentence.add(fieldSentenceTextURL = new Field("textURL", "", TYPE_STORED_NOTINDEXED));
    }

    /**
     * Add sentence to database. Sentence linked to previously added text.
     */
    public int addSentence(P paragraph, byte[] xml, String volume, String textURL) throws Exception {
        values.setLength(0);
        lemmas.setLength(0);
        dbGrammarTags.setLength(0);

        int wordsCount = 0;
        for (Object op : paragraph.getSOrTag()) {
            if (op instanceof S) {
                for (Object o : ((S) op).getWOrTag()) {
                    if (!(o instanceof W)) {
                        continue;
                    }
                    W w = (W) o;
                    wordsCount++;
                    if (w.getValue() != null) {
                        String wc = WordNormalizer.normalize(w.getValue());
                        values.append(wc).append(' ');
                    }
                    if (StringUtils.isNotEmpty(w.getCat())) {
                        for (String t : w.getCat().split("_")) {
                            if (!BelarusianTags.getInstance().isValid(t, null)) {
                                // TODO throw new Exception("Няправільны тэг: " + t);
                            } else {
                                dbGrammarTags.append(DBTagsGroups.getDBTagString(t)).append(' ');
                            }
                        }
                    }
                    if (w.getLemma() != null) {
                        lemmas.append(w.getLemma().replace('_', ' ')).append(' ');
                    }
                }
            }
        }

        // fieldID.setIntValue(id);
        fieldSentenceTextURL.setStringValue(textURL);
        fieldSentenceValues.setStringValue(values.toString());
        fieldSentenceDBGrammarTags.setStringValue(dbGrammarTags.toString());
        fieldSentenceLemmas.setStringValue(lemmas.toString());
        fieldSentenceXML.setBytesValue(xml);

        indexWriter.addDocument(docSentence);
        
        return wordsCount;
    }
}
