/**
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.


    Author: Aleś Bułojčyk <alex73mail@gmail.com>
    Homepage: http://sourceforge.net/projects/korpus/
 */
package org.alex73.lat;

public class LatTest {
    /**
     * Спраўджваем канвэртар лацінкі.
     */
    public static void main(String[] args) throws Exception {
        mustLat("салаўіную", false, "salaŭinuju");
        mustLat("салаўіную", true, "sałaŭjinuju");
        mustLat("Аб’еўшыся шчаўя, кволыя вераб’і захапілі салаўіную сям’ю і ўюць гнёзды.", false,
                "Abjeŭšysia ščaŭja, kvolyja vierabji zachapili salaŭinuju siamju i ŭjuć hniozdy.");
        mustLat("Я ня п’ю", true, "Ja nia pju");
        mustLat("Я і ты любім наліваць к’янці ў кілішкі і піць літрамі. Д’ябал! Салаўі захапілі вераб’ёў. Пароль? Лялька, Нянька, Яйка.",
                true,
                "Ja i ty lubim nalivać kjanci ŭ kiliški i pić litrami. Djabał! Sałaŭji zachapili vierabjoŭ. Parol? Lalka, Niańka, Jajka.");
        mustLat("Лінія", true, "Linija");
        mustLat("шчаўя", true, "ščaŭja");
        mustLat("пі", true, "pi");
        mustLat("б’ю", true, "bju");
        mustLat("ўюць", true, "ŭjuć");
        mustLat("вераб’ям", true, "vierabjam");
    }

    static void mustLat(String orig, boolean latTrad, String expected) {
        String result = Lat.lat(orig, latTrad);
        if (result.equals(expected)) {
            System.out.println( "OK: " + orig + "->" + result);
        } else {
            System.out.println( "чакаем " + expected + ": " + orig + "->" + result);
        }

       /* var result = Lat.cyr(expected, latTrad);
        if (result == orig) {
            out += "OK: " + expected + "->" + result;
        } else {
            out += "чакаем " + orig + ": " + expected + "->" + result;
        }
        out += "\n";*/
    }
}
