class DialogList {
	private controlId: string;
	constructor(list: string[], controlId: string, title: string) {
		this.controlId = controlId;
		const html = $.templates("#template-list").render({
			list: list,
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
}
