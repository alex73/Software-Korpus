package org.alex73.korpus.rerank;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerankClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(RerankClient.class);

    private static final Gson GSON = new Gson();

    private final URI endpointUri;
    private final String model;
    private final String apiKey;
    private final HttpClient httpClient;

    protected RerankClient(String endpointUrl, String model) {
        this(endpointUrl, model, null, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public RerankClient(String endpointUrl, String model, String apiKey, HttpClient httpClient) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            throw new IllegalArgumentException("Endpoint URL cannot be empty");
        }
        String url = endpointUrl.trim();
        if (!url.endsWith("/rerank")) {
            if (url.endsWith("/")) {
                url = url + "v1/rerank";
            } else {
                url = url + "/v1/rerank";
            }
        }
        this.endpointUri = URI.create(url);
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Model cannot be empty");
        }
        this.model = model;
        this.apiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey : null;

        LOGGER.info("Initialized RerankClient with URL: " + url + ", model: " + this.model);

        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public List<RerankResult> rerank(String query, List<String> documents) throws IOException, InterruptedException {
        return rerank(query, documents, null);
    }

    public synchronized List<RerankResult> rerank(String query, List<String> documents, Integer topN) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be blank");
        }
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<String> sanitized = documents.stream()
                .map(d -> (d == null || d.isBlank()) ? " " : d)
                .toList();

        RerankRequest reqBody = new RerankRequest(model, query, sanitized, topN);
        String json = GSON.toJson(reqBody);
        System.out.println("Rerank request JSON: " + json);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(endpointUri)
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (apiKey != null) {
            reqBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Rerank service returned HTTP " + response.statusCode() + ": " + response.body());
        }

        RerankResponse respObj = GSON.fromJson(response.body(), RerankResponse.class);
        if (respObj == null || respObj.results == null) {
            throw new IOException("Invalid response structure from " + endpointUri + ": " + response.body());
        }
        System.out.println("Rerank response: " + response.body());

        List<RerankResult> results = new ArrayList<>(respObj.results.size());
        for (RerankResponse.RerankItem item : respObj.results) {
            results.add(new RerankResult(item.index, item.relevanceScore));
        }

        results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
        return results;
    }

    public String getModel() {
        return model;
    }

    public URI getEndpointUri() {
        return endpointUri;
    }

    private static class RerankRequest {
        @SerializedName("model")
        String model;
        @SerializedName("query")
        String query;
        @SerializedName("documents")
        List<String> documents;
        @SerializedName("top_n")
        Integer topN;

        public RerankRequest(String model, String query, List<String> documents, Integer topN) {
            this.model = model;
            this.query = query;
            this.documents = documents;
            this.topN = topN;
        }
    }

    private static class RerankResponse {
        @SerializedName("results")
        List<RerankItem> results;

        private static class RerankItem {
            @SerializedName("index")
            int index;
            @SerializedName("relevance_score")
            double relevanceScore;
        }
    }
}
