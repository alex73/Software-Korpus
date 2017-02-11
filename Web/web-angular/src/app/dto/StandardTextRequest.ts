export class StandardTextRequest {
    public author: string = null;
    public stylegenres: string[] = [];
    public yearWrittenFrom: string = null;
    public yearWrittenTo: string = null;
    public yearPublishedFrom: string = null;
    public yearPublishedTo: string = null;
    
    exportParameters() {
        let out: string = "";

        out += this.out("author", this.author);
        out += this.out("yearPublishedFrom", this.yearPublishedFrom);
        out += this.out("yearPublishedTo", this.yearPublishedTo);
        out += this.out("yearWrittenFrom", this.yearWrittenFrom);
        out += this.out("yearWrittenTo", this.yearWrittenTo);
        out += this.out("stylegenres", this.stylegenres.join(';'));
        
        return out;
    }
    importParameters(key: string, value: string) {
      switch(key) {
        case "author":
          this.author = value;
          break;
        case "yearPublishedFrom":
          this.yearPublishedFrom = value;
          break;
        case "yearPublishedTo":
          this.yearPublishedTo = value;
          break;
        case "yearWrittenFrom":
          this.yearWrittenFrom = value;
          break;
        case "yearWrittenTo":
          this.yearWrittenTo = value;
          break;
        case "stylegenres":
          this.stylegenres = value.split(';');
          break;
      }
    }
    private out(name: string, txt: any): string {
        return txt ? '&'+name+'='+txt : "";
    }
    clone(): StandardTextRequest {
      let r = new StandardTextRequest();
      r.author = this.author;
      r.stylegenres = this.stylegenres.slice(0);
      r.yearWrittenFrom = this.yearWrittenFrom;
      r.yearWrittenTo = this.yearWrittenTo;
      r.yearPublishedFrom = this.yearPublishedFrom;
      r.yearPublishedTo = this.yearPublishedTo;
      return r;
    }
}
