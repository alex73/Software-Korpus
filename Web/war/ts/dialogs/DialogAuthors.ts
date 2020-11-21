class DialogAuthors {
	constructor() {
		const html = $.templates("#template-authors").render({ authors: korpusService.initial.authors });
		$('#dialog-authors-place').html(html);
		let fs: string[] = document.getElementById('inputFilterAuthor').innerText.split(';');
		fs.forEach(v => {
			let cb = <HTMLInputElement>document.querySelector("#dialog-authors input[type='checkbox'][name='" + v + "']");
			if (cb) {
				cb.checked = true;
			}
		});
		$('#dialog-authors').modal('show');
	}
	onOk() {
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-authors input[type='checkbox']");
		let authors = [];
		var hasUnchecked = false;
		collection.forEach(cb => {
			if (cb.checked) {
				authors.push(cb.name);
			} else {
				hasUnchecked = true;
			}
		});
		if (!hasUnchecked) {
			authors = [];
		}
		document.getElementById('inputFilterAuthor').innerText = authors.length == 0 ? "Усе" : authors.join(';');
		$('#dialog-authors').modal('hide');
	}
	onCancel() {
		$('#dialog-authors').modal('hide');
	}
	all(v: boolean) {
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-authors input[type='checkbox']");
		collection.forEach(cb => cb.checked = v);
	}
}
