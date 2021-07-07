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
import java.util.List;

import org.junit.Test;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;
import org.kie.pmml.commons.transformations.KiePMMLDerivedField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class KiePMMLFieldRefTest {

    private static final String FIELD_NAME = "FIELD_NAME";

    @Test
    public void evaluateFromKiePMMLNameValues() {
        final Object value = 234.45;
        final List<KiePMMLNameValue> kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue(FIELD_NAME,
                                                                                                        value));
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(FIELD_NAME, Collections.emptyList(), null);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), kiePMMLNameValues);
        final Object retrieved = kiePMMLFieldRef.evaluate(processingDTO);
        assertEquals(value, retrieved);
    }

    @Test
    public void evaluateFromDerivedFields() {
        final Object value = 234.45;
        final KiePMMLConstant kiePMMLConstant = new KiePMMLConstant("NAME", Collections.emptyList(), value);
        final KiePMMLDerivedField kiePMMLDerivedField = KiePMMLDerivedField.builder(FIELD_NAME,
                                                                                    Collections.emptyList(),
                                                                                    DATA_TYPE.DOUBLE.getName(),
                                                                                    OP_TYPE.CONTINUOUS.getName(),
                                                                                    kiePMMLConstant)
                .build();
        final List<KiePMMLDerivedField> derivedFields = Collections.singletonList(kiePMMLDerivedField);
        final List<KiePMMLNameValue> kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue("UNKNOWN",
                                                                                                        "WRONG"));
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(FIELD_NAME, Collections.emptyList(), null);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), derivedFields, Collections.emptyList(), kiePMMLNameValues);
        final Object retrieved = kiePMMLFieldRef.evaluate(processingDTO);
        assertEquals(value, retrieved);
    }

    @Test
    public void evaluateFromMapMissingTo() {
        final String value = "234.45";
        final KiePMMLConstant kiePMMLConstant = new KiePMMLConstant("NAME", Collections.emptyList(), "WRONG-CONSTANT");
        final KiePMMLDerivedField kiePMMLDerivedField = KiePMMLDerivedField.builder("ANOTHER_FIELD",
                                                                                    Collections.emptyList(),
                                                                                    DATA_TYPE.DOUBLE.getName(),
                                                                                    OP_TYPE.CONTINUOUS.getName(),
                                                                                    kiePMMLConstant)
                .build();
        final List<KiePMMLDerivedField> derivedFields = Collections.singletonList(kiePMMLDerivedField);
        final List<KiePMMLNameValue> kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue("UNKNOWN",
                                                                                                        "WRONG"));
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(FIELD_NAME, Collections.emptyList(), value);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), derivedFields, Collections.emptyList(), kiePMMLNameValues);
        final Object retrieved = kiePMMLFieldRef.evaluate(processingDTO);
        assertEquals(value, retrieved);
    }

    @Test
    public void evaluateNull() {
        final KiePMMLConstant kiePMMLConstant = new KiePMMLConstant("NAME", Collections.emptyList(), "WRONG-CONSTANT");
        final KiePMMLDerivedField kiePMMLDerivedField = KiePMMLDerivedField.builder("ANOTHER_FIELD",
                                                                                    Collections.emptyList(),
                                                                                    DATA_TYPE.DOUBLE.getName(),
                                                                                    OP_TYPE.CONTINUOUS.getName(),
                                                                                    kiePMMLConstant)
                .build();
        final List<KiePMMLDerivedField> derivedFields = Collections.singletonList(kiePMMLDerivedField);
        final List<KiePMMLNameValue> kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue("UNKNOWN",
                                                                                                        "WRONG"));
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(FIELD_NAME, Collections.emptyList(), null);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), derivedFields, Collections.emptyList(), kiePMMLNameValues);
        final Object retrieved = kiePMMLFieldRef.evaluate(processingDTO);
        assertNull(retrieved);
    }
}