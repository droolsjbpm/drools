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

package org.drools.core.rule;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.drools.core.WorkingMemory;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.reteoo.AccumulateNode;
import org.drools.core.reteoo.AccumulateNode.GroupByContext;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.RightTuple;
import org.drools.core.reteoo.RuleTerminalNode;
import org.drools.core.spi.Accumulator;
import org.drools.core.spi.Tuple;
import org.drools.core.util.index.TupleList;

/**
 * A class to represent the Accumulate CE
 */
public abstract class Accumulate extends ConditionalElement
    implements
    PatternSource {

    private static final long    serialVersionUID = 510l;

    protected RuleConditionElement source;
    protected Declaration[]        requiredDeclarations;
    protected Declaration[]        innerDeclarationCache;

    protected List<Accumulate>     cloned           = Collections.emptyList();

    public Accumulate() { }

    public Accumulate(final RuleConditionElement source,
                      final Declaration[] requiredDeclarations) {

        this.source = source;
        this.requiredDeclarations = requiredDeclarations;
        initInnerDeclarationCache();
    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        source = (RuleConditionElement) in.readObject();
        requiredDeclarations = (Declaration[]) in.readObject();
        this.cloned = (List<Accumulate>) in.readObject();
        initInnerDeclarationCache();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( this.source );
        out.writeObject( this.requiredDeclarations );
        out.writeObject( this.cloned );
    }

    public abstract Accumulator[] getAccumulators();

    public abstract Object createFunctionContext();

    /**
     * Executes the initialization block of code
     */
    public abstract Object init(final Object workingMemoryContext,
                                final Object accContext,
                                Object funcContext, final Tuple leftTuple,
                                final WorkingMemory workingMemory);

    /**
     * Executes the accumulate (action) code for the given fact handle
     */
    public abstract Object accumulate(final Object workingMemoryContext,
                                      final Object context,
                                      final Tuple match,
                                      final InternalFactHandle handle,
                                      final WorkingMemory workingMemory);

    /**
     * Executes the reverse (action) code for the given fact handle
     */
    public abstract boolean tryReverse(final Object workingMemoryContext,
                                       final Object context,
                                       final Tuple leftTuple,
                                       final InternalFactHandle handle,
                                       final RightTuple rightParent,
                                       final LeftTuple match,
                                       final WorkingMemory workingMemory);

    /**
     * Gets the result of the accumulation
     */
    public abstract Object getResult(final Object workingMemoryContext,
                                     final Object context,
                                     final Tuple leftTuple,
                                     final WorkingMemory workingMemory);

    /**
     * Returns true if this accumulate supports reverse
     */
    public abstract boolean supportsReverse();

    public abstract Accumulate clone();

    protected void registerClone(Accumulate clone) {
        if ( this.cloned == Collections.EMPTY_LIST ) {
            this.cloned = new ArrayList<>( 1 );
        }
        this.cloned.add( clone );
    }

    public RuleConditionElement getSource() {
        return this.source;
    }

    public Map<String, Declaration> getInnerDeclarations() {
        return this.source.getInnerDeclarations();
    }

    public Map<String, Declaration> getOuterDeclarations() {
        return Collections.emptyMap();
    }
    
    /**
     * @inheritDoc
     */
    public Declaration resolveDeclaration(final String identifier) {
        return this.source.getInnerDeclarations().get( identifier );
    }

    public abstract Object createWorkingMemoryContext();

    public List<RuleConditionElement> getNestedElements() {
        return Collections.singletonList( this.source );
    }

    public boolean isPatternScopeDelimiter() {
        return true;
    }

    public abstract boolean isMultiFunction();

    public void replaceDeclaration(Declaration declaration,
                                   Declaration resolved) {
        for ( int i = 0; i < this.requiredDeclarations.length; i++ ) {
            if ( this.requiredDeclarations[i].equals( declaration ) ) {
                this.requiredDeclarations[i] = resolved;
            }
        }
        replaceAccumulatorDeclaration(declaration, resolved);
    }

    public abstract void replaceAccumulatorDeclaration(Declaration declaration,
                                                          Declaration resolved);
    
    protected Declaration[] getInnerDeclarationCache() {
        return this.innerDeclarationCache;
    }

    private void initInnerDeclarationCache() {
        Map<String, Declaration> innerDeclarations = this.source.getInnerDeclarations();
        this.innerDeclarationCache = innerDeclarations.values().toArray( new Declaration[innerDeclarations.size()] );
        Arrays.sort( this.innerDeclarationCache, RuleTerminalNode.SortDeclarations.instance );
    }

    public Declaration[] getRequiredDeclarations() {
        return requiredDeclarations;
    }

    public boolean hasRequiredDeclarations() {
        return requiredDeclarations != null && requiredDeclarations.length > 0;
    }

    @Override
    public boolean requiresLeftActivation() {
        return true;
    }

    public boolean isGroupBy() {
        return false;
    }

    public abstract Object accumulate(Object workingMemoryContext, Tuple match, InternalFactHandle childHandle,
                                      GroupByContext groupByContext, TupleList<AccumulateNode.AccumulateContextEntry> tupleList, WorkingMemory wm);
}
