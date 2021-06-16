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

package org.kie.pmml.models.regression.tests;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.evaluator.core.PMMLContextImpl;

@RunWith(Parameterized.class)
public class PredictorTermRegressionTest extends AbstractPMMLRegressionTest {

    private static final String MODEL_NAME = "predictorTerm_Model";
    private static final String PMML_SOURCE = "predictorTermRegression.pmml";
    private static final String TARGET_FIELD = "result";

    private double x;
    private double y;
    private double z;

    public PredictorTermRegressionTest(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0, 0, 0}, {-1, 2, 3}, {0.5, -2.5, 4}, {3, 1, 2}, {25, 50, 15},
                {-100, 250, -10}, {-100.1, 800, 105}, {-8, 12.5, 230}, {-1001, -500, 8}, {-1701, 508, 9}
        });
    }

    private static double regressionFunction(double x, double y, double z) {
        return 2 * x + y + 5 * z * z + 4 * y * z - 2.5 * x * y * z + 5;
    }

    @Test
    public void testPredictorTermRegression() {
        final KiePMMLModel pmmlModel = loadPMMLModel(PMML_SOURCE);

        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("x", x);
        inputData.put("y", y);
        inputData.put("z", z);

        final PMMLRequestData pmmlRequestData = getPMMLRequestData(MODEL_NAME, inputData);
        PMML4Result pmml4Result = EXECUTOR.evaluate(pmmlModel, new PMMLContextImpl(pmmlRequestData), RELEASE_ID);

        Assertions.assertThat(pmml4Result).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables()).containsKey(TARGET_FIELD);
        Assertions.assertThat((Double) pmml4Result.getResultVariables().get(TARGET_FIELD))
                .isEqualTo(regressionFunction(x, y, z));
    }
}
