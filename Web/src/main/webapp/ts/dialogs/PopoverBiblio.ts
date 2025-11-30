class PopoverBiblio extends BasePopover {
	constructor(event, doc:TextInfo, subdoc: Subtext, page: number) {
		super();
		
		let html = subdoc.passport;
		html = html.replace("{{page}}", page ? (" (старонка "+page+")") : "");
		korpusService.initial.subcorpuses.forEach(kv => html = html.replace("{{subcorpus:"+kv.key+"}}", kv.value.replace(/\|\|.+/g, '')));
		
		$('#dialog-biblio-details').html(html);
		let popover: HTMLElement = document.getElementById('dialog-biblio');

		this.showAt(event.target, 'dialog-biblio', PopoverPlaceVertical.TOP, PopoverPlaceHorizontal.RIGHT);
	}
	leave() {
		$('#dialog-biblio').modal('hide');
	}
}
