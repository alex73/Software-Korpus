class GrammarSearchResult {
	public error: string;
	public hasDuplicateParadigms: boolean;
	public output: LemmaInfo[];
}
class LemmaInfo {
    public pdgId: number;
    public output: string;
    public meaning: string;
}

class LemmaParadigm {
    public lemma: string;
    public tag: string;
    public meaning: string;
    public variants: LemmaVariant[];
}

class LemmaVariant {
    public id: string;
    public forms: LemmaForm[];
}

class LemmaForm {
    public tag: string;
    public value: string;
}
