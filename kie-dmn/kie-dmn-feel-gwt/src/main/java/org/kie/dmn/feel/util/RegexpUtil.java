package org.kie.dmn.feel.util;

import org.gwtproject.regexp.shared.MatchResult;
import org.gwtproject.regexp.shared.RegExp;

public class RegexpUtil {

    public static boolean find(final String input, final String pattern, String flags) {

        if (flags == null) {
            flags = "";
        }
        final RegExp p = RegExp.compile(pattern, flags);
        final MatchResult m = p.exec(input);
        return m != null;
    }
}
