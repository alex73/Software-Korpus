export class UnprocessedTextRequest {
    public volume: string = null;
    
    exportParameters() {
        let out: string = "";

        out += this.out("volume", this.volume);
        
        return out;
    }
    importParameters(key: string, value: string) {
      switch(key) {
        case "volume":
          this.volume = value;
          break;
      }
    }
    private out(name: string, txt: any): string {
        return txt ? '&'+name+'='+txt : "";
    }
    clone(): UnprocessedTextRequest {
      let r = new UnprocessedTextRequest();
      r.volume = this.volume;
      return r;
    }
}
