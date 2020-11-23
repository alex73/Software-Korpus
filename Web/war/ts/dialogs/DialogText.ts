class DialogText {
	constructor(row: ResultSearchOutRow, target: HTMLElement) {
		const html = $.templates("#template-text").render({
			biblio: row.doc,
			detailsText: row.origText
		});
		$('#dialog-text').html(html);
		$('#dialog-text').modal('show');
		target.classList.add('visited');
	}
	fullText(origText: SearchResultsText, doc: TextInfo, docOther: OtherInfo, event) {
		this.biblio = doc;
		this.biblioOther = docOther;
		this.detailsText = origText.words;
		this.textDetailsModal.show();
		event.target.className = "visited";
		return false;
	}
	cancel() {
		$('#dialog-text').modal('hide');
	}
}
