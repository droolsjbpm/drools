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

package org.kie.pmml.commons.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.kie.pmml.api.enums.RESULT_FEATURE;
import org.kie.pmml.commons.model.expressions.KiePMMLApply;
import org.kie.pmml.commons.model.expressions.KiePMMLConstant;
import org.kie.pmml.commons.model.expressions.KiePMMLFieldRef;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KiePMMLOutputFieldTest {

    private static final String CUSTOM_FIELD = "CUSTOM_FIELD";
    private static final String PARAM_1 = "PARAM_1";
    private static final String PARAM_2 = "PARAM_2";
    private static final Double value1 = 100.0;
    private static final Double value2 = 5.0;

    @Test
    public void getValueFromKiePMMLNameValuesByVariableName() {
        final String variableName = "variableName";
        final List<KiePMMLNameValue> kiePMMLNameValues = IntStream.range(0, 3).mapToObj(i -> new KiePMMLNameValue(
                "val-" + i, i)).collect(Collectors.toList());
        Optional<Object> retrieved = KiePMMLOutputField.getValueFromKiePMMLNameValuesByVariableName(variableName,
                                                                                                    kiePMMLNameValues);
        assertFalse(retrieved.isPresent());
        final Object variableValue = 243.94;
        kiePMMLNameValues.add(new KiePMMLNameValue(variableName, variableValue));
        retrieved = KiePMMLOutputField.getValueFromKiePMMLNameValuesByVariableName(variableName, kiePMMLNameValues);
        assertTrue(retrieved.isPresent());
        assertEquals(variableValue, retrieved.get());
    }

    @Test
    public void getValueFromPMMLResultByVariableName() {
        final String variableName = "variableName";
        final Map<String, Object> resultsVariables = new HashMap<>();
        Optional<Object> retrieved = KiePMMLOutputField.getValueFromPMMLResultByVariableName(variableName,
                                                                                             resultsVariables);
        assertFalse(retrieved.isPresent());
        final Object variableValue = 243.94;
        resultsVariables.put(variableName, variableValue);
        retrieved = KiePMMLOutputField.getValueFromPMMLResultByVariableName(variableName, resultsVariables);
        assertTrue(retrieved.isPresent());
        assertEquals(variableValue, retrieved.get());
    }

    @Test
    public void evaluatePredictedValue() {
        final String variableName = "variableName";
        KiePMMLOutputField kiePMMLOutputField = KiePMMLOutputField.builder("outputfield", Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.PREDICTED_VALUE)
                .withTargetField(variableName)
                .build();
        final List<KiePMMLNameValue> kiePMMLNameValues = IntStream.range(0, 3).mapToObj(i -> new KiePMMLNameValue(
                "val-" + i, i)).collect(Collectors.toList());
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), kiePMMLNameValues);
        assertNull(kiePMMLOutputField.evaluate(processingDTO));
        final Object variableValue = 243.94;
        kiePMMLNameValues.add(new KiePMMLNameValue(variableName, variableValue));
        processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                                          kiePMMLNameValues);
        Object retrieved = kiePMMLOutputField.evaluate(processingDTO);
        assertNotNull(retrieved);
        assertEquals(variableValue, retrieved);
    }

    @Test
    public void evaluateReasonCodeValue() {
        KiePMMLOutputField kiePMMLOutputField = KiePMMLOutputField.builder("outputfield", Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.REASON_CODE)
                .withRank(4)
                .build();
        final List<String> reasonCodes = IntStream.range(0, 3).mapToObj(i ->
                                                                                "reasonCode-" + i)
                .collect(Collectors.toList());
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), Collections.emptyList(), reasonCodes);
        assertNull(kiePMMLOutputField.evaluate(processingDTO));
        final String variableValue = "reasonCode-3";
        reasonCodes.add(variableValue);
        processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                                          Collections.emptyList(), reasonCodes);
        Object retrieved = kiePMMLOutputField.evaluate(processingDTO);
        assertNotNull(retrieved);
        assertEquals(variableValue, retrieved);
    }

    @Test
    public void evaluateTransformedValueFromConstant() {
        // <OutputField name="CUSTOM_FIELD" optype="continuous" dataType="double" feature="transformedValue">
        //     <Constant>100.0</Constant>
        // </OutputField>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLOutputField outputField = KiePMMLOutputField.builder(CUSTOM_FIELD, Collections.emptyList())
                .withKiePMMLExpression(kiePMMLConstant1)
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), new ArrayList<>());
        Object retrieved = outputField.evaluate(processingDTO);
        assertEquals(value1, retrieved);
    }

    @Test
    public void evaluateTransformedValueFromFieldRef() {
        // <OutputField name="CUSTOM_FIELD" optype="continuous" dataType="double" feature="transformedValue">
        //     <FieldRef field="PARAM_1"/>
        // </OutputField>
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLOutputField outputField = KiePMMLOutputField.builder(CUSTOM_FIELD, Collections.emptyList())
                .withKiePMMLExpression(kiePMMLFieldRef)
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(),
                                                        Arrays.asList(new KiePMMLNameValue(PARAM_1, value1)));
        Object retrieved = outputField.evaluate(processingDTO);
        assertEquals(value1, retrieved);
    }

    @Test
    public void evaluateTransformedValueFromApplyWithKiePMMLNameValues() {
        // <OutputField name="CUSTOM_FIELD" optype="continuous" dataType="double" feature="transformedValue">
        //     <Apply function="/">
        //        <FieldRef>PARAM_1</FieldRef>
        //        <FieldRef>PARAM_2</FieldRef>
        //      </Apply>
        // </OutputField>
        final KiePMMLFieldRef kiePMMLFieldRef1 = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLFieldRef kiePMMLFieldRef2 = new KiePMMLFieldRef(PARAM_2, Collections.emptyList(), null);
        final KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef1, kiePMMLFieldRef2))
                .build();
        final KiePMMLOutputField outputField = KiePMMLOutputField.builder(CUSTOM_FIELD, Collections.emptyList())
                .withKiePMMLExpression(kiePMMLApply)
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), getKiePMMLNameValues());
        Object retrieved = outputField.evaluate(processingDTO);
        Object expected = value1 / value2;
        assertEquals(expected, retrieved);
    }

    @Test
    public void evaluateTransformedValueFromApplyWithOutputFields() {
        // <OutputField name="CUSTOM_FIELD" optype="continuous" dataType="double" feature="transformedValue">
        //     <Apply function="/">
        //        <FieldRef>PARAM_1</FieldRef>
        //        <FieldRef>PARAM_2</FieldRef>
        //      </Apply>
        // </OutputField>
        final KiePMMLFieldRef kiePMMLFieldRef1 = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLFieldRef kiePMMLFieldRef2 = new KiePMMLFieldRef(PARAM_2, Collections.emptyList(), null);
        final KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef1, kiePMMLFieldRef2))
                .build();
        final KiePMMLOutputField outputField = KiePMMLOutputField.builder(CUSTOM_FIELD, Collections.emptyList())
                .withKiePMMLExpression(kiePMMLApply)
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        getOutputFields(), new ArrayList<>());
        Object retrieved = outputField.evaluate(processingDTO);
        Object expected = value1 / value2;
        assertEquals(expected, retrieved);
    }

    private List<KiePMMLNameValue> getKiePMMLNameValues() {
        return Arrays.asList(new KiePMMLNameValue(PARAM_1, value1), new KiePMMLNameValue(PARAM_2, value2));
    }

    private List<KiePMMLOutputField> getOutputFields() {
        // <OutputField name="PARAM_1" optype="continuous" dataType="double" feature="transformedValue">
        //     <Constant>100.0</Constant>
        // </OutputField>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLOutputField outputField1 = KiePMMLOutputField.builder(PARAM_1, Collections.emptyList())
                .withKiePMMLExpression(kiePMMLConstant1)
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .build();
        // <OutputField name="PARAM_1" optype="continuous" dataType="double" feature="transformedValue">
        //     <Constant>5.0</Constant>
        // </OutputField>
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        final KiePMMLOutputField outputField2 = KiePMMLOutputField.builder(PARAM_2, Collections.emptyList())
                .withKiePMMLExpression(kiePMMLConstant2)
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .build();
        return Arrays.asList(outputField1, outputField2);
    }
}