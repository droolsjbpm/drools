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

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.dmg.pmml.PMML;
import org.dmg.pmml.regression.RegressionModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.compiler.testutils.TestUtils;
import org.kie.pmml.evaluator.core.PMMLContextImpl;

import java.nio.charset.Charset;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class LogisticRegressionWithNormalizationTest extends AbstractPMMLRegressionTest {

    private static final String MODEL_NAME = "LogisticRegressionWithNormalization";
    private static final String PMML_SOURCE = "/logisticRegressionWithNormalization.pmml";
    private static final String TARGET_FIELD = "Species";
    private static final String PROBABILITY_SETOSA_FIELD = "Probability_setosa";
    private static final String PROBABILITY_VERSICOLOR_FIELD = "Probability_versicolor";
    private static final String PROBABILITY_VIRGINICA_FIELD = "Probability_virginica";

    private static final Percentage TOLERANCE_PERCENTAGE = Percentage.withPercentage(0.001);

    private static final String NONE_NORMALIZATION_METHOD = "none";
    private static final String SOFTMAX_NORMALIZATION_METHOD = "softmax";
    private static final String SIMPLEMAX_NORMALIZATION_METHOD = "simplemax";

    private static final String NORMALIZATION_METHOD_PLACEHOLDER = "NORMALIZATION_METHOD_PLACEHOLDER";

    private double sepalLength;
    private double sepalWidth;
    private double petalLength;
    private double petalWidth;
    private String expectedResult;
    private String normalizationMethod;
    private double expectedSetosaProbability;
    private double expectedVersicolorProbability;
    private double expectedVirginicaProbability;

    public LogisticRegressionWithNormalizationTest(double sepalLength, double sepalWidth, double petalLength,
                                                   double petalWidth, String expectedResult, String normalizationMethod,
                                                   double expectedSetosaProbability, double expectedVersicolorProbability,
                                                   double expectedVirginicaProbability) {
        this.sepalLength = sepalLength;
        this.sepalWidth = sepalWidth;
        this.petalLength = petalLength;
        this.petalWidth = petalWidth;
        this.expectedResult = expectedResult;
        this.normalizationMethod = normalizationMethod;
        this.expectedSetosaProbability = expectedSetosaProbability;
        this.expectedVersicolorProbability = expectedVersicolorProbability;
        this.expectedVirginicaProbability = expectedVirginicaProbability;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {6.9, 3.1, 5.1, 2.3, "virginica", NONE_NORMALIZATION_METHOD,
                        0.04871813160275851, 0.04509592640753013, 0.9061859419897114},
                {5.8, 2.6, 4.0, 1.2, "versicolor", NONE_NORMALIZATION_METHOD,
                        0.16500427922560845, 0.5910742380929204, 0.24392148268147118},
                {5.4, 3.9, 1.3, 0.4, "setosa", NONE_NORMALIZATION_METHOD,
                        1.1068470023312305, -0.18052700039608738, 0.07367999806485692},

                {6.9, 3.1, 5.1, 2.3, "virginica", SOFTMAX_NORMALIZATION_METHOD,
                        0.22969661966054, 0.228866116406123, 0.541437263933338},
                {5.8, 2.6, 4.0, 1.2, "versicolor", SOFTMAX_NORMALIZATION_METHOD,
                        0.276752056446685, 0.423770468651362, 0.299477474901954},
                {5.4, 3.9, 1.3, 0.4, "setosa", SOFTMAX_NORMALIZATION_METHOD,
                        0.612792897443624, 0.169127526544678, 0.218079576011698},

                {6.9, 3.1, 5.1, 2.3, "virginica", SIMPLEMAX_NORMALIZATION_METHOD,
                        0.0487181316027585, 0.0450959264075301, 0.906185941989711},
                {5.8, 2.6, 4.0, 1.2, "versicolor", SIMPLEMAX_NORMALIZATION_METHOD,
                        0.165004279225608, 0.59107423809292, 0.243921482681471},
                {5.4, 3.9, 1.3, 0.4, "setosa", SIMPLEMAX_NORMALIZATION_METHOD,
                        1.10684700233123, -0.180527000396087, 0.0736799980648569},
        });
    }

    @Test
    public void testLogisticRegressionWithNormalization() throws Exception {
        String pmmlXML = IOUtils.resourceToString(PMML_SOURCE, Charset.defaultCharset());
        pmmlXML = pmmlXML.replace(NORMALIZATION_METHOD_PLACEHOLDER, normalizationMethod);
        final PMML pmml = TestUtils.loadFromSource(pmmlXML);

        Assertions.assertThat(pmml).isNotNull();
        assertEquals(1, pmml.getModels().size());
        assertTrue(pmml.getModels().get(0) instanceof RegressionModel);

        final KiePMMLModel pmmlModel = PROVIDER.getKiePMMLModel(pmml.getDataDictionary(),
                (RegressionModel) pmml.getModels().get(0), RELEASE_ID);
        Assertions.assertThat(pmmlModel).isNotNull();

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
                .isCloseTo(expectedSetosaProbability, TOLERANCE_PERCENTAGE);
        Assertions.assertThat((double) pmml4Result.getResultVariables().get(PROBABILITY_VERSICOLOR_FIELD))
                .isCloseTo(expectedVersicolorProbability, TOLERANCE_PERCENTAGE);
        Assertions.assertThat((double) pmml4Result.getResultVariables().get(PROBABILITY_VIRGINICA_FIELD))
                .isCloseTo(expectedVirginicaProbability, TOLERANCE_PERCENTAGE);
    }
}
