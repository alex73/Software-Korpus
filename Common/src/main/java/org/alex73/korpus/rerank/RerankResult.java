package org.alex73.korpus.rerank;

public class RerankResult {
    private final int index;
    private final double relevanceScore;

    public RerankResult(int index, double relevanceScore) {
        this.index = index;
        this.relevanceScore = relevanceScore;
    }

    public int getIndex() {
        return index;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    @Override
    public String toString() {
        return "RerankResult{index=" + index + ", score=" + relevanceScore + "}";
    }
}
