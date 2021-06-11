package org.kie.dmn.feel.util;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringUtilTest {

    private int i = 0;

    @Test
    public void testCodePoints() {
        final String test = "hello";

        final IntStream originalIntStream = test.codePoints();

        final IntStream utilIntStream = StringUtil.codePoints(test);

        final int[] originalFromJava = new int[(int) test.codePointCount(0, test.length())];

        originalIntStream.iterator().forEachRemaining((Consumer<Integer>) integer -> originalFromJava[i++] = integer);

        i = 0;

        utilIntStream.iterator().forEachRemaining((Consumer<Integer>) integer -> assertTrue(originalFromJava[i++] == integer));
    }

    @Test
    public void testFormat() {

        final String mask = "Happy %.0fth birthday, Mr %s!";
        final String original = String.format(mask, BigDecimal.valueOf(38), "Doe");
        final String utilVersion = StringUtil.format(mask, BigDecimal.valueOf(38), "Doe");

        assertEquals(original, utilVersion);
    }
}