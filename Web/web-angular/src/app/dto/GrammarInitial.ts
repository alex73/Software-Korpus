export class GrammarInitial {
    public grammarTree: { [key:string]:GrammarLetter; } = {};
    public grammarWordTypes: KeyValue[] = [];
    public grammarWordTypesGroups: { [key:string]:DBTagsGroups; } = {};
    
    static text(cat: string, grammarTree: { [key:string]:GrammarLetter; } ): string {
        let gr = grammarTree;
        let oo: string = "";
        for(let c of cat.split('')) {
          let g = gr[c];
          if (g) {
            oo += ", " + g.desc;
            gr = g.ch;
            if (!gr) {
              break;
            }
          } else {
            break;
          }
        }
        return oo.substring(2);
    }
    static subtree(cat: string, grammarTree: { [key:string]:GrammarLetter; } ): { [key:string]:GrammarLetter; } {
        let gr = grammarTree;
        for(let c of cat.split('')) {
          let g = gr[c];
          if (g) {
            gr = g.ch;
            if (!gr) {
              return null;
            }
          } else {
            return gr;
          }
        }
        return gr;
    }
}

export class GrammarLetter {
    public name: string;
    public desc: string;
    public ch: { [key:string]:GrammarLetter; };
}

export class KeyValue {
    public key: string;
    public value: string;
}

export class DBTagsGroups {
  public groups: Group[];
}

export class Group {
    public name: string;
    public hidden: boolean;
    public items: Item[];
}

export class Item {
    public code: string;
    public description: string;
}
