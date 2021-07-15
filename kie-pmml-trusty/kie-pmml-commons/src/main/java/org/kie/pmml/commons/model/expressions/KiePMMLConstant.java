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

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.kie.pmml.commons.model.KiePMMLExtension;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.abstracts.AbstractKiePMMLComponent;

/**
 * @see <a href=http://dmg.org/pmml/v4-4-1/Transformations.html#xsdElement_Constant>Constant</a>
 */
public class KiePMMLConstant extends AbstractKiePMMLComponent implements KiePMMLExpression {

    private static final long serialVersionUID = 3312643278386342170L;
    private final Object value;

    public KiePMMLConstant(String name, List<KiePMMLExtension> extensions, Object value) {
        super(name, extensions);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public Object evaluate(final ProcessingDTO processingDTO) {
        return getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KiePMMLConstant that = (KiePMMLConstant) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", KiePMMLConstant.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("extensions=" + extensions)
                .add("id='" + id + "'")
                .add("parentId='" + parentId + "'")
                .add("value=" + value)
                .toString();
    }
}
