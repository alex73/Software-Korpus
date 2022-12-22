package org.alex73.korpus.future;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;

/**
 * Фармаванне граматыкі для граматычнага слоўніка.
 */
public class HramatycnyHram {
    
    static private final ILanguage.IGrammarTags BEL_TAGS = LanguageFactory.get("bel").getTags();
    
    private StringBuilder text = new StringBuilder();
    private List<Group> groups = new ArrayList<>();
    private List<String> other = new ArrayList<>();

    public HramatycnyHram(Paradigm p, Variant v, List<Form> forms) {
        String tag = SetUtils.tag(p, v);
        switch (BEL_TAGS.getValueOfGroup(tag, "Часціна мовы")) {
        case 'N':
            toGrammarSklonavyja(BEL_TAGS.getValueOfGroup(tag, "Субстантываванасць") != 0);
            break;
        case 'A':
        case 'P':
        case 'M':
        case 'S':
            toGrammarSklonavyja(true);
            break;
        case 'V':
            toGrammarDziejaslou();
            break;
        case 'R':
            toGrammarPryslouje();
            break;
        case 'F':
            toGrammarNiazmiennyja();
            break;
        }
        for (Form f : forms) {
            String ftag = SetUtils.tag(p, v, f);
            int groupCounts = 0, itemsCount = 0;
            for (Group g : groups) {
                if (g.isAccepted(ftag)) {
                    groupCounts++;
                    for (Group.Item it : g.items) {
                        if (it.isAccepted(ftag)) {
                            itemsCount++;
                            it.addForm(StressUtils.combineAccute(f.getValue()));
                        }
                    }
                }
            }
            if (groupCounts > 1 || itemsCount > 1) {
                System.err.println(
                        ftag + " трапляе ў некалькі граматык: " + BEL_TAGS.describe(ftag, null));
            }
            if (itemsCount == 0) {
                other.add(StressUtils.combineAccute(f.getValue()) + '/' + ftag);
            }
        }
        text.append("<b>").append(StressUtils.combineAccute(v.getLemma())).append("</b> ");
        text.append(groups.stream().filter(g -> !g.isEmpty()).map(g -> g.toString()).collect(Collectors.joining("; ")));
        if (!other.isEmpty()) {
            text.append(" <b>ІНШЫЯ ФОРМЫ:</b> ");
            text.append(String.join(", ", other));
        }
    }

    @Override
    public String toString() {
        return text.toString();
    }

    private void toGrammarSklonavyja(boolean usieRody) {
        if (usieRody) {
            Group am = new Group("<i>адз. м.р.</i> ", "Склон").setTag("Лік", 'S').setTag("Род", 'M');
            sklony(am);
            Group af = new Group("<i>адз. ж.р.</i> ", "Склон").setTag("Лік", 'S').setTag("Род", 'F');
            sklony(af);
            Group an = new Group("<i>адз. н.р.</i> ", "Склон").setTag("Лік", 'S').setTag("Род", 'N');
            sklony(an);
            Group au = new Group("<i>адз.</i> ", "Склон").setTag("Лік", 'S').setTag("Род", '0');
            sklony(au);
        } else {
            Group a = new Group("<i>адз.</i> ", "Склон").setTag("Лік", 'S');
            sklony(a);
        }
        Group mn = new Group("<i>мн.</i> ", "Склон").setTag("Лік", 'P');
        sklony(mn);
    }

    private void toGrammarDziejaslou() {
        Group h = new HiddenGroup().setTag("Інфінітыў", '0');
        h.new Item();
        Group c = new Group("<i>цяп.</i> ", null).setTag("Час", 'R').setTag("Дзеепрыслоўе", '\0');
        c.new Item();
        Group b = new Group("<i>будуч.</i> ", null).setTag("Час", 'F').setTag("Дзеепрыслоўе", '\0');
        b.new Item();
        Group m = new Group("<i>прошл.</i> ", null).setTag("Час", 'P').setTag("Дзеепрыслоўе", '\0');
        m.new Item();
        Group z = new Group("<i>заг.</i> ", null).setTag("Загадны лад", 'I');
        z.new Item();
        Group d = new Group("<i>дзеепрысл.</i> ", null).setTag("Дзеепрыслоўе", 'G');
        d.new Item();
    }

    private void toGrammarPryslouje() {
        Group p = new Group("", "Ступень параўнання");
        p.new Item("", 'P');
        p.new Item("<i>выш.</i> ", 'C');
        p.new Item("<i>найвыш.</i> ", 'S');
    }

    private void toGrammarNiazmiennyja() {
        Group p = new Group("", null);
        p.new Item("", ' ');
    }

    private void sklony(Group g) {
        g.new Item("<i>Н</i> ", 'N');
        g.new Item("<i>Р</i> ", 'G');
        g.new Item("<i>Д</i> ", 'D');
        g.new Item("<i>В</i> ", 'A');
        g.new Item("<i>Т</i> ", 'I');
        g.new Item("<i>М</i> ", 'L');
        g.new Item("<i>К</i> ", 'V');
    }

    public class Group {
        private final String prefix;
        private final String itemTagName;
        private final Map<String, Character> tags = new TreeMap<>();
        private List<Item> items = new ArrayList<>();

        public class Item {
            private final String prefix;
            private final char tagValue;
            private final List<String> forms = new ArrayList<>();

            public Item() {
                this.prefix = "";
                this.tagValue = 0;
                items.add(this);
            }

            public Item(String prefix, char tagValue) {
                this.prefix = prefix;
                this.tagValue = tagValue;
                items.add(this);
            }

            public boolean isAccepted(String tag) {
                if (Group.this.itemTagName == null) {
                    return true;
                }
                char tv = BEL_TAGS.getValueOfGroup(tag, Group.this.itemTagName);
                return tv == tagValue;
            }

            public boolean isEmpty() {
                return forms.isEmpty();
            }

            public void addForm(String w) {
                forms.add(w);
            }

            @Override
            public String toString() {
                return prefix + String.join(", ", forms);
            }
        }

        public Group(String prefix, String itemTagName) {
            this.prefix = prefix;
            this.itemTagName = itemTagName;
            groups.add(this);
        }

        public boolean isAccepted(String tag) {
            for (Map.Entry<String, Character> t : tags.entrySet()) {
                char tv = BEL_TAGS.getValueOfGroup(tag, t.getKey());
                if (tv != t.getValue().charValue()) {
                    return false;
                }
            }
            return true;
        }

        public boolean isEmpty() {
            return items.stream().allMatch(it -> it.isEmpty());
        }

        public Group setTag(String n, char v) {
            tags.put(n, v);
            return this;
        }

        @Override
        public String toString() {
            return prefix + items.stream().filter(it -> !it.isEmpty()).map(it -> it.toString())
                    .collect(Collectors.joining(", "));
        }
    }

    public class HiddenGroup extends Group {
        public HiddenGroup() {
            super(null, null);
        }

        public boolean isEmpty() {
            return true;
        }
    }
}
