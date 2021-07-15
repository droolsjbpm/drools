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
package org.kie.pmml.models.drools.tree.compiler.factories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.False;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.models.drools.ast.KiePMMLDroolsRule;
import org.kie.pmml.models.drools.ast.factories.KiePMMLPredicateASTFactory;
import org.kie.pmml.models.drools.ast.factories.PredicateASTFactoryData;
import org.kie.pmml.models.drools.tuples.KiePMMLOriginalTypeGeneratedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.models.drools.commons.utils.KiePMMLDroolsModelUtils.getCorrectlyFormattedResult;
import static org.kie.pmml.models.drools.tree.compiler.factories.KiePMMLTreeModelASTFactory.PATH_PATTERN;

/**
 * Class used to generate <code>KiePMMLDroolsRule</code>s out of a <code>Node</code>
 */
public class KiePMMLTreeModelNodeASTFactory {

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLTreeModelNodeASTFactory.class.getName());

    private final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap;
    private final List<OutputField> outputFields;
    private final TreeModel.NoTrueChildStrategy noTrueChildStrategy;
    private final DATA_TYPE targetType;

    private KiePMMLTreeModelNodeASTFactory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap, final List<OutputField> outputFields, final TreeModel.NoTrueChildStrategy noTrueChildStrategy, final DATA_TYPE targetType) {
        this.fieldTypeMap = fieldTypeMap;
        this.outputFields = outputFields;
        this.noTrueChildStrategy = noTrueChildStrategy;
        this.targetType = targetType;
    }

    public static KiePMMLTreeModelNodeASTFactory factory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap, final List<OutputField> outputFields, final TreeModel.NoTrueChildStrategy noTrueChildStrategy, final DATA_TYPE targetType) {
        return new KiePMMLTreeModelNodeASTFactory(fieldTypeMap, outputFields, noTrueChildStrategy, targetType);
    }

    public List<KiePMMLDroolsRule> declareRulesFromRootNode(final Node node, final String parentPath) {
        logger.trace("declareRulesFromRootNode {} {}", node, parentPath);
        List<KiePMMLDroolsRule> toReturn = new ArrayList<>();
        declareRuleFromNode(node, parentPath, toReturn);
        return toReturn;
    }

    protected void declareRuleFromNode(final Node node, final String parentPath,
                                       final List<KiePMMLDroolsRule> rules) {
        logger.trace("declareRuleFromNode {} {}", node, parentPath);
        if (isFinalLeaf(node)) {
            declareFinalRuleFromNode(node, parentPath, rules);
        } else {
            declareIntermediateRuleFromNode(node, parentPath, rules);
            if (TreeModel.NoTrueChildStrategy.RETURN_LAST_PREDICTION.equals(noTrueChildStrategy) && node.getScore() != null) {
                declareDefaultRuleFromNode(node, parentPath, rules);
            }
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
                                            final List<KiePMMLDroolsRule> rules) {
        logger.trace("declareFinalRuleFromNode {} {}", node, parentPath);
        final Predicate predicate = node.getPredicate();
        // This means the rule should not be created at all.
        // Different semantics has to be implemented if the "False"/"True" predicates are declared inside
        // an XOR compound predicate
        if (predicate instanceof False) {
            return;
        }
        String currentRule = String.format(PATH_PATTERN, parentPath, node.hashCode());
        PredicateASTFactoryData predicateASTFactoryData = new PredicateASTFactoryData(predicate, outputFields, rules, parentPath, currentRule, fieldTypeMap);
        KiePMMLPredicateASTFactory.factory(predicateASTFactoryData).declareRuleFromPredicate(getCorrectlyFormattedResult(node.getScore(), targetType), true);
    }

    /**
     * This method is meant to be executed when <code>node</code> <b>is not</b> a <i>final leaf</i>
     * @param node
     * @param parentPath
     * @param rules
     */
    protected void declareIntermediateRuleFromNode(final Node node,
                                                   final String parentPath,
                                                   final List<KiePMMLDroolsRule> rules) {
        logger.trace("declareIntermediateRuleFromNode {} {}", node, parentPath);
        final Predicate predicate = node.getPredicate();
        // This means the rule should not be created at all.
        // Different semantics has to be implemented if the "False"/"True" predicates are declared inside
        // an XOR compound predicate
        if (predicate instanceof False) {
            return;
        }
        String currentRule = String.format(PATH_PATTERN, parentPath, node.hashCode());
        PredicateASTFactoryData predicateASTFactoryData = new PredicateASTFactoryData(predicate, outputFields, rules, parentPath, currentRule, fieldTypeMap);
        KiePMMLPredicateASTFactory.factory(predicateASTFactoryData).declareRuleFromPredicate(getCorrectlyFormattedResult(node.getScore(), targetType), false);
        node.getNodes().forEach(child -> declareRuleFromNode(child, currentRule, rules));
    }

    /**
     * This method is meant to be executed when <b>noTrueChildStrategy</b> is <code>TreeModel.NoTrueChildStrategy.RETURN_LAST_PREDICTION</code>, <b>node</b>
     * is not a <i>final leaf</i>, and <b>node</b>'s score is not null
     * @param node
     * @param parentPath
     * @param rules
     */
    protected void declareDefaultRuleFromNode(final Node node,
                                              final String parentPath,
                                              final List<KiePMMLDroolsRule> rules) {
        logger.trace("declareDefaultRuleFromNode {} {}", node, parentPath);
        String originalRule = String.format(PATH_PATTERN, parentPath, node.hashCode());
        String currentRule = String.format(PATH_PATTERN, "default", originalRule);
        PredicateASTFactoryData predicateASTFactoryData = new PredicateASTFactoryData(new True(), outputFields, rules, originalRule, currentRule, fieldTypeMap);
        KiePMMLPredicateASTFactory.factory(predicateASTFactoryData).declareRuleFromPredicate(getCorrectlyFormattedResult(node.getScore(), targetType), true);
    }

    protected boolean isFinalLeaf(final Node node) {
        return node instanceof LeafNode || node.getNodes() == null || node.getNodes().isEmpty();
    }
}
