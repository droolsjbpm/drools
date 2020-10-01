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

package org.drools.core.marshalling.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ExtensionRegistry;
import org.drools.core.SessionConfiguration;
import org.drools.core.SessionConfigurationImpl;
import org.drools.core.WorkingMemoryEntryPoint;
import org.drools.core.common.ActivationsFilter;
import org.drools.core.common.AgendaGroupQueueImpl;
import org.drools.core.common.DefaultFactHandle;
import org.drools.core.common.EqualityKey;
import org.drools.core.common.EventFactHandle;
import org.drools.core.common.InternalAgenda;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.NamedEntryPoint;
import org.drools.core.common.ObjectStore;
import org.drools.core.common.PropagationContextFactory;
import org.drools.core.common.QueryElementFactHandle;
import org.drools.core.common.TruthMaintenanceSystem;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.impl.EnvironmentFactory;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.drools.core.marshalling.impl.ProtobufMessages.FactHandle;
import org.drools.core.marshalling.impl.ProtobufMessages.ObjectTypeConfiguration;
import org.drools.core.marshalling.impl.ProtobufMessages.RuleData;
import org.drools.core.marshalling.impl.ProtobufMessages.Timers.Timer;
import org.drools.core.phreak.PhreakTimerNode.Scheduler;
import org.drools.core.phreak.RuleAgendaItem;
import org.drools.core.phreak.RuleExecutor;
import org.drools.core.process.instance.WorkItem;
import org.drools.core.reteoo.ObjectTypeConf;
import org.drools.core.reteoo.TerminalNode;
import org.drools.core.rule.EntryPointId;
import org.drools.core.spi.Activation;
import org.drools.core.spi.FactHandleFactory;
import org.drools.core.spi.GlobalResolver;
import org.drools.core.spi.PropagationContext;
import org.drools.core.spi.Tuple;
import org.drools.core.time.Trigger;
import org.drools.core.time.impl.CompositeMaxDurationTrigger;
import org.drools.core.time.impl.CronTrigger;
import org.drools.core.time.impl.IntervalTrigger;
import org.drools.core.time.impl.PointInTimeTrigger;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.api.runtime.rule.Match;

/**
 * An input marshaller that uses protobuf. 
 * 
 */
public class ProtobufInputMarshaller {
    // NOTE: all variables prefixed with _ (underscore) are protobuf structs

    private static ProcessMarshaller processMarshaller = createProcessMarshaller();

    private static ProcessMarshaller createProcessMarshaller() {
        try {
            return ProcessMarshallerFactory.newProcessMarshaller();
        } catch ( IllegalArgumentException e ) {
            return null;
        }
    }

    /**
     * Stream the data into an existing session
     * 
     * @param session
     * @param context
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static StatefulKnowledgeSessionImpl readSession(StatefulKnowledgeSessionImpl session,
                                                    MarshallerReaderContext context) throws IOException,
                                                                                    ClassNotFoundException {

        ProtobufMessages.KnowledgeSession _session = loadAndParseSession( context );

        InternalAgenda agenda = resetSession( session,
                                             context,
                                             _session );

        readSession( _session,
                     session,
                     agenda,
                     context );

        return session;
    }

    /**
     * Create a new session into which to read the stream data
     */
    public static ReadSessionResult readSession(MarshallerReaderContext context,
                                                           int id) throws IOException, ClassNotFoundException {
        return readSession( context,
                            id,
                            EnvironmentFactory.newEnvironment(),
                            new SessionConfigurationImpl() );
    }

    public static ReadSessionResult readSession(MarshallerReaderContext context,
                                                           int id,
                                                           Environment environment,
                                                           SessionConfiguration config) throws IOException, ClassNotFoundException {
        return readSession( context, id, environment, config, null );
    }

    public static ReadSessionResult readSession(MarshallerReaderContext context,
                                                           int id,
                                                           Environment environment,
                                                           SessionConfiguration config,
                                                           KieSessionInitializer initializer) throws IOException, ClassNotFoundException {

        ProtobufMessages.KnowledgeSession _session = loadAndParseSession( context );

        StatefulKnowledgeSessionImpl session = createAndInitializeSession( context,
                                                                           id,
                                                                           environment,
                                                                           config,
                                                                           _session );
        // Initialize the session before unmarshalling data
        if (initializer != null) {
            initializer.init( session );
        }

        return new ReadSessionResult(readSession(_session,
                                                 session,
                                                 session.getAgenda(),
                                                 context),
                                     _session);
    }

