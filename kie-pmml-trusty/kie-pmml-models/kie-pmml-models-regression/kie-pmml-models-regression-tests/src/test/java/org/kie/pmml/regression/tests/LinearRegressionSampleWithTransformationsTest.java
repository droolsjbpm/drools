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

package org.kie.pmml.regression.tests;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.pmml.PMML4Result;
import org.kie.pmml.api.runtime.PMMLRuntime;
import org.kie.pmml.models.tests.AbstractPMMLTest;

@RunWith(Parameterized.class)
public class LinearRegressionSampleWithTransformationsTest extends AbstractPMMLTest {

    private static final String FILE_NAME = "LinearRegressionSampleWithTransformations.pmml";
    private static final String MODEL_NAME = "LinearRegressionSampleWithTransformations";
    private static final String TARGET_FIELD = "number_of_claims";

    private static final String OUT_NUMBER_OF_CLAIMS = "Number of Claims";
    private static final String OUT_DER_FUN_CAR_LOCATION_REFERRED = "out_der_fun_car_location_referred";
    private static final String OUT_SALARY = "out_salary";
    private static final String OUT_DER_AGE = "out_der_age";
    private static final String OUT_DER_SALARY = "out_der_salary";
    private static final String OUT_DER_CAR_LOCATION = "out_der_car_location";
    private static final String OUT_DER_CAR_LOCATION_REFERRAL = "out_der_car_location_referral";
    private static final String OUT_DER_CONSTANT = "out_der_constant";
    private static final String OUT_DER_FUN_SALARY_FIELDREF = "out_der_fun_salary_fieldref";
    private static final String OUT_DER_FUN_SALARY_CONSTANT = "out_der_fun_salary_constant";
    private static final String OUT_DER_FUN_SALARY_APPLY = "out_der_fun_salary_apply";
    private static final String OUT_DER_FUN_SALARY_APPLY_FUN_SALARY_FIELDREF =
            "out_der_fun_salary_apply_fun_salary_fieldref";
    private static final String OUT_NORMDISCRETE_FIELD = "out_normdiscrete_field";
    private static final String CONSTANT = "constant";
    private static final String FUN_SALARY_CONSTANT = "FUN_SALARY_CONSTANT";
    private static final String STRING_CONSTANT = "987654321";

    private static final Percentage TOLERANCE_PERCENTAGE = Percentage.withPercentage(0.001);
    private static PMMLRuntime pmmlRuntime;

    private double age;
    private double salary;
    private String car_location;
    private double expectedResult;

    public LinearRegressionSampleWithTransformationsTest(double age,
                                                         double salary,
                                                         String car_location,
                                                         double expectedResult) {
        this.age = age;
        this.salary = salary;
        this.car_location = car_location;
        this.expectedResult = expectedResult;
    }

    @BeforeClass
    public static void setupClass() {
        pmmlRuntime = getPMMLRuntime(FILE_NAME);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {27, 34000, "street", 3116.0},
                {49, 78000, "carpark", 4096.0},
                {57, 72000, "street", 4978.0},
                {61, 123000, "carpark", 5777.0},
                {18, 26000, "street", 2664.0},
        });
    }

    @Test
    public void testLogisticRegressionIrisData() {
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("age", age);
        inputData.put("salary", salary);
        inputData.put("car_location", car_location);
        PMML4Result pmml4Result = evaluate(pmmlRuntime, inputData, MODEL_NAME);

        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isNotNull();
        Assertions.assertThat((double) pmml4Result.getResultVariables().get(TARGET_FIELD)).isCloseTo(expectedResult,
                                                                                                     TOLERANCE_PERCENTAGE);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NUMBER_OF_CLAIMS)).isNotNull();
        Assertions.assertThat((double) pmml4Result.getResultVariables().get(OUT_NUMBER_OF_CLAIMS)).isCloseTo(expectedResult, TOLERANCE_PERCENTAGE);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_CAR_LOCATION_REFERRED)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_CAR_LOCATION_REFERRED)).isEqualTo(car_location);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_SALARY)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_SALARY)).isEqualTo(salary);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_AGE)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_AGE)).isEqualTo(age);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_SALARY)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_SALARY)).isEqualTo(salary);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_CAR_LOCATION)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_CAR_LOCATION)).isEqualTo(car_location);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_CAR_LOCATION_REFERRAL)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_CAR_LOCATION_REFERRAL)).isEqualTo(car_location);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_CONSTANT)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_CONSTANT)).isEqualTo(CONSTANT);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_SALARY_FIELDREF)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_SALARY_FIELDREF)).isEqualTo(STRING_CONSTANT);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_SALARY_CONSTANT)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_SALARY_CONSTANT)).isEqualTo(FUN_SALARY_CONSTANT);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_SALARY_APPLY)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_SALARY_APPLY)).isEqualTo(FUN_SALARY_CONSTANT);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_SALARY_APPLY_FUN_SALARY_FIELDREF)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_DER_FUN_SALARY_APPLY_FUN_SALARY_FIELDREF)).isEqualTo(STRING_CONSTANT);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isNotNull();
        if (car_location.equals("carpark")) {
            Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(1.0);
        } else {
            Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(0.0);
        }
    }
}
