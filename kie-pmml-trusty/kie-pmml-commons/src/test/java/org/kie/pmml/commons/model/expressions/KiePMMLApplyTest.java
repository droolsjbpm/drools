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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;
import org.kie.pmml.commons.transformations.KiePMMLDefineFunction;
import org.kie.pmml.commons.transformations.KiePMMLDerivedField;
import org.kie.pmml.commons.transformations.KiePMMLParameterField;

import static org.junit.Assert.assertEquals;

public class KiePMMLApplyTest {

    private static final String FIELD_NAME = "FIELD_NAME";
    private static final String CUSTOM_FUNCTION = "CUSTOM_FUNCTION";
    private static final String OUTER_FUNCTION = "OUTER_FUNCTION";
    private static final String PARAM_1 = "PARAM_1";
    private static final String PARAM_2 = "PARAM_2";
    private static final Double value1 = 100.0;
    private static final Double value2 = 5.0;
    private static final Object expected = value1 / value2;

    @Test(expected = IllegalArgumentException.class)
    public void evaluateUnknownFunction() {
        // <Apply function="UNKNOWN">
        //      <Constant>33.0</Constant>
        //      <Constant>27.0</Constant>
        // </Apply>
        String name = "name";
        String function = "UNKNOWN";
        String defaultValue = null;
        String mapMissingTo = null;
        String invalidTreatmentValue = null;
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant("NAME-1", Collections.emptyList(), value1);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant("NAME-1", Collections.emptyList(), value2);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder(name, Collections.emptyList(), function)
                .withKiePMMLExpressions(Arrays.asList(kiePMMLConstant1, kiePMMLConstant2))
                .withDefaultValue(defaultValue)
                .withMapMissingTo(mapMissingTo)
                .withInvalidValueTreatmentMethod(invalidTreatmentValue)
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        kiePMMLApply.evaluate(processingDTO);
    }

