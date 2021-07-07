/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.base;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValueTypeTest {

    @Test
    public void testIsBoolean() {
        assertTrue( ValueType.BOOLEAN_TYPE.isBoolean() );
        assertTrue( ValueType.PBOOLEAN_TYPE.isBoolean() );
    }

    @Test
    public void testIsNumber() {
        assertTrue( ValueType.PBYTE_TYPE.isNumber() );
        assertTrue( ValueType.PSHORT_TYPE.isNumber() );
        assertTrue( ValueType.PINTEGER_TYPE.isNumber() );
        assertTrue( ValueType.PLONG_TYPE.isNumber() );
        assertTrue( ValueType.PFLOAT_TYPE.isNumber() );
        assertTrue( ValueType.PDOUBLE_TYPE.isNumber() );
        assertTrue( ValueType.BYTE_TYPE.isNumber() );
        assertTrue( ValueType.SHORT_TYPE.isNumber() );
        assertTrue( ValueType.INTEGER_TYPE.isNumber() );
        assertTrue( ValueType.LONG_TYPE.isNumber() );
        assertTrue( ValueType.FLOAT_TYPE.isNumber() );
        assertTrue( ValueType.DOUBLE_TYPE.isNumber() );

    }

}
