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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.base.ValueType;
import org.drools.core.common.BaseNode;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.NetworkNode;
import org.drools.core.rule.IndexableConstraint;
import org.drools.core.spi.AlphaNodeFieldConstraint;
import org.drools.core.spi.FieldValue;
import org.drools.core.spi.InternalReadAccessor;
import org.drools.core.spi.PropagationContext;
import org.drools.core.spi.ReadAccessor;
import org.drools.core.util.Entry;
import org.drools.core.util.FastIterator;
import org.drools.core.util.Iterator;
import org.drools.core.util.LinkedList;
import org.drools.core.util.LinkedListNode;
import org.drools.core.util.ObjectHashMap;
import org.drools.core.util.ObjectHashMap.ObjectEntry;
import org.drools.core.util.RBTree.Node;
import org.drools.core.util.index.AlphaIndexRBTree;
import org.drools.core.util.index.IndexUtil.ConstraintType;

public class CompositeObjectSinkAdapter implements ObjectSinkPropagator {

    //    /** You can override this property via a system property (eg -Ddrools.hashThreshold=4) */
    //    public static final String HASH_THRESHOLD_SYSTEM_PROPERTY = "drools.hashThreshold";
    //
    //    /** The threshold for when hashing kicks in */
    //    public static final int    THRESHOLD_TO_HASH              = Integer.parseInt( System.getProperty( HASH_THRESHOLD_SYSTEM_PROPERTY,
    //                                                                                                      "3" ) );

    public static final int RANGE_INDEX_THRESHOLD = 3; // TODO: make configurable Option
    public static final boolean RANGE_INDEX_ENABLED = Boolean.parseBoolean( System.getProperty( "drools.alphaNodeRangeIndexing.enabled", "true"));


    private static final long serialVersionUID = 510L;

    private ObjectSinkNodeList        otherSinks;
    private ObjectSinkNodeList        hashableSinks;
    private ObjectSinkNodeList        rangeIndexableAscSinks;
    private ObjectSinkNodeList        rangeIndexableDescSinks;


    private LinkedList<FieldIndex>    hashedFieldIndexes;
    private LinkedList<FieldIndex>    rangeIndexedAscFieldIndexes;
    private LinkedList<FieldIndex>    rangeIndexedDescFieldIndexes;


    private ObjectHashMap             hashedSinkMap;
    private Map<FieldIndex, AlphaIndexRBTree>          rangeIndexAscTreeMap;
    private Map<FieldIndex, AlphaIndexRBTree>          rangeIndexDescTreeMap;

    private int               alphaNodeHashingThreshold;

    private ObjectSink[]      sinks;

    private Map<NetworkNode, NetworkNode> sinksMap;

    public CompositeObjectSinkAdapter() {
        this( 3 );
    }

    public CompositeObjectSinkAdapter(final int alphaNodeHashingThreshold) {
        this.alphaNodeHashingThreshold = alphaNodeHashingThreshold;
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        otherSinks = (ObjectSinkNodeList) in.readObject();
        hashableSinks = (ObjectSinkNodeList) in.readObject();
        rangeIndexableAscSinks = (ObjectSinkNodeList) in.readObject();
        rangeIndexableDescSinks = (ObjectSinkNodeList) in.readObject();
        hashedFieldIndexes = (LinkedList) in.readObject();
        rangeIndexedAscFieldIndexes = (LinkedList) in.readObject();
        rangeIndexedDescFieldIndexes = (LinkedList) in.readObject();
        hashedSinkMap = (ObjectHashMap) in.readObject();
        rangeIndexAscTreeMap = (Map<FieldIndex, AlphaIndexRBTree>) in.readObject();
        rangeIndexDescTreeMap = (Map<FieldIndex, AlphaIndexRBTree>) in.readObject();
        alphaNodeHashingThreshold = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( otherSinks );
        out.writeObject( hashableSinks );
        out.writeObject( rangeIndexableAscSinks );
        out.writeObject( rangeIndexableDescSinks );
        out.writeObject( hashedFieldIndexes );
        out.writeObject( rangeIndexedAscFieldIndexes );
        out.writeObject( rangeIndexedDescFieldIndexes );
        out.writeObject( hashedSinkMap );
        out.writeObject( rangeIndexAscTreeMap );
        out.writeObject( rangeIndexDescTreeMap );
        out.writeInt( alphaNodeHashingThreshold );
    }

    public ObjectSinkNodeList getOthers() {
        return this.otherSinks;
    }

    public ObjectSinkNodeList getHashableSinks() {
        return this.hashableSinks;
    }

    public ObjectHashMap getHashedSinkMap() {
        return this.hashedSinkMap;
    }

    public ObjectSinkNodeList getRangeIndexableAscSinks() {
        return rangeIndexableAscSinks;
    }

    public ObjectSinkNodeList getRangeIndexableDescSinks() {
        return rangeIndexableDescSinks;
    }

    public Map<FieldIndex, AlphaIndexRBTree> getRangeIndexAscTreeMap() {
        return rangeIndexAscTreeMap;
    }

    public Map<FieldIndex, AlphaIndexRBTree> getRangeIndexDescTreeMap() {
        return rangeIndexDescTreeMap;
    }

    public ObjectSinkPropagator addObjectSink(ObjectSink sink) {
        return addObjectSink(sink, 0);
    }