    private static InternalAgenda resetSession(StatefulKnowledgeSessionImpl session,
                                              MarshallerReaderContext context,
                                              ProtobufMessages.KnowledgeSession _session) {
        session.reset( _session.getRuleData().getLastId(),
                       _session.getRuleData().getLastRecency(),
                       1 );
        InternalAgenda agenda = (InternalAgenda) session.getAgenda();

        readAgenda( context,
                    _session.getRuleData(),
                    agenda );
        return agenda;
    }

    private static StatefulKnowledgeSessionImpl createAndInitializeSession(MarshallerReaderContext context,
                                                                    int id,
                                                                    Environment environment,
                                                                    SessionConfiguration config,
                                                                    ProtobufMessages.KnowledgeSession _session) throws IOException {
        FactHandleFactory handleFactory = context.kBase.newFactHandleFactory( _session.getRuleData().getLastId(),
                                                                                 _session.getRuleData().getLastRecency() );

        InternalAgenda agenda = context.kBase.getConfiguration().getComponentFactory().getAgendaFactory().createAgenda( context.kBase, false );
        readAgenda( context, _session.getRuleData(), agenda );

        return context.kBase.createSession( id, handleFactory,
                                            1, // pCTx starts at 1, as InitialFact is 0
                                            config, agenda, environment );
    }

    private static ProtobufMessages.KnowledgeSession loadAndParseSession(MarshallerReaderContext context) throws IOException,
                                                                                                         ClassNotFoundException {
        ExtensionRegistry registry = PersisterHelper.buildRegistry( context, processMarshaller );

        ProtobufMessages.Header _header = PersisterHelper.readFromStreamWithHeaderPreloaded( context, registry );

        return ProtobufMessages.KnowledgeSession.parseFrom( _header.getPayload(), registry );
    }

    public static StatefulKnowledgeSessionImpl readSession(ProtobufMessages.KnowledgeSession _session,
                                                           StatefulKnowledgeSessionImpl session,
                                                    InternalAgenda agenda,
                                                    MarshallerReaderContext context) throws IOException,
                                                                                    ClassNotFoundException {
        GlobalResolver globalResolver = (GlobalResolver) context.env.get( EnvironmentName.GLOBALS );
        if ( globalResolver != null ) {
            session.setGlobalResolver( globalResolver );
        }

        if ( session.getTimerService() instanceof PseudoClockScheduler ) {
            PseudoClockScheduler clock = (PseudoClockScheduler) session.getTimerService();
            clock.advanceTime( _session.getTime(),
                               TimeUnit.MILLISECONDS );
        }

        context.wm = session;

        // need to read node memories before reading the fact handles
        // because this data is required during fact propagation 
        readNodeMemories( context,
                          _session.getRuleData() );

        List<PropagationContext> pctxs = new ArrayList<PropagationContext>();

        if ( _session.getRuleData().hasInitialFact() ) {
            session.setInitialFactHandle( session.initInitialFact(context.kBase, context) );
            context.handles.put( session.getInitialFactHandle().getId(), session.getInitialFactHandle() );
        }

        for ( ProtobufMessages.EntryPoint _ep : _session.getRuleData().getEntryPointList() ) {
            EntryPoint wmep = ((StatefulKnowledgeSessionImpl)context.wm).getEntryPointMap().get(_ep.getEntryPointId());
            readFactHandles( context,
                             _ep,
                             ((WorkingMemoryEntryPoint) wmep).getObjectStore(),
                             pctxs );

            context.wm.getFactHandleFactory().doRecycleIds( context.handles.keySet() );

            context.filter.fireRNEAs( context.wm );

            readTruthMaintenanceSystem( context,
                                        wmep,
                                        _ep,
                                        pctxs );

            context.wm.getFactHandleFactory().stopRecycleIds();
        }

        cleanReaderContexts( pctxs );

        readActionQueue( context,
                         _session.getRuleData() );

        if ( processMarshaller != null ) {
            if ( _session.hasProcessData() ) {
                context.parameterObject = _session.getProcessData();
                processMarshaller.readProcessInstances( context );

                context.parameterObject = _session.getProcessData();
                processMarshaller.readWorkItems( context );

                // This actually does ALL timers, due to backwards compatability issues
                // It will read in old JBPM binaries, but always write to the new binary format.
                context.parameterObject = _session.getProcessData();
                processMarshaller.readProcessTimers( context );
            }
        } else {
            if ( _session.hasProcessData() ) {
                throw new IllegalStateException( "No process marshaller, unable to unmarshall process data." );
            }
        }

        if ( _session.hasTimers() ) {
            for ( ProtobufMessages.Timers.Timer _timer : _session.getTimers().getTimerList() ) {
                readTimer( context,
                           _timer );
            }
        }
        // need to process any eventual left over timer node timers
        if( ! context.timerNodeSchedulers.isEmpty() ) {
            for( Map<TupleKey, Scheduler> schedulers : context.timerNodeSchedulers.values() ) {
                for( Scheduler scheduler : schedulers.values() ) {
                    scheduler.schedule( scheduler.getTrigger() );
                }
            }
            context.timerNodeSchedulers.clear();
        }

        // remove the activations filter
        agenda.setActivationsFilter( null );

        return session;
    }

