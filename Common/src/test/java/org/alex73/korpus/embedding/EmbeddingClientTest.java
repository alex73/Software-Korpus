package org.alex73.korpus.embedding;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmbeddingClientTest {

    @Test
    public void testUrlFormatting() {
        EmbeddingClient c1 = new EmbeddingClient("http://localhost:8080/v1/embeddings", "bge-m3");
        assertEquals(URI.create("http://localhost:8080/v1/embeddings"), c1.getEndpointUri());

        EmbeddingClient c2 = new EmbeddingClient("http://localhost:8080", "bge-m3");
        assertEquals(URI.create("http://localhost:8080/v1/embeddings"), c2.getEndpointUri());

        EmbeddingClient c3 = new EmbeddingClient("http://localhost:11434/", "bge-m3");
        assertEquals(URI.create("http://localhost:11434/v1/embeddings"), c3.getEndpointUri());
    }

    @Test
    public void testJsonParsing() {
        String json = "{\n" +
                "  \"object\": \"list\",\n" +
                "  \"data\": [\n" +
                "    {\n" +
                "      \"object\": \"embedding\",\n" +
                "      \"index\": 1,\n" +
                "      \"embedding\": [0.2, 0.3]\n" +
                "    },\n" +
                "    {\n" +
                "      \"object\": \"embedding\",\n" +
                "      \"index\": 0,\n" +
                "      \"embedding\": [0.1, 0.05]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Gson gson = new Gson();
        EmbeddingClientTestHelper.EmbeddingResponse resp = gson.fromJson(json, EmbeddingClientTestHelper.EmbeddingResponse.class);
        assertNotNull(resp);
        assertEquals(2, resp.data.size());
        assertEquals(1, resp.data.get(0).index);
        assertEquals(0.2f, resp.data.get(0).embedding[0]);
    }

    public static class EmbeddingClientTestHelper {
        public static class EmbeddingResponse {
            List<EmbeddingItem> data;
            static class EmbeddingItem {
                int index;
                float[] embedding;
            }
        }
    }
}
