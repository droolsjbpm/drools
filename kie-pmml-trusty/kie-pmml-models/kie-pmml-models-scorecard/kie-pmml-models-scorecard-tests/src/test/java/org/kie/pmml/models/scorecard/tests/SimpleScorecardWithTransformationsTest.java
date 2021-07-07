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

package org.kie.pmml.models.scorecard.tests;

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
public class SimpleScorecardWithTransformationsTest extends AbstractPMMLTest {

    private static final String FILE_NAME = "SimpleScorecardWithTransformations.pmml";
    private static final String MODEL_NAME = "SimpleScorecardWithTransformations";
    private static final String TARGET_FIELD = "Score";
    private static final String REASON_CODE1_FIELD = "Reason Code 1";
    private static final String REASON_CODE2_FIELD = "Reason Code 2";
    private static final String OUT_DER_INPUT1 = "out_der_input1";
    private static final String OUT_DER_INPUT2 = "out_der_input2";
    private static final String OUT_DER_CONSTANT = "out_der_constant";
    private static final String CONSTANT = "constant";
    private static final String OUT_NORMDISCRETE_FIELD = "out_normdiscrete_field";

    private static PMMLRuntime pmmlRuntime;

    private double input1;
    private double input2;
    private double score;
    private String reasonCode1;
    private String reasonCode2;

    public SimpleScorecardWithTransformationsTest(double input1, double input2, double score, String reasonCode1, String reasonCode2) {
        this.input1 = input1;
        this.input2 = input2;
        this.score = score;
        this.reasonCode1 = reasonCode1;
        this.reasonCode2 = reasonCode2;
    }

    @BeforeClass
    public static void setupClass() {
        pmmlRuntime = getPMMLRuntime(FILE_NAME);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {5, 5, 25, "Input1ReasonCode", null},
                {5, -10, -15, "Input1ReasonCode", "Input2ReasonCode"},
                {20.5, 4, 87, null, null},
                {23.5, -12, 47, "Input2ReasonCode", null},
                {10, -5, -15, "Input1ReasonCode", "Input2ReasonCode"},
        });
    }

    @Test
    public void testSimpleScorecard() {
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("input1", input1);
        inputData.put("input2", input2);
        PMML4Result pmml4Result = evaluate(pmmlRuntime, inputData, MODEL_NAME);

        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isEqualTo(score);

        Assertions.assertThat(pmml4Result.getResultVariables().get(REASON_CODE1_FIELD)).isEqualTo(reasonCode1);
        Assertions.assertThat(pmml4Result.getResultVariables().get(REASON_CODE2_FIELD)).isEqualTo(reasonCode2);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_INPUT1)).isEqualTo(input1);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_INPUT2)).isEqualTo(input2);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_CONSTANT)).isEqualTo(CONSTANT);
        if (reasonCode1 != null) {
            Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isNotNull();
            if (reasonCode1.equals("Input1ReasonCode")) {
                Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(1.0);
            } else {
                Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(0.0);
            }
        }
    }
}
