class BaseParams {
	public lang: string;
    public textStandard: StandardTextRequest = new StandardTextRequest();
}

class SearchParams extends BaseParams {
    public words: WordRequest[];
    public wordsOrder: string;
}

class ClusterParams extends BaseParams {
    public word: WordRequest;
    public wordsBefore: number;
    public wordsAfter: number;
}