    @Test
    public void evaluateFromBuiltIn() {
        // <Apply function="/">
        //      <Constant>33.0</Constant>
        //      <Constant>27.0</Constant>
        // </Apply>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant("NAME-1", Collections.emptyList(), value1);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant("NAME-1", Collections.emptyList(), value2);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLConstant1, kiePMMLConstant2))
                .build();
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Object retrieved = kiePMMLApply.evaluate(processingDTO);
        assertEquals(expected, retrieved);
        //
        // <Apply function="/">
        //      <Constant>33.0</Constant>
        //      <FieldRef>FIELD_NAME</FieldRef>
        // </Apply>
        // Apply with a Constant and a FieldRef: returns kiePMMLConstant1 divided evaluation of FieldRef from
        // kiePMMLNameValues
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(FIELD_NAME, Collections.emptyList(), null);
        kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLConstant1, kiePMMLFieldRef))
                .build();
        List<KiePMMLNameValue> kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue(FIELD_NAME, value2));
        processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), kiePMMLNameValues);
        retrieved = kiePMMLApply.evaluate(processingDTO);
        assertEquals(expected, retrieved);
        // Apply with a Constant and a FieldRef: returns kiePMMLConstant1 divided evaluation of FieldRef from
        // derivedFields
        final KiePMMLDerivedField kiePMMLDerivedField = KiePMMLDerivedField.builder(FIELD_NAME,
                                                                                    Collections.emptyList(),
                                                                                    DATA_TYPE.DOUBLE.getName(),
                                                                                    OP_TYPE.CONTINUOUS.getName(),
                                                                                    kiePMMLConstant2)
                .build();
        final List<KiePMMLDerivedField> derivedFields = Collections.singletonList(kiePMMLDerivedField);
        kiePMMLNameValues = Collections.singletonList(new KiePMMLNameValue("UNKNOWN", "WRONG"));
        processingDTO = new ProcessingDTO(Collections.emptyList(), derivedFields, Collections.emptyList(), kiePMMLNameValues);
        retrieved = kiePMMLApply.evaluate(processingDTO);
        assertEquals(expected, retrieved);
    }

    @Test
    public void evaluateFromDefineFunction() {
        Double valueA = 33.0;
        Double valueB = 27.0;
        // <Apply function="CUSTOM_FUNCTION">
        //      <Constant>33.0</Constant>
        //      <Constant>27.0</Constant>
        // </Apply>
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1"/>
        //     <ParameterField field="PARAM_2"/>
        //     <Apply function="/">
        //        <Constant>100.0</Constant>
        //        <Constant>5.0</Constant>
        //      </Apply>
        // </DefineFunction>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant("NAME-1", Collections.emptyList(), valueA);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant("NAME-1", Collections.emptyList(), valueB);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), CUSTOM_FUNCTION)
                .withKiePMMLExpressions(Arrays.asList(kiePMMLConstant1, kiePMMLConstant2))
                .build();
        List<KiePMMLDefineFunction> defineFunctions = Collections.singletonList(getDefineFunctionApplyFromConstant());
        ProcessingDTO processingDTO = new ProcessingDTO(defineFunctions, Collections.emptyList(), Collections.emptyList(), new ArrayList<>());
        Object retrieved = kiePMMLApply.evaluate(processingDTO);
        assertEquals(expected, retrieved);
        //
        // Apply with a Constant and a FieldRef: returns kiePMMLConstant1 divided evaluation of FieldRef from
        // kiePMMLNameValues
        // <Apply function="CUSTOM_FUNCTION">
        //      <Constant>33.0</Constant>
        //      <Constant>27.0</Constant>
        // </Apply>
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1"/>
        //     <ParameterField field="PARAM_2"/>
        //     <Apply function="/">
        //        <Constant>100.0</Constant>
        //        <FieldRef field="PARAM_2"/>
        //      </Apply>
        // </DefineFunction>
        defineFunctions = Collections.singletonList(getDefineFunctionApplyFromFieldRef());
        processingDTO = new ProcessingDTO(defineFunctions, Collections.emptyList(), Collections.emptyList(), new ArrayList<>());
        retrieved = kiePMMLApply.evaluate(processingDTO);
        Double locallyExpected = value1 / valueB;
        assertEquals(locallyExpected, retrieved);
        //
        // Apply invoking another custom function
        // <Apply function="OUTER_FUNCTION">
        //  <Constant>33.0</Constant>
        //  <Constant>27.0</Constant>
        // </Apply>
        // <DefineFunction name="OUTER_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1"/>
        //     <ParameterField field="PARAM_2"/>
        //     <Apply function="CUSTOM_FUNCTION">
        //        <FieldRef field="PARAM_1"/>
        //        <FieldRef field="PARAM_2"/>
        //      </Apply>
        // </DefineFunction>
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1"/>
        //     <ParameterField field="PARAM_2"/>
        //     <Apply function="/">
        //        <Constant>100.0</Constant>
        //        <FieldRef field="PARAM_2"/>
        //      </Apply>
        // </DefineFunction>
        kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), OUTER_FUNCTION)
                .withKiePMMLExpressions(Arrays.asList(kiePMMLConstant1, kiePMMLConstant2))
                .build();
        defineFunctions = Arrays.asList(getDefineFunctionApplyFromFieldRef(),
                                        getDefineFunctionApplyFromCustomFunction());
        processingDTO = new ProcessingDTO(defineFunctions, Collections.emptyList(), Collections.emptyList(), new ArrayList<>());
        retrieved = kiePMMLApply.evaluate(processingDTO);
        assertEquals(locallyExpected, retrieved);
    }

    private KiePMMLDefineFunction getDefineFunctionApplyFromConstant() {
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1"/>
        //     <ParameterField field="PARAM_2"/>
        //     <Apply function="/">
        //        <Constant>100.0</Constant>
        //        <Constant>5.0</Constant>
        //      </Apply>
        // </DefineFunction>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLConstant kiePMMLConstant2 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLConstant1, kiePMMLConstant2))
                .build();
        final KiePMMLParameterField parameterField1 =
                KiePMMLParameterField.builder(PARAM_1, Collections.emptyList()).build();
        final KiePMMLParameterField parameterField2 =
                KiePMMLParameterField.builder(PARAM_2, Collections.emptyList()).build();
        return new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections.emptyList(),
                                         OP_TYPE.CONTINUOUS.getName(),
                                         Arrays.asList(parameterField1,
                                                       parameterField2),
                                         kiePMMLApply);
    }

    private KiePMMLDefineFunction getDefineFunctionApplyFromFieldRef() {
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1"/>
        //     <ParameterField field="PARAM_2"/>
        //     <Apply function="/">
        //        <Constant>100.0</Constant>
        //        <FieldRef field="PARAM_2"/>
        //      </Apply>
        // </DefineFunction>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(PARAM_2, Collections.emptyList(), null);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLConstant1, kiePMMLFieldRef))
                .build();
        final KiePMMLParameterField parameterField1 =
                KiePMMLParameterField.builder(PARAM_1, Collections.emptyList()).build();
        final KiePMMLParameterField parameterField2 =
                KiePMMLParameterField.builder(PARAM_2, Collections.emptyList()).build();
        return new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections.emptyList(),
                                         OP_TYPE.CONTINUOUS.getName(),
                                         Arrays.asList(parameterField1,
                                                       parameterField2),
                                         kiePMMLApply);
    }

    private KiePMMLDefineFunction getDefineFunctionApplyFromCustomFunction() {
        // <DefineFunction name="OUTER_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1"/>
        //     <ParameterField field="PARAM_2"/>
        //     <Apply function="CUSTOM_FUNCTION">
        //        <FieldRef field="PARAM_1"/>
        //        <FieldRef field="PARAM_2"/>
        //      </Apply>
        // </DefineFunction>
        final KiePMMLFieldRef kiePMMLFieldRef1 = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLFieldRef kiePMMLFieldRef2 = new KiePMMLFieldRef(PARAM_2, Collections.emptyList(), null);
        KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), CUSTOM_FUNCTION)
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef1, kiePMMLFieldRef2))
                .build();
        final KiePMMLParameterField parameterField1 =
                KiePMMLParameterField.builder(PARAM_1, Collections.emptyList()).build();
        final KiePMMLParameterField parameterField2 =
                KiePMMLParameterField.builder(PARAM_2, Collections.emptyList()).build();
        return new KiePMMLDefineFunction(OUTER_FUNCTION, Collections.emptyList(),
                                         OP_TYPE.CONTINUOUS.getName(),
                                         Arrays.asList(parameterField1,
                                                       parameterField2),
                                         kiePMMLApply);
    }
}