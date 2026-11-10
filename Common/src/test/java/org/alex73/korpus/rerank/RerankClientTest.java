package org.alex73.korpus.rerank;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RerankClientTest {

    @Test
    public void testUrlFormatting() {
        RerankClient c1 = new RerankClient("http://localhost:4000/v1/rerank", "rerank-model");
        assertEquals(URI.create("http://localhost:4000/v1/rerank"), c1.getEndpointUri());

        RerankClient c2 = new RerankClient("http://localhost:4000", "rerank-model");
        assertEquals(URI.create("http://localhost:4000/v1/rerank"), c2.getEndpointUri());

        RerankClient c3 = new RerankClient("http://localhost:4000/", "rerank-model");
        assertEquals(URI.create("http://localhost:4000/v1/rerank"), c3.getEndpointUri());

        RerankClient c4 = new RerankClient("http://localhost:4000/rerank", "rerank-model");
        assertEquals(URI.create("http://localhost:4000/rerank"), c4.getEndpointUri());
    }

    @Test
    public void testJsonParsing() {
        String json = "{\n" +
                "  \"id\": \"rerank-123\",\n" +
                "  \"results\": [\n" +
                "    {\n" +
                "      \"index\": 1,\n" +
                "      \"relevance_score\": 0.95\n" +
                "    },\n" +
                "    {\n" +
                "      \"index\": 0,\n" +
                "      \"relevance_score\": 0.32\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Gson gson = new Gson();
        RerankResponse resp = gson.fromJson(json, RerankResponse.class);
        assertNotNull(resp);
        assertEquals(2, resp.results.size());
        assertEquals(1, resp.results.get(0).index);
        assertEquals(0.95, resp.results.get(0).relevanceScore);
        assertEquals(0, resp.results.get(1).index);
        assertEquals(0.32, resp.results.get(1).relevanceScore);
    }

    private static class RerankResponse {
        List<RerankItem> results;
        private static class RerankItem {
            int index;
            @com.google.gson.annotations.SerializedName("relevance_score")
            double relevanceScore;
        }
    }
}