    private static void readNodeMemories(MarshallerReaderContext context,
                                         RuleData _session) {
        for ( ProtobufMessages.NodeMemory _node : _session.getNodeMemoryList() ) {
            Object memory = null;
            switch ( _node.getNodeType() ) {
                // ACCUMULATE, RIA and FROM memories are no longer serialized, so the following 3 cases are useless for
                // new serialized session, but are still necessary for sessions serialized before the marshalling refactor
                case ACCUMULATE : {
                    Map<TupleKey, ProtobufMessages.FactHandle> map = new HashMap<TupleKey, ProtobufMessages.FactHandle>();
                    for ( ProtobufMessages.NodeMemory.AccumulateNodeMemory.AccumulateContext _ctx : _node.getAccumulate().getContextList() ) {
                        map.put( PersisterHelper.createTupleKey( _ctx.getTuple() ), _ctx.getResultHandle() );
                    }
                    context.withSerializedNodeMemories();
                    memory = map;
                    break;
                }
                case RIA : {
                    Map<TupleKey, ProtobufMessages.FactHandle> map = new HashMap<TupleKey, ProtobufMessages.FactHandle>();
                    for ( ProtobufMessages.NodeMemory.RIANodeMemory.RIAContext _ctx : _node.getRia().getContextList() ) {
                        map.put( PersisterHelper.createTupleKey( _ctx.getTuple() ), _ctx.getResultHandle() );
                    }
                    context.withSerializedNodeMemories();
                    memory = map;
                    break;
                }
                case FROM : {
                    Map<TupleKey, List<ProtobufMessages.FactHandle>> map = new HashMap<>();
                    for ( ProtobufMessages.NodeMemory.FromNodeMemory.FromContext _ctx : _node.getFrom().getContextList() ) {
                        // have to instantiate a modifiable list
                        map.put( PersisterHelper.createTupleKey( _ctx.getTuple() ), new LinkedList<>(_ctx.getHandleList()) );
                    }
                    context.withSerializedNodeMemories();
                    memory = map;
                    break;
                }
                case QUERY_ELEMENT : {
                    Map<TupleKey, QueryElementContext> map = new HashMap<TupleKey, QueryElementContext>();
                    for ( ProtobufMessages.NodeMemory.QueryElementNodeMemory.QueryContext _ctx : _node.getQueryElement().getContextList() ) {
                        // we have to use a "cloned" query element context as we need to write on it during deserialization process and the 
                        // protobuf one is read-only
                        map.put( PersisterHelper.createTupleKey( _ctx.getTuple() ), new QueryElementContext( _ctx ) );
                    }
                    memory = map;
                    break;
                }
                default : {
                    throw new IllegalArgumentException( "Unknown node type " + _node.getNodeType() + " while deserializing session." );
                }
            }
            context.getNodeMemories().put( _node.getNodeId(), memory );
        }
    }

    public static class QueryElementContext {
        public final ProtobufMessages.FactHandle             handle;
        public final LinkedList<ProtobufMessages.FactHandle> results;

        public QueryElementContext(ProtobufMessages.NodeMemory.QueryElementNodeMemory.QueryContext _ctx) {
            this.handle = _ctx.getHandle();
            this.results = new LinkedList<ProtobufMessages.FactHandle>( _ctx.getResultList() );
        }
    }

    private static void readInitialFactHandle(MarshallerReaderContext context,
                                              RuleData _session,
                                              List<PropagationContext> pctxs) {
        long ifhId = context.wm.getInitialFactHandle().getId();
        context.handles.put( ifhId,
                             context.wm.getInitialFactHandle() );

        // special case we have to handle for the initial fact
        boolean initialFactPropagated = true;
        for ( ProtobufMessages.ActionQueue.Action _action : _session.getActionQueue().getActionList() ) {
            if ( _action.getType() == ProtobufMessages.ActionQueue.ActionType.ASSERT ) {
                if ( _action.getAssert().getHandleId() == ifhId ) {
                    initialFactPropagated = false;
                    break;
                }
            }
        }
        if ( initialFactPropagated ) {
            assertHandleIntoOTN( context,
                                 context.wm,
                                 context.wm.getInitialFactHandle(),
                                 pctxs );
        }
    }

