/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.core.addon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.rule.Accumulate;
import org.drools.core.rule.Collect;
import org.drools.core.rule.Forall;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.Pattern;
import org.drools.core.rule.PatternSource;
import org.drools.core.rule.RuleConditionElement;
import org.drools.core.spi.AlphaNodeFieldConstraint;
import org.drools.core.spi.ObjectType;
import org.kie.api.definition.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Reorder based on usage count of alpha constraints
 *
 */
public class CountBasedOrderingStrategy implements AlphaNodeOrderingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(CountBasedOrderingStrategy.class);

    private Map<ObjectType, Map<AlphaNodeFieldConstraint, Integer>> analyzedAlphaConstraints = new HashMap<>();

    @Override
    public void analyzeAlphaConstraints(Map<String, InternalKnowledgePackage> pkgs, Collection<InternalKnowledgePackage> newPkgs) {

        Set<Rule> ruleSet = collectRules(pkgs, newPkgs);

        List<Pattern> patternList = new ArrayList<>();
        ruleSet.stream()
               .forEach(rule -> collectPatterns(((RuleImpl) rule).getLhs(), patternList));
        patternList.removeIf(Objects::isNull);

        for (Pattern pattern : patternList) {
            ObjectType objectType = pattern.getObjectType();
            Map<AlphaNodeFieldConstraint, Integer> analyzedAlphaConstraintsPerObjectType = analyzedAlphaConstraints.computeIfAbsent(objectType, key -> new HashMap<AlphaNodeFieldConstraint, Integer>());
            pattern.getConstraints()
                   .stream()
                   .filter(AlphaNodeFieldConstraint.class::isInstance)
                   .map(AlphaNodeFieldConstraint.class::cast)
                   .forEach(constraint -> analyzedAlphaConstraintsPerObjectType.merge(constraint, 1, (count, newValue) -> count + 1));
        }

        logger.trace("analyzedAlphaConstraints : {}", analyzedAlphaConstraints);
    }

    private Set<Rule> collectRules(Map<String, InternalKnowledgePackage> pkgs, Collection<InternalKnowledgePackage> newPkgs) {
        Set<Rule> ruleSet = new HashSet<>();
        pkgs.forEach((pkgName, pkg) -> ruleSet.addAll(pkg.getRules()));
        newPkgs.forEach(pkg -> ruleSet.addAll(pkg.getRules())); // okay to overwrite
        return ruleSet;
    }

    private void collectPatterns(GroupElement ge, List<Pattern> patternList) {
        List<RuleConditionElement> children = ge.getChildren();
        for (RuleConditionElement child : children) {
            if (child instanceof Pattern) {
                Pattern pattern = (Pattern) child;
                patternList.add(pattern);
                PatternSource source = pattern.getSource();
                if (source instanceof Accumulate) {
                    RuleConditionElement accSource = ((Accumulate) source).getSource();
                    if (accSource instanceof Pattern) {
                        patternList.add((Pattern) accSource);
                    }
                } else if (source instanceof Collect) {
                    patternList.add(((Collect) source).getSourcePattern());
                } else {
                    // do nothing for null, From, EntryPointId, WindowReference
                }
            } else if (child instanceof GroupElement) {
                collectPatterns((GroupElement) child, patternList);
            } else if (child instanceof Forall) {
                Forall forall = (Forall) child;
                patternList.add(forall.getBasePattern());
                patternList.addAll(forall.getRemainingPatterns());
            } else {
                // do nothing for null, EvalCondition, ConditionalBranch, QueryElement, NamedConsequence
            }
        }
    }

    @Override
    public void reorderAlphaConstraints(List<AlphaNodeFieldConstraint> alphaConstraints, ObjectType objectType) {
        // greater usage count is earlier
        logger.trace("** before alphaConstraints : {}", alphaConstraints);
        Map<AlphaNodeFieldConstraint, Integer> analyzedAlphaConstraintsPerObjectType = analyzedAlphaConstraints.get(objectType);
        if (analyzedAlphaConstraintsPerObjectType == null) {
            logger.trace(" ** after alphaConstraints : {}", alphaConstraints);
            return;
        }
        alphaConstraints.sort((constraint1, constraint2) -> {
            Integer count1 = analyzedAlphaConstraintsPerObjectType.getOrDefault(constraint1, 1);
            Integer count2 = analyzedAlphaConstraintsPerObjectType.getOrDefault(constraint2, 1);
            return count2.compareTo(count1);
        });
        logger.trace(" ** after alphaConstraints : {}", alphaConstraints);
    }

}
