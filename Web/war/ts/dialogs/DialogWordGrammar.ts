class DialogWordGrammar {
	private currentWordElement: HTMLElement;

	constructor(inner: HTMLElement) {
		this.currentWordElement = inner.closest(".card");
		const html = $.templates("#template-wordgrammar").render({ grammar: korpusService.initial.grammar });
		let grammar = this.currentWordElement.querySelector(".wordgram-grammar-string").textContent;
		$('#dialog-wordgrammar-place').html(html);
		if (grammar) {
			(<HTMLSelectElement>document.getElementById("dialog-wordgrammar-grselected")).value = grammar.charAt(0);
			this.changeType(grammar.charAt(0));
			KorpusUI.textToWordGrammar(grammar);
		}
		$('#dialog-wordgrammar').modal('show');
	}
	changeType(v: string) {
		const html = $.templates("#template-wordgrammar2").render({ grammar: korpusService.initial.grammar, grselected: v });
		$('#dialog-wordgrammar-place2').html(html);
	}
	onOk() {
		let gr: string = this.toString();
		this.currentWordElement.querySelector(".wordgram-grammar-string").textContent = gr;
		this.currentWordElement.querySelector(".wordgram-display").textContent = KorpusUI.wordGrammarToText(gr);

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
		let db: DBTagsGroups = korpusService.initial.grammar.grammarWordTypesGroups[grselected];

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
}