    public static void readAgenda(MarshallerReaderContext context,
                                  RuleData _ruleData,
                                  InternalAgenda agenda) {
        ProtobufMessages.Agenda _agenda = _ruleData.getAgenda();

        for ( org.drools.core.marshalling.impl.ProtobufMessages.Agenda.AgendaGroup _agendaGroup : _agenda.getAgendaGroupList() ) {
            AgendaGroupQueueImpl group = (AgendaGroupQueueImpl) agenda.getAgendaGroup( _agendaGroup.getName(), context.kBase );
            group.setActive( _agendaGroup.getIsActive() );
            group.setAutoDeactivate( _agendaGroup.getIsAutoDeactivate() );
            group.setClearedForRecency( _agendaGroup.getClearedForRecency() );
            group.hasRuleFlowListener( _agendaGroup.getHasRuleFlowLister() );
            group.setActivatedForRecency( _agendaGroup.getActivatedForRecency() );

            for ( org.drools.core.marshalling.impl.ProtobufMessages.Agenda.AgendaGroup.NodeInstance _nodeInstance : _agendaGroup.getNodeInstanceList() ) {
                group.addNodeInstance( _nodeInstance.hasProcessInstanceId() ? _nodeInstance.getProcessInstanceId() : _nodeInstance.getProcessInstanceStringId(),
                                       _nodeInstance.getNodeInstanceId() );
            }
            agenda.getAgendaGroupsMap().put( group.getName(),
                                             group );
        }
        
        for ( String _groupName : _agenda.getFocusStack().getGroupNameList() ) {
            agenda.addAgendaGroupOnStack( agenda.getAgendaGroup( _groupName ) );
        }
        
        for ( ProtobufMessages.Agenda.RuleFlowGroup _ruleFlowGroup : _agenda.getRuleFlowGroupList() ) {
            AgendaGroupQueueImpl group = (AgendaGroupQueueImpl) agenda.getAgendaGroup( _ruleFlowGroup.getName(), context.kBase );
            group.setActive( _ruleFlowGroup.getIsActive() );
            group.setAutoDeactivate( _ruleFlowGroup.getIsAutoDeactivate() );
            

            for ( org.drools.core.marshalling.impl.ProtobufMessages.Agenda.RuleFlowGroup.NodeInstance _nodeInstance : _ruleFlowGroup.getNodeInstanceList() ) {
                group.addNodeInstance( _nodeInstance.getProcessInstanceId(),
                                       _nodeInstance.getNodeInstanceId() );
            }
            agenda.getAgendaGroupsMap().put( group.getName(),
                                             group );
            if (group.isActive()) {
                agenda.addAgendaGroupOnStack( agenda.getAgendaGroup( group.getName() ) );
            }
        }

        

        readActivations( context,
                         _agenda.getMatchList(),
                         _agenda.getRuleActivationList() );
        agenda.setActivationsFilter( context.filter );
    }

    public static void readActionQueue(MarshallerReaderContext context,
                                       RuleData _session) throws IOException,
                                                         ClassNotFoundException {
        StatefulKnowledgeSessionImpl wm = (StatefulKnowledgeSessionImpl) context.wm;
        for ( ProtobufMessages.ActionQueue.Action _action : _session.getActionQueue().getActionList() ) {
            wm.addPropagation(PersisterHelper.deserializeWorkingMemoryAction(context, _action));
        }
    }

    public static void readFactHandles(MarshallerReaderContext context,
                                       org.drools.core.marshalling.impl.ProtobufMessages.EntryPoint _ep,
                                       ObjectStore objectStore,
                                       List<PropagationContext> pctxs) throws IOException,
                                                                          ClassNotFoundException {
        InternalWorkingMemory wm = context.wm;

        EntryPoint entryPoint = ((StatefulKnowledgeSessionImpl)context.wm).getEntryPointMap().get(_ep.getEntryPointId());
        
        // load the handles
        for ( ProtobufMessages.FactHandle _handle : _ep.getHandleList() ) {
            InternalFactHandle handle = readFactHandle( context,
                                                        entryPoint,
                                                        _handle );

            context.handles.put( handle.getId(),
                                 handle );

            if ( !_handle.getIsJustified() ) {
                // BeliefSystem handles the Object type 
                if ( handle.getObject() != null ) {
                    objectStore.addHandle( handle,
                                           handle.getObject() );
                }

                // add handle to object type node
                assertHandleIntoOTN( context,
                                     wm,
                                     handle,
                                     pctxs );
            }
        }

    }

