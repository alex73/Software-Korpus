package org.alex73.korpus.compiler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.utils.KorpusFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessHeaders extends BaseParallelProcessor<MessageParsedText> {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessHeaders.class);

    public List<TextInfo> textInfos = Collections.synchronizedList(new ArrayList<>());
    public Set<String> allLanguages = Collections.synchronizedSet(new TreeSet<>());

    public ProcessHeaders() {
        super(1, 20);
    }

    @Override
    public void accept(MessageParsedText text) {
        if (text.textInfo.sourceFilePath == null) {
            throw new RuntimeException("sourceFilePath not defined");
        }
        if (text.textInfo.subcorpus == null) {
            throw new RuntimeException("subcorpus not defined in the " + text.textInfo.sourceFilePath);
        }

        run(() -> {
            for (TextInfo.Subtext st : text.textInfo.subtexts) {
                st.creationTimeLatest();
                st.publicationTimeLatest();
                ProcessTexts.preprocessor.preprocess(text);
                if (st.label == null || st.passport == null) {
                    throw new RuntimeException("label ці пашпарт нявызначаныя ў " + text.textInfo.sourceFilePath);
                }
                if (!allLanguages.contains(st.lang)) {
                    // check contains for better performance
                    allLanguages.add(st.lang);
                }
            }
            textInfos.add(text.textInfo);
        });
    }

    Map<String, Integer> calcTextsPositions(Path outputFile) throws Exception {
        LOG.info("Sorting {} text infos...", textInfos.size());
        // sort
        Collections.sort(textInfos, ProcessTexts.preprocessor.getTextsComparator());

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

        KorpusFileUtils.writeGzip(outputFile, textInfos.stream().map(ti -> {
            try {
                return objectMapper.writeValueAsString(ti);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }));

        textInfos = null;

        return result;
    }
}
