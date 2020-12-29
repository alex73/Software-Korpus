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
 * Зьмяніць першую літару.
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

function fanetyka(text) {
    text = text.toLowerCase();
    var slova = "";
    var result = "";
    for(var i=0;i<text.length;i++) {
        var c=text.charAt(i);
        if (isLitara(c)) {
            slova += c;
            for(i++;i<text.length;i++) {
                c=text.charAt(i);
                if (!isLitara(c)) {
                    i--;
                    break;
                }
                slova += c;
            }
            result += fanetykaSlova(slova);
            slova = "";
        } else {
            result += c;
        }
    }
    return result;
}

function fanetykaSlova(slova) {
        slova = slova.replace(/бе/g, 'bʲɛ');
        slova = slova.replace(/бё/g, 'bʲɔ');
        slova = slova.replace(/бі/g, 'bʲi');
        slova = slova.replace(/бю/g, 'bʲu');
        slova = slova.replace(/бя/g, 'bʲa');
        slova = slova.replace(/мве/g, 'ɱvʲɛ');
        slova = slova.replace(/мвё/g, 'ɱvʲɔ');
        slova = slova.replace(/мві/g, 'ɱvʲi');
        slova = slova.replace(/мвю/g, 'ɱvʲu');
        slova = slova.replace(/мвя/g, 'ɱvʲa');
        slova = slova.replace(/мфе/g, 'ɱfʲɛ');
        slova = slova.replace(/мфё/g, 'ɱfʲɔ');
        slova = slova.replace(/мфі/g, 'ɱfʲi');
        slova = slova.replace(/мфю/g, 'ɱfʲu');
        slova = slova.replace(/мфя/g, 'ɱfʲa');
        slova = slova.replace(/ве/g, 'vʲɛ');
        slova = slova.replace(/вё/g, 'βʲɔ');
        slova = slova.replace(/ві/g, 'vʲi');
        slova = slova.replace(/вю/g, 'βʲu');
        slova = slova.replace(/вя/g, 'vʲa');
        slova = slova.replace(/ге/g, 'ʝɛ');
        slova = slova.replace(/гё/g, 'ʝɔ');
        slova = slova.replace(/гі/g, 'ʝi');
        slova = slova.replace(/гю/g, 'ʝu');
        slova = slova.replace(/гя/g, 'ʝa');
        slova = slova.replace(/ддзе/g, 'd͡zʲːɛ');
        slova = slova.replace(/ддзё/g, 'd͡zʲːɔ');
        slova = slova.replace(/ддзі/g, 'd͡zʲːi');
        slova = slova.replace(/ддзю/g, 'd͡zʲːu');
        slova = slova.replace(/ддзя/g, 'd͡zʲːa');
        slova = slova.replace(/дзе/g, 'd͡zʲɛ');
        slova = slova.replace(/дзё/g, 'd͡zʲɔ');
        slova = slova.replace(/дзі/g, 'd͡zʲi');
        slova = slova.replace(/дзю/g, 'd͡zʲu');
        slova = slova.replace(/дзя/g, 'd͡zʲa');
        slova = slova.replace(/ззе/g, 'zʲːɛ');
        slova = slova.replace(/ззё/g, 'zʲːɔ');
        slova = slova.replace(/ззі/g, 'zʲːi');
        slova = slova.replace(/ззю/g, 'zʲːu');
        slova = slova.replace(/ззя/g, 'zʲːa');
        slova = slova.replace(/зе/g, 'zʲɛ');
        slova = slova.replace(/зё/g, 'zʲɔ');
        slova = slova.replace(/зі/g, 'zʲi');
        slova = slova.replace(/зю/g, 'zʲu');
        slova = slova.replace(/зя/g, 'zʲa');
        slova = slova.replace(/ке/g, 'cɛ');
        slova = slova.replace(/кё/g, 'cɔ');
        slova = slova.replace(/кі/g, 'ci');
        slova = slova.replace(/кю/g, 'cu');
        slova = slova.replace(/кя/g, 'ca');
        slova = slova.replace(/лле/g, 'ʎːɛ');
        slova = slova.replace(/ллё/g, 'ʎːɔ');
        slova = slova.replace(/ллі/g, 'ʎːi');
        slova = slova.replace(/ллю/g, 'ʎːu');
        slova = slova.replace(/лля/g, 'ʎːa');
        slova = slova.replace(/ле/g, 'ʎɛ');
        slova = slova.replace(/лё/g, 'ʎɔ');
        slova = slova.replace(/лі/g, 'ʎi');
        slova = slova.replace(/лю/g, 'ʎu');
        slova = slova.replace(/ля/g, 'ʎa');
        slova = slova.replace(/ме/g, 'mʲɛ');
        slova = slova.replace(/мё/g, 'mʲɔ');
        slova = slova.replace(/мі/g, 'mʲi');
        slova = slova.replace(/мю/g, 'mʲu');
        slova = slova.replace(/мя/g, 'mʲa');
        slova = slova.replace(/нне/g, 'ɲːɛ');
        slova = slova.replace(/ннё/g, 'ɲːɔ');
        slova = slova.replace(/нні/g, 'ɲːi');
        slova = slova.replace(/нню/g, 'ɲːu');
        slova = slova.replace(/ння/g, 'ɲːa');
        slova = slova.replace(/не/g, 'ɲɛ');
        slova = slova.replace(/нё/g, 'ɲɔ');
        slova = slova.replace(/ні/g, 'ɲi');
        slova = slova.replace(/ню/g, 'ɲu');
        slova = slova.replace(/ня/g, 'ɲa');
        slova = slova.replace(/пе/g, 'pʲɛ');
        slova = slova.replace(/пё/g, 'pʲɔ');
        slova = slova.replace(/пі/g, 'pʲi');
        slova = slova.replace(/пю/g, 'pʲu');
        slova = slova.replace(/пя/g, 'pʲa');
        slova = slova.replace(/ссе/g, 'sʲːɛ');
        slova = slova.replace(/ссё/g, 'sʲːɔ');
        slova = slova.replace(/ссі/g, 'sʲːi');
        slova = slova.replace(/ссю/g, 'sʲːu');
        slova = slova.replace(/сся/g, 'sʲːa');
        slova = slova.replace(/се/g, 'sʲɛ');
        slova = slova.replace(/сё/g, 'sʲɔ');
        slova = slova.replace(/сі/g, 'sʲi');
        slova = slova.replace(/сю/g, 'sʲu');
        slova = slova.replace(/ся/g, 'sʲa');
        slova = slova.replace(/фе/g, 'fʲɛ');
        slova = slova.replace(/фё/g, 'fʲɔ');
        slova = slova.replace(/фі/g, 'fʲi');
        slova = slova.replace(/фю/g, 'fʲu');
        slova = slova.replace(/фя/g, 'fʲa');
        slova = slova.replace(/хе/g, 'çɛ');
        slova = slova.replace(/хё/g, 'çɔ');
        slova = slova.replace(/хі/g, 'çi');
        slova = slova.replace(/хю/g, 'çu');
        slova = slova.replace(/хя/g, 'ça');
        slova = slova.replace(/цце/g, 't͡sʲːɛ');
        slova = slova.replace(/ццё/g, 't͡sʲːɔ');
        slova = slova.replace(/цці/g, 't͡sʲːi');
        slova = slova.replace(/ццю/g, 't͡sʲːu');
        slova = slova.replace(/цця/g, 't͡sʲːa');
        slova = slova.replace(/це/g, 't͡sʲɛ');
        slova = slova.replace(/цё/g, 't͡sʲɔ');
        slova = slova.replace(/ці/g, 't͡sʲi');
        slova = slova.replace(/цю/g, 't͡sʲu');
        slova = slova.replace(/ця/g, 't͡sʲa');
        slova = slova.replace(/дзь/g, 'd͡zʲ');
        slova = slova.replace(/дd͡zʲ/g, 'd͡zʲː');
        slova = slova.replace(/зь/g, 'zʲ');
        slova = slova.replace(/ль/g, 'ʎ');
        slova = slova.replace(/нь/g, 'ɲ');
        slova = slova.replace(/сь/g, 'sʲ');
        slova = slova.replace(/ць/g, 't͡sʲ');
        slova = slova.replace(/сbʲ/g, 'zʲbʲ');
        slova = slova.replace(/сvʲ/g, 'sʲvʲ');
        slova = slova.replace(/сd͡zʲ/g, 'zʲd͡zʲ');
        slova = slova.replace(/сzʲ/g, 'zʲː');
        slova = slova.replace(/сʎ/g, 'sʲʎ');
        slova = slova.replace(/сmʲ/g, 'sʲmʲ');
        slova = slova.replace(/сɲ/g, 'sʲɲ');
        slova = slova.replace(/сpʲ/g, 'sʲpʲ');
        slova = slova.replace(/сsʲ/g, 'sʲː');
        slova = slova.replace(/сfʲ/g, 'sʲfʲ');
        slova = slova.replace(/сt͡sʲ/g, 'sʲt͡sʲ');
        slova = slova.replace(/зbʲ/g, 'zʲbʲ');
        slova = slova.replace(/зvʲ/g, 'zʲvʲ');
        slova = slova.replace(/зd͡zʲ/g, 'zʲd͡zʲ');
        slova = slova.replace(/зzʲ/g, 'zʲː');
        slova = slova.replace(/зʎ/g, 'zʲʎ');
        slova = slova.replace(/зmʲ/g, 'zʲmʲ');
        slova = slova.replace(/зɲʲ/g, 'zʲɲʲ');
        slova = slova.replace(/зt͡sʲ/g, 'sʲt͡sʲ');
        slova = slova.replace(/цvʲ/g, 't͡sʲvʲ');
        slova = slova.replace(/дзvʲ/g, 'd͡zʲvʲ');
        slova = slova.replace(/кcʲ/g, 'cʲː');
        slova = slova.replace(/kz/g, 'gz');
        slova = slova.replace(/mf/g, 'ɱf');
        slova = slova.replace(/’я/g, 'ja');
        slova = slova.replace(/’е/g, 'jɛ');
        slova = slova.replace(/’ё/g, 'jɔ');
        slova = slova.replace(/’ю/g, 'ju');
        slova = slova.replace(/д$/g, 't');
        slova = slova.replace(/ж$/g, 'ʂ');
        slova = slova.replace(/з$/g, 's');
        slova = slova.replace(/б$/g, 'p');
        slova = slova.replace(/г$/g, 'x');
        slova = slova.replace(/дж$/g, 't͡ʂ');
        slova = slova.replace(/дзь$/g, 't͡sʲ');
        slova = slova.replace(/зь$/g, 'sʲ');
        slova = slova.replace(/здч/g, 'ʂt͡ʂ');
        slova = slova.replace(/ждж/g, 'ʐd͡ʐ');
        slova = slova.replace(/здж/g, 'ʐd͡ʐ');
        slova = slova.replace(/жк/g, 'ʂk');
        slova = slova.replace(/зж/g, 'ʐː');
        slova = slova.replace(/зш/g, 'ʂː');
        slova = slova.replace(/сш/g, 'ʂː');
        slova = slova.replace(/зч/g, 'ʂt͡ʂ');
        slova = slova.replace(/сч/g, 'ʂt͡ʂ');
        slova = slova.replace(/жч/g, 'ʂt͡ʂ');
        slova = slova.replace(/дч/g, 't͡ʂː');
        slova = slova.replace(/тч/g, 't͡ʂː');
        slova = slova.replace(/дт/g, 'tː');
        slova = slova.replace(/гк/g, 'xk');
        slova = slova.replace(/гч/g, 'xt͡ʂ');
        slova = slova.replace(/дц/g, 't͡sː');
        slova = slova.replace(/тц/g, 't͡sː');
        slova = slova.replace(/чц/g, 't͡sː');
        slova = slova.replace(/цц/g, 't͡sː');
        slova = slova.replace(/жж/g, 'ʐː');
        slova = slova.replace(/шш/g, 'ʂː');
        slova = slova.replace(/чч/g, 't͡ʂː');
        slova = slova.replace(/нн/g, 'nː');
        slova = slova.replace(/а/g, 'a');
        slova = slova.replace(/о/g, 'ɔ');
        slova = slova.replace(/й/g, 'j');
        slova = slova.replace(/у/g, 'u');
        slova = slova.replace(/ы/g, 'ɨ');
        slova = slova.replace(/э/g, 'ɛ');
        slova = slova.replace(/шч/g, 'ʂt͡ʂ');
        slova = slova.replace(/дж/g, 'd͡ʐ');
        slova = slova.replace(/дз/g, 'd͡z');
        slova = slova.replace(/aдс/g, 'at͡s');
        slova = slova.replace(/aдsʲ/g, 'at͡sʲsʲ');
        slova = slova.replace(/aдt͡sʲ/g, 'at͡sʲː');
        slova = slova.replace(/дск/g, 't͡sk');
        slova = slova.replace(/дсc/g, 't͡sc');
        slova = slova.replace(/дст/g, 't͡st');
        slova = slova.replace(/тсc/g, 't͡sc');
        slova = slova.replace(/тск/g, 't͡sk');
        slova = slova.replace(/тст/g, 't͡st');
        slova = slova.replace(/я/g, 'ja');
        slova = slova.replace(/р/g, 'r');
        slova = slova.replace(/г/g, 'ɣ');
        slova = slova.replace(/вɔ/g, 'βɔ');
        slova = slova.replace(/вu/g, 'βu');
        slova = slova.replace(/ств/g, 'stv');
        slova = slova.replace(/зв/g, 'zv');
        slova = slova.replace(/зк/g, 'sk');
        slova = slova.replace(/дк/g, 'tk');
        slova = slova.replace(/чн/g, 't͡ʂn');
        slova = slova.replace(/нк/g, 'nk');
        slova = slova.replace(/зн/g, 'zn');
        slova = slova.replace(/с$/g, 's');
        slova = slova.replace(/к/g, 'k');
        slova = slova.replace(/п/g, 'p');
        slova = slova.replace(/т/g, 't');
        slova = slova.replace(/х/g, 'x');
        slova = slova.replace(/ц/g, 't͡s');
        slova = slova.replace(/ч/g, 't͡ʂ');
        slova = slova.replace(/ш/g, 'ʂ');
        slova = slova.replace(/л/g, 'l');
        slova = slova.replace(/ь/g, 'j');
        slova = slova.replace(/н/g, 'n');
        slova = slova.replace(/ф/g, 'f');
        slova = slova.replace(/d͡zʲ$/g, 't͡sʲ');
        slova = slova.replace(/d͡ʐ$/g, 't͡ʂ');
        slova = slova.replace(/ў/g, 'u̯');
        slova = slova.replace(/сl/g, 'sl');
        slova = slova.replace(/зɲ/g, 'zʲɲ');
        slova = slova.replace(/вɨ/g, 'vɨ');
        slova = slova.replace(/б/g, 'b');
        slova = slova.replace(/с/g, 's');
        slova = slova.replace(/м/g, 'm');
        slova = slova.replace(/ж/g, 'ʐ');
        slova = slova.replace(/д/g, 'd');
        slova = slova.replace(/ё/g, 'jɔ');
        slova = slova.replace(/з/g, 'z');
        slova = slova.replace(/в/g, 'v');
        slova = slova.replace(/е/g, 'jɛ');

        slova = slova.replace(/^antɨі/g, 'antɨi');
        slova = slova.replace(/^au̯taі/g, 'au̯tai');
        slova = slova.replace(/^vɨsɔkaі/g, 'vɨsɔkai');
        slova = slova.replace(/^ʝipʲɛrі/g, 'ʝipʲɛrɨ');
        slova = slova.replace(/^pa-і/g, 'pai');

        slova = slova.replace(/b'j/g, 'bj');
        slova = slova.replace(/d'j/g, 'dj');
        slova = slova.replace(/p'j/g, 'pj');
        slova = slova.replace(/r'j/g, 'rj');
        slova = slova.replace(/z'j/g, 'zj');
        slova = slova.replace(/v'j/g, 'vj');
        slova = slova.replace(/x'j/g, 'xj');

        slova = slova.replace(/b'і/g, 'bji');
        slova = slova.replace(/r'і/g, 'rji');

        slova = slova.replace(/aю/g, 'aju');
        slova = slova.replace(/uю/g, 'uju');
        slova = slova.replace(/ɔю/g, 'ɔju');
        slova = slova.replace(/ɨю/g, 'ɨju');

        slova = slova.replace(/aі/g, 'aji');
        slova = slova.replace(/ɔі/g, 'ɔji');
        slova = slova.replace(/uі/g, 'uji');
        slova = slova.replace(/ɛі/g, 'ɛji');

        slova = slova.replace(/d'ю/g, 'dju');
        slova = slova.replace(/ɛ-ю/g, 'ɛju');

        ////////////////////
        slova = slova.replace(/tі/g, 'tɨ');
        slova = slova.replace(/dі/g, 'dɨ');
        slova = slova.replace(/ʐі/g, 'ʐɨ');
        slova = slova.replace(/d-і/g, 'd-ɨ');
        slova = slova.replace(/t-і/g, 't-ɨ');

        slova = slova.replace(/іnʂa/g, 'jіnʂa');
        slova = slova.replace(/mʲiʐі/g, 'mʲiʐɨ');
        slova = slova.replace(/v'юі/g, 'vjuji');
        slova = slova.replace(/'ю/g, 'ju');
        slova = slova.replace(/ю/g, 'ju');

        slova = slova.replace(/^bʎit͡sʲi/g, 'bʎit͡sɨ');
        slova = slova.replace(/^bʎit͡s-і/g, 'bʎit͡s-ɨ');
        slova = slova.replace(/^unutrɨі/g, 'unutrɨi');
        slova = slova.replace(/іnʂɨ/g, 'jinʂɨ');
        slova = slova.replace(/іxɲi/g, 'jixɲi');
        slova = slova.replace(/іxnɨ/g, 'jixnɨ');
        slova = slova.replace(/ɲі/g, 'ɲjі');
        slova = slova.replace(/z'і/g, 'zji');
        slova = slova.replace(/ɨі/g, 'ɨji');
        slova = slova.replace(/iі/g, 'ii');

        slova = slova.replace(/a-і/g, 'a-i');
        slova = slova.replace(/t-і/g, 't-ɨ');
        slova = slova.replace(/rі/g, 'rɨ');

        slova = slova.replace(/ʐ'j/g, 'ʐj');
        slova = slova.replace(/n'j/g, 'nj');
        slova = slova.replace(/p'і/g, 'pji');
        slova = slova.replace(/m'j/g, 'mj');
        slova = slova.replace(/ʂ'j/g, 'ʂj');
        slova = slova.replace(/k'j/g, 'kj');
        slova = slova.replace(/f'j/g, 'fj');
        slova = slova.replace(/t'j/g, 'tj');

        slova = slova.replace(/tюx/g, 't͡sʲjux');

        slova = slova.replace(/і/g, 'i');
        slova = slova.replace(/s'j/g, 'sj');
        /////////////////
        slova = slova.replace(/bbʲ/g, 'bʲː');
        slova = slova.replace(/bb/g, 'bː');
        slova = slova.replace(/bt͡sʲ/g, 'pt͡sʲ');
        slova = slova.replace(/mv/g, 'ɱv');
        slova = slova.replace(/dc/g, 'tc');
        slova = slova.replace(/zt/g, 'st');
        slova = slova.replace(/zz/g, 'zː');
        slova = slova.replace(/bk/g, 'pk');
        slova = slova.replace(/bc/g, 'pc');

        slova = slova.replace(/ʐc/g, 'ʂc');
        slova = slova.replace(/bpʲ/g, 'pʲː');
        slova = slova.replace(/bp/g, 'pː');
        slova = slova.replace(/bsʲ/g, 'psʲ');

        slova = slova.replace(/bs/g, 'ps');
        slova = slova.replace(/bt/g, 'pt');
        slova = slova.replace(/bx/g, 'px');
        slova = slova.replace(/bt͡s/g, 'pt͡s');

        slova = slova.replace(/bç/g, 'pç');
        slova = slova.replace(/zdn/g, 'zn');
        slova = slova.replace(/bʂ/g, 'pʂ');
        slova = slova.replace(/ssʲ/g, 'sʲː');
        slova = slova.replace(/ss/g, 'sː');
        slova = slova.replace(/dd/g, 'dː');

        slova = slova.replace(/stn/g, 'sn');
        slova = slova.replace(/dp/g, 'tp');
        slova = slova.replace(/df/g, 'tf');
        slova = slova.replace(/dx/g, 'tx');
        slova = slova.replace(/dʂ/g, 'tʂ');
        slova = slova.replace(/kz/g, 'gz');
        slova = slova.replace(/kd/g, 'gd');
        slova = slova.replace(/zːʲ/g, 'zʲː');
        slova = slova.replace(/zc/g, 'sc');
        slova = slova.replace(/ʐsc/g, 'sc');
        slova = slova.replace(/ɣc/g, 'xc');

        slova = slova.replace(/dç/g, 'tç');


    return slova;
}

