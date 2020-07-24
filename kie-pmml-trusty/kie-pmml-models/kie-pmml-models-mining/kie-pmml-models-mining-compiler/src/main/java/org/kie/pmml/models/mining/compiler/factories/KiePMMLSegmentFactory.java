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

import java.util.List;
import java.util.stream.Collectors;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.TransformationDictionary;
import org.dmg.pmml.mining.Segment;
import org.kie.pmml.commons.exceptions.KiePMMLException;
import org.kie.pmml.models.mining.model.segmentation.KiePMMLSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.compiler.commons.factories.KiePMMLExtensionFactory.getKiePMMLExtensions;
import static org.kie.pmml.compiler.commons.factories.KiePMMLPredicateFactory.getPredicate;
import static org.kie.pmml.compiler.commons.implementations.KiePMMLModelRetriever.getFromCommonDataAndTransformationDictionaryAndModel;

public class KiePMMLSegmentFactory {

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLSegmentFactory.class.getName());

    private KiePMMLSegmentFactory() {
    }

    public static List<KiePMMLSegment> getSegments(final DataDictionary dataDictionary,
                                                   final TransformationDictionary transformationDictionary,
                                                   final List<Segment> segments,
                                                   final Object kBuilder) {
        logger.debug("getSegments {}", segments);
        return segments.stream().map(segment -> getSegment(dataDictionary, transformationDictionary, segment, kBuilder)).collect(Collectors.toList());
    }

    public static KiePMMLSegment getSegment(final DataDictionary dataDictionary,
                                            final TransformationDictionary transformationDictionary,
                                            final Segment segment,
                                            final Object kBuilder) {
        logger.debug("getSegment {}", segment);
        return KiePMMLSegment.builder("PUPPA",
                                      getKiePMMLExtensions(segment.getExtensions()),
                                      getPredicate(segment.getPredicate(), dataDictionary),
                                      getFromCommonDataAndTransformationDictionaryAndModel(dataDictionary,
                                                                                           transformationDictionary,
                                                                                           segment.getModel(),
                                                                                           kBuilder).orElseThrow(() -> new KiePMMLException("Failed to get the KiePMMLModel for segment " + segment.getModel().getModelName())))
                .withWeight(segment.getWeight().doubleValue())
                .build();
    }
}
