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

package org.drools.core.factmodel.traits;

import org.drools.core.base.TraitHelper;
import org.drools.core.common.InternalWorkingMemoryActions;
import org.drools.core.common.InternalWorkingMemoryEntryPoint;
import org.drools.core.factmodel.ClassBuilder;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.spi.ObjectType;

public interface TraitCoreService {
    TraitRegistry createRegistry();

    TraitFactory createTraitFactory();

    ClassBuilder createTraitProxyClassBuilder();

    ClassBuilder createPropertyWrapperBuilder();

    TraitHelper createTraitHelper();

    TraitHelper createTraitHelper(InternalWorkingMemoryActions workingMemory, InternalWorkingMemoryEntryPoint nep );

    Class<?> baseTraitProxyClass();

    ObjectTypeNode createTraitObjectTypeNode(int id, EntryPointNode source, ObjectType objectType, BuildContext context );
}