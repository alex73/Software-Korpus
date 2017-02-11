import { GrammarInitial } from './GrammarInitial';

export class InitialData {
    public authors: string[] = [];
    public volumes: string[] = [];
    public statKorpus: StatLine[] = [];
    public statOther: StatLine[] = [];
    public styleGenresParts: string[] = [];
    public styleGenres: { [key:string]:string[]; } = {};
    public grammar: GrammarInitial = new GrammarInitial();
}

export class StatLine {
    public name: string;
    public texts: number;
    public words: number;
}
