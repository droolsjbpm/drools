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

import java.util.Collections;

import org.junit.Test;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KiePMMLNormDiscreteTest {

    @Test
    public void evaluateMissingValue() {
        String fieldName = "fieldName";
        String fieldValue = "fieldValue";
        Number mapMissingTo = 1.0;
        KiePMMLNormDiscrete kiePMMLNormContinuous = getKiePMMLNormDiscrete(fieldName, fieldValue, mapMissingTo);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Object retrieved = kiePMMLNormContinuous.evaluate(processingDTO);
        assertNotNull(retrieved);
        assertEquals(mapMissingTo, retrieved);
    }

    @Test
    public void evaluateSameValue() {
        String fieldName = "fieldName";
        String fieldValue = "fieldValue";
        Number mapMissingTo = null;
        KiePMMLNormDiscrete kiePMMLNormContinuous = getKiePMMLNormDiscrete(fieldName, fieldValue, mapMissingTo);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.singletonList(new KiePMMLNameValue(fieldName, fieldValue)));
        Object retrieved = kiePMMLNormContinuous.evaluate(processingDTO);
        assertNotNull(retrieved);
        assertEquals(1.0, retrieved);
    }

    @Test
    public void evaluateDifferentValue() {
        String fieldName = "fieldName";
        String fieldValue = "fieldValue";
        Number mapMissingTo = null;
        KiePMMLNormDiscrete kiePMMLNormContinuous = getKiePMMLNormDiscrete(fieldName, fieldValue, mapMissingTo);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.singletonList(new KiePMMLNameValue(fieldName, "anotherValue")));
        Object retrieved = kiePMMLNormContinuous.evaluate(processingDTO);
        assertNotNull(retrieved);
        assertEquals(0.0, retrieved);
    }

    private KiePMMLNormDiscrete getKiePMMLNormDiscrete(String name,
                                                       String value,
                                                       Number mapMissingTo) {
        return new KiePMMLNormDiscrete(name, Collections.emptyList(), value, mapMissingTo);
    }
}