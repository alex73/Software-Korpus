/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.alex73.spellchecker;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.languagetool.Language;
import org.languagetool.UserConfig;
import org.languagetool.language.Contributor;
import org.languagetool.rules.CommaWhitespaceRule;
import org.languagetool.rules.DoublePunctuationRule;
import org.languagetool.rules.MultipleWhitespaceRule;
import org.languagetool.rules.Rule;
import org.languagetool.rules.UppercaseSentenceStartRule;
import org.languagetool.tagging.Tagger;
import org.languagetool.tagging.xx.DemoTagger;
import org.languagetool.tokenizers.SRXSentenceTokenizer;
import org.languagetool.tokenizers.SentenceTokenizer;

/**
 * Belarusian language declarations.
 *
 * Copyright (C) 2010 Alex Buloichik (alex73mail@gmail.com)
 */
public class Belarusian extends Language {

    @Override
    public String getName() {
        return "Belarusian";
    }

    @Override
    public String getShortCode() {
        return "be";
    }

    @Override
    public String[] getCountries() {
        return new String[] { "BY" };
    }

    @Override
    public Tagger createDefaultTagger() {
        return new DemoTagger();
    }

    @Override
    public SentenceTokenizer createDefaultSentenceTokenizer() {
        return new SRXSentenceTokenizer(this);
    }

    @Override
    public Contributor[] getMaintainers() {
        return new Contributor[0];
    }

    @Override
    public List<Rule> getRelevantRules(ResourceBundle messages, UserConfig userConfig, Language motherTongue,
            List<Language> altLanguages) throws IOException {
        return Arrays.asList(new CommaWhitespaceRule(messages), new DoublePunctuationRule(messages),
              //  new BelarusianMorfologikSpellerRule(messages, this, userConfig),
                new MorfologikBelarusianSpellerRule(messages, this, userConfig, altLanguages),
                new UppercaseSentenceStartRule(messages, this), new MultipleWhitespaceRule(messages, this));
    }

}
