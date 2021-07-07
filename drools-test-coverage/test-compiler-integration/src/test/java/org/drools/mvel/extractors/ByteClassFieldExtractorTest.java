/*
 * Copyright (c) 2020. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.mvel.extractors;

import org.drools.core.base.ClassFieldAccessorCache;
import org.drools.core.base.ClassFieldAccessorStore;
import org.drools.core.base.TestBean;
import org.drools.core.spi.InternalReadAccessor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ByteClassFieldExtractorTest extends BaseClassFieldExtractorsTest {
    InternalReadAccessor reader;
    TestBean             bean = new TestBean();

    @Before
    public void setUp() throws Exception {
        ClassFieldAccessorStore store = new ClassFieldAccessorStore();
        store.setClassFieldAccessorCache( new ClassFieldAccessorCache( Thread.currentThread().getContextClassLoader() ) );
        store.setEagerWire( true );
        this.reader = store.getReader( TestBean.class,
                                          "byteAttr" );
    }

    @Test
    public void testGetBooleanValue() {
        try {
            this.reader.getBooleanValue( null,
                                            this.bean );
            fail( "Should have throw an exception" );
        } catch ( final Exception e ) {
            // success
        }
    }

    @Test
    public void testGetByteValue() {
        assertEquals(1,
                this.reader.getByteValue(null,
                        this.bean));
    }

    @Test
    public void testGetCharValue() {
        try {
            this.reader.getCharValue( null,
                                         this.bean );
            fail( "Should have throw an exception" );
        } catch ( final Exception e ) {
            // success
        }
    }

    @Test
    public void testGetDoubleValue() {
        assertEquals(1.0,
                this.reader.getDoubleValue(null,
                        this.bean),
                0.01);
    }

    @Test
    public void testGetFloatValue() {
        assertEquals(1.0f,
                this.reader.getFloatValue(null,
                        this.bean),
                0.01);
    }

    @Test
    public void testGetIntValue() {
        assertEquals(1,
                this.reader.getIntValue(null,
                        this.bean));
    }

    @Test
    public void testGetLongValue() {
        assertEquals(1,
                this.reader.getLongValue(null,
                        this.bean));
    }

    @Test
    public void testGetShortValue() {
        assertEquals(1,
                this.reader.getShortValue(null,
                        this.bean));
    }

    @Test
    public void testGetValue() {
        assertEquals(1,
                ((Number) this.reader.getValue(null,
                        this.bean)).byteValue());
    }

    @Test
    public void testIsNullValue() {
        assertFalse(this.reader.isNullValue(null,
                this.bean));
    }
}
