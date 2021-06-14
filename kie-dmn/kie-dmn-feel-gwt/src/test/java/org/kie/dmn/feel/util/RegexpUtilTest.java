package org.kie.dmn.feel.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RegexpUtilTest {

    @Test
    public void testBasic() {
        assertTrue(RegexpUtil.find("foobar", "^fo*b", ""));
    }
}