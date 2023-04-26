enum WordMode {
	USUAL = 'USUAL',
	ALL_FORMS = 'ALL_FORMS',
	EXACT = 'EXACT',
	GRAMMAR = 'GRAMMAR'
};

class ChainRequest {
	public words: WordRequest[];
	public seps: string[];
}

class WordRequest {
	public mode: string;
	public variants: boolean;
	public word: string = "";
	public regexp: boolean;
	public grammar: string = null;
}
