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

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.TransformationDictionary;
import org.dmg.pmml.mining.Segmentation;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.pmml.models.mining.model.enums.MULTIPLE_MODEL_METHOD;
import org.kie.pmml.models.mining.model.segmentation.KiePMMLSegmentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.commons.utils.KiePMMLModelUtils.getSanitizedPackageName;
import static org.kie.pmml.compiler.commons.factories.KiePMMLExtensionFactory.getKiePMMLExtensions;
import static org.kie.pmml.models.mining.compiler.factories.KiePMMLSegmentFactory.getSegments;
import static org.kie.pmml.models.mining.compiler.factories.KiePMMLSegmentFactory.getSegmentsSourcesMap;

public class KiePMMLSegmentationFactory {

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLSegmentationFactory.class.getName());

    private KiePMMLSegmentationFactory() {
    }

    public static KiePMMLSegmentation getSegmentation(final DataDictionary dataDictionary,
                                                      final TransformationDictionary transformationDictionary,
                                                      final Segmentation segmentation,
                                                      final String segmentationName,
                                                      final KnowledgeBuilder kBuilder) {
        logger.debug("getSegmentation {}", segmentation);
        return KiePMMLSegmentation.builder(segmentationName,
                                           getKiePMMLExtensions(segmentation.getExtensions()),
                                           MULTIPLE_MODEL_METHOD.byName(segmentation.getMultipleModelMethod().value()))
                .withSegments(getSegments(dataDictionary, transformationDictionary, segmentation.getSegments(),
                                          kBuilder))
                .build();
    }

    public static Map<String, String> getSegmentationSourcesMap(final String parentPackageName,
                                                                final DataDictionary dataDictionary,
                                                                final TransformationDictionary transformationDictionary,
                                                                final Segmentation segmentation,
                                                                final String segmentationName,
                                                                final KnowledgeBuilder kBuilder) {
        logger.debug("getSegmentationSourcesMap {}", segmentation);
        final String packageName = getSanitizedPackageName(parentPackageName + "." + segmentationName);
        return getSegmentsSourcesMap(packageName,
                                     dataDictionary,
                                     transformationDictionary,
                                     segmentation.getSegments(),
                                     kBuilder);
    }
}
