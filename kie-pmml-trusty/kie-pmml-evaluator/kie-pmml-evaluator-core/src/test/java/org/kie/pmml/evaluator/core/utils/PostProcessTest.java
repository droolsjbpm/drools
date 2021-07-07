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

package org.kie.pmml.evaluator.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.kie.api.pmml.PMML4Result;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.MINING_FUNCTION;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.api.enums.RESULT_FEATURE;
import org.kie.pmml.api.enums.ResultCode;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.commons.model.KiePMMLOutputField;
import org.kie.pmml.commons.model.KiePMMLTarget;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.expressions.KiePMMLApply;
import org.kie.pmml.commons.model.expressions.KiePMMLConstant;
import org.kie.pmml.commons.model.expressions.KiePMMLFieldRef;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;
import org.kie.pmml.commons.testingutility.KiePMMLTestingModel;
import org.kie.pmml.commons.transformations.KiePMMLDefineFunction;
import org.kie.pmml.commons.transformations.KiePMMLDerivedField;
import org.kie.pmml.commons.transformations.KiePMMLLocalTransformations;
import org.kie.pmml.commons.transformations.KiePMMLParameterField;
import org.kie.pmml.commons.transformations.KiePMMLTransformationDictionary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostProcessTest {

    private static final String CUSTOM_FUNCTION = "CUSTOM_FUNCTION";
    private static final String CUSTOM_FIELD = "CUSTOM_FIELD";
    private static final String CUSTOM_REF_FIELD = "CUSTOM_REF_FIELD";
    private static final String PARAM_1 = "PARAM_1";
    private static final String PARAM_2 = "PARAM_2";
    private static final Double value1 = 100.0;
    private static final Double value2 = 5.0;

    @Test
    public void executeTargets() {
        // Build model
        String TARGET_NAME = "TARGET_NAME";
        String FIELD_NAME = "FIELD_NAME";
        KiePMMLTarget kiePMMLTarget = KiePMMLTarget.builder(TARGET_NAME, Collections.emptyList())
                .withMin(4.34)
                .withField(FIELD_NAME)
                .build();
        List<KiePMMLTarget> kiePMMLTargets = Arrays.asList(kiePMMLTarget, KiePMMLTarget.builder("NEW_TARGET",
                                                                                                Collections.emptyList()).build());
        KiePMMLTestingModel model = KiePMMLTestingModel.builder("TESTINGMODEL", Collections.emptyList(),
                                                                        MINING_FUNCTION.REGRESSION)
                        .withKiePMMLTargets(kiePMMLTargets)
                        .build();
        // Build PMML4Result
        PMML4Result toModify = new PMML4Result();
        toModify.setResultCode(ResultCode.FAIL.getName());
        toModify.addResultVariable(FIELD_NAME, 4.33);
        assertEquals(4.33, toModify.getResultVariables().get(FIELD_NAME));
        ProcessingDTO processingDTO = getProcessingDTO(model, new ArrayList(),  new ArrayList());
        PostProcess.executeTargets(toModify, processingDTO);
        assertEquals(4.33, toModify.getResultVariables().get(FIELD_NAME));
        toModify.setResultCode(ResultCode.OK.getName());
        PostProcess.executeTargets(toModify, processingDTO);
        assertEquals(4.33, toModify.getResultVariables().get(FIELD_NAME));
        toModify.setResultObjectName(FIELD_NAME);
        PostProcess.executeTargets(toModify, processingDTO);
        assertEquals(4.34, toModify.getResultVariables().get(FIELD_NAME));
    }

    @Test
    public void populatePredictedOutputField() {
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.PREDICTED_VALUE)
                .build();
        final PMML4Result toUpdate = new PMML4Result();
        final List<KiePMMLNameValue> kiePMMLNameValues = IntStream.range(0, 3).mapToObj(i -> new KiePMMLNameValue(
                "val-" + i, i)).collect(Collectors.toList());
        KiePMMLTestingModel kiePMMLModel =
                KiePMMLTestingModel.builder("TESTINGMODEL", Collections.emptyList(),
                                                                        MINING_FUNCTION.REGRESSION)
                        .build();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, kiePMMLNameValues,  new ArrayList());
        PostProcess.populatePredictedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertTrue(toUpdate.getResultVariables().isEmpty());
        //
        final String targetField = "targetField";
        outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.PREDICTED_VALUE)
                .withTargetField(targetField)
                .build();
        final Object targetValue = 54346.32454;
        final KiePMMLNameValue kiePMMLNameValue = new KiePMMLNameValue(targetField, targetValue);
        kiePMMLNameValues.add(kiePMMLNameValue);
        processingDTO = getProcessingDTO(kiePMMLModel, kiePMMLNameValues,  new ArrayList());
        PostProcess.populatePredictedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(kiePMMLNameValue.getValue(), toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test(expected = KiePMMLException.class)
    public void populatePredictedOutputFieldWrongResultFeature() {
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.ANTECEDENT)
                .build();
        KiePMMLTestingModel kiePMMLModel =
                KiePMMLTestingModel.builder("TESTINGMODEL", Collections.emptyList(),
                                                                        MINING_FUNCTION.REGRESSION)
                        .build();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   new PMML4Result(),
                                                   processingDTO);
    }

    @Test
    public void populateTransformedOutputField() {
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .build();
        final PMML4Result toUpdate = new PMML4Result();
        final List<KiePMMLNameValue> kiePMMLNameValues = IntStream.range(0, 3).mapToObj(i -> new KiePMMLNameValue(
                "val-" + i, i)).collect(Collectors.toList());
        KiePMMLTestingModel kiePMMLModel =
                KiePMMLTestingModel.builder("TESTINGMODEL", Collections.emptyList(),
                                                                        MINING_FUNCTION.REGRESSION)
                        .build();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, kiePMMLNameValues,  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertTrue(toUpdate.getResultVariables().isEmpty());
        //
        final String value = "String";
        final String CONSTANT_NAME = "CONSTANT_NAME";
        KiePMMLConstant kiePMMLConstant = new KiePMMLConstant(CONSTANT_NAME, Collections.emptyList(), value);

        outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLConstant)
                .build();

        processingDTO = getProcessingDTO(kiePMMLModel, kiePMMLNameValues,  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(kiePMMLConstant.getValue(), toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test(expected = KiePMMLException.class)
    public void populateTransformedOutputFieldWrongResultFeature() {
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.ANTECEDENT)
                .build();
        KiePMMLTestingModel kiePMMLModel =
                KiePMMLTestingModel.builder("TESTINGMODEL", Collections.emptyList(),
                                                                        MINING_FUNCTION.REGRESSION)
                        .build();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   new PMML4Result(),
                                                   processingDTO);
    }

    @Test
    public void populateTransformedOutputFieldWithApplyWithConstants() {
        //     <Apply function="/">
        //        <Constant>100.0</Constant>
        //        <Constant>5.0</Constant>
        //      </Apply>
        final String functionName = "/";
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), functionName)
                .withKiePMMLExpressions(Arrays.asList(kiePMMLConstant1, kiePMMLConstant2))
                .build();

        KiePMMLTestingModel kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .build();
        //
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLApply)
                .build();
        final PMML4Result toUpdate = new PMML4Result();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        Object expected = value1 / value2;
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test
    public void populateTransformedOutputFieldWithApplyDerivedFieldFromConstant() {
        // <DerivedField name="CUSTOM_FIELD" optype="continuous" dataType="double">
        //        <Constant>100.0</Constant>
        // </DerivedField>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLDerivedField derivedField = KiePMMLDerivedField.builder(CUSTOM_FIELD, Collections.emptyList(),
                                                                             DATA_TYPE.DOUBLE.getName(),
                                                                             OP_TYPE.CONTINUOUS.getName(),
                                                                             kiePMMLConstant1)
                .build();
        //     <Apply function="/">
        //        <FieldRef>CUSTOM_FIELD</FieldRef>
        //        <Constant>5.0</Constant>
        //      </Apply>
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(CUSTOM_FIELD, Collections.emptyList(), null);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef, kiePMMLConstant2))
                .build();
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLApply)
                .build();
        Object expected = value1 / value2;
        // From TransformationDictionary
        KiePMMLTransformationDictionary transformationDictionary = KiePMMLTransformationDictionary.builder(
                "transformationDictionary", Collections.emptyList())
                .withDerivedFields(Collections.singletonList(derivedField))
                .build();
        KiePMMLTestingModel kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withTransformationDictionary(transformationDictionary)
                .build();
        //
        PMML4Result toUpdate = new PMML4Result();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
        // From LocalTransformations
        KiePMMLLocalTransformations localTransformations = KiePMMLLocalTransformations.builder("localTransformations"
                , Collections.emptyList())
                .withDerivedFields(Collections.singletonList(derivedField))
                .build();
        kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withLocalTransformations(localTransformations)
                .build();
        //
        toUpdate = new PMML4Result();
        processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test
    public void populateTransformedOutputFieldWithApplyDerivedFieldFromFieldRef() {
        // <DerivedField name="CUSTOM_REF_FIELD" optype="continuous" dataType="double">
        //        <Constant>100.0</Constant>
        // </DerivedField>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLDerivedField derivedField1 = KiePMMLDerivedField.builder(CUSTOM_REF_FIELD, Collections
        .emptyList(),
                                                                              DATA_TYPE.DOUBLE.getName(),
                                                                              OP_TYPE.CONTINUOUS.getName(),
                                                                              kiePMMLConstant1).build();

        // <DerivedField name="CUSTOM_FIELD" optype="continuous" dataType="double">
        //        <FieldRef>CUSTOM_REF_FIELD</FieldRef>
        // </DerivedField>
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(CUSTOM_REF_FIELD, Collections.emptyList(), null);
        final KiePMMLDerivedField derivedField2 = KiePMMLDerivedField.builder(CUSTOM_FIELD, Collections.emptyList(),
                                                                              DATA_TYPE.DOUBLE.getName(),
                                                                              OP_TYPE.CONTINUOUS.getName(),
                                                                              kiePMMLFieldRef).build();
        //     <Apply function="/">
        //        <FieldRef>CUSTOM_FIELD</FieldRef>
        //        <Constant>5.0</Constant>
        //      </Apply>
        final KiePMMLFieldRef kiePMMLFieldRef2 = new KiePMMLFieldRef(CUSTOM_FIELD, Collections.emptyList(), null);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef2, kiePMMLConstant2))
                .build();
        Object expected = value1 / value2;
        // From TransformationDictionary
        KiePMMLTransformationDictionary transformationDictionary = KiePMMLTransformationDictionary.builder(
                "transformationDictionary", Collections.emptyList())
                .withDerivedFields(Arrays.asList(derivedField1, derivedField2))
                .build();
        KiePMMLTestingModel kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withTransformationDictionary(transformationDictionary)
                .build();
        //
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLApply)
                .build();
        PMML4Result toUpdate = new PMML4Result();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
        // From LocalTransformations
        KiePMMLLocalTransformations localTransformations = KiePMMLLocalTransformations.builder("localTransformations"
                , Collections.emptyList())
                .withDerivedFields(Arrays.asList(derivedField1, derivedField2))
                .build();
        kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withLocalTransformations(localTransformations)
                .build();
        //
        toUpdate = new PMML4Result();
        processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test
    public void populateTransformedOutputFieldWithApplyDerivedFieldFromApply() {
        // <DerivedField name="CUSTOM_FIELD" optype="continuous" dataType="double">
        //     <Apply function="+">
        //        <Constant>64.0</Constant>
        //        <Constant>36.0</Constant>
        //      </Apply>
        // </DerivedField>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), 64.0);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), 36);
        final KiePMMLApply kiePMMLApplyRef = KiePMMLApply.builder("NAMEREF", Collections.emptyList(), "+")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLConstant1, kiePMMLConstant2))
                .build();
        final KiePMMLDerivedField derivedField = KiePMMLDerivedField.builder(CUSTOM_FIELD, Collections.emptyList(),
                                                                              DATA_TYPE.DOUBLE.getName(),
                                                                              OP_TYPE.CONTINUOUS.getName(),
                                                                              kiePMMLApplyRef).build();
        //     <Apply function="/">
        //        <FieldRef>CUSTOM_FIELD</FieldRef>
        //        <Constant>5.0</Constant>
        //      </Apply>
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(CUSTOM_FIELD, Collections.emptyList(), null);
        final KiePMMLConstant kiePMMLConstant3 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef, kiePMMLConstant3))
                .build();
        Object expected = value1 / value2;
        // From TransformationDictionary
        KiePMMLTransformationDictionary transformationDictionary = KiePMMLTransformationDictionary.builder(
                "transformationDictionary", Collections.emptyList())
                .withDerivedFields(Collections.singletonList(derivedField))
                .build();
        KiePMMLTestingModel kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withTransformationDictionary(transformationDictionary)
                .build();
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLApply)
                .build();
        PMML4Result toUpdate = new PMML4Result();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
        // From LocalTransformations
        KiePMMLLocalTransformations localTransformations = KiePMMLLocalTransformations.builder("localTransformations"
                , Collections.emptyList())
                .withDerivedFields(Collections.singletonList(derivedField))
                .build();
        kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withLocalTransformations(localTransformations)
                .build();
        //
        toUpdate = new PMML4Result();
        processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test
    public void populateTransformedOutputFieldWithApplyDefineFunctionFromConstant() {
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <Constant>100.0</Constant>
        // </DefineFunction>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLDefineFunction defineFunction = new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections
        .emptyList(),
                                                                               OP_TYPE.CONTINUOUS.getName(),
                                                                               Collections.emptyList(),
                                                                               kiePMMLConstant1);
        //     <Apply function="CUSTOM_FUNCTION">
        //        <FieldRef>CUSTOM_FIELD</FieldRef>
        //        <Constant>5.0</Constant>
        //      </Apply>
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(CUSTOM_FIELD, Collections.emptyList(), null);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), CUSTOM_FUNCTION)
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef, kiePMMLConstant2))
                .build();
        Object expected = value1;
        // From TransformationDictionary
        KiePMMLTransformationDictionary transformationDictionary = KiePMMLTransformationDictionary.builder(
                "transformationDictionary", Collections.emptyList())
                .withDefineFunctions(Collections.singletonList(defineFunction))
                .build();
        KiePMMLTestingModel kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withTransformationDictionary(transformationDictionary)
                .build();
        //
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLApply)
                .build();
        PMML4Result toUpdate = new PMML4Result();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test
    public void populateTransformedOutputFieldWithApplyDefineFunctionFromFieldRef() {
        // <DerivedField name="CUSTOM_REF_FIELD" optype="continuous" dataType="double">
        //        <Constant>100.0</Constant>
        // </DerivedField>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLDerivedField derivedField = KiePMMLDerivedField.builder(CUSTOM_REF_FIELD, Collections
        .emptyList(),
                                                                              DATA_TYPE.DOUBLE.getName(),
                                                                              OP_TYPE.CONTINUOUS.getName(),
                                                                              kiePMMLConstant1).build();
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <FieldRef>CUSTOM_REF_FIELD</FieldRef>
        // </DefineFunction>
        final KiePMMLFieldRef kiePMMLFieldRef1 = new KiePMMLFieldRef(CUSTOM_REF_FIELD, Collections.emptyList(), null);
        final KiePMMLDefineFunction defineFunction = new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections
        .emptyList(),
                                                                               OP_TYPE.CONTINUOUS.getName(),
                                                                               Collections.emptyList(),
                                                                               kiePMMLFieldRef1);
        //     <Apply function="CUSTOM_FUNCTION">
        //        <Constant>5.0</Constant>
        //      </Apply>
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), CUSTOM_FUNCTION)
                .withKiePMMLExpressions(Collections.singletonList(kiePMMLConstant2))
                .build();
        Object expected = value1;
        // From TransformationDictionary
        KiePMMLTransformationDictionary transformationDictionary = KiePMMLTransformationDictionary.builder(
                "transformationDictionary", Collections.emptyList())
                .withDefineFunctions(Collections.singletonList(defineFunction))
                .withDerivedFields(Collections.singletonList(derivedField))
                .build();
        KiePMMLTestingModel kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withTransformationDictionary(transformationDictionary)
                .build();
        //
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLApply)
                .build();
        PMML4Result toUpdate = new PMML4Result();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test
    public void populateTransformedOutputFieldWithApplyDefineFunctionFromApply() {
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1" />
        //     <Apply function="/">
        //        <FieldRef>PARAM_1</FieldRef>
        //        <Constant>5.0</Constant>
        //      </Apply>
        // </DefineFunction>
        final KiePMMLParameterField kiePMMLParameterField = KiePMMLParameterField.builder(PARAM_1, Collections
        .emptyList()).build();
        final KiePMMLFieldRef kiePMMLFieldRef1 = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        final KiePMMLApply kiePMMLApplyRef = KiePMMLApply.builder("NAMEREF", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef1, kiePMMLConstant2))
                .build();
        final KiePMMLDefineFunction defineFunction = new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections
        .emptyList(),
                                                                               OP_TYPE.CONTINUOUS.getName(),
                                                                               Collections.singletonList
                                                                               (kiePMMLParameterField),
                                                                               kiePMMLApplyRef);
        //     <Apply function="CUSTOM_FUNCTION">
        //        <Constant>100.0</Constant>
        //      </Apply>
        final KiePMMLConstant kiePMMLConstant3 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value1);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), CUSTOM_FUNCTION)
                .withKiePMMLExpressions(Collections.singletonList(kiePMMLConstant3))
                .build();
        Object expected = value1/value2;
        // From TransformationDictionary
        KiePMMLTransformationDictionary transformationDictionary = KiePMMLTransformationDictionary.builder(
                "transformationDictionary", Collections.emptyList())
                .withDefineFunctions(Collections.singletonList(defineFunction))
                .build();
        KiePMMLTestingModel kiePMMLModel = KiePMMLTestingModel
                .builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withTransformationDictionary(transformationDictionary)
                .build();
        //
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLApply)
                .build();
        PMML4Result toUpdate = new PMML4Result();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList<>(), new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(expected, toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test
    public void populateTransformedOutputFieldWithConstant() {
        final String value = "String";
        KiePMMLConstant kiePMMLConstant = new KiePMMLConstant("NAME", Collections.emptyList(), value);

        KiePMMLTestingModel kiePMMLModel =
                KiePMMLTestingModel.builder("TESTINGMODEL", Collections.emptyList(),
                                                                        MINING_FUNCTION.REGRESSION)
                        .build();
        //
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLConstant)
                .build();
        PMML4Result toUpdate = new PMML4Result();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, new ArrayList(),  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(kiePMMLConstant.getValue(), toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    @Test
    public void populateTransformedOutputFieldWithFieldRef() {
        final String mapMissingTo = "mapMissingTo";
        final String variableName = "variableName";
        KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(variableName, Collections.emptyList(), mapMissingTo);
        KiePMMLTestingModel kiePMMLModel =
                KiePMMLTestingModel.builder("TESTINGMODEL", Collections.emptyList(),
                                                                        MINING_FUNCTION.REGRESSION)
                        .build();
        final List<KiePMMLNameValue> kiePMMLNameValues = IntStream.range(0, 3).mapToObj(i -> new KiePMMLNameValue(
                "val-" + i, i)).collect(Collectors.toList());
        final Object variableValue = 543.65434;
        kiePMMLNameValues.add(new KiePMMLNameValue(variableName, variableValue));
        final String OUTPUT_NAME = "OUTPUT_NAME";
        KiePMMLOutputField outputField = KiePMMLOutputField.builder(OUTPUT_NAME, Collections.emptyList())
                .withResultFeature(RESULT_FEATURE.TRANSFORMED_VALUE)
                .withKiePMMLExpression(kiePMMLFieldRef)
                .build();
        PMML4Result toUpdate = new PMML4Result();
        ProcessingDTO processingDTO = getProcessingDTO(kiePMMLModel, kiePMMLNameValues,  new ArrayList());
        PostProcess.populateTransformedOutputField(outputField,
                                                   toUpdate,
                                                   processingDTO);
        assertFalse(toUpdate.getResultVariables().isEmpty());
        assertTrue(toUpdate.getResultVariables().containsKey(OUTPUT_NAME));
        assertEquals(variableValue, toUpdate.getResultVariables().get(OUTPUT_NAME));
    }

    private ProcessingDTO getProcessingDTO(final KiePMMLModel model,
                                           final List<KiePMMLNameValue> kiePMMLNameValues,
                                           final List<String> orderedReasonCodes) {
        return new ProcessingDTO(model, kiePMMLNameValues, orderedReasonCodes);
    }
}