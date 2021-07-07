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

package org.drools.core.util.debug;

import org.drools.core.common.NetworkNode;
import org.drools.core.reteoo.FromNode;
import org.drools.core.reteoo.FromNode.FromMemory;
import org.drools.core.reteoo.LeftTuple;

import java.util.Stack;

public class FromNodeVisitor extends AbstractNetworkNodeVisitor {
    
    public static final FromNodeVisitor INSTANCE = new FromNodeVisitor();
    
    protected FromNodeVisitor() {
    }

    @Override
    protected void doVisit(NetworkNode node,
                           Stack<NetworkNode> nodeStack,
                           StatefulKnowledgeSessionInfo info) {
        FromNode fn = (FromNode) node;
        DefaultNodeInfo ni = info.getNodeInfo( node );
        final FromMemory memory = (FromMemory) info.getSession().getNodeMemory( fn );
        
        ni.setMemoryEnabled( true );
        
        if( fn.isLeftTupleMemoryEnabled() ) {
            ni.setTupleMemorySize( memory.getBetaMemory().getLeftTupleMemory().size() );

            long handles = 0;
            org.drools.core.util.Iterator it = memory.getBetaMemory().getLeftTupleMemory().iterator();
            for ( LeftTuple leftTuple = (LeftTuple) it.next(); leftTuple != null; leftTuple = (LeftTuple) it.next() ) {
                LeftTuple child = leftTuple.getFirstChild();
                while( child != null ) {
                    handles++;
                    child = child.getHandleNext();
                }
            }
            ni.setCreatedFactHandles( handles );
        } else {
            info.warn( "The left memory for this node is disabled, making it impossible to calculate the number of created handles" );
        }

    }

}
