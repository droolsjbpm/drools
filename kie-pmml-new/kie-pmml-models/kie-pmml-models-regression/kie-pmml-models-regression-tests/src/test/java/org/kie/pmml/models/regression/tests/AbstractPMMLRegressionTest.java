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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.dmg.pmml.PMML;
import org.dmg.pmml.regression.RegressionModel;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.compiler.testutils.TestUtils;
import org.kie.pmml.evaluator.core.executor.PMMLModelExecutor;
import org.kie.pmml.evaluator.core.utils.PMMLRequestDataBuilder;
import org.kie.pmml.models.regression.compiler.executor.RegressionModelImplementationProvider;
import org.kie.pmml.models.regression.evaluator.PMMLRegressionModelExecutor;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractPMMLRegressionTest {

    protected static final RegressionModelImplementationProvider PROVIDER = new RegressionModelImplementationProvider();
    protected static final PMMLModelExecutor EXECUTOR = new PMMLRegressionModelExecutor();
    protected static final String RELEASE_ID = "org.drools:kie-pmml-models-testing:1.0";

    protected static PMMLRequestData getPMMLRequestData(String modelName, Map<String, Object> parameters) {
        String correlationId = "CORRELATION_ID";
        PMMLRequestDataBuilder pmmlRequestDataBuilder = new PMMLRequestDataBuilder(correlationId, modelName);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object pValue = entry.getValue();
            Class class1 = pValue.getClass();
            pmmlRequestDataBuilder.addParameter(entry.getKey(), pValue, class1);
        }
        return pmmlRequestDataBuilder.build();
    }

    protected KiePMMLModel loadPMMLModel(final String resourcePath) {
        final PMML pmml;

        try {
            pmml = TestUtils.loadFromFile(resourcePath);
        } catch (Exception e) {
            throw new RuntimeException("Error loading PMML", e);
        }

        Assertions.assertThat(pmml).isNotNull();
        assertEquals(1, pmml.getModels().size());
        assertTrue(pmml.getModels().get(0) instanceof RegressionModel);

        final KiePMMLModel pmmlModel = PROVIDER.getKiePMMLModel(pmml.getDataDictionary(),
                                                                (RegressionModel) pmml.getModels().get(0), RELEASE_ID);
        Assertions.assertThat(pmmlModel).isNotNull();

        return pmmlModel;
    }
}
