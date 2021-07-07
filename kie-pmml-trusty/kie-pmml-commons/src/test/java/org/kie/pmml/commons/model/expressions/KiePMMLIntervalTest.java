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

import org.junit.Test;
import org.kie.pmml.api.enums.CLOSURE;

import static org.junit.Assert.*;

public class KiePMMLIntervalTest {

    @Test
    public void isIn() {
        KiePMMLInterval kiePMMLInterval = new KiePMMLInterval(20, 40, CLOSURE.OPEN_OPEN);
        assertTrue(kiePMMLInterval.isIn(30));
        assertFalse(kiePMMLInterval.isIn(10));
        assertFalse(kiePMMLInterval.isIn(20));
        assertFalse(kiePMMLInterval.isIn(40));
        assertFalse(kiePMMLInterval.isIn(50));
        kiePMMLInterval = new KiePMMLInterval(20, 40, CLOSURE.OPEN_CLOSED);
        assertTrue(kiePMMLInterval.isIn(30));
        assertFalse(kiePMMLInterval.isIn(10));
        assertFalse(kiePMMLInterval.isIn(20));
        assertTrue(kiePMMLInterval.isIn(40));
        assertFalse(kiePMMLInterval.isIn(50));
        kiePMMLInterval = new KiePMMLInterval(20, 40, CLOSURE.CLOSED_OPEN);
        assertTrue(kiePMMLInterval.isInsideClosedOpen(30));
        assertFalse(kiePMMLInterval.isInsideClosedOpen(10));
        assertTrue(kiePMMLInterval.isInsideClosedOpen(20));
        assertFalse(kiePMMLInterval.isInsideClosedOpen(40));
        assertFalse(kiePMMLInterval.isInsideClosedOpen(50));
        kiePMMLInterval = new KiePMMLInterval(20, 40, CLOSURE.CLOSED_CLOSED);
        assertTrue(kiePMMLInterval.isIn(30));
        assertFalse(kiePMMLInterval.isIn(10));
        assertTrue(kiePMMLInterval.isIn(20));
        assertTrue(kiePMMLInterval.isIn(40));
        assertFalse(kiePMMLInterval.isIn(50));
    }

    @Test
    public void isInsideOpenOpen() {
        KiePMMLInterval kiePMMLInterval = new KiePMMLInterval(null, 20, CLOSURE.OPEN_OPEN);
        assertTrue(kiePMMLInterval.isInsideOpenOpen(10));
        assertFalse(kiePMMLInterval.isInsideOpenOpen(20));
        assertFalse(kiePMMLInterval.isInsideOpenOpen(30));
        kiePMMLInterval = new KiePMMLInterval(20, null, CLOSURE.OPEN_OPEN);
        assertTrue(kiePMMLInterval.isInsideOpenOpen(30));
        assertFalse(kiePMMLInterval.isInsideOpenOpen(20));
        assertFalse(kiePMMLInterval.isInsideOpenOpen(10));
        kiePMMLInterval = new KiePMMLInterval(20, 40, CLOSURE.OPEN_OPEN);
        assertTrue(kiePMMLInterval.isInsideOpenOpen(30));
        assertFalse(kiePMMLInterval.isInsideOpenOpen(10));
        assertFalse(kiePMMLInterval.isInsideOpenOpen(20));
        assertFalse(kiePMMLInterval.isInsideOpenOpen(40));
        assertFalse(kiePMMLInterval.isInsideOpenOpen(50));
    }

    @Test
    public void isInsideOpenClosed() {
        KiePMMLInterval kiePMMLInterval = new KiePMMLInterval(null, 20, CLOSURE.OPEN_CLOSED);
        assertTrue(kiePMMLInterval.isInsideOpenClosed(10));
        assertTrue(kiePMMLInterval.isInsideOpenClosed(20));
        assertFalse(kiePMMLInterval.isInsideOpenClosed(30));
        kiePMMLInterval = new KiePMMLInterval(20, null, CLOSURE.OPEN_CLOSED);
        assertTrue(kiePMMLInterval.isInsideOpenClosed(30));
        assertFalse(kiePMMLInterval.isInsideOpenClosed(20));
        assertFalse(kiePMMLInterval.isInsideOpenClosed(10));
        kiePMMLInterval = new KiePMMLInterval(20, 40, CLOSURE.OPEN_CLOSED);
        assertTrue(kiePMMLInterval.isInsideOpenClosed(30));
        assertFalse(kiePMMLInterval.isInsideOpenClosed(10));
        assertFalse(kiePMMLInterval.isInsideOpenClosed(20));
        assertTrue(kiePMMLInterval.isInsideOpenClosed(40));
        assertFalse(kiePMMLInterval.isInsideOpenClosed(50));
    }

    @Test
    public void isInsideClosedOpen() {
        KiePMMLInterval kiePMMLInterval = new KiePMMLInterval(null, 20, CLOSURE.CLOSED_OPEN);
        assertTrue(kiePMMLInterval.isInsideClosedOpen(10));
        assertFalse(kiePMMLInterval.isInsideClosedOpen(20));
        assertFalse(kiePMMLInterval.isInsideClosedOpen(30));
        kiePMMLInterval = new KiePMMLInterval(20, null, CLOSURE.CLOSED_OPEN);
        assertTrue(kiePMMLInterval.isInsideClosedOpen(30));
        assertTrue(kiePMMLInterval.isInsideClosedOpen(20));
        assertFalse(kiePMMLInterval.isInsideClosedOpen(10));
        kiePMMLInterval = new KiePMMLInterval(20, 40, CLOSURE.CLOSED_OPEN);
        assertTrue(kiePMMLInterval.isInsideClosedOpen(30));
        assertFalse(kiePMMLInterval.isInsideClosedOpen(10));
        assertTrue(kiePMMLInterval.isInsideClosedOpen(20));
        assertFalse(kiePMMLInterval.isInsideClosedOpen(40));
        assertFalse(kiePMMLInterval.isInsideClosedOpen(50));
    }

    @Test
    public void isInsideClosedClosed() {
        KiePMMLInterval kiePMMLInterval = new KiePMMLInterval(null, 20, CLOSURE.CLOSED_CLOSED);
        assertTrue(kiePMMLInterval.isInsideClosedClosed(10));
        assertTrue(kiePMMLInterval.isInsideClosedClosed(20));
        assertFalse(kiePMMLInterval.isInsideClosedClosed(30));
        kiePMMLInterval = new KiePMMLInterval(20, null, CLOSURE.CLOSED_CLOSED);
        assertTrue(kiePMMLInterval.isInsideClosedClosed(30));
        assertTrue(kiePMMLInterval.isInsideClosedClosed(20));
        assertFalse(kiePMMLInterval.isInsideClosedClosed(10));
        kiePMMLInterval = new KiePMMLInterval(20, 40, CLOSURE.CLOSED_CLOSED);
        assertTrue(kiePMMLInterval.isInsideClosedClosed(30));
        assertFalse(kiePMMLInterval.isInsideClosedClosed(10));
        assertTrue(kiePMMLInterval.isInsideClosedClosed(20));
        assertTrue(kiePMMLInterval.isInsideClosedClosed(40));
        assertFalse(kiePMMLInterval.isInsideClosedClosed(50));
    }
}