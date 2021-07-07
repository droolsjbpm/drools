package org.kie.pmml.clustering.tests;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kie.api.pmml.PMML4Result;
import org.kie.pmml.api.runtime.PMMLRuntime;
import org.kie.pmml.models.tests.AbstractPMMLTest;

public abstract class AbstractSingleIrisKMeansClusteringTest extends AbstractPMMLTest {

    private static final String MODEL_NAME = "SingleIrisKMeansClustering";
    private static final String TARGET_FIELD = "class";
    private static final String OUT_NORMCONTINUOUS_FIELD = "out_normcontinuous_field";
    private static final String OUT_NORMDISCRETE_FIELD = "out_normdiscrete_field";

    protected static PMMLRuntime pmmlRuntime;

    private final double sepalLength;
    private final double sepalWidth;
    private final double petalLength;
    private final double petalWidth;
    private final String irisClass;
    private final double outNormcontinuousField;

    public AbstractSingleIrisKMeansClusteringTest(double sepalLength, double sepalWidth, double petalLength, double petalWidth, String irisClass, double outNormcontinuousField) {
        this.sepalLength = sepalLength;
        this.sepalWidth = sepalWidth;
        this.petalLength = petalLength;
        this.petalWidth = petalWidth;
        this.irisClass = irisClass;
        this.outNormcontinuousField = outNormcontinuousField;
    }

    @Test
    public void testLogisticRegressionIrisData() {
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("sepal_length", sepalLength);
        inputData.put("sepal_width", sepalWidth);
        inputData.put("petal_length", petalLength);
        inputData.put("petal_width", petalWidth);

        PMML4Result pmml4Result = evaluate(pmmlRuntime, inputData, MODEL_NAME);

        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(TARGET_FIELD)).isEqualTo(irisClass);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMCONTINUOUS_FIELD)).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMCONTINUOUS_FIELD)).isEqualTo(outNormcontinuousField);
        Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isNotNull();
        if (irisClass.equals("1")) {
            Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(1.0);
        } else {
            Assertions.assertThat(pmml4Result.getResultVariables().get(OUT_NORMDISCRETE_FIELD)).isEqualTo(0.0);
        }
    }
}
