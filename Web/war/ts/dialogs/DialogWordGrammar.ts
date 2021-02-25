class DialogWordGrammar {
	private currentWordElement: HTMLElement;
	private hideParadigmGroups: boolean;
	private hideFormGroups: boolean;

	private constructor(inner: HTMLElement) {
		this.currentWordElement = inner.closest(".word-select");
	}
	static createParadigmTagsDialog(inner: HTMLElement, showFormGroups: boolean): DialogWordGrammar {
		let result: DialogWordGrammar = new DialogWordGrammar(inner);
		result.hideParadigmGroups = false;
		result.hideFormGroups = !showFormGroups;
		let grammar = result.currentWordElement.querySelector(".wordgram-grammar-string").textContent;
		const html = $.templates("#template-wordgrammar").render({
			grammar: DialogWordGrammar.getInitial(),
			hideParadigmGroups: result.hideParadigmGroups,
			hideFormGroups: result.hideFormGroups
		});
		$('#dialog-wordgrammar-place').html(html);
		if (grammar) {
			(<HTMLSelectElement>document.getElementById("dialog-wordgrammar-grselected")).value = grammar.charAt(0);
			result.changeType(grammar.charAt(0));
			DialogWordGrammar.textToWordGrammar(grammar, grammar, true, false);
		}
		$('#dialog-wordgrammar').modal('show');
		return result;
	}
	static createFormOnlyTagsDialog(inner: HTMLElement, paradigmSelectedSpanId: string): DialogWordGrammar {
		let result: DialogWordGrammar = new DialogWordGrammar(inner);
		result.hideParadigmGroups = true;
		result.hideFormGroups = false;
		let paradigmGrammar = document.getElementById(paradigmSelectedSpanId).textContent;
		let grammar = result.currentWordElement.querySelector(".wordgram-grammar-string").textContent;
		const html = $.templates("#template-wordgrammar").render({
			grammar: DialogWordGrammar.getInitial(),
			hideParadigmGroups: result.hideParadigmGroups,
			hideFormGroups: result.hideFormGroups
		});
		$('#dialog-wordgrammar-place').html(html);
		(<HTMLSelectElement>document.getElementById("dialog-wordgrammar-grselected")).value = paradigmGrammar.charAt(0);
		result.changeType(paradigmGrammar.charAt(0));
		DialogWordGrammar.textToWordGrammar(paradigmGrammar, grammar, true, true);
		$('#dialog-wordgrammar').modal('show');
		return result;
	}
	changeType(v: string) {
		let html = v ? $.templates("#template-wordgrammar2").render({
			grammar: DialogWordGrammar.getInitial(),
			grselected: v,
			hideParadigmGroups: this.hideParadigmGroups,
			hideFormGroups: this.hideFormGroups
		}) : "";
		$('#dialog-wordgrammar-place2').html(html);
	}
	onOk() {
		let gr: string = this.toString();
		this.currentWordElement.querySelector(".wordgram-grammar-string").textContent = gr;
		DialogWordGrammar.wordGrammarToText(gr, this.currentWordElement.querySelector(".wordgram-display"));

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
		let db: DBTagsGroups = DialogWordGrammar.getInitial().grammarWordTypesGroups[grselected];

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
	static getInitial(): GrammarInitial {
		if (korpusService) {
			return korpusService.initial.grammar;
		} else {
			return grammarService.initial;
		}
	}
	static wordGrammarToText(grammar: string, outputElement: HTMLElement) {
		if (grammar) {
			let p: string = grammar.charAt(0);
			let display = DialogWordGrammar.getInitial().grammarTree[p].desc;
			if (!/^\.*$/.test(grammar.substring(1))) {
				display += ',&nbsp;...';
			}
			outputElement.innerHTML = display;
			let describeFormsTagsOnly: boolean = outputElement.classList.contains('wordgram-display-formsonly');
			outputElement.title = DialogWordGrammar.textToWordGrammar(grammar, grammar, false, describeFormsTagsOnly);
		} else {
			outputElement.innerHTML = '---';
		}
	}
	static hasFormTags(partName: string): boolean {
		let db: DBTagsGroups = DialogWordGrammar.getInitial().grammarWordTypesGroups[partName.charAt(0)];
		for (let group of db.groups) {
			if (group.formGroup) {
				return true;
			}
		}
		return false;
	}
	private static textToWordGrammar(partName: string, grammar: string, showinUI: boolean, describeFormsTagsOnly: boolean): string {
		let db: DBTagsGroups = DialogWordGrammar.getInitial().grammarWordTypesGroups[partName.charAt(0)];
		let checked = {};
		for (let group of db.groups) {
			checked[group.name] = "";
		}
		let info = "";
		let grindex: number = 0;
		for (let i = 1; i < grammar.length; i++) {
			let group: Group = db.groups[grindex];
			let groupValues = DialogWordGrammar.getGroupValues(group);
			let ch: string = grammar.charAt(i);
			if (ch == '[') {
				let valuesList: string[] = [];
				for (i++; i < grammar.length; i++) {
					ch = grammar.charAt(i);
					if (ch == ']') {
						break;
					} else {
						checked[group.name] += ch;
						valuesList.push(groupValues[ch]);
					}
				}
				if (group.formGroup || !describeFormsTagsOnly) {
					info += group.name + ': ' + valuesList + '\n';
				}
			} else if (ch == '.') {
			} else {
				checked[group.name] += ch;
				if (group.formGroup || !describeFormsTagsOnly) {
					info += group.name + ': ' + groupValues[ch] + '\n';
				}
			}
			grindex++;
		}
		if (showinUI) {
			let cbs: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-wordgrammar-place input[type='checkbox']");
			cbs.forEach(cb => {
				cb.checked = checked[cb.name].indexOf(cb.value) >= 0;
			});
		}
		return info.trim();
	}
	static getGroupValues(group: Group): { [key: string]: string; } {
		let r: { [key: string]: string; } = {};
		for (let item of group.items) {
			r[item.code] = item.description;
		}
		return r;
	}
}
