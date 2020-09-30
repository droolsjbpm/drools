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

package org.drools.core.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.SessionConfiguration;
import org.drools.core.SessionConfigurationImpl;
import org.drools.core.addon.AlphaNodeOrderingStrategy;
import org.drools.core.addon.NoopOrderingStrategy;
import org.drools.core.base.ClassFieldAccessorCache;
import org.drools.core.base.ClassObjectType;
import org.drools.core.common.BaseNode;
import org.drools.core.common.DroolsObjectInput;
import org.drools.core.common.DroolsObjectInputStream;
import org.drools.core.common.DroolsObjectOutputStream;
import org.drools.core.common.InternalAgenda;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.RuleBasePartitionId;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.event.KieBaseEventSupport;
import org.drools.core.factmodel.ClassDefinition;
import org.drools.core.factmodel.traits.TraitRegistry;
import org.drools.core.management.DroolsManagementAgent;
import org.drools.core.reteoo.AsyncReceiveNode;
import org.drools.core.reteoo.BetaNode;
import org.drools.core.reteoo.CompositePartitionAwareObjectSinkAdapter;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.KieComponentFactory;
import org.drools.core.reteoo.LeftTupleNode;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.ObjectSinkPropagator;
import org.drools.core.reteoo.ObjectSource;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.Rete;
import org.drools.core.reteoo.ReteooBuilder;
import org.drools.core.reteoo.RightInputAdapterNode;
import org.drools.core.reteoo.SegmentMemory;
import org.drools.core.reteoo.Sink;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.reteoo.builder.NodeFactory;
import org.drools.core.rule.DialectRuntimeRegistry;
import org.drools.core.rule.EntryPointId;
import org.drools.core.rule.Function;
import org.drools.core.rule.ImportDeclaration;
import org.drools.core.rule.InvalidPatternException;
import org.drools.core.rule.JavaDialectRuntimeData;
import org.drools.core.rule.TypeDeclaration;
import org.drools.core.rule.WindowDeclaration;
import org.drools.core.ruleunit.RuleUnitDescriptionRegistry;
import org.drools.core.spi.FactHandleFactory;
import org.drools.core.util.TripleStore;
import org.drools.reflective.classloader.ProjectClassLoader;
import org.kie.api.builder.ReleaseId;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.process.Process;
import org.kie.api.definition.rule.Query;
import org.kie.api.definition.rule.Rule;
import org.kie.api.definition.type.Expires.Policy;
import org.kie.api.definition.type.FactType;
import org.kie.api.definition.type.Role;
import org.kie.api.event.kiebase.KieBaseEventListener;
import org.kie.api.internal.io.ResourceTypePackage;
import org.kie.api.internal.utils.ServiceRegistry;
import org.kie.api.internal.weaver.KieWeavers;
import org.kie.api.io.Resource;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.KieSessionsPool;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.core.util.BitMaskUtil.isSet;
import static org.drools.core.util.ClassUtils.areNullSafeEquals;
import static org.drools.core.util.ClassUtils.convertClassToResourcePath;
import static org.drools.reflective.classloader.ProjectClassLoader.createProjectClassLoader;

