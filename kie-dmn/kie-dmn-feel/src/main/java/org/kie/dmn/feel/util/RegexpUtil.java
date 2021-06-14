package org.kie.dmn.feel.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexpUtil {

    public static boolean find(final String input, final String pattern, final String flags) {
        int f = processFlags(flags);
        Pattern p = Pattern.compile(pattern, f);
        Matcher m = p.matcher(input);
        return m.find();
    }

    public static List<String> split(final String string, final String delimiter, final String flags) {
        int f = processFlags(flags);
        Pattern p = Pattern.compile(delimiter, f);
        String[] split = p.split(string, -1);
        return Arrays.asList(split );
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
