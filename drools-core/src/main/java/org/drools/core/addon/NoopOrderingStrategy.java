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

public class NoopOrderingStrategy implements AlphaNodeOrderingStrategy {

    @Override
    public void analyzeAlphaConstraints(Map<String, InternalKnowledgePackage> pkgs, Collection<InternalKnowledgePackage> newPkgs) {
        // no op
    }

    @Override
    public void reorderAlphaConstraints(List<AlphaNodeFieldConstraint> alphaConstraints, ObjectType objectType) {
        // no op
    }
}
