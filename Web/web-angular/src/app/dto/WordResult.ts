export class WordResult {
    public orig: string;
    public normalized: string;
    public cat: string;
    public lemma: string;
    /** True if word is requested by user, i.e. should be marked in output. */
    public requestedWord: boolean;
    public isWord: boolean;
    
    constructor(private text: string) {
      this.orig = text;
    }
}
