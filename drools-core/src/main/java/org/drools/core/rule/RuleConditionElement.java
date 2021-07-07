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

package org.drools.core.rule;

import org.drools.core.spi.RuleComponent;

import java.io.Externalizable;
import java.util.List;
import java.util.Map;

public interface RuleConditionElement
    extends
    RuleComponent,
    Externalizable,
    Cloneable {

    /**
     * Returns a Map of declarations that are
     * visible inside this conditional element
     * 
     * @return
     */
    Map<String,Declaration> getInnerDeclarations();

    /**
     * Returns a Map of declarations that are visible
     * outside this conditional element. 
     * 
     * @return
     */
    Map<String,Declaration> getOuterDeclarations();

    /**
     * Resolves the given identifier in the current scope and
     * returns the Declaration object for the declaration.
     * Returns null if identifier can not be resolved.
     *  
     * @param identifier
     * @return
     */
    Declaration resolveDeclaration(String identifier);

    /**
     * Returns a clone from itself
     * @return
     */
    RuleConditionElement clone();
    
    /**
     * Returs a list of RuleConditionElement's that are nested
     * inside the current element
     * @return
     */
    List<? extends RuleConditionElement> getNestedElements();
    
    /**
     * Returns true in case this RuleConditionElement delimits
     * a pattern visibility scope.
     * 
     * For instance, AND CE is not a scope delimiter, while 
     * NOT CE is a scope delimiter
     * @return
     */
    boolean isPatternScopeDelimiter();

}
