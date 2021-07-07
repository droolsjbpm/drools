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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.common.BaseNode;
import org.drools.core.common.BetaConstraints;
import org.drools.core.common.EmptyBetaConstraints;
import org.drools.core.common.RuleBasePartitionId;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.AlphaNode;
import org.drools.core.reteoo.BetaNode;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.LeftTupleNode;
import org.drools.core.reteoo.NodeTypeEnums;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.rule.AbstractCompositeConstraint;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.IntervalProviderConstraint;
import org.drools.core.rule.Pattern;
import org.drools.core.rule.RuleConditionElement;
import org.drools.core.spi.AlphaNodeFieldConstraint;
import org.drools.core.spi.BetaNodeFieldConstraint;
import org.drools.core.spi.ObjectType;
import org.drools.core.time.Interval;
import org.drools.core.time.TemporalDependencyMatrix;
import org.drools.core.time.TimeUtils;
import org.kie.api.definition.rule.Rule;

/**
 * Utility functions for reteoo build
 */
public class BuildUtils {

    private final Map<Class< ? >, ReteooComponentBuilder> componentBuilders = new HashMap<Class< ? >, ReteooComponentBuilder>();

    /**
     * Adds the given builder for the given target to the builders map
     */
    public void addBuilder(final Class< ? > target,
                           final ReteooComponentBuilder builder) {
        this.componentBuilders.put( target,
                                    builder );
    }

    /**
     * Returns a builder for the given target from the builders map
     */
    public ReteooComponentBuilder getBuilderFor(final RuleConditionElement target) {
        return getBuilderFor( target.getClass() );
    }

    public ReteooComponentBuilder getBuilderFor(final Class cls) {
        ReteooComponentBuilder builder = this.componentBuilders.get( cls );
        return builder != null || cls.getSuperclass() == null ? builder : getBuilderFor(cls.getSuperclass());
    }

    /**
     * Attaches a node into the network. If a node already exists that could
     * substitute, it is used instead.
     *
     * @param context
     *            The current build context
     * @param candidate
     *            The node to attach.
     *
     * @return the actual attached node that may be the one given as parameter
     *         or eventually one that was already in the cache if sharing is enabled
     */
    public <T extends BaseNode> T attachNode(BuildContext context, T candidate) {
        BaseNode node = null;
        RuleBasePartitionId partition = null;
        if ( candidate.getType() == NodeTypeEnums.EntryPointNode ) {
            // entry point nodes are always shared
            node = context.getKnowledgeBase().getRete().getEntryPointNode( ((EntryPointNode) candidate).getEntryPoint() );
            // all EntryPointNodes belong to the main partition
            partition = RuleBasePartitionId.MAIN_PARTITION;
        } else if ( candidate.getType() == NodeTypeEnums.ObjectTypeNode ) {
            // object type nodes are always shared
            Map<ObjectType, ObjectTypeNode> map = context.getKnowledgeBase().getRete().getObjectTypeNodes( context.getCurrentEntryPoint() );
            if ( map != null ) {
                ObjectTypeNode otn = map.get( ((ObjectTypeNode) candidate).getObjectType() );
                if ( otn != null ) {
                    // adjusting expiration offset
                    otn.mergeExpirationOffset( (ObjectTypeNode) candidate );
                    node = otn;
                }
            }
            // all ObjectTypeNodes belong to the main partition
            partition = RuleBasePartitionId.MAIN_PARTITION;
        } else if ( isSharingEnabledForNode( context,
                                             candidate ) ) {
            if ( (context.getTupleSource() != null) && NodeTypeEnums.isLeftTupleSink( candidate ) ) {
                node = context.getTupleSource().getSinkPropagator().getMatchingNode( candidate );
            } else if ( (context.getObjectSource() != null) && NodeTypeEnums.isObjectSink( candidate ) ) {
                node = context.getObjectSource().getObjectSinkPropagator().getMatchingNode( candidate );
            } else {
                throw new RuntimeException( "This is a bug on node sharing verification. Please report to development team." );
            }
        }

        if ( node != null && !areNodesCompatibleForSharing(context, node) ) {
            node = null;
        }

        if ( node == null ) {
            // only attach() if it is a new node
            node = candidate;

            // new node, so it must be labeled
            if ( partition == null ) {
                // if it does not has a predefined label
                if ( context.getPartitionId() == null ) {
                    // if no label in current context, create one
                    context.setPartitionId( context.getKnowledgeBase().createNewPartitionId() );
                }
                partition = context.getPartitionId();
            }
            // set node whit the actual partition label
            node.setPartitionId( context, partition );
            node.attach(context);
        } else {
            // shared node found
            mergeNodes(node, candidate);
            // undo previous id assignment
            context.releaseId( candidate );
            if ( partition == null && context.getPartitionId() == null ) {
                partition = node.getPartitionId();
                // if no label in current context, create one
                context.setPartitionId( partition );
            }
        }
        // adds the node to the context list to track all added nodes
        context.getNodes().add( node );
        node.addAssociation( context, context.getRule() );
        return (T)node;
    }

