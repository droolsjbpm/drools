/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.reteoo.builder;

import org.drools.core.rule.RuleConditionElement;

/**
 * An interface for Reteoo Component builders
 */
public interface ReteooComponentBuilder {

    /**
     * Builds and attach if needed the given RuleConditionalElement
     * 
     * @param context current build context
     * @param rce 
     */
    public void build(BuildContext context,
                      BuildUtils utils,
                      RuleConditionElement rce);

    /**
     * A boolean function that indicates if the builder requires a previous left 
     * (tuple) activation in order to corretly build the given component.
     * 
     * In other words, if it returns true and no previous TupleSource is already created,
     * an InitialFact pattern must be added with appropriate left input adapter for 
     * the network to be correctly built.
     * 
     * For instance, NOT / EXISTS / ACCUMULATE are examples of builders that must return true 
     * for this method, while PATTERN must return false.
     * 
     * @param rce the element to be built
     * 
     * @return true if a tuple source is required, false otherwise.
     */
    public boolean requiresLeftActivation(BuildUtils utils,
                                          RuleConditionElement rce);

}
