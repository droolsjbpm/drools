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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.drools.core.WorkingMemory;
import org.drools.core.common.NetworkNode;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.LeftTupleSink;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.NodeTypeEnums;
import org.drools.core.reteoo.ObjectSink;
import org.drools.core.reteoo.ObjectSource;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.QueryTerminalNode;
import org.drools.core.reteoo.Rete;
import org.drools.core.reteoo.RuleTerminalNode;
import org.kie.api.runtime.KieSession;

public class SessionInspector {

    private StatefulKnowledgeSessionImpl   session;
    private Map<Short, NetworkNodeVisitor> visitors;

    // default initializer
    {
        this.visitors = new HashMap<Short, NetworkNodeVisitor>();

        // terminal nodes
        this.visitors.put( NodeTypeEnums.RuleTerminalNode,
                           RuleTerminalNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.QueryTerminalNode,
                           QueryTerminalNodeVisitor.INSTANCE );

        // root node
        this.visitors.put( NodeTypeEnums.ReteNode,
                           DefaultNetworkNodeVisitor.INSTANCE );

        // object source nodes
        this.visitors.put( NodeTypeEnums.EntryPointNode,
                           DefaultNetworkNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.ObjectTypeNode,
                           ObjectTypeNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.AlphaNode,
                           AlphaNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.RightInputAdaterNode,
                           RightInputAdapterNodeVisitor.INSTANCE );

        // left tuple source nodes
        this.visitors.put( NodeTypeEnums.JoinNode,
                           BetaNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.BetaNode,
                           BetaNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.ExistsNode,
                           BetaNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.NotNode,
                           BetaNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.AccumulateNode,
                           AccumulateNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.EvalConditionNode,
                           EvalConditionNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.FromNode,
                           FromNodeVisitor.INSTANCE );
        this.visitors.put( NodeTypeEnums.LeftInputAdapterNode,
                           LeftInputAdapterNodeVisitor.INSTANCE );
    }
    
    public SessionInspector(KieSession session) {
        this.session = (StatefulKnowledgeSessionImpl) session;
    }

    public SessionInspector(WorkingMemory session) {
        this.session = (StatefulKnowledgeSessionImpl) session;
    }

    public StatefulKnowledgeSessionInfo getSessionInfo() {
        StatefulKnowledgeSessionInfo info = new StatefulKnowledgeSessionInfo();

        info.setSession( session );

        Stack<NetworkNode> nodeStack = new Stack<NetworkNode>();
        gatherNodeInfo( session.getKnowledgeBase().getRete(),
                        nodeStack,
                        info );

        return info;
    }

    private void gatherNodeInfo(NetworkNode parent,
                                Stack<NetworkNode> nodeStack,
                                StatefulKnowledgeSessionInfo info) {
        if ( !info.visited( parent ) ) {
            nodeStack.push( parent );
            NetworkNodeVisitor visitor = visitors.get( parent.getType() );
            if ( visitor != null ) {
                visitor.visit( parent,
                               nodeStack,
                               info );
            } else {
                throw new RuntimeException( "No visitor found for node class: " + parent.getClass()+" node: "+parent );
            }
            visitChildren( parent,
                           nodeStack,
                           info );
            nodeStack.pop();
        } else {
            // if already visited, then assign the same rules to the nodes currently in the stack
            Set<RuleImpl> rules = info.getNodeInfo( parent ).getRules();
            for ( NetworkNode snode : nodeStack ) {
                for ( RuleImpl rule : rules ) {
                    info.assign( snode,
                                 rule );
                }
            }
        }
    }

    protected void visitChildren(NetworkNode parent,
                                 Stack<NetworkNode> nodeStack,
                                 StatefulKnowledgeSessionInfo info) {
        if ( parent instanceof Rete ) {
            Rete rete = (Rete) parent;
            for ( EntryPointNode sink : rete.getEntryPointNodes().values() ) {
                gatherNodeInfo( sink,
                                nodeStack,
                                info );
            }
        } else if ( parent instanceof EntryPointNode ) {
            EntryPointNode epn = (EntryPointNode) parent;
            for ( ObjectTypeNode sink : epn.getObjectTypeNodes().values() ) {
                gatherNodeInfo( sink,
                                nodeStack,
                                info );
            }
        } else if ( parent instanceof ObjectSource ) {
            ObjectSource source = (ObjectSource) parent;
            for ( ObjectSink sink : source.getObjectSinkPropagator().getSinks() ) {
                gatherNodeInfo( sink,
                                nodeStack,
                                info );
            }
        } else if ( parent instanceof LeftTupleSource ) {
            LeftTupleSource source = (LeftTupleSource) parent;
            for ( LeftTupleSink sink : source.getSinkPropagator().getSinks() ) {
                gatherNodeInfo( sink,
                                nodeStack,
                                info );
            }
        } else if ( parent instanceof RuleTerminalNode || parent instanceof QueryTerminalNode ) {
            // no children to visit
        } else {
            // did we forget any node type?
            throw new RuntimeException( "ERROR: No idea how to visit childrens of this node: " + parent );
        }
    }
}
