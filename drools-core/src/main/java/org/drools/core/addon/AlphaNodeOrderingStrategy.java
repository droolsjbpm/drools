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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.spi.AlphaNodeFieldConstraint;
import org.drools.core.spi.ObjectType;
import org.drools.core.util.ClassUtils;
import org.kie.api.conf.AlphaNodeOrderingOption;

/**
 * 
 * Interface for alpha node ordering. Implement methods to analyze rules and reorder alpha constraints
 * in order to maximize alpha node sharing and minimize the number of evaluations.
 *
 */
public interface AlphaNodeOrderingStrategy {

    void analyzeAlphaConstraints(Map<String, InternalKnowledgePackage> pkgs, Collection<InternalKnowledgePackage> newPkgs);

    void reorderAlphaConstraints(List<AlphaNodeFieldConstraint> alphaConstraints, ObjectType objectType);

    static AlphaNodeOrderingStrategy createAlphaNodeOrderingStrategy(AlphaNodeOrderingOption option) {
        if (AlphaNodeOrderingOption.COUNT.equals(option)) {
            return new CountBasedOrderingStrategy();
        } else if (AlphaNodeOrderingOption.CUSTOM.equals(option)) {
            String customStrategyClassName = System.getProperty(AlphaNodeOrderingOption.CUSTOM_CLASS_PROPERTY_NAME);
            if (customStrategyClassName == null || customStrategyClassName.trim().isEmpty()) {
                throw new RuntimeException("Configure system property " + AlphaNodeOrderingOption.CUSTOM_CLASS_PROPERTY_NAME + " with custom strategy implementation FQCN when you use AlphaNodeOrderingOption.CUSTOM");
            } else {
                return (AlphaNodeOrderingStrategy) ClassUtils.instantiateObject(customStrategyClassName);
            }
        } else if (AlphaNodeOrderingOption.NONE.equals(option)) {
            return new NoopOrderingStrategy();
        } else {
            throw new IllegalArgumentException("No implementation found for AlphaNodeOrderingOption [" + option + "]");
        }
    }
}
