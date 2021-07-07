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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
public class ClassUtilTest {

    @Test
    public void testIsAssignableFromWhenItIsAssignable() {
        assertTrue(ClassUtil.isAssignableFrom(A.class, B.class));
    }

    @Test
    public void testIsAssignableFromWhenItIsNotAssignable() {
        assertFalse(ClassUtil.isAssignableFrom(A.class, C.class));
    }

    static class A {
        // empty.
    }

    static class B extends A {
        // empty.
    }

    static class C {
        // empty.
    }
}
