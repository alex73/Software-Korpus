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

    public static ServerError rusForms() {
        return new ServerError("Запыт для рускай мовы не можа выкарыстоўваць словаформы ці граматыку. Абярыце іншы рэжым.");
    }

    public static ServerError rusVariants() {
        return new ServerError("Запыт для рускай мовы не можа адбывацца з варыянтамі напісання. Абярыце іншы рэжым.");
    }

    public static ServerError noWord() {
        return new ServerError("Слова не ўведзена");
    }

    public static ServerError noWordButGrammar() {
        return new ServerError("Слова не ўведзена. Уключыце рэжым 'Толькі граматыка'");
    }
}
