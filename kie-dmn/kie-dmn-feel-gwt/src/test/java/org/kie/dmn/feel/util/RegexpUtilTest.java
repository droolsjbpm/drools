package org.kie.dmn.feel.util;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegexpUtilTest {

    @Test
    public void testFind() {
        assertTrue(RegexpUtil.find("foobar", "^fo*b", ""));
    }

    @Test
    public void testSplit() {
        final List<String> split = RegexpUtil.split("foo,bar,baz", ",", null);
        assertEquals(3, split.size());
        assertTrue(split.contains("foo"));
        assertTrue(split.contains("bar"));
        assertTrue(split.contains("baz"));
    }
}