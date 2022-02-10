class InitialData {
    public subcorpuses: KeyValue[];
    public authors: { [key:string]:string[]; };
    public sources: { [key:string]:string[]; };
    public styleGenresParts: string[];
    public styleGenres: { [key:string]:string[]; };
    public grammar: GrammarInitial;
    public stat: InitialDataStat[];
    public kankardansnyjaSpisy: string[];
}

class InitialDataStat {
    public name: string;
    public texts: number;
    public words: number;
}
