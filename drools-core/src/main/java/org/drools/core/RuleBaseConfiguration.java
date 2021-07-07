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

package org.drools.core;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.drools.core.base.CoreComponentsBuilder;
import org.drools.core.common.AgendaGroupFactory;
import org.drools.core.reteoo.KieComponentFactory;
import org.drools.core.runtime.rule.impl.DefaultConsequenceExceptionHandler;
import org.drools.core.spi.ConflictResolver;
import org.drools.core.util.ConfFileUtils;
import org.drools.core.util.StringUtils;
import org.drools.reflective.classloader.ProjectClassLoader;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.conf.BetaRangeIndexOption;
import org.kie.api.conf.DeclarativeAgendaOption;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.conf.KieBaseMutabilityOption;
import org.kie.api.conf.KieBaseOption;
import org.kie.api.conf.MBeansOption;
import org.kie.api.conf.MultiValueKieBaseOption;
import org.kie.api.conf.RemoveIdentitiesOption;
import org.kie.api.conf.SequentialOption;
import org.kie.api.conf.SessionsPoolOption;
import org.kie.api.conf.SingleValueKieBaseOption;
import org.kie.api.runtime.rule.ConsequenceExceptionHandler;
import org.kie.internal.builder.conf.ClassLoaderCacheOption;
import org.kie.internal.conf.AlphaRangeIndexThresholdOption;
import org.kie.internal.conf.AlphaThresholdOption;
import org.kie.internal.conf.CompositeKeyDepthOption;
import org.kie.internal.conf.ConsequenceExceptionHandlerOption;
import org.kie.internal.conf.ConstraintJittingThresholdOption;
import org.kie.internal.conf.IndexLeftBetaMemoryOption;
import org.kie.internal.conf.IndexPrecedenceOption;
import org.kie.internal.conf.IndexRightBetaMemoryOption;
import org.kie.internal.conf.MaxThreadsOption;
import org.kie.internal.conf.MultithreadEvaluationOption;
import org.kie.internal.conf.PermGenThresholdOption;
import org.kie.internal.conf.SequentialAgendaOption;
import org.kie.internal.conf.ShareAlphaNodesOption;
import org.kie.internal.conf.ShareBetaNodesOption;
import org.kie.internal.utils.ChainedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.core.reteoo.KieComponentFactory.createKieComponentFactory;
import static org.drools.core.util.Drools.isJmxAvailable;
import static org.drools.core.util.MemoryUtil.hasPermGen;

/**
 * RuleBaseConfiguration
 *
 * A class to store RuleBase related configuration. It must be used at rule base instantiation time
 * or not used at all.
 * This class will automatically load default values from system properties, so if you want to set
 * a default configuration value for all your new rule bases, you can simply set the property as
 * a System property.
 *
 * After RuleBase is created, it makes the configuration immutable and there is no way to make it
 * mutable again. This is to avoid inconsistent behavior inside rulebase.
 *
 * NOTE: This API is under review and may change in the future.
 */

/**
 * Available configuration options:
 * <pre>
 * drools.maintainTms = &lt;true|false&gt;
 * drools.sequential = &lt;true|false&gt;
 * drools.sequential.agenda = &lt;sequential|dynamic&gt;
 * drools.removeIdentities = &lt;true|false&gt;
 * drools.shareAlphaNodes  = &lt;true|false&gt;
 * drools.shareBetaNodes = &lt;true|false&gt;
 * drools.alphaNodeHashingThreshold = &lt;1...n&gt;
 * drools.alphaNodeRangeIndexThreshold = &lt;1...n&gt;
 * drools.betaNodeRangeIndexEnabled = &lt;true|false&gt;
 * drools.sessionPool = &lt;1...n&gt;
 * drools.compositeKeyDepth = &lt;1..3&gt;
 * drools.indexLeftBetaMemory = &lt;true/false&gt;
 * drools.indexRightBetaMemory = &lt;true/false&gt;
 * drools.equalityBehavior = &lt;identity|equality&gt;
 * drools.conflictResolver = &lt;qualified class name&gt;
 * drools.consequenceExceptionHandler = &lt;qualified class name&gt;
 * drools.ruleBaseUpdateHandler = &lt;qualified class name&gt;
 * drools.sessionClock = &lt;qualified class name&gt;
 * drools.mbeans = &lt;enabled|disabled&gt;
 * drools.classLoaderCacheEnabled = &lt;true|false&gt;
 * drools.declarativeAgendaEnabled =  &lt;true|false&gt;
 * drools.permgenThreshold = &lt;1...n&gt;
 * drools.jittingThreshold = &lt;1...n&gt;
 * </pre>
 */
