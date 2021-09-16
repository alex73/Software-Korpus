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

function roundnum(v: number): string {
    let f = new Intl.NumberFormat('be', { maximumSignificantDigits: 2 })
    if (v <= 100) {
        return v.toString();
    } else if (v < 900) {
        return "~" + f.format(v);;
    } else if (v < 990000) {
        return "~" + f.format(v / 1000) + " тыс.";
    } else if (v < 990000000) {
        return "~" + f.format(v / 1000000) + " млн.";
    } else {
        return "~" + f.format(v / 1000000000) + " млрд.";
    }
}
