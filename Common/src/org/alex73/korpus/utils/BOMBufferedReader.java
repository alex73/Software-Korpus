/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)

 This file is part of Korpus.

 Korpus is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Korpus is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.korpus.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class BOMBufferedReader extends BufferedReader {
    private int lineNumber = 0;

    public BOMBufferedReader(Reader rd) throws IOException {
        super(rd);
        mark(4);

        int char1 = read();
        if (char1 != 65279) { // BOM: EF BB BF
            reset();
        }
    }

    public String readLine() throws IOException {
        lineNumber++;
        return super.readLine();
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
