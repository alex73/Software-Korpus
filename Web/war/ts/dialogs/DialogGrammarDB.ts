class DialogGrammarDB {
	constructor(data: LemmaParadigm) {
		$('#dialog-grammardb-details').html($.templates("#template-grammardb").render({
			details: this.convert(data),
			getter: this.getter,
			log: this.log
		}));
        $('[data-toggle="tooltip"]').tooltip();
		this.hideBlock('tr.gr-p1');
		this.hideBlock('tr.gr-p2');
		this.hideBlock('tr.gr-p3');
		this.hideBlock('tr.gr-p4');
		this.hideBlock('tr.gr-p5');
		// merge the same cells vertically
		$('#dialog-grammardb table').each((i,e) => this.mergeCellsVertically(e));
		$('#dialog-grammardb').modal('show');
	}
	hideBlock(cls:string) {
		var hide = true;
		$(cls).each((idx,e) => {
			if ($(e).find('w').length) {
				hide = false;
			}
		});
		if (hide) {
			$(cls).remove();
		}
	}
	mergeCellsVertically(e:HTMLTableElement) {
		for(var r=e.rows.length-1; r>0; r--) {
			for(var c=e.rows[r-1].cells.length-1; c>=0; c--) {
				let v1 = e.rows[r-1].cells[c];
				let v2 = e.rows[r].cells[c];
				if (v1 && v2 && v1.classList.contains('grtag') && v2.classList.contains('grtag') &&  v1.innerHTML == v2.innerHTML) {
					e.rows[r-1].cells[c].rowSpan+=e.rows[r].cells[c].rowSpan;
					e.rows[r].deleteCell(c);
				}
			}
		}
	}
	onClose() {
		$('#dialog-grammardb').modal('hide');
	}
	getter(spec:string, v:OutGrammarVariant): string {
		let rq:KeyValue[] = Array()
		if (spec) {
			for(let r of spec.split(';')) {
				let m = r.match("(.+):(.)");
				if (m) {
					let kv:KeyValue = new KeyValue();
					kv.key = m[1]
					kv.value = m[2]
					rq.push(kv)
				} else {
					throw "Wrong form specification: "+spec;
				}
			}
		}
		let result = Array();
		for (var fi = 0; fi < v.sourceForms.length; fi++) {
			let f = v.sourceForms[fi];
			let descr = Grammar.getCodes(grammarService.initial, v.tag+f.tag);
			let passed:boolean = true;
			for(let kv of rq) {
				if (descr[kv.key] !== kv.value) {
					passed = false;
					break;
				}
			}
			if (passed) {
				v.sourceFormsUsageCount[fi]++;
				var suffix = '';
				if (f.options === 'ANIM') {
					suffix = ' <span class="grtag">(адуш.)</span>';
				} else if (f.options === 'INANIM') {
					suffix = ' <span class="grtag">(неадуш.)</span>';
				}
				result.push(f.value + suffix);
			}
		}
		return result.length ? '<w>'+result.join('<br/>')+'</w>' : '–';
	}
	log(v:OutGrammarVariant): string {
		let r = Array();
		for (var fi = 0; fi < v.sourceForms.length; fi++) {
			if (v.sourceFormsUsageCount[fi] == 0) {
				r.push(v.sourceForms[fi].value+": непаказана")
			} else if (v.sourceFormsUsageCount[fi] > 1) {
				r.push(v.sourceForms[fi].value+": некалькі разоў")
			}
		}
		return r.join(", ");
	}
	convert(p: LemmaParadigm): OutGrammarParadigm {
		let r: OutGrammarParadigm = new OutGrammarParadigm();
		r.lemma = p.lemma;
		r.meaning = p.meaning;
		r.variants = [];
		for (let v of p.variants) {
			let rv: OutGrammarVariant = new OutGrammarVariant();
            rv.tag = (p.tag != null ? p.tag : "") + (v.tag != null ? v.tag : "");
            let skip: string[] = Grammar.getSkipParts(grammarService.initial, rv.tag);
            rv.catText = Grammar.parseCode(grammarService.initial, rv.tag).filter(kv => skip.indexOf(kv.key) < 0);
            rv.subtree = Grammar.subtree(rv.tag, grammarService.initial.grammarTree);
			rv.forms = [];
			rv.catnames = [];
            rv.dictionaries = [];
            for(let sl of grammarService.initial.slouniki) {
                if (v.dictionaries.indexOf(sl.name) >= 0) {
                    rv.dictionaries.push(sl);
                }
            }
            rv.authors = v.authors.slice(0, 15);
            rv.authorsOtherCount = v.authors.slice(15).length;
            rv.authorsOtherList = v.authors.slice(15, 60).map(a => a.displayName).join(", ");
            if (v.authors.length > 60) {
                rv.authorsOtherList += ", ...";
            }

			for (let f of v.forms) {
				let gr = rv.subtree;
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
			rv.sourceForms = v.forms;
			rv.sourceFormsUsageCount = Array();
			for(let f of rv.sourceForms) {
				rv.sourceFormsUsageCount.push(0);
			}

			for (let f of v.forms) {
				let rf: OutGrammarForm = new OutGrammarForm();
				rf.value = f.value;
				rf.data = [];
				rf.colspan = [];

				let gr = rv.subtree;
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
