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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.pmml.api.enums.CLOSURE;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KiePMMLDiscretizeTest {

    private static final String NAME = "name";
    private static final String MAP_MISSING_TO = "mapMissingTo";
    private static final String DEFAULTVALUE = "defaultValue";
    private static final DATA_TYPE DATATYPE = DATA_TYPE.INTEGER;

    private static KiePMMLDiscretizeBin kiePMMLDiscretizeBin1;
    private static KiePMMLDiscretizeBin kiePMMLDiscretizeBin2;
    private static KiePMMLDiscretizeBin kiePMMLDiscretizeBin3;
    private static KiePMMLDiscretizeBin kiePMMLDiscretizeBin4;
    private static KiePMMLDiscretizeBin kiePMMLDiscretizeBin5;
    private static List<KiePMMLDiscretizeBin> discretizeBins;

    @BeforeClass
    public static void setup() {
        kiePMMLDiscretizeBin1 = getKiePMMLDiscretizeBin("kiePMMLDiscretizeBin1", new KiePMMLInterval(null, 20,
                                                                                                     CLOSURE.OPEN_OPEN), "kiePMMLDiscretizeBin1");
        kiePMMLDiscretizeBin2 = getKiePMMLDiscretizeBin("kiePMMLDiscretizeBin2", new KiePMMLInterval(21, 30,
                                                                                                     CLOSURE.OPEN_CLOSED), "kiePMMLDiscretizeBin2");
        kiePMMLDiscretizeBin3 = getKiePMMLDiscretizeBin("kiePMMLDiscretizeBin3", new KiePMMLInterval(31, 40,
                                                                                                     CLOSURE.CLOSED_OPEN), "kiePMMLDiscretizeBin3");
        kiePMMLDiscretizeBin4 = getKiePMMLDiscretizeBin("kiePMMLDiscretizeBin4", new KiePMMLInterval(41, 50,
                                                                                                     CLOSURE.CLOSED_CLOSED), "kiePMMLDiscretizeBin4");
        kiePMMLDiscretizeBin5 = getKiePMMLDiscretizeBin("kiePMMLDiscretizeBin5", new KiePMMLInterval(51, null,
                                                                                                     CLOSURE.CLOSED_CLOSED), "kiePMMLDiscretizeBin5");
        discretizeBins = Arrays.asList(kiePMMLDiscretizeBin1, kiePMMLDiscretizeBin2,
                                                                  kiePMMLDiscretizeBin3, kiePMMLDiscretizeBin4,
                                                                  kiePMMLDiscretizeBin5);
    }

    @Test
    public void evaluateNoInput() {
        KiePMMLDiscretize kiePMMLDiscretize = getKiePMMLDiscretize(null, null);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Object retrieved = kiePMMLDiscretize.evaluate(processingDTO);
        assertNull(retrieved);
        kiePMMLDiscretize = getKiePMMLDiscretize(MAP_MISSING_TO, null);
        retrieved = kiePMMLDiscretize.evaluate(processingDTO);
        assertNotNull(retrieved);
        assertEquals(MAP_MISSING_TO, retrieved);
    }

    @Test
    public void evaluateDefaultValue() {
        KiePMMLDiscretize kiePMMLDiscretize = getKiePMMLDiscretize(null, null);

        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Arrays.asList(new KiePMMLNameValue(NAME, 20)));
        Object retrieved = kiePMMLDiscretize.evaluate(processingDTO);
        assertNull(retrieved);
        kiePMMLDiscretize = getKiePMMLDiscretize(MAP_MISSING_TO, DEFAULTVALUE);
        processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Arrays.asList(new KiePMMLNameValue(NAME, 20)));
        retrieved = kiePMMLDiscretize.evaluate(processingDTO);
        assertNotNull(retrieved);
        assertEquals(DEFAULTVALUE, retrieved);
        processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Arrays.asList(new KiePMMLNameValue(NAME, 21)));
        retrieved = kiePMMLDiscretize.evaluate(processingDTO);
        assertNotNull(retrieved);
        assertEquals(DEFAULTVALUE, retrieved);
        processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Arrays.asList(new KiePMMLNameValue(NAME, 40)));
        retrieved = kiePMMLDiscretize.evaluate(processingDTO);
        assertNotNull(retrieved);
        assertEquals(DEFAULTVALUE, retrieved);
    }

    @Test
    public void getFromDiscretizeBins() {
        KiePMMLDiscretize kiePMMLDiscretize = getKiePMMLDiscretize(null, null);
        Optional<String> retrieved = kiePMMLDiscretize.getFromDiscretizeBins(10);
        assertTrue(retrieved.isPresent());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(20);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(21);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(29);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin2.getBinValue(), retrieved.get());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(30);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin2.getBinValue(), retrieved.get());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(31);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin3.getBinValue(), retrieved.get());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(32);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin3.getBinValue(), retrieved.get());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(40);
        assertFalse(retrieved.isPresent());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(41);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin4.getBinValue(), retrieved.get());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(42);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin4.getBinValue(), retrieved.get());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(49);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin4.getBinValue(), retrieved.get());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(50);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin4.getBinValue(), retrieved.get());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(51);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin5.getBinValue(), retrieved.get());
        retrieved = kiePMMLDiscretize.getFromDiscretizeBins(52);
        assertTrue(retrieved.isPresent());
        assertEquals(kiePMMLDiscretizeBin5.getBinValue(), retrieved.get());
    }
    
    private KiePMMLDiscretize getKiePMMLDiscretize(String mapMissingTo, String defaultValue) {
        Collections.shuffle(discretizeBins);
        return new KiePMMLDiscretize(NAME, Collections.emptyList(), discretizeBins, mapMissingTo, defaultValue, DATATYPE);
    }

    private static KiePMMLDiscretizeBin getKiePMMLDiscretizeBin(String name, KiePMMLInterval interval, String binValue) {
        return new KiePMMLDiscretizeBin(name, Collections.emptyList(), binValue, interval);
    }
}