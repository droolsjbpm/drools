package org.kie.dmn.feel.util;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.Test;

public class StringUtilTest {

    @Test
    public void name() {
        String test = "hello";

        IntStream intStream = test.codePoints();

        IntStream intStream1 = StringUtil.codePoints(test);
        intStream.iterator().forEachRemaining(new Consumer<Integer>() {
            @Override
            public void accept(final Integer integer) {
                System.out.println("a" + integer);
            }
        });
        intStream1.iterator().forEachRemaining(new Consumer<Integer>() {
            @Override
            public void accept(final Integer integer) {
                System.out.println("b" + integer);
            }
        });

        String gg = "";
    }
}