public class RuleBaseConfiguration
    implements
        KieBaseConfiguration,
    Externalizable {
    private static final long serialVersionUID = 510l;

    public static final boolean DEFAULT_PHREAK = true;
    public static final boolean DEFAULT_SESSION_CACHE = true;

    public static final String DEFAULT_SIGN_ON_SERIALIZATION = "false";

    protected static final transient Logger logger = LoggerFactory.getLogger(RuleBaseConfiguration.class);

    private ChainedProperties chainedProperties;

    private boolean immutable;

    private boolean          sequential;
    private SequentialAgenda sequentialAgenda;

    private boolean         maintainTms;
    private boolean         removeIdentities;
    private boolean         shareAlphaNodes;
    private boolean         shareBetaNodes;
    private int             permGenThreshold;
    private int             jittingThreshold;
    private int             alphaNodeHashingThreshold;
    private int             alphaNodeRangeIndexThreshold;
    private boolean         betaNodeRangeIndexEnabled;
    private int             compositeKeyDepth;
    private boolean         indexLeftBetaMemory;
    private boolean         indexRightBetaMemory;
    private AssertBehaviour assertBehaviour;
    private String          consequenceExceptionHandler;
    private String          ruleBaseUpdateHandler;
    private boolean         classLoaderCacheEnabled;
    private boolean         mutabilityEnabled;

    private boolean declarativeAgenda;

    private EventProcessingOption eventProcessingMode;

    private IndexPrecedenceOption indexPrecedenceOption;

    // if "true", rulebase builder will try to split
    // the rulebase into multiple partitions that can be evaluated
    // in parallel by using multiple internal threads
    private boolean multithread;
    private int     maxThreads;

    // this property activates MBean monitoring and management
    private boolean mbeansEnabled;

    private ConflictResolver conflictResolver;

    private Map<String, ActivationListenerFactory> activationListeners;

    private List<Map<String, Object>> workDefinitions;
    private boolean                   advancedProcessRuleIntegration;

    private transient ClassLoader classLoader;

    private KieComponentFactory componentFactory;

    private int sessionPoolSize;

    private static class DefaultRuleBaseConfigurationHolder {
        private static final RuleBaseConfiguration defaultConf = new RuleBaseConfiguration();
    }

    public static RuleBaseConfiguration getDefaultInstance() {
        return DefaultRuleBaseConfigurationHolder.defaultConf;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(chainedProperties);
        out.writeBoolean(immutable);
        out.writeBoolean(sequential);
        out.writeObject(sequentialAgenda);
        out.writeBoolean(maintainTms);
        out.writeBoolean(removeIdentities);
        out.writeBoolean(shareAlphaNodes);
        out.writeBoolean(shareBetaNodes);
        out.writeInt(permGenThreshold);
        out.writeInt(jittingThreshold);
        out.writeInt(alphaNodeHashingThreshold);
        out.writeInt(alphaNodeRangeIndexThreshold);
        out.writeBoolean(betaNodeRangeIndexEnabled);
        out.writeInt(compositeKeyDepth);
        out.writeBoolean(indexLeftBetaMemory);
        out.writeBoolean(indexRightBetaMemory);
        out.writeObject(indexPrecedenceOption);
        out.writeObject(assertBehaviour);
        out.writeObject(consequenceExceptionHandler);
        out.writeObject(ruleBaseUpdateHandler);
        out.writeObject(conflictResolver);
        out.writeBoolean(advancedProcessRuleIntegration);
        out.writeBoolean(multithread);
        out.writeInt(maxThreads);
        out.writeObject(eventProcessingMode);
        out.writeBoolean(classLoaderCacheEnabled);
        out.writeBoolean(declarativeAgenda);
        out.writeObject(componentFactory);
        out.writeInt(sessionPoolSize);
        out.writeBoolean(mutabilityEnabled);
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        chainedProperties = (ChainedProperties) in.readObject();
        immutable = in.readBoolean();
        sequential = in.readBoolean();
        sequentialAgenda = (SequentialAgenda) in.readObject();
        maintainTms = in.readBoolean();
        removeIdentities = in.readBoolean();
        shareAlphaNodes = in.readBoolean();
        shareBetaNodes = in.readBoolean();
        permGenThreshold = in.readInt();
        jittingThreshold = in.readInt();
        alphaNodeHashingThreshold = in.readInt();
        alphaNodeRangeIndexThreshold = in.readInt();
        betaNodeRangeIndexEnabled = in.readBoolean();
        compositeKeyDepth = in.readInt();
        indexLeftBetaMemory = in.readBoolean();
        indexRightBetaMemory = in.readBoolean();
        indexPrecedenceOption = (IndexPrecedenceOption) in.readObject();
        assertBehaviour = (AssertBehaviour) in.readObject();
        consequenceExceptionHandler = (String) in.readObject();
        ruleBaseUpdateHandler = (String) in.readObject();
        conflictResolver = (ConflictResolver) in.readObject();
        advancedProcessRuleIntegration = in.readBoolean();
        multithread = in.readBoolean();
        maxThreads = in.readInt();
        eventProcessingMode = (EventProcessingOption) in.readObject();
        classLoaderCacheEnabled = in.readBoolean();
        declarativeAgenda = in.readBoolean();
        componentFactory = (KieComponentFactory) in.readObject();
        sessionPoolSize = in.readInt();
        mutabilityEnabled = in.readBoolean();
    }

    /**
     * Creates a new rulebase configuration using the provided properties
     * as configuration options. Also, if a Thread.currentThread().getContextClassLoader()
     * returns a non-null class loader, it will be used as the parent classloader
     * for this rulebase class loaders, otherwise, the RuleBaseConfiguration.class.getClassLoader()
     * class loader will be used.
     *
     * @param properties
     */
    public RuleBaseConfiguration(Properties properties) {
        init(properties,
             null);
    }

    /**
     * Creates a new rulebase with a default parent class loader set according
     * to the following algorithm:
     *
     * If a Thread.currentThread().getContextClassLoader() returns a non-null class loader,
     * it will be used as the parent class loader for this rulebase class loaders, otherwise,
     * the RuleBaseConfiguration.class.getClassLoader() class loader will be used.
     */
    public RuleBaseConfiguration() {
        init(null,
             null);
    }

    /**
     * A constructor that sets the parent classloader to be used
     * while dealing with this rule base
     *
     * @param classLoaders
     */
    public RuleBaseConfiguration(ClassLoader... classLoaders) {
        init(null,
             classLoaders);
    }

    public void setProperty(String name,
                            String value) {
        name = name.trim();
        if (StringUtils.isEmpty(name)) {
            return;
        }

        if (name.equals(SequentialAgendaOption.PROPERTY_NAME)) {
            setSequentialAgenda(SequentialAgenda.determineSequentialAgenda(StringUtils.isEmpty(value) ? "sequential" : value));
        } else if (name.equals(SequentialOption.PROPERTY_NAME)) {
            setSequential(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
        } else if (name.equals(RemoveIdentitiesOption.PROPERTY_NAME)) {
            setRemoveIdentities(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
        } else if (name.equals(ShareAlphaNodesOption.PROPERTY_NAME)) {
            setShareAlphaNodes(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
        } else if ( name.equals( ShareBetaNodesOption.PROPERTY_NAME ) ) {
            setShareBetaNodes(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
        } else if ( name.equals( PermGenThresholdOption.PROPERTY_NAME ) ) {
            setPermGenThreshold(StringUtils.isEmpty(value) ? PermGenThresholdOption.DEFAULT_VALUE : Integer.parseInt(value));
        } else if ( name.equals( ConstraintJittingThresholdOption.PROPERTY_NAME ) ) {
            setJittingThreshold( StringUtils.isEmpty( value ) ? ConstraintJittingThresholdOption.DEFAULT_VALUE : Integer.parseInt( value ) );
        } else if ( name.equals( AlphaThresholdOption.PROPERTY_NAME ) ) {
            setAlphaNodeHashingThreshold( StringUtils.isEmpty( value ) ? 3 : Integer.parseInt(value));
        } else if ( name.equals( AlphaRangeIndexThresholdOption.PROPERTY_NAME ) ) {
            setAlphaNodeRangeIndexThreshold( StringUtils.isEmpty( value ) ? AlphaRangeIndexThresholdOption.DEFAULT_VALUE : Integer.parseInt(value));
        } else if ( name.equals( BetaRangeIndexOption.PROPERTY_NAME ) ) {
            setBetaNodeRangeIndexEnabled( StringUtils.isEmpty( value ) ? false : Boolean.valueOf(value));
        } else if ( name.equals( SessionsPoolOption.PROPERTY_NAME ) ) {
            setSessionPoolSize( StringUtils.isEmpty( value ) ? -1 : Integer.parseInt(value));
        } else if ( name.equals( CompositeKeyDepthOption.PROPERTY_NAME ) ) {
            setCompositeKeyDepth( StringUtils.isEmpty( value ) ? 3 : Integer.parseInt(value));
        } else if ( name.equals( IndexLeftBetaMemoryOption.PROPERTY_NAME ) ) {
            setIndexLeftBetaMemory( StringUtils.isEmpty( value ) ? true : Boolean.valueOf(value));
        } else if ( name.equals( IndexRightBetaMemoryOption.PROPERTY_NAME ) ) {
            setIndexRightBetaMemory( StringUtils.isEmpty( value ) ? true : Boolean.valueOf(value));
        } else if ( name.equals( IndexPrecedenceOption.PROPERTY_NAME ) ) {
            setIndexPrecedenceOption( StringUtils.isEmpty( value ) ? IndexPrecedenceOption.EQUALITY_PRIORITY : IndexPrecedenceOption.determineIndexPrecedence(value));
        } else if ( name.equals( EqualityBehaviorOption.PROPERTY_NAME ) ) {
            setAssertBehaviour( AssertBehaviour.determineAssertBehaviour( StringUtils.isEmpty( value ) ? "identity" : value));
        } else if ( name.equals( ConsequenceExceptionHandlerOption.PROPERTY_NAME ) ) {
            setConsequenceExceptionHandler( StringUtils.isEmpty( value ) ? DefaultConsequenceExceptionHandler.class.getName() : value);
        } else if ( name.equals( "drools.ruleBaseUpdateHandler" ) ) {
            setRuleBaseUpdateHandler( StringUtils.isEmpty( value ) ? "" : value);
        } else if ( name.equals( "drools.conflictResolver" ) ) {
            setAdvancedProcessRuleIntegration( StringUtils.isEmpty( value ) ? false : Boolean.valueOf(value));
        } else if ( name.equals( MultithreadEvaluationOption.PROPERTY_NAME ) ) {
            setMultithreadEvaluation( StringUtils.isEmpty( value ) ? false : Boolean.valueOf(value));
        } else if ( name.equals( MaxThreadsOption.PROPERTY_NAME ) ) {
            setMaxThreads( StringUtils.isEmpty( value ) ? 3 : Integer.parseInt(value));
        } else if ( name.equals( EventProcessingOption.PROPERTY_NAME ) ) {
            setEventProcessingMode( EventProcessingOption.determineEventProcessingMode( StringUtils.isEmpty( value ) ? "cloud" : value));
        } else if ( name.equals( MBeansOption.PROPERTY_NAME ) ) {
            setMBeansEnabled( MBeansOption.isEnabled(value));
        } else if ( name.equals( ClassLoaderCacheOption.PROPERTY_NAME ) ) {
            setClassLoaderCacheEnabled( StringUtils.isEmpty( value ) ? true : Boolean.valueOf(value));
        } else if ( name.equals( KieBaseMutabilityOption.PROPERTY_NAME ) ) {
            setMutabilityEnabled( StringUtils.isEmpty( value ) ? true : KieBaseMutabilityOption.determineMutability(value) == KieBaseMutabilityOption.ALLOWED );
        }
    }

    public String getProperty(String name) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return null;
        }

        if ( name.equals( SequentialAgendaOption.PROPERTY_NAME ) ) {
            return getSequentialAgenda().toExternalForm();
        } else if ( name.equals( SequentialOption.PROPERTY_NAME ) ) {
            return Boolean.toString( isSequential() );
        } else if ( name.equals( RemoveIdentitiesOption.PROPERTY_NAME ) ) {
            return Boolean.toString( isRemoveIdentities() );
        } else if ( name.equals( ShareAlphaNodesOption.PROPERTY_NAME ) ) {
            return Boolean.toString( isShareAlphaNodes() );
        } else if ( name.equals( ShareBetaNodesOption.PROPERTY_NAME ) ) {
            return Boolean.toString( isShareBetaNodes() );
        } else if ( name.equals( PermGenThresholdOption.PROPERTY_NAME ) ) {
            return Integer.toString( getPermGenThreshold() );
        } else if ( name.equals( ConstraintJittingThresholdOption.PROPERTY_NAME ) ) {
            return Integer.toString( getJittingThreshold() );
        } else if ( name.equals( AlphaThresholdOption.PROPERTY_NAME ) ) {
            return Integer.toString( getAlphaNodeHashingThreshold() );
        } else if ( name.equals( AlphaRangeIndexThresholdOption.PROPERTY_NAME ) ) {
            return Integer.toString( getAlphaNodeRangeIndexThreshold() );
        } else if ( name.equals( BetaRangeIndexOption.PROPERTY_NAME ) ) {
            return Boolean.toString( isBetaNodeRangeIndexEnabled() );
        } else if ( name.equals( SessionsPoolOption.PROPERTY_NAME ) ) {
            return Integer.toString( getSessionPoolSize() );
        } else if ( name.equals( CompositeKeyDepthOption.PROPERTY_NAME ) ) {
            return Integer.toString( getCompositeKeyDepth() );
        } else if ( name.equals( IndexLeftBetaMemoryOption.PROPERTY_NAME ) ) {
            return Boolean.toString( isIndexLeftBetaMemory() );
        } else if ( name.equals( IndexRightBetaMemoryOption.PROPERTY_NAME ) ) {
            return Boolean.toString( isIndexRightBetaMemory());
        } else if ( name.equals( IndexPrecedenceOption.PROPERTY_NAME ) ) {
            return getIndexPrecedenceOption().getValue();
        } else if ( name.equals( EqualityBehaviorOption.PROPERTY_NAME ) ) {
            return getAssertBehaviour().toExternalForm();
        } else if ( name.equals( ConsequenceExceptionHandlerOption.PROPERTY_NAME ) ) {
            return getConsequenceExceptionHandler();
        } else if ( name.equals( "drools.ruleBaseUpdateHandler" ) ) {
            return getRuleBaseUpdateHandler();
        } else if ( name.equals( "drools.advancedProcessRuleIntegration" ) ) {
            return Boolean.toString(isAdvancedProcessRuleIntegration());
        } else if ( name.equals( MultithreadEvaluationOption.PROPERTY_NAME ) ) {
            return Boolean.toString( isMultithreadEvaluation() );
        } else if ( name.equals( MaxThreadsOption.PROPERTY_NAME ) ) {
            return Integer.toString( getMaxThreads());
        } else if ( name.equals( EventProcessingOption.PROPERTY_NAME ) ) {
            return getEventProcessingMode().getMode();
        } else if ( name.equals( MBeansOption.PROPERTY_NAME ) ) {
            return isMBeansEnabled() ? "enabled" : "disabled";
        } else if ( name.equals( ClassLoaderCacheOption.PROPERTY_NAME ) ) {
            return Boolean.toString( isClassLoaderCacheEnabled() );
        } else if ( name.equals( KieBaseMutabilityOption.PROPERTY_NAME ) ) {
            return isMutabilityEnabled() ? "ALLOWED" : "DISABLED";
        }

        return null;
    }

    /**
     * A constructor that sets the classloader to be used as the parent classloader
     * of this rule base classloaders, and the properties to be used
     * as base configuration options
     *
     * @param classLoaders
     * @param properties
     */
    public RuleBaseConfiguration(Properties properties,
                                 ClassLoader... classLoaders) {
        init( properties,
              classLoaders );
    }
    
    private void init(Properties properties, ClassLoader... classLoaders) {
        if (classLoaders != null && classLoaders.length > 1) {
            throw new RuntimeException("Multiple classloaders are no longer supported");
        }
        setClassLoader( classLoaders == null || classLoaders.length == 0 ? null : classLoaders[0] );
        init(properties);
    }
    
    private void init(Properties properties) {
        this.componentFactory = createKieComponentFactory();

        this.immutable = false;

        this.chainedProperties = ChainedProperties.getChainedProperties( this.classLoader );

        if ( properties != null ) {
            this.chainedProperties.addProperties( properties );
        }

        setRemoveIdentities(Boolean.valueOf(this.chainedProperties.getProperty("drools.removeIdentities", "false")).booleanValue());

        setShareAlphaNodes(Boolean.valueOf(this.chainedProperties.getProperty(ShareAlphaNodesOption.PROPERTY_NAME, "true")).booleanValue());

        setShareBetaNodes(Boolean.valueOf(this.chainedProperties.getProperty(ShareBetaNodesOption.PROPERTY_NAME, "true")).booleanValue());

        setPermGenThreshold(Integer.parseInt(this.chainedProperties.getProperty(PermGenThresholdOption.PROPERTY_NAME, "" + PermGenThresholdOption.DEFAULT_VALUE)));

        setJittingThreshold( Integer.parseInt( this.chainedProperties.getProperty( ConstraintJittingThresholdOption.PROPERTY_NAME, "" + ConstraintJittingThresholdOption.DEFAULT_VALUE)));

        setAlphaNodeHashingThreshold(Integer.parseInt(this.chainedProperties.getProperty(AlphaThresholdOption.PROPERTY_NAME, "3")));

        setAlphaNodeRangeIndexThreshold(Integer.parseInt(this.chainedProperties.getProperty(AlphaRangeIndexThresholdOption.PROPERTY_NAME, "" + AlphaRangeIndexThresholdOption.DEFAULT_VALUE)));

        setBetaNodeRangeIndexEnabled(Boolean.valueOf(this.chainedProperties.getProperty(BetaRangeIndexOption.PROPERTY_NAME, "false")));

        setSessionPoolSize(Integer.parseInt(this.chainedProperties.getProperty( SessionsPoolOption.PROPERTY_NAME, "-1")));

        setCompositeKeyDepth(Integer.parseInt(this.chainedProperties.getProperty(CompositeKeyDepthOption.PROPERTY_NAME, "3")));

        setIndexLeftBetaMemory(Boolean.valueOf(this.chainedProperties.getProperty(IndexLeftBetaMemoryOption.PROPERTY_NAME, "true")).booleanValue());

        setIndexRightBetaMemory(Boolean.valueOf(this.chainedProperties.getProperty(IndexRightBetaMemoryOption.PROPERTY_NAME, "true")).booleanValue());

        setIndexPrecedenceOption(IndexPrecedenceOption.determineIndexPrecedence(this.chainedProperties.getProperty(IndexPrecedenceOption.PROPERTY_NAME, "equality")));

        setAssertBehaviour(AssertBehaviour.determineAssertBehaviour(this.chainedProperties.getProperty(EqualityBehaviorOption.PROPERTY_NAME, "identity")));

        setConsequenceExceptionHandler(this.chainedProperties.getProperty(ConsequenceExceptionHandlerOption.PROPERTY_NAME, "org.drools.core.runtime.rule.impl.DefaultConsequenceExceptionHandler"));

        setRuleBaseUpdateHandler(this.chainedProperties.getProperty("drools.ruleBaseUpdateHandler", ""));

        setSequentialAgenda(SequentialAgenda.determineSequentialAgenda(this.chainedProperties.getProperty(SequentialAgendaOption.PROPERTY_NAME, "sequential")));

        setSequential(Boolean.valueOf(this.chainedProperties.getProperty(SequentialOption.PROPERTY_NAME, "false")).booleanValue());

        setAdvancedProcessRuleIntegration( Boolean.valueOf( this.chainedProperties.getProperty( "drools.advancedProcessRuleIntegration",
                                                                                                "false" ) ).booleanValue() );

        setMultithreadEvaluation( Boolean.valueOf( this.chainedProperties.getProperty( MultithreadEvaluationOption.PROPERTY_NAME,
                                                                                       "false" ) ).booleanValue() );

        setMaxThreads( Integer.parseInt( this.chainedProperties.getProperty( MaxThreadsOption.PROPERTY_NAME,
                                                                             "3" ) ) );

        setEventProcessingMode( EventProcessingOption.determineEventProcessingMode( this.chainedProperties.getProperty( EventProcessingOption.PROPERTY_NAME,
                                                                                                                        "cloud" ) ) );

        setMBeansEnabled( MBeansOption.isEnabled( this.chainedProperties.getProperty( MBeansOption.PROPERTY_NAME,
                                                                                      "disabled" ) ) );

        setClassLoaderCacheEnabled( Boolean.valueOf( this.chainedProperties.getProperty( ClassLoaderCacheOption.PROPERTY_NAME,
                                                                                         "true" ) ) );
        
        setDeclarativeAgendaEnabled( Boolean.valueOf( this.chainedProperties.getProperty( DeclarativeAgendaOption.PROPERTY_NAME,
                                                                                          "false" ) ) );

        setMutabilityEnabled( KieBaseMutabilityOption.determineMutability(
                this.chainedProperties.getProperty( KieBaseMutabilityOption.PROPERTY_NAME, "ALLOWED" )) == KieBaseMutabilityOption.ALLOWED );
    }

    /**
     * Makes the configuration object immutable. Once it becomes immutable,
     * there is no way to make it mutable again.
     * This is done to keep consistency.
     */
    public void makeImmutable() {
        this.immutable = true;
    }

    /**
     * Returns true if this configuration object is immutable or false otherwise.
     * @return
     */
    public boolean isImmutable() {
        return this.immutable;
    }

    private void checkCanChange() {
        if ( this.immutable ) {
            throw new UnsupportedOperationException( "Can't set a property after configuration becomes immutable" );
        }
    }

    public void setSequential(boolean sequential) {
        this.sequential = sequential;
    }

    public boolean isSequential() {
        return this.sequential;
    }

    public boolean isMaintainTms() {
        return this.maintainTms;
    }

    public void setMaintainTms(final boolean maintainTms) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.maintainTms = maintainTms;
    }

    public boolean isRemoveIdentities() {
        return this.removeIdentities;
    }

    public void setRemoveIdentities(final boolean removeIdentities) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.removeIdentities = removeIdentities;
    }

    public boolean isShareAlphaNodes() {
        return this.shareAlphaNodes;
    }

    public void setShareAlphaNodes(final boolean shareAlphaNodes) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.shareAlphaNodes = shareAlphaNodes;
    }

    public boolean isShareBetaNodes() {
        return this.shareBetaNodes;
    }

    public void setShareBetaNodes(final boolean shareBetaNodes) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.shareBetaNodes = shareBetaNodes;
    }

    public int getPermGenThreshold() {
        return this.permGenThreshold;
    }

    public void setPermGenThreshold(final int permGenThreshold) {
        checkCanChange(); // throws an exception if a change isn't possible;
        if (permGenThreshold < 0 || permGenThreshold > 100) {
            throw new UnsupportedOperationException( "The PermGen threshold should be a number between 0 and 100" );
        }
        if (isJmxAvailable() && !hasPermGen()) {
            if (permGenThreshold != PermGenThresholdOption.DEFAULT_VALUE) {
                logger.warn( "JVM version " + System.getProperty("java.version") + " has no PermGen space. " +
                             "Attempt to set the permgenThreshold to " + permGenThreshold + " will be ignored");
            }
            this.permGenThreshold = 100;
            return;
        }
        this.permGenThreshold = permGenThreshold;
    }

    public int getJittingThreshold() {
        return jittingThreshold;
    }

    public void setJittingThreshold( int jittingThreshold ) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.jittingThreshold = jittingThreshold;
    }

    public int getAlphaNodeHashingThreshold() {
        return this.alphaNodeHashingThreshold;
    }

    public void setAlphaNodeHashingThreshold(final int alphaNodeHashingThreshold) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.alphaNodeHashingThreshold = alphaNodeHashingThreshold;
    }

    public int getAlphaNodeRangeIndexThreshold() {
        return this.alphaNodeRangeIndexThreshold;
    }

    public void setAlphaNodeRangeIndexThreshold(final int alphaNodeRangeIndexThreshold) {
        checkCanChange();
        this.alphaNodeRangeIndexThreshold = alphaNodeRangeIndexThreshold;
    }

    public boolean isBetaNodeRangeIndexEnabled() {
        return this.betaNodeRangeIndexEnabled;
    }

    public void setBetaNodeRangeIndexEnabled(final boolean betaNodeRangeIndexEnabled) {
        checkCanChange();
        this.betaNodeRangeIndexEnabled = betaNodeRangeIndexEnabled;
    }

    public int getSessionPoolSize() {
        return this.sessionPoolSize;
    }

    public void setSessionPoolSize(final int sessionPoolSize) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.sessionPoolSize = sessionPoolSize;
    }

    public AssertBehaviour getAssertBehaviour() {
        return this.assertBehaviour;
    }

    public void setAssertBehaviour(final AssertBehaviour assertBehaviour) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.assertBehaviour = assertBehaviour;
    }

    public EventProcessingOption getEventProcessingMode() {
        return this.eventProcessingMode;
    }

    public void setEventProcessingMode(final EventProcessingOption mode) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.eventProcessingMode = mode;
    }

    public int getCompositeKeyDepth() {
        return this.compositeKeyDepth;
    }

    public void setCompositeKeyDepth(final int compositeKeyDepth) {
        if ( !this.immutable ) {
            if ( compositeKeyDepth > 3 ) {
                throw new UnsupportedOperationException( "compositeKeyDepth cannot be greater than 3" );
            }
            this.compositeKeyDepth = compositeKeyDepth;
        } else {
            throw new UnsupportedOperationException( "Can't set a property after configuration becomes immutable" );
        }
    }

    public boolean isIndexLeftBetaMemory() {
        return this.indexLeftBetaMemory;
    }

    public void setIndexLeftBetaMemory(final boolean indexLeftBetaMemory) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.indexLeftBetaMemory = indexLeftBetaMemory;
    }

    public boolean isIndexRightBetaMemory() {
        return this.indexRightBetaMemory;
    }

    public void setIndexRightBetaMemory(final boolean indexRightBetaMemory) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.indexRightBetaMemory = indexRightBetaMemory;
    }

    public IndexPrecedenceOption getIndexPrecedenceOption() {
        return this.indexPrecedenceOption;
    }

    public void setIndexPrecedenceOption(final IndexPrecedenceOption precedence) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.indexPrecedenceOption = precedence;
    }

    public String getConsequenceExceptionHandler() {
        return consequenceExceptionHandler;
    }

    public void setConsequenceExceptionHandler(String consequenceExceptionHandler) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.consequenceExceptionHandler = consequenceExceptionHandler;
    }

    public String getRuleBaseUpdateHandler() {
        return ruleBaseUpdateHandler;
    }

    public void setRuleBaseUpdateHandler(String ruleBaseUpdateHandler) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.ruleBaseUpdateHandler = ruleBaseUpdateHandler;
    }

    public AgendaGroupFactory getAgendaGroupFactory() {
        return getComponentFactory().getAgendaGroupFactory();
    }

    public SequentialAgenda getSequentialAgenda() {
        return this.sequentialAgenda;
    }

    public void setSequentialAgenda(final SequentialAgenda sequentialAgenda) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.sequentialAgenda = sequentialAgenda;
    }

    /**
     * Defines if the RuleBase should be executed using a pool of
     * threads for evaluating the rules ("true"), or if the rulebase 
     * should work in classic single thread mode ("false").
     * 
     * @param enableMultithread true for multi-thread or 
     *                     false for single-thread. Default is false.
     */
    public void setMultithreadEvaluation(boolean enableMultithread) {
        checkCanChange();
        this.multithread = enableMultithread;
    }

    public void enforceSingleThreadEvaluation() {
        this.multithread = false;
    }

    /**
     * Returns true if the partitioning of the rulebase is enabled
     * and false otherwise. Default is false.
     * 
     * @return
     */
    public boolean isMultithreadEvaluation() {
        return this.multithread;
    }

    /**
     * If multi-thread evaluation is enabled, this parameter configures the 
     * maximum number of threads each session can use for concurrent Rete
     * propagation. 
     * 
     * @param maxThreads the maximum number of threads to use. If 0 or a 
     *                   negative number is set, the engine will use number
     *                   of threads equal to the number of partitions in the
     *                   rule base. Default number of threads is 0. 
     */
    public void setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Returns the configured number of maximum threads to use for concurrent
     * propagation when multi-thread evaluation is enabled. Default is zero.
     * 
     * @return
     */
    public int getMaxThreads() {
        return this.maxThreads;
    }

    public boolean isClassLoaderCacheEnabled() {
        return this.classLoaderCacheEnabled;
    }

    public void setClassLoaderCacheEnabled(final boolean classLoaderCacheEnabled) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.classLoaderCacheEnabled = classLoaderCacheEnabled;
    }
    
    public boolean isDeclarativeAgenda() {
        return this.declarativeAgenda;
    }
    
    /**
     * Enable declarative agenda
     * @param enabled
     */
    public void setDeclarativeAgendaEnabled(boolean enabled) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.declarativeAgenda = enabled;
    }    

    public List<Map<String, Object>> getWorkDefinitions() {
        if ( this.workDefinitions == null ) {
            initWorkDefinitions();
        }
        return this.workDefinitions;

    }

    private void initWorkDefinitions() {
        this.workDefinitions = new ArrayList<Map<String, Object>>();

        // split on each space
        String locations[] = this.chainedProperties.getProperty( "drools.workDefinitions",
                                                                 "" ).split( "\\s" );

        // load each SemanticModule
        for ( String factoryLocation : locations ) {
            // trim leading/trailing spaces and quotes
            factoryLocation = factoryLocation.trim();
            if ( factoryLocation.startsWith( "\"" ) ) {
                factoryLocation = factoryLocation.substring( 1 );
            }
            if ( factoryLocation.endsWith( "\"" ) ) {
                factoryLocation = factoryLocation.substring( 0,
                                                             factoryLocation.length() - 1 );
            }
            if ( !factoryLocation.equals( "" ) ) {
                loadWorkItems( factoryLocation );
            }
        }
    }

    private void loadWorkItems(String location) {
        String content = ConfFileUtils.URLContentsToString( ConfFileUtils.getURL( location,
                                                                                  null,
                                                                                  RuleBaseConfiguration.class ) );
        try {
            this.workDefinitions.addAll(
                (List<Map<String, Object>>) CoreComponentsBuilder.get().getMVELExecutor().eval( content, new HashMap() ) );
        } catch ( Throwable t ) {
            logger.error("Error occurred while loading work definitions " + location
                    + "\nContinuing without reading these work definitions", t);
            throw new RuntimeException( "Could not parse work definitions " + location + ": " + t.getMessage() );
        }
    }

    public boolean isAdvancedProcessRuleIntegration() {
        return advancedProcessRuleIntegration;
    }

    public void setAdvancedProcessRuleIntegration(boolean advancedProcessRuleIntegration) {
        this.advancedProcessRuleIntegration = advancedProcessRuleIntegration;
    }
    
    public void addActivationListener(String name, ActivationListenerFactory factory) {
        if ( this.activationListeners == null ) {
            this.activationListeners = new HashMap<String, ActivationListenerFactory>();
        }
        this.activationListeners.put( name, factory );
    }
    
    public ActivationListenerFactory getActivationListenerFactory(String name) {
        ActivationListenerFactory factory = null;
        if ( this.activationListeners != null ) {
            factory = this.activationListeners.get( name );
        }
        
        if ( factory != null ) {
            return factory;
        } else {
            if ( "query".equals( name )) {
                return QueryActivationListenerFactory.INSTANCE;
            } else  if ( "agenda".equals( name ) || "direct".equals( name ) ) {
                return RuleActivationListenerFactory.INSTANCE;
            } 
        } 
        
        throw new IllegalArgumentException( "ActivationListenerFactory not found for '" + name + "'" );
    }

    private boolean determineShadowProxy(String userValue) {
        if ( this.isSequential() ) {
            // sequential never needs shadowing, so always override
            return false;
        }

        if ( userValue != null ) {
            return Boolean.valueOf( userValue ).booleanValue();
        } else {
            return true;
        }
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = ProjectClassLoader.getClassLoader( classLoader,
                                                              getClass(),
                                                              isClassLoaderCacheEnabled());
    }

    public KieComponentFactory getComponentFactory() {
        return componentFactory;
    }

    public void setComponentFactory(KieComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
    }

    /**
     * Defines if the RuleBase should expose management and monitoring MBeans
     *
     * @param mbeansEnabled true for multi-thread or
     *                     false for single-thread. Default is false.
     */
    public void setMBeansEnabled(boolean mbeansEnabled) {
        checkCanChange();
        this.mbeansEnabled = mbeansEnabled;
    }

    /**
     * Returns true if the management and monitoring through MBeans is active 
     *
     * @return
     */
    public boolean isMBeansEnabled() {
        return this.mbeansEnabled;
    }

    public void setMutabilityEnabled( boolean mutabilityEnabled ) {
        this.mutabilityEnabled = mutabilityEnabled;
    }

    public boolean isMutabilityEnabled() {
        return mutabilityEnabled;
    }

    public static class AssertBehaviour
            implements
            Externalizable {
        private static final long serialVersionUID = 510l;

        public static final AssertBehaviour IDENTITY = new AssertBehaviour(0);
        public static final AssertBehaviour EQUALITY = new AssertBehaviour(1);

        private int value;

        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            value = in.readInt();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(value);
        }

        public AssertBehaviour() {

        }

        private AssertBehaviour(final int value) {
            this.value = value;
        }

        public boolean equals(Object obj) {
            if (obj == this) return true;
            else if (obj instanceof AssertBehaviour) {
                AssertBehaviour that = (AssertBehaviour) obj;

                return value == that.value;
            }
            return false;
        }

        public static AssertBehaviour determineAssertBehaviour(final String value) {
            if ("IDENTITY".equalsIgnoreCase(value)) {
                return IDENTITY;
            } else if ("EQUALITY".equalsIgnoreCase(value)) {
                return EQUALITY;
            } else {
                throw new IllegalArgumentException("Illegal enum value '" + value + "' for AssertBehaviour");
            }
        }

        private Object readResolve() throws java.io.ObjectStreamException {
            switch (this.value) {
                case 0:
                    return IDENTITY;
                case 1:
                    return EQUALITY;
                default:
                    throw new IllegalArgumentException("Illegal enum value '" + this.value + "' for AssertBehaviour");
            }
        }

        public String toExternalForm() {
            return (this.value == 0) ? "identity" : "equality";
        }

        public String toString() {
            return "AssertBehaviour : " + ((this.value == 0) ? "identity" : "equality");
        }
    }

    public static class LogicalOverride
            implements
            Externalizable {
        private static final long serialVersionUID = 510l;

        public static final LogicalOverride PRESERVE = new LogicalOverride(0);
        public static final LogicalOverride DISCARD  = new LogicalOverride(1);

        private int value;

        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            value = in.readInt();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(value);
        }

        public LogicalOverride() {

        }

        private LogicalOverride(final int value) {
            this.value = value;
        }

        public static LogicalOverride determineLogicalOverride(final String value) {
            if ("PRESERVE".equalsIgnoreCase(value)) {
                return PRESERVE;
            } else if ("DISCARD".equalsIgnoreCase(value)) {
                return DISCARD;
            } else {
                throw new IllegalArgumentException("Illegal enum value '" + value + "' for LogicalOverride");
            }
        }

        private Object readResolve() throws java.io.ObjectStreamException {
            switch (this.value) {
                case 0:
                    return PRESERVE;
                case 1:
                    return DISCARD;
                default:
                    throw new IllegalArgumentException("Illegal enum value '" + this.value + "' for LogicalOverride");
            }
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof LogicalOverride) {
                return value == ((LogicalOverride) obj).value;
            }
            return false;
        }

        public String toExternalForm() {
            return (this.value == 0) ? "preserve" : "discard";
        }

        public String toString() {
            return "LogicalOverride : " + ((this.value == 0) ? "preserve" : "discard");
        }
    }

    public static class SequentialAgenda
            implements
            Externalizable {
        private static final long serialVersionUID = 510l;

        public static final SequentialAgenda SEQUENTIAL = new SequentialAgenda(0);
        public static final SequentialAgenda DYNAMIC    = new SequentialAgenda(1);

        private int value;

        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            value = in.readInt();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(value);
        }

        public SequentialAgenda() {

        }

        private SequentialAgenda(final int value) {
            this.value = value;
        }

        public static SequentialAgenda determineSequentialAgenda(final String value) {
            if ("sequential".equalsIgnoreCase(value)) {
                return SEQUENTIAL;
            } else if ("dynamic".equalsIgnoreCase(value)) {
                return DYNAMIC;
            } else {
                throw new IllegalArgumentException("Illegal enum value '" + value + "' for SequentialAgenda");
            }
        }

        private Object readResolve() throws java.io.ObjectStreamException {
            switch (this.value) {
                case 0:
                    return SEQUENTIAL;
                case 1:
                    return DYNAMIC;
                default:
                    throw new IllegalArgumentException("Illegal enum value '" + this.value + "' for SequentialAgenda");
            }
        }

        public String toExternalForm() {
            return (this.value == 0) ? "sequential" : "dynamic";
        }

        public String toString() {
            return "SequentialAgenda : " + ((this.value == 0) ? "sequential" : "dynamic");
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends SingleValueKieBaseOption> T getOption(Class<T> option) {
        if (SequentialOption.class.equals(option)) {
            return (T) (this.sequential ? SequentialOption.YES : SequentialOption.NO);
        } else if (RemoveIdentitiesOption.class.equals(option)) {
            return (T) (this.removeIdentities ? RemoveIdentitiesOption.YES : RemoveIdentitiesOption.NO);
        } else if (ShareAlphaNodesOption.class.equals(option)) {
            return (T) (this.shareAlphaNodes ? ShareAlphaNodesOption.YES : ShareAlphaNodesOption.NO);
        } else if (ShareBetaNodesOption.class.equals(option)) {
            return (T) (this.shareBetaNodes ? ShareBetaNodesOption.YES : ShareBetaNodesOption.NO);
        } else if (IndexLeftBetaMemoryOption.class.equals(option)) {
            return (T) (this.indexLeftBetaMemory ? IndexLeftBetaMemoryOption.YES : IndexLeftBetaMemoryOption.NO);
        } else if (IndexRightBetaMemoryOption.class.equals(option)) {
            return (T) (this.indexRightBetaMemory ? IndexRightBetaMemoryOption.YES : IndexRightBetaMemoryOption.NO);
        } else if (IndexPrecedenceOption.class.equals(option)) {
            return (T) getIndexPrecedenceOption();
        } else if (EqualityBehaviorOption.class.equals(option)) {
            return (T) ((this.assertBehaviour == AssertBehaviour.IDENTITY) ? EqualityBehaviorOption.IDENTITY : EqualityBehaviorOption.EQUALITY);
        } else if (SequentialAgendaOption.class.equals(option)) {
            return (T) ((this.sequentialAgenda == SequentialAgenda.SEQUENTIAL) ? SequentialAgendaOption.SEQUENTIAL : SequentialAgendaOption.DYNAMIC);
        } else if (PermGenThresholdOption.class.equals(option)) {
            return (T) PermGenThresholdOption.get(permGenThreshold);
        } else if (ConstraintJittingThresholdOption.class.equals(option)) {
            return (T) ConstraintJittingThresholdOption.get(jittingThreshold);
        } else if (AlphaThresholdOption.class.equals(option)) {
            return (T) AlphaThresholdOption.get(alphaNodeHashingThreshold);
        } else if (AlphaRangeIndexThresholdOption.class.equals(option)) {
            return (T) AlphaRangeIndexThresholdOption.get(alphaNodeRangeIndexThreshold);
        } else if (BetaRangeIndexOption.class.equals(option)) {
            return (T) (this.betaNodeRangeIndexEnabled ? BetaRangeIndexOption.ENABLED : BetaRangeIndexOption.DISABLED);
        } else if ( SessionsPoolOption.class.equals(option)) {
            return (T) SessionsPoolOption.get(sessionPoolSize);
        } else if (CompositeKeyDepthOption.class.equals(option)) {
            return (T) CompositeKeyDepthOption.get(compositeKeyDepth);
        } else if (ConsequenceExceptionHandlerOption.class.equals(option)) {
            Class<? extends ConsequenceExceptionHandler> handler;
            try {
                handler = (Class<? extends ConsequenceExceptionHandler>) Class.forName(consequenceExceptionHandler);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to resolve ConsequenceExceptionHandler class: " + consequenceExceptionHandler,
                                           e);
            }
            return (T) ConsequenceExceptionHandlerOption.get(handler);
        } else if (EventProcessingOption.class.equals(option)) {
            return (T) getEventProcessingMode();
        } else if (MaxThreadsOption.class.equals(option)) {
            return (T) MaxThreadsOption.get(getMaxThreads());
        } else if (MultithreadEvaluationOption.class.equals(option)) {
            return (T) (this.multithread ? MultithreadEvaluationOption.YES : MultithreadEvaluationOption.NO);
        } else if (MBeansOption.class.equals(option)) {
            return (T) (this.isMBeansEnabled() ? MBeansOption.ENABLED : MBeansOption.DISABLED);
        } else if (ClassLoaderCacheOption.class.equals(option)) {
            return (T) (this.isClassLoaderCacheEnabled() ? ClassLoaderCacheOption.ENABLED : ClassLoaderCacheOption.DISABLED);
        } else if (DeclarativeAgendaOption.class.equals(option)) {
            return (T) (this.isDeclarativeAgenda() ? DeclarativeAgendaOption.ENABLED : DeclarativeAgendaOption.DISABLED);
        } else if (KieBaseMutabilityOption.class.equals(option)) {
            return (T) (this.isMutabilityEnabled() ? KieBaseMutabilityOption.ALLOWED : KieBaseMutabilityOption.DISABLED);
        }
        return null;

    }

    public <T extends KieBaseOption> void setOption(T option) {
        if (option instanceof SequentialOption) {
            setSequential(((SequentialOption) option).isSequential());
        } else if (option instanceof RemoveIdentitiesOption) {
            setRemoveIdentities(((RemoveIdentitiesOption) option).isRemoveIdentities());
        } else if (option instanceof ShareAlphaNodesOption) {
            setShareAlphaNodes(((ShareAlphaNodesOption) option).isShareAlphaNodes());
        } else if (option instanceof ShareBetaNodesOption) {
            setShareBetaNodes(((ShareBetaNodesOption) option).isShareBetaNodes());
        } else if (option instanceof IndexLeftBetaMemoryOption) {
            setIndexLeftBetaMemory(((IndexLeftBetaMemoryOption) option).isIndexLeftBetaMemory());
        } else if (option instanceof IndexRightBetaMemoryOption) {
            setIndexRightBetaMemory(((IndexRightBetaMemoryOption) option).isIndexRightBetaMemory());
        } else if (option instanceof IndexPrecedenceOption) {
            setIndexPrecedenceOption((IndexPrecedenceOption) option);
        } else if (option instanceof EqualityBehaviorOption) {
            setAssertBehaviour((option == EqualityBehaviorOption.IDENTITY) ? AssertBehaviour.IDENTITY : AssertBehaviour.EQUALITY);
        } else if (option instanceof SequentialAgendaOption) {
            setSequentialAgenda((option == SequentialAgendaOption.SEQUENTIAL) ? SequentialAgenda.SEQUENTIAL : SequentialAgenda.DYNAMIC);
        } else if (option instanceof PermGenThresholdOption) {
            setPermGenThreshold(((PermGenThresholdOption) option).getThreshold());
        } else if (option instanceof ConstraintJittingThresholdOption) {
            setJittingThreshold( ( (ConstraintJittingThresholdOption) option ).getThreshold());
        } else if (option instanceof AlphaThresholdOption) {
            setAlphaNodeHashingThreshold( ( (AlphaThresholdOption) option ).getThreshold());
        } else if (option instanceof AlphaRangeIndexThresholdOption) {
            setAlphaNodeRangeIndexThreshold( ( (AlphaRangeIndexThresholdOption) option ).getThreshold());
        } else if (option instanceof BetaRangeIndexOption) {
            setBetaNodeRangeIndexEnabled( ( (BetaRangeIndexOption) option ).isBetaRangeIndexEnabled());
        } else if (option instanceof SessionsPoolOption ) {
            setSessionPoolSize( ( ( SessionsPoolOption ) option ).getSize());
        } else if (option instanceof CompositeKeyDepthOption) {
            setCompositeKeyDepth( ( (CompositeKeyDepthOption) option ).getDepth());
        } else if (option instanceof ConsequenceExceptionHandlerOption) {
            setConsequenceExceptionHandler( ( (ConsequenceExceptionHandlerOption) option ).getHandler().getName());
        } else if (option instanceof EventProcessingOption) {
            setEventProcessingMode( (EventProcessingOption) option);
        } else if (option instanceof MaxThreadsOption) {
            setMaxThreads( ( (MaxThreadsOption) option ).getMaxThreads());
        } else if (option instanceof MultithreadEvaluationOption) {
            setMultithreadEvaluation( ( (MultithreadEvaluationOption) option ).isMultithreadEvaluation());
        } else if (option instanceof MBeansOption) {
            setMBeansEnabled( ( (MBeansOption) option ).isEnabled());
        } else if (option instanceof ClassLoaderCacheOption) {
            setClassLoaderCacheEnabled( ( (ClassLoaderCacheOption) option ).isClassLoaderCacheEnabled());
        } else if (option instanceof DeclarativeAgendaOption) {
            setDeclarativeAgendaEnabled(((DeclarativeAgendaOption) option).isDeclarativeAgendaEnabled());
        } else if (option instanceof KieBaseMutabilityOption) {
            setMutabilityEnabled(((KieBaseMutabilityOption) option) == KieBaseMutabilityOption.ALLOWED);
        }

    }

    public <T extends MultiValueKieBaseOption> T getOption(Class<T> option,
                                                           String key) {
        return null;
    }

    public ChainedProperties getChainedProperties() {
        return chainedProperties;
    }
}
