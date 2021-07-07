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

package org.drools.core.reteoo.builder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.drools.core.common.BaseNode;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.NetworkNode;
import org.drools.core.common.RuleBasePartitionId;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.reteoo.KieComponentFactory;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.ObjectSource;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.PathEndNode;
import org.drools.core.rule.EntryPointId;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.Pattern;
import org.drools.core.rule.RuleConditionElement;
import org.drools.core.rule.constraint.XpathConstraint;
import org.drools.core.spi.AlphaNodeFieldConstraint;
import org.drools.core.spi.BetaNodeFieldConstraint;
import org.drools.core.spi.RuleComponent;
import org.drools.core.time.TemporalDependencyMatrix;

import static org.drools.core.rule.TypeDeclaration.NEVER_EXPIRES;

/**
 * A build context for Reteoo Builder
 */
public class BuildContext {

    // tuple source to attach next node to
    private LeftTupleSource tupleSource;
    // object source to attach next node to
    private ObjectSource objectSource;
    // object type cache to check for cross products
    private List<Pattern> patterns;

    // rule base to add rules to
    private final InternalKnowledgeBase kBase;
    // rule being added at this moment
    private RuleImpl rule;
    private GroupElement subRule;
    // the rule component being processed at the moment
    private final Deque<RuleComponent> ruleComponent = new ArrayDeque<>();
    // a build stack to track nested elements
    private Deque<RuleConditionElement> buildstack;
    // beta constraints from the last pattern attached
    private List<BetaNodeFieldConstraint>    betaconstraints;
    // alpha constraints from the last pattern attached
    private List<AlphaNodeFieldConstraint>   alphaConstraints;
    // xpath constraints from the last pattern attached
    private List<XpathConstraint>            xpathConstraints;

    // the current entry point
    private EntryPointId                     currentEntryPoint;
    private boolean                          tupleMemoryEnabled;
    private boolean                          objectTypeNodeMemoryEnabled;
    private boolean                          query;

    private final List<PathEndNode>          pathEndNodes = new ArrayList<>();

    /**
     * Stores the list of nodes being added that require partitionIds
     */
    private List<BaseNode> nodes = new ArrayList<>();
    /**
     * Stores the id of the partition this rule will be added to
     */
    private RuleBasePartitionId              partitionId;
    /**
     * the calculate temporal distance matrix
     */
    private TemporalDependencyMatrix         temporal;
    private ObjectTypeNode                   rootObjectTypeNode;
    private Pattern[]                        lastBuiltPatterns;
    // The reason why this is here is because forall can inject a
    //  "this == " + BASE_IDENTIFIER $__forallBaseIdentifier
    // Which we don't want to actually count in the case of forall node linking    
    private boolean                          emptyForAllBetaConstraints;
    private boolean                          attachPQN;
    private boolean                          terminated;

    private final KieComponentFactory        componentFactory;

    private String                           consequenceName;

    public BuildContext(final InternalKnowledgeBase kBase) {
        this.kBase = kBase;
        this.tupleMemoryEnabled = true;
        this.objectTypeNodeMemoryEnabled = true;
        this.currentEntryPoint = EntryPointId.DEFAULT;
        this.attachPQN = true;
        this.componentFactory = kBase.getConfiguration().getComponentFactory();
        this.emptyForAllBetaConstraints = false;
    }

    public boolean isEmptyForAllBetaConstraints() {
        return emptyForAllBetaConstraints;
    }

    void setEmptyForAllBetaConstraints() {
        this.emptyForAllBetaConstraints = true;
    }

    public void syncObjectTypesWithObjectCount() {
        if (this.patterns == null) {
            return;
        }
        if (tupleSource != null) {
            while (this.patterns.size() > tupleSource.getObjectCount()) {
                this.patterns.remove(this.patterns.size()-1);
            }
        }
    }

    /**
     * @return the objectSource
     */
    public ObjectSource getObjectSource() {
        return this.objectSource;
    }

    /**
     * @param objectSource the objectSource to set
     */
    public void setObjectSource(final ObjectSource objectSource) {
        this.objectSource = objectSource;
    }

    public List<Pattern> getPatterns() {
        return this.patterns == null ? Collections.emptyList() : this.patterns;
    }

    public void addPattern(Pattern pattern) {
        if (this.patterns == null) {
            this.patterns = new ArrayList<>();
        }
        this.patterns.add( pattern );
    }

    /**
     * @return the tupleSource
     */
    public LeftTupleSource getTupleSource() {
        return this.tupleSource;
    }

    /**
     * @param tupleSource the tupleSource to set
     */
    public void setTupleSource(final LeftTupleSource tupleSource) {
        this.tupleSource = tupleSource;
    }

    /**
     * Returns context rulebase
     */
    public InternalKnowledgeBase getKnowledgeBase() {
        return this.kBase;
    }

    /**
     * Return the array of working memories associated with the given
     * rulebase.
     */
    public Collection<InternalWorkingMemory> getWorkingMemories() {
        return this.kBase.getWorkingMemories();
    }

    /**
     * Returns an Id for the next node
     */
    public int getNextId() {
        return kBase.getReteooBuilder().getIdGenerator().getNextId();
    }

    public int getNextId(String topic) {
        return kBase.getReteooBuilder().getIdGenerator().getNextId(topic);
    }

    /**
     * Method used to undo previous id assignment
     */
    public void releaseId(NetworkNode node) {
        kBase.getReteooBuilder().getIdGenerator().releaseId(node);
    }

    /**
     * Adds the rce to the build stack
     */
    public void push(final RuleConditionElement rce) {
        if (this.buildstack == null) {
            this.buildstack = new ArrayDeque<>();
        }
        this.buildstack.addLast(rce);
    }

