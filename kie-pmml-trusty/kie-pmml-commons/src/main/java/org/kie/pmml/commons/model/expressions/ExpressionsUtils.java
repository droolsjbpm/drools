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

import java.util.Optional;
import java.util.stream.Stream;

import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;

/**
 * Helper methods for <code>KiePMMLExpression</code>s
 */
public class ExpressionsUtils {

    private ExpressionsUtils() {
        // avoid instantiation
    }

    public static Optional<Object> getFromPossibleSources(final String name, final ProcessingDTO processingDTO) {
        return Stream.of(getFromKiePMMLNameValues(name, processingDTO),
                         getFromDerivedFields(name, processingDTO),
                         getFromOutputFields(name, processingDTO))
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get);
    }

    public static Optional<Object> getFromKiePMMLNameValues(final String name, final ProcessingDTO processingDTO) {
        return processingDTO.getKiePMMLNameValues()
                .stream()
                .filter(kiePMMLNameValue -> kiePMMLNameValue.getName().equals(name))
                .findFirst()
                .map(KiePMMLNameValue::getValue);
    }

    public static Optional<Object> getFromDerivedFields(final String name, final ProcessingDTO processingDTO) {
        return processingDTO.getDerivedFields()
                .stream()
                .filter(derivedField -> derivedField.getName().equals(name))
                .findFirst()
                .map(derivedField -> derivedField.evaluate(processingDTO));
    }

    public static Optional<Object> getFromOutputFields(final String name,
                                                       final ProcessingDTO processingDTO) {
        return processingDTO.getOutputFields()
                .stream()
                .filter(outputField -> outputField.getName().equals(name))
                .findFirst()
                .map(outputField -> outputField.evaluate(processingDTO));
    }
}
