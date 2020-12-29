#!/bin/sh


java -cp lib-utils/languagetool-tools-5.1-jar-with-dependencies.jar org.languagetool.tools.POSDictionaryBuilder -i slovy-2008-uniq.txt -info src/org/languagetool/resource/be.info -o src/org/languagetool/resource/be.dict
