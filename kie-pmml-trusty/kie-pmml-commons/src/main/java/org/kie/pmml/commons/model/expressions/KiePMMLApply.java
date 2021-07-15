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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.kie.pmml.api.enums.BUILTIN_FUNCTIONS;
import org.kie.pmml.api.enums.INVALID_VALUE_TREATMENT_METHOD;
import org.kie.pmml.commons.model.KiePMMLExtension;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.abstracts.AbstractKiePMMLComponent;
import org.kie.pmml.commons.transformations.KiePMMLDefineFunction;

/**
 * @see <a href=http://dmg.org/pmml/v4-4-1/Functions.html#xsdElement_Apply>Apply</a>
 */
public class KiePMMLApply extends AbstractKiePMMLComponent implements KiePMMLExpression {

    private static final long serialVersionUID = -6975232157053159223L;
    private final String function;
    private String mapMissingTo;
    private String defaultValue;
    private INVALID_VALUE_TREATMENT_METHOD invalidValueTreatmentMethod;
    private List<KiePMMLExpression> kiePMMLExpressions;

    private KiePMMLApply(String name, List<KiePMMLExtension> extensions, String function) {
        super(name, extensions);
        this.function = function;
    }

    public static Builder builder(String name, List<KiePMMLExtension> extensions, String function) {
        return new Builder(name, extensions, function);
    }

    @Override
    public Object evaluate(final ProcessingDTO processingDTO) {
        if (kiePMMLExpressions == null) {
            return null;
        }
        List<Object> expressionValues = new ArrayList<>(); // <- Insertion order matter
        for (KiePMMLExpression kiePMMLExpression : kiePMMLExpressions) {
            expressionValues.add(kiePMMLExpression.evaluate(processingDTO));
        }
        if (BUILTIN_FUNCTIONS.isBUILTIN_FUNCTIONS(function)) {
            BUILTIN_FUNCTIONS builtinFunction = BUILTIN_FUNCTIONS.byName(function);
            return builtinFunction.getValue(expressionValues.toArray(new Object[0]));
        } else {
            final KiePMMLDefineFunction definedFunction = processingDTO.getDefineFunctions()
                    .stream()
                    .filter(defineFunction -> defineFunction.getName().equals(function))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown function " + function));
            return definedFunction.evaluate(processingDTO, expressionValues);
        }
    }

    public String getFunction() {
        return function;
    }

    public String getMapMissingTo() {
        return mapMissingTo;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public INVALID_VALUE_TREATMENT_METHOD getInvalidValueTreatmentMethod() {
        return invalidValueTreatmentMethod;
    }

    public List<KiePMMLExpression> getKiePMMLExpressions() {
        return kiePMMLExpressions != null ? Collections.unmodifiableList(kiePMMLExpressions) : Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KiePMMLApply that = (KiePMMLApply) o;
        return Objects.equals(function, that.function) && Objects.equals(mapMissingTo, that.mapMissingTo) && Objects.equals(defaultValue, that.defaultValue) && invalidValueTreatmentMethod == that.invalidValueTreatmentMethod && Objects.equals(kiePMMLExpressions, that.kiePMMLExpressions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(function, mapMissingTo, defaultValue, invalidValueTreatmentMethod, kiePMMLExpressions);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", KiePMMLApply.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("extensions=" + extensions)
                .add("id='" + id + "'")
                .add("parentId='" + parentId + "'")
                .add("function='" + function + "'")
                .add("mapMissingTo='" + mapMissingTo + "'")
                .add("defaultValue='" + defaultValue + "'")
                .add("invalidValueTreatmentMethod=" + invalidValueTreatmentMethod)
                .add("kiePMMLExpressions=" + kiePMMLExpressions)
                .toString();
    }

    public static class Builder extends AbstractKiePMMLComponent.Builder<KiePMMLApply> {

        private Builder(String name, List<KiePMMLExtension> extensions, String function) {
            super("Apply-", () -> new KiePMMLApply(name, extensions, function));
        }

        public Builder withMapMissingTo(String mapMissingTo) {
            if (mapMissingTo != null) {
                toBuild.mapMissingTo = mapMissingTo;
            }
            return this;
        }

        public Builder withDefaultValue(String defaultValue) {
            if (defaultValue != null) {
                toBuild.defaultValue = defaultValue;
            }
            return this;
        }

        public Builder withInvalidValueTreatmentMethod(String invalidValueTreatment) {
            if (invalidValueTreatment != null) {
                toBuild.invalidValueTreatmentMethod = INVALID_VALUE_TREATMENT_METHOD.byName(invalidValueTreatment);
            }
            return this;
        }

        public Builder withKiePMMLExpressions(List<KiePMMLExpression> kiePMMLExpressions) {
            if (kiePMMLExpressions != null) {
                toBuild.kiePMMLExpressions = kiePMMLExpressions;
            }
            return this;
        }
    }
}
