/*
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.


    Author: Aleś Bułojčyk <alex73mail@gmail.com>
    Homepage: http://sourceforge.net/projects/korpus/

    Асноўныя функцыі:
        lat(text, latTrad) - Лацінізаваць кірылічны тэкст
        cyr(text, latTrad) - Кірылізаваць лацінкавы тэкст
        unhac(text)        - Прыбраць гачыкі з лацінкавага тэксту
*/

/**
 * Ці гэта літара ?
 */
function isLitara(c) {
  return oneOf(c.toLowerCase(), "йцкнгшўзхфвпрлджчсмтбёуеыаоэяію'’ь");
}
/**
 * Ці літара - зычная ?
 */
function isZyc(c) {
  return oneOf(c.toLowerCase(), "йцкнгшўзхфвпрлджчсмтб");
}
/**
 * Ці літара - галосная ?
 */
function isHal(c) {
  return oneOf(c.toLowerCase(), "ёуеыаоэяію");
}
/**
 * Вялікая літара ?
 */
function isU(c) {
  return c==c.toUpperCase() && c!=c.toLowerCase();
}
/**
 * Вялікая літара пабач зь іншай вялікай літарай ?
 */
function isUW(c,prev,next) {
  return isU(c)&&(isU(prev)||isU(next));
}
/**
 * Зьмяніць апошнюю літару.
 */
function changeLastLetter(text,newLetter) {
  return text.substr(0, text.length - 1)+newLetter;
}
/**
 * Літара адна з ... ?
 */
function oneOf(letter, many) {
  return many.indexOf(letter)>=0;
}
/**
 * Лацінізаваць кірылічны тэкст.
 *   latTrad == true  : традыцыйная лацінка
 *   latTrad == false : афіцыйная лацінка (згодна https://be-x-old.wikipedia.org/wiki/Інструкцыя_па_трансьлітарацыі)
 */
function lat(text, latTrad) {
  var out  = "";
  var simple = new Array();
  simple['а'] = 'a';
  simple['б'] = 'b';
  simple['в'] = 'v';
  simple['г'] = 'h';
  simple['ґ'] = 'g';
  simple['д'] = 'd';
  simple['ж'] = 'ž';
  simple['з'] = 'z';
  simple['й'] = 'j';
  simple['к'] = 'k';
  simple['м'] = 'm';
  simple['н'] = 'n';
  simple['о'] = 'o';
  simple['п'] = 'p';
  simple['р'] = 'r';
  simple['с'] = 's';
  simple['т'] = 't';
  simple['у'] = 'u';
  simple['ў'] = 'ŭ';
  simple['ф'] = 'f';
  simple['х'] = 'ch';
  simple['ц'] = 'c';
  simple['ч'] = 'č';
  simple['ш'] = 'š';
  simple['ы'] = 'y';
  simple['э'] = 'e';
  var halosnyja = new Array();
  halosnyja['е']='e';
  halosnyja['ё']='o';
  halosnyja['ю']='u';
  halosnyja['я']='a';

  for(var i=0;i<text.length;i++) {
    var c=text.charAt(i);
    var prev = i>0?text.charAt(i-1):'?';
    var next = i<(text.length-1)?text.charAt(i+1):'?';
    var wordUpper=isUW(c,prev,next);
    var thisUpper=isU(c);
    var prevUpper=isU(prev);
    c = c.toLowerCase();
    prev = prev.toLowerCase();
    next = next.toLowerCase();

    if (c=='\'' || c=='’') {
      continue;
    }
    var sm = simple[c];
    if (sm == null) {
      sm = '';
      switch(c) {
        case 'л':
          if (latTrad) {
            if (oneOf(next,'еёюяіь\'’')) {
              sm='l';
            } else {
              sm='ł';
            }
          } else {
            sm='l';
          }
          break;
        case 'е': case 'ё': case 'ю': case 'я':
          if (prev == 'л' && latTrad) {
            sm = halosnyja[c];
          } else if (prev == '\'' || prev == '’' || prev == 'й' || prev == 'ў') {
            sm = 'j'+halosnyja[c];
          } else if (isZyc(prev)) {
            sm = 'i'+halosnyja[c];
          } else {
            sm = 'j'+halosnyja[c];
          }
          break;
        case 'і':
          if (prev == '\'' || prev == '’') {
            sm = 'ji';
          } else if (prev == 'й' || prev == 'ў') {
            if (latTrad) {
              sm = 'ji';
            } else {
              sm = 'i';
            }
          } else {
            sm = 'i';
          }
          break;
        case 'ь':
          sm='';
          if (out.length>0) {
              var p = out.charAt(out.length - 1);
              switch (p) {
              case 'Z':
                p = 'Ź';
                break;
              case 'z':
                p = 'ź';
                break;
              case 'N':
                p = 'Ń';
                break;
              case 'n':
                p = 'ń';
                break;
              case 'S':
                p = 'Ś';
                break;
              case 's':
                p = 'ś';
                break;
              case 'C':
                p = 'Ć';
                break;
              case 'c':
                p = 'ć';
                break;
              case 'L':
                if (!latTrad) {
                  p = 'Ĺ';
                }
                break;
              case 'l':
                if (!latTrad) {
                  p = 'ĺ';
                }
                break;
              case 'Ł':
                if (latTrad) {
                  p = 'L';
                }
                break;
              case 'ł':
                if (latTrad) {
                  p = 'l';
                }
                break;
              }
              out = changeLastLetter(out,p);
          }
          break;
        default:
          sm = c;
          break;
        }
    }

    if (thisUpper) {
      if (wordUpper || sm.length<2) {
        sm = sm.toUpperCase();
      } else {
        sm = sm.charAt(0).toUpperCase()+sm.substr(1,sm.length);
      }
    } 
    out += sm;
  }
  return out;
}
/**
 * Шукае мяккую галосную па цьвердай.
 */
