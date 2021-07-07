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
package org.kie.pmml.commons.model.expressions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.commons.model.KiePMMLExtension;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.abstracts.AbstractKiePMMLComponent;

import static org.kie.pmml.commons.model.expressions.ExpressionsUtils.getFromPossibleSources;

/**
 * @see <a href=http://dmg.org/pmml/v4-4-1/Transformations.html#xsdElement_MapValues>MapValues</a>
 */
public class KiePMMLMapValues extends AbstractKiePMMLComponent implements KiePMMLExpression {

    private static final long serialVersionUID = 4576394527423997787L;
    private final String outputColumn;
    private KiePMMLInlineTable inlineTable;
    private List<KiePMMLFieldColumnPair> fieldColumnPairs = new ArrayList<>();
    private String defaultValue;
    private String mapMissingTo;
    private DATA_TYPE dataType;

    private KiePMMLMapValues(String name, List<KiePMMLExtension> extensions, String outputColumn) {
        super(name, extensions);
        this.outputColumn = outputColumn;
    }

    public static Builder builder(String name, List<KiePMMLExtension> extensions, String outputColumn) {
        return new Builder(name, extensions, outputColumn);
    }

    @Override
    public Object evaluate(final ProcessingDTO processingDTO) {
        Map<String, Object> columnPairsMap = new HashMap<>();
        for (KiePMMLFieldColumnPair kiePMMLFieldColumnPair : fieldColumnPairs) {
            Optional<Object> value = getFromPossibleSources(kiePMMLFieldColumnPair.getName(), processingDTO);
            if (value.isPresent()) {
                columnPairsMap.put(kiePMMLFieldColumnPair.getColumn(), value.get());
            } else {
                return mapMissingTo;
            }
        }
        Optional<Object> retrieved = Optional.empty();
        if (inlineTable != null) {
            retrieved = inlineTable.evaluate(columnPairsMap, outputColumn);
        }
        return retrieved.orElse(defaultValue);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", KiePMMLMapValues.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("outputColumn='" + outputColumn + "'")
                .add("inlineTable=" + inlineTable)
                .add("defaultValue='" + defaultValue + "'")
                .add("mapMissingTo='" + mapMissingTo + "'")
                .add("dataType=" + dataType)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KiePMMLMapValues that = (KiePMMLMapValues) o;
        return Objects.equals(outputColumn, that.outputColumn) && Objects.equals(inlineTable, that.inlineTable) && Objects.equals(defaultValue, that.defaultValue) && Objects.equals(mapMissingTo, that.mapMissingTo) && dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(outputColumn, inlineTable, defaultValue, mapMissingTo, dataType);
    }

    public static class Builder extends AbstractKiePMMLComponent.Builder<KiePMMLMapValues> {

        private Builder(String name, List<KiePMMLExtension> extensions, String outputColumn) {
            super("MapValues-", () -> new KiePMMLMapValues(name, extensions, outputColumn));
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

        public Builder withKiePMMLInlineTable(KiePMMLInlineTable inlineTable) {
            if (inlineTable != null) {
                toBuild.inlineTable = inlineTable;
            }
            return this;
        }

        public Builder withKiePMMLFieldColumnPairs(List<KiePMMLFieldColumnPair> fieldColumnPairs) {
            if (fieldColumnPairs != null) {
                toBuild.fieldColumnPairs = fieldColumnPairs;
            }
            return this;
        }

        public Builder withDataType(DATA_TYPE dataType) {
            if (dataType != null) {
                toBuild.dataType = dataType;
            }
            return this;
        }
    }
}
