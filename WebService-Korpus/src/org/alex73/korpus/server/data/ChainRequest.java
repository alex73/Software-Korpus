package org.alex73.korpus.server.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
/**
 * List of fixed order of words. Chains doesn't cross sentence border even in
 * case chainsInParagraph=true.
 */
public class ChainRequest implements Serializable {
    public List<WordRequest> words = new ArrayList<>();
    /**
     * Спіс знакаў прыпынку. Магчымыя значэнні адрозніваюцца для самага першага, у
     * сярэдзіне, і самага апошняга.
     * 
     * Колькасць элементаў мусіць быць "words.size()+1". Спецыяльныя значэнні: "^" -
     * пачатак сказу, "$" - канец сказу, "NONE" - не мусіць быць ніякага знаку.
     */
    public List<String> seps = new ArrayList<>();
}
