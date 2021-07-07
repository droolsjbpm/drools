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

import org.drools.core.common.BetaConstraints;
import org.drools.core.reteoo.AccumulateNode;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.RightInputAdapterNode;
import org.drools.core.rule.Accumulate;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.RuleConditionElement;
import org.drools.core.spi.AlphaNodeFieldConstraint;
import org.drools.core.spi.BetaNodeFieldConstraint;

import java.util.ArrayList;
import java.util.List;

public class AccumulateBuilder
        implements
        ReteooComponentBuilder {

    /**
     * @inheritDoc
     */
    public void build(final BuildContext context,
                      final BuildUtils utils,
                      final RuleConditionElement rce) {
        final Accumulate accumulate = (Accumulate) rce;
        context.pushRuleComponent( accumulate );

        final List<BetaNodeFieldConstraint> resultBetaConstraints = context.getBetaconstraints();
        final List<AlphaNodeFieldConstraint> resultAlphaConstraints = context.getAlphaConstraints();

        RuleConditionElement source = accumulate.getSource();
        if( source instanceof GroupElement ) {
            GroupElement ge = (GroupElement) source;
            if( ge.isAnd() && ge.getChildren().size() == 1 ) {
                source = ge.getChildren().get( 0 );
            }
        }

        // get builder for the pattern
        final ReteooComponentBuilder builder = utils.getBuilderFor( source );

        // save tuple source and current pattern offset for later if needed
        LeftTupleSource tupleSource = context.getTupleSource();

        // builds the source pattern
        builder.build( context,
                       utils,
                       source );

        // if object source is null, then we need to adapt tuple source into a subnetwork
        if ( context.getObjectSource() == null ) {
            // attach right input adapter node to convert tuple source into an object source
            RightInputAdapterNode riaNode = context.getComponentFactory().getNodeFactoryService().buildRightInputNode( context.getNextId(),
                                                                                                                       context.getTupleSource(),
                                                                                                                       tupleSource,
                                                                                                                       context );

            // attach right input adapter node to convert tuple source into an object source
            context.setObjectSource( utils.attachNode( context, riaNode ) );

            // restore tuple source from before the start of the sub network
            context.setTupleSource( tupleSource );

            // create a tuple start equals constraint and set it in the context
            final List<BetaNodeFieldConstraint> betaConstraints = new ArrayList<>();
            context.setBetaconstraints( betaConstraints ); // Empty list ensures EmptyBetaConstraints is assigned
        }

        NodeFactory nfactory = context.getComponentFactory().getNodeFactoryService();

        final BetaConstraints resultsBinder = utils.createBetaNodeConstraint( context,
                                                                              resultBetaConstraints,
                                                                              true );
        final BetaConstraints sourceBinder = utils.createBetaNodeConstraint( context,
                                                                             context.getBetaconstraints(),
                                                                             false );

        AccumulateNode accNode = nfactory.buildAccumulateNode(context.getNextId(),
                                                              context.getTupleSource(),
                                                              context.getObjectSource(),
                                                              resultAlphaConstraints.toArray(new AlphaNodeFieldConstraint[resultAlphaConstraints.size()]),
                                                              sourceBinder,
                                                              resultsBinder,
                                                              accumulate,
                                                              context);

        context.setTupleSource( utils.attachNode( context, accNode ) );

        // source pattern was bound, so nulling context
        context.setObjectSource( null );
        context.popRuleComponent();
    }

    /**
     * @inheritDoc
     */
    public boolean requiresLeftActivation(final BuildUtils utils,
                                          final RuleConditionElement rce) {
        return true;
    }

}
