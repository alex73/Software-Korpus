package org.alex73.korpus.compiler;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.korpus.base.TextInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessHeaders extends BaseParallelProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessHeaders.class);

    private static ProcessHeaders instance;

    public static List<TextInfo> textInfos = Collections.synchronizedList(new ArrayList<>());

    public ProcessHeaders() {
        super(1, 20);
        instance = this;
    }

    public static void process(TextInfo textInfo) {
        if (textInfo.sourceFilePath == null) {
            throw new RuntimeException("sourceFilePath нявызначаны");
        }
        if (textInfo.subcorpus == null) {
            throw new RuntimeException("subcorpus нявызначаны ў " + textInfo.sourceFilePath);
        }
        if (textInfo.title == null) {
            throw new RuntimeException("title нявызначаны ў " + textInfo.sourceFilePath);
        }

        instance.run(() -> {
            textInfo.creationTimeLatest();
            textInfo.publicationTimeLatest();
            textInfos.add(textInfo);
        });
    }

    static Map<String, Integer> calcTextsPositions(Path outputFile) throws Exception {
        LOG.info("Sorting {} text infos...", textInfos.size());
        // sort
        Collections.sort(textInfos, new TextOrder());

        LOG.info("Storing text positions...");
        // remember positions
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < textInfos.size(); i++) {
            if (result.put(textInfos.get(i).sourceFilePath, i) != null) {
                throw new Exception("Text " + textInfos.get(i).sourceFilePath + " produced twice !!!");
            }
        }

        LOG.info("Writing texts infos to file...");
        // write to json
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        try (BufferedWriter wr = Files.newBufferedWriter(outputFile)) {
            for (TextInfo ti : textInfos) {
                wr.write(objectMapper.writeValueAsString(ti));
                wr.write('\n');
            }
        }

        textInfos = null;

        return result;
    }
}
