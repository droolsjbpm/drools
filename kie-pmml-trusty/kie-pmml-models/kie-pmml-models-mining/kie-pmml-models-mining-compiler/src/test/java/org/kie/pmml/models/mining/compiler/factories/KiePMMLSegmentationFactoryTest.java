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

package org.kie.pmml.models.mining.compiler.factories;

import java.util.Map;

import org.junit.Test;
import org.kie.pmml.models.mining.model.segmentation.KiePMMLSegmentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KiePMMLSegmentationFactoryTest extends AbstractKiePMMLFactoryTest {

    @Test
    public void getSegmentation() {
        final String segmentationName = "SEGMENTATION_NAME";
        final KiePMMLSegmentation retrieved = KiePMMLSegmentationFactory.getSegmentation(DATA_DICTIONARY,
                                                                                         TRANSFORMATION_DICTIONARY,
                                                                                         MINING_MODEL.getSegmentation(),
                                                                                         segmentationName,
                                                                                         KNOWLEDGE_BUILDER);
        assertNotNull(retrieved);
        assertEquals(segmentationName, retrieved.getName());
    }

    @Test
    public void getSegmentationSourcesMap() {
        final String segmentationName = "SEGMENTATION_NAME";
        final String packageName = "packagename";
        final Map<String, String> retrieved = KiePMMLSegmentationFactory.getSegmentationSourcesMap(packageName,
                                                                                                   DATA_DICTIONARY,
                                                                                                   TRANSFORMATION_DICTIONARY,
                                                                                                   MINING_MODEL.getSegmentation(),
                                                                                                   segmentationName,
                                                                                                   KNOWLEDGE_BUILDER);
        assertNotNull(retrieved);
    }
}