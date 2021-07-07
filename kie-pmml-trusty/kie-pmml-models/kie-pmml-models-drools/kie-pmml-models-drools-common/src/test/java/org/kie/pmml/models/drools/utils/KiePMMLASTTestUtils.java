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
package org.kie.pmml.models.drools.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.TransformationDictionary;
import org.kie.pmml.models.drools.ast.KiePMMLDroolsRule;
import org.kie.pmml.models.drools.ast.factories.KiePMMLDataDictionaryASTFactory;
import org.kie.pmml.models.drools.ast.factories.KiePMMLDerivedFieldASTFactory;
import org.kie.pmml.models.drools.ast.factories.PredicateASTFactoryData;
import org.kie.pmml.models.drools.tuples.KiePMMLOriginalTypeGeneratedType;

/**
 * Utility methods for other <b>Test</b> classes
 */
public class KiePMMLASTTestUtils {

    public static PredicateASTFactoryData getPredicateASTFactoryData(Predicate predicate,
                                                                     List<OutputField> outputFields,
                                                                     List<KiePMMLDroolsRule> rules,
                                                                     String parentPath,
                                                                     String currentRule,
                                                                     Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap) {
        return new PredicateASTFactoryData(predicate, outputFields, rules, parentPath, currentRule, fieldTypeMap);
    }

    public static Map<String, KiePMMLOriginalTypeGeneratedType> getFieldTypeMap(final DataDictionary dataDictionary, final TransformationDictionary transformationDictionary, final LocalTransformations localTransformations) {
        final Map<String, KiePMMLOriginalTypeGeneratedType> toReturn = new HashMap<>();
        KiePMMLDerivedFieldASTFactory kiePMMLDerivedFieldASTFactory = KiePMMLDerivedFieldASTFactory.factory(toReturn);
        if (transformationDictionary != null && transformationDictionary.getDerivedFields() != null) {
            kiePMMLDerivedFieldASTFactory.declareTypes(transformationDictionary.getDerivedFields());
        }
        if (localTransformations != null && localTransformations.getDerivedFields() != null) {
            kiePMMLDerivedFieldASTFactory.declareTypes(localTransformations.getDerivedFields());
        }
        KiePMMLDataDictionaryASTFactory.factory(toReturn).declareTypes(dataDictionary);
        return toReturn;
    }

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

}
