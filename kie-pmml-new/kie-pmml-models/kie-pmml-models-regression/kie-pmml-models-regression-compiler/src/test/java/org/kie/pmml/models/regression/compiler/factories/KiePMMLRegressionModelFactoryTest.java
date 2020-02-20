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

package org.kie.pmml.models.regression.compiler.factories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.dmg.pmml.regression.NumericPredictor;
import org.dmg.pmml.regression.PredictorTerm;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.pmml.models.regression.model.KiePMMLRegressionModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getCategoricalPredictor;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getDataDictionary;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getDataField;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getMiningField;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getMiningSchema;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getNumericPredictor;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getPredictorTerm;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getRegressionModel;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getRegressionTable;
import static org.kie.pmml.models.regression.compiler.factories.KiePMMLRegressionModelFactory.getKiePMMLRegressionModel;

@RunWith(Parameterized.class)
public class KiePMMLRegressionModelFactoryTest {

    private List<RegressionTable> regressionTables;
    private List<DataField> dataFields;
    private List<MiningField> miningFields;
    private MiningField targetMiningField;
    private DataDictionary dataDictionary;
    private MiningSchema miningSchema;
    private RegressionModel regressionModel;

    public KiePMMLRegressionModelFactoryTest(String modelName, double tableIntercept, Object tableTargetCategory) {
        Random random = new Random();
        Set<String> fieldNames = new HashSet<>();
        regressionTables = IntStream.range(0, 3).mapToObj(i -> {
                                                              List<CategoricalPredictor> categoricalPredictors = new ArrayList<>();
                                                              List<NumericPredictor> numericPredictors = new ArrayList<>();
                                                              List<PredictorTerm> predictorTerms = new ArrayList<>();
                                                              IntStream.range(0, 3).forEach(j -> {
                                                                  String catFieldName = "CatPred-" + j;
                                                                  String numFieldName = "NumPred-" + j;
                                                                  categoricalPredictors.add(getCategoricalPredictor(catFieldName, random.nextDouble(), random.nextDouble()));
                                                                  numericPredictors.add(getNumericPredictor(numFieldName, random.nextInt(), random.nextDouble()));
                                                                  predictorTerms.add(getPredictorTerm("PredTerm-" + j, random.nextDouble(), Arrays.asList(catFieldName, numFieldName)));
                                                                  fieldNames.add(catFieldName);
                                                                  fieldNames.add(numFieldName);
                                                              });
                                                              return getRegressionTable(categoricalPredictors, numericPredictors, predictorTerms, tableIntercept + random.nextDouble(), tableTargetCategory + "-" + i);
                                                          }
        ).collect(Collectors.toList());
        dataFields = new ArrayList<>();
        miningFields = new ArrayList<>();
        fieldNames.forEach(fieldName -> {
            dataFields.add(getDataField(fieldName, OpType.CATEGORICAL));
            miningFields.add(getMiningField(fieldName, MiningField.UsageType.ACTIVE));
        });
        targetMiningField = miningFields.get(0);
        targetMiningField.setUsageType(MiningField.UsageType.TARGET);
        dataDictionary = getDataDictionary(dataFields);
        miningSchema = getMiningSchema(miningFields);
        regressionModel = getRegressionModel(modelName, MiningFunction.REGRESSION, miningSchema, regressionTables);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"firstModel", 3.5, "professional"},
                {"secondModel", 27.4, "clerical"}
        });
    }

    @Test
    public void getKiePMMLRegressionModelTest() {
        KiePMMLRegressionModel retrieved = getKiePMMLRegressionModel(dataDictionary, regressionModel);
        assertNotNull(retrieved);
        assertEquals(regressionModel.getModelName(), retrieved.getName());
        assertEquals(regressionModel.getMiningFunction().value(), retrieved.getMiningFunction().getName());
        assertEquals(regressionModel.getNormalizationMethod().value(), retrieved.getRegressionNormalizationMethod().getName());
        assertEquals(targetMiningField.getName().getValue(), retrieved.getTargetField());
    }
}