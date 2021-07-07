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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.api.pmml.ParameterInfo;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.MINING_FUNCTION;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.api.runtime.PMMLContext;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.expressions.KiePMMLApply;
import org.kie.pmml.commons.model.expressions.KiePMMLConstant;
import org.kie.pmml.commons.model.expressions.KiePMMLFieldRef;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;
import org.kie.pmml.commons.testingutility.KiePMMLTestingModel;
import org.kie.pmml.commons.transformations.KiePMMLDefineFunction;
import org.kie.pmml.commons.transformations.KiePMMLDerivedField;
import org.kie.pmml.commons.transformations.KiePMMLParameterField;
import org.kie.pmml.commons.transformations.KiePMMLTransformationDictionary;
import org.kie.pmml.evaluator.core.PMMLContextImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreProcessTest {

    private static final String CUSTOM_FUNCTION = "CUSTOM_FUNCTION";
    private static final String CUSTOM_FIELD = "CUSTOM_FIELD";
    private static final String CUSTOM_REF_FIELD = "CUSTOM_REF_FIELD";
    private static final String INPUT_FIELD = "INPUT_FIELD";
    private static final String PARAM_1 = "PARAM_1";
    private static final String PARAM_2 = "PARAM_2";
    private static final Double value1 = 100.0;
    private static final Double value2 = 5.0;

    @Test
    public void addMissingValuesReplacements() {
        Map<String, Object> missingValueReplacementMap = new HashMap<>();
        missingValueReplacementMap.put("fieldA", "one");
        missingValueReplacementMap.put("fieldB", 2);
        KiePMMLTestingModel model = KiePMMLTestingModel.builder("TESTINGMODEL", Collections.emptyList(), MINING_FUNCTION.REGRESSION)
                .withMissingValueReplacementMap(missingValueReplacementMap)
                .build();
        PMMLRequestData pmmlRequestData = new PMMLRequestData("123", "modelName");
        pmmlRequestData.addRequestParam("age", 123);
        pmmlRequestData.addRequestParam("work", "work");
        PMMLContext pmmlContext = new PMMLContextImpl(pmmlRequestData);
        missingValueReplacementMap.keySet().forEach(key -> {
            assertFalse(pmmlContext.getRequestData().getMappedRequestParams().containsKey(key));
            assertFalse(pmmlContext.getMissingValueReplacedMap().containsKey(key));
        });
        PreProcess.addMissingValuesReplacements(model, pmmlContext);
        missingValueReplacementMap.forEach((key, value) -> {
            assertTrue(pmmlContext.getRequestData().getMappedRequestParams().containsKey(key));
            final ParameterInfo<?> parameterInfo = pmmlContext.getRequestData().getMappedRequestParams().get(key);
            assertEquals(key, parameterInfo.getName());
            assertEquals(value.getClass(), parameterInfo.getType());
            assertEquals(value, parameterInfo.getValue());
            assertTrue(pmmlContext.getMissingValueReplacedMap().containsKey(key));
            assertEquals(value, pmmlContext.getMissingValueReplacedMap().get(key));
        });
    }

    @Test
    public void executeTransformations() {
        // <DefineFunction name="CUSTOM_FUNCTION" optype="continuous" dataType="double">
        //     <ParameterField name="PARAM_1" />
        //     <ParameterField name="PARAM_2" />
        //     <Apply function="/">
        //        <FieldRef>PARAM_1</FieldRef>
        //        <FieldRef>PARAM_2</FieldRef>
        //      </Apply>
        // </DefineFunction>
        final KiePMMLParameterField kiePMMLParameterField1 = KiePMMLParameterField.builder(PARAM_1, Collections.emptyList()).build();
        final KiePMMLParameterField kiePMMLParameterField2 = KiePMMLParameterField.builder(PARAM_2, Collections.emptyList()).build();
        final KiePMMLFieldRef kiePMMLFieldRef1 = new KiePMMLFieldRef(PARAM_1, Collections.emptyList(), null);
        final KiePMMLFieldRef kiePMMLFieldRef2 = new KiePMMLFieldRef(PARAM_2, Collections.emptyList(), null);
        final KiePMMLApply kiePMMLApplyRef = KiePMMLApply.builder("NAMEREF", Collections.emptyList(), "/")
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef1, kiePMMLFieldRef2))
                .build();
        final KiePMMLDefineFunction defineFunction = new KiePMMLDefineFunction(CUSTOM_FUNCTION, Collections.emptyList(),
                                                                               OP_TYPE.CONTINUOUS.getName(),
                                                                               Arrays.asList(kiePMMLParameterField1, kiePMMLParameterField2),
                                                                               kiePMMLApplyRef);

        // <DerivedField name="CUSTOM_REF_FIELD" optype="continuous" dataType="double">
        //     <Apply function="CUSTOM_FUNCTION">
        //        <FieldRef>INPUT_FIELD</FieldRef>
        //        <Constant>5.0</Constant>
        //      </Apply>
        // </DerivedField>
        final KiePMMLFieldRef kiePMMLFieldRef3 = new KiePMMLFieldRef(INPUT_FIELD, Collections.emptyList(), null);
        final KiePMMLConstant kiePMMLConstant1 = new KiePMMLConstant(PARAM_2, Collections.emptyList(), value2);
        final KiePMMLApply kiePMMLApply = KiePMMLApply.builder("NAME", Collections.emptyList(), CUSTOM_FUNCTION)
                .withKiePMMLExpressions(Arrays.asList(kiePMMLFieldRef3, kiePMMLConstant1))
                .build();
        final KiePMMLDerivedField derivedField = KiePMMLDerivedField.builder(CUSTOM_REF_FIELD, Collections.emptyList(),
                                                                             DATA_TYPE.DOUBLE.getName(),
                                                                             OP_TYPE.CONTINUOUS.getName(),
                                                                             kiePMMLApply).build();
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
        final PMMLRequestData pmmlRequestData = new PMMLRequestData();
        pmmlRequestData.addRequestParam(INPUT_FIELD, value1);
        Map<String, ParameterInfo> mappedRequestParams = pmmlRequestData.getMappedRequestParams();
        final List<KiePMMLNameValue> kiePMMLNameValues = PreProcess.getKiePMMLNameValuesFromParameterInfos(mappedRequestParams.values());
        Optional<KiePMMLNameValue> retrieved = kiePMMLNameValues.stream().filter(kiePMMLNameValue -> kiePMMLNameValue.getName().equals(INPUT_FIELD)).findFirst();
        assertTrue(retrieved.isPresent());
        assertEquals(value1, retrieved.get().getValue());

        ProcessingDTO processingDTO = new ProcessingDTO(kiePMMLModel, kiePMMLNameValues, new ArrayList<>());
        PreProcess.executeTransformations(processingDTO, pmmlRequestData);
        mappedRequestParams = pmmlRequestData.getMappedRequestParams();

        Object expected = value1 / value2;
        assertTrue(mappedRequestParams.containsKey(CUSTOM_REF_FIELD));
        assertEquals(expected, mappedRequestParams.get(CUSTOM_REF_FIELD).getValue());
        retrieved = kiePMMLNameValues.stream().filter(kiePMMLNameValue -> kiePMMLNameValue.getName().equals(CUSTOM_REF_FIELD)).findFirst();
        assertTrue(retrieved.isPresent());
        assertEquals(expected, retrieved.get().getValue());
    }

}