    private static void assertHandleIntoOTN(MarshallerReaderContext context,
                                            InternalWorkingMemory wm,
                                            InternalFactHandle handle,
                                            List<PropagationContext> pctxs) {
        Object object = handle.getObject();
        WorkingMemoryEntryPoint ep = handle.getEntryPoint(wm);
        ObjectTypeConf typeConf = ep.getObjectTypeConfigurationRegistry().getObjectTypeConf( ep.getEntryPoint(), object );

        PropagationContextFactory pctxFactory = wm.getKnowledgeBase().getConfiguration().getComponentFactory().getPropagationContextFactory();

        PropagationContext propagationContext = pctxFactory.createPropagationContext(wm.getNextPropagationIdCounter(), PropagationContext.Type.INSERTION, null, null, handle, ep.getEntryPoint(), context);
        // keeping this list for a later cleanup is necessary because of the lazy propagations that might occur
        pctxs.add( propagationContext );

        ep.getEntryPointNode().assertObject( handle,
                                             propagationContext,
                                             typeConf,
                                             wm );

        wm.flushPropagations();
    }

    private static void cleanReaderContexts(List<PropagationContext> pctxs) {
        for ( PropagationContext ctx : pctxs ) {
            ctx.cleanReaderContext();
        }
    }

    public static InternalFactHandle readFactHandle(MarshallerReaderContext context,
                                                    EntryPoint entryPoint,
                                                    FactHandle _handle) throws IOException,
                                                                       ClassNotFoundException {
        Object object = null;
        ObjectMarshallingStrategy strategy = null;
        if ( _handle.hasStrategyIndex() ) {
            strategy = context.usedStrategies.get( _handle.getStrategyIndex() );
            object = strategy.unmarshal( context.strategyContexts.get( strategy ),
                                         context,
                                         _handle.getObject().toByteArray(),
                                         (context.kBase == null) ? null : context.kBase.getRootClassLoader() );
        }


        EntryPointId confEP;
        if ( entryPoint != null ) {
            confEP = ((NamedEntryPoint) entryPoint).getEntryPoint();
        } else {
            confEP = context.wm.getEntryPoint();
        }
        ObjectTypeConf typeConf = context.wm.getObjectTypeConfigurationRegistry().getObjectTypeConf( confEP, object );


        InternalFactHandle handle = null;
        switch ( _handle.getType() ) {
            case FACT : {
                handle = new DefaultFactHandle( _handle.getId(),
                                                object,
                                                _handle.getRecency(),
                                                (WorkingMemoryEntryPoint) entryPoint,
                                                typeConf != null && typeConf.isTrait() );
                break;
            }
            case QUERY : {
                handle = new QueryElementFactHandle( object,
                                                     _handle.getId(),
                                                     _handle.getRecency() );
                break;
            }
            case EVENT : {
                handle = new EventFactHandle( _handle.getId(),
                                              object,
                                              _handle.getRecency(),
                                              _handle.getTimestamp(),
                                              _handle.getDuration(),
                                              (WorkingMemoryEntryPoint) entryPoint,
                                              typeConf != null && typeConf.isTrait() );
                ((EventFactHandle) handle).setExpired( _handle.getIsExpired() );
                ((EventFactHandle) handle).setOtnCount( _handle.getOtnCount() );
                // the event is re-propagated through the network, so the activations counter will be recalculated
                //((EventFactHandle) handle).setActivationsCount( _handle.getActivationsCount() );
                break;
            }
            default : {
                throw new IllegalStateException( "Unable to marshal FactHandle, as type does not exist:" + _handle.getType() );
            }
        }
        return handle;
    }