    private void mergeNodes(BaseNode node, BaseNode duplicate) {
        if (node instanceof AlphaNode) {
            AlphaNodeFieldConstraint alphaConstraint = ((AlphaNode) node).getConstraint();
            alphaConstraint.addPackageNames(((AlphaNode) duplicate).getConstraint().getPackageNames());
        } else if (node instanceof BetaNode) {
            BetaNodeFieldConstraint[] betaConstraints = ((BetaNode) node).getConstraints();
            int i = 0;
            for (BetaNodeFieldConstraint betaConstraint : betaConstraints) {
                betaConstraint.addPackageNames(((BetaNode) duplicate).getConstraints()[i].getPackageNames());
                i++;
            }
        }
    }

    /**
     * Utility function to check if sharing is enabled for nodes of the given class
     */
    private boolean isSharingEnabledForNode(BuildContext context, BaseNode node) {
        if ( NodeTypeEnums.isLeftTupleSource( node )) {
            return context.getKnowledgeBase().getConfiguration().isShareBetaNodes();
        } else if ( NodeTypeEnums.isObjectSource( node ) ) {
            return context.getKnowledgeBase().getConfiguration().isShareAlphaNodes();
        }
        return false;
    }

    private boolean areNodesCompatibleForSharing(BuildContext context, BaseNode node) {
        if ( node.getType() == NodeTypeEnums.RightInputAdaterNode ) {
            // avoid subnetworks sharing when they belong to 2 different agenda-groups
            String agendaGroup = context.getRule().getAgendaGroup();
            for (Rule associatedRule : node.getAssociatedRules()) {
                if (!agendaGroup.equals( (( RuleImpl ) associatedRule).getAgendaGroup() )) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Creates and returns a BetaConstraints object for the given list of constraints
     *
     * @param context the current build context
     * @param list the list of constraints
     */
    public BetaConstraints createBetaNodeConstraint(final BuildContext context,
                                                    final List<BetaNodeFieldConstraint> list,
                                                    final boolean disableIndexing) {
        BetaConstraints constraints;
        switch ( list.size() ) {
            case 0 :
                constraints = EmptyBetaConstraints.getInstance();
                break;
            case 1 :
                constraints = BetaNodeConstraintFactory.Factory.get().createSingleBetaConstraints( list.get( 0 ),
                                                         context.getKnowledgeBase().getConfiguration(),
                                                         disableIndexing );
                break;
            case 2 :
                constraints = BetaNodeConstraintFactory.Factory.get().createDoubleBetaConstraints( list.toArray( new BetaNodeFieldConstraint[list.size()] ),
                                                         context.getKnowledgeBase().getConfiguration(),
                                                         disableIndexing );
                break;
            case 3 :
                constraints = BetaNodeConstraintFactory.Factory.get().createTripleBetaConstraints( list.toArray( new BetaNodeFieldConstraint[list.size()] ),
                                                         context.getKnowledgeBase().getConfiguration(),
                                                         disableIndexing );
                break;
            case 4 :
                constraints = BetaNodeConstraintFactory.Factory.get().createQuadroupleBetaConstraints( list.toArray( new BetaNodeFieldConstraint[list.size()] ),
                                                             context.getKnowledgeBase().getConfiguration(),
                                                             disableIndexing );
                break;
            default :
                constraints = BetaNodeConstraintFactory.Factory.get().createDefaultBetaConstraints( list.toArray( new BetaNodeFieldConstraint[list.size()] ),
                                                          context.getKnowledgeBase().getConfiguration(),
                                                          disableIndexing );
        }
        return constraints;
    }

    /**
     * Calculates the temporal distance between all event patterns in the given 
     * subrule.
     * 
     * @param groupElement the root element of a subrule being added to the rulebase
     */
    public TemporalDependencyMatrix calculateTemporalDistance(GroupElement groupElement) {
        // find the events
        List<Pattern> events = new ArrayList<Pattern>();
        selectAllEventPatterns( events,
                                groupElement );

        final int size = events.size();
        if ( size >= 1 ) {
            // create the matrix
            Interval[][] source = new Interval[size][];
            for ( int row = 0; row < size; row++ ) {
                source[row] = new Interval[size];
                for ( int col = 0; col < size; col++ ) {
                    if ( row == col ) {
                        source[row][col] = new Interval( 0,
                                                         0 );
                    } else {
                        source[row][col] = new Interval( Interval.MIN,
                                                         Interval.MAX );
                    }
                }
            }

            Interval[][] result;
            if ( size > 1 ) {
                List<Declaration> declarations = new ArrayList<>();
                int eventIndex = 0;
                // populate the matrix
                for ( Pattern event : events ) {
                    // references to other events are always backward references, so we can build the list as we go
                    declarations.add( event.getDeclaration() );
                    Map<Declaration, Interval> temporal = new HashMap<>();
                    gatherTemporalRelationships( event.getConstraints(),
                                                 temporal );
                    // intersects default values with the actual constrained intervals
                    for ( Map.Entry<Declaration, Interval> entry : temporal.entrySet() ) {
                        int targetIndex = declarations.indexOf( entry.getKey() );
                        Interval interval = entry.getValue();
                        source[targetIndex][eventIndex].intersect( interval );
                        Interval reverse = new Interval( interval.getUpperBound() == Long.MAX_VALUE ? Long.MIN_VALUE : -interval.getUpperBound(), 
                                                         interval.getLowerBound() == Long.MIN_VALUE ? Long.MAX_VALUE : -interval.getLowerBound() );
                        source[eventIndex][targetIndex].intersect( reverse );
                    }
                    eventIndex++;
                }
                result = TimeUtils.calculateTemporalDistance( source );
            } else {
                result = source;
            }
            return new TemporalDependencyMatrix( result, events );
        }
        return null;
    }

    private void gatherTemporalRelationships(List< ? > constraints,
                                             Map<Declaration, Interval> temporal) {
        for ( Object obj : constraints ) {
            if ( obj instanceof IntervalProviderConstraint) {
                IntervalProviderConstraint constr = (IntervalProviderConstraint) obj;
                if ( constr.isTemporal() ) {
                    // if a constraint already exists, calculate the intersection
                    Declaration[] decs = constr.getRequiredDeclarations();
                    // only calculate relationships to other event patterns
                    if( decs.length > 0 && decs[0].isPatternDeclaration() && decs[0].getPattern().getObjectType().isEvent() ) {
                        Declaration target = decs[0];
                        Interval interval = temporal.get( target );
                        if ( interval == null ) {
                            interval = constr.getInterval();
                            temporal.put( target,
                                          interval );
                        } else {
                            interval.intersect( constr.getInterval() );
                        }
                    }
                }
            } else if ( obj instanceof AbstractCompositeConstraint ) {
                gatherTemporalRelationships( Arrays.asList( ((AbstractCompositeConstraint) obj).getBetaConstraints() ),
                                             temporal );
            }
        }
    }

    private void selectAllEventPatterns(List<Pattern> events,
                                        RuleConditionElement rce) {
        if ( rce instanceof Pattern ) {
            Pattern p = (Pattern) rce;
            if ( p.getObjectType().isEvent() ) {
                events.add( p );
            }
        }
        for ( RuleConditionElement child : rce.getNestedElements() ) {
            selectAllEventPatterns( events,
                                    child );
        }
    }

}
