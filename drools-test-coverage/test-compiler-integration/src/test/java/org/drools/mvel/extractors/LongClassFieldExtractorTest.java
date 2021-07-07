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

public class LongClassFieldExtractorTest extends BaseClassFieldExtractorsTest {
    private static final long VALUE     = 5;

    InternalReadAccessor                 extractor;
    TestBean                  bean      = new TestBean();

    @Before
    public void setUp() throws Exception {
        ClassFieldAccessorStore store = new ClassFieldAccessorStore();
        store.setClassFieldAccessorCache( new ClassFieldAccessorCache( Thread.currentThread().getContextClassLoader() ) );
        store.setEagerWire( true );
        extractor = store.getReader( TestBean.class,
                                "longAttr" );
    }

    @Test
    public void testGetBooleanValue() {
        try {
            this.extractor.getBooleanValue( null,
                                            this.bean );
            fail( "Should have throw an exception" );
        } catch ( final Exception e ) {
            // success
        }
    }

    @Test
    public void testGetByteValue() {
        try {
            assertEquals( LongClassFieldExtractorTest.VALUE,
                                 this.extractor.getByteValue( null,
                                                              this.bean ) );
        } catch ( final Exception e ) {
            fail( "Should not throw an exception" );
        }
    }

    @Test
    public void testGetCharValue() {
        try {
            this.extractor.getCharValue( null,
                                         this.bean );
            fail( "Should have throw an exception" );
        } catch ( final Exception e ) {
            // success
        }
    }

    @Test
    public void testGetDoubleValue() {
        try {
            assertEquals( LongClassFieldExtractorTest.VALUE,
                                 this.extractor.getDoubleValue( null,
                                                                this.bean ),
                                 0.01 );
        } catch ( final Exception e ) {
            fail( "Should not throw an exception" );
        }
    }

    @Test
    public void testGetFloatValue() {
        try {
            assertEquals( LongClassFieldExtractorTest.VALUE,
                                 this.extractor.getFloatValue( null,
                                                               this.bean ),
                                 0.01 );
        } catch ( final Exception e ) {
            fail( "Should not throw an exception" );
        }
    }

    @Test
    public void testGetIntValue() {
        try {
            assertEquals( LongClassFieldExtractorTest.VALUE,
                                 this.extractor.getIntValue( null,
                                                             this.bean ) );
        } catch ( final Exception e ) {
            fail( "Should not throw an exception" );
        }
    }

    @Test
    public void testGetLongValue() {
        try {
            assertEquals( LongClassFieldExtractorTest.VALUE,
                                 this.extractor.getLongValue( null,
                                                              this.bean ) );
        } catch ( final Exception e ) {
            fail( "Should not throw an exception" );
        }
    }

    @Test
    public void testGetShortValue() {
        try {
            assertEquals( LongClassFieldExtractorTest.VALUE,
                                 this.extractor.getShortValue( null,
                                                               this.bean ) );
        } catch ( final Exception e ) {
            fail( "Should not throw an exception" );
        }
    }

    @Test
    public void testGetValue() {
        try {
            assertEquals( new Long( (short) LongClassFieldExtractorTest.VALUE ),
                                 this.extractor.getValue( null,
                                                          this.bean ) );
            assertTrue( this.extractor.getValue( null,
                                                        this.bean ) instanceof Long );
        } catch ( final Exception e ) {
            fail( "Should not throw an exception" );
        }
    }

    @Test
    public void testIsNullValue() {
        try {
            assertFalse( this.extractor.isNullValue( null,
                                                            this.bean ) );
        } catch ( final Exception e ) {
            fail( "Should not throw an exception" );
        }
    }
}
