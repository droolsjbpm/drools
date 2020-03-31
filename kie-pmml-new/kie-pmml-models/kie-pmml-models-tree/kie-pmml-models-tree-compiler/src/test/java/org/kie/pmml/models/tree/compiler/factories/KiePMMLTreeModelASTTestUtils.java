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
package org.kie.pmml.models.tree.compiler.factories;

import java.util.Map;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.SimplePredicate;
import org.kie.pmml.models.drooled.tuples.KiePMMLOriginalTypeGeneratedType;

import static org.kie.pmml.commons.utils.DrooledModelUtils.getSanitizedClassName;

public class KiePMMLTreeModelASTTestUtils {

    public static DataField getTypeDataField() {
        DataField toReturn = new DataField();
        toReturn.setOpType(OpType.CONTINUOUS);
        toReturn.setDataType(DataType.DATE);
        toReturn.setName(FieldName.create("dataField"));
        return toReturn;
    }

    public static DataField getDottedTypeDataField() {
        DataField toReturn = new DataField();
        toReturn.setOpType(OpType.CONTINUOUS);
        toReturn.setDataType(DataType.BOOLEAN);
        toReturn.setName(FieldName.create("dotted.field"));
        return toReturn;
    }

    public static SimplePredicate getSimplePredicate(String predicateName, DataType dataType, String value, final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap) {
        FieldName fieldName = FieldName.create(predicateName);
        fieldTypeMap.put(fieldName.getValue(), new KiePMMLOriginalTypeGeneratedType(dataType.value()/*DataType.STRING.value()*/, getSanitizedClassName(fieldName.getValue().toUpperCase())));
        SimplePredicate toReturn = new SimplePredicate();
        toReturn.setField(fieldName);
        toReturn.setOperator(SimplePredicate.Operator.LESS_THAN);
        toReturn.setValue(value);
        return toReturn;
    }
}