class PopoverWord {
	constructor(event) {
		if (event.target) {
			let wlemma = event.target.getAttribute("wlemma");
			let wcat = event.target.getAttribute("wcat");
			let popover: HTMLElement = document.getElementById('popoverWord');
			if (wlemma && wcat) {
				popover.innerHTML = this.html(wlemma, wcat);
				let spanPlace = this.getOffsetRect(event.target);
				let popoverSize = popover.getBoundingClientRect();
				popover.style.top = (spanPlace.top - popoverSize.height) + 'px';
				popover.style.left = (spanPlace.left + spanPlace.width / 2 - popoverSize.width / 2) + 'px';
				popover.style.visibility = 'visible';
				popover.focus();
			} else {
				popover.style.visibility = 'hidden';
			}
		}
	}
	getOffsetRect(elem: HTMLElement) {
		var box = elem.getBoundingClientRect();
		var body = document.body;
		var docElem = document.documentElement;

		var scrollTop = window.pageYOffset || docElem.scrollTop || body.scrollTop;
		var scrollLeft = window.pageXOffset || docElem.scrollLeft || body.scrollLeft;

		var clientTop = docElem.clientTop || body.clientTop || 0;
		var clientLeft = docElem.clientLeft || body.clientLeft || 0;

		var top = box.top + scrollTop - clientTop;
		var left = box.left + scrollLeft - clientLeft;

		return { top: Math.round(top), left: Math.round(left), height: box.height, width: box.width };
	}
	html(lemma: string, cat: string): string {
		let o: string = "";
		if (lemma) {
			let lemmas: string[] = lemma.split('_');
			o += 'Лема:&nbsp;' + lemmas.join(', ');
		}
		if (cat) {
			o += '<br/>Граматыка:';
			for (let c of cat.split('_')) {
				o += '<br/>' + c + ':&nbsp;';
				let oo: string = GrammarInitial.text(c, korpusService.initial.grammar.grammarTree);
				if (oo) {
					o += oo.replace(' ', '&nbsp;');
				}
			}
		}
		return o;
	}
}
