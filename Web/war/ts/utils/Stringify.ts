function stringify(v: Object) {
	return JSON.stringify(v, (key, value) => {
		if (value) return value
	});
}
