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

package org.kie.pmml.commons.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.expressions.KiePMMLApply;
import org.kie.pmml.commons.model.expressions.KiePMMLConstant;
import org.kie.pmml.commons.model.expressions.KiePMMLFieldRef;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;

import static org.junit.Assert.assertEquals;

public class KiePMMLDerivedFieldTest {

    private static final String CUSTOM_FIELD = "CUSTOM_FIELD";
    private static final String PARAM_1 = "PARAM_1";
    private static final String PARAM_2 = "PARAM_2";
    private static final Double value1 = 100.0;
    private static final Double value2 = 5.0;

    @Test
    public void evaluateFromConstant() {
        // <DerivedField name="CUSTOM_FIELD" optype="continuous" dataType="double">
        //     <Constant>100.0</Constant>
        // </DerivedField>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLDerivedField derivedField = KiePMMLDerivedField.builder(CUSTOM_FIELD, Collections.emptyList(),
                                                                             DATA_TYPE.DOUBLE.getName(),
                                                                             OP_TYPE.CONTINUOUS.getName(),
                                                                             kiePMMLConstant1)
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), new ArrayList<>());
        Object retrieved = derivedField.evaluate(processingDTO);
        assertEquals(value1, retrieved);
    }

    @Test
    public void evaluateFromFieldRef() {
        // <DerivedField name="CUSTOM_FIELD" optype="continuous" dataType="double">
        //     <FieldRef field="PARAM_1"/>
        // </DerivedField>
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLDerivedField derivedField = KiePMMLDerivedField.builder(CUSTOM_FIELD, Collections.emptyList(),
                                                                             DATA_TYPE.DOUBLE.getName(),
                                                                             OP_TYPE.CONTINUOUS.getName(),
                                                                             kiePMMLFieldRef)
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(),
                                                        Arrays.asList(new KiePMMLNameValue(PARAM_1, value1)));
        Object retrieved = derivedField.evaluate(processingDTO);
        assertEquals(value1, retrieved);
    }

    @Test
    public void evaluateFromApplyWithKiePMMLNameValues() {
        // <DerivedField name="CUSTOM_FIELD" optype="continuous" dataType="double">
        //     <Apply function="/">
        //        <FieldRef>PARAM_1</FieldRef>
        //        <FieldRef>PARAM_2</FieldRef>
        //      </Apply>
        // </DerivedField>
        final KiePMMLFieldRef kiePMMLFieldRef1 = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLFieldRef kiePMMLFieldRef2 = new KiePMMLFieldRef(PARAM_2, Collections.emptyList(), null);
        final KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef1, kiePMMLFieldRef2))
                .build();
        final KiePMMLDerivedField derivedField = KiePMMLDerivedField.builder(CUSTOM_FIELD, Collections.emptyList(),
                                                                             DATA_TYPE.DOUBLE.getName(),
                                                                             OP_TYPE.CONTINUOUS.getName(),
                                                                             kiePMMLApply)
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), getKiePMMLNameValues());
        Object retrieved = derivedField.evaluate(processingDTO);
        Object expected = value1 / value2;
        assertEquals(expected, retrieved);
    }

    @Test
    public void evaluateFromApplyWithDerivedFields() {
        // <DerivedField name="CUSTOM_FIELD" optype="continuous" dataType="double">
        //     <Apply function="/">
        //        <FieldRef>PARAM_1</FieldRef>
        //        <FieldRef>PARAM_2</FieldRef>
        //      </Apply>
        // </DerivedField>
        final KiePMMLFieldRef kiePMMLFieldRef1 = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLFieldRef kiePMMLFieldRef2 = new KiePMMLFieldRef(PARAM_2, Collections.emptyList(), null);
        final KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef1, kiePMMLFieldRef2))
                .build();
        final KiePMMLDerivedField derivedField = KiePMMLDerivedField.builder(CUSTOM_FIELD, Collections.emptyList(),
                                                                             DATA_TYPE.DOUBLE.getName(),
                                                                             OP_TYPE.CONTINUOUS.getName(),
                                                                             kiePMMLApply)
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), getDerivedFields(),
                                                        Collections.emptyList(), new ArrayList<>());
        Object retrieved = derivedField.evaluate(processingDTO);
        Object expected = value1 / value2;
        assertEquals(expected, retrieved);
    }

    private List<KiePMMLNameValue> getKiePMMLNameValues() {
        return Arrays.asList(new KiePMMLNameValue(PARAM_1, value1), new KiePMMLNameValue(PARAM_2, value2));
    }

    private List<KiePMMLDerivedField> getDerivedFields() {
        // <DerivedField name="PARAM_1" optype="continuous" dataType="double">
        //     <Constant>100.0</Constant>
        // </DerivedField>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLDerivedField derivedField1 = KiePMMLDerivedField.builder(PARAM_1, Collections.emptyList(),
                                                                              DATA_TYPE.DOUBLE.getName(),
                                                                              OP_TYPE.CONTINUOUS.getName(),
                                                                              kiePMMLConstant1)
                .build();
        // <DerivedField name="PARAM_1" optype="continuous" dataType="double">
        //     <Constant>5.0</Constant>
        // </DerivedField>
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        final KiePMMLDerivedField derivedField2 = KiePMMLDerivedField.builder(PARAM_2, Collections.emptyList(),
                                                                              DATA_TYPE.DOUBLE.getName(),
                                                                              OP_TYPE.CONTINUOUS.getName(),
                                                                              kiePMMLConstant2)
                .build();
        return Arrays.asList(derivedField1, derivedField2);
    }
}