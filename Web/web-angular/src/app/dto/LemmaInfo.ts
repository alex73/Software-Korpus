export class LemmaInfo {
    public pdgId: number;
    public lemma: string;
    public lemmaGrammar: string;
}

export class LemmaParadigm {
    public lemma: string;
    public tag: string;
    public variants: LemmaVariant[];
}

export class LemmaVariant {
    public forms: LemmaForm[];
}

export class LemmaForm {
    public tag: string;
    public value: string;
}