    public static void readTruthMaintenanceSystem(MarshallerReaderContext context,
                                                  EntryPoint wmep,
                                                  ProtobufMessages.EntryPoint _ep,
                                                  List<PropagationContext> pctxs) throws IOException,
                                                                                     ClassNotFoundException {
        TruthMaintenanceSystem tms = ((NamedEntryPoint) wmep).getTruthMaintenanceSystem();
        
        boolean wasOTCSerialized = _ep.getOtcCount() > 0; // if 0, then the OTC was not serialized (older versions of drools)
        Set<String> tmsEnabled = new HashSet<String>();
        for( ObjectTypeConfiguration _otc : _ep.getOtcList() ) {
        	if( _otc.getTmsEnabled() ) {
            	tmsEnabled.add( _otc.getType() );
        	}
        }

        ProtobufMessages.TruthMaintenanceSystem _tms = _ep.getTms();

        for ( ProtobufMessages.EqualityKey _key : _tms.getKeyList() ) {
            InternalFactHandle handle = (InternalFactHandle) context.handles.get( _key.getHandleId() );

            // ObjectTypeConf state is not marshalled, so it needs to be re-determined
            ObjectTypeConf typeConf = context.wm.getObjectTypeConfigurationRegistry().getObjectTypeConf( handle.getEntryPointId(),
                                                                                                         handle.getObject() );
            if ( !typeConf.isTMSEnabled() && (!wasOTCSerialized || tmsEnabled.contains(typeConf.getTypeName()) ) ) {
                typeConf.enableTMS();
            }

            EqualityKey key = new EqualityKey( handle,
                                               _key.getStatus() );
            handle.setEqualityKey( key );

            if ( key.getStatus() == EqualityKey.JUSTIFIED ) {
                // not yet added to the object stores
                ((NamedEntryPoint) handle.getEntryPoint((( NamedEntryPoint ) wmep).getInternalWorkingMemory())).getObjectStore()
                        .addHandle( handle, handle.getObject() );
                // add handle to object type node
                assertHandleIntoOTN( context,
                                     context.wm,
                                     handle,
                                     pctxs );
            }

            for ( Long factHandleId : _key.getOtherHandleList() ) {
                handle = context.handles.get( factHandleId );
                key.addFactHandle( handle );
                handle.setEqualityKey( key );
            }
            tms.put( key );

            context.filter.fireRNEAs( context.wm );
            readBeliefSet( context, tms, key, _key );
        }

    }

    private static void readBeliefSet(MarshallerReaderContext context,
                                      TruthMaintenanceSystem tms,
                                      EqualityKey key,
                                      ProtobufMessages.EqualityKey _key) throws IOException,
                                                                            ClassNotFoundException {
        if( _key.hasBeliefSet() ) {
            ProtobufMessages.BeliefSet _beliefSet = _key.getBeliefSet();
            InternalFactHandle handle = (InternalFactHandle) context.handles.get( _key.getHandleId() );
            // phreak might serialize empty belief sets, so he have to handle it during deserialization 
            if( _beliefSet.getLogicalDependencyCount() > 0 ) {
                for ( ProtobufMessages.LogicalDependency _logicalDependency : _beliefSet.getLogicalDependencyList() ) {
                    ProtobufMessages.Activation _activation = _logicalDependency.getActivation();
                    ActivationKey activationKey = PersisterHelper.createActivationKey(_activation.getPackageName(),
                                                                                      _activation.getRuleName(),
                                                                                      _activation.getTuple());
                    Activation activation = (Activation) context.filter.getTuplesCache().get(activationKey).getContextObject();

                    Object object = null;
                    ObjectMarshallingStrategy strategy = null;
                    if ( _logicalDependency.hasObjectStrategyIndex() ) {
                        strategy = context.usedStrategies.get( _logicalDependency.getObjectStrategyIndex() );
                        object = strategy.unmarshal( context.strategyContexts.get( strategy ),
                                                     context,
                                                     _logicalDependency.getObject().toByteArray(),
                                                     (context.kBase == null) ? null : context.kBase.getRootClassLoader() );
                    }

                    Object value = null;
                    if ( _logicalDependency.hasValueStrategyIndex() ) {
                        strategy = context.usedStrategies.get( _logicalDependency.getValueStrategyIndex() );
                        value = strategy.unmarshal( context.strategyContexts.get( strategy ),
                                                    context,
                                                    _logicalDependency.getValue().toByteArray(),
                                                    (context.kBase == null) ? null : context.kBase.getRootClassLoader() );
                    }

                    ObjectTypeConf typeConf = context.wm.getObjectTypeConfigurationRegistry().getObjectTypeConf( handle.getEntryPointId(),
                                                                                                                 handle.getObject() );
                    tms.readLogicalDependency( handle,
                                               object,
                                               value,
                                               activation,
                                               activation.getPropagationContext(),
                                               activation.getRule(),
                                               typeConf );
                }
            } else {
                handle.getEqualityKey().setBeliefSet( tms.getBeliefSystem().newBeliefSet( handle ) );
            }
        }
    }

