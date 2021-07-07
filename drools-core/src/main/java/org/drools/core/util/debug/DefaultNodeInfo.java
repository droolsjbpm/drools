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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.drools.core.common.NetworkNode;
import org.drools.core.common.RuleBasePartitionId;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.LeftTupleSink;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.ObjectSink;
import org.drools.core.reteoo.ObjectSource;
import org.drools.core.reteoo.Rete;

public class DefaultNodeInfo
    implements
    NodeInfo {

    private NetworkNode node;
    private Set<RuleImpl>   rules;

    private boolean memoryEnabled = false;
    private long tupleMemorySize = -1;
    private long factMemorySize = -1;
    private long createdFactHandles = -1;
    private long actionQueueSize = -1;

    public DefaultNodeInfo(NetworkNode node) {
        this.node = node;
        this.rules = new HashSet<RuleImpl>();
    }

    public void assign(RuleImpl rule) {
        this.rules.add( rule );
    }

    public Set<RuleImpl> getRules() {
        return rules;
    }

    public int getId() {
        return node.getId();
    }

    public RuleBasePartitionId getPartitionId() {
        return node.getPartitionId();
    }

    public long getTupleMemorySize() {
        return tupleMemorySize;
    }

    public void setTupleMemorySize(long leftMemorySize) {
        this.tupleMemorySize = leftMemorySize;
    }

    public long getFactMemorySize() {
        return factMemorySize;
    }

    public void setFactMemorySize(long rightMemorySize) {
        this.factMemorySize = rightMemorySize;
    }

    public long getCreatedFactHandles() {
        return createdFactHandles;
    }

    public void setCreatedFactHandles(long createdFactHandles) {
        this.createdFactHandles = createdFactHandles;
    }

    public long getActionQueueSize() {
        return this.actionQueueSize;
    }
    
    public void setActionQueueSize(long size) {
        this.actionQueueSize = size;
    }

    public NetworkNode getNode() {
        return node;
    }

    public boolean isMemoryEnabled() {
        return memoryEnabled;
    }

    public void setMemoryEnabled(boolean memoryEnabled) {
        this.memoryEnabled = memoryEnabled;
    }
    
    public Collection<? extends NetworkNode> getSinkList() {
        if ( node instanceof Rete ) {
            Rete rete = (Rete) node;
            return  rete.getEntryPointNodes().values();
        } else if ( node instanceof EntryPointNode ) {
            EntryPointNode epn = (EntryPointNode) node;
            return epn.getObjectTypeNodes().values();
        } else if ( node instanceof ObjectSource ) {
            List<NetworkNode> result = new ArrayList<NetworkNode>();
            for ( ObjectSink sink : ((ObjectSource)node).getObjectSinkPropagator().getSinks() ) {
                result.add( (NetworkNode) sink );
            }
            return result;
        } else if ( node instanceof LeftTupleSource ) {
            List<NetworkNode> result = new ArrayList<NetworkNode>();
            LeftTupleSource source = (LeftTupleSource) node;
            for ( LeftTupleSink sink : source.getSinkPropagator().getSinks() ) {
                result.add( (NetworkNode) sink );
            }
            return result;
        }
        return Collections.emptyList();
    }

}
