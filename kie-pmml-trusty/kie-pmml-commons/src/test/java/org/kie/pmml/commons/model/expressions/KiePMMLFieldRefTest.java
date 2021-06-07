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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.pmml.ParameterInfo;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;
import org.kie.pmml.commons.transformations.KiePMMLDerivedField;

import static org.junit.Assert.*;

public class KiePMMLFieldRefTest {
    private static final String FIELD_NAME = "FIELD_NAME";


    @Test
    public void evaluateFromKiePMMLNameValues() {
        final Object value = 234.45;
        final List<KiePMMLNameValue> kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue(FIELD_NAME, value));
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(FIELD_NAME, Collections.emptyList(), null);
        final Object retrieved = kiePMMLFieldRef.evaluate(Collections.emptyList(), Collections.emptyList(), kiePMMLNameValues);
        assertEquals(value, retrieved);
    }

    @Test
    public void evaluateFromDerivedFields() {
        final Object value = 234.45;
        final KiePMMLConstant kiePMMLConstant = new KiePMMLConstant("NAME", Collections.emptyList(), value);
        final KiePMMLDerivedField kiePMMLDerivedField = new KiePMMLDerivedField(FIELD_NAME,
                                                                                Collections.emptyList(),
                                                                                DATA_TYPE.DOUBLE,
                                                                                OP_TYPE.CONTINUOUS,
                                                                                kiePMMLConstant);
        final List<KiePMMLDerivedField> derivedFields = Collections.singletonList(kiePMMLDerivedField);
        final List<KiePMMLNameValue> kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue("UNKNOWN", "WRONG"));
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(FIELD_NAME, Collections.emptyList(), null);
        final Object retrieved = kiePMMLFieldRef.evaluate(Collections.emptyList(), derivedFields, kiePMMLNameValues);
        assertEquals(value, retrieved);
    }

    @Test
    public void evaluateFromMapMissingTo() {
        final String value = "234.45";
        final KiePMMLConstant kiePMMLConstant = new KiePMMLConstant("NAME", Collections.emptyList(), "WRONG-CONSTANT");
        final KiePMMLDerivedField kiePMMLDerivedField = new KiePMMLDerivedField("ANOTHER_FIELD",
                                                                                Collections.emptyList(),
                                                                                DATA_TYPE.DOUBLE,
                                                                                OP_TYPE.CONTINUOUS,
                                                                                kiePMMLConstant);
        final List<KiePMMLDerivedField> derivedFields = Collections.singletonList(kiePMMLDerivedField);
        final List<KiePMMLNameValue> kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue("UNKNOWN", "WRONG"));
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(FIELD_NAME, Collections.emptyList(), value);
        final Object retrieved = kiePMMLFieldRef.evaluate(Collections.emptyList(), derivedFields, kiePMMLNameValues);
        assertEquals(value, retrieved);
    }

    @Test
    public void evaluateNull() {
        final KiePMMLConstant kiePMMLConstant = new KiePMMLConstant("NAME", Collections.emptyList(), "WRONG-CONSTANT");
        final KiePMMLDerivedField kiePMMLDerivedField = new KiePMMLDerivedField("ANOTHER_FIELD",
                                                                                Collections.emptyList(),
                                                                                DATA_TYPE.DOUBLE,
                                                                                OP_TYPE.CONTINUOUS,
                                                                                kiePMMLConstant);
        final List<KiePMMLDerivedField> derivedFields = Collections.singletonList(kiePMMLDerivedField);
        final List<KiePMMLNameValue> kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue("UNKNOWN", "WRONG"));
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(FIELD_NAME, Collections.emptyList(), null);
        final Object retrieved = kiePMMLFieldRef.evaluate(Collections.emptyList(), derivedFields, kiePMMLNameValues);
        assertNull(retrieved);
    }
}