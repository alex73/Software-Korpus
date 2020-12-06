class DialogSubcorpuses {
	constructor() {
		const html = $.templates("#template-subcorpuses").render({ subcorpuses: korpusService.initial.subcorpuses });
		$('#dialog-subcorpuses-place').html(html);
		let fs: string[] = document.getElementById('inputFilterCorpus').innerText.split(';');
		fs.forEach(v => {
			let cb = <HTMLInputElement>document.querySelector("#dialog-subcorpuses input[type='checkbox'][name='" + v + "']");
			if (cb) {
				cb.checked = true;
			}
		});
		$('#dialog-subcorpuses').modal('show');
	}
	onOk() {
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-subcorpuses input[type='checkbox']");
		let subcorpuses = [];
		var hasUnchecked = false;
		collection.forEach(cb => {
			if (cb.checked) {
				subcorpuses.push(cb.name);
			} else {
				hasUnchecked = true;
			}
		});
		if (!hasUnchecked) {
			subcorpuses = [];
		}
		document.getElementById('inputFilterCorpus').innerText = subcorpuses.length == 0 ? "Усе" : subcorpuses.join(';');
		$('#dialog-subcorpuses').modal('hide');
	}
	onCancel() {
		$('#dialog-subcorpuses').modal('hide');
	}
	all(v: boolean) {
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-subcorpuses input[type='checkbox']");
		collection.forEach(cb => cb.checked = v);
	}
}
