/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.kie.pmml.models.tree.tests;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.pmml.PMML4Result;
import org.kie.pmml.api.runtime.PMMLRuntime;
import org.kie.pmml.models.tests.AbstractPMMLTest;


@RunWith(Parameterized.class)
public class SampleMineTreeModelWithTransformationsTest extends AbstractPMMLTest {

    private static final String FILE_NAME = "SampleMineTreeModelWithTransformations.pmml";
    private static final String MODEL_NAME = "SampleMineTreeModelWithTransformations";
    private static final String TARGET_FIELD = "decision";
    private static final String OUT_DER_TEMPERATURE = "out_der_temperature";
    private static final String OUT_DER_FUN_HUMIDITY_APPLY = "out_der_fun_humidity_apply";
    private static final String OUT_DER_CONSTANT = "out_der_constant";
    private static final String CONSTANT = "constant";
    private static final String WEATHERDECISION = "weatherdecision";
    private static final String OUT_NORMDISCRETE_FIELD = "out_normdiscrete_field";

    private static PMMLRuntime pmmlRuntime;

    private double temperature;
    private double humidity;
    private String expectedResult;

    public SampleMineTreeModelWithTransformationsTest(double temperature, double humidity, String expectedResult) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.expectedResult = expectedResult;
    }

    @BeforeClass
    public static void setupClass() {
        pmmlRuntime = getPMMLRuntime(FILE_NAME);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {30.0, 10.0, "sunglasses"},
                {5.0, 70.0, "umbrella"},
                {10.0, 15.0, "nothing"}
        });
    }

    @Test
    public void testSetPredicateTree() {
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("temperature", temperature);
        inputData.put("humidity", humidity);
        PMML4Result pmml4Result = evaluate(pmmlRuntime, inputData, MODEL_NAME);
        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isEqualTo(expectedResult);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_TEMPERATURE)).isEqualTo(temperature);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_HUMIDITY_APPLY)).isEqualTo(humidity);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_CONSTANT)).isEqualTo(CONSTANT);
        Assertions.assertThat(pmml4Result.getResultVariables().get(WEATHERDECISION)).isEqualTo(expectedResult);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isNotNull();
        if (expectedResult.equals("umbrella")) {
            Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(1.0);
        } else {
            Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(0.0);
        }
    }
}
