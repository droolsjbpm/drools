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

package org.drools.compiler.kie.builder.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.protobuf.ExtensionRegistry;
import org.appformer.maven.support.DependencyFilter;
import org.appformer.maven.support.PomModel;
import org.drools.compiler.builder.InternalKnowledgeBuilder;
import org.drools.compiler.builder.impl.KnowledgeBuilderConfigurationImpl;
import org.drools.compiler.kie.builder.impl.KieModuleCache.CompDataEntry;
import org.drools.compiler.kie.builder.impl.KieModuleCache.CompilationData;
import org.drools.compiler.kie.builder.impl.KieModuleCache.Header;
import org.drools.compiler.kie.builder.impl.KieModuleCache.KModuleCache;
import org.drools.compiler.kproject.ReleaseIdImpl;
import org.drools.compiler.kproject.models.KieBaseModelImpl;
import org.drools.core.RuleBaseConfiguration;
import org.drools.core.builder.conf.impl.DecisionTableConfigurationImpl;
import org.drools.core.builder.conf.impl.ResourceConfigurationImpl;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.impl.KnowledgePackageImpl;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.drools.core.util.Drools;
import org.drools.core.util.StringUtils;
import org.drools.reflective.ResourceProvider;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.RuleTemplateModel;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.definition.KiePackage;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceConfiguration;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.CompositeKnowledgeBuilder;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.DecisionTableInputType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.ResourceChangeSet;
import org.kie.internal.builder.RuleTemplateConfiguration;
import org.kie.internal.io.ResourceTypeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.compiler.kproject.ReleaseIdImpl.adaptAll;

