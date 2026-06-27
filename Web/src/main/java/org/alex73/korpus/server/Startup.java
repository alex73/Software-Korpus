package org.alex73.korpus.server;

import io.javalin.Javalin;
import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.GrammarFinder;
import org.alex73.korpus.base.GrammarDBUtils;
import org.rocksdb.RocksDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Startup {
    private final static Logger LOGGER = LoggerFactory.getLogger(Startup.class);

    private static final int PORT = 8080;

    static void main(String[] args) throws Exception {
        RocksDB.loadLibrary();

        String GRAMMAR_DB = getEnv("GRAMMAR_DB");

        LOGGER.info("Чытаем GRAMMAR_DB з {}", GRAMMAR_DB);
        GrammarDB2 grammarDb = GrammarDB2.initializeFromDir(GRAMMAR_DB);
        GrammarDBUtils.minimizeMemory(grammarDb);
        GrammarFinder grFinder = new GrammarFinder(grammarDb);

        LOGGER.info("Ствараем /");
        ApplicationWeb appWeb = new ApplicationWeb();
        LOGGER.info("Ствараем /korpus");
        ApplicationKorpus appKorpus = new ApplicationKorpus(
                getEnv("KORPUS_CONFIG_DIR"),
                getEnv("KORPUS_CACHE_PATH"),
                getEnv("KORPUS_LANGUAGES"),
                GRAMMAR_DB,
                grammarDb, grFinder);
        LOGGER.info("Ствараем /paralelny");
        ApplicationKorpus appKorpusParallel = new ApplicationKorpus(
                getEnv("PARALELNY_CONFIG_DIR"),
                getEnv("PARALELNY_CACHE_PATH"),
                getEnv("PARALELNY_LANGUAGES"),
                GRAMMAR_DB,
                grammarDb, grFinder);
        LOGGER.info("Ствараем /other");
        ApplicationOther appOther = new ApplicationOther(
                getEnv("KORPUS_CONFIG_DIR"),
                getEnv("KORPUS_CACHE_PATH"),
                getEnv("SYNTH_URL"),
                grammarDb, grFinder);

        Javalin.create(config -> {
            appWeb.registerRoutes(config);
            appKorpus.registerRoutes("/korpus", config.routes);
            appKorpusParallel.registerRoutes("/paralelny", config.routes);
            appOther.registerRoutes("/other", config.routes);
        }).start(PORT);
    }

    static String getEnv(String name) throws Exception {
        String value = System.getenv(name);
        if (value == null) {
            throw new Exception(name + " is not defined");
        }
        return value;
    }
}
