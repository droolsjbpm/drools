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
package org.kie.pmml.models.drools.ast.factories;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.OutputField;
import org.dmg.pmml.Predicate;
import org.kie.pmml.models.drools.ast.KiePMMLDroolsRule;
import org.kie.pmml.models.drools.tuples.KiePMMLOriginalTypeGeneratedType;

/**
 * Data class to contain objects required by <b>Predicate</b>s concrete ASTFactories
 */
public class PredicateASTFactoryData {

    private final Predicate predicate;
    private final List<OutputField> outputFields;
    private final List<KiePMMLDroolsRule> rules;
    private final String parentPath;
    private final String currentRule;
    private final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap;

    public PredicateASTFactoryData(Predicate predicate,
                                   List<OutputField> outputFields,
                                   List<KiePMMLDroolsRule> rules,
                                   String parentPath,
                                   String currentRule,
                                   Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap) {
        this.predicate = predicate;
        this.outputFields = outputFields != null ? Collections.unmodifiableList(outputFields) : Collections.emptyList();
        this.rules = rules;
        this.parentPath = parentPath;
        this.currentRule = currentRule;
        this.fieldTypeMap = fieldTypeMap != null ? Collections.unmodifiableMap(fieldTypeMap) : Collections.emptyMap();
    }

    public PredicateASTFactoryData cloneWithPredicate(Predicate predicate) {
        return new PredicateASTFactoryData(predicate, outputFields, rules, parentPath, currentRule, fieldTypeMap);
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public List<OutputField> getOutputFields() {
        return outputFields;
    }

    public List<KiePMMLDroolsRule> getRules() {
        return rules;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getCurrentRule() {
        return currentRule;
    }

    public Map<String, KiePMMLOriginalTypeGeneratedType> getFieldTypeMap() {
        return fieldTypeMap;
    }
}
