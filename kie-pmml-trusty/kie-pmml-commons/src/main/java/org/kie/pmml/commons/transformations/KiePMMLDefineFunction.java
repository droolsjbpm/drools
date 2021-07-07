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
package org.kie.pmml.commons.transformations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.commons.model.KiePMMLExtension;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.abstracts.AbstractKiePMMLComponent;
import org.kie.pmml.commons.model.expressions.KiePMMLExpression;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;

/**
 * @see <a href=http://dmg.org/pmml/v4-4-1/Functions.html#xsdElement_DefineFunction>DefineFunction</a>
 */
public class KiePMMLDefineFunction extends AbstractKiePMMLComponent implements Serializable {

    private static final long serialVersionUID = 9187889880911645935L;
    private final OP_TYPE opType;
    private final List<KiePMMLParameterField> parameterFields;
    private final KiePMMLExpression kiePMMLExpression;
    private DATA_TYPE dataType;

    public KiePMMLDefineFunction(String name,
                                 List<KiePMMLExtension> extensions,
                                 String opType,
                                 List<KiePMMLParameterField> parameterFields,
                                 KiePMMLExpression kiePMMLExpression) {
        super(name, extensions);
        this.opType = OP_TYPE.byName(opType);
        this.parameterFields = parameterFields;
        this.kiePMMLExpression = kiePMMLExpression;
    }

    public OP_TYPE getOpType() {
        return opType;
    }

    public List<KiePMMLParameterField> getParameterFields() {
        return Collections.unmodifiableList(parameterFields);
    }

    public DATA_TYPE getDataType() {
        return dataType;
    }

    public Object evaluate(final ProcessingDTO processingDTO,
                           final List<Object> paramValues) {
        final List<KiePMMLNameValue> kiePMMLNameValues = new ArrayList<>();
        if (parameterFields != null) {
            if (paramValues == null || paramValues.size() < parameterFields.size()) {
                throw new IllegalArgumentException("Expected at least " + parameterFields.size() + " arguments for " + name + " DefineFunction");
            }
            for (int i = 0; i < parameterFields.size(); i++) {
                kiePMMLNameValues.add(new KiePMMLNameValue(parameterFields.get(i).getName(), paramValues.get(i)));
            }
        }
        for (KiePMMLNameValue kiePMMLNameValue : kiePMMLNameValues) {
            processingDTO.addKiePMMLNameValue(kiePMMLNameValue);
        }
        return kiePMMLExpression.evaluate(processingDTO);
    }
}