    private static void readActivations(MarshallerReaderContext context,
                                        List<ProtobufMessages.Activation> _dormant,
                                        List<ProtobufMessages.Activation> _rneas) {

        for ( ProtobufMessages.Activation _activation : _dormant ) {
            ProtobufMessages.Tuple _tuple = _activation.getTuple();
            // this is a dormant activation
            ActivationKey activationKey;
            if (!_tuple.getObjectList().isEmpty()) {
                Object[] objects = new Object[_tuple.getObjectList().size()];
                int i = 0;
                for (ProtobufMessages.SerializedObject _object : _tuple.getObjectList()) {
                    ObjectMarshallingStrategy strategy = context.usedStrategies.get( _object.getStrategyIndex() );

                    try {
                        objects[i++] = strategy.unmarshal( context.strategyContexts.get( strategy ),
                                                           context,
                                                           _object.getObject().toByteArray(),
                                                           (context.kBase == null) ? null : context.kBase.getRootClassLoader() );
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException( e );
                    }
                }
                activationKey = PersisterHelper.createActivationKey( _activation.getPackageName(), _activation.getRuleName(), objects );

            } else {
                activationKey = PersisterHelper.createActivationKey(_activation.getPackageName(), _activation.getRuleName(), _tuple);
            }
            context.filter.addDormantActivation(activationKey);
        }

        for ( ProtobufMessages.Activation _activation : _rneas ) {
            // this is an active rule network evaluator
            context.filter.getRneActivations().put( PersisterHelper.createActivationKey( _activation.getPackageName(),
                                                                                         _activation.getRuleName(),
                                                                                         _activation.getTuple() ),
                                                    _activation );
        }
    }

    public static void readTimer(MarshallerReaderContext inCtx,
                                 Timer _timer) throws IOException,
                                              ClassNotFoundException {
        TimersInputMarshaller reader = inCtx.readersByInt.get( _timer.getType().getNumber() );
        reader.deserialize( inCtx, _timer );
    }

    public static Trigger readTrigger(MarshallerReaderContext inCtx,
                                      ProtobufMessages.Trigger _trigger) {
        switch ( _trigger.getType() ) {
            case CRON : {
                ProtobufMessages.Trigger.CronTrigger _cron = _trigger.getCron();
                CronTrigger trigger = new CronTrigger();
                trigger.setStartTime( new Date( _cron.getStartTime() ) );
                if ( _cron.hasEndTime() ) {
                    trigger.setEndTime( new Date( _cron.getEndTime() ) );
                }
                trigger.setRepeatLimit( _cron.getRepeatLimit() );
                trigger.setRepeatCount( _cron.getRepeatCount() );
                trigger.setCronExpression( _cron.getCronExpression() );
                if ( _cron.hasNextFireTime() ) {
                    trigger.setNextFireTime( new Date( _cron.getNextFireTime() ) );
                }
                String[] calendarNames = new String[_cron.getCalendarNameCount()];
                for ( int i = 0; i < calendarNames.length; i++ ) {
                    calendarNames[i] = _cron.getCalendarName( i );
                }
                trigger.setCalendarNames( calendarNames );
                return trigger;
            }
            case INTERVAL : {
                ProtobufMessages.Trigger.IntervalTrigger _interval = _trigger.getInterval();
                IntervalTrigger trigger = new IntervalTrigger();
                trigger.setStartTime( new Date( _interval.getStartTime() ) );
                if ( _interval.hasEndTime() ) {
                    trigger.setEndTime( new Date( _interval.getEndTime() ) );
                }
                trigger.setRepeatLimit( _interval.getRepeatLimit() );
                trigger.setRepeatCount( _interval.getRepeatCount() );
                if ( _interval.hasNextFireTime() ) {
                    long now = inCtx.wm.getSessionClock().getCurrentTime() + _interval.getPeriod();
                    long nextFireTime = Math.max(now, _interval.getNextFireTime());
                    trigger.setNextFireTime( new Date( nextFireTime ) );
                }
                trigger.setPeriod( _interval.getPeriod() );
                String[] calendarNames = new String[_interval.getCalendarNameCount()];
                for ( int i = 0; i < calendarNames.length; i++ ) {
                    calendarNames[i] = _interval.getCalendarName( i );
                }
                trigger.setCalendarNames( calendarNames );
                return trigger;
            }
            case POINT_IN_TIME : {
                PointInTimeTrigger trigger = PointInTimeTrigger.createPointInTimeTrigger( _trigger.getPit().getNextFireTime(), null );
                return trigger;
            }
            case COMPOSITE_MAX_DURATION : {
                ProtobufMessages.Trigger.CompositeMaxDurationTrigger _cmdTrigger = _trigger.getCmdt();
                CompositeMaxDurationTrigger trigger = new CompositeMaxDurationTrigger();
                if ( _cmdTrigger.hasMaxDurationTimestamp() ) {
                    trigger.setMaxDurationTimestamp( new Date( _cmdTrigger.getMaxDurationTimestamp() ) );
                }
                if ( _cmdTrigger.hasTimerCurrentDate() ) {
                    trigger.setTimerCurrentDate( new Date( _cmdTrigger.getTimerCurrentDate() ) );
                }
                if ( _cmdTrigger.hasTimerTrigger() ) {
                    trigger.setTimerTrigger( readTrigger( inCtx, _cmdTrigger.getTimerTrigger() ) );
                }
                return trigger;
            }
        }
        throw new RuntimeException( "Unable to deserialize Trigger for type: " + _trigger.getType() );

    }

