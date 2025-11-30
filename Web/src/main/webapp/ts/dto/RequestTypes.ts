class BaseParams {
	public lang: string;
    public textStandard: StandardTextRequest = new StandardTextRequest();
}

class SearchParams extends BaseParams {
    public chainsInParagraph: boolean;
    public chains: ChainRequest[];
}

class ClusterParams extends BaseParams {
    public word: WordRequest;
    public wordsBefore: number;
    public wordsAfter: number;
}
