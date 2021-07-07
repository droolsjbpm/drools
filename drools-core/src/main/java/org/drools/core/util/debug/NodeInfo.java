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

import java.util.Collection;
import java.util.Set;

import org.drools.core.common.NetworkNode;
import org.drools.core.common.RuleBasePartitionId;
import org.drools.core.definitions.rule.impl.RuleImpl;

public interface NodeInfo {

    /**
     * Returns this node ID
     * @return
     */
    public int getId();

    /**
     * Returns the partition ID this node belongs to
     * @return
     */
    public RuleBasePartitionId getPartitionId();

    /**
     * Returns the set of rules this node belongs to
     * @return
     */
    public Set<RuleImpl> getRules();

    /**
     * Returns the actual node
     * @return
     */
    public NetworkNode getNode();
    
    /**
     * Returns true if memory is enabled for this node
     * @return
     */
    public boolean isMemoryEnabled();

    /**
     * Returns the number of tuples in the tuple memory 
     * @return
     */
    public long getTupleMemorySize();

    /**
     * Returns the number of facts in the fact memory
     * @return
     */
    public long getFactMemorySize();

    /**
     * Returns the number of infered fact handles created in this node
     * @return
     */
    public long getCreatedFactHandles();
    
    public Collection<? extends NetworkNode> getSinkList();

}
