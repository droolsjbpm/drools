/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.SessionConfiguration;
import org.drools.core.addon.AlphaNodeOrderingStrategy;
import org.drools.core.base.ClassFieldAccessorCache;
import org.drools.core.common.InternalAgenda;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.RuleBasePartitionId;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.factmodel.traits.TraitRegistry;
import org.drools.core.reteoo.AsyncReceiveNode;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.LeftTupleNode;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.Rete;
import org.drools.core.reteoo.ReteooBuilder;
import org.drools.core.reteoo.SegmentMemory;
import org.drools.core.rule.InvalidPatternException;
import org.drools.core.rule.TypeDeclaration;
import org.drools.core.ruleunit.RuleUnitDescriptionRegistry;
import org.drools.core.spi.FactHandleFactory;
import org.drools.core.util.TripleStore;
import org.kie.api.KieBase;
import org.kie.api.builder.ReleaseId;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.process.Process;
import org.kie.api.io.Resource;
import org.kie.api.runtime.Environment;

public interface InternalKnowledgeBase extends KieBase {

    StatefulKnowledgeSessionImpl createSession( long id, FactHandleFactory handleFactory, long propagationContext, SessionConfiguration config, InternalAgenda agenda, Environment environment );

    String getId();

    RuleBasePartitionId createNewPartitionId();

    RuleBaseConfiguration getConfiguration();

    void readLock();
    void readUnlock();

    void enqueueModification(Runnable modification);
    boolean flushModifications();

    int nextWorkingMemoryCounter();

    int getWorkingMemoryCounter();

    FactHandleFactory newFactHandleFactory();

    FactHandleFactory newFactHandleFactory(long id, long counter) throws IOException;

    Map<String, Class<?>> getGlobals();

    int getNodeCount();
    int getMemoryCount(String unitName);

    void executeQueuedActions();

    ReteooBuilder getReteooBuilder();

    void registerAddedEntryNodeCache(EntryPointNode node);
    Set<EntryPointNode> getAddedEntryNodeCache();

    void registeRremovedEntryNodeCache(EntryPointNode node);
    Set<EntryPointNode> getRemovedEntryNodeCache();

    Rete getRete();

    ClassLoader getRootClassLoader();

    void disposeStatefulSession(StatefulKnowledgeSessionImpl statefulSession);

    TripleStore getTripleStore();

    TraitRegistry getTraitRegistry();

    Class<?> registerAndLoadTypeDefinition( String className, byte[] def ) throws ClassNotFoundException;

    InternalKnowledgePackage getPackage(String name);
    Future<KiePackage> addPackage( KiePackage pkg );
    void addPackages( Collection<? extends KiePackage> newPkgs );
    Map<String, InternalKnowledgePackage> getPackagesMap();
    
    ClassFieldAccessorCache getClassFieldAccessorCache();

    Collection<InternalWorkingMemory> getWorkingMemories();

    boolean hasSegmentPrototypes();
    void invalidateSegmentPrototype(LeftTupleNode rootNode);
    SegmentMemory createSegmentFromPrototype(InternalWorkingMemory wm, LeftTupleSource tupleSource);
    SegmentMemory.Prototype getSegmentPrototype(SegmentMemory segment);

    void processAllTypesDeclaration( Collection<InternalKnowledgePackage> pkgs );

    void addRules( Collection<RuleImpl> rules ) throws InvalidPatternException;
    void removeRules( Collection<RuleImpl> rules ) throws InvalidPatternException;

    @Deprecated
    void addProcess( Process process );
    @Deprecated
    void removeProcess( final String id );

    void addGlobal(String identifier, Class clazz);
    void removeGlobal(String identifier);

    boolean removeObjectsGeneratedFromResource(Resource resource);

    TypeDeclaration getTypeDeclaration( Class<?> clazz );
    TypeDeclaration getExactTypeDeclaration( Class<?> clazz );
    TypeDeclaration getOrCreateExactTypeDeclaration( Class<?> clazz );
    Collection<TypeDeclaration> getTypeDeclarations();
    void registerTypeDeclaration( TypeDeclaration newDecl, InternalKnowledgePackage newPkg );

	ReleaseId getResolvedReleaseId();
	void setResolvedReleaseId(ReleaseId currentReleaseId);
	String getContainerId();
	void setContainerId(String containerId);
    void setKieContainer( InternalKieContainer kieContainer );
	void initMBeans();

    RuleUnitDescriptionRegistry getRuleUnitDescriptionRegistry();
    boolean hasUnits();

    SessionConfiguration getSessionConfiguration();

    List<AsyncReceiveNode> getReceiveNodes();
    void addReceiveNode(AsyncReceiveNode node);

    AlphaNodeOrderingStrategy getAlphaNodeOrderingStrategy();
}
