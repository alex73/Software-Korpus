class PopoverWord extends BasePopover {
	constructor(event) {
		super();
		if (event.target) {
			let wlemma = event.target.getAttribute("wlemma");
			let wcat = event.target.getAttribute("wcat");
			let popover: HTMLElement = document.getElementById('popoverWord');
			if (wlemma && wcat) {
				document.getElementById('popoverWordContent').innerHTML = this.html(wlemma, wcat);
				this.showAt(event.target, 'popoverWord', PopoverPlaceVertical.TOP, PopoverPlaceHorizontal.CENTER);
			} else {
				popover.style.visibility = 'hidden';
			}
		}
	}
	html(lemma: string, cat: string): string {
		let o: string = "";
		if (lemma) {
			let lemmas: string[] = lemma.split(';');
			o += '<b>Пачатковая форма</b>:&nbsp;' + lemmas.join(', ');
		}
		if (cat) {
			o += '<br/><b>Граматыка</b>:';
			for (let c of cat.split(';')) {
				o += '<br/>' + c + ':&nbsp;';
				let oo: KeyValue[] = Grammar.parseCode(korpusService.initial.grammar, c);
				if (oo) {
					o += oo.map(kv=>kv.value).join(', ').replace(' ', '&nbsp;');
				}
			}
		}
		return o;
	}
	
}

function popoverWordHide() {
	let popover: HTMLElement = document.getElementById('popoverWord');
	popover.style.visibility = 'hidden';
}
