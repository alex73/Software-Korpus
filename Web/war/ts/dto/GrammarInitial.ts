class GrammarInitial {
  public grammarTree: { [key: string]: GrammarLetter; };
  public grammarWordTypes: KeyValue[];
  public grammarWordTypesGroups: { [key: string]: DBTagsGroups; };
  public skipGrammar: { [key: string]: string[]; };
  public slouniki: GrammarDict[];
  public stat: GrammarInitialStat;
}

class GrammarInitialStat {
    public title: string;
    public paradigmCount: number;
    public formCount: number;
}

class GrammarDict {
    public name: string;
    public desc: string;
}

class GrammarLetter {
  public name: string;
  public desc: string;
  public ch: { [key: string]: GrammarLetter; };
}

class KeyValue {
  public key: string;
  public value: string;
}

class DBTagsGroups {
  public groups: Group[];
}

class Group {
  public name: string;
  public formGroup: boolean;
  public items: Item[];
}

class Item {
  public code: string;
  public description: string;
}
