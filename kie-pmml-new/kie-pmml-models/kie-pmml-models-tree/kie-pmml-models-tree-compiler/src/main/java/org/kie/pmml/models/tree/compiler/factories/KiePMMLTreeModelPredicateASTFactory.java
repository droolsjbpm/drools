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
import java.util.Queue;

import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.kie.pmml.models.drooled.ast.KiePMMLDrooledRule;
import org.kie.pmml.models.drooled.tuples.KiePMMLOriginalTypeGeneratedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to generate a <code>KiePMMLDrooledAST</code> out of a<b>TreeModel</b>
 */
public class KiePMMLTreeModelPredicateASTFactory {

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLTreeModelPredicateASTFactory.class.getName());
    private final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap;
    private final Queue<KiePMMLDrooledRule> rules;

    private KiePMMLTreeModelPredicateASTFactory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap, final Queue<KiePMMLDrooledRule> rules) {
        this.fieldTypeMap = fieldTypeMap;
        this.rules = rules;
    }

    public static KiePMMLTreeModelPredicateASTFactory factory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap, final Queue<KiePMMLDrooledRule> rules) {
        return new KiePMMLTreeModelPredicateASTFactory(fieldTypeMap, rules);
    }

    /**
     * Manage the given <code>Predicate</code>. At this point of the execution, <b>predicate</b> could be:
     * <p>1) @see <a href="http://dmg.org/pmml/v4-4/TreeModel.html#xsdElement_SimplePredicate">SimplePredicate</a><p>
     * <p>2) @see <a href="http://dmg.org/pmml/v4-4/TreeModel.html#xsdElement_CompoundPredicate">CompoundPredicate</a><p>
     * <p>3) @see <a href="http://dmg.org/pmml/v4-4/TreeModel.html#xsdElement_SimpleSetPredicate">SimpleSetPredicate</a><p>
     * @param predicate
     * @param parentPath
     */
    public void declareRuleFromPredicate(final Predicate predicate,
                                         final String parentPath,
                                         final String currentRule,
                                         final Object result,
                                         final boolean isFinalLeaf) {
        logger.info("declareRuleFromPredicate {} {} {} {}", predicate, parentPath, currentRule, result);
        if (predicate instanceof True) {
            KiePMMLTreeModelTruePredicateASTFactory.factory((True) predicate, rules).declareRuleFromTruePredicate(parentPath, currentRule, result, isFinalLeaf);
        } else if (predicate instanceof SimplePredicate) {
            KiePMMLTreeModelSimplePredicateASTFactory.factory((SimplePredicate) predicate, fieldTypeMap, rules).declareRuleFromSimplePredicate(parentPath, currentRule, result, isFinalLeaf);
        } else if (predicate instanceof CompoundPredicate) {
            KiePMMLTreeModelCompoundPredicateASTFactory.factory((CompoundPredicate) predicate, fieldTypeMap, rules).declareRuleFromCompoundPredicate(parentPath, currentRule, result, isFinalLeaf);
        }
    }
}