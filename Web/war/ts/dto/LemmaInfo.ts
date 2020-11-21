class LemmaInfo {
    public pdgId: number;
    public lemma: string;
    public lemmaGrammar: string;
}

class LemmaParadigm {
    public lemma: string;
    public tag: string;
    public variants: LemmaVariant[];
}

class LemmaVariant {
    public forms: LemmaForm[];
}

class LemmaForm {
    public tag: string;
    public value: string;
}
