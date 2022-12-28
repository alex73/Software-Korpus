class PopoverBiblio extends BasePopover {
	constructor(event, doc:TextInfo, subdoc: Subtext, page: number) {
		super();
		const html = $.templates("#template-biblio").render({ doc: doc, subdoc: subdoc, page: page });
		$('#dialog-biblio-details').html(html);
		let popover: HTMLElement = document.getElementById('dialog-biblio');

		this.showAt(event.target, 'dialog-biblio', PopoverPlaceVertical.TOP, PopoverPlaceHorizontal.RIGHT);
	}
	leave() {
		$('#dialog-biblio').modal('hide');
	}
}
