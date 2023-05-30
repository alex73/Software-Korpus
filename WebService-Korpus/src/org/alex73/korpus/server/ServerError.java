package org.alex73.korpus.server;

import java.text.MessageFormat;

/**
 * Service errors for user.
 */
@SuppressWarnings("serial")
public class ServerError extends RuntimeException {

    private ServerError(String msg) {
        super(msg);
    }

    public static ServerError internalError() {
        return new ServerError("Памылка сервера. Звярніцеся да распрацоўшчыкаў");
    }

    public static ServerError tooSimple() {
        return new ServerError("Запыт занадта агульны");
    }

    public static ServerError lemmaNotFound(String lemma) {
        return new ServerError(MessageFormat.format("Лемы ''{0}'' няма ў граматычнай базе, паспрабуйце звычайны ці дакладны пошук", lemma));
    }
}
