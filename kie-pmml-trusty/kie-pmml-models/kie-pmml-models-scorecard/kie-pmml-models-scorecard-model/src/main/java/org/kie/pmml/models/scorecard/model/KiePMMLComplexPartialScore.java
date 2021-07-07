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
package org.kie.pmml.models.scorecard.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kie.pmml.commons.model.KiePMMLExtension;
import org.kie.pmml.commons.model.KiePMMLOutputField;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.abstracts.AbstractKiePMMLComponent;
import org.kie.pmml.commons.model.expressions.KiePMMLExpression;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;
import org.kie.pmml.commons.transformations.KiePMMLDefineFunction;
import org.kie.pmml.commons.transformations.KiePMMLDerivedField;

/**
 * @see <a href=http://dmg.org/pmml/v4-4-1/Scorecard.html#xsdElement_ComplexPartialScore>ComplexPartialScore</a>
 */
public class KiePMMLComplexPartialScore extends AbstractKiePMMLComponent {

    private static final long serialVersionUID = 3456691439946792947L;
    private final KiePMMLExpression expression;

    public KiePMMLComplexPartialScore(String name, List<KiePMMLExtension> extensions, KiePMMLExpression expression) {
        super(name, extensions);
        this.expression = expression;
    }

    public Number evaluate(final List<KiePMMLDefineFunction> defineFunctions,
                           final List<KiePMMLDerivedField> derivedFields,
                           final List<KiePMMLOutputField> outputFields,
                           final Map<String, Object> inputData) {
        final List<KiePMMLNameValue> kiePMMLNameValues = getKiePMMLNameValuesFromInputDataMap(inputData);
        ProcessingDTO processingDTO = new ProcessingDTO(defineFunctions, derivedFields, outputFields, kiePMMLNameValues);
        Object toReturn = expression.evaluate(processingDTO);
        return toReturn != null ? (Number) toReturn : null;
    }

    static List<KiePMMLNameValue> getKiePMMLNameValuesFromInputDataMap(final Map<String, Object> inputData) {
        return inputData.entrySet().stream()
                .map(entry -> new KiePMMLNameValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
