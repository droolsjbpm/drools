package org.kie.dmn.feel.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.ArrayList;
import java.util.List;

import org.gwtproject.regexp.shared.MatchResult;
import org.gwtproject.regexp.shared.RegExp;
import org.gwtproject.regexp.shared.SplitResult;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

public class RegexpUtil {
    public static final RegExp BEGIN_YEAR = RegExp.compile("^-?(([1-9]\\d\\d\\d+)|(0\\d\\d\\d))-"); // FEEL spec, "specified by XML Schema Part 2 Datatypes", hence: yearFrag ::= '-'? (([1-9] digit digit digit+)) | ('0' digit digit digit))
    public static final DateTimeFormatter FEEL_DATE;
    static {
        FEEL_DATE = new DateTimeFormatterBuilder().appendValue(YEAR, 4, 9, SignStyle.NORMAL)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(DAY_OF_MONTH, 2)
                .toFormatter()
                .withResolverStyle(ResolverStyle.STRICT);
    }

    public static boolean find(final String input, final String pattern, String flags) {

        if (flags == null) {
            flags = "";
        }
        final RegExp p = RegExp.compile(pattern, flags);
        final MatchResult m = p.exec(input);
        return m != null;
    }


    public static LocalDate parseFeelDate(final String val) {
        return LocalDate.from(FEEL_DATE.parse(val));
    }

    public static boolean findFindYear(final String val) { // TODO find find?
        return BEGIN_YEAR.exec(val) != null;
    }
    public static List<String> split(final String string, final String delimiter, final String flags) {
        ArrayList<String> result = new ArrayList<>();
        SplitResult splitResult = getRegExp(delimiter, flags).split(string, -1);
        for (int i = 0; i < splitResult.length(); i++) {
            result.add(splitResult.get(i));
        }
        return result;
    }

    private static RegExp getRegExp(final String delimiter, final String flags) {
        if (flags == null) {
            return RegExp.compile(delimiter);
        } else {
            return RegExp.compile(delimiter, flags);
        }
    }
}
