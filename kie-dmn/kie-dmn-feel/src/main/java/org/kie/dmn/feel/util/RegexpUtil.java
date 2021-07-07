package org.kie.dmn.feel.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

public class RegexpUtil {

    public static final Pattern BEGIN_YEAR = Pattern.compile("^-?(([1-9]\\d\\d\\d+)|(0\\d\\d\\d))-"); // FEEL spec, "specified by XML Schema Part 2 Datatypes", hence: yearFrag ::= '-'? (([1-9] digit digit digit+)) | ('0' digit digit digit))

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

    public static boolean find(final String input, final String pattern, final String flags) {
        int f = processFlags(flags);
        Pattern p = Pattern.compile(pattern, f);
        Matcher m = p.matcher(input);
        return m.find();
    }

    public static String formatDate(final LocalDate date) {
        return FEEL_DATE.format(date);
    }

    public static LocalDate parseFeelDate(final String val) {
        return LocalDate.from(FEEL_DATE.parse(val));
    }

    public static boolean findFindYear(final String val) {
        return BEGIN_YEAR.matcher(val).find();
    }

    public static List<String> split(final String string, final String delimiter, final String flags) {
        int f = processFlags(flags);
        Pattern p = Pattern.compile(delimiter, f);
        String[] split = p.split(string, -1);
        return Arrays.asList(split);
    }

    private static int processFlags(String flags) {
        int f = 0;
        if (flags != null) {
            if (flags.contains("s")) {
                f |= Pattern.DOTALL;
            }
            if (flags.contains("m")) {
                f |= Pattern.MULTILINE;
            }
            if (flags.contains("i")) {
                f |= Pattern.CASE_INSENSITIVE;
            }
        }
        return f;
    }
}
