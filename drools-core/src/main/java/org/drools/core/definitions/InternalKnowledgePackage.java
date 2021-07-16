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

package org.drools.core.definitions;

import java.io.Externalizable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.core.base.ClassFieldAccessorCache;
import org.drools.core.base.ClassFieldAccessorStore;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.factmodel.traits.TraitRegistry;
import org.drools.core.facttemplates.FactTemplate;
import org.drools.core.rule.DialectRuntimeRegistry;
import org.drools.core.rule.Function;
import org.drools.core.rule.ImportDeclaration;
import org.drools.core.rule.TypeDeclaration;
import org.drools.core.rule.WindowDeclaration;
import org.drools.core.ruleunit.RuleUnitDescriptionLoader;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.process.Process;
import org.kie.api.definition.type.FactType;
import org.kie.api.io.Resource;
import org.kie.api.runtime.rule.AccumulateFunction;
import org.drools.core.addon.TypeResolver;

public interface InternalKnowledgePackage extends KiePackage,
                                                  Externalizable {

    void clear();

    void checkValidity();

    boolean isValid();

    void setNeedStreamMode();

    void resetErrors();

    void setError(String summary);

    ResourceTypePackageRegistry getResourceTypePackages();

    Map<String, Class<?>> getGlobals();

    @Deprecated
    Map<String, Process> getRuleFlows();

    Map<String, TypeDeclaration> getTypeDeclarations();

    Map<String, Function> getFunctions();

    Map<String, ImportDeclaration> getImports();

    Map<String, WindowDeclaration> getWindowDeclarations();

    Map<String, AccumulateFunction> getAccumulateFunctions();

    Set<String> getEntryPointIds();

    Set<String> getStaticImports();

    void addFunction(Function function);

    void addGlobal(String identifier, Class<?> clazz);

    void addEntryPointId(String id);

    void addWindowDeclaration(WindowDeclaration window);

    void addRule(RuleImpl rule);

    @Deprecated
    void addProcess(Process process);

    void addTypeDeclaration(TypeDeclaration typeDecl);

    void addFactTemplate(FactTemplate factTemplate);

    void addImport(ImportDeclaration importDecl);

    void addAccumulateFunction(String name, AccumulateFunction function);

    void addStaticImport(String functionImport);

    void removeFunction(String functionName);

    @Deprecated
    void removeRuleFlow(String id);

    void removeRule(RuleImpl rule);

    void removeGlobal(String identifier);

    void removeTypeDeclaration(String type);

    boolean removeObjectsGeneratedFromResource(Resource resource);

    List<TypeDeclaration> removeTypesGeneratedFromResource(Resource resource);

    List<RuleImpl> getRulesGeneratedFromResource(Resource resource);
    List<RuleImpl> removeRulesGeneratedFromResource(Resource resource);

    List<Function> removeFunctionsGeneratedFromResource(Resource resource);

    @Deprecated
    List<Process> removeProcessesGeneratedFromResource(Resource resource);

    boolean removeFromResourceTypePackageGeneratedFromResource(Resource resource);

    DialectRuntimeRegistry getDialectRuntimeRegistry();

    void setDialectRuntimeRegistry(DialectRuntimeRegistry dialectRuntimeRegistry);

    RuleImpl getRule(String name);

    FactType getFactType(String typeName);

    TypeDeclaration getTypeDeclaration(Class<?> clazz);

    TypeDeclaration getTypeDeclaration(String type);

    FactTemplate getFactTemplate(String name);

    ClassLoader getPackageClassLoader();

    TypeResolver getTypeResolver();

    void setClassLoader(ClassLoader classLoader);

    RuleUnitDescriptionLoader getRuleUnitDescriptionLoader();

    ClassFieldAccessorStore getClassFieldAccessorStore();

    void setClassFieldAccessorCache(ClassFieldAccessorCache classFieldAccessorCache);

    InternalKnowledgePackage deepCloneIfAlreadyInUse(ClassLoader classLoader);

    boolean hasTraitRegistry();

    TraitRegistry getTraitRegistry();

    void addCloningResource(String key, Object resource);

    void wireTypeDeclarations();
}
