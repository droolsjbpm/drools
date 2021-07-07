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

import org.drools.core.rule.Declaration;
import org.drools.core.rule.RuleConditionElement;
import org.drools.core.time.impl.BaseTimer;
import org.drools.core.time.impl.Timer;

public class TimerBuilder
    implements
    ReteooComponentBuilder {

    public void build(final BuildContext context,
                      final BuildUtils utils,
                      final RuleConditionElement rce) {
        final Timer timer = (Timer) rce;
        context.pushRuleComponent( timer );

        Declaration[][] declrs = timer instanceof BaseTimer ?
                                 ((BaseTimer)timer).getTimerDeclarations(context.getSubRule().getOuterDeclarations()) :
                                 null;

        context.setTupleSource( utils.attachNode( context,
                                context.getComponentFactory().getNodeFactoryService().buildTimerNode( context.getNextId(),
                                                                                                      timer,
                                                                                                      context.getRule().getCalendars(),
                                                                                                      declrs,
                                                                                                      context.getTupleSource(),
                                                                                                      context  ) ) );

        context.setAlphaConstraints( null );
        context.setBetaconstraints( null );
        context.popRuleComponent();
    }

    public boolean requiresLeftActivation(final BuildUtils utils,
                                          final RuleConditionElement rce) {
        return true;
    }

}
