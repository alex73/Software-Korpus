declare var $: any;

interface ModalDialog {
	modal?: () => any;
}

class KorpusService {
	public initial: InitialData = new InitialData();

	constructor() {
		fetch('rest/korpus/initial')
			.then(data => data.json())
			.then(v => this.initial = v);
	}
	addWord(type: String) {
		const templateWord: HTMLElement = document.getElementById("template-" + type);
		const newWord: HTMLElement = <HTMLElement>templateWord.cloneNode(true);
		newWord.removeAttribute('id');
		newWord.style.display = 'block';
		templateWord.parentElement.insertBefore(newWord, templateWord);
		this.repaintRemoveIcons(type);
	}
	removeWord(type: String, button: HTMLElement) {
		const block: HTMLElement = button.closest("." + type);
		block.remove();
		this.repaintRemoveIcons(type);
	}
	repaintRemoveIcons(type: String) {
		var collection: NodeListOf<HTMLElement> = document.querySelectorAll("." + type + ":not(#template-" + type + ")");
		collection.forEach(el => (<NodeListOf<HTMLElement>>el.querySelectorAll("." + type + "-remove")).forEach(er => er.style.display = collection.length > 1 ? 'block' : 'none'));
	}
	showDialogStyleGenres() {
		$('#dialog-sg').modal();
	}
}

let korpusService = new KorpusService();
korpusService.addWord('inputword');
