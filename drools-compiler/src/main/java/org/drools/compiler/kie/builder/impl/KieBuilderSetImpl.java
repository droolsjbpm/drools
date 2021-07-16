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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.drools.compiler.builder.InternalKnowledgeBuilder;
import org.drools.compiler.builder.impl.KnowledgeBuilderImpl;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.io.impl.BaseResource;
import org.drools.reflective.classloader.ProjectClassLoader;
import org.kie.api.KieServices;
import org.kie.api.builder.Message;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.definition.KiePackage;
import org.kie.api.io.Resource;
import org.kie.internal.builder.CompositeKnowledgeBuilder;
import org.kie.internal.builder.IncrementalResults;
import org.kie.internal.builder.KieBuilderSet;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderResult;
import org.kie.internal.builder.ResultSeverity;

import static java.util.Arrays.asList;

import static org.drools.compiler.kie.builder.impl.KieBuilderImpl.filterFileInKBase;

public class KieBuilderSetImpl implements KieBuilderSet {

    private final KieBuilderImpl kieBuilder;
    private final Message.Level minimalLevel;
    private String[] files;

    private Map<String, Collection<KnowledgeBuilderResult>> previousErrors;

    private final Map<String, Set<String>> resourcesWithErrors = new HashMap<>();

    public KieBuilderSetImpl(KieBuilderImpl kieBuilder) {
        this(kieBuilder, Message.Level.ERROR);
        registerInitialErrors(kieBuilder);
    }

    public KieBuilderSetImpl(KieBuilderImpl kieBuilder, Message.Level minimalLevel) {
        this.kieBuilder = kieBuilder;
        this.minimalLevel = minimalLevel;
        registerInitialErrors(kieBuilder);
    }

    private void registerInitialErrors(KieBuilderImpl kieBuilder) {
        previousErrors = new HashMap<>();
        InternalKieModule kieModule = (InternalKieModule) kieBuilder.getKieModuleIgnoringErrors();
        for (KieBaseModel kBaseModel : kieModule.getKieModuleModel().getKieBaseModels().values()) {
            KnowledgeBuilder kBuilder = kieModule.getKnowledgeBuilderForKieBase( kBaseModel.getName() );
            if (kBuilder != null) {
                previousErrors.put( kBaseModel.getName(), kBuilder.getResults( getSeverities() ) );
                resourcesWithErrors.put(kBaseModel.getName(), findResourcesWithMessages(kBuilder));
            }
        }
    }

    public Message.Level getMinimalLevel() {
        return minimalLevel;
    }

    private ResultSeverity[] getSeverities() {
        switch (minimalLevel) {
            case ERROR: return new ResultSeverity[] { ResultSeverity.ERROR };
            case WARNING: return new ResultSeverity[] { ResultSeverity.ERROR, ResultSeverity.WARNING };
            case INFO: return new ResultSeverity[] { ResultSeverity.ERROR, ResultSeverity.WARNING, ResultSeverity.INFO };
            default: throw new UnsupportedOperationException( "Unknow message level:  " + minimalLevel );
        }
    }

    public KieBuilderSetImpl setFiles( String[] files) {
        this.files = files;
        return this;
    }

    @Override
    public IncrementalResults build() {
        Collection<String> srcFiles = files != null ? asList(files) : kieBuilder.getModifiedResourcesSinceLastMark();
        Collection<String> filesToBuild = new ArrayList<>();
        if ( srcFiles.isEmpty() ) {
            return new IncrementalResultsImpl();
        }
        kieBuilder.cloneKieModuleForIncrementalCompilation();
        for (String file : srcFiles) {
            if ( !file.endsWith( ".properties" ) ) {
                String trgFile = kieBuilder.copySourceToTarget(file);
                if (trgFile != null) {
                    filesToBuild.add(trgFile);
                }
            }
        }
        IncrementalResults result = buildChanges(filesToBuild);
        files = null;
        kieBuilder.markSource();
        return result;
    }

    private Set<String> findResourcesWithMessages( KnowledgeBuilder kBuilder) {
        if ( kBuilder.hasResults( getSeverities() ) ) {
            Set<String> resourcesWithMessages = new HashSet<String>();
            for ( KnowledgeBuilderResult result : kBuilder.getResults( getSeverities() ) ) {
                resourcesWithMessages.add(result.getResource().getSourcePath());
            }
            return resourcesWithMessages;
        }
        return Collections.emptySet();
    }

