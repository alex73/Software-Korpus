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
		collection.forEach(cb => {
			if (cb.checked) {
				subcorpuses.push(cb.name);
			}
		});
		document.getElementById('inputFilterCorpus').innerText = subcorpuses.length == 0 ? korpusService.initial.preselectedSubcorpuses : subcorpuses.join(';');
		korpusui.showSubcorpusNames();
		DialogSubcorpuses.showInputFilterDependsOnSubcorpus();
		$('#dialog-subcorpuses').modal('hide');
	}
	onCancel() {
		$('#dialog-subcorpuses').modal('hide');
	}
	all(v: boolean) {
		var collection: NodeListOf<HTMLInputElement> = document.querySelectorAll("#dialog-subcorpuses input[type='checkbox']");
		collection.forEach(cb => cb.checked = v);
	}
	static showInputFilterDependsOnSubcorpus() {
		let subcorpuses: string[] = KorpusUI.separatedStringToArray(document.getElementById('inputFilterCorpus').innerText);
		if (subcorpuses) {
			$('#inputFilterAuthorShow').hide();
			$('#inputFilterStyleShow').hide();
			$('#inputFilterSourceShow').hide();
			$('#inputFilterYearWrittenShow').hide();
			$('#inputFilterYearPublishedShow').hide();
			if (subcorpuses.indexOf('teksty') >= 0 || subcorpuses.indexOf('pieraklady') >= 0) {
				$('#inputFilterAuthorShow').show();
				$('#inputFilterStyleShow').show();
				$('#inputFilterYearWrittenShow').show();
				$('#inputFilterYearPublishedShow').show();
			}
			if (subcorpuses.indexOf('sajty') >= 0) {
				$('#inputFilterSourceShow').show();
				$('#inputFilterYearPublishedShow').show();
			}
            if (subcorpuses.indexOf('nierazabranaje') >= 0) {
                $('#inputFilterSourceShow').show();
            }
		} else {
			$('#inputFilterAuthorShow').show();
			$('#inputFilterStyleShow').show();
			$('#inputFilterSourceShow').show();
			$('#inputFilterYearWrittenShow').show();
			$('#inputFilterYearPublishedShow').show();
		}
	}
}
