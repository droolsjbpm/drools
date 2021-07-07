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

import java.util.Stack;

import org.drools.core.common.NetworkNode;
import org.drools.core.reteoo.AccumulateNode;
import org.drools.core.reteoo.AccumulateNode.AccumulateContext;
import org.drools.core.reteoo.AccumulateNode.AccumulateMemory;
import org.drools.core.reteoo.BetaNode;
import org.drools.core.spi.Tuple;
import org.drools.core.util.FastIterator;

public class AccumulateNodeVisitor extends AbstractNetworkNodeVisitor {
    
    public static final AccumulateNodeVisitor INSTANCE = new AccumulateNodeVisitor();
    
    protected AccumulateNodeVisitor() {
    }

    @Override
    protected void doVisit(NetworkNode node,
                           Stack<NetworkNode> nodeStack,
                           StatefulKnowledgeSessionInfo info) {
        AccumulateNode an = (AccumulateNode) node;
        DefaultNodeInfo ni = info.getNodeInfo( node );
        final AccumulateMemory memory = (AccumulateMemory) info.getSession().getNodeMemory( an );
        
        ni.setMemoryEnabled( true );
        
        if( an.isObjectMemoryEnabled() ) {
            ni.setFactMemorySize( memory.getBetaMemory().getRightTupleMemory().size() );
        }
        if( an.isLeftTupleMemoryEnabled() ) {
            ni.setTupleMemorySize( memory.getBetaMemory().getLeftTupleMemory().size() );
            FastIterator it =  memory.getBetaMemory().getLeftTupleMemory().fullFastIterator();
            
            int i = 0;
            for ( Tuple leftTuple = BetaNode.getFirstTuple( memory.getBetaMemory().getLeftTupleMemory(), it ); leftTuple != null; leftTuple = ( Tuple) it.next( leftTuple  )) {
                AccumulateContext ctx = (AccumulateContext) leftTuple.getContextObject();
                if ( ctx != null ) {
                    i++;
                }
            }
             
            ni.setCreatedFactHandles( i );
        }

    }

}
