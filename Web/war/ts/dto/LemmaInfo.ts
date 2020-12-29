class LemmaInfo {
    public pdgId: number;
    public lemma: string;
    public lemmaGrammar: string;
    public meaning: string;
}

class LemmaParadigm {
    public lemma: string;
    public tag: string;
    public meaning: string;
    public variants: LemmaVariant[];
}

class LemmaVariant {
    public forms: LemmaForm[];
}

class LemmaForm {
    public tag: string;
    public value: string;
}
