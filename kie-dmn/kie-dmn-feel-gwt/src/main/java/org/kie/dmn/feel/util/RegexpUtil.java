package org.kie.dmn.feel.util;

import java.util.ArrayList;
import java.util.List;

import org.gwtproject.regexp.shared.MatchResult;
import org.gwtproject.regexp.shared.RegExp;
import org.gwtproject.regexp.shared.SplitResult;

public class RegexpUtil {

    public static boolean find(final String input, final String pattern, String flags) {

        if (flags == null) {
            flags = "";
        }
        final RegExp p = RegExp.compile(pattern, flags);
        final MatchResult m = p.exec(input);
        return m != null;
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