function cyrmk(c) {
  switch(c) {
  case 'e':
    return 'е';
  case 'o':
    return 'ё';
  case 'u':
    return 'ю';
  case 'a':
    return 'я';
  case 'i':
    return 'і';
  }
  return null;
}
/**
 * Кірылізаваць лацінкавы тэкст.
 *   latTrad == true  : традыцыйная лацінка
 *   latTrad == false : афіцыйная лацінка (згодна https://be-x-old.wikipedia.org/wiki/Інструкцыя_па_трансьлітарацыі)
 */
function cyr(text, latTrad) {
  var lcm=latTrad?'ł':'l';
  var lcv=latTrad?'Ł':'L';
  var lmm=latTrad?'l':'ĺ';
  var lmv=latTrad?'L':'Ĺ';
  var out  = "";
  var simple = new Array();
  simple['a'] = 'а';
  simple['b'] = 'б';
  simple['v'] = 'в';
  simple['h'] = 'г';
  simple['g'] = 'ґ';
  simple['d'] = 'д';
  simple['e'] = 'э';// not simple
  simple['o'] = 'о';// not simple
  simple['ž'] = 'ж';
  simple['z'] = 'з';
  simple['ź'] = 'зь';
  simple['k'] = 'к';
  simple['m'] = 'м';
  simple['n'] = 'н';
  simple['ń'] = 'нь';
  simple['o'] = 'о';
  simple['p'] = 'п';
  simple['r'] = 'р';
  simple['s'] = 'с';
  simple['ś'] = 'сь';
  simple['t'] = 'т';
  simple['u'] = 'у';
  simple['ŭ'] = 'ў';
  simple['f'] = 'ф';
  simple['ć'] = 'ць';
  simple['č'] = 'ч';
  simple['š'] = 'ш';
  simple['y'] = 'ы';

  for(var i=0;i<text.length;i++) {
    var c=text.charAt(i);
    var prev = i>0?text.charAt(i-1):'?';
    var next = i<(text.length-1)?text.charAt(i+1):'?';
    var wordUpper=isUW(c,prev,next);
    var thisUpper=isU(c);
    c = c.toLowerCase();
    prev = prev.toLowerCase();
    next = next.toLowerCase();

    var sm = simple[c];
    if (sm == null) {
      sm = '';
      switch(c) {
      case 'l':
        sm = 'л';
        if (latTrad) {
            var m=cyrmk(next);
            if (m!=null) {
              sm += m;
              i++;
            } else if (next=='i') {
              sm += 'і';
              i++;
            } else {
              sm += 'ь';
            }
        } else {
          sm = 'л';
        }
        break;
      case 'ł':
        sm = 'л';
        break;
      case 'ĺ':
        sm = 'ль';
        break;
      case 'c':
        if (next=='h') {
          sm = 'х';
          i++;
        } else {
          sm = 'ц';
        }
        break;
      case 'j':
        // bju ščaŭja chaj zaja
        var m = cyrmk(next);
        var prevT = out.length>0 ? out.charAt(out.length-1).toLowerCase() : '-----';
        if (oneOf(prevT,"цкнгшзхфвпрлджчсмтб")) {
          sm = '’';
          if (m!=null) {
            sm += m;
            i++;
          }
        } else {
          if (m!=null) {
            sm += m;
            i++;
          } else {
            sm = 'й';
          }
        }
        break;
      case 'i':
        var m=cyrmk(next);
        if (m!=null) {
          sm = m;
          i++;
        } else {
          sm = 'і';
        }
        break;
      default:
        sm = c;
        break;
      }
    }

    if (thisUpper) {
      if (wordUpper || sm.length<2) {
        sm = sm.toUpperCase();
      } else {
        sm = sm.charAt(0).toUpperCase()+sm.substr(1,sm.length);
      }
    }
    out += sm;
  }

  return out;
}
/**
 * Прыбраць гачыкі з лацінкавага тэксту
 */
function unhac(text) {
    return text
    .replace(/Ć/g,'C').replace(/ć/g,'c')
    .replace(/Č/g,'C').replace(/č/g,'c')
    .replace(/Ł/g,'L').replace(/ł/g,'l')
    .replace(/Ĺ/g,'L').replace(/ĺ/g,'l')
    .replace(/Ń/g,'N').replace(/ń/g,'n')
    .replace(/Ś/g,'S').replace(/ś/g,'s')
    .replace(/Š/g,'S').replace(/š/g,'s')
    .replace(/Ŭ/g,'U').replace(/ŭ/g,'u')
    .replace(/Ź/g,'Z').replace(/ź/g,'z')
    .replace(/Ž/g,'Z').replace(/ž/g,'z');
}

