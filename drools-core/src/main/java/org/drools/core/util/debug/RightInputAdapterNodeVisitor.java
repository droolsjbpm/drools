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

import org.drools.core.common.Memory;
import org.drools.core.common.NetworkNode;
import org.drools.core.reteoo.AccumulateNode.AccumulateMemory;
import org.drools.core.reteoo.BetaMemory;
import org.drools.core.reteoo.BetaNode;
import org.drools.core.reteoo.NodeTypeEnums;
import org.drools.core.reteoo.RightInputAdapterNode;

import java.util.Stack;

public class RightInputAdapterNodeVisitor extends AbstractNetworkNodeVisitor {
    
    public static final RightInputAdapterNodeVisitor INSTANCE = new RightInputAdapterNodeVisitor();
    
    protected RightInputAdapterNodeVisitor() {
    }

    @Override
    protected void doVisit(NetworkNode node,
                           Stack<NetworkNode> nodeStack,
                           StatefulKnowledgeSessionInfo info) {
        RightInputAdapterNode an = (RightInputAdapterNode) node;
        DefaultNodeInfo ni = info.getNodeInfo( node );

        BetaNode betaNode = (BetaNode) an.getObjectSinkPropagator().getSinks()[0];

        Memory childMemory = info.getSession().getNodeMemory( betaNode );

        BetaMemory bm;
        if ( betaNode.getType() == NodeTypeEnums.AccumulateNode ) {
            bm =  ((AccumulateMemory) childMemory).getBetaMemory();
        } else {
            bm =  (BetaMemory) childMemory;
        }

        ni.setMemoryEnabled( true );
        ni.setTupleMemorySize( bm.getRightTupleMemory().size() );
        ni.setCreatedFactHandles( bm.getRightTupleMemory().size() );
    }

}
