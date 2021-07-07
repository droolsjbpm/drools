/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.pmml.commons.model.expressions;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.kie.pmml.api.enums.CLOSURE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KiePMMLDiscretizeBinTest {
    
    private static final String NAME = "name";
    private static final String BINVALUE = "binValue";

    @Test
    public void evaluateOpenOpen() {
        KiePMMLDiscretizeBin kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(null, 20, CLOSURE.OPEN_OPEN));
        Optional<String> retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertFalse(retrieved.isPresent());
        kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(20, null, CLOSURE.OPEN_OPEN));
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertFalse(retrieved.isPresent());
        kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(20, 40, CLOSURE.OPEN_OPEN));
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(40);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(50);
        assertFalse(retrieved.isPresent());
    }

    @Test
    public void evaluateOpenClosed() {
        KiePMMLDiscretizeBin kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(null, 20, CLOSURE.OPEN_CLOSED));
        Optional<String> retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertFalse(retrieved.isPresent());
        kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(20, null, CLOSURE.OPEN_CLOSED));
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertFalse(retrieved.isPresent());
        kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(20, 40, CLOSURE.OPEN_CLOSED));
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(40);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(50);
        assertFalse(retrieved.isPresent());
    }

    @Test
    public void evaluateClosedOpen() {
        KiePMMLDiscretizeBin kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(null, 20, CLOSURE.CLOSED_OPEN));
        Optional<String> retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertFalse(retrieved.isPresent());
        kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(20, null, CLOSURE.CLOSED_OPEN));
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertFalse(retrieved.isPresent());
        kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(20, 40, CLOSURE.CLOSED_OPEN));
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(40);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(50);
        assertFalse(retrieved.isPresent());
    }

    @Test
    public void evaluateClosedClosed() {
        KiePMMLDiscretizeBin kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(null, 20, CLOSURE.CLOSED_CLOSED));
        Optional<String> retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertFalse(retrieved.isPresent());
        kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(20, null, CLOSURE.CLOSED_CLOSED));
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertFalse(retrieved.isPresent());
        kiePMMLDiscretizeBin = getKiePMMLDiscretizeBin(new KiePMMLInterval(20, 40, CLOSURE.CLOSED_CLOSED));
        retrieved = kiePMMLDiscretizeBin.evaluate(30);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(10);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretizeBin.evaluate(20);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(40);
        assertTrue(retrieved.isPresent());
        assertEquals(BINVALUE, retrieved.get());
        retrieved = kiePMMLDiscretizeBin.evaluate(50);
        assertFalse(retrieved.isPresent());
    }
    
    private KiePMMLDiscretizeBin getKiePMMLDiscretizeBin(KiePMMLInterval interval) {
        return new KiePMMLDiscretizeBin(NAME, Collections.emptyList(), BINVALUE, interval);
    }
}