    public static WorkItem readWorkItem( MarshallerReaderContext context ) {
        return processMarshaller.readWorkItem( context );
    }

    public static class PBActivationsFilter implements ActivationsFilter, AgendaFilter {

        private final Set<ActivationKey> dormantActivations = new HashSet<>();
        private final Map<ActivationKey, ProtobufMessages.Activation> rneActivations = new HashMap<>();
        private final Map<ActivationKey, Tuple> tuplesCache = new HashMap<>();
        private final Queue<RuleAgendaItem> rneaToFire = new ConcurrentLinkedQueue<>();

        private boolean serializedNodeMemories = false;

        public void addDormantActivation(ActivationKey key) {
            this.dormantActivations.add( key );
        }

        public boolean accept(Activation activation,
                              InternalWorkingMemory workingMemory,
                              TerminalNode rtn) {
            if ( activation.isRuleAgendaItem() ) {
                ActivationKey key = PersisterHelper.createActivationKey( activation.getRule().getPackageName(), activation.getRule().getName(), activation.getTuple() );
                if ( !this.rneActivations.containsKey( key ) || this.rneActivations.get( key ).getEvaluated() ) {
                    rneaToFire.add( (RuleAgendaItem) activation );
                }
                return true;
            } else {

                RuleImpl rule = activation.getRule();
                ActivationKey activationKey = PersisterHelper.hasNodeMemory( rtn ) && !serializedNodeMemories ?
                        PersisterHelper.createActivationKey( rule.getPackageName(), rule.getName(), activation.getTuple().toObjects(true)) :
                        PersisterHelper.createActivationKey( rule.getPackageName(), rule.getName(), activation.getTuple() );

                this.tuplesCache.put( activationKey, activation.getTuple() );

                return !dormantActivations.contains(activationKey);
            }
        }

        public Map<ActivationKey, Tuple> getTuplesCache() {
            return tuplesCache;
        }

        public Map<ActivationKey, ProtobufMessages.Activation> getRneActivations() {
            return rneActivations;
        }

        public void fireRNEAs(final InternalWorkingMemory wm) {
            RuleAgendaItem rai = null;
            while ( (rai = rneaToFire.poll()) != null ) {
                RuleExecutor ruleExecutor = rai.getRuleExecutor();
                ruleExecutor.reEvaluateNetwork( wm );
                ruleExecutor.removeRuleAgendaItemWhenEmpty( wm );
            }
        }

        @Override
        public boolean accept(Match match) {
            Tuple tuple = ((Activation)match).getTuple();
            ActivationKey key = PersisterHelper.createActivationKey( match.getRule().getPackageName(), 
                                                                     match.getRule().getName(),
                                                                     tuple );
            // add the tuple to the cache for correlation
            this.tuplesCache.put( key, tuple );
            // check if there was an active activation for it
            return !this.dormantActivations.contains( key );
        }

        public void withSerializedNodeMemories() {
            serializedNodeMemories = true;
        }
    }

    public static class ActivationKey {

        private final String pkgName;
        private final String ruleName;
        private final Object[] tuple;

        public ActivationKey(String pkgName,
                             String ruleName,
                             Object[] tuple) {
            this.pkgName = pkgName;
            this.ruleName = ruleName;
            this.tuple = tuple;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((pkgName == null) ? 0 : pkgName.hashCode());
            result = prime * result + ((ruleName == null) ? 0 : ruleName.hashCode());
            result = prime * result + Arrays.deepHashCode( tuple );
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            ActivationKey other = (ActivationKey) obj;
            if ( pkgName == null ) {
                if ( other.pkgName != null ) return false;
            } else if ( !pkgName.equals( other.pkgName ) ) return false;
            if ( ruleName == null ) {
                if ( other.ruleName != null ) return false;
            } else if ( !ruleName.equals( other.ruleName ) ) return false;
            return Arrays.deepEquals( tuple, other.tuple );
        }
    }

    public static class TupleKey {
        private final long[] tuple;

        public TupleKey(long[] tuple) {
            super();
            this.tuple = tuple;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode( tuple );
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            TupleKey other = (TupleKey) obj;
            if ( !Arrays.equals( tuple, other.tuple ) ) return false;
            return true;
        }
    }

}
