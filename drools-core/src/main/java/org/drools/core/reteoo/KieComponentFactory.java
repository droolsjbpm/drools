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

import java.io.Serializable;

import org.drools.core.base.DefaultKnowledgeHelperFactory;
import org.drools.core.base.FieldDataFactory;
import org.drools.core.base.FieldFactory;
import org.drools.core.base.KnowledgeHelperFactory;
import org.drools.core.common.AgendaFactory;
import org.drools.core.common.AgendaGroupFactory;
import org.drools.core.common.BeliefSystemFactory;
import org.drools.core.common.DefaultAgendaFactory;
import org.drools.core.common.PhreakBeliefSystemFactory;
import org.drools.core.common.PhreakPropagationContextFactory;
import org.drools.core.common.PhreakWorkingMemoryFactory;
import org.drools.core.common.PriorityQueueAgendaGroupFactory;
import org.drools.core.common.PropagationContextFactory;
import org.drools.core.common.WorkingMemoryFactory;
import org.drools.core.factmodel.ClassBuilderFactory;
import org.drools.core.factmodel.traits.TraitFactory;
import org.drools.core.factmodel.traits.TraitProxy;
import org.drools.core.factmodel.traits.TraitRegistry;
import org.drools.core.reteoo.builder.NodeFactory;
import org.drools.core.reteoo.builder.PhreakNodeFactory;
import org.drools.core.rule.DefaultLogicTransformerFactory;
import org.drools.core.rule.LogicTransformerFactory;
import org.drools.core.spi.FactHandleFactory;
import org.drools.core.util.TripleFactory;
import org.drools.core.util.TripleFactoryImpl;
import org.drools.core.util.TripleStore;

public class KieComponentFactory implements Serializable {

    public static final KieComponentFactory DEFAULT = new KieComponentFactory();

    public static KieComponentFactory getDefault() {
        return DEFAULT;
    }

    private FactHandleFactory handleFactory = new ReteooFactHandleFactory();


    public FactHandleFactory getFactHandleFactoryService() {
         return handleFactory;
    }

    public void setHandleFactoryProvider( FactHandleFactory provider ) {
        handleFactory = provider;
    }


    private WorkingMemoryFactory wmFactory = PhreakWorkingMemoryFactory.getInstance();

    public WorkingMemoryFactory getWorkingMemoryFactory() {
        return wmFactory;
    }


    private NodeFactory nodeFactory = PhreakNodeFactory.getInstance();

    public NodeFactory getNodeFactoryService() {
        return nodeFactory;
    }

    public void setNodeFactoryProvider(NodeFactory provider) {
        nodeFactory = provider;
    }


    private PropagationContextFactory propagationFactory = PhreakPropagationContextFactory.getInstance();

    public PropagationContextFactory getPropagationContextFactory() {
        return propagationFactory;
    }


    private BeliefSystemFactory bsFactory = new PhreakBeliefSystemFactory();

    public BeliefSystemFactory getBeliefSystemFactory() {
        return bsFactory;
    }

    private RuleBuilderFactory ruleBuilderFactory = new ReteooRuleBuilderFactory();

    public RuleBuilderFactory getRuleBuilderFactory() {
        return ruleBuilderFactory;
    }


    private AgendaFactory agendaFactory = DefaultAgendaFactory.getInstance();

    public AgendaFactory getAgendaFactory() {
        return agendaFactory;
    }


    private AgendaGroupFactory agendaGroupFactory = PriorityQueueAgendaGroupFactory.getInstance();

    public AgendaGroupFactory getAgendaGroupFactory() {
        return agendaGroupFactory;
    }


    private FieldDataFactory fieldFactory = FieldFactory.getInstance();

    public FieldDataFactory getFieldFactory() {
        return fieldFactory;
    }


    private TripleFactory tripleFactory = new TripleFactoryImpl();

    public TripleFactory getTripleFactory() {
        return tripleFactory;
    }


    private KnowledgeHelperFactory knowledgeHelperFactory = new DefaultKnowledgeHelperFactory();

    public KnowledgeHelperFactory getKnowledgeHelperFactory() {
        return knowledgeHelperFactory;
    }


    private LogicTransformerFactory logicTransformerFactory = new DefaultLogicTransformerFactory();

    public LogicTransformerFactory getLogicTransformerFactory() {
        return logicTransformerFactory;
    }


    private TraitFactory traitFactory = new TraitFactory();

    public TraitFactory getTraitFactory() {
        return traitFactory;
    }


    private TraitRegistry traitRegistry;

    public TraitRegistry getTraitRegistry() {
        if ( traitRegistry == null ) {
            traitRegistry = new TraitRegistry();
        }
        return traitRegistry;
    }


    private TripleStore tripleStore = new TripleStore();

    public TripleStore getTripleStore() {
        return tripleStore;
    }


    private ClassBuilderFactory classBuilderFactory = new ClassBuilderFactory();

    public ClassBuilderFactory getClassBuilderFactory() {
        return classBuilderFactory;
    }


    private Class<?> baseTraitProxyClass = TraitProxy.class;

    public Class<?> getBaseTraitProxyClass() {
        return baseTraitProxyClass;
    }

}