    /**
     * Removes the top stack element
     */
    public RuleConditionElement pop() {
        return this.buildstack.removeLast();
    }

    /**
     * Returns the top stack element without removing it
     */
    public RuleConditionElement peek() {
        return this.buildstack.getLast();
    }

    public Collection<RuleConditionElement> getBuildstack() {
        return this.buildstack == null ? Collections.emptyList() : buildstack;
    }

    public List<BetaNodeFieldConstraint> getBetaconstraints() {
        return this.betaconstraints;
    }

    public void setBetaconstraints(final List<BetaNodeFieldConstraint> betaconstraints) {
        this.betaconstraints = betaconstraints;
    }

    public List<AlphaNodeFieldConstraint> getAlphaConstraints() {
        return alphaConstraints;
    }

    void setAlphaConstraints(List<AlphaNodeFieldConstraint> alphaConstraints) {
        this.alphaConstraints = alphaConstraints;
    }

    List<XpathConstraint> getXpathConstraints() {
        return xpathConstraints;
    }

    List<PathEndNode> getPathEndNodes() {
        return pathEndNodes;
    }

    public void addPathEndNode(PathEndNode node) {
        pathEndNodes.add(node);
    }

    void setXpathConstraints(List<XpathConstraint> xpathConstraints) {
        this.xpathConstraints = xpathConstraints;
    }

    public boolean isTupleMemoryEnabled() {
        return this.tupleMemoryEnabled;
    }

    public void setTupleMemoryEnabled(boolean hasLeftMemory) {
        this.tupleMemoryEnabled = hasLeftMemory;
    }

    public boolean isObjectTypeNodeMemoryEnabled() {
        return objectTypeNodeMemoryEnabled;
    }

    public void setObjectTypeNodeMemoryEnabled(boolean hasObjectTypeMemory) {
        this.objectTypeNodeMemoryEnabled = hasObjectTypeMemory;
    }

    public boolean isQuery() {
        return query;
    }

    /**
     * @return the currentEntryPoint
     */
    public EntryPointId getCurrentEntryPoint() {
        return currentEntryPoint;
    }

    /**
     * @param currentEntryPoint the currentEntryPoint to set
     */
    public void setCurrentEntryPoint(EntryPointId currentEntryPoint) {
        this.currentEntryPoint = currentEntryPoint;
    }

    /**
     * @return the nodes
     */
    public List<BaseNode> getNodes() {
        return nodes;
    }

    BaseNode getLastNode() {
        return nodes.get(nodes.size()-1);
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(List<BaseNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * @return the partitionId
     */
    public RuleBasePartitionId getPartitionId() {
        return partitionId;
    }

    /**
     * @param partitionId the partitionId to set
     */
    public void setPartitionId(RuleBasePartitionId partitionId) {
        this.partitionId = partitionId;
    }

    public boolean isStreamMode() {
        // eager rules don't need to use the event queue
        return this.temporal != null && !rule.isEager();
    }

    public long getExpirationOffset(Pattern pattern) {
        return temporal != null ? temporal.getExpirationOffset( pattern ) : NEVER_EXPIRES;
    }

    void setTemporalDistance(TemporalDependencyMatrix temporal) {
        this.temporal = temporal;
    }

    public RuleImpl getRule() {
        return rule;
    }

    public void setRule(RuleImpl rule) {
        this.rule = rule;
        if (rule.isQuery()) {
            this.query = true;
        }
    }

    public GroupElement getSubRule() {
        return subRule;
    }

    void setSubRule(GroupElement subRule) {
        this.subRule = subRule;
    }

    /**
     * Removes the top element from the rule component stack.
     * The rule component stack is used to add trackability to
     * the ReteOO nodes so that they can be linked to the rule
     * components that originated them.
     */
    public RuleComponent popRuleComponent() {
        return this.ruleComponent.pop();
    }

    /**
     * Peeks at the top element from the rule component stack.
     * The rule component stack is used to add trackability to
     * the ReteOO nodes so that they can be linked to the rule
     * components that originated them.
     */
    public RuleComponent peekRuleComponent() {
        return this.ruleComponent.isEmpty() ? null : this.ruleComponent.peek();
    }

    /**
     * Adds the ruleComponent to the top of the rule component stack.
     * The rule component stack is used to add trackability to
     * the ReteOO nodes so that they can be linked to the rule
     * components that originated them.
     */
    public void pushRuleComponent(RuleComponent ruleComponent) {
        this.ruleComponent.push(ruleComponent);
    }

    public ObjectTypeNode getRootObjectTypeNode() {
        return rootObjectTypeNode;
    }

    public void setRootObjectTypeNode(ObjectTypeNode source) {
        rootObjectTypeNode = source;
    }

    public Pattern[] getLastBuiltPatterns() {
        return lastBuiltPatterns;
    }

    public void setLastBuiltPattern(Pattern lastBuiltPattern) {
        if (this.lastBuiltPatterns == null) {
            this.lastBuiltPatterns = new Pattern[]{lastBuiltPattern, null};
        } else {
            this.lastBuiltPatterns[1] = this.lastBuiltPatterns[0];
            this.lastBuiltPatterns[0] = lastBuiltPattern;
        }
    }

    boolean isAttachPQN() {
        return attachPQN;
    }

    void setAttachPQN(final boolean attachPQN) {
        this.attachPQN = attachPQN;
    }

    public KieComponentFactory getComponentFactory() {
        return componentFactory;
    }

    boolean isTerminated() {
        return terminated;
    }

    void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public String getConsequenceName() {
        return consequenceName;
    }

    public void setConsequenceName( String consequenceName ) {
        this.consequenceName = consequenceName;
    }
}
