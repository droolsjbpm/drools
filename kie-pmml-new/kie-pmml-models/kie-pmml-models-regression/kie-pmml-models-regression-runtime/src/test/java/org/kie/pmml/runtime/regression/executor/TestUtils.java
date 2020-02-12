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
package org.kie.pmml.runtime.regression.executor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kie.api.pmml.PMMLRequestData;
import org.kie.pmml.commons.model.enums.MINING_FUNCTION;
import org.kie.pmml.commons.model.enums.OP_TYPE;
import org.kie.pmml.models.regression.api.model.KiePMMLRegressionModel;
import org.kie.pmml.models.regression.api.model.KiePMMLRegressionTable;
import org.kie.pmml.models.regression.api.model.enums.MODEL_TYPE;
import org.kie.pmml.models.regression.api.model.enums.REGRESSION_NORMALIZATION_METHOD;
import org.kie.pmml.models.regression.api.model.predictors.KiePMMLCategoricalPredictor;
import org.kie.pmml.models.regression.api.model.predictors.KiePMMLNumericPredictor;
import org.kie.pmml.runtime.core.utils.PMMLRequestDataBuilder;

/**
 * @see <a href=http://dmg.org/pmml/v4-2-1/Regression.html>Regression</a>
 */
public class TestUtils {

    public static final String MODEL_NAME = "Sample for linear regression";
    public static final String ALGORITHM_NAME = "linearRegression";
    public static final MINING_FUNCTION _MINING_FUNCTION = MINING_FUNCTION.REGRESSION;
    public static final MODEL_TYPE _MODEL_TYPE = MODEL_TYPE.LINEAR_REGRESSION;
    public static final REGRESSION_NORMALIZATION_METHOD _REGRESSION_NORMALIZATION_METHOD = REGRESSION_NORMALIZATION_METHOD.SIMPLEMAX;
    public static final List<KiePMMLRegressionTable> REGRESSION_TABLES = new ArrayList<>();
    public static final boolean SCORABLE = true;
    public static final String TARGETFIELD_NAME = "number_of_claims";
    public static final OP_TYPE _OP_TYPE = OP_TYPE.CONTINUOUS;
    public static final String CARPARK = "carpark";
    public static final String STREET = "street";
    public static final double INTERCEPT = 132.37;
    public static final double CARPARK_COEFF = 41.1;
    public static final double STREET_COEFF = 325.03;
    public static final double AGE_COEFF = 7.1;
    public static final double SALARY_COEFF = 0.01;

    public static KiePMMLRegressionModel getKiePMMLRegressionModel() {
        return KiePMMLRegressionModel.builder(MODEL_NAME, _MINING_FUNCTION)
                .withAlgorithmName(ALGORITHM_NAME)
                .withModelType(_MODEL_TYPE)
                .withRegressionNormalizationMethod(_REGRESSION_NORMALIZATION_METHOD)
                .withRegressionTables(REGRESSION_TABLES)
                .withScorable(SCORABLE)
                .withTargetOpType(_OP_TYPE)
                .withTargetField(TARGETFIELD_NAME)
                .build();
    }

    public static KiePMMLRegressionTable getKiePMMLRegressionTable() {
        return KiePMMLRegressionTable.builder()
                .withIntercept(INTERCEPT)
                .withCategoricalPredictors(getKiePMMLCategoricalPredictor())
                .withNumericPredictors(getKiePMMLNumericPredictors())
                .build();
    }

    public static List<KiePMMLCategoricalPredictor> getKiePMMLCategoricalPredictor() {
        List<KiePMMLCategoricalPredictor> toReturn = new ArrayList<>();
        toReturn.add(new KiePMMLCategoricalPredictor("car_location", CARPARK, CARPARK_COEFF));
        toReturn.add(new KiePMMLCategoricalPredictor("car_location", STREET, STREET_COEFF));
        return toReturn;
    }

    public static Set<KiePMMLNumericPredictor> getKiePMMLNumericPredictors() {
        Set<KiePMMLNumericPredictor> toReturn = new HashSet<>();
        toReturn.add(new KiePMMLNumericPredictor("age", 1, AGE_COEFF));
        toReturn.add(new KiePMMLNumericPredictor("salary", 1, SALARY_COEFF));
        return toReturn;
    }

    public static PMMLRequestData getPMMLRequestData(String modelName, Map<String, Object> parameters) {
        String correlationId = "CORRELATION_ID";
        PMMLRequestDataBuilder pmmlRequestDataBuilder = new PMMLRequestDataBuilder(correlationId, modelName);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object pValue = entry.getValue();
            Class class1 = pValue.getClass();
            pmmlRequestDataBuilder.addParameter(entry.getKey(), pValue, class1);
        }
        return pmmlRequestDataBuilder.build();
    }
}