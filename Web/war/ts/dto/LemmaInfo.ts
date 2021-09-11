class GrammarSearchResult {
	public error: string;
	public hasDuplicateParadigms: boolean;
	public output: LemmaInfo[];
}
class LemmaInfo {
    public pdgId: number;
    public output: string;
    public meaning: string;
    public grammar: string;
}

class LemmaParadigm {
    public lemma: string;
    public tag: string;
    public meaning: string;
    public variants: LemmaVariant[];
}

class LemmaVariant {
    public id: string;
    public tag: string;
    public forms: LemmaForm[];
    public dictionaries: string[];
    public authors: Author[];
    public authorsOther: number;
}

class LemmaForm {
    public tag: string;
    public value: string;
    public options: string;
}

class Author {
    public name: string;
    public displayName: string;
}