    public ObjectSinkPropagator addObjectSink(ObjectSink sink, int alphaNodeHashingThreshold) {
        this.sinks = null; // dirty it, so it'll rebuild on next get
        if (this.sinksMap != null) {
            this.sinksMap.put( sink, sink );
        }
        if ( sink.getType() ==  NodeTypeEnums.AlphaNode ) {
            final AlphaNode alphaNode = (AlphaNode) sink;
            final InternalReadAccessor readAccessor = getHashableAccessor(alphaNode);

            // hash indexing
            if ( readAccessor != null ) {
                final int index = readAccessor.getIndex();
                final FieldIndex fieldIndex = registerFieldIndex( index, readAccessor );

                //DROOLS-678 : prevent null values from being hashed as 0s
                final FieldValue value = ((IndexableConstraint)alphaNode.getConstraint()).getField();
                if ( fieldIndex.getCount() >= this.alphaNodeHashingThreshold && this.alphaNodeHashingThreshold != 0 && ! value.isNull() ) {
                    if ( !fieldIndex.isHashed() ) {
                        hashSinks( fieldIndex );
                    }

                    // no need to check, we know  the sink  does not exist
                    this.hashedSinkMap.put( new HashKey( index,
                                                         value,
                                                         fieldIndex.getFieldExtractor() ),
                                            alphaNode,
                                            false );
                } else {
                    if ( this.hashableSinks == null ) {
                        this.hashableSinks = new ObjectSinkNodeList();
                    }
                    this.hashableSinks.add( alphaNode );
                }
                return this;
            }

            // range indexing
            if (isRangeIndexable(alphaNode)) {
                IndexableConstraint indexableConstraint = (IndexableConstraint) alphaNode.getConstraint();
                InternalReadAccessor internalReadAccessor = indexableConstraint.getFieldExtractor();
                final int index = internalReadAccessor.getIndex();

                ConstraintType constraintType = indexableConstraint.getConstraintType();
                if (constraintType.isAscending()) {
                    if (rangeIndexedAscFieldIndexes == null) {
                        rangeIndexedAscFieldIndexes = new LinkedList<>();
                    }
                    final FieldIndex fieldIndex = registerFieldIndexForRange(index, internalReadAccessor, rangeIndexedAscFieldIndexes);
                    final FieldValue value = indexableConstraint.getField();
                    if (fieldIndex.getCount() >= RANGE_INDEX_THRESHOLD && RANGE_INDEX_THRESHOLD != 0 && !value.isNull()) {
                        if (!fieldIndex.isRangeIndexed()) {
                            if (rangeIndexAscTreeMap == null) {
                                rangeIndexAscTreeMap = new HashMap<>();
                            }
                            rangeIndexSinks(fieldIndex, rangeIndexableAscSinks, rangeIndexAscTreeMap);
                            if (rangeIndexableAscSinks.isEmpty()) {
                                rangeIndexableAscSinks = null;
                            }
                        }
                        this.rangeIndexAscTreeMap.get(fieldIndex).add(alphaNode);
                    } else {
                        if (rangeIndexableAscSinks == null) {
                            rangeIndexableAscSinks = new ObjectSinkNodeList();
                        }
                        rangeIndexableAscSinks.add(alphaNode);
                    }
                } else { // Descending
                    if (rangeIndexedDescFieldIndexes == null) {
                        rangeIndexedDescFieldIndexes = new LinkedList<>();
                    }
                    final FieldIndex fieldIndex = registerFieldIndexForRange(index, internalReadAccessor, rangeIndexedDescFieldIndexes);
                    final FieldValue value = indexableConstraint.getField();
                    if (fieldIndex.getCount() >= RANGE_INDEX_THRESHOLD && RANGE_INDEX_THRESHOLD != 0 && !value.isNull()) {
                        if (!fieldIndex.isRangeIndexed()) {
                            if (rangeIndexDescTreeMap == null) {
                                rangeIndexDescTreeMap = new HashMap<>();
                            }
                            rangeIndexSinks(fieldIndex, rangeIndexableDescSinks, rangeIndexDescTreeMap);
                            if (rangeIndexableDescSinks.isEmpty()) {
                                rangeIndexableDescSinks = null;
                            }
                        }
                        this.rangeIndexDescTreeMap.get(fieldIndex).add(alphaNode);
                    } else {
                        if (rangeIndexableDescSinks == null) {
                            rangeIndexableDescSinks = new ObjectSinkNodeList();
                        }
                        rangeIndexableDescSinks.add(alphaNode);
                    }
                }
                return this;
            }
        }

        if ( this.otherSinks == null ) {
            this.otherSinks = new ObjectSinkNodeList();
        }

        this.otherSinks.add( (ObjectSinkNode) sink );
        return this;
    }

    static InternalReadAccessor getHashableAccessor(AlphaNode alphaNode) {
        AlphaNodeFieldConstraint fieldConstraint = alphaNode.getConstraint();
        if ( fieldConstraint instanceof IndexableConstraint ) {
            IndexableConstraint indexableConstraint = (IndexableConstraint) fieldConstraint;
            if ( isHashable( indexableConstraint ) ) {
                return indexableConstraint.getFieldExtractor();
            }
        }
        return null;
    }

    private static boolean isHashable( IndexableConstraint indexableConstraint ) {
        return indexableConstraint.getConstraintType() == ConstraintType.EQUAL && indexableConstraint.getField() != null &&
                indexableConstraint.getFieldExtractor().getValueType() != ValueType.OBJECT_TYPE &&
                // our current implementation does not support hashing of deeply nested properties
                indexableConstraint.getFieldExtractor().getIndex() >= 0;
    }

    public ObjectSinkPropagator removeObjectSink(final ObjectSink sink) {
        this.sinks = null; // dirty it, so it'll rebuild on next get
        if (this.sinksMap != null) {
            this.sinksMap.remove( sink );
        }
        if ( sink.getType() ==  NodeTypeEnums.AlphaNode ) {
            final AlphaNode alphaNode = (AlphaNode) sink;
            final AlphaNodeFieldConstraint fieldConstraint = alphaNode.getConstraint();

            if ( fieldConstraint instanceof IndexableConstraint ) {
                final IndexableConstraint indexableConstraint = (IndexableConstraint) fieldConstraint;
                final FieldValue value = indexableConstraint.getField();

                // hash index
                if ( isHashable( indexableConstraint ) ) {
                    final InternalReadAccessor fieldAccessor = indexableConstraint.getFieldExtractor();
                    final int index = fieldAccessor.getIndex();
                    final FieldIndex fieldIndex = unregisterFieldIndex( index );

                    if ( fieldIndex != null && fieldIndex.isHashed() ) {
                        HashKey hashKey = new HashKey( index,
                                                       value,
                                                       fieldAccessor );
                        this.hashedSinkMap.remove( hashKey );
                        if ( fieldIndex.getCount() <= this.alphaNodeHashingThreshold - 1 ) {
                            // we have less than three so unhash
                            unHashSinks( fieldIndex );
                        }
                    } else {
                        this.hashableSinks.remove( alphaNode );
                    }

                    if ( this.hashableSinks != null && this.hashableSinks.isEmpty() ) {
                        this.hashableSinks = null;
                    }

                    return size() == 1 ? new SingleObjectSinkAdapter( getSinks()[0] ) : this;
                }

                // range index
                if (isRangeIndexable(alphaNode)) {
                    final InternalReadAccessor fieldAccessor = indexableConstraint.getFieldExtractor();
                    final int index = fieldAccessor.getIndex();
                    ConstraintType constraintType = indexableConstraint.getConstraintType();
                    if (constraintType.isAscending()) {
                        final FieldIndex fieldIndex = unregisterFieldIndexForRange(index, rangeIndexedAscFieldIndexes);
                        if (rangeIndexedAscFieldIndexes.isEmpty()) {
                            rangeIndexedAscFieldIndexes = null;
                        }
                        if (fieldIndex.isRangeIndexed()) {
                            AlphaIndexRBTree tree = this.rangeIndexAscTreeMap.get(fieldIndex);
                            tree.remove(alphaNode);
                            if (fieldIndex.getCount() <= RANGE_INDEX_THRESHOLD - 1) {
                                // we have less than THRESHOLD so unindex
                                if (rangeIndexableAscSinks == null) {
                                    rangeIndexableAscSinks = new ObjectSinkNodeList();
                                }
                                unRangeIndexSinks(fieldIndex, rangeIndexableAscSinks, tree, rangeIndexAscTreeMap);
                                if (rangeIndexAscTreeMap.isEmpty()) {
                                    rangeIndexAscTreeMap = null;
                                }
                            }
                        } else {
                            this.rangeIndexableAscSinks.remove(alphaNode);
                        }

                        if (this.rangeIndexableAscSinks != null && this.rangeIndexableAscSinks.isEmpty()) {
                            this.rangeIndexableAscSinks = null;
                        }

                        return size() == 1 ? new SingleObjectSinkAdapter(getSinks()[0]) : this;
                    } else { // Descending
                        final FieldIndex fieldIndex = unregisterFieldIndexForRange(index, rangeIndexedDescFieldIndexes);
                        if (rangeIndexedDescFieldIndexes.isEmpty()) {
                            rangeIndexedDescFieldIndexes = null;
                        }
                        if (fieldIndex.isRangeIndexed()) {
                            AlphaIndexRBTree tree = this.rangeIndexDescTreeMap.get(fieldIndex);
                            tree.remove(alphaNode);
                            if (fieldIndex.getCount() <= RANGE_INDEX_THRESHOLD - 1) {
                                // we have less than THRESHOLD so unindex
                                if (rangeIndexableDescSinks == null) {
                                    rangeIndexableDescSinks = new ObjectSinkNodeList();
                                }
                                unRangeIndexSinks(fieldIndex, rangeIndexableDescSinks, tree, rangeIndexDescTreeMap);
                                if (rangeIndexDescTreeMap.isEmpty()) {
                                    rangeIndexDescTreeMap = null;
                                }
                            }
                        } else {
                            this.rangeIndexableDescSinks.remove(alphaNode);
                        }

                        if (this.rangeIndexableDescSinks != null && this.rangeIndexableDescSinks.isEmpty()) {
                            this.rangeIndexableDescSinks = null;
                        }

                        return size() == 1 ? new SingleObjectSinkAdapter(getSinks()[0]) : this;
                    }
                }
            }
        }

        this.otherSinks.remove( (ObjectSinkNode) sink );

        if ( this.otherSinks.isEmpty() ) {
            this.otherSinks = null;
        }

        return size() == 1 ? new SingleObjectSinkAdapter( getSinks()[0] ) : this;
    }