public abstract class AbstractKieModule
        implements
        InternalKieModule, Serializable {

    private static final Logger log = LoggerFactory.getLogger(AbstractKieModule.class);

    private final transient Map<String, KnowledgeBuilder> kBuilders = new HashMap<>();

    private final transient Map<String, Results> resultsCache = new HashMap<>();

    protected ReleaseId releaseId;

    private transient KieModuleModel kModuleModel;

    private Map<ReleaseId, InternalKieModule> kieDependencies;

    // Map< KBaseName, CompilationCache>
    protected Map<String, CompilationCache> compilationCache = new HashMap<>();

    private transient Map<String, ResourceConfiguration> resourceConfigurationCache = new HashMap<>();

    protected transient PomModel pomModel;

    private Collection<ReleaseId> unresolvedDependencies;

    public AbstractKieModule() { }

    public AbstractKieModule(ReleaseId releaseId, KieModuleModel kModuleModel) {
        this.releaseId = releaseId;
        this.kModuleModel = kModuleModel;
    }

    public KieModuleModel getKieModuleModel() {
        return this.kModuleModel;
    }

    public Map<ReleaseId, InternalKieModule> getKieDependencies() {
        return kieDependencies == null ? Collections.<ReleaseId, InternalKieModule> emptyMap() : kieDependencies;
    }

    public void addKieDependency(InternalKieModule dependency) {
        if (kieDependencies == null) {
            kieDependencies = new HashMap<>();
        }
        kieDependencies.put(dependency.getReleaseId(), dependency);
    }

    public Collection<ReleaseId> getJarDependencies(DependencyFilter filter ) {
        if( pomModel == null ) {
            getPomModel();
        }
        Collection<ReleaseId> deps = null;
        if( pomModel != null ) {
            deps = adaptAll( pomModel.getDependencies(filter), pomModel );
        }
        return deps == null ? Collections.<ReleaseId> emptyList() : deps;
    }

    public Collection<ReleaseId> getUnresolvedDependencies() {
        return unresolvedDependencies == null ? Collections.<ReleaseId> emptyList() : unresolvedDependencies;
    }

    public void setUnresolvedDependencies(Collection<ReleaseId> unresolvedDependencies) {
        this.unresolvedDependencies = unresolvedDependencies;
    }

    public ReleaseId getReleaseId() {
        return releaseId;
    }

    @Override
    public ClassLoader getModuleClassLoader() {
        return kBuilders.isEmpty() ? null : (( InternalKnowledgeBuilder ) kBuilders.values().iterator().next()).getRootClassLoader();
    }

    public KnowledgeBuilder getKnowledgeBuilderForKieBase( String kieBaseName) {
        return kBuilders.get(kieBaseName);
    }

    public InternalKnowledgePackage getPackage(String packageName) {
        for (KnowledgeBuilder kbuilder : kBuilders.values()) {
            InternalKnowledgePackage pkg = (( InternalKnowledgeBuilder ) kbuilder).getPackage( packageName );
            if (pkg != null) {
                return pkg;
            }
        }
        return null;
    }

    @Override
    public Collection<KiePackage> getKnowledgePackagesForKieBase(String kieBaseName) {
        KnowledgeBuilder kbuilder = kBuilders.get(kieBaseName);
        return kbuilder != null ? kbuilder.getKnowledgePackages() : null;
    }

    public void cacheKnowledgeBuilderForKieBase(String kieBaseName, KnowledgeBuilder kbuilder) {
        kBuilders.put(kieBaseName, kbuilder);
    }

    public Map<String, Results> getKnowledgeResultsCache() {
        return resultsCache;
    }

    public void cacheResultsForKieBase(String kieBaseName, Results results) {
        resultsCache.put(kieBaseName, results);
    }

    public Map<String, byte[]> getClassesMap() {
        Map<String, byte[]> classes = new HashMap<>();
        for (String fileName : getFileNames()) {
            if (fileName.endsWith(".class")) {
                classes.put(fileName, getBytes(fileName));
            }
        }
        return classes;
    }

    public KnowledgePackagesBuildResult buildKnowledgePackages(KieBaseModelImpl kBaseModel, KieProject kieProject, ResultsImpl messages) {
        Collection<KiePackage> pkgs = getKnowledgePackagesForKieBase(kBaseModel.getName());

        if ( pkgs == null ) {
            KnowledgeBuilder kbuilder = kieProject.buildKnowledgePackages(kBaseModel, messages);
            if ( kbuilder.hasErrors() ) {
                // Messages already populated by the buildKnowlegePackages
                return new KnowledgePackagesBuildResult(true, pkgs);
            }
            pkgs = kbuilder.getKnowledgePackages();
        }

        return new KnowledgePackagesBuildResult(false, pkgs);
    }

    public InternalKnowledgeBase createKieBase( KieBaseModelImpl kBaseModel, KieProject kieProject, ResultsImpl messages, KieBaseConfiguration conf ) {
        KnowledgePackagesBuildResult knowledgePackagesBuildResult = buildKnowledgePackages(kBaseModel, kieProject, messages);
        if(knowledgePackagesBuildResult.hasErrors()) {
            return null;
        }

        Collection<KiePackage> pkgs = knowledgePackagesBuildResult.getPkgs();
        checkStreamMode( kBaseModel, conf, pkgs );

        ClassLoader cl = kieProject.getClassLoader();
        if (conf == null) {
            conf = getKnowledgeBaseConfiguration(kBaseModel, cl);
        } else if (conf instanceof RuleBaseConfiguration ) {
            ((RuleBaseConfiguration)conf).setClassLoader(cl);
        }

        InternalKnowledgeBase kBase = KnowledgeBaseFactory.newKnowledgeBase(kBaseModel.getName(), conf );
        kBase.addPackages( pkgs );
        return kBase;
    }

    public static void checkStreamMode( KieBaseModelImpl kBaseModel, KieBaseConfiguration conf, Collection<? extends KiePackage> pkgs ) {
        if ( kBaseModel.getEventProcessingMode() == EventProcessingOption.CLOUD &&
             (conf == null || conf.getOption(EventProcessingOption.class) == EventProcessingOption.CLOUD ) ) {
            for (KiePackage kpkg : pkgs) {
                if ( ((KnowledgePackageImpl ) kpkg).needsStreamMode() ) {
                    throw new RuntimeException( "The requested KieBase \"" + kBaseModel.getName() + "\" has been set to run in CLOUD mode but requires features only available in STREAM mode" );
                }
            }
        }
    }

    private KieBaseConfiguration getKnowledgeBaseConfiguration(KieBaseModelImpl kBaseModel, ClassLoader cl) {
        KieBaseConfiguration kbConf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration(null, cl);
        kbConf.setOption(kBaseModel.getEqualsBehavior());
        kbConf.setOption(kBaseModel.getEventProcessingMode());
        kbConf.setOption(kBaseModel.getDeclarativeAgenda());
        kbConf.setOption(kBaseModel.getSequential());
        kbConf.setOption(kBaseModel.getSessionsPool());
        kbConf.setOption(kBaseModel.getAlphaNodeOrdering());
        return kbConf;
    }

    public KnowledgeBuilderConfiguration getBuilderConfiguration(KieBaseModel kBaseModel, ClassLoader classLoader) {
        KnowledgeBuilderConfigurationImpl pconf = new KnowledgeBuilderConfigurationImpl(classLoader);
        setModelPropsOnConf( (KieBaseModelImpl) kBaseModel, pconf );
        return pconf;
    }

    static void setModelPropsOnConf( KieBaseModelImpl kBaseModel, KnowledgeBuilderConfigurationImpl pconf ) {
        KieModuleModel kModuleModel = kBaseModel.getKModule();
        for (Map.Entry<String, String> entry : kModuleModel.getConfigurationProperties().entrySet()) {
            pconf.setProperty(entry.getKey(), entry.getValue());
        }
    }

    public final boolean addResourceToCompiler(CompositeKnowledgeBuilder ckbuilder, KieBaseModel kieBaseModel, String fileName) {
        return addResourceToCompiler(ckbuilder, kieBaseModel, fileName, null);
    }

    public final boolean addResourceToCompiler(CompositeKnowledgeBuilder ckbuilder, KieBaseModel kieBaseModel, String fileName, ResourceChangeSet rcs) {
        Resource resource = getResource(fileName);
        if (resource != null) {
            ResourceConfiguration conf = getResourceConfiguration(fileName);
            ResourceType resourceType = conf instanceof ResourceConfigurationImpl && ((ResourceConfigurationImpl)conf).getResourceType() != null ?
                                        ((ResourceConfigurationImpl)conf).getResourceType() :
                                        ResourceType.determineResourceType(fileName);

            if (resourceType == ResourceType.DTABLE && conf instanceof DecisionTableConfiguration) {
                addDTableToCompiler( ckbuilder, kieBaseModel, fileName, resource, rcs, ( DecisionTableConfiguration ) conf );
            } else {
                ckbuilder.add(resource, resourceType, conf, rcs);
            }
            return true;
        }
        return false;
    }

    private void addDTableToCompiler( CompositeKnowledgeBuilder ckbuilder, KieBaseModel kieBaseModel, String fileName, Resource resource, ResourceChangeSet rcs, DecisionTableConfiguration dtableConf ) {
        for (RuleTemplateModel template : kieBaseModel.getRuleTemplates()) {
            if (template.getDtable().equals( fileName )) {
                Resource templateResource = getResource( template.getTemplate() );
                if ( templateResource != null ) {
                    dtableConf.addRuleTemplateConfiguration( templateResource, template.getRow(), template.getCol() );
                } else {
                    throw new RuntimeException( "Cannot find resource: '" + template.getTemplate() + "'" );
                }
            }
        }
        addDTableToCompiler( ckbuilder, resource, dtableConf, rcs );
    }

    public static void addDTableToCompiler( CompositeKnowledgeBuilder ckbuilder, Resource resource, DecisionTableConfiguration dtableConf ) {
        addDTableToCompiler( ckbuilder, resource, dtableConf, null );
    }

    private static void addDTableToCompiler( CompositeKnowledgeBuilder ckbuilder, Resource resource, DecisionTableConfiguration dtableConf, ResourceChangeSet rcs ) {
        String sheetNames = dtableConf.getWorksheetName();
        if (sheetNames == null || sheetNames.indexOf( ',' ) < 0) {
            ckbuilder.add( resource, ResourceType.DTABLE, dtableConf, rcs );
        } else {
            for (String sheetName : sheetNames.split( "\\," ) ) {
                ckbuilder.add( resource, ResourceType.DTABLE, new DecisionTableConfigurationDelegate( dtableConf, sheetName), rcs );
            }
        }
    }

    static class DecisionTableConfigurationDelegate implements DecisionTableConfiguration {

        private final DecisionTableConfiguration delegate;
        private final String sheetName;

        DecisionTableConfigurationDelegate( DecisionTableConfiguration delegate, String sheetName ) {
            this.delegate = delegate;
            this.sheetName = sheetName;
        }

        @Override
        public void setInputType( DecisionTableInputType inputType ) {
            delegate.setInputType( inputType );

        }

        @Override
        public DecisionTableInputType getInputType() {
            return delegate.getInputType();
        }

        @Override
        public void setWorksheetName( String name ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getWorksheetName() {
            return sheetName;
        }

        @Override
        public void addRuleTemplateConfiguration( Resource template, int row, int col ) {
            delegate.addRuleTemplateConfiguration( template, row, col );
        }

        @Override
        public List<RuleTemplateConfiguration> getRuleTemplateConfigurations() {
            return delegate.getRuleTemplateConfigurations();
        }

        @Override
        public boolean isTrimCell() {
            return delegate.isTrimCell();
        }

        @Override
        public void setTrimCell( boolean trimCell ) {
            delegate.setTrimCell( trimCell );
        }

        @Override
        public Properties toProperties() {
            return delegate.toProperties();
        }

        @Override
        public ResourceConfiguration fromProperties( Properties prop ) {
            return delegate.fromProperties( prop );
        }
    }

    public boolean hasResource(String fileName) {
        byte[] bytes = getBytes(fileName);
        return bytes != null && bytes.length > 0;
    }

    public ResourceConfiguration getResourceConfiguration(String fileName) {
        return resourceConfigurationCache.computeIfAbsent(fileName, this::loadResourceConfiguration);
    }

    private ResourceConfiguration loadResourceConfiguration( String fileName ) {
        return loadResourceConfiguration( fileName, this::isAvailable, file -> new ByteArrayInputStream(getBytes( fileName + ".properties")) );
    }

    public static ResourceConfiguration loadResourceConfiguration( String fileName, Predicate<String> fileAvailable, Function<String, InputStream> fileProvider ) {
        ResourceConfiguration conf;
        Properties prop = new Properties();
        if ( fileAvailable.test( fileName + ".properties") ) {
            try ( InputStream input = fileProvider.apply( fileName + ".properties") ) {
                prop.load(input);
            } catch (IOException e) {
                log.error(String.format("Error loading resource configuration from file: %s.properties", fileName ));
            }
        }
        if (ResourceType.DTABLE.matchesExtension( fileName )) {
            int lastDot = fileName.lastIndexOf( '.' );
            if (lastDot >= 0 && fileName.length() > lastDot+1) {
                String extension = fileName.substring( lastDot+1 );
                Object confClass = prop.get(ResourceTypeImpl.KIE_RESOURCE_CONF_CLASS);
                if (confClass == null || confClass.toString().equals( ResourceConfigurationImpl.class.getCanonicalName() )) {
                    prop.setProperty( ResourceTypeImpl.KIE_RESOURCE_CONF_CLASS, DecisionTableConfigurationImpl.class.getName() );
                }
                prop.setProperty(DecisionTableConfigurationImpl.DROOLS_DT_TYPE, DecisionTableInputType.valueOf( extension.toUpperCase() ).toString());
            }
        }
        conf = prop.isEmpty() ? null : ResourceTypeImpl.fromProperties(prop);
        if (conf instanceof DecisionTableConfiguration && (( DecisionTableConfiguration ) conf).getWorksheetName() == null) {
            (( DecisionTableConfiguration ) conf).setWorksheetName( prop.getProperty( "sheets" ) );
        }
        return conf;
    }

    @Override
    public CompilationCache getCompilationCache(String kbaseName) {
        // Map< DIALECT, Map< RESOURCE, List<BYTECODE> > >
        CompilationCache cache = compilationCache.get(kbaseName);
        if (cache == null) {
            byte[] fileContents = getBytes(KieBuilderImpl.getCompilationCachePath(releaseId, kbaseName));
            if (fileContents != null) {
                ExtensionRegistry registry = KieModuleCacheHelper.buildRegistry();
                try {
                    Header header = KieModuleCacheHelper.readFromStreamWithHeaderPreloaded(new ByteArrayInputStream(fileContents), registry);

                    if (!Drools.isCompatible(header.getVersion().getVersionMajor(),
                                             header.getVersion().getVersionMinor(),
                                             header.getVersion().getVersionRevision())) {
                        // if cache has been built with an incompatible version avoid to use it
                        log.warn("The compilation cache has been built with an incompatible version. " +
                                 "You should recompile your project in order to use it with current release.");
                        return null;
                    }

                    KModuleCache kModuleCache = KModuleCache.parseFrom(header.getPayload());

                    cache = new CompilationCache();
                    for (CompilationData _data : kModuleCache.getCompilationDataList()) {
                        for (CompDataEntry _entry : _data.getEntryList()) {
                            cache.addEntry(_data.getDialect(), _entry.getId(),  _entry.getData().toByteArray());
                        }
                    }
                    compilationCache.put(kbaseName, cache);
                } catch (Exception e) {
                    log.error("Unable to load compilation cache... ", e);
                }
            }
        }
        return cache;
    }

    public PomModel getPomModel() {
        if (pomModel == null) {
            try {
                byte[] pomXml = getPomXml();
                if( pomXml != null ) {
                    PomModel tempPomModel = PomModel.Parser.parse("pom.xml", new ByteArrayInputStream(pomXml));
                    validatePomModel(tempPomModel); // throws an exception if invalid
                    pomModel = tempPomModel;
                }
            } catch( Exception e ) {
                // nothing to do as it was not possible to retrieve pom.xml
            }
        }
        return pomModel;
    }

    public void setPomModel(PomModel pomModel) {
        this.pomModel = pomModel;
    }

    private void validatePomModel(PomModel pomModel) {
        org.appformer.maven.support.AFReleaseId pomReleaseId = pomModel.getReleaseId();
        if (StringUtils.isEmpty(pomReleaseId.getGroupId()) || StringUtils.isEmpty(pomReleaseId.getArtifactId()) || StringUtils.isEmpty(pomReleaseId.getVersion())) {
            throw new RuntimeException("Maven pom.properties exists but ReleaseId content is malformed");
        }
    }

    private byte[] getPomXml() {
        return getBytes(((ReleaseIdImpl)releaseId).getPomXmlPath());
    }

    public InputStream getPomAsStream() {
        byte[] pom = getBytes(((ReleaseIdImpl)releaseId).getPomXmlPath());
        return pom != null ? new ByteArrayInputStream(pom) : null;
    }

    public static boolean updateResource(CompositeKnowledgeBuilder ckbuilder,
                                         InternalKieModule kieModule,
                                         String resourceName,
                                         ResourceChangeSet changes) {
        ResourceConfiguration conf = kieModule.getResourceConfiguration(resourceName);
        Resource resource = kieModule.getResource(resourceName);
        if (resource != null) {
            if (conf == null) {
                ckbuilder.add(resource,
                        ResourceType.determineResourceType(resourceName),
                        changes );
            } else {
                ckbuilder.add(resource,
                        ResourceType.determineResourceType(resourceName),
                        conf,
                        changes );
            }
            return true;
        }
        return false;
    }
    
    @Override
    public ResourceProvider createResourceProvider() {
        try {
            return new KieModuleResourceProvider(this, getFile().toURI().toURL());
        } catch (Exception e) {
            return null;
        }
    }

    private static class KieModuleResourceProvider implements ResourceProvider {

        private final InternalKieModule kieModule;
        private final URL kieModuleUrl;

        private KieModuleResourceProvider(InternalKieModule kieModule, URL kieModuleUrl) {
            this.kieModule = kieModule;
            this.kieModuleUrl = kieModuleUrl;
        }

        @Override
        public InputStream getResourceAsStream(String name) throws IOException {
            if (name.endsWith( "/" )) {
                name = name.substring( 0, name.length()-1 );
            }
            Resource resource = kieModule.getResource(name);
            return resource != null ? resource.getInputStream() : null;
        }

        @Override
        public URL getResource(String name) {
            if (name.endsWith( "/" )) {
                name = name.substring( 0, name.length()-1 );
            }
            return kieModule.hasResource(name) ? createURLForResource(name) : null;
        }

        private URL createURLForResource(String name) {
            try {
                if (kieModule instanceof ZipKieModule) {
                    return new URL("jar", "", kieModuleUrl + "!/" + name);
                } else {
                    return new URL(kieModuleUrl, name);
                }
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }
}
