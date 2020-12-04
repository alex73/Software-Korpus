enum PopoverPlaceVertical { TOP, BOTTOM };
enum PopoverPlaceHorizontal { LEFT, CENTER, RIGHT };

class BasePopover {
	showAt(elem: HTMLElement, id: string, placeVertical: PopoverPlaceVertical, placeHorizontal: PopoverPlaceHorizontal) {
		let popover: HTMLElement = document.getElementById(id);
		let spanPlace = this.getOffsetRect(elem);
		let popoverSize = popover.getBoundingClientRect();
		switch (placeVertical) {
			case PopoverPlaceVertical.TOP:
				popover.style.top = (spanPlace.top - popoverSize.height) + 'px';
				break;
			case PopoverPlaceVertical.BOTTOM:
				popover.style.top = spanPlace.top + 'px';
				break;
		}
		switch (placeHorizontal) {
			case PopoverPlaceHorizontal.LEFT:
				popover.style.left = (spanPlace.left - popoverSize.width) + 'px';
				break;
			case PopoverPlaceHorizontal.CENTER:
				popover.style.left = (spanPlace.left + spanPlace.width / 2 - popoverSize.width / 2) + 'px';
				break;
			case PopoverPlaceHorizontal.RIGHT:
				popover.style.left = (spanPlace.left) + 'px';
				break;
		}
		popover.style.visibility = 'visible';
		popover.focus();
	}
	getOffsetRect(elem: HTMLElement) {
		var box = elem.getBoundingClientRect();
		var body = document.body;
		var docElem = document.documentElement;

		var scrollTop = window.pageYOffset || docElem.scrollTop || body.scrollTop;
		var scrollLeft = window.pageXOffset || docElem.scrollLeft || body.scrollLeft;

		var clientTop = docElem.clientTop || body.clientTop || 0;
		var clientLeft = docElem.clientLeft || body.clientLeft || 0;

		var top = box.top + scrollTop - clientTop;
		var left = box.left + scrollLeft - clientLeft;

		return { top: Math.round(top), left: Math.round(left), height: box.height, width: box.width };
	}
}