    void hashSinks(final FieldIndex fieldIndex) {
        if ( this.hashedSinkMap == null ) {
            this.hashedSinkMap = new ObjectHashMap();
        }

        final int index = fieldIndex.getIndex();
        final InternalReadAccessor fieldReader = fieldIndex.getFieldExtractor();

        ObjectSinkNode currentSink = this.hashableSinks.getFirst();

        while ( currentSink != null ) {
            final AlphaNode alphaNode = (AlphaNode) currentSink;
            final AlphaNodeFieldConstraint fieldConstraint = alphaNode.getConstraint();
            final IndexableConstraint indexableConstraint = (IndexableConstraint)  fieldConstraint;

            // position to the next sink now because alphaNode may be removed if the index is equal. If we were to do this
            // afterwards, currentSink.nextNode would be null
            currentSink = currentSink.getNextObjectSinkNode();

            // only alpha nodes that have an Operator.EQUAL are in hashableSinks, so only check if it is
            // the right field index
            if ( index == indexableConstraint.getFieldExtractor().getIndex() ) {
                final FieldValue value = indexableConstraint.getField();
                this.hashedSinkMap.put( new HashKey( index,
                                                     value,
                                                     fieldReader ),
                                        alphaNode );

                // remove the alpha from the possible candidates of hashable sinks since it is now hashed
                hashableSinks.remove( alphaNode );
            }
        }

        if ( this.hashableSinks.isEmpty() ) {
            this.hashableSinks = null;
        }

        fieldIndex.setHashed( true );
    }

    void unHashSinks(final FieldIndex fieldIndex) {
        final int index = fieldIndex.getIndex();
        // this is the list of sinks that need to be removed from the hashedSinkMap
        final List<HashKey> unhashedSinks = new ArrayList<>();

        final Iterator iter = this.hashedSinkMap.newIterator();
        ObjectHashMap.ObjectEntry entry = (ObjectHashMap.ObjectEntry) iter.next();

        while ( entry != null ) {
            final AlphaNode alphaNode = (AlphaNode) entry.getValue();
            final IndexableConstraint indexableConstraint = (IndexableConstraint) alphaNode.getConstraint();

            // only alpha nodes that have an Operator.EQUAL are in sinks, so only check if it is
            // the right field index
            if ( index == indexableConstraint.getFieldExtractor().getIndex() ) {
                final FieldValue value = indexableConstraint.getField();
                if ( this.hashableSinks == null ) {
                    this.hashableSinks = new ObjectSinkNodeList();
                }
                this.hashableSinks.add( alphaNode );

                unhashedSinks.add( new HashKey( index,
                                                value,
                                                fieldIndex.getFieldExtractor() ) );
            }

            entry = (ObjectHashMap.ObjectEntry) iter.next();
        }

        for ( HashKey hashKey : unhashedSinks ) {
            this.hashedSinkMap.remove( hashKey );
        }

        if ( this.hashedSinkMap.isEmpty() ) {
            this.hashedSinkMap = null;
        }

        fieldIndex.setHashed( false );
    }

    /**
     * Returns a FieldIndex which Keeps a count on how many times a particular field is used with an equality check
     * in the sinks.
     */
    private FieldIndex registerFieldIndex(final int index,
                                          final InternalReadAccessor fieldExtractor) {
        FieldIndex fieldIndex = null;

        // is linkedlist null, if so create and add
        if ( this.hashedFieldIndexes == null ) {
            this.hashedFieldIndexes = new LinkedList<>();
            fieldIndex = new FieldIndex( index,
                                         fieldExtractor );
            this.hashedFieldIndexes.add( fieldIndex );
        }

        // still null, so see if it already exists
        if ( fieldIndex == null ) {
            fieldIndex = findFieldIndex( index );
        }

        // doesn't exist so create it
        if ( fieldIndex == null ) {
            fieldIndex = new FieldIndex( index,
                                         fieldExtractor );
            this.hashedFieldIndexes.add( fieldIndex );
        }

        fieldIndex.increaseCounter();

        return fieldIndex;
    }

