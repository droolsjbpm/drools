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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class NormalizationMethodsRegressionTest extends AbstractPMMLRegressionTest {

    private static final String MODEL_NAME = "lmWithRegularization_Model";
    private static final String PMML_SOURCE = "/regressionNormalization.pmml";
    private static final String TARGET_FIELD = "result";

    private static final String SOFTMAX_NORMALIZATION_METHOD = "softmax";
    private static final String LOGIT_NORMALIZATION_METHOD = "logit";
    private static final String EXP_NORMALIZATION_METHOD = "exp";

    private static final String NORMALIZATION_METHOD_PLACEHOLDER = "NORMALIZATION_METHOD_PLACEHOLDER";

    private double x;
    private double y;
    private String normalizationMethod;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0, 0, "softmax"}, {-1, 2, "softmax"}, {0.5, -2.5, "softmax"}, {3, 1, "softmax"}, {25, 50, "softmax"},
                {-100, 250, "softmax"}, {-100.1, 800, "softmax"}, {-8, 12.5, "softmax"}, {-1001.1, -500.2, "softmax"}, {-1701, 508, "softmax"},
                {0, 0, "logit"}, {-1, 2, "logit"}, {0.5, -2.5, "logit"}, {3, 1, "logit"}, {25, 50, "logit"},
                {-100, 250, "logit"}, {-100.1, 800, "logit"}, {-8, 12.5, "logit"}, {-1001.1, -500.2, "logit"}, {-1701, 508, "logit"},
                {0, 0, "exp"}, {-1, 2, "exp"}, {0.5, -2.5, "exp"}, {3, 1, "exp"}, {25, 50, "exp"},
                {-100, 250, "exp"}, {-100.1, 800, "exp"}, {-8, 12.5, "exp"}, {-1001.1, -500.2, "exp"}, {-1701, 508, "exp"},
        });
    }

    public NormalizationMethodsRegressionTest(double x, double y, String normalizationMethod) {
        this.x = x;
        this.y = y;
        this.normalizationMethod = normalizationMethod;
    }

    private static double normalizedRegressionFunction(double x, double y, String normalizationMethod) {
        final double regressionValue = 2 * x + y + 5;

        if (normalizationMethod.equals(SOFTMAX_NORMALIZATION_METHOD) || normalizationMethod.equals(LOGIT_NORMALIZATION_METHOD)) {
            return 1 / (1 + Math.exp(-regressionValue));
        } else if (normalizationMethod.equals(EXP_NORMALIZATION_METHOD)) {
            return Math.exp(regressionValue);
        } else {
            throw new RuntimeException("Unknown normalization method");
        }
    }

    @Test
    public void testNormalizationMethodsRegression() throws Exception {
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
        inputData.put("x", x);
        inputData.put("y", y);

        final PMMLRequestData pmmlRequestData = getPMMLRequestData(MODEL_NAME, inputData);
        PMML4Result pmml4Result = EXECUTOR.evaluate(pmmlModel, new PMMLContextImpl(pmmlRequestData), RELEASE_ID);

        Assertions.assertThat(pmml4Result).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables()).containsKey(TARGET_FIELD);
        Assertions.assertThat((Double) pmml4Result.getResultVariables().get(TARGET_FIELD))
                .isEqualTo(normalizedRegressionFunction(x, y, normalizationMethod));
    }
}
