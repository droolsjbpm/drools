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

import org.drools.core.rule.Forall;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.GroupElementFactory;
import org.drools.core.rule.Pattern;
import org.drools.core.rule.RuleConditionElement;

/**
 * The Reteoo component builder for forall CE
 */
public class ForallBuilder
    implements
    ReteooComponentBuilder {

    /**
     * @inheritDoc
     */
    public void build(final BuildContext context,
                      final BuildUtils utils,
                      final RuleConditionElement rce) {
        final Forall forall = (Forall) rce;
        context.pushRuleComponent( forall );

        // forall can be translated into
        // not( basePattern and not( <remaining_patterns>+ ) ) 
        // so we just do that:

        final GroupElement and = GroupElementFactory.newAndInstance();
        and.addChild( forall.getBasePattern() );

        final GroupElement not2 = GroupElementFactory.newNotInstance();
        if ( forall.getRemainingPatterns().size() == 1 ) {
            if ( forall.isEmptyBetaConstraints() ) {
                // The reason why this is here is because forall can inject a
                //  "this == " + BASE_IDENTIFIER $__forallBaseIdentifier
                // Which we don't want to actually count in the case of forall node linking                
                context.setEmptyForAllBetaConstraints();
            }
            not2.addChild( forall.getRemainingPatterns().get( 0 ) );
            and.addChild( not2 );
        } else if ( forall.getRemainingPatterns().size() > 1 ) {
            final GroupElement and2 = GroupElementFactory.newAndInstance();
            for ( Pattern pattern : forall.getRemainingPatterns() ) {
                and2.addChild( pattern );
            }
            not2.addChild( and2 );
            and.addChild( not2 );
        }

        final GroupElement not = GroupElementFactory.newNotInstance();
        not.addChild( and );

        // get builder for the CEs
        final ReteooComponentBuilder builder = utils.getBuilderFor( not );

        // builds the CEs
        builder.build( context,
                       utils,
                       not );
        
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
