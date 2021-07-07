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

import org.junit.Test;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.expressions.KiePMMLApply;
import org.kie.pmml.commons.model.expressions.KiePMMLConstant;
import org.kie.pmml.commons.model.expressions.KiePMMLFieldRef;

import static org.junit.Assert.assertEquals;

public class KiePMMLDefineFunctionTest {

    private static final String CUSTOM_FUNCTION = "CUSTOM_FUNCTION";
    private static final String PARAM_1 = "PARAM_1";
    private static final String PARAM_2 = "PARAM_2";
    private static final Double value1 = 100.0;
    private static final Double value2 = 5.0;

    @Test(expected = IllegalArgumentException.class)
    public void evaluateNoParamValues() {
        final KiePMMLParameterField parameterField1 =
                KiePMMLParameterField.builder(PARAM_1, Collections.emptyList()).build();
        final KiePMMLParameterField parameterField2 =
                KiePMMLParameterField.builder(PARAM_2, Collections.emptyList()).build();
        final KiePMMLDefineFunction defineFunction = new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections.emptyList(),
                                                                               OP_TYPE.CONTINUOUS.getName(),
                                                                               Arrays.asList(parameterField1,
                                                                                             parameterField2),
                                                                               null);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), Collections.emptyList());
        defineFunction.evaluate(processingDTO, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void evaluateEmptyParamValues() {
        final KiePMMLParameterField parameterField1 = KiePMMLParameterField.builder(PARAM_1, Collections.emptyList
        ()).build();
        final KiePMMLParameterField parameterField2 = KiePMMLParameterField.builder(PARAM_2, Collections.emptyList
        ()).build();
        final KiePMMLDefineFunction defineFunction = new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections
        .emptyList(),
                                                                               OP_TYPE.CONTINUOUS.getName(),
                                                                               Arrays.asList(parameterField1,
                                                                                             parameterField2),
                                                                               null);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), Collections.emptyList());
        defineFunction.evaluate(processingDTO, Collections.emptyList());
    }

    @Test
    public void evaluateFromConstant() {
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <Constant>100.0</Constant>
        // </DefineFunction>
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_1, Collections.emptyList(), value1);
        final KiePMMLDefineFunction defineFunction = new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections.emptyList(),
                                                                               OP_TYPE.CONTINUOUS.getName(),
                                                                               Collections.emptyList(),
                                                                               kiePMMLConstant1);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), Collections.emptyList());
        Object retrieved = defineFunction.evaluate(processingDTO, Collections.emptyList());
        assertEquals(value1, retrieved);
    }

    @Test
    public void evaluateFromFieldRef() {
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1"/>
        //     <FieldRef field="PARAM_1"/>
        // </DefineFunction>
        final KiePMMLFieldRef kiePMMLFieldRef = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLDefineFunction defineFunction = new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections.emptyList(),
                                                                               OP_TYPE.CONTINUOUS.getName(),
                                                                               Collections.singletonList(KiePMMLParameterField.builder(PARAM_1, Collections.emptyList()).build()),
                                                                               kiePMMLFieldRef);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), new ArrayList<>());
        Object retrieved = defineFunction.evaluate(processingDTO, Collections.singletonList(value1));
        assertEquals(value1, retrieved);
    }

    @Test
    public void evaluateFromApply() {
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1"/>
        //     <ParameterField field="PARAM_2"/>
        //     <Apply function="/">
        //        <FieldRef>PARAM_1</FieldRef>
        //        <FieldRef>PARAM_2</FieldRef>
        //      </Apply>
        // </DefineFunction>
        final KiePMMLFieldRef kiePMMLFieldRef1 = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLFieldRef kiePMMLFieldRef2 = new KiePMMLFieldRef(PARAM_2, Collections.emptyList(), null);
        final KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef1, kiePMMLFieldRef2))
                .build();
        final KiePMMLParameterField parameterField1 =
                KiePMMLParameterField.builder(PARAM_1, Collections.emptyList()).build();
        final KiePMMLParameterField parameterField2 =
                KiePMMLParameterField.builder(PARAM_2, Collections.emptyList()).build();
        final KiePMMLDefineFunction defineFunction = new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections.emptyList(),
                                                                               OP_TYPE.CONTINUOUS.getName(),
                                                                               Arrays.asList(parameterField1,
                                                                                             parameterField2),
                                                                               kiePMMLApply);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(),
                                                        Collections.emptyList(), new ArrayList<>());
        Object retrieved = defineFunction.evaluate(processingDTO, Arrays.asList(value1, value2));
        Object expected = value1 / value2;
        assertEquals(expected, retrieved);
    }
}