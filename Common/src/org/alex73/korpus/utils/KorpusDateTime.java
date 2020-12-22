package org.alex73.korpus.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class KorpusDateTime {
    static final DatatypeFactory DTFACTORY;
    static {
        try {
            DTFACTORY = DatatypeFactory.newInstance();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
    static Pattern RE_YEAR_SIMPLE = Pattern.compile("([0-9]{4})\\??");
    static Pattern RE_YEAR_TWO = Pattern.compile("([0-9]{4})\\-([0-9]{4})\\??");
    static Pattern RE_DATE1 = Pattern.compile("([0-9]{2})\\.([0-9]{2})\\.([0-9]{4})\\??");
    static Pattern RE_DATE2 = Pattern
            .compile("([0-9]{2})\\.([0-9]{2})\\.([0-9]{4})\\-([0-9]{2})\\.([0-9]{2})\\.([0-9]{4})\\??");
    static Pattern RE_DATE3 = Pattern.compile("([0-9]{4})\\-([0-9]{2})\\-([0-9]{2})");
    static Pattern RE_MONTH1 = Pattern.compile(
            "(студзень|люты|сакавік|красавік|травень|май|чэрвень|ліпень|жнівень|верасень|кастрычнік|лістапад|снежань)\\s+([0-9]{4})");
    static Pattern RE_DATETIME1 = Pattern
            .compile("([0-9]{4})\\-([0-9]{2})\\-([0-9]{2})T([0-9]{2})\\:([0-9]{2})\\:([0-9]{2})");
    static Pattern RE_DATETIME2 = Pattern.compile(
            "([0-9]{4})\\-([0-9]{2})\\-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})[\\-+]([0-9]{2}):([0-9]{2})");
    static List<String> months = Arrays.asList("студзень", "люты", "сакавік", "красавік", "травень", "май", "чэрвень",
            "ліпень", "жнівень", "верасень", "кастрычнік", "лістапад", "снежань");

    private final String date;

    private List<Pair> pairs = new ArrayList<>();

    public KorpusDateTime(String date) {
        this.date = date;
        for (String y : date.split(";")) {
            y = y.trim();
            Matcher m;
            XMLGregorianCalendar d1, d2;
            if ((m = RE_YEAR_SIMPLE.matcher(y)).matches()) {
                d1 = DTFACTORY.newXMLGregorianCalendarDate(Integer.parseInt(m.group(1)),
                        DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED);
                d2 = d1;
            } else if ((m = RE_YEAR_TWO.matcher(y)).matches()) {
                d1 = DTFACTORY.newXMLGregorianCalendarDate(Integer.parseInt(m.group(1)),
                        DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED);
                d2 = DTFACTORY.newXMLGregorianCalendarDate(Integer.parseInt(m.group(2)),
                        DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED);
            } else if ((m = RE_DATE1.matcher(y)).matches()) {
                d1 = DTFACTORY.newXMLGregorianCalendarDate(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(1)), DatatypeConstants.FIELD_UNDEFINED);
                d2 = d1;
            } else if ((m = RE_DATE2.matcher(y)).matches()) {
                d1 = DTFACTORY.newXMLGregorianCalendarDate(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(1)), DatatypeConstants.FIELD_UNDEFINED);
                d2 = DTFACTORY.newXMLGregorianCalendarDate(Integer.parseInt(m.group(6)), Integer.parseInt(m.group(5)),
                        Integer.parseInt(m.group(4)), DatatypeConstants.FIELD_UNDEFINED);
            } else if ((m = RE_DATE3.matcher(y)).matches()) {
                d1 = DTFACTORY.newXMLGregorianCalendarDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3)), DatatypeConstants.FIELD_UNDEFINED);
                d2 = d1;
            } else if ((m = RE_MONTH1.matcher(y)).matches()) {
                d1 = DTFACTORY.newXMLGregorianCalendarDate(Integer.parseInt(m.group(2)), months.indexOf(m.group(1)) + 1,
                        DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
                d2 = d1;
            } else if (RE_DATETIME1.matcher(y).matches() || RE_DATETIME2.matcher(y).matches()) {
                d1 = DTFACTORY.newXMLGregorianCalendar(y);
                d2 = d1;
            } else {
                throw new RuntimeException("Wrong date format: " + date);
            }
            if (d1.compare(d2) > 0) {
                throw new RuntimeException("Wrong date format: " + date);
            }
            pairs.add(new Pair(d1, d2));
        }
        for (int i = 1; i < pairs.size(); i++) {
            if (pairs.get(i - 1).to.compare(pairs.get(i).from) > 0) {
                throw new RuntimeException("Wrong date format: " + date);
            }
        }
    }

    public int getEarliestYear() {
        return pairs.get(0).from.getYear();
    }

    public int getLatestYear() {
        return pairs.get(pairs.size() - 1).to.getYear();
    }

    public long earliest() {
        return pairs.get(0).from.toGregorianCalendar().getTimeInMillis();
    }

    public long latest() {
        return pairs.get(pairs.size() - 1).to.toGregorianCalendar().getTimeInMillis();
    }

    static class Pair {
        public final XMLGregorianCalendar from, to;

        public Pair(XMLGregorianCalendar from, XMLGregorianCalendar to) {
            this.from = from;
            this.to = to;
        }
    }
}
