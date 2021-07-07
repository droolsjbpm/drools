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

import static org.junit.Assert.assertEquals;

public class KiePMMLConstantTest {

    @Test
    public void evaluate() {
        Object value = 234.45;
        final KiePMMLConstant kiePMMLConstant = new KiePMMLConstant("NAME", Collections.emptyList(), value);
        ProcessingDTO processingDTO = new ProcessingDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Object retrieved = kiePMMLConstant.evaluate(processingDTO);
        assertEquals(value, retrieved);
    }
}