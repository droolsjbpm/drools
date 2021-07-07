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

package org.drools.core.reteoo;

import org.drools.core.common.MemoryFactory;
import org.drools.core.common.NetworkNode;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.GroupElement;
import org.drools.core.util.bitmask.BitMask;

/**
 * A markup interface for terminal nodes
 */
public interface TerminalNode
    extends
    NetworkNode, LeftTupleSinkNode, MemoryFactory<PathMemory> {
    
    LeftTupleSource getLeftTupleSource();
    
    LeftTupleSource unwrapTupleSource();
    
    void initInferredMask();

    BitMask getDeclaredMask();
    void setDeclaredMask(BitMask mask);

    BitMask getInferredMask();
    void setInferredMask(BitMask mask);

    BitMask getNegativeMask();
    void setNegativeMask(BitMask mask);
    
    RuleImpl getRule();

    GroupElement getSubRule();

    boolean isFireDirect();

    Declaration[] getAllDeclarations();
    Declaration[] getRequiredDeclarations();

    Declaration[] getSalienceDeclarations();

    boolean isTerminalNodeOf(LeftTupleNode node);
}