public class KnowledgeBaseImpl
    implements
    InternalKnowledgeBase,
    Externalizable {

    protected static final transient Logger logger = LoggerFactory.getLogger(KnowledgeBaseImpl.class);

    private static final long serialVersionUID = 510l;

    private Set<EntryPointNode> addedEntryNodeCache;
    private Set<EntryPointNode> removedEntryNodeCache;
    // ------------------------------------------------------------
    // Instance members
    // ------------------------------------------------------------
    private String              id;

    private final AtomicInteger workingMemoryCounter = new AtomicInteger(0);

    private RuleBaseConfiguration config;

    protected Map<String, InternalKnowledgePackage> pkgs;

    private Map<String, Process> processes;

    private transient ClassLoader rootClassLoader;

    /**
     * The fact handle factory.
     */
    private FactHandleFactory factHandleFactory;

    private transient Map<String, Class<?>> globals;

    private final transient Queue<DialectRuntimeRegistry> reloadPackageCompilationData = new ConcurrentLinkedQueue<>();

    private KieBaseEventSupport eventSupport = new KieBaseEventSupport(this);

    private final transient Set<StatefulKnowledgeSessionImpl> statefulSessions = ConcurrentHashMap.newKeySet();

    // lock for entire rulebase, used for dynamic updates
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final transient Map<String, TypeDeclaration> classTypeDeclaration = new ConcurrentHashMap<>();

    private ClassFieldAccessorCache classFieldAccessorCache;
    /** The root Rete-OO for this <code>RuleBase</code>. */
    private transient Rete rete;
    private ReteooBuilder reteooBuilder;
    private transient Map<Integer, SegmentMemory.Prototype> segmentProtos = new ConcurrentHashMap<>();

    private KieComponentFactory kieComponentFactory;

    // This is just a hack, so spring can find the list of generated classes
    public List<List<String>> jaxbClasses;

    public final Set<KieBaseEventListener> kieBaseListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private transient Queue<Runnable> kbaseModificationsQueue = new ConcurrentLinkedQueue<>();

    private transient AtomicInteger sessionDeactivationsCounter = new AtomicInteger();
    private transient AtomicBoolean flushingUpdates = new AtomicBoolean( false );

    private transient InternalKieContainer kieContainer;

    private ReleaseId resolvedReleaseId;
    private String containerId;
    private AtomicBoolean mbeanRegistered = new AtomicBoolean(false);

    private RuleUnitDescriptionRegistry ruleUnitDescriptionRegistry = new RuleUnitDescriptionRegistry();

    private SessionConfiguration sessionConfiguration;

    private List<AsyncReceiveNode> receiveNodes;

    private KieSessionsPool sessionPool;

    private transient AlphaNodeOrderingStrategy alphaNodeOrderingStrategy = new NoopOrderingStrategy();

    public KnowledgeBaseImpl() { }

    public KnowledgeBaseImpl(final String id,
                             final RuleBaseConfiguration config) {
        this.config = (config != null) ? config : new RuleBaseConfiguration();
        this.config.makeImmutable();

        createRulebaseId(id);

        this.rootClassLoader = this.config.getClassLoader();

        this.pkgs = new HashMap<>();
        this.processes = new HashMap<>();
        this.globals = new HashMap<>();

        this.classFieldAccessorCache = new ClassFieldAccessorCache(this.rootClassLoader);
        kieComponentFactory = getConfiguration().getComponentFactory();

        this.factHandleFactory = kieComponentFactory.getFactHandleFactoryService();
        kieComponentFactory.initTraitFactory(this);
        kieComponentFactory.getTripleStore().setId(id);

        setupRete();

        sessionConfiguration = new SessionConfigurationImpl( null, this.config.getClassLoader(), this.config.getChainedProperties() );

        if (this.config.getSessionPoolSize() > 0) {
            sessionPool = newKieSessionsPool( this.config.getSessionPoolSize() );
        }

        alphaNodeOrderingStrategy = AlphaNodeOrderingStrategy.createAlphaNodeOrderingStrategy(this.config.getAlphaNodeOrderingOption());
    }

    @Override
    public void initMBeans() {
        if (config != null && config.isMBeansEnabled() && mbeanRegistered.compareAndSet(false, true)) {
            // no further synch enforced at this point, even if other threads might not immediately see (yet) the MBean registered on JMX.
            DroolsManagementAgent.getInstance().registerKnowledgeBase(this);
        }
    }

    public int nextWorkingMemoryCounter() {
        return this.workingMemoryCounter.getAndIncrement();
    }

    public int getWorkingMemoryCounter() {
        return this.workingMemoryCounter.get();
    }

    private void createRulebaseId(final String id) {
        if (id != null) {
            this.id = id;
        } else {
            String key = "";
            if (config.isMBeansEnabled()) {
                DroolsManagementAgent agent = DroolsManagementAgent.getInstance();
                key = String.valueOf(agent.getNextKnowledgeBaseId());
            }
            this.id = "default" + key;
        }
    }

    public void addEventListener(KieBaseEventListener listener) {
        synchronized (kieBaseListeners) {
            if ( !kieBaseListeners.contains( listener ) ) {
                eventSupport.addEventListener( listener );
                kieBaseListeners.add( listener );
            }
        }
    }

    public void removeEventListener(KieBaseEventListener listener) {
        synchronized (kieBaseListeners) {
            eventSupport.removeEventListener( listener );
            kieBaseListeners.remove( listener );
        }
    }

    public Collection<KieBaseEventListener> getKieBaseEventListeners() {
        return Collections.unmodifiableCollection( kieBaseListeners );
    }

    public SessionConfiguration getSessionConfiguration() {
        return sessionConfiguration;
    }

    public void removeKiePackage(String packageName) {
        enqueueModification( () -> {
            final InternalKnowledgePackage pkg = this.pkgs.get( packageName );
            if (pkg == null) {
                throw new IllegalArgumentException( "Package name '" + packageName +
                                                    "' does not exist for this Rule Base." );
            }
            this.eventSupport.fireBeforePackageRemoved( pkg );

            internalRemoveRules( ( (Collection<RuleImpl>) (Object) pkg.getRules() ) );

            // getting the list of referenced globals
            final Set<String> referencedGlobals = new HashSet<>();
            for (InternalKnowledgePackage pkgref : this.pkgs.values()) {
                if (pkgref != pkg) {
                    referencedGlobals.addAll( pkgref.getGlobals().keySet() );
                }
            }
            // removing globals declared inside the package that are not shared
            for (String globalName : pkg.getGlobals().keySet()) {
                if (!referencedGlobals.contains( globalName )) {
                    this.globals.remove( globalName );
                }
            }
            //and now the rule flows
            for ( String processName : new ArrayList<>(pkg.getRuleFlows().keySet()) ) {
                internalRemoveProcess( processName );
            }
            // removing the package itself from the list
            this.pkgs.remove( pkg.getName() );

            pkg.getDialectRuntimeRegistry().onRemove();

            //clear all members of the pkg
            pkg.clear();

            this.eventSupport.fireAfterPackageRemoved( pkg );
        });
    }

    public Rule getRule(String packageName,
                        String ruleName) {
        InternalKnowledgePackage p = getPackage(packageName);
        return p == null ? null : p.getRule( ruleName );
    }

    public Query getQuery(String packageName,
                          String queryName) {
        return getPackage(packageName).getRule( queryName );
    }

    public KieSessionsPool newKieSessionsPool( int initialSize) {
        return new KieSessionsPoolImpl(this, initialSize);
    }

    public KieSession newKieSession() {
        return newKieSession(null, EnvironmentFactory.newEnvironment());
    }

    public KieSession newKieSession(KieSessionConfiguration conf, Environment environment) {
        return newKieSession(conf, environment, false);
    }

    KieSession newKieSession(KieSessionConfiguration conf, Environment environment, boolean fromPool) {
        // NOTE if you update here, you'll also need to update the JPAService
        if ( conf == null ) {
            conf = getSessionConfiguration();
        }

        SessionConfiguration sessionConfig = (SessionConfiguration) conf;

        if ( environment == null ) {
            environment = EnvironmentFactory.newEnvironment();
        }

        if ( this.getConfiguration().isSequential() ) {
            throw new RuntimeException( "Cannot have a stateful rule session, with sequential configuration set to true" );
        }

        readLock();
        try {
            return internalCreateStatefulKnowledgeSession( environment, sessionConfig, fromPool );
        } finally {
            readUnlock();
        }
    }

    @Override
    public StatefulKnowledgeSessionImpl createSession(long id, FactHandleFactory handleFactory, long propagationContext, SessionConfiguration config, InternalAgenda agenda, Environment environment) {
        StatefulKnowledgeSessionImpl session = ( StatefulKnowledgeSessionImpl ) kieComponentFactory.getWorkingMemoryFactory()
                .createWorkingMemory( id, this, handleFactory, propagationContext, config, agenda, environment );
        return internalInitSession( config, session );
    }

    public StatefulKnowledgeSessionImpl internalCreateStatefulKnowledgeSession( Environment environment, SessionConfiguration sessionConfig, boolean fromPool ) {
        if (fromPool || sessionPool == null) {
            StatefulKnowledgeSessionImpl session = ( StatefulKnowledgeSessionImpl ) kieComponentFactory.getWorkingMemoryFactory()
                    .createWorkingMemory( nextWorkingMemoryCounter(), this, sessionConfig, environment );
            return internalInitSession( sessionConfig, session );
        }
        return (StatefulKnowledgeSessionImpl) sessionPool.newKieSession( sessionConfig );
    }

    private StatefulKnowledgeSessionImpl internalInitSession( SessionConfiguration sessionConfig, StatefulKnowledgeSessionImpl session ) {
        if ( sessionConfig.isKeepReference() ) {
            addStatefulSession(session);
        }
        return session;
    }

    public Collection<? extends KieSession> getKieSessions() {
        return Collections.unmodifiableSet( statefulSessions );
    }

    public StatelessKieSession newStatelessKieSession(KieSessionConfiguration conf) {
        return new StatelessKnowledgeSessionImpl( this, conf );
    }

    public StatelessKieSession newStatelessKieSession() {
        return new StatelessKnowledgeSessionImpl( this, null );
    }

    public Collection<KiePackage> getKiePackages() {
        InternalKnowledgePackage[] knowledgePackages = getPackages();
        List<KiePackage> list = new ArrayList<>(knowledgePackages.length);
        Collections.addAll( list, knowledgePackages );
        return list;
    }

    public KiePackage getKiePackage(String packageName) {
        return getPackage(packageName);
    }

    // ------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------

    /**
     * Handles the read serialization of the Package. Patterns in Rules may reference generated data which cannot be serialized by default methods.
     * The Package uses PackageCompilationData to hold a reference to the generated bytecode; which must be restored before any Rules.
     * A custom ObjectInputStream, able to resolve classes against the bytecode in the PackageCompilationData, is used to restore the Rules.
     */
    public void readExternal(final ObjectInput in) throws IOException,
                                                          ClassNotFoundException {
        // PackageCompilationData must be restored before Rules as it has the ClassLoader needed to resolve the generated code references in Rules
        DroolsObjectInput droolsStream;
        boolean isDrools = in instanceof DroolsObjectInputStream;

        boolean wasDrools = in.readBoolean();
        if (wasDrools && !isDrools) {
            throw new IllegalArgumentException("The knowledge base was serialized using a DroolsObjectOutputStream. A DroolsObjectInputStream is required for deserialization.");
        }

        if (isDrools) {
            droolsStream = (DroolsObjectInput) in;
        } else {
            ByteArrayInputStream bytes = new ByteArrayInputStream((byte[]) in.readObject());
            droolsStream = new DroolsObjectInputStream(bytes);
        }

        // boolean classLoaderCacheEnabled field
        droolsStream.readBoolean();
        Map<String, byte[]> store = (Map<String, byte[]>) droolsStream.readObject();

        this.rootClassLoader = createProjectClassLoader(droolsStream.getParentClassLoader(), store);

        droolsStream.setClassLoader(this.rootClassLoader);
        droolsStream.setKnowledgeBase(this);

        this.classFieldAccessorCache = new ClassFieldAccessorCache(this.rootClassLoader);

        this.config = (RuleBaseConfiguration) droolsStream.readObject();
        this.config.setClassLoader(droolsStream.getParentClassLoader());

        this.sessionConfiguration = new SessionConfigurationImpl( null, config.getClassLoader(), config.getChainedProperties() );

        kieComponentFactory = getConfiguration().getComponentFactory();

        this.pkgs = (Map<String, InternalKnowledgePackage>) droolsStream.readObject();

        for (InternalKnowledgePackage pkg : this.pkgs.values()) {
            pkg.getDialectRuntimeRegistry().onAdd(this.rootClassLoader);
        }

        // PackageCompilationData must be restored before Rules as it has the ClassLoader needed to resolve the generated code references in Rules
        this.id = (String) droolsStream.readObject();
        this.workingMemoryCounter.set(droolsStream.readInt());

        this.processes = (Map<String, Process>) droolsStream.readObject();
        final String classNameFromStream = droolsStream.readUTF();
        try {
            Class cls = droolsStream.getParentClassLoader().loadClass(classNameFromStream);
            this.factHandleFactory = (FactHandleFactory) cls.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            DroolsObjectInputStream.newInvalidClassException(classNameFromStream, e);
        }

        for (InternalKnowledgePackage pkg : this.pkgs.values()) {
            pkg.getDialectRuntimeRegistry().onBeforeExecute();
            pkg.getClassFieldAccessorStore().setClassFieldAccessorCache(this.classFieldAccessorCache);
            pkg.getClassFieldAccessorStore().wire();
        }

        this.populateTypeDeclarationMaps();

        // read globals
        Map<String, String> globs = (Map<String, String>) droolsStream.readObject();
        populateGlobalsMap(globs);

        this.eventSupport = (KieBaseEventSupport) droolsStream.readObject();
        this.eventSupport.setKnowledgeBase(this);

        this.reteooBuilder = (ReteooBuilder) droolsStream.readObject();
        this.reteooBuilder.setRuleBase(this);
        this.rete = (Rete) droolsStream.readObject();

        this.resolvedReleaseId = (ReleaseId) droolsStream.readObject();

        ( (DroolsObjectInputStream) droolsStream ).bindAllExtractors(this);

        if (!isDrools) {
            droolsStream.close();
        }

        this.getConfiguration().getComponentFactory().initTraitFactory(this);

        this.alphaNodeOrderingStrategy = AlphaNodeOrderingStrategy.createAlphaNodeOrderingStrategy(this.config.getAlphaNodeOrderingOption());

        rewireReteAfterDeserialization();
    }

    private void rewireReteAfterDeserialization() {
        for (EntryPointNode entryPointNode : rete.getEntryPointNodes().values()) {
            entryPointNode.setParentObjectSource( rete );
            rewireNodeAfterDeserialization( entryPointNode );
        }
    }

    private void rewireNodeAfterDeserialization(BaseNode node) {
        Sink[] sinks = node.getSinks();
        if (sinks != null) {
            for (Sink sink : sinks) {
                if (sink instanceof ObjectSource) {
                    if (node instanceof ObjectSource) {
                        ( (ObjectSource) sink ).setParentObjectSource( (ObjectSource) node );
                    } else if (sink instanceof RightInputAdapterNode ) {
                        ( (RightInputAdapterNode) sink ).setTupleSource( (LeftTupleSource) node );
                    }
                } else if (sink instanceof LeftTupleSource) {
                    if (node instanceof LeftTupleSource) {
                        ( (LeftTupleSource) sink ).setLeftTupleSource( (LeftTupleSource) node );
                    } else if (sink instanceof BetaNode ) {
                        ( (BetaNode) sink ).setRightInput( (ObjectSource) node );
                    }
                }
                if (sink instanceof BaseNode) {
                    rewireNodeAfterDeserialization((BaseNode)sink);
                }
            }
        }
    }

    /**
     * Handles the write serialization of the Package. Patterns in Rules may reference generated data which cannot be serialized by default methods.
     * The Package uses PackageCompilationData to hold a reference to the generated bytecode. The generated bytecode must be restored before any Rules.
     */
    public void writeExternal(final ObjectOutput out) throws IOException {
        ObjectOutput droolsStream;
        boolean isDrools = out instanceof DroolsObjectOutputStream;
        ByteArrayOutputStream bytes;
        out.writeBoolean( isDrools );
        if (out instanceof DroolsObjectOutputStream) {
            droolsStream = out;
            bytes = null;
        } else {
            bytes = new ByteArrayOutputStream();
            droolsStream = new DroolsObjectOutputStream(bytes);
        }
        try {
            // must write this option first in order to properly deserialize later
            droolsStream.writeBoolean(this.config.isClassLoaderCacheEnabled());

            droolsStream.writeObject((( ProjectClassLoader ) rootClassLoader).getStore());

            droolsStream.writeObject(this.config);
            droolsStream.writeObject(this.pkgs);

            // Rules must be restored by an ObjectInputStream that can resolve using a given ClassLoader to handle seaprately by storing as
            // a byte[]
            droolsStream.writeObject(this.id);
            droolsStream.writeInt(this.workingMemoryCounter.get());
            droolsStream.writeObject(this.processes);
            droolsStream.writeUTF(this.factHandleFactory.getClass().getName());
            droolsStream.writeObject(buildGlobalMapForSerialization());

            this.eventSupport.removeEventListener(KieBaseEventListener.class);
            droolsStream.writeObject(this.eventSupport);

            droolsStream.writeObject(this.reteooBuilder);
            droolsStream.writeObject(this.rete);

            droolsStream.writeObject(this.resolvedReleaseId);
        } finally {
            if (bytes != null) {
                droolsStream.flush();
                droolsStream.close();
                bytes.close();
                out.writeObject(bytes.toByteArray());
            }
        }
    }

    private Map<String, String> buildGlobalMapForSerialization() {
        Map<String, String> gl = new HashMap<>();
        for (Map.Entry<String, Class<?>> entry : this.globals.entrySet()) {
            gl.put( entry.getKey(),
                    entry.getValue().getName() );
        }
        return gl;
    }

    /**
     * globals class types must be re-wired after serialization
     *
     * @throws ClassNotFoundException
     */
    private void populateGlobalsMap(Map<String, String> globs) throws ClassNotFoundException {
        this.globals = new HashMap<>();
        for (Map.Entry<String, String> entry : globs.entrySet()) {
            addGlobal( entry.getKey(),
                       this.rootClassLoader.loadClass( entry.getValue() ) );
        }
    }

    /**
     * type classes must be re-wired after serialization
     *
     * @throws ClassNotFoundException
     */
    private void populateTypeDeclarationMaps() throws ClassNotFoundException {
        for (InternalKnowledgePackage pkg : this.pkgs.values()) {
            for (TypeDeclaration type : pkg.getTypeDeclarations().values()) {
                type.setTypeClass(this.rootClassLoader.loadClass(type.getTypeClassName()));
                this.classTypeDeclaration.put(type.getTypeClassName(),
                                              type);
            }
        }
    }

    /**
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    public void disposeStatefulSession(StatefulKnowledgeSessionImpl statefulSession) {
        this.statefulSessions.remove(statefulSession);
        if (kieContainer != null) {
            kieContainer.disposeSession( statefulSession );
        }
    }

    public FactHandleFactory newFactHandleFactory() {
        return this.factHandleFactory.newInstance();
    }

    public FactHandleFactory newFactHandleFactory(long id,
                                                  long counter) {
        return this.factHandleFactory.newInstance(id,
                                                  counter);
    }

    public Collection<Process> getProcesses() {
        readLock();
        try {
            return this.processes.values();
        } finally {
            readUnlock();
        }
    }

    public InternalKnowledgePackage[] getPackages() {
        readLock();
        try {
            return this.pkgs.values().toArray( new InternalKnowledgePackage[this.pkgs.size()] );
        } finally {
            readUnlock();
        }
    }

    // TODO WARN: the below must be mutale as that is used by TraitFactory.getClassFieldAccessorStore
    @Override
    public Map<String, InternalKnowledgePackage> getPackagesMap() {
        return this.pkgs;
    }

    // TODO WARN: the below must be mutable as it's used by org.drools.compiler.builder.impl.KnowledgeBuilderTest
    @Override
    public Map<String, Class<?>> getGlobals() {
        return this.globals;
    }

    private void lock() {
        // The lock is reentrant, so we need additional magic here to skip
        // notifications for locked if this thread already has locked it.
        boolean firstLock = !this.lock.isWriteLockedByCurrentThread();
        if (firstLock) {
            this.eventSupport.fireBeforeRuleBaseLocked();
        }
        // Always lock to increase the counter
        this.lock.writeLock().lock();
        if ( firstLock ) {
            this.eventSupport.fireAfterRuleBaseLocked();
        }
    }

    private void unlock() {
        boolean lastUnlock = this.lock.getWriteHoldCount() == 1;
        if (lastUnlock) {
            this.eventSupport.fireBeforeRuleBaseUnlocked();
        }
        this.lock.writeLock().unlock();
        if ( lastUnlock ) {
            this.eventSupport.fireAfterRuleBaseUnlocked();
        }
    }

    public void readLock() {
        this.lock.readLock().lock();
    }

    public void readUnlock() {
        this.lock.readLock().unlock();
    }

    /**
     * Add a <code>Package</code> to the network. Iterates through the
     * <code>Package</code> adding Each individual <code>Rule</code> to the
     * network. Before update network each referenced <code>WorkingMemory</code>
     * is locked.
     *
     * @param newPkgs The package to add.
     */
    @Override
    public void addPackages( Collection<? extends KiePackage> newPkgs ) {
        final List<InternalKnowledgePackage> clonedPkgs = new ArrayList<>();
        for (KiePackage newPkg : newPkgs) {
            clonedPkgs.add(((InternalKnowledgePackage)newPkg).deepCloneIfAlreadyInUse(rootClassLoader));
        }

        clonedPkgs.sort(Comparator.comparing( (InternalKnowledgePackage p) -> p.getRules().size() ).reversed().thenComparing( InternalKnowledgePackage::getName ));
        enqueueModification( () -> internalAddPackages( clonedPkgs ) );
    }

    @Override
    public Future<KiePackage> addPackage( final KiePackage newPkg ) {
        InternalKnowledgePackage clonedPkg = ((InternalKnowledgePackage)newPkg).deepCloneIfAlreadyInUse(rootClassLoader);
        CompletableFuture<KiePackage> result = new CompletableFuture<>();
        enqueueModification( () -> {
            internalAddPackages( Collections.singletonList(clonedPkg) );
            result.complete( getPackage(newPkg.getName()) );
        } );
        return result;
    }

    public void enqueueModification(Runnable modification) {
        if ( tryLockAndDeactivate() ) {
            try {
                modification.run();
            } finally {
                unlockAndActivate();
            }
        } else {
            kbaseModificationsQueue.offer(modification);
        }
    }

    public boolean flushModifications() {
        if (!flushingUpdates.compareAndSet( false, true )) {
            return false;
        }

        if (kbaseModificationsQueue.isEmpty()) {
            flushingUpdates.set( false );
            return false;
        }

        try {
            lockAndDeactivate();
            while (!kbaseModificationsQueue.isEmpty()) {
                kbaseModificationsQueue.poll().run();
            }
        } finally {
            flushingUpdates.set( false );
            unlockAndActivate();
        }
        return true;
    }

    private void lockAndDeactivate() {
        lock();
        deactivateAllSessions();
    }

    private void unlockAndActivate() {
        activateAllSessions();
        unlock();
    }

    private boolean tryDeactivateAllSessions() {
        Collection<InternalWorkingMemory> wms = getWorkingMemories();
        if (wms.isEmpty()) {
            return true;
        }
        List<InternalWorkingMemory> deactivatedWMs = new ArrayList<>();
        for ( InternalWorkingMemory wm : wms ) {
            if (wm.tryDeactivate()) {
                deactivatedWMs.add(wm);
            } else {
                for (InternalWorkingMemory deactivatedWM : deactivatedWMs) {
                    deactivatedWM.activate();
                }
                return false;
            }
        }
        return true;
    }

    private boolean tryLockAndDeactivate() {
        if ( sessionDeactivationsCounter.incrementAndGet() > 1 ) {
            this.lock.writeLock().lock();
            return true;
        }

        boolean locked = this.lock.writeLock().tryLock();
        if ( locked && !tryDeactivateAllSessions() ) {
            this.lock.writeLock().unlock();
            locked = false;
        }

        if (!locked) {
            sessionDeactivationsCounter.decrementAndGet();
        }

        return locked;
    }

    private void deactivateAllSessions() {
        if ( sessionDeactivationsCounter.incrementAndGet() < 2 ) {
            for ( InternalWorkingMemory wm : getWorkingMemories() ) {
                wm.deactivate();
            }
        }
    }

    private void activateAllSessions() {
        if ( sessionDeactivationsCounter.decrementAndGet() == 0 ) {
            for ( InternalWorkingMemory wm : getWorkingMemories() ) {
                wm.activate();
            }
        }
    }

    private void internalAddPackages(Collection<InternalKnowledgePackage> clonedPkgs) {
        for ( InternalWorkingMemory wm : getWorkingMemories() ) {
            wm.flushPropagations();
        }

        alphaNodeOrderingStrategy.analyzeAlphaConstraints(pkgs, clonedPkgs);

        // we need to merge all byte[] first, so that the root classloader can resolve classes
        for (InternalKnowledgePackage newPkg : clonedPkgs) {
            newPkg.checkValidity();
            this.eventSupport.fireBeforePackageAdded( newPkg );

            if ( newPkg.hasTraitRegistry() ) {
                getTraitRegistry().merge( newPkg.getTraitRegistry() );
            }

            InternalKnowledgePackage pkg = this.pkgs.get( newPkg.getName() );
            if ( pkg == null ) {
                pkg = kieComponentFactory.createKnowledgePackage(newPkg.getName());

                // @TODO we really should have a single root cache
                pkg.setClassFieldAccessorCache( this.classFieldAccessorCache );
                pkgs.put( pkg.getName(),
                          pkg );
            }

            // first merge anything related to classloader re-wiring
            pkg.getDialectRuntimeRegistry().merge( newPkg.getDialectRuntimeRegistry(),
                                                   this.rootClassLoader,
                                                   true );

        }

        processAllTypesDeclaration( clonedPkgs );

        for ( InternalKnowledgePackage newPkg : clonedPkgs ) {
            // Add functions
            JavaDialectRuntimeData runtime = ((JavaDialectRuntimeData) newPkg.getDialectRuntimeRegistry().getDialectData( "java" ));

            for ( Function function : newPkg.getFunctions().values() ) {
                String functionClassName = function.getClassName();
                try {
                    registerFunctionClassAndInnerClasses( functionClassName, runtime, this::registerAndLoadTypeDefinition );
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException( "Unable to compile function '" + function.getName() + "'", e );
                }
            }
        }

        // now iterate again, this time onBeforeExecute will handle any wiring or cloader re-creating that needs to be done as part of the merge
        for (InternalKnowledgePackage newPkg : clonedPkgs) {
            InternalKnowledgePackage pkg = this.pkgs.get( newPkg.getName() );

            // this needs to go here, as functions will set a java dialect to dirty
            if (newPkg.getFunctions() != null) {
                for (Map.Entry<String, Function> entry : newPkg.getFunctions().entrySet()) {
                    pkg.addFunction( entry.getValue() );
                }
            }

            pkg.getDialectRuntimeRegistry().onBeforeExecute();

            // with the classloader recreated for all byte[] classes, we should now merge and wire any new accessors
            pkg.getClassFieldAccessorStore().merge( newPkg.getClassFieldAccessorStore() );
        }


        for (InternalKnowledgePackage newPkg : clonedPkgs) {
            InternalKnowledgePackage pkg = this.pkgs.get( newPkg.getName() );

            // now merge the new package into the existing one
            mergePackage( pkg,
                          newPkg );

            // add the window declarations to the kbase
            for( WindowDeclaration window : newPkg.getWindowDeclarations().values() ) {
                this.reteooBuilder.addNamedWindow(window);
            }

            // add entry points to the kbase
            for (String entryPointId : newPkg.getEntryPointIds()) {
                this.reteooBuilder.addEntryPoint(entryPointId);
            }

            // add the rules to the RuleBase
            for ( Rule r : newPkg.getRules() ) {
                RuleImpl rule = (RuleImpl)r;
                checkMultithreadedEvaluation( rule );
                internalAddRule( rule );
            }

            // add the flows to the RuleBase
            if ( newPkg.getRuleFlows() != null ) {
                final Map<String, Process> flows = newPkg.getRuleFlows();
                for ( Process process : flows.values() ) {
                    internalAddProcess( process );
                }
            }

            if ( ! newPkg.getResourceTypePackages().isEmpty() ) {
                KieWeavers weavers = ServiceRegistry.getInstance().get( KieWeavers.class );
                for ( ResourceTypePackage rtkKpg : newPkg.getResourceTypePackages().values() ) {
                    weavers.weave( this, newPkg, rtkKpg );
                }
            }

            ruleUnitDescriptionRegistry.add(newPkg.getRuleUnitDescriptionLoader());

            this.eventSupport.fireAfterPackageAdded( newPkg );
        }

        if (config.isMultithreadEvaluation() && !hasMultiplePartitions()) {
            disableMultithreadEvaluation("The rete network cannot be partitioned: disabling multithread evaluation");
        }
    }

    public void processAllTypesDeclaration( Collection<InternalKnowledgePackage> pkgs ) {
        List<TypeDeclaration> allTypeDeclarations = new ArrayList<>();
        // Add all Type Declarations, this has to be done first incase packages cross reference each other during build process.
        for ( InternalKnowledgePackage newPkg : pkgs ) {
            // we have to do this before the merging, as it does some classloader resolving
            if ( newPkg.getTypeDeclarations() != null ) {
                allTypeDeclarations.addAll( newPkg.getTypeDeclarations().values() );
            }
        }
        Collections.sort( allTypeDeclarations );

        // add type declarations according to the global order
        for ( TypeDeclaration newDecl : allTypeDeclarations ) {
            InternalKnowledgePackage newPkg = null;
            for ( InternalKnowledgePackage kpkg : pkgs ) {
                if ( kpkg.getTypeDeclarations().containsKey( newDecl.getTypeName() ) ) {
                    newPkg = kpkg;
                    break;
                }
            }
            processTypeDeclaration( newDecl, newPkg );
        }
    }

    private void checkMultithreadedEvaluation( RuleImpl rule ) {
        if (config.isMultithreadEvaluation()) {
            if (!rule.isMainAgendaGroup()) {
                disableMultithreadEvaluation( "Agenda-groups are not supported with multithread evaluation: disabling it" );
            } else if (rule.getActivationGroup() != null) {
                disableMultithreadEvaluation( "Activation-groups are not supported with multithread evaluation: disabling it" );
            } else if (!rule.getSalience().isDefault()) {
                disableMultithreadEvaluation( "Salience is not supported with multithread evaluation: disabling it" );
            } else if (rule.isQuery()) {
                disableMultithreadEvaluation( "Queries are not supported with multithread evaluation: disabling it" );
            }
        }
    }

    private boolean hasMultiplePartitions() {
        for (EntryPointNode entryPointNode : rete.getEntryPointNodes().values()) {
            for ( ObjectTypeNode otn : entryPointNode.getObjectTypeNodes().values() ) {
                ObjectSinkPropagator sink = otn.getObjectSinkPropagator();
                if (sink instanceof CompositePartitionAwareObjectSinkAdapter && ( (CompositePartitionAwareObjectSinkAdapter) sink ).getUsedPartitionsCount() > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private void disableMultithreadEvaluation(String warningMessage) {
        config.enforceSingleThreadEvaluation();
        logger.warn( warningMessage );
        for (EntryPointNode entryPointNode : rete.getEntryPointNodes().values()) {
            entryPointNode.setPartitionsEnabled( false );
            for (ObjectTypeNode otn : entryPointNode.getObjectTypeNodes().values()) {
                ObjectSinkPropagator sink = otn.getObjectSinkPropagator();
                if (sink instanceof CompositePartitionAwareObjectSinkAdapter) {
                    otn.setObjectSinkPropagator( ( (CompositePartitionAwareObjectSinkAdapter) sink )
                                                         .asNonPartitionedSinkPropagator( config.getAlphaNodeHashingThreshold() ) );
                }
            }
        }
    }

    public interface ClassRegister {
        void register(String name, byte[] bytes) throws ClassNotFoundException;
    }

    public static void registerFunctionClassAndInnerClasses( String functionClassName, JavaDialectRuntimeData runtime, ClassRegister consumer ) throws ClassNotFoundException {
        String className = convertClassToResourcePath(functionClassName);
        String innerClassName = className.substring( 0, className.length() - ".class".length() ) + "$";
        for (Map.Entry<String, byte[]> entry : runtime.getStore().entrySet()) {
            if (entry.getKey().equals( className )) {
                consumer.register( functionClassName, entry.getValue() );
            } else if (entry.getKey().startsWith( innerClassName )) {
                String innerName = functionClassName + entry.getKey().substring( functionClassName.length(), entry.getKey().length() - ".class".length() );
                consumer.register( innerName, entry.getValue() );
            }
        }
    }

    public void registerTypeDeclaration( TypeDeclaration newDecl, InternalKnowledgePackage newPkg ) {
        this.classTypeDeclaration.put( newDecl.getTypeClassName(), newDecl );
    }

    protected void processTypeDeclaration( TypeDeclaration newDecl, InternalKnowledgePackage newPkg ) {
        JavaDialectRuntimeData runtime = ((JavaDialectRuntimeData) newPkg.getDialectRuntimeRegistry().getDialectData( "java" ));

        TypeDeclaration typeDeclaration = this.classTypeDeclaration.get( newDecl.getTypeClassName() );
        if ( typeDeclaration == null ) {
            String className = newDecl.getTypeClassName();

            byte [] def = runtime != null ? runtime.getClassDefinition(convertClassToResourcePath(className)) : null;
            try {
                Class<?> definedKlass = registerAndLoadTypeDefinition( className, def );

                if (newDecl.getTypeClassDef() == null) {
                    newDecl.setTypeClassDef( new ClassDefinition() );
                }
                newDecl.setTypeClass( definedKlass );
            } catch (ClassNotFoundException e) {
                if (newDecl.isNovel()) {
                    throw new RuntimeException( "unable to resolve Type Declaration class '" + className + "'", e );
                }
            }

            this.classTypeDeclaration.put( className, newDecl );
            typeDeclaration = newDecl;
        } else {
            Class<?> definedKlass = typeDeclaration.getTypeClass();

            newDecl.getTypeClassDef().setDefinedClass( definedKlass );
            newDecl.setTypeClass( definedKlass );

            mergeTypeDeclarations( typeDeclaration,
                    newDecl );
        }

        // update existing OTNs
        updateDependentTypes( typeDeclaration );
    }

    public Class<?> registerAndLoadTypeDefinition( String className, byte[] def ) throws ClassNotFoundException {
        try {
            return this.rootClassLoader.loadClass( className );
        } catch (ClassNotFoundException e) {
            if (def != null && rootClassLoader instanceof ProjectClassLoader) {
                return ((ProjectClassLoader)rootClassLoader).defineClass(className, def);
            }
            throw e;
        }
    }

    private void updateDependentTypes( TypeDeclaration typeDeclaration ) {
        // update OTNs
        if( this.getConfiguration().getEventProcessingMode().equals( EventProcessingOption.STREAM ) ) {
            // if the expiration for the type was set, then add 1, otherwise return -1
            long exp = typeDeclaration.getExpirationOffset() > -1 ? typeDeclaration.getExpirationOffset() + 1 : -1;

            // if we are running in STREAM mode, update expiration offset
            for( EntryPointNode ep : this.rete.getEntryPointNodes().values() ) {
                for( ObjectTypeNode node : ep.getObjectTypeNodes().values() ) {
                    if( node.isAssignableFrom( typeDeclaration.getObjectType() ) ) {
                        node.setExpirationOffset( Math.max( node.getExpirationOffset(), exp ) );
                    }
                }
            }
        }
    }

    private void mergeTypeDeclarations( TypeDeclaration existingDecl,
                                        TypeDeclaration newDecl ) {

        if ( ! areNullSafeEquals( existingDecl.getFormat(),
                                  newDecl.getFormat() ) ||
             ! areNullSafeEquals( existingDecl.getObjectType(),
                                  newDecl.getObjectType() ) ||
             ! areNullSafeEquals( existingDecl.getTypeClassName(),
                                  newDecl.getTypeClassName() ) ||
             ! areNullSafeEquals( existingDecl.getTypeName(),
                                  newDecl.getTypeName() ) ) {

            throw new RuntimeException( "Unable to merge Type Declaration for class '" + existingDecl.getTypeName() + "'" );

        }

        existingDecl.setDurationAttribute( mergeLeft( existingDecl.getTypeName(),
                                                      "Unable to merge @duration attribute for type declaration of class:",
                                                      existingDecl.getDurationAttribute(),
                                                      newDecl.getDurationAttribute(),
                                                      true,
                                                      false ) );

        existingDecl.setDynamic( mergeLeft( existingDecl.getTypeName(),
                                            "Unable to merge @propertyChangeSupport  (a.k.a. dynamic) attribute for type declaration of class:",
                                            existingDecl.isDynamic(),
                                            newDecl.isDynamic(),
                                            true,
                                            false ) );

        existingDecl.setPropertyReactive( mergeLeft(existingDecl.getTypeName(),
                                                    "Unable to merge @propertyReactive attribute for type declaration of class:",
                                                    existingDecl.isPropertyReactive(),
                                                    newDecl.isPropertyReactive(),
                                                    true,
                                                    false) );

        if ( newDecl.getExpirationPolicy() == Policy.TIME_HARD ) {
            if (existingDecl.getExpirationPolicy() == Policy.TIME_SOFT ||
                newDecl.getExpirationOffset() > existingDecl.getExpirationOffset()) {
                existingDecl.setExpirationOffset( newDecl.getExpirationOffset() );
                existingDecl.setExpirationType( Policy.TIME_HARD );
            }
        } else {
            if (existingDecl.getExpirationPolicy() == Policy.TIME_SOFT &&
                newDecl.getExpirationOffset() > existingDecl.getExpirationOffset()) {
                existingDecl.setExpirationOffset( newDecl.getExpirationOffset() );
            }
        }

        if ( newDecl.getNature().equals( TypeDeclaration.Nature.DEFINITION ) && newDecl.isNovel() ) {
            // At this point, the definitions must be equivalent.
            // So the only illegal case is a novel definition of an already existing type
            existingDecl.setNovel( mergeLeft( existingDecl.getTypeName(),
                                              "Unable to merge @novel attribute for type declaration of class:",
                                              existingDecl.isNovel(),
                                              newDecl.isNovel(),
                                              true,
                                              false ) );
        }

        if ( newDecl.getNature().equals( TypeDeclaration.Nature.DEFINITION ) || existingDecl.getResource() == null ) {
            existingDecl.setResource( mergeLeft( existingDecl.getTypeName(),
                                                 "Unable to merge resource attribute for type declaration of class:",
                                                 existingDecl.getResource(),
                                                 newDecl.getResource(),
                                                 true,
                                                 true ) );
        }

        existingDecl.setRole( mergeLeft( existingDecl.getTypeName(),
                                         "Unable to merge @role attribute for type declaration of class:",
                                         isSet(existingDecl.getSetMask(), TypeDeclaration.ROLE_BIT)
                                         && newDecl.getRole() != Role.Type.FACT
                                         ? existingDecl.getRole() : null,
                                         newDecl.getRole(),
                                         true,
                                         false ) );

        existingDecl.setTimestampAttribute( mergeLeft( existingDecl.getTypeName(),
                                                       "Unable to merge @timestamp attribute for type declaration of class:",
                                                       existingDecl.getTimestampAttribute(),
                                                       newDecl.getTimestampAttribute(),
                                                       true,
                                                       false ) );

        existingDecl.setTypesafe( mergeLeft(existingDecl.getTypeName(),
                                            "Unable to merge @typesafe attribute for type declaration of class:",
                                            existingDecl.isTypesafe(),
                                            newDecl.isTypesafe(),
                                            true,
                                            false ) );
    }

    private <T> T mergeLeft( String typeClass,
                             String errorMsg,
                             T leftVal,
                             T rightVal,
                             boolean errorOnDiff,
                             boolean override ) {
        T newValue = leftVal;
        if ( ! areNullSafeEquals( leftVal, rightVal ) ) {
            if ( leftVal == null ) {
                newValue = rightVal;
            } else if ( rightVal != null ) {
                if ( override ) {
                    newValue = rightVal;
                } else {
                    if ( errorOnDiff ) {
                        throw new RuntimeException( errorMsg + " '" + typeClass + "'" );
                    } else {
                        // do nothing, just use the left value
                    }
                }
            }
        }
        return newValue;
    }

    /**
     * Merge a new package with an existing package.
     * Most of the work is done by the concrete implementations,
     * but this class does some work (including combining imports, compilation data, globals,
     * and the actual Rule objects into the package).
     */
    private void mergePackage( InternalKnowledgePackage pkg,
                               InternalKnowledgePackage newPkg ) {
        // Merge imports
        final Map<String, ImportDeclaration> imports = pkg.getImports();
        imports.putAll(newPkg.getImports());

        // Merge static imports
        for (String staticImport : newPkg.getStaticImports()) {
            pkg.addStaticImport(staticImport);
        }

        // merge globals
        if (newPkg.getGlobals() != null && !newPkg.getGlobals().isEmpty()) {
            Map<String, Class<?>> pkgGlobals = pkg.getGlobals();
            // Add globals
            for (final Map.Entry<String, Class<?>> entry : newPkg.getGlobals().entrySet()) {
                final String identifier = entry.getKey();
                final Class<?> type = entry.getValue();
                if (pkgGlobals.containsKey( identifier ) && !pkgGlobals.get( identifier ).equals( type )) {
                    throw new RuntimeException(pkg.getName() + " cannot be integrated");
                } else {
                    pkg.addGlobal( identifier, type );
                    // this isn't a package merge, it's adding to the rulebase, but I've put it here for convienience
                    addGlobal( identifier, type );
                }
            }
        }

        // merge entry point declarations
        if (newPkg.getEntryPointIds() != null) {
            for (String ep : newPkg.getEntryPointIds()) {
                pkg.addEntryPointId( ep );

            }
        }

        // merge the type declarations
        if (newPkg.getTypeDeclarations() != null) {
            // add type declarations
            for (TypeDeclaration type : newPkg.getTypeDeclarations().values()) {
                // @TODO should we allow overrides? only if the class is not in use.
                if (!pkg.getTypeDeclarations().containsKey( type.getTypeName() )) {
                    // add to package list of type declarations
                    pkg.addTypeDeclaration( type );
                }
            }
        }

        // merge window declarations
        if ( newPkg.getWindowDeclarations() != null ) {
            // add window declarations
            for ( WindowDeclaration window : newPkg.getWindowDeclarations().values() ) {
                if ( !pkg.getWindowDeclarations().containsKey( window.getName() ) ||
                     pkg.getWindowDeclarations().get( window.getName() ).equals( window ) ) {
                    pkg.addWindowDeclaration( window );
                } else {
                    throw new RuntimeException( "Unable to merge two conflicting window declarations for window named: "+window.getName() );
                }
            }
        }

        //Merge rules into the RuleBase package
        //as this is needed for individual rule removal later on
        List<RuleImpl> rulesToBeRemoved = new ArrayList<>();
        for (Rule newRule : newPkg.getRules()) {
            // remove the rule if it already exists
            RuleImpl oldRule = pkg.getRule(newRule.getName());
            if (oldRule != null) {
                rulesToBeRemoved.add(oldRule);
            }
        }
        if (!rulesToBeRemoved.isEmpty()) {
            removeRules( rulesToBeRemoved );
        }

        for (Rule newRule : newPkg.getRules()) {
            pkg.addRule((RuleImpl)newRule);
        }

        //Merge The Rule Flows
        if (newPkg.getRuleFlows() != null) {
            for (Process flow : newPkg.getRuleFlows().values()) {
                pkg.addProcess(flow);
            }
        }

        if ( ! newPkg.getResourceTypePackages().isEmpty() ) {
            KieWeavers weavers = ServiceRegistry.getInstance().get(KieWeavers.class);
            for ( ResourceTypePackage rtkKpg : newPkg.getResourceTypePackages().values() ) {
                weavers.merge( this, pkg, rtkKpg );
            }
        }
    }

    public void addGlobal(String identifier, Class clazz) {
        this.globals.put( identifier, clazz );
    }

    public void removeGlobal(String identifier) {
        // check if there is still at least a package containing the global
        for (InternalKnowledgePackage pkg : pkgs.values()) {
            if ( pkg.getGlobals().get( identifier ) != null) {
                return;
            }
        }

        this.globals.remove( identifier );
        for ( InternalWorkingMemory wm : getWorkingMemories() ) {
            wm.removeGlobal(identifier);
        }
    }

    protected void setupRete() {
        this.rete = new Rete( this );
        this.reteooBuilder = new ReteooBuilder( this );

        NodeFactory nodeFactory = kieComponentFactory.getNodeFactoryService();

        // always add the default entry point
        EntryPointNode epn = nodeFactory.buildEntryPointNode(this.reteooBuilder.getIdGenerator().getNextId(),
                                                             RuleBasePartitionId.MAIN_PARTITION,
                                                             this.getConfiguration().isMultithreadEvaluation(),
                                                             this.rete,
                                                             EntryPointId.DEFAULT);
        epn.attach();

        BuildContext context = new BuildContext(this);
        context.setCurrentEntryPoint(epn.getEntryPoint());
        context.setTupleMemoryEnabled(true);
        context.setObjectTypeNodeMemoryEnabled(true);
        context.setPartitionId(RuleBasePartitionId.MAIN_PARTITION);

        ObjectTypeNode otn = nodeFactory.buildObjectTypeNode(this.reteooBuilder.getIdGenerator().getNextId(),
                                                             epn,
                                                             ClassObjectType.InitialFact_ObjectType,
                                                             context);
        otn.attach(context);
    }

    public void registerAddedEntryNodeCache(EntryPointNode node) {
        if (addedEntryNodeCache == null) addedEntryNodeCache = new HashSet<>();
        addedEntryNodeCache.add(node);
    }

    public Set<EntryPointNode> getAddedEntryNodeCache() {
        return addedEntryNodeCache;
    }

    public void registeRremovedEntryNodeCache(EntryPointNode node) {
        if (removedEntryNodeCache == null) removedEntryNodeCache = new HashSet<>();
        removedEntryNodeCache.add(node);
    }

    public Set<EntryPointNode> getRemovedEntryNodeCache() {
        return removedEntryNodeCache;
    }

    /**
     * Retrieve the Rete-OO network for this <code>RuleBase</code>.
     *
     * @return The RETE-OO network.
     */
    public Rete getRete() {
        return this.rete;
    }

    public ReteooBuilder getReteooBuilder() {
        return this.reteooBuilder;
    }

    public int getNodeCount() {
        // may start in 0
        return this.reteooBuilder.getIdGenerator().getLastId() + 1;
    }

    public int getMemoryCount(String topic) {
        // may start in 0
        return this.reteooBuilder.getIdGenerator().getLastId(topic) + 1;
    }

    public void registerSegmentPrototype(LeftTupleSource tupleSource, SegmentMemory smem) {
        segmentProtos.put(tupleSource.getId(), smem.asPrototype());
    }

    public boolean hasSegmentPrototypes() {
        return !segmentProtos.isEmpty();
    }

    public void invalidateSegmentPrototype(LeftTupleNode rootNode) {
        segmentProtos.remove(rootNode.getId());
    }

    public SegmentMemory createSegmentFromPrototype(InternalWorkingMemory wm, LeftTupleSource tupleSource) {
        SegmentMemory.Prototype proto = segmentProtos.get(tupleSource.getId());
        if (proto == null) {
            return null;
        }
        return proto.newSegmentMemory(wm);
    }

    public SegmentMemory.Prototype getSegmentPrototype(SegmentMemory segment) {
        return segmentProtos.get(segment.getRootNode().getId());
    }

    private static class TypeDeclarationCandidate {

        public TypeDeclaration candidate = null;
        public int             score     = Integer.MAX_VALUE;
    }

    public TypeDeclaration getExactTypeDeclaration( Class<?> clazz ) {
        return this.classTypeDeclaration.get( clazz.getName() );
    }

    public TypeDeclaration getOrCreateExactTypeDeclaration( Class<?> clazz ) {
        return this.classTypeDeclaration.computeIfAbsent( clazz.getName(), c -> TypeDeclaration.createTypeDeclarationForBean( clazz ) );
    }

    public TypeDeclaration getTypeDeclaration( Class<?> clazz ) {
        TypeDeclaration typeDeclaration = getExactTypeDeclaration( clazz );
        if (typeDeclaration == null) {
            // check super classes and keep a score of how up in the hierarchy is there a declaration
            TypeDeclarationCandidate candidate = checkSuperClasses( clazz );
            // now check interfaces
            candidate = checkInterfaces( clazz,
                                         candidate,
                                         1 );
            if (candidate != null) {
                typeDeclaration = candidate.candidate;
            }
        }
        return typeDeclaration;
    }

    private TypeDeclarationCandidate checkSuperClasses( Class<?> clazz ) {

        TypeDeclaration typeDeclaration = null;
        Class<?> current = clazz.getSuperclass();
        int score = 0;
        while ( typeDeclaration == null && current != null ) {
            score++;
            typeDeclaration = this.classTypeDeclaration.get( current.getName() );
            current = current.getSuperclass();
        }
        TypeDeclarationCandidate candidate = null;
        if ( typeDeclaration != null ) {
            candidate = new TypeDeclarationCandidate();
            candidate.candidate = typeDeclaration;
            candidate.score = score;
        }
        return candidate;
    }

    private TypeDeclarationCandidate checkInterfaces( Class<?> clazz,
                                                      TypeDeclarationCandidate baseline,
                                                      int level ) {
        TypeDeclarationCandidate candidate = null;
        if (baseline == null || level < baseline.score) {
            // search
            TypeDeclaration typeDeclaration;
            for (Class<?> ifc : clazz.getInterfaces()) {
                typeDeclaration = this.classTypeDeclaration.get( ifc.getName() );
                if (typeDeclaration != null) {
                    candidate = new TypeDeclarationCandidate();
                    candidate.candidate = typeDeclaration;
                    candidate.score = level;
                    break;
                } else {
                    candidate = checkInterfaces( ifc,
                                                 baseline,
                                                 level + 1 );
                }
            }
        } else {
            candidate = baseline;
        }
        return candidate;
    }

    public Collection<TypeDeclaration> getTypeDeclarations() {
        return this.classTypeDeclaration.values();
    }

    public void addRules( Collection<RuleImpl> rules ) throws InvalidPatternException {
        enqueueModification( () -> {
            for (RuleImpl rule : rules) {
                internalAddRule( rule );
            }
        });
    }

    private void internalAddRule( RuleImpl rule ) {
        this.eventSupport.fireBeforeRuleAdded( rule );
        this.reteooBuilder.addRule(rule);
        this.eventSupport.fireAfterRuleAdded( rule );
    }

    public void removeQuery( final String packageName,
                             final String ruleName ) {
        removeRule(packageName,
                   ruleName);
    }

    public void removeRule( final String packageName,
                            final String ruleName ) {
        enqueueModification( () -> {
            final InternalKnowledgePackage pkg = pkgs.get( packageName );
            if (pkg == null) {
                throw new IllegalArgumentException( "Package name '" + packageName +
                                                    "' does not exist for this Rule Base." );
            }

            RuleImpl rule = pkg.getRule( ruleName );
            if (rule == null) {
                throw new IllegalArgumentException( "Rule name '" + ruleName +
                                                    "' does not exist in the Package '" +
                                                    packageName +
                                                    "'." );
            }

            this.eventSupport.fireBeforeRuleRemoved(rule);
            this.reteooBuilder.removeRules(Collections.singletonList(rule));
            this.eventSupport.fireAfterRuleRemoved(rule);

            pkg.removeRule( rule );
            addReloadDialectDatas( pkg.getDialectRuntimeRegistry() );
        } );
    }

    public void removeRules( Collection<RuleImpl> rules ) {
        enqueueModification( () -> internalRemoveRules( rules ) );
    }

    private void internalRemoveRules(Collection<RuleImpl> rules) {
        for (RuleImpl rule : rules) {
            this.eventSupport.fireBeforeRuleRemoved( rule );
        }
        this.reteooBuilder.removeRules(rules);
        for (RuleImpl rule : rules) {
            this.eventSupport.fireAfterRuleRemoved( rule );
        }
    }

    public void removeFunction( final String packageName,
                                final String functionName ) {
        enqueueModification( () -> internalRemoveFunction( packageName, functionName ) );
    }

    private void internalRemoveFunction( String packageName, String functionName ) {
        final InternalKnowledgePackage pkg = this.pkgs.get( packageName );
        if (pkg == null) {
            throw new IllegalArgumentException( "Package name '" + packageName +
                                                "' does not exist for this Rule Base." );
        }

        Function function = pkg.getFunctions().get( functionName );
        if (function == null) {
            throw new IllegalArgumentException( "function name '" + packageName +
                                                "' does not exist in the Package '" +
                                                packageName +
                                                "'." );
        }

        this.eventSupport.fireBeforeFunctionRemoved( pkg, functionName );
        pkg.removeFunction( functionName );
        this.eventSupport.fireAfterFunctionRemoved( pkg, functionName );
        if (rootClassLoader instanceof ProjectClassLoader ) {
            ((ProjectClassLoader)rootClassLoader).undefineClass(function.getClassName());
        }

        addReloadDialectDatas( pkg.getDialectRuntimeRegistry() );
    }

    public void addProcess( final Process process ) {
        // XXX: could use a synchronized(processes) here.
        lock();
        try {
            internalAddProcess( process );
        } finally {
            unlock();
        }
    }

    private void internalAddProcess( Process process ) {
        this.eventSupport.fireBeforeProcessAdded(process);
        this.processes.put( process.getId(), process );
        this.eventSupport.fireAfterProcessAdded(process);
    }

    public void removeProcess( final String id ) {
        enqueueModification( () -> internalRemoveProcess( id ) );
    }

    private void internalRemoveProcess( String id ) {
        Process process = this.processes.get( id );
        if ( process == null ) {
            throw new IllegalArgumentException( "Process '" + id + "' does not exist for this Rule Base." );
        }
        this.eventSupport.fireBeforeProcessRemoved( process );
        this.processes.remove( id );
        this.pkgs.get( process.getPackageName() ).removeRuleFlow( id );
        this.eventSupport.fireAfterProcessRemoved( process );
    }

    public Process getProcess( final String id ) {
        readLock();
        try {
            return this.processes.get( id );
        } finally {
            readUnlock();
        }
    }

    public void addStatefulSession( StatefulKnowledgeSessionImpl wm ) {
        this.statefulSessions.add( wm );
    }

    public InternalKnowledgePackage getPackage( final String name ) {
        return this.pkgs.get( name );
    }

    public Collection<InternalWorkingMemory> getWorkingMemories() {
        return Collections.unmodifiableSet( statefulSessions );
    }

    public RuleBaseConfiguration getConfiguration() {
        if ( this.config == null ) {
            this.config = new RuleBaseConfiguration();
        }
        return this.config;
    }

    public ClassLoader getRootClassLoader() {
        return this.rootClassLoader;
    }

    public void executeQueuedActions() {
        DialectRuntimeRegistry registry;
        while (( registry = reloadPackageCompilationData.poll() ) != null) {
            registry.onBeforeExecute();
        }
    }

    public RuleBasePartitionId createNewPartitionId() {
        return RuleBasePartitionId.createPartition();
    }

    public FactType getFactType(String packageName, String typeName) {
        String name = packageName + "." + typeName;
        readLock();
        try {
            for (InternalKnowledgePackage pkg : this.pkgs.values()) {
                FactType type = pkg.getFactType( name );
                if (type != null) {
                    return type;
                }
            }
            return null;
        } finally {
            readUnlock();
        }
    }

    private void addReloadDialectDatas( DialectRuntimeRegistry registry ) {
        this.reloadPackageCompilationData.offer( registry );
    }

    public ClassFieldAccessorCache getClassFieldAccessorCache() {
        return this.classFieldAccessorCache;
    }

    public Set<String> getEntryPointIds() {
        Set<String> entryPointIds = new HashSet<>();
        for (InternalKnowledgePackage pkg : this.pkgs.values()) {
            entryPointIds.addAll( pkg.getEntryPointIds() );
        }
        return entryPointIds;
    }

    public TripleStore getTripleStore() {
        return this.getConfiguration().getComponentFactory().getTripleStore();
    }

    public TraitRegistry getTraitRegistry() {
        return this.getConfiguration().getComponentFactory().getTraitRegistry();
    }

    public boolean removeObjectsGeneratedFromResource(Resource resource) {
        boolean modified = false;
        for (InternalKnowledgePackage pkg : pkgs.values()) {
            List<RuleImpl> rulesToBeRemoved = pkg.getRulesGeneratedFromResource(resource);
            if (!rulesToBeRemoved.isEmpty()) {
                this.reteooBuilder.removeRules( rulesToBeRemoved );
                // removal of rule from package has to be delayed after the rule has been removed from the phreak network
                // in order to allow the correct flushing of all outstanding staged tuples
                for (RuleImpl rule : rulesToBeRemoved) {
                    pkg.removeRule(rule);
                }
            }

            List<Function> functionsToBeRemoved = pkg.removeFunctionsGeneratedFromResource(resource);
            for (Function function : functionsToBeRemoved) {
                internalRemoveFunction(pkg.getName(), function.getName());
            }

            List<Process> processesToBeRemoved = pkg.removeProcessesGeneratedFromResource(resource);
            for (Process process : processesToBeRemoved) {
                processes.remove(process.getId());
            }

            List<TypeDeclaration> removedTypes = pkg.removeTypesGeneratedFromResource(resource);

            boolean resourceTypePackageSomethingRemoved = pkg.removeFromResourceTypePackageGeneratedFromResource( resource );

            modified |= !rulesToBeRemoved.isEmpty()
                        || !functionsToBeRemoved.isEmpty()
                        || !processesToBeRemoved.isEmpty()
                        || !removedTypes.isEmpty()
                        || resourceTypePackageSomethingRemoved;
        }
        return modified;
    }

    @Override
    public ReleaseId getResolvedReleaseId() {
        return resolvedReleaseId;
    }

    @Override
    public void setResolvedReleaseId(ReleaseId currentReleaseId) {
        this.resolvedReleaseId = currentReleaseId;
    }

    @Override
    public String getContainerId() {
        return containerId;
    }

    @Override
    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    @Override
    public void setKieContainer( InternalKieContainer kieContainer ) {
        this.kieContainer = kieContainer;
    }

    public InternalKieContainer getKieContainer() {
       return this.kieContainer;
    }

    public RuleUnitDescriptionRegistry getRuleUnitDescriptionRegistry() {
        return ruleUnitDescriptionRegistry;
    }

    public boolean hasUnits() {
        return ruleUnitDescriptionRegistry.hasUnits();
    }

    public List<AsyncReceiveNode> getReceiveNodes() {
        return receiveNodes;
    }

    public void addReceiveNode( AsyncReceiveNode node) {
        if (receiveNodes == null) {
            receiveNodes = new ArrayList<>();
        }
        receiveNodes.add(node);
    }

    public AlphaNodeOrderingStrategy getAlphaNodeOrderingStrategy() {
        return alphaNodeOrderingStrategy;
    }
}
