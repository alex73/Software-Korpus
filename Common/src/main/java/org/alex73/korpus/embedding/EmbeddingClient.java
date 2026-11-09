package org.alex73.korpus.embedding;

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

public class EmbeddingClient {
    private static final Gson GSON = new Gson();

    private final URI endpointUri;
    private final String model;
    private final String apiKey;
    private final HttpClient httpClient;

    public EmbeddingClient(String endpointUrl, String model) {
        this(endpointUrl, model, null, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public EmbeddingClient(String endpointUrl, String model, String apiKey, HttpClient httpClient) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            throw new IllegalArgumentException("Endpoint URL cannot be empty");
        }
        String url = endpointUrl.trim();
        if (!url.endsWith("/embeddings")) {
            if (url.endsWith("/")) {
                url = url + "v1/embeddings";
            } else {
                url = url + "/v1/embeddings";
            }
        }
        this.endpointUri = URI.create(url);
        this.model = (model != null && !model.isBlank()) ? model : "BAAI/bge-m3";
        this.apiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey : null;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public static final int DEFAULT_BATCH_SIZE = 16;

    public float[] getEmbedding(String text) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be blank");
        }
        List<float[]> results = getEmbeddings(List.of(text));
        if (results.isEmpty()) {
            throw new IOException("Empty embedding response from " + endpointUri);
        }
        return results.get(0);
    }

    public List<float[]> getEmbeddings(List<String> texts) throws IOException, InterruptedException {
        return getEmbeddings(texts, DEFAULT_BATCH_SIZE);
    }

    public List<float[]> getEmbeddings(List<String> texts, int batchSize) throws IOException, InterruptedException {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        if (texts.size() <= batchSize) {
            return getEmbeddingsBatch(texts);
        }
        List<float[]> all = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += batchSize) {
            int to = Math.min(i + batchSize, texts.size());
            all.addAll(getEmbeddingsBatch(texts.subList(i, to)));
        }
        return all;
    }

    // synchronized to avoid concurrent requests to the same endpoint, which may cause rate limiting or other issues
    private synchronized List<float[]> getEmbeddingsBatch(List<String> texts) throws IOException, InterruptedException {
        List<String> sanitized = texts.stream()
                .map(t -> (t == null || t.isBlank()) ? " " : t)
                .toList();
        EmbeddingRequest reqBody = new EmbeddingRequest(model, sanitized);
        String json = GSON.toJson(reqBody);

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
            throw new IOException("Embedding service returned HTTP " + response.statusCode() + ": " + response.body());
        }

        EmbeddingResponse respObj = GSON.fromJson(response.body(), EmbeddingResponse.class);
        if (respObj == null || respObj.data == null) {
            throw new IOException("Invalid response structure from " + endpointUri + ": " + response.body());
        }

        List<float[]> embeddings = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            embeddings.add(null);
        }

        for (EmbeddingResponse.EmbeddingItem item : respObj.data) {
            if (item.index >= 0 && item.index < texts.size()) {
                embeddings.set(item.index, item.embedding);
            }
        }

        for (int i = 0; i < texts.size(); i++) {
            if (embeddings.get(i) == null) {
                throw new IOException("Missing embedding for item at index " + i);
            }
        }

        return embeddings;
    }

    public String getModel() {
        return model;
    }

    public URI getEndpointUri() {
        return endpointUri;
    }

    private static class EmbeddingRequest {
        @SerializedName("model")
        String model;
        @SerializedName("input")
        List<String> input;

        public EmbeddingRequest(String model, List<String> input) {
            this.model = model;
            this.input = input;
        }
    }

    private static class EmbeddingResponse {
        @SerializedName("data")
        List<EmbeddingItem> data;

        private static class EmbeddingItem {
            @SerializedName("index")
            int index;
            @SerializedName("embedding")
            float[] embedding;
        }
    }
}
