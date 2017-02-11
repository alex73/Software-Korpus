import { WordRequest } from './dto/WordRequest';

export class LatestMark {
    public score: number = null;
    public doc: number = null;
    public shardIndex: number = null;
}

export class TextInfo {
    public authors: string[];
    public title: string;
    public styleGenres: string[];
    public writtenYear: number;
    public publishedYear: number;
}

export class OtherInfo {
    public textURL: string;
}

export class ClusterResult {
    public rows: ClusterRow[];
    constructor(r:ClusterRow[]) {
      this.rows = r;
    }
}

export class ClusterRow {
    public wordsBefore: string[];
    public word: string;
    public wordsAfter: string[];
    public count: number;
}

