class DialogText {
	constructor(row: ResultSearchOutRow, target: HTMLElement) {
		const html = $.templates("#template-text").render({
			title: row.subdoc.title,
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
