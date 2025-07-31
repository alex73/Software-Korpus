class DialogText {
	constructor(row: ResultSearchOutRow, target: HTMLElement) {
		let page = row.origText.page;
		const html = $.templates("#template-text").render({
			title: row.subdoc.title.replace("{{page}}", page ? (" (старонка "+page+")") : ""),
			audio: row.origText.audioPreview,
			detailsText: row.origText
		});
		$('#dialog-text').html(html);
		$('#dialog-text').modal('show');
		korpusui.visitedList.push(row.docId);
		target.classList.add('visited');
	}
	cancel() {
		$('#dialog-text').modal('hide');
	}
}
