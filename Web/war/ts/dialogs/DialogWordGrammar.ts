class DialogWordGrammar {
	private currentWordElement: HTMLElement;
	private initial: GrammarInitial;

	constructor(inner: HTMLElement) {
		if (korpusService) {
			this.initial = korpusService.initial.grammar;
		} else {
			this.initial = grammarService.initial;
		}
		this.currentWordElement = inner.closest(".card");
		const html = $.templates("#template-wordgrammar").render({ grammar: this.initial });
		let grammar = this.currentWordElement.querySelector(".wordgram-grammar-string").textContent;
		$('#dialog-wordgrammar-place').html(html);
		if (grammar) {
			(<HTMLSelectElement>document.getElementById("dialog-wordgrammar-grselected")).value = grammar.charAt(0);
			this.changeType(grammar.charAt(0));
			this.textToWordGrammar(grammar);
		}
		$('#dialog-wordgrammar').modal('show');
	}
	changeType(v: string) {
		const html = $.templates("#template-wordgrammar2").render({ grammar: this.initial, grselected: v });
		$('#dialog-wordgrammar-place2').html(html);
	}
	onOk() {
		let gr: string = this.toString();
		this.currentWordElement.querySelector(".wordgram-grammar-string").textContent = gr;
		this.currentWordElement.querySelector(".wordgram-display").textContent = DialogWordGrammar.wordGrammarToText(gr, this.initial);

		$('#dialog-wordgrammar').modal('hide');
	}
	onCancel() {
		$('#dialog-wordgrammar').modal('hide');
	}
	toString(): string {
		let grselectedElement: HTMLSelectElement = <HTMLSelectElement>document.getElementById("dialog-wordgrammar-grselected");
		let grselected = grselectedElement.value;
		if (!grselected) {
			return null;
		}
		let r: string = grselected;
		let db: DBTagsGroups = this.initial.grammarWordTypesGroups[grselected];

		// усе пазначаныя checkboxes пераносім у Map<groupname,listofletters>
		let checked = {};
		for (let group of db.groups) {
			checked[group.name] = "";
		}
		let cbs: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-wordgrammar-place input[type='checkbox']:checked");
		cbs.forEach(cb => {
			checked[cb.name] += cb.value;
		});
		// ствараем радок grammar regexp
		for (let group of db.groups) {
			let r1: string = checked[group.name];
			if (r1.length == 0 || r1.length == group.items.length) {
				r1 = '.';
			} else if (r1.length > 1) {
				r1 = '[' + r1 + ']';
			}
			r += r1;
		}
		return r;
	}
	static wordGrammarToText(grammar: string, info: GrammarInitial): string {
		if (grammar) {
			let p: string = grammar.charAt(0);
			let display = info.grammarTree[p].desc;
			if (!/^\.*$/.test(grammar.substring(1))) {
				display += ', ...';
			}
			return display;
		} else {
			return '---';
		}
	}
	textToWordGrammar(grammar: string) {
		let db: DBTagsGroups = this.initial.grammarWordTypesGroups[grammar.charAt(0)];
		let checked = {};
		for (let group of db.groups) {
			checked[group.name] = "";
		}
		let grindex: number = 0;
		for (let i = 1; i < grammar.length; i++) {
			let group = db.groups[grindex];
			let ch: string = grammar.charAt(i);
			if (ch == '[') {
				for (; i < grammar.length; i++) {
					ch = grammar.charAt(i);
					if (ch == ']') {
						break;
					} else {
						checked[group.name] += ch;
					}
				}
			} else if (ch == '.') {
			} else {
				checked[group.name] += ch;
			}
			grindex++;
		}
		let cbs: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-wordgrammar-place input[type='checkbox']");
		cbs.forEach(cb => {
			cb.checked = checked[cb.name].indexOf(cb.value) >= 0;
		});
	}
}
