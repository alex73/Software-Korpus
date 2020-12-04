function stringify(v: Object) {
	return JSON.stringify(v, (key, value) => {
		if (value) return value
	});
}

function fulltrim(s: string): string {
	if (s) {
		s = s.trim();
	}
	return s ? s : null;
}
