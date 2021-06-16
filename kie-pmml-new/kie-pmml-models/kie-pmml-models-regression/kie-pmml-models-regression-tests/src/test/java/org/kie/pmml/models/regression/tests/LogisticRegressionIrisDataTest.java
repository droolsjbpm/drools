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
import org.assertj.core.data.Percentage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.evaluator.core.PMMLContextImpl;

@RunWith(Parameterized.class)
public class LogisticRegressionIrisDataTest extends AbstractPMMLRegressionTest {

    private static final String MODEL_NAME = "LogisticRegressionIrisData";
    private static final String PMML_SOURCE = "logisticRegressionIrisData.pmml";
    private static final String TARGET_FIELD = "Species";
    private static final String PROBABILITY_SETOSA_FIELD = "Probability_setosa";
    private static final String PROBABILITY_VERSICOLOR_FIELD = "Probability_versicolor";
    private static final String PROBABILITY_VIRGINICA_FIELD = "Probability_virginica";

    private static final Percentage TOLERANCE_PERCENTAGE = Percentage.withPercentage(0.001);

    private double sepalLength;
    private double sepalWidth;
    private double petalLength;
    private double petalWidth;
    private String expectedResult;

    public LogisticRegressionIrisDataTest(double sepalLength, double sepalWidth, double petalLength,
                                          double petalWidth, String expectedResult) {
        this.sepalLength = sepalLength;
        this.sepalWidth = sepalWidth;
        this.petalLength = petalLength;
        this.petalWidth = petalWidth;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {6.9, 3.1, 5.1, 2.3, "virginica"},
                {5.8, 2.6, 4.0, 1.2, "versicolor"},
                {5.7, 3.0, 4.2, 1.2, "versicolor"},
                {5.0, 3.3, 1.4, 0.2, "setosa"},
                {5.4, 3.9, 1.3, 0.4, "setosa"}
        });
    }

    @Test
    public void testLogisticRegressionIrisData() {
        final KiePMMLModel pmmlModel = loadPMMLModel(PMML_SOURCE);

        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("Sepal.Length", sepalLength);
        inputData.put("Sepal.Width", sepalWidth);
        inputData.put("Petal.Length", petalLength);
        inputData.put("Petal.Width", petalWidth);

        final PMMLRequestData pmmlRequestData = getPMMLRequestData(MODEL_NAME, inputData);
        PMML4Result pmml4Result = EXECUTOR.evaluate(pmmlModel, new PMMLContextImpl(pmmlRequestData), RELEASE_ID);
        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isEqualTo(expectedResult);

        Assertions.assertThat((double) pmml4Result.getResultVariables().get(PROBABILITY_SETOSA_FIELD))
                .isCloseTo(setosaProbability(), TOLERANCE_PERCENTAGE);
        Assertions.assertThat((double) pmml4Result.getResultVariables().get(PROBABILITY_VERSICOLOR_FIELD))
                .isCloseTo(versicolorProbability(), TOLERANCE_PERCENTAGE);
        Assertions.assertThat((double) pmml4Result.getResultVariables().get(PROBABILITY_VIRGINICA_FIELD))
                .isCloseTo(virginicaProbability(), TOLERANCE_PERCENTAGE);
    }

    private double setosaProbability() {
        return 0.0660297693761902 * sepalLength + 0.242847872054487 * sepalWidth
                + -0.224657116235727 * petalLength + -0.0574727291860025 * petalWidth + 0.11822288946815;
    }

    private double versicolorProbability() {
        return -0.0201536848255179 * sepalLength + -0.44561625761404 * sepalWidth
                + 0.22066920522933 * petalLength + -0.494306595747785 * petalWidth + 1.57705897385745;
    }

    private double virginicaProbability() {
        return -0.0458760845506725 * sepalLength + 0.202768385559553 * sepalWidth
                + 0.00398791100639665 * petalLength + 0.551779324933787 * petalWidth - 0.695281863325603;
    }
}
