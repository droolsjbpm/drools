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

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.kie.pmml.commons.model.enums.DATA_TYPE;
import org.kie.pmml.models.drooled.ast.KiePMMLDrooledType;
import org.kie.pmml.models.drooled.tuples.KiePMMLOriginalTypeGeneratedType;

import static org.kie.pmml.commons.utils.DrooledModelUtils.getSanitizedClassName;

/**
 * Class used to generate a <code>KiePMMLDrooledType</code>s out of a<b>DataDictionary</b>
 */
public class KiePMMLTreeModelDataDictionaryASTFactory {

    private final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap;

    private KiePMMLTreeModelDataDictionaryASTFactory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap) {
        this.fieldTypeMap = fieldTypeMap;
    }

    /**
     * @param fieldTypeMap the <code>Map&lt;String, KiePMMLOriginalTypeGeneratedType&gt;</code> to be populated with mapping between original field' name and <b>original type/generated type</b> tupla
     * @return
     */
    public static KiePMMLTreeModelDataDictionaryASTFactory factory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap) {
        return new KiePMMLTreeModelDataDictionaryASTFactory(fieldTypeMap);
    }

    /**
     * Create a <code>List&lt;KiePMMLDrooledType&gt;</code> out of original <code>DataField</code>s,
     * and <b>populate</b> the <b>fieldNameTypeNameMap</b> with mapping between original field' name and <b>original type/generated type</b> tupla
     * @param dataDictionary
     */
    public Queue<KiePMMLDrooledType> declareTypes(final DataDictionary dataDictionary) {
        return dataDictionary.getDataFields().stream().map(this::declareType).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Create a <code>KiePMMLDrooledType</code> out of original <code>DataField</code>,
     * and <b>populate</b> the <b>fieldNameTypeNameMap</b> with mapping between original field' name and <b>original type/generated type</b> tupla
     * @param dataField
     */
    public KiePMMLDrooledType declareType(DataField dataField) {
        String generatedType = getSanitizedClassName(dataField.getName().getValue().toUpperCase());
        String fieldName = dataField.getName().getValue();
        String fieldType = dataField.getDataType().value();
        fieldTypeMap.put(fieldName, new KiePMMLOriginalTypeGeneratedType(fieldType, generatedType));
        return new KiePMMLDrooledType(generatedType, DATA_TYPE.byName(fieldType).getMappedClass().getSimpleName());
    }
}