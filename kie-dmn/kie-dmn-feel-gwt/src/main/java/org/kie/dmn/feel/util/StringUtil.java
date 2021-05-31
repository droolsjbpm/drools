/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.dmn.feel.util;

import java.util.stream.IntStream;

import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;

public class StringUtil {

    public static String format(final String mask, final Object... params) {
        final RegExp regex = RegExp.compile("%[a-z]");
        final SplitResult split = regex.split(mask);
        final StringBuffer msg = new StringBuffer();
        for (int pos = 0; pos < split.length() - 1; ++pos) {
            msg.append(split.get(pos));
            msg.append(params[pos].toString());
        }
        msg.append(split.get(split.length() - 1));
        return msg.toString();
    }

    public static IntStream codePoints(final String string) {
        int index = 0;

        int codePointIndex = 0;
        int[] codePoints = new int[string.codePointCount(0, string.length())];

        while (index < string.length()) {

            int codePoint = Character.codePointAt(string, index);
            codePoints[codePointIndex] = codePoint;

            int charCount = Character.charCount(codePoint);
            index += charCount;
        }

        return IntStream.of(codePoints);
    }
}
