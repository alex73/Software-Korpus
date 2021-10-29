class GrammarSearchResult {
	public error: string;
	public hasDuplicateParadigms: boolean;
	public output: LemmaInfo[];
    public hasMultiformResult: boolean;
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
}

class LemmaForm {
    public tag: string;
    public value: string;
    public options: string;
    public type: string;
}

class Author {
    public name: string;
    public displayName: string;
}
