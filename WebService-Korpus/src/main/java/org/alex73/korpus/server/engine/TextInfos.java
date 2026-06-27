package org.alex73.korpus.server.engine;

import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.alex73.korpus.base.TextInfo;
import org.rocksdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TextInfos {
    private final RocksDB readDb;
    private final ColumnFamilyHandle textInfosCf;
    private final ColumnFamilyHandle authorsByLemmasCf;
    private final Gson gson = new Gson();

    public TextInfos(String filePath) throws Exception {
        List<ColumnFamilyHandle> cfHandles = new ArrayList<>();

        BlockBasedTableConfig tableConfig = new BlockBasedTableConfig();
        tableConfig.setBlockSize(4096);
        tableConfig.setBlockCache(new LRUCache(64 * 1024 * 1024)); // 64 MB cache
        tableConfig.setFilterPolicy(new BloomFilter(10, false)); // Bloom filter with 10 bits per key
        tableConfig.setCacheIndexAndFilterBlocks(true);
        tableConfig.setPinTopLevelIndexAndFilter(true);

        ColumnFamilyOptions cfOptions = new ColumnFamilyOptions().setTableFormatConfig(tableConfig);

        DBOptions options = new DBOptions().setCreateIfMissing(false).setMaxOpenFiles(-1);
        readDb = RocksDB.openReadOnly(options, filePath,
                List.of(
                        new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOptions),
                        new ColumnFamilyDescriptor("textInfos".getBytes(UTF_8), cfOptions),
                        new ColumnFamilyDescriptor("authorsByLemmas".getBytes(UTF_8), cfOptions)
                ),
                cfHandles);

        textInfosCf = cfHandles.get(1);
        authorsByLemmasCf = cfHandles.get(2);
    }

    public TextInfo getTextInfo(int id) {
        try {
            byte[] val = readDb.get(textInfosCf, Ints.toByteArray(id));
            if (val == null) return null;
            return gson.fromJson(new String(val, UTF_8), TextInfo.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Set<String> getAuthors(String lemma) {
        try {
            byte[] val = readDb.get(authorsByLemmasCf, lemma.getBytes(UTF_8));
            if (val == null) return null;
            return gson.fromJson(new String(val, UTF_8), new TypeToken<Set<String>>() {
            }.getType());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
