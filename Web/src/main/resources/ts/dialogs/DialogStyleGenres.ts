class DialogStyleGenres {
	constructor() {
		const html = $.templates("#styleGenresCards").render({ initial: korpusService.initial });
		$('#dialog-sg-place').html(html);
		let fs: string[] = document.getElementById('inputFilterStyle').innerText.split(';');
		fs.forEach(v => {
			let cb = <HTMLInputElement>document.querySelector("#dialog-sg input[type='checkbox'][name='" + v + "']");
			if (cb) {
				cb.checked = true;
			}
		});
		$('#dialog-sg').modal('show');
	}
	onOk() {
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-sg input[type='checkbox']");
		let selectedStyleGenres = [];
		var hasUnchecked = false;
		collection.forEach(cb => {
			if (cb.name != '') {
				if (cb.checked) {
					selectedStyleGenres.push(cb.name);
				} else {
					hasUnchecked = true;
				}
			}
		});
		if (!hasUnchecked) {
			selectedStyleGenres = [];
		}
		document.getElementById('inputFilterStyle').innerText = selectedStyleGenres.length == 0 ? "Усе" : selectedStyleGenres.join(';');

		$('#dialog-sg').modal('hide');
	}
	onCancel() {
		$('#dialog-sg').modal('hide');
	}
	all(v: boolean) {
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-sg input[type='checkbox']");
		collection.forEach(cb => cb.checked = v);
	}
	group(cb: HTMLInputElement) {
		const card: HTMLElement = cb.closest(".card");
		var collection: NodeListOf<HTMLInputElement> = card.querySelectorAll("input[type='checkbox']");
		collection.forEach(i => i.checked = cb.checked);
	}
}
