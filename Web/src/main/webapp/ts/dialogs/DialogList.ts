class DialogList {
	private controlId: string;
	constructor(list: { [key: string]: string[]; }, controlId: string, title: string) {
		this.controlId = controlId;
		let selectedSubcorpuses: string[] = document.getElementById('inputFilterCorpus').innerText.split(';');
		let fullList: string[] = [];
		if (!selectedSubcorpuses) {
			selectedSubcorpuses = Object.keys(list);
		}
		for (let s of selectedSubcorpuses) {
			if (list[s]) {
				fullList = fullList.concat(list[s]);
			}
		}
		let BE = new Intl.Collator('be');
		fullList = fullList.filter((item, index) => {
			return (fullList.indexOf(item) == index)
		}).sort((a, b) => BE.compare(a, b));
		const html = $.templates("#template-list").render({
			list: fullList,
			title: title
		});
		$('#dialog-list-place').html(html);
		let fs: string[] = document.getElementById(this.controlId).innerText.split(';');
		fs.forEach(v => {
			let cb = <HTMLInputElement>document.querySelector("#dialog-list input[type='checkbox'][name='" + v + "']");
			if (cb) {
				cb.checked = true;
			}
		});
		$('#dialog-list').modal('show');
	}
	onOk() {
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-list input[type='checkbox']");
		let result = [];
		var hasUnchecked = false;
		collection.forEach(cb => {
			if (cb.checked) {
				result.push(cb.name);
			} else {
				hasUnchecked = true;
			}
		});
		if (!hasUnchecked) {
			result = [];
		}
		document.getElementById(this.controlId).innerText = result.length == 0 ? "Усе" : result.join(';');
		$('#dialog-list').modal('hide');
	}
	onCancel() {
		$('#dialog-list').modal('hide');
	}
	all(v: boolean) {
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-list input[type='checkbox']");
		collection.forEach(cb => cb.checked = v);
	}
	filter(elem: HTMLInputElement) {
		let s: string = elem.value.trim().toLowerCase();
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-list input[type='checkbox']");
		collection.forEach(cb => {
			cb.closest('div').style.display = cb.checked || cb.name.toLowerCase().indexOf(s) >= 0 ? "block" : "none";
		});
	}
}
