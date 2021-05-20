class Grammar {
  static getSkipParts(data: GrammarInitial, cat: string): string[] {
    let skip: string[] = data.skipGrammar[cat.charAt(0)];
    return skip ? skip : [];
  }
  static parseCode(data: GrammarInitial, cat: string): KeyValue[] {
    let gr = data.grammarTree;
    let r = [];
    for (let c of cat.split('')) {
      let g = gr[c];
      if (g) {
        let kv = new KeyValue();
        kv.key = g.name;
        kv.value = g.desc;
        r.push(kv);
        gr = g.ch;
        if (!gr) {
          break;
        }
      } else {
        break;
      }
    }
    return r;
  }
  static getCodes(data: GrammarInitial, cat: string): { [key:string]: string; } {
    let gr = data.grammarTree;
    let r = {};
    for (let c of cat.split('')) {
      let g = gr[c];
      if (g) {
        r[g.name] = c;
        gr = g.ch;
        if (!gr) {
          break;
        }
      } else {
        break;
      }
    }
    return r;
  }
  static subtree(cat: string, grammarTree: { [key: string]: GrammarLetter; }): { [key: string]: GrammarLetter; } {
    let gr = grammarTree;
    for (let c of cat.split('')) {
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