    private FieldIndex unregisterFieldIndex(final int index) {
        final FieldIndex fieldIndex = findFieldIndex( index );
        if (fieldIndex == null) {
            throw new IllegalStateException("Cannot find field index for index " + index + "!");
        }
        if (fieldIndex != null) {
            fieldIndex.decreaseCounter();

            // if the fieldcount is 0 then remove it from the linkedlist
            if ( fieldIndex.getCount() == 0 ) {
                this.hashedFieldIndexes.remove( fieldIndex );

                // if the linkedlist is empty then null it
                if ( this.hashedFieldIndexes.isEmpty() ) {
                    this.hashedFieldIndexes = null;
                }
            }

        }
        return fieldIndex;
    }

    private FieldIndex findFieldIndex(final int index) {
        for ( FieldIndex node = this.hashedFieldIndexes.getFirst(); node != null; node = node.getNext() ) {
            if ( node.getIndex() == index ) {
                return node;
            }
        }
        return null;
    }

    /**
     * Pick sinks from rangeIndexableSinks (which were stored until index threshold is exceeded) and put them into rangeIndexTree.
     * Separately for asc and desc trees
     */
    void rangeIndexSinks(final FieldIndex fieldIndex, ObjectSinkNodeList rangeIndexableSinks, Map<FieldIndex, AlphaIndexRBTree> rangeIndexTreeMap) {
        AlphaIndexRBTree rangeIndexTree = rangeIndexTreeMap.computeIfAbsent(fieldIndex, AlphaIndexRBTree::new);

        final int index = fieldIndex.getIndex();

        ObjectSinkNode currentSink = rangeIndexableSinks.getFirst();

        while (currentSink != null) {
            final AlphaNode alphaNode = (AlphaNode) currentSink;
            final AlphaNodeFieldConstraint fieldConstraint = alphaNode.getConstraint();
            final IndexableConstraint indexableConstraint = (IndexableConstraint) fieldConstraint;

            // position to the next sink now because alphaNode may be removed if the index is equal. If we were to do this
            // afterwards, currentSink.nextNode would be null
            currentSink = currentSink.getNextObjectSinkNode();

            if (index == indexableConstraint.getFieldExtractor().getIndex()) {
                rangeIndexTree.add(alphaNode);

                // remove the alpha from the possible candidates of range indexable sinks since it is now range indexed
                rangeIndexableSinks.remove(alphaNode);
            }
        }

        fieldIndex.setRangeIndexed(true);
    }

    void unRangeIndexSinks(final FieldIndex fieldIndex, ObjectSinkNodeList rangeIndexableSinks, AlphaIndexRBTree rangeIndexTree, Map<FieldIndex, AlphaIndexRBTree> rangeIndexTreeMap) {
        FastIterator it = rangeIndexTree.fastIterator();
        for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
            Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
            AlphaNode alphaNode = node.value;
            rangeIndexableSinks.add(alphaNode);
        }

        rangeIndexTree.clear();
        rangeIndexTreeMap.remove(fieldIndex);