    private IncrementalResults buildChanges(Collection<String> filesToBuild) {
        Map<String, Collection<KnowledgeBuilderResult>> currentResults = new HashMap<>();

        InternalKieModule kieModule = (InternalKieModule) kieBuilder.getKieModuleIgnoringErrors();
        for (KieBaseModel kBaseModel : kieModule.getKieModuleModel().getKieBaseModels().values()) {
            InternalKnowledgeBuilder kBuilder = (InternalKnowledgeBuilder)kieModule.getKnowledgeBuilderForKieBase( kBaseModel.getName() );
            if (kBuilder == null) {
                continue;
            }
            CompositeKnowledgeBuilder ckbuilder = kBuilder.batch();
            boolean useFolders = kBuilder.getBuilderConfiguration().isGroupDRLsInKieBasesByFolder();

            KnowledgeBuilderImpl.ResourceRemovalResult removalResult = new KnowledgeBuilderImpl.ResourceRemovalResult();

            Set<String> wrongResources = resourcesWithErrors.get(kBaseModel.getName());
            for ( String resourceName : wrongResources ) {
                removalResult.add( kBuilder.removeObjectsGeneratedFromResource(new DummyResource(resourceName)) );
                removalResult.mergeModified( addResource(ckbuilder, kBaseModel, kieModule, resourceName, useFolders) );
            }

            for (String file : filesToBuild) {
                if ( wrongResources.contains(file) ) {
                    removalResult.mergeModified( true );
                } else {
                    // remove the objects generated by the old Resource
                    removalResult.add( kBuilder.removeObjectsGeneratedFromResource(new DummyResource(file)) );
                    // add the modified Resource
                    removalResult.mergeModified( addResource(ckbuilder, kBaseModel, kieModule, file, useFolders) );
                }
            }

            if (removalResult.isModified()) {
                boolean typeRefreshed = !removalResult.getRemovedTypes().isEmpty();
                if (typeRefreshed) {
                    ProjectClassLoader projectClassLoader = (ProjectClassLoader) kBuilder.getRootClassLoader();
                    projectClassLoader.reinitTypes();
                    for (String removedType : removalResult.getRemovedTypes()) {
                        projectClassLoader.undefineClass(removedType);
                    }
                }

                ckbuilder.build();

                if (typeRefreshed) {
                    Collection<KiePackage> kiePackages = kBuilder.getKnowledgePackages();
                    for (KiePackage kiePackage : kiePackages) {
                        ((InternalKnowledgePackage) kiePackage).getClassFieldAccessorStore().wire();
                        ((InternalKnowledgePackage) kiePackage).wireTypeDeclarations();
                    }
                }

                resourcesWithErrors.put(kBaseModel.getName(), findResourcesWithMessages(kBuilder));
                if ( kBuilder.hasResults( getSeverities() ) ) {
                    currentResults.put( kBaseModel.getName(), kBuilder.getResults( getSeverities() ) );
                }

                if ( kBuilder.hasErrors()) {
                    kBuilder.undo();
                } else {
                    KieServices.Factory.get().getRepository().addKieModule( kieModule );
                    kieBuilder.updateKieModuleMetaInfo();
                }
            }
        }

        IncrementalResultsImpl results = getIncrementalResults(currentResults);
        previousErrors = currentResults;
        return results;
    }

    private IncrementalResultsImpl getIncrementalResults(Map<String, Collection<KnowledgeBuilderResult>> currentResults) {
        IncrementalResultsImpl results = new IncrementalResultsImpl();
        for (Map.Entry<String, Collection<KnowledgeBuilderResult>> entry : currentResults.entrySet()) {
            Collection<KnowledgeBuilderResult> previousErrorsInKB = previousErrors.remove(entry.getKey());
            for ( KnowledgeBuilderResult error : entry.getValue() ) {
                if ( previousErrorsInKB == null || !previousErrorsInKB.remove( error ) ) {
                    results.addMessage( error, entry.getKey() );
                }
            }
            if (previousErrorsInKB != null) {
                for ( KnowledgeBuilderResult error : previousErrorsInKB ) {
                    results.removeMessage( error, entry.getKey() );
                }
            }
        }
        for (Map.Entry<String, Collection<KnowledgeBuilderResult>> entry : previousErrors.entrySet()) {
            for ( KnowledgeBuilderResult error : entry.getValue() ) {
                results.removeMessage( error, entry.getKey() );
            }
        }
        return results;
    }

    private boolean addResource( CompositeKnowledgeBuilder ckbuilder,
                                 KieBaseModel kieBaseModel,
                                 InternalKieModule kieModule,
                                 String resourceName,
                                 boolean useFolders ) {
        return !resourceName.endsWith(".properties") &&
               filterFileInKBase(kieModule, kieBaseModel, resourceName, () -> kieModule.getResource( resourceName ), useFolders) &&
               kieModule.addResourceToCompiler(ckbuilder, kieBaseModel, resourceName);
    }

    public static class DummyResource extends BaseResource {
        public DummyResource(String resourceName) {
            setSourcePath(decode(resourceName));
        }

        public DummyResource() {
        }

        @Override
        public URL getURL() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasURL() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDirectory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<Resource> listResources() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getEncoding() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Reader getReader() throws IOException {
            throw new UnsupportedOperationException();
        }

        private String decode(final String resourceName) {
            try {
                return URLDecoder.decode(resourceName, "UTF-8");
            } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                return resourceName;
            }
        }
    }
}
