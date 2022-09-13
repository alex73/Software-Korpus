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
    let f = new Intl.NumberFormat('be', { maximumSignificantDigits: 3 })
    if (v < 1000) {
        return v.toString();
    } else if (v < 990000) {
        return "~" + f.format(v / 1000) + " " + localization['stat_ths'];
    } else if (v < 990000000) {
        return "~" + f.format(v / 1000000) + " " + localization['stat_mln'];
    } else {
        return "~" + f.format(v / 1000000000) + " " + localization['stat_bln'];
    }
}
