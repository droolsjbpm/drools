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

import org.dmg.pmml.False;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.kie.pmml.models.drooled.ast.KiePMMLDrooledRule;
import org.kie.pmml.models.drooled.tuples.KiePMMLOriginalTypeGeneratedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.models.tree.compiler.factories.KiePMMLTreeModelASTFactory.PATH_PATTERN;

/**
 * Class used to generate a <code>KiePMMLDrooledAST</code> out of a<b>TreeModel</b>
 */
public class KiePMMLTreeModelNodeASTFactory {

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLTreeModelNodeASTFactory.class.getName());

    private final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap;

    private KiePMMLTreeModelNodeASTFactory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap) {
        this.fieldTypeMap = fieldTypeMap;
    }

    public static KiePMMLTreeModelNodeASTFactory factory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap) {
        return new KiePMMLTreeModelNodeASTFactory(fieldTypeMap);
    }

    public Queue<KiePMMLDrooledRule> declareRulesFromRootNode(final Node node, final String parentPath) {
        logger.info("declareRulesFromRootNode {} {}", node, parentPath);
        Queue<KiePMMLDrooledRule> toReturn = new LinkedList<>();
        declareRuleFromNode(node, parentPath, toReturn);
        return toReturn;
    }

    protected void declareRuleFromNode(final Node node, final String parentPath,
                                    final Queue<KiePMMLDrooledRule> rules) {
        logger.info("declareRuleFromNode {} {}", node, parentPath);
        if (isFinalLeaf(node)) {
            declareFinalRuleFromNode(node, parentPath, rules);
        } else {
            declareIntermediateRuleFromNode(node, parentPath, rules);
        }
    }

    /**
     * This method is meant to be executed when <code>node</code> <b>is</b> a <i>final leaf</i>
     * @param node
     * @param parentPath
     * @param rules
     */
    protected void declareFinalRuleFromNode(final Node node,
                                         final String parentPath,
                                         final Queue<KiePMMLDrooledRule> rules) {
        logger.info("declareFinalRuleFromNode {} {}", node, parentPath);
        final Predicate predicate = node.getPredicate();
        // This means the rule should not be created at all.
        // Different semantics has to be implemented if the "False"/"True" predicates are declared inside
        // an XOR compound predicate
        if (predicate instanceof False) {
            return;
        }
        String currentRule = String.format(PATH_PATTERN, parentPath, node.getScore().toString());
        if (!(predicate instanceof True)) {
            KiePMMLTreeModelPredicateASTFactory.factory(fieldTypeMap, rules).declareRuleFromPredicate(predicate, parentPath, currentRule, node.getScore(), true);
        }
    }

    /**
     * This method is meant to be executed when <code>node</code> <b>is not</b> a <i>final leaf</i>
     * @param node
     * @param parentPath
     * @param rules
     */
    protected void declareIntermediateRuleFromNode(final Node node,
                                                final String parentPath,
                                                final Queue<KiePMMLDrooledRule> rules) {
        logger.info("declareIntermediateRuleFromNode {} {}", node, parentPath);
        final Predicate predicate = node.getPredicate();
        // This means the rule should not be created at all.
        // Different semantics has to be implemented if the "False"/"True" predicates are declared inside
        // an XOR compound predicate
        if (predicate instanceof False) {
            return;
        }
        String currentRule = String.format(PATH_PATTERN, parentPath, node.getScore().toString());
        if (predicate instanceof True) {
            KiePMMLTreeModelPredicateASTFactory.factory(fieldTypeMap, rules).declareRuleFromPredicate(predicate, parentPath, currentRule, node.getScore(), false);
        } else {
            KiePMMLTreeModelPredicateASTFactory.factory(fieldTypeMap, rules).declareRuleFromPredicate(predicate, parentPath, currentRule, node.getScore(), false);
        }
        node.getNodes().forEach(child -> declareRuleFromNode(child, currentRule, rules));
    }

    protected boolean isFinalLeaf(final Node node) {
        return node instanceof LeafNode || node.getNodes() == null || node.getNodes().isEmpty();
    }
}