        fieldIndex.setHashed(false);
    }

    private boolean isRangeIndexable(AlphaNode alphaNode) {
        if (!RANGE_INDEX_ENABLED) {
            return false;
        }
        AlphaNodeFieldConstraint fieldConstraint = alphaNode.getConstraint();
        if (fieldConstraint instanceof IndexableConstraint) {
            IndexableConstraint indexableConstraint = (IndexableConstraint) fieldConstraint;
            ConstraintType constraintType = indexableConstraint.getConstraintType();
            if ((constraintType.isAscending() || constraintType.isDescending()) && indexableConstraint.getField() != null &&
                indexableConstraint.getFieldExtractor().getValueType() != ValueType.OBJECT_TYPE &&
                // our current implementation does not support range indexing of deeply nested properties
                indexableConstraint.getFieldExtractor().getIndex() >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a FieldIndex which Keeps a count on how many times a particular field is used with a range check
     * in the sinks. Stores the FieldIndex for asc and desc separately
     */
    private FieldIndex registerFieldIndexForRange(final int index,
                                                  final InternalReadAccessor fieldExtractor,
                                                  LinkedList<FieldIndex> rangeIndexedFieldIndexes) {
        FieldIndex fieldIndex = findFieldIndexForRange(index, rangeIndexedFieldIndexes);

        // doesn't exist so create it
        if (fieldIndex == null) {
            fieldIndex = new FieldIndex(index,
                                        fieldExtractor);
            rangeIndexedFieldIndexes.add(fieldIndex);
        }

        fieldIndex.increaseCounter();

        return fieldIndex;
    }

    private FieldIndex unregisterFieldIndexForRange(final int index, LinkedList<FieldIndex> rangeIndexedFieldIndexes) {
        final FieldIndex fieldIndex = findFieldIndexForRange( index, rangeIndexedFieldIndexes );
        if (fieldIndex == null) {
            throw new IllegalStateException("Cannot find field index for index " + index + "!");
        }
        fieldIndex.decreaseCounter();

        // if the fieldcount is 0 then remove it from the linkedlist
        if ( fieldIndex.getCount() == 0 ) {
            rangeIndexedFieldIndexes.remove( fieldIndex );
        }
        return fieldIndex;
    }

    private FieldIndex findFieldIndexForRange(final int index, LinkedList<FieldIndex> rangeIndexedFieldIndexes) {
        if (rangeIndexedFieldIndexes == null) {
            return null;
        }
        for (FieldIndex node = rangeIndexedFieldIndexes.getFirst(); node != null; node = node.getNext()) {
            if (node.getIndex() == index) {
                return node;
            }
        }
        return null;
    }

    public void propagateAssertObject(final InternalFactHandle factHandle,
                                      final PropagationContext context,
                                      final InternalWorkingMemory workingMemory) {
        final Object object = factHandle.getObject();
        //System.out.println("propagateAssertObject: object = " + object);

        // Iterates the FieldIndex collection, which tells you if particularly field is hashed or not
        // if the field is hashed then it builds the hashkey to return the correct sink for the current objects slot's
        // value, one object may have multiple fields indexed.
        if ( this.hashedFieldIndexes != null ) {
            // Iterate the FieldIndexes to see if any are hashed
            for ( FieldIndex fieldIndex = this.hashedFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext() ) {
                if ( !fieldIndex.isHashed() ) {
                    continue;
                }
                // this field is hashed so set the existing hashKey and see if there is a sink for it
                final AlphaNode sink = (AlphaNode) this.hashedSinkMap.get( new HashKey( fieldIndex, object ) );
                if ( sink != null ) {
                    // go straight to the AlphaNode's propagator, as we know it's true and no need to retest
                    sink.getObjectSinkPropagator().propagateAssertObject( factHandle, context, workingMemory );
                }
            }
        }

        // Range indexing
        if (this.rangeIndexedAscFieldIndexes != null) {
            // Iterate the FieldIndexes to see if any are range indexed
            for (FieldIndex fieldIndex = this.rangeIndexedAscFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext()) {
                if (!fieldIndex.isRangeIndexed()) {
                    continue;
                }
                AlphaIndexRBTree rangeIndexTree = this.rangeIndexAscTreeMap.get(fieldIndex);
                FastIterator it = rangeIndexTree.ascMatchingAlphaNodeIterator(object);
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    AlphaNode sink = node.value;
                    // go straight to the AlphaNode's propagator, as we know it's true and no need to retest
                    //System.out.println("### asc hit : " + sink);
                    sink.getObjectSinkPropagator().propagateAssertObject(factHandle, context, workingMemory);
                }
            }
        }
        if (this.rangeIndexedDescFieldIndexes != null) {
            // Iterate the FieldIndexes to see if any are range indexed
            for (FieldIndex fieldIndex = this.rangeIndexedDescFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext()) {
                if (!fieldIndex.isRangeIndexed()) {
                    continue;
                }
                AlphaIndexRBTree rangeIndexTree = this.rangeIndexDescTreeMap.get(fieldIndex);
                FastIterator it = rangeIndexTree.descMatchingAlphaNodeIterator(object);
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    AlphaNode sink = node.value;
                    // go straight to the AlphaNode's propagator, as we know it's true and no need to retest
                    //System.out.println("### desc hit : " + sink);
                    sink.getObjectSinkPropagator().propagateAssertObject(factHandle, context, workingMemory);
                }
            }
        }

        // propagate unhashed
        if ( this.hashableSinks != null ) {
            for ( ObjectSinkNode sink = this.hashableSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                doPropagateAssertObject( factHandle,
                                         context,
                                         workingMemory,
                                         sink );
            }
        }

        // propagate un-rangeindexed
        if ( this.rangeIndexableAscSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableAscSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                doPropagateAssertObject( factHandle,
                                         context,
                                         workingMemory,
                                         sink );
            }
        }
        if ( this.rangeIndexableDescSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableDescSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                doPropagateAssertObject( factHandle,
                                         context,
                                         workingMemory,
                                         sink );
            }
        }

        if ( this.otherSinks != null ) {
            // propagate others
            for ( ObjectSinkNode sink = this.otherSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                doPropagateAssertObject( factHandle,
                                         context,
                                         workingMemory,
                                         sink );
            }
        }
    }

    public void propagateModifyObject(final InternalFactHandle factHandle,
                                      final ModifyPreviousTuples modifyPreviousTuples,
                                      final PropagationContext context,
                                      final InternalWorkingMemory workingMemory) {
        final Object object = factHandle.getObject();

        // Iterates the FieldIndex collection, which tells you if particularly field is hashed or not
        // if the field is hashed then it builds the hashkey to return the correct sink for the current objects slot's
        // value, one object may have multiple fields indexed.
        if ( this.hashedFieldIndexes != null ) {
            // Iterate the FieldIndexes to see if any are hashed
            for ( FieldIndex fieldIndex = this.hashedFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext() ) {
                if ( !fieldIndex.isHashed() ) {
                    continue;
                }
                // this field is hashed so set the existing hashKey and see if there is a sink for it
                final AlphaNode sink = (AlphaNode) this.hashedSinkMap.get( new HashKey( fieldIndex, object ) );
                if ( sink != null ) {
                    // go straight to the AlphaNode's propagator, as we know it's true and no need to retest
                    sink.getObjectSinkPropagator().propagateModifyObject( factHandle, modifyPreviousTuples, context, workingMemory );
                }
            }
        }

        // Range indexing
        if (this.rangeIndexedAscFieldIndexes != null) {
            // Iterate the FieldIndexes to see if any are range indexed
            for (FieldIndex fieldIndex = this.rangeIndexedAscFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext()) {
                if (!fieldIndex.isRangeIndexed()) {
                    continue;
                }
                AlphaIndexRBTree rangeIndexTree = this.rangeIndexAscTreeMap.get(fieldIndex);
                FastIterator it = rangeIndexTree.ascMatchingAlphaNodeIterator(object);
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    AlphaNode sink = node.value;
                    // go straight to the AlphaNode's propagator, as we know it's true and no need to retest
                    //System.out.println("### asc hit : " + sink);
                    sink.getObjectSinkPropagator().propagateModifyObject(factHandle, modifyPreviousTuples, context, workingMemory);
                }
            }
        }
        if (this.rangeIndexedDescFieldIndexes != null) {
            // Iterate the FieldIndexes to see if any are range indexed
            for (FieldIndex fieldIndex = this.rangeIndexedDescFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext()) {
                if (!fieldIndex.isRangeIndexed()) {
                    continue;
                }
                AlphaIndexRBTree rangeIndexTree = this.rangeIndexDescTreeMap.get(fieldIndex);
                FastIterator it = rangeIndexTree.descMatchingAlphaNodeIterator(object);
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    AlphaNode sink = node.value;
                    // go straight to the AlphaNode's propagator, as we know it's true and no need to retest
                    //System.out.println("### desc hit : " + sink);
                    sink.getObjectSinkPropagator().propagateModifyObject(factHandle, modifyPreviousTuples, context, workingMemory);
                }
            }
        }

        // propagate unhashed
        if ( this.hashableSinks != null ) {
            for ( ObjectSinkNode sink = this.hashableSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                doPropagateModifyObject( factHandle,
                                         modifyPreviousTuples,
                                         context,
                                         workingMemory,
                                         sink );
            }
        }

        // propagate un-rangeindexed
        if ( this.rangeIndexableAscSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableAscSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                doPropagateModifyObject( factHandle,
                                         modifyPreviousTuples,
                                         context,
                                         workingMemory,
                                         sink );
            }
        }
        if ( this.rangeIndexableDescSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableDescSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                doPropagateModifyObject( factHandle,
                                         modifyPreviousTuples,
                                         context,
                                         workingMemory,
                                         sink );
            }
        }

        if ( this.otherSinks != null ) {
            // propagate others
            for ( ObjectSinkNode sink = this.otherSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                doPropagateModifyObject( factHandle,
                                         modifyPreviousTuples,
                                         context,
                                         workingMemory,
                                         sink );
            }
        }
    }
    
    public void byPassModifyToBetaNode (final InternalFactHandle factHandle,
                                        final ModifyPreviousTuples modifyPreviousTuples,
                                        final PropagationContext context,
                                        final InternalWorkingMemory workingMemory) {
        final Object object = factHandle.getObject();

        // We need to iterate in the same order as the assert
        if ( this.hashedFieldIndexes != null ) {
            // Iterate the FieldIndexes to see if any are hashed
            for ( FieldIndex fieldIndex = this.hashedFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext() ) {
                if ( !fieldIndex.isHashed() ) {
                    continue;
                }
                // this field is hashed so set the existing hashKey and see if there is a sink for it
                final AlphaNode sink = (AlphaNode) this.hashedSinkMap.get( new HashKey( fieldIndex, object ) );
                if ( sink != null ) {
                    // only alpha nodes are hashable
                    sink.getObjectSinkPropagator().byPassModifyToBetaNode( factHandle, modifyPreviousTuples, context, workingMemory );
                }
            }
        }

        // Range indexing
        if (this.rangeIndexedAscFieldIndexes != null) {
            // Iterate the FieldIndexes to see if any are range indexed
            for (FieldIndex fieldIndex = this.rangeIndexedAscFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext()) {
                if (!fieldIndex.isRangeIndexed()) {
                    continue;
                }
                AlphaIndexRBTree rangeIndexTree = this.rangeIndexAscTreeMap.get(fieldIndex);
                FastIterator it = rangeIndexTree.ascMatchingAlphaNodeIterator(object);
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    AlphaNode sink = node.value;
                    //System.out.println("### asc hit : " + sink);
                    sink.getObjectSinkPropagator().byPassModifyToBetaNode(factHandle, modifyPreviousTuples, context, workingMemory);
                }
            }
        }
        if (this.rangeIndexedDescFieldIndexes != null) {
            // Iterate the FieldIndexes to see if any are range indexed
            for (FieldIndex fieldIndex = this.rangeIndexedDescFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext()) {
                if (!fieldIndex.isRangeIndexed()) {
                    continue;
                }
                AlphaIndexRBTree rangeIndexTree = this.rangeIndexDescTreeMap.get(fieldIndex);
                FastIterator it = rangeIndexTree.descMatchingAlphaNodeIterator(object);
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    AlphaNode sink = node.value;
                    //System.out.println("### desc hit : " + sink);
                    sink.getObjectSinkPropagator().byPassModifyToBetaNode(factHandle, modifyPreviousTuples, context, workingMemory);
                }
            }
        }

        // propagate unhashed
        if ( this.hashableSinks != null ) {
            for ( ObjectSinkNode sink = this.hashableSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                // only alpha nodes are hashable
                ((AlphaNode)sink).getObjectSinkPropagator().byPassModifyToBetaNode( factHandle, modifyPreviousTuples, context, workingMemory );
            }
        }

        // propagate un-rangeindexed
        if ( this.rangeIndexableAscSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableAscSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                ((AlphaNode)sink).getObjectSinkPropagator().byPassModifyToBetaNode( factHandle, modifyPreviousTuples, context, workingMemory );
            }
        }
        if ( this.rangeIndexableDescSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableDescSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                ((AlphaNode)sink).getObjectSinkPropagator().byPassModifyToBetaNode( factHandle, modifyPreviousTuples, context, workingMemory );
            }
        }

        if ( this.otherSinks != null ) {
            // propagate others
            for ( ObjectSinkNode sink = this.otherSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {                
                // compound alpha, lianode or betanode
                sink.byPassModifyToBetaNode( factHandle, modifyPreviousTuples, context, workingMemory );
            }
        }        
    }

    /**
     * This is a Hook method for subclasses to override. Please keep it protected unless you know
     * what you are doing.
     */
    protected void doPropagateAssertObject(InternalFactHandle factHandle,
                                           PropagationContext context,
                                           InternalWorkingMemory workingMemory,
                                           ObjectSink sink) {
        sink.assertObject( factHandle,
                           context,
                           workingMemory );
    }

    protected void doPropagateModifyObject(InternalFactHandle factHandle,
                                           final ModifyPreviousTuples modifyPreviousTuples,
                                           PropagationContext context,
                                           InternalWorkingMemory workingMemory,
                                           ObjectSink sink) {
        sink.modifyObject( factHandle,
                           modifyPreviousTuples,
                           context,
                           workingMemory );
    }

    public BaseNode getMatchingNode(BaseNode candidate) {
        if (sinksMap == null) {
            reIndexNodes();
        }
        return (( BaseNode ) sinksMap.get( candidate ));
    }

    public void reIndexNodes() {
        sinksMap = new HashMap<>();
        if ( this.otherSinks != null ) {
            for ( ObjectSinkNode sink = this.otherSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                sinksMap.put( sink, sink );
            }
        }

        if ( this.hashableSinks != null ) {
            for ( ObjectSinkNode sink = this.hashableSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                sinksMap.put( sink, sink );
            }
        }

        if ( this.rangeIndexableAscSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableAscSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                sinksMap.put( sink, sink );
            }
        }
        if ( this.rangeIndexableDescSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableDescSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                sinksMap.put( sink, sink );
            }
        }

        if ( this.hashedSinkMap != null ) {
            final Iterator it = this.hashedSinkMap.newIterator();
            for ( ObjectEntry entry = (ObjectEntry) it.next(); entry != null; entry = (ObjectEntry) it.next() ) {
                final ObjectSink sink = (ObjectSink) entry.getValue();
                sinksMap.put( sink, sink );
            }
        }

        if ( this.rangeIndexAscTreeMap != null ) {
            Collection<AlphaIndexRBTree> trees = rangeIndexAscTreeMap.values();
            for (AlphaIndexRBTree tree : trees) {
                FastIterator it = tree.fastIterator();
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    AlphaNode sink = node.value;
                    sinksMap.put( sink, sink );
                }
            }
        }
        if ( this.rangeIndexDescTreeMap != null ) {
            Collection<AlphaIndexRBTree> trees = rangeIndexDescTreeMap.values();
            for (AlphaIndexRBTree tree : trees) {
                FastIterator it = tree.fastIterator();
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    AlphaNode sink = node.value;
                    sinksMap.put( sink, sink );
                }
            }
        }

    }

    public ObjectSink[] getSinks() {
        if ( this.sinks != null ) {
            return sinks;
        }
        ObjectSink[] newSinks = new ObjectSink[size()];
        int at = 0;

        if ( this.hashedFieldIndexes != null ) {
            // Iterate the FieldIndexes to see if any are hashed
            for ( FieldIndex fieldIndex = this.hashedFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext() ) {
                if ( !fieldIndex.isHashed() ) {
                    continue;
                }
                // this field is hashed so set the existing hashKey and see if there is a sink for it
                final int index = fieldIndex.getIndex();
                final Iterator it = this.hashedSinkMap.newIterator();
                for ( ObjectEntry entry = (ObjectEntry) it.next(); entry != null; entry = (ObjectEntry) it.next() ) {
                    HashKey hashKey = (HashKey) entry.getKey();
                    if (hashKey.getIndex() == index) {
                        newSinks[at++] = (ObjectSink) entry.getValue();
                    }
                }
            }
        }

        if ( this.rangeIndexedAscFieldIndexes != null ) {
            // Iterate the FieldIndexes to see if any are range indexed
            for ( FieldIndex fieldIndex = this.rangeIndexedAscFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext() ) {
                if ( !fieldIndex.isRangeIndexed() ) {
                    continue;
                }
                FastIterator it = this.rangeIndexAscTreeMap.get(fieldIndex).fastIterator();
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    newSinks[at++] = node.value;
                }
            }
        }
        if ( this.rangeIndexedDescFieldIndexes != null ) {
            // Iterate the FieldIndexes to see if any are range indexed
            for ( FieldIndex fieldIndex = this.rangeIndexedDescFieldIndexes.getFirst(); fieldIndex != null; fieldIndex = fieldIndex.getNext() ) {
                if ( !fieldIndex.isRangeIndexed() ) {
                    continue;
                }
                FastIterator it = this.rangeIndexDescTreeMap.get(fieldIndex).fastIterator();
                for (Entry entry = it.next(null); entry != null; entry = it.next(entry)) {
                    Node<Comparable<Comparable>, AlphaNode> node = (Node<Comparable<Comparable>, AlphaNode>) entry;
                    newSinks[at++] = node.value;
                }
            }
        }

        if ( this.hashableSinks != null ) {
            for ( ObjectSinkNode sink = this.hashableSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                newSinks[at++] = sink;
            }
        }

        if ( this.rangeIndexableAscSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableAscSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                newSinks[at++] = sink;
            }
        }
        if ( this.rangeIndexableDescSinks != null ) {
            for ( ObjectSinkNode sink = this.rangeIndexableDescSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                newSinks[at++] = sink;
            }
        }

        if ( this.otherSinks != null ) {
            for ( ObjectSinkNode sink = this.otherSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                newSinks[at++] = sink;
            }
        }
        this.sinks = newSinks;
        return newSinks;
    }
    
    public void doLinkRiaNode(InternalWorkingMemory wm) {
        if ( this.otherSinks != null ) {
            // this is only used for ria nodes when exists are shared, we know there is no indexing for those
            for ( ObjectSinkNode sink = this.otherSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                SingleObjectSinkAdapter.staticDoLinkRiaNode( sink, wm );
            }
        }
    }

    public void doUnlinkRiaNode(InternalWorkingMemory wm) {
        if ( this.otherSinks != null ) {
            // this is only used for ria nodes when exists are shared, we know there is no indexing for those
            for ( ObjectSinkNode sink = this.otherSinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode() ) {
                SingleObjectSinkAdapter.staticDoUnlinkRiaNode( sink, wm );
            }
        }
    }     

    public int size() {
        return (this.otherSinks != null ? this.otherSinks.size() : 0) + (this.hashableSinks != null ? this.hashableSinks.size() : 0) + (this.hashedSinkMap != null ? this.hashedSinkMap.size() : 0)
                + (this.rangeIndexableAscSinks != null ? this.rangeIndexableAscSinks.size() : 0) + (this.rangeIndexableDescSinks != null ? this.rangeIndexableDescSinks.size() : 0)
                + sizeInAllTrees();
    }

    private int sizeInAllTrees() {
        int size = 0;
        if (rangeIndexAscTreeMap != null) {
            size += rangeIndexAscTreeMap.values().stream().map(AlphaIndexRBTree::size).reduce(0, Integer::sum);
        }
        if (rangeIndexDescTreeMap != null) {
            size += rangeIndexDescTreeMap.values().stream().map(AlphaIndexRBTree::size).reduce(0, Integer::sum);
        }
        return size;
    }

    public boolean isEmpty() {
        return false;
    }

    public ObjectSinkNodeList getOtherSinks() {
        return otherSinks;
    }

    public LinkedList<FieldIndex> getHashedFieldIndexes() {
        return hashedFieldIndexes;
    }

    public static class HashKey
        implements
        Externalizable {
        private static final long serialVersionUID = 510l;

        private static final byte OBJECT           = 1;
        private static final byte LONG             = 2;
        private static final byte DOUBLE           = 3;
        private static final byte BOOL             = 4;

        private int               index;

        private byte              type;
        private Object            ovalue;
        private long              lvalue;
        private boolean           bvalue;
        private double            dvalue;

        private boolean           isNull;

        private int               hashCode;

        public HashKey() {
        }

        public HashKey(FieldIndex fieldIndex, Object value) {
            this.setValue( fieldIndex.getIndex(),
                           value,
                           fieldIndex.getFieldExtractor() );
        }

        public HashKey(final int index,
                       final FieldValue value,
                       final InternalReadAccessor extractor) {
            this.setValue( index,
                           extractor,
                           value );
        }

        public void readExternal(ObjectInput in) throws IOException,
                                                ClassNotFoundException {
            index = in.readInt();
            type = in.readByte();
            ovalue = in.readObject();
            lvalue = in.readLong();
            bvalue = in.readBoolean();
            dvalue = in.readDouble();
            isNull = in.readBoolean();
            hashCode = in.readInt();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt( index );
            out.writeByte( type );
            out.writeObject( ovalue );
            out.writeLong( lvalue );
            out.writeBoolean( bvalue );
            out.writeDouble( dvalue );
            out.writeBoolean( isNull );
            out.writeInt( hashCode );
        }

        public int getIndex() {
            return this.index;
        }

        public void setValue(final int index,
                             final Object value,
                             final InternalReadAccessor extractor) {
            this.index = index;
            final ValueType vtype = extractor.getValueType();

            isNull = extractor.isNullValue( null, value );

            if ( vtype.isBoolean() ) {
                this.type = BOOL;
                if ( !isNull ) {
                    this.bvalue = extractor.getBooleanValue( null,
                                                             value );
                    this.setHashCode( this.bvalue ? 1231 : 1237 );
                } else {
                    this.setHashCode( 0 );
                }
            } else if ( vtype.isIntegerNumber() || vtype.isChar() ) {
                this.type = LONG;
                if ( !isNull ) {
                    this.lvalue = extractor.getLongValue( null,
                                                          value );
                    this.setHashCode( (int) (this.lvalue ^ (this.lvalue >>> 32)) );
                } else {
                    this.setHashCode( 0 );
                }
            } else if ( vtype.isFloatNumber() ) {
                this.type = DOUBLE;
                if ( !isNull ) {
                    this.dvalue = extractor.getDoubleValue( null,
                                                            value );
                    final long temp = Double.doubleToLongBits( this.dvalue );
                    this.setHashCode( (int) (temp ^ (temp >>> 32)) );
                } else {
                    this.setHashCode( 0 );
                }
            } else {
                this.type = OBJECT;
                if ( !isNull ) {
                    this.ovalue = extractor.getValue( null,
                                                      value );
                    this.setHashCode( this.ovalue != null ? this.ovalue.hashCode() : 0 );
                } else {
                    this.setHashCode( 0 );
                }
            }
        }

        public void setValue(final int index,
                             final InternalReadAccessor extractor,
                             final FieldValue value) {
            this.index = index;

            this.isNull = value.isNull();
            final ValueType vtype = extractor.getValueType();

            if ( vtype.isBoolean() ) {
                this.type = BOOL;
                if ( !isNull ) {
                    this.bvalue = value.getBooleanValue();
                    this.setHashCode( this.bvalue ? 1231 : 1237 );
                } else {
                    this.setHashCode( 0 );
                }
            } else if ( vtype.isIntegerNumber() ) {
                this.type = LONG;
                if ( !isNull ) {
                    this.lvalue = value.getLongValue();
                    this.setHashCode( (int) (this.lvalue ^ (this.lvalue >>> 32)) );
                } else {
                    this.setHashCode( 0 );
                }
            } else if ( vtype.isFloatNumber() ) {
                this.type = DOUBLE;
                if ( !isNull ) {
                    this.dvalue = value.getDoubleValue();
                    final long temp = Double.doubleToLongBits( this.dvalue );
                    this.setHashCode( (int) (temp ^ (temp >>> 32)) );
                } else {
                    this.setHashCode( 0 );
                }
            } else {
                this.type = OBJECT;
                if ( !isNull ) {
                    this.ovalue = vtype.coerce( value.getValue() );
                    this.setHashCode( this.ovalue != null ? this.ovalue.hashCode() : 0 );
                } else {
                    this.setHashCode( 0 );
                }
            }
        }

        private void setHashCode(final int hashSeed) {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + hashSeed;
            result = PRIME * result + this.index;
            this.hashCode = result;
        }

        public boolean getBooleanValue() {
            switch ( this.type ) {
                case BOOL :
                    return this.bvalue;
                case OBJECT :
                    if ( this.ovalue == null ) {
                        return false;
                    } else if ( this.ovalue instanceof Boolean ) {
                        return (Boolean) this.ovalue;
                    } else if ( this.ovalue instanceof String ) {
                        return Boolean.valueOf( (String) this.ovalue );
                    } else {
                        throw new ClassCastException( "Can't convert " + this.ovalue.getClass() + " to a boolean value." );
                    }
                case LONG :
                    throw new ClassCastException( "Can't convert long to a boolean value." );
                case DOUBLE :
                    throw new ClassCastException( "Can't convert double to a boolean value." );

            }
            return false;
        }

        public long getLongValue() {
            switch ( this.type ) {
                case BOOL :
                    return this.bvalue ? 1 : 0;
                case OBJECT :
                    if ( this.ovalue == null ) {
                        return 0;
                    } else if ( this.ovalue instanceof Number ) {
                        return ((Number) this.ovalue).longValue();
                    } else if ( this.ovalue instanceof String ) {
                        return Long.parseLong( (String) this.ovalue );
                    } else {
                        throw new ClassCastException( "Can't convert " + this.ovalue.getClass() + " to a long value." );
                    }
                case LONG :
                    return this.lvalue;
                case DOUBLE :
                    return (long) this.dvalue;

            }
            return 0;
        }

        public double getDoubleValue() {
            switch ( this.type ) {
                case BOOL :
                    return this.bvalue ? 1 : 0;
                case OBJECT :
                    if ( this.ovalue == null ) {
                        return 0;
                    } else if ( this.ovalue instanceof Number ) {
                        return ((Number) this.ovalue).doubleValue();
                    } else if ( this.ovalue instanceof String ) {
                        return Double.parseDouble( (String) this.ovalue );
                    } else {
                        throw new ClassCastException( "Can't convert " + this.ovalue.getClass() + " to a double value." );
                    }
                case LONG :
                    return this.lvalue;
                case DOUBLE :
                    return this.dvalue;
            }
            return 0;
        }

        public Object getObjectValue() {
            switch ( this.type ) {
                case BOOL :
                    return this.bvalue ? Boolean.TRUE : Boolean.FALSE;
                case OBJECT :
                    return this.ovalue;
                case LONG :
                    return this.lvalue;
                case DOUBLE :
                    return this.dvalue;
            }
            return null;
        }

        public int hashCode() {
            return this.hashCode;
        }

        public boolean equals(final Object object) {
            if (!(object instanceof HashKey)) {
                return false;
            }
            final HashKey other = (HashKey) object;

            if (this.isNull != other.isNull || this.index != other.index) {
                return false;
            }

            switch ( this.type ) {
                case BOOL :
                    return this.bvalue == other.getBooleanValue();
                case LONG :
                    return this.lvalue == other.getLongValue();
                case DOUBLE :
                    return this.dvalue == other.getDoubleValue();
                case OBJECT :
                    final Object otherValue = other.getObjectValue();
                    return this.ovalue == null ? otherValue == null : this.ovalue.equals( otherValue );
            }
            return false;
        }

    }

    public static class FieldIndex
        implements
        LinkedListNode<FieldIndex>,
        Externalizable {
        private static final long    serialVersionUID = 510l;
        private int                  index;
        private InternalReadAccessor fieldExtactor;

        private int                  count;

        private boolean              hashed;
        private boolean              rangeIndexed;

        private FieldIndex           previous;
        private FieldIndex           next;

        public FieldIndex() {
        }

        public FieldIndex(final int index,
                          final InternalReadAccessor fieldExtractor) {
            this.index = index;
            this.fieldExtactor = fieldExtractor;
        }

        public void readExternal(ObjectInput in) throws IOException,
                                                ClassNotFoundException {
            index = in.readInt();
            fieldExtactor = (InternalReadAccessor) in.readObject();
            count = in.readInt();
            hashed = in.readBoolean();
            rangeIndexed = in.readBoolean();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt( index );
            out.writeObject( fieldExtactor );
            out.writeInt( count );
            out.writeBoolean( hashed );
            out.writeBoolean( rangeIndexed );
        }

        public InternalReadAccessor getFieldExtractor() {
            return this.fieldExtactor;
        }

        public int getIndex() {
            return this.index;
        }

        public int getCount() {
            return this.count;
        }

        public ReadAccessor getFieldExtactor() {
            return this.fieldExtactor;
        }

        public boolean isHashed() {
            return this.hashed;
        }

        public void setHashed(final boolean hashed) {
            this.hashed = hashed;
        }

        public boolean isRangeIndexed() {
            return rangeIndexed;
        }

        public void setRangeIndexed(boolean rangeIndexed) {
            this.rangeIndexed = rangeIndexed;
        }

        public void increaseCounter() {
            this.count++;
        }

        public void decreaseCounter() {
            this.count--;
        }

        public FieldIndex getNext() {
            return this.next;
        }

        public FieldIndex getPrevious() {
            return this.previous;
        }

        public void setNext(final FieldIndex next) {
            this.next = next;
        }

        public void setPrevious(final FieldIndex previous) {
            this.previous = previous;
        }

        public void nullPrevNext() {
            previous = null;
            next = null;
        }
    }
}
