package org.kie.dmn.feel.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexpUtil {

    public static boolean find(final String input, final String pattern, final String flags) {
        int f = processFlags(flags);
        Pattern p = Pattern.compile(pattern, f);
        Matcher m = p.matcher(input);
        return m.find();
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
