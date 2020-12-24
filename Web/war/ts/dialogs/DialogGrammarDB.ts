class DialogGrammarDB {
	constructor(data: LemmaParadigm) {
		$('#dialog-grammardb-details').html($.templates("#template-grammardb").render({
			details: this.convert(data)
		}));
		$('#dialog-grammardb').modal('show');
	}
	onClose() {
		$('#dialog-grammardb').modal('hide');
	}
	convert(p: LemmaParadigm): OutGrammarParadigm {
		let r: OutGrammarParadigm = new OutGrammarParadigm();
		r.lemma = p.lemma;
		r.meaning = p.meaning;
		let skip: string[] = Grammar.getSkipParts(grammarService.initial, p.tag);
		r.catText = Grammar.parseCode(grammarService.initial, p.tag).filter(kv => skip.indexOf(kv.key) < 0);
		r.subtree = Grammar.subtree(p.tag, grammarService.initial.grammarTree);
		r.variants = [];
		for (let v of p.variants) {
			let rv: OutGrammarVariant = new OutGrammarVariant();
			rv.forms = [];
			rv.catnames = [];

			for (let f of v.forms) {
				let gr = r.subtree;
				for (let c of f.tag.split('')) {
					if (gr) {
						let g = gr[c];
						if (g) {
							if (rv.catnames.indexOf(g.name) < 0 && skip.indexOf(g.name) < 0) {
								rv.catnames.push(g.name);
							}
							gr = g.ch;
						}
					} else {
						break;
					}
				}
			}

			for (let f of v.forms) {
				let rf: OutGrammarForm = new OutGrammarForm();
				rf.value = f.value;
				rf.data = [];
				rf.colspan = [];

				let gr = r.subtree;
				for (let c of f.tag.split('')) {
					if (gr) {
						let g = gr[c];
						if (g) {
							let idx: number = rv.catnames.indexOf(g.name);
							rf.data[idx] = g.desc;
							gr = g.ch;
						}
					} else {
						break;
					}
				}
				for (let i = 0; i < rv.catnames.length; i++) {
					if (rf.data[i]) {
					} else {
						rf.data[i] = "";
					}
				}
				rv.forms.push(rf);
			}
			for (let i = 0; i < rv.catnames.length; i++) {
				for (let fi: number = 0; fi < rv.forms.length; fi++) {
					rv.forms[fi].colspan[i] = 1;
				}
				for (let fi: number = rv.forms.length - 1; fi > 0; fi--) {
					if (rv.forms[fi].data[i] == rv.forms[fi - 1].data[i]) {
						rv.forms[fi - 1].colspan[i] += rv.forms[fi].colspan[i];
						rv.forms[fi].colspan[i] = 0;
					}
				}
			}
			r.variants.push(rv);
		}
		return r;
	}
}
