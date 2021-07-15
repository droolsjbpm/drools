/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.kie.pmml.commons.model.expressions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.kie.pmml.api.enums.OUTLIER_TREATMENT_METHOD;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;

import static org.junit.Assert.*;

public class KiePMMLNormContinuousTest {

    @Test
    public void sortLinearNorms() {
        KiePMMLLinearNorm ln0 = new KiePMMLLinearNorm("0", Collections.emptyList(), 34, 45);
        KiePMMLLinearNorm ln1 = new KiePMMLLinearNorm("1", Collections.emptyList(), 32, 5);
        KiePMMLLinearNorm ln2 = new KiePMMLLinearNorm("2", Collections.emptyList(), 33, 34);
        List<KiePMMLLinearNorm> linearNorms = Arrays.asList(ln0, ln1, ln2);
        assertEquals(ln0, linearNorms.get(0));
        assertEquals(ln1, linearNorms.get(1));
        assertEquals(ln2, linearNorms.get(2));
        KiePMMLNormContinuous.sortLinearNorms(linearNorms);
        assertEquals(ln1, linearNorms.get(0));
        assertEquals(ln2, linearNorms.get(1));
        assertEquals(ln0, linearNorms.get(2));
    }

    @Test
    public void evaluate() {
        String fieldName = "fieldName";
        Number input = 24;
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(fieldName, null, null);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(),
                                                        Collections.emptyList(),
                                                        Collections.emptyList(),
                                                        Collections.singletonList(new KiePMMLNameValue(fieldName, input)));
        Number retrieved = (Number) kiePMMLNormContinuous.evaluate(processingDTO);
        Number expected =
                kiePMMLNormContinuous.linearNorms.get(0).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(1).getOrig() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(1).getNorm() - kiePMMLNormContinuous.linearNorms.get(0).getNorm());
        assertEquals(expected, retrieved);
    }

    @Test
    public void evaluateWithExpectedValue() {
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(null, null, null);
        Number input = 24;
        Number retrieved = kiePMMLNormContinuous.evaluate(input);
        Number expected =
                kiePMMLNormContinuous.linearNorms.get(0).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(1).getOrig() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(1).getNorm() - kiePMMLNormContinuous.linearNorms.get(0).getNorm());
        assertEquals(expected, retrieved);
        input = 28;
        expected =
                kiePMMLNormContinuous.linearNorms.get(0).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(1).getOrig() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(1).getNorm() - kiePMMLNormContinuous.linearNorms.get(0).getNorm());
        retrieved = kiePMMLNormContinuous.evaluate(input);
        assertEquals(expected, retrieved);
        input = 30;
        retrieved = kiePMMLNormContinuous.evaluate(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(1).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(1).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(2).getOrig() - kiePMMLNormContinuous.linearNorms.get(1).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(2).getNorm() - kiePMMLNormContinuous.linearNorms.get(1).getNorm());
        assertEquals(expected, retrieved);
        input = 31;
        retrieved = kiePMMLNormContinuous.evaluate(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(1).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(1).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(2).getOrig() - kiePMMLNormContinuous.linearNorms.get(1).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(2).getNorm() - kiePMMLNormContinuous.linearNorms.get(1).getNorm());
        assertEquals(expected, retrieved);
        input = 36;
        retrieved = kiePMMLNormContinuous.evaluate(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(2).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(3).getOrig() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(3).getNorm() - kiePMMLNormContinuous.linearNorms.get(2).getNorm());
        assertEquals(expected, retrieved);
        input = 37;
        retrieved = kiePMMLNormContinuous.evaluate(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(2).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(3).getOrig() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(3).getNorm() - kiePMMLNormContinuous.linearNorms.get(2).getNorm());
        assertEquals(expected, retrieved);
        input = 40;
        retrieved = kiePMMLNormContinuous.evaluate(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(2).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(3).getOrig() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(3).getNorm() - kiePMMLNormContinuous.linearNorms.get(2).getNorm());
        assertEquals(expected, retrieved);
    }

    @Test
    public void evaluateWithOutlierValueAsIs() {
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(null, OUTLIER_TREATMENT_METHOD.AS_IS, null);
        Number input = 23;
        Number retrieved = kiePMMLNormContinuous.evaluate(input);
        Number expected =
                kiePMMLNormContinuous.linearNorms.get(0).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(1).getOrig() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(1).getNorm() - kiePMMLNormContinuous.linearNorms.get(0).getNorm());

        assertEquals(expected, retrieved);
        input = 41;
        retrieved = kiePMMLNormContinuous.evaluate(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(2).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(3).getOrig() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(3).getNorm() - kiePMMLNormContinuous.linearNorms.get(2).getNorm());
        assertEquals(expected, retrieved);
    }

    @Test
    public void evaluateWithOutlierValueAsMissingValues() {
        Number missingValue = 45;
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(null, OUTLIER_TREATMENT_METHOD.AS_MISSING_VALUES, missingValue);
        Number input = 23;
        Number retrieved = kiePMMLNormContinuous.evaluate(input);
        assertEquals(missingValue, retrieved);
        input = 41;
        retrieved = kiePMMLNormContinuous.evaluate(input);
        assertEquals(missingValue, retrieved);
    }

    @Test
    public void evaluateWithOutlierValueAsExtremeValues() {
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(null, OUTLIER_TREATMENT_METHOD.AS_EXTREME_VALUES, null);
        Number input = 23;
        Number retrieved = kiePMMLNormContinuous.evaluate(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(0).getNorm(), retrieved);
        input = 41;
        retrieved = kiePMMLNormContinuous.evaluate(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(3).getNorm(), retrieved);
    }

    @Test
    public void evaluateExpectedValue() {
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(null, null, null);
        Number input = 24;
        Number retrieved = kiePMMLNormContinuous.evaluateExpectedValue(input);
        Number expected =
                kiePMMLNormContinuous.linearNorms.get(0).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(1).getOrig() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(1).getNorm() - kiePMMLNormContinuous.linearNorms.get(0).getNorm());
        assertEquals(expected, retrieved);
        input = 28;
        expected =
                kiePMMLNormContinuous.linearNorms.get(0).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(1).getOrig() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(1).getNorm() - kiePMMLNormContinuous.linearNorms.get(0).getNorm());
        retrieved = kiePMMLNormContinuous.evaluateExpectedValue(input);
        assertEquals(expected, retrieved);
        input = 30;
        retrieved = kiePMMLNormContinuous.evaluateExpectedValue(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(1).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(1).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(2).getOrig() - kiePMMLNormContinuous.linearNorms.get(1).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(2).getNorm() - kiePMMLNormContinuous.linearNorms.get(1).getNorm());
        assertEquals(expected, retrieved);
        input = 31;
        retrieved = kiePMMLNormContinuous.evaluateExpectedValue(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(1).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(1).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(2).getOrig() - kiePMMLNormContinuous.linearNorms.get(1).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(2).getNorm() - kiePMMLNormContinuous.linearNorms.get(1).getNorm());
        assertEquals(expected, retrieved);
        input = 36;
        retrieved = kiePMMLNormContinuous.evaluateExpectedValue(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(2).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(3).getOrig() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(3).getNorm() - kiePMMLNormContinuous.linearNorms.get(2).getNorm());
        assertEquals(expected, retrieved);
        input = 37;
        retrieved = kiePMMLNormContinuous.evaluateExpectedValue(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(2).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(3).getOrig() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(3).getNorm() - kiePMMLNormContinuous.linearNorms.get(2).getNorm());
        assertEquals(expected, retrieved);
        input = 40;
        retrieved = kiePMMLNormContinuous.evaluateExpectedValue(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(2).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(3).getOrig() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(3).getNorm() - kiePMMLNormContinuous.linearNorms.get(2).getNorm());
        assertEquals(expected, retrieved);
    }

    @Test
    public void evaluateOutlierValueAsIs() {
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(null, OUTLIER_TREATMENT_METHOD.AS_IS, null);
        Number input = 23;
        Number retrieved = kiePMMLNormContinuous.evaluateOutlierValue(input);
        Number expected =
                kiePMMLNormContinuous.linearNorms.get(0).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(1).getOrig() - kiePMMLNormContinuous.linearNorms.get(0).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(1).getNorm() - kiePMMLNormContinuous.linearNorms.get(0).getNorm());

        assertEquals(expected, retrieved);
        input = 41;
        retrieved = kiePMMLNormContinuous.evaluateOutlierValue(input);
        expected =
                kiePMMLNormContinuous.linearNorms.get(2).getNorm() +
                        ((input.doubleValue() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()) / (kiePMMLNormContinuous.linearNorms.get(3).getOrig() - kiePMMLNormContinuous.linearNorms.get(2).getOrig()))
                                * (kiePMMLNormContinuous.linearNorms.get(3).getNorm() - kiePMMLNormContinuous.linearNorms.get(2).getNorm());
        assertEquals(expected, retrieved);
    }

    @Test
    public void evaluateOutlierValueAsMissingValues() {
        Number missingValue = 45;
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(null, OUTLIER_TREATMENT_METHOD.AS_MISSING_VALUES, missingValue);
        Number input = 23;
        Number retrieved = kiePMMLNormContinuous.evaluateOutlierValue(input);
        assertEquals(missingValue, retrieved);
        input = 41;
        retrieved = kiePMMLNormContinuous.evaluateOutlierValue(input);
        assertEquals(missingValue, retrieved);
    }

    @Test
    public void evaluateOutlierValueAsExtremeValues() {
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(null, OUTLIER_TREATMENT_METHOD.AS_EXTREME_VALUES, null);
        Number input = 23;
        Number retrieved = kiePMMLNormContinuous.evaluateOutlierValue(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(0).getNorm(), retrieved);
        input = 41;
        retrieved = kiePMMLNormContinuous.evaluateOutlierValue(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(3).getNorm(), retrieved);
    }

    @Test
    public void getLimitExpectedValue() {
        KiePMMLNormContinuous kiePMMLNormContinuous = getKiePMMLNormContinuous(null, null, null);
        Number input = 24;
        KiePMMLLinearNorm[] retrieved = kiePMMLNormContinuous.getLimitExpectedValue(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(0), retrieved[0]);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(1), retrieved[1]);
        input = 28;
        retrieved = kiePMMLNormContinuous.getLimitExpectedValue(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(0), retrieved[0]);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(1), retrieved[1]);
        input = 30;
        retrieved = kiePMMLNormContinuous.getLimitExpectedValue(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(1), retrieved[0]);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(2), retrieved[1]);
        input = 31;
        retrieved = kiePMMLNormContinuous.getLimitExpectedValue(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(1), retrieved[0]);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(2), retrieved[1]);
        input = 36;
        retrieved = kiePMMLNormContinuous.getLimitExpectedValue(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(2), retrieved[0]);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(3), retrieved[1]);
        input = 37;
        retrieved = kiePMMLNormContinuous.getLimitExpectedValue(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(2), retrieved[0]);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(3), retrieved[1]);
        input = 40;
        retrieved = kiePMMLNormContinuous.getLimitExpectedValue(input);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(2), retrieved[0]);
        assertEquals(kiePMMLNormContinuous.linearNorms.get(3), retrieved[1]);
    }

    @Test
    public void evaluateInputAndLimitLinearNorms() {
        double startOrig = 2.1;
        double startNorm = 2.6;
        double endOrig = 7.4;
        double endNorm = 6.9;
        KiePMMLLinearNorm startLinearNorm = new KiePMMLLinearNorm("start",
                                                                  Collections.emptyList(),
                                                                  startOrig,
                                                                  startNorm);
        KiePMMLLinearNorm endLinearNorm = new KiePMMLLinearNorm("end",
                                                                Collections.emptyList(),
                                                                endOrig,
                                                                endNorm);
        KiePMMLLinearNorm[] limitLinearNorms = {startLinearNorm, endLinearNorm};
        Number input = 3.5;
        Number retrieved = KiePMMLNormContinuous.evaluate(input, limitLinearNorms);
        assertNotNull(retrieved);
        Number expected =
                startNorm + ((input.doubleValue() - startOrig) / (endOrig - startOrig)) * (endNorm - startNorm);
        assertEquals(expected, retrieved);
    }

    private KiePMMLNormContinuous getKiePMMLNormContinuous(String name,
                                                           OUTLIER_TREATMENT_METHOD outlierTreatmentMethod,
                                                           Number mapMissingTo) {
        KiePMMLLinearNorm ln0 = new KiePMMLLinearNorm("0", Collections.emptyList(), 24, 26);
        KiePMMLLinearNorm ln1 = new KiePMMLLinearNorm("1", Collections.emptyList(), 30, 32);
        KiePMMLLinearNorm ln2 = new KiePMMLLinearNorm("2", Collections.emptyList(), 36, 34);
        KiePMMLLinearNorm ln3 = new KiePMMLLinearNorm("3", Collections.emptyList(), 40, 39);
        List<KiePMMLLinearNorm> linearNorms = Arrays.asList(ln0, ln1, ln2, ln3);
        return new KiePMMLNormContinuous(name, Collections.emptyList(), linearNorms, outlierTreatmentMethod, mapMissingTo);
    }
}