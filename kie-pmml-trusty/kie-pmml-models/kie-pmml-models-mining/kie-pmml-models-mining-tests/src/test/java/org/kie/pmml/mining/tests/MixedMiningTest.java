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

package org.kie.pmml.mining.tests;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.pmml.PMML4Result;
import org.kie.pmml.api.runtime.PMMLRuntime;
import org.kie.pmml.models.tests.AbstractPMMLTest;

@RunWith(Parameterized.class)
public class MixedMiningTest extends AbstractPMMLTest {

    private static final String FILE_NAME = "MiningModel_Mixed.pmml";
    private static final String MODEL_NAME = "MixedMining";
    private static final String TARGET_FIELD = "categoricalResult";
    private static final String NUMBER_OF_CLAIMS = "Number of Claims";
    private static final String OUT_DER_FUN_OCCUPATION = "out_der_fun_occupation";
    private static final String OUT_RESIDENCESTATE = "out_residenceState";
    private static final String OUT_FUN_OCCUPATION_REFERRED = "out_fun_occupation_referred";
    private static final String CONSTANT_OCCUPATION = "CONSTANT_OCCUPATION";
    private static final String OUT_NORMDISCRETE_FIELD = "out_normdiscrete_field";

    private static PMMLRuntime pmmlRuntime;

    private String categoricalX;
    private String categoricalY;
    private double age;
    private String occupation;
    private String residenceState;
    private boolean validLicense;
    private double expectedResult;

    public MixedMiningTest(String categoricalX,
                           String categoricalY,
                           double age,
                           String occupation,
                           String residenceState,
                           boolean validLicense,
                           double expectedResult) {
        this.categoricalX = categoricalX;
        this.categoricalY = categoricalY;
        this.age = age;
        this.occupation = occupation;
        this.residenceState = residenceState;
        this.validLicense = validLicense;
        this.expectedResult = expectedResult;
    }

  @BeforeClass
    public static void setupClass() {
        pmmlRuntime = getPMMLRuntime(FILE_NAME);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"red", "classA", 25.0, "ASTRONAUT", "AP", true, 17.0},
                {"blue", "classA", 2.3, "PROGRAMMER", "KN", true, 36.0},
                {"yellow", "classC", 333.56, "INSTRUCTOR", "TN", false, -58.0},
                {"orange", "classB", 0.12, "ASTRONAUT", "KN", true, 33.0},
                {"green", "classC", 122.12, "TEACHER", "TN", false, 123.0},
                {"green", "classB", 11.33, "INSTRUCTOR", "AP", false, 76.0},
                {"orange", "classB", 423.2, "SKYDIVER", "KN", true, 57.0},
        });
    }

    @Test
    public void testMixedMining() {
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("categoricalX", categoricalX);
        inputData.put("categoricalY", categoricalY);
        inputData.put("age", age);
        inputData.put("occupation", occupation);
        inputData.put("residenceState", residenceState);
        inputData.put("validLicense", validLicense);
        PMML4Result pmml4Result = evaluate(pmmlRuntime, inputData, MODEL_NAME);

        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isEqualTo(expectedResult);
        Assertions.assertThat(pmml4Result.getResultVariables().get(NUMBER_OF_CLAIMS)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(NUMBER_OF_CLAIMS)).isEqualTo(expectedResult);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_OCCUPATION)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_OCCUPATION)).isEqualTo(occupation);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_RESIDENCESTATE)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_RESIDENCESTATE)).isEqualTo(residenceState);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_FUN_OCCUPATION_REFERRED)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_FUN_OCCUPATION_REFERRED)).isEqualTo(CONSTANT_OCCUPATION);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isNotNull();
        if (occupation.equals("SKYDIVER")) {
            Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(1.0);
        } else {
            Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(0.0);
        }
    }
}
