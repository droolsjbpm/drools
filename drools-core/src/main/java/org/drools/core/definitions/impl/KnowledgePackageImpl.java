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

package org.drools.core.definitions.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.drools.core.addon.ClassTypeResolver;
import org.drools.core.addon.TypeResolver;
import org.drools.core.base.ClassFieldAccessorCache;
import org.drools.core.base.ClassFieldAccessorStore;
import org.drools.core.common.DroolsObjectInputStream;
import org.drools.core.common.DroolsObjectOutputStream;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.ProcessPackage;
import org.drools.core.definitions.ResourceTypePackageRegistry;
import org.drools.core.definitions.rule.impl.GlobalImpl;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.factmodel.traits.TraitRegistry;
import org.drools.core.facttemplates.FactTemplate;
import org.drools.core.rule.DialectRuntimeRegistry;
import org.drools.core.rule.Function;
import org.drools.core.rule.ImportDeclaration;
import org.drools.core.rule.InvalidRulePackage;
import org.drools.core.rule.JavaDialectRuntimeData;
import org.drools.core.rule.TypeDeclaration;
import org.drools.core.rule.WindowDeclaration;
import org.drools.core.ruleunit.RuleUnitDescriptionLoader;
import org.drools.core.util.ClassUtils;
import org.drools.reflective.classloader.ProjectClassLoader;
import org.kie.api.definition.process.Process;
import org.kie.api.definition.rule.Global;
import org.kie.api.definition.rule.Query;
import org.kie.api.definition.rule.Rule;
import org.kie.api.definition.type.FactType;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.rule.AccumulateFunction;

public class KnowledgePackageImpl
        implements
        InternalKnowledgePackage,
        Externalizable {

    private static final long serialVersionUID = 510l;

    private static final String[] implicitImports = new String[]{
            "org.kie.api.definition.rule.*",
            "org.kie.api.definition.type.*",
            "org.drools.core.beliefsystem.abductive.Abductive",
            "org.drools.core.beliefsystem.abductive.Abducible"};

    /**
     * Name of the pkg.
     */
    protected String name;

    /**
     * Set of all rule-names in this <code>Package</code>.
     */
    protected Map<String, RuleImpl> rules = new LinkedHashMap<>();

    protected Map<String, ImportDeclaration> imports = new HashMap<>();

    protected Map<String, Function> functions;

    protected Map<String, AccumulateFunction> accumulateFunctions;

    protected Set<String> staticImports;

    protected Map<String, Class<?>> globals;

    protected Map<String, FactTemplate> factTemplates;

    protected DialectRuntimeRegistry dialectRuntimeRegistry;

    protected Map<String, TypeDeclaration> typeDeclarations = new ConcurrentHashMap<>();

    protected Set<String> entryPointsIds;

    protected Map<String, WindowDeclaration> windowDeclarations;

    protected ClassFieldAccessorStore classFieldAccessorStore;

    protected ResourceTypePackageRegistry resourceTypePackages;

    protected Map<String, Object> cloningResources = new HashMap<>();

    /**
     * This is to indicate the the package has no errors during the
     * compilation/building phase
     */
    protected boolean valid = true;

    protected boolean needStreamMode = false;

    /**
     * This will keep a summary error message as to why this package is not
     * valid
     */
    private String errorSummary;

    private transient TypeResolver typeResolver;

    private transient RuleUnitDescriptionLoader ruleUnitDescriptionLoader;

    private transient AtomicBoolean inUse = new AtomicBoolean(false);

    public KnowledgePackageImpl() {
        this(null);
    }

    /**
     * Construct.
     *
     * @param name The name of this <code>Package</code>.
     */
    public KnowledgePackageImpl(final String name) {
        this.name = name;
        this.accumulateFunctions = Collections.emptyMap();
        this.staticImports = Collections.emptySet();
        this.globals = Collections.emptyMap();
        this.factTemplates = Collections.emptyMap();
        this.functions = Collections.emptyMap();
        this.dialectRuntimeRegistry = new DialectRuntimeRegistry();
        this.classFieldAccessorStore = new ClassFieldAccessorStore();
        this.entryPointsIds = Collections.emptySet();
        this.windowDeclarations = Collections.emptyMap();
        this.resourceTypePackages = new ResourceTypePackageRegistry();
    }

    public ResourceTypePackageRegistry getResourceTypePackages() {
        return resourceTypePackages;
    }

    public Collection<Rule> getRules() {
        return Collections.unmodifiableCollection(rules.values());
    }

    public Function getFunction(String name) {
        return functions.getOrDefault(name, null);
    }

    public Collection<Process> getProcesses() {
        if (getRuleFlows().isEmpty()) {
            return Collections.emptyList();
        }
        Collection<org.kie.api.definition.process.Process> processes = getRuleFlows().values();
        List<Process> list = new ArrayList<>(processes.size());
        for (org.kie.api.definition.process.Process process : processes) {
            list.add(process);
        }
        return Collections.unmodifiableCollection(list);
    }

    public Collection<FactType> getFactTypes() {
        if (typeDeclarations.isEmpty()) {
            return Collections.emptyList();
        }
        List<FactType> list = new ArrayList<>();
        for (TypeDeclaration typeDeclaration : typeDeclarations.values()) {
            list.add(typeDeclaration.getTypeClassDef());
        }
        return Collections.unmodifiableCollection(list);
    }

    public Map<String, FactType> getFactTypesMap() {
        Map<String, FactType> types = new HashMap<>();
        for (Map.Entry<String, TypeDeclaration> entry : typeDeclarations.entrySet()) {
            types.put(entry.getKey(), entry.getValue().getTypeClassDef());
        }
        return types;
    }

    public Collection<Query> getQueries() {
        List<Query> list = new ArrayList<>(rules.size());
        for (RuleImpl rule : rules.values()) {
            if (rule.isQuery()) {
                list.add(rule);
            }
        }
        return Collections.unmodifiableCollection(list);
    }

    public Collection<String> getFunctionNames() {
        return Collections.unmodifiableCollection(functions.keySet());
    }

    public Collection<Global> getGlobalVariables() {
        List<Global> list = new ArrayList<>(getGlobals().size());
        for (Map.Entry<String, Class<?>> global : getGlobals().entrySet()) {
            list.add(new GlobalImpl(global.getKey(), global.getValue().getName()));
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * Handles the write serialization of the Package. Patterns in Rules may
     * reference generated data which cannot be serialized by default methods.
     * The Package uses PackageCompilationData to hold a reference to the
     * generated bytecode. The generated bytecode must be restored before any
     * Rules.
     *
     * @param stream out the stream to write the object to; should be an instance
     *               of DroolsObjectOutputStream or OutputStream
     */
    public void writeExternal(ObjectOutput stream) throws IOException {
        ByteArrayOutputStream bytes = null;
        ObjectOutput out;

        if (stream instanceof DroolsObjectOutputStream) {
            out = stream;
        } else {
            bytes = new ByteArrayOutputStream();
            out = new DroolsObjectOutputStream(bytes);
        }

        try {
            out.writeObject(this.name);
            out.writeObject(this.classFieldAccessorStore);
            out.writeObject(this.dialectRuntimeRegistry);
            out.writeObject(this.typeDeclarations);
            out.writeObject(this.imports);
            out.writeObject(this.staticImports);
            out.writeObject(this.functions);
            out.writeObject(this.accumulateFunctions);
            out.writeObject(this.factTemplates);
            out.writeObject(this.globals);
            out.writeBoolean(this.valid);
            out.writeBoolean(this.needStreamMode);
            out.writeObject(this.rules);
            out.writeObject(this.entryPointsIds);
            out.writeObject(this.windowDeclarations);
            out.writeObject(this.resourceTypePackages);
        } finally {
            // writing the whole stream as a byte array
            if (bytes != null) {
                bytes.flush();
                bytes.close();
                stream.writeObject(bytes.toByteArray());
            }
        }
    }

    /**
     * Handles the read serialization of the Package. Patterns in Rules may
     * reference generated data which cannot be serialized by default methods.
     * The Package uses PackageCompilationData to hold a reference to the
     * generated bytecode; which must be restored before any Rules. A custom
     * ObjectInputStream, able to resolve classes against the bytecode in the
     * PackageCompilationData, is used to restore the Rules.
     *
     * @param stream, the stream to read data from in order to restore the object;
     *                should be an instance of DroolsObjectInputStream or
     *                InputStream
     */
    public void readExternal(ObjectInput stream) throws IOException,
            ClassNotFoundException {
        boolean isDroolsStream = stream instanceof DroolsObjectInputStream;
        DroolsObjectInputStream in = isDroolsStream ? (DroolsObjectInputStream) stream
                : new DroolsObjectInputStream(
                new ByteArrayInputStream(
                        (byte[]) stream.readObject()));

        this.name = (String) in.readObject();
        this.classFieldAccessorStore = (ClassFieldAccessorStore) in.readObject();
        in.setStore(this.classFieldAccessorStore);

        this.dialectRuntimeRegistry = (DialectRuntimeRegistry) in.readObject();
        this.typeDeclarations = (Map) in.readObject();
        this.imports = (Map<String, ImportDeclaration>) in.readObject();
        this.staticImports = (Set) in.readObject();
        this.functions = (Map<String, Function>) in.readObject();
        this.accumulateFunctions = (Map<String, AccumulateFunction>) in.readObject();
        this.factTemplates = (Map) in.readObject();
        this.globals = (Map<String, Class<?>>) in.readObject();
        this.valid = in.readBoolean();
        this.needStreamMode = in.readBoolean();
        this.rules = (Map<String, RuleImpl>) in.readObject();
        this.entryPointsIds = (Set<String>) in.readObject();
        this.windowDeclarations = (Map<String, WindowDeclaration>) in.readObject();
        this.resourceTypePackages = (ResourceTypePackageRegistry) in.readObject();

        in.setStore(null);

        if (!isDroolsStream) {
            in.close();
        }
    }

    // ------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------

    /**
     * Retrieve the name of this <code>Package</code>.
     *
     * @return The name of this <code>Package</code>.
     */
    public String getName() {
        return this.name;
    }

    public ClassLoader getPackageClassLoader() {
        JavaDialectRuntimeData javaRuntime = (JavaDialectRuntimeData) getDialectRuntimeRegistry().getDialectData("java");
        return javaRuntime.getClassLoader();
    }

    public DialectRuntimeRegistry getDialectRuntimeRegistry() {
        return this.dialectRuntimeRegistry;
    }

    public void setDialectRuntimeRegistry(DialectRuntimeRegistry dialectRuntimeRegistry) {
        this.dialectRuntimeRegistry = dialectRuntimeRegistry;
    }

    public void addImport(final ImportDeclaration importDecl) {
        this.imports.put(importDecl.getTarget(), importDecl);
        if (this.typeResolver != null) {
            this.typeResolver.addImport( importDecl.getTarget() );
        }
    }

    public Map<String, ImportDeclaration> getImports() {
        return this.imports;
    }

    public void addTypeDeclaration(final TypeDeclaration typeDecl) {
        this.typeDeclarations.put(typeDecl.getTypeName(),
                                  typeDecl);
    }

    public void removeTypeDeclaration(final String type) {
        this.typeDeclarations.remove(type);
    }

    public Map<String, TypeDeclaration> getTypeDeclarations() {
        return this.typeDeclarations;
    }

    public TypeDeclaration getTypeDeclaration(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        TypeDeclaration typeDeclaration = getTypeDeclaration(ClassUtils.getSimpleName(clazz));
        if (typeDeclaration == null) {
            // check if clazz is resolved by any of the type declarations
            for (TypeDeclaration type : this.typeDeclarations.values()) {
                if (type.isValid() && type.matches(clazz)) {
                    typeDeclaration = type;
                    break;
                }
            }
        }
        return typeDeclaration;
    }

    public TypeDeclaration getTypeDeclaration(String type) {
        return this.typeDeclarations.get(type);
    }

    public void addStaticImport(final String functionImport) {
        if (this.staticImports == Collections.EMPTY_SET) {
            this.staticImports = new HashSet<>(2);
        }
        this.staticImports.add(functionImport);
    }

    public void addFunction(final Function function) {
        if (this.functions == Collections.EMPTY_MAP) {
            this.functions = new HashMap<>(1);
        }

        this.functions.put(function.getName(),
                           function);
        dialectRuntimeRegistry.getDialectData(function.getDialect()).setDirty(true);
    }

    public Map<String, Function> getFunctions() {
        return this.functions;
    }

    public void addAccumulateFunction(final String name, final AccumulateFunction function) {
        if (this.accumulateFunctions == Collections.EMPTY_MAP) {
            this.accumulateFunctions = new HashMap<>(1);
        }

        this.accumulateFunctions.put(name,
                                     function);
    }

    public Map<String, AccumulateFunction> getAccumulateFunctions() {
        return this.accumulateFunctions;
    }

    public void removeFunctionImport(final String functionImport) {
        this.staticImports.remove(functionImport);
    }

    public Set<String> getStaticImports() {
        return this.staticImports;
    }

    public void addGlobal(final String identifier,
                          final Class<?> clazz) {
        if (this.globals == Collections.EMPTY_MAP) {
            this.globals = new HashMap<>(1);
        }
        this.globals.put(identifier, clazz);
    }

    public void removeGlobal(final String identifier) {
        this.globals.remove(identifier);
    }

    public Map<String, Class<?>> getGlobals() {
        return this.globals;
    }

    public void removeFunction(final String functionName) {
        Function function = this.functions.remove(functionName);
        if (function != null) {
            this.dialectRuntimeRegistry.removeFunction(this,
                                                       function);
        }
    }

    public FactTemplate getFactTemplate(final String name) {
        return this.factTemplates.get(name);
    }

    public void addFactTemplate(final FactTemplate factTemplate) {
        if (this.factTemplates == Collections.EMPTY_MAP) {
            this.factTemplates = new HashMap<>(1);
        }
        this.factTemplates.put(factTemplate.getName(),
                               factTemplate);
    }

    /**
     * Add a <code>Rule</code> to this <code>Package</code>.
     *
     * @param rule The rule to add.
     * @throws org.drools.core.rule.DuplicateRuleNameException If the <code>Rule</code> attempting to be added has the
     *                                                         same name as another previously added <code>Rule</code>.
     * @throws org.drools.core.rule.InvalidRuleException       If the <code>Rule</code> is not valid.
     */
    public void addRule(RuleImpl rule) {
        this.rules.put(rule.getName(),
                       rule);
    }

    /**
     * Add a rule flow to this package.
     */
    public void addProcess(Process process) {
        ResourceTypePackageRegistry rtps = getResourceTypePackages();
        ProcessPackage rtp = ProcessPackage.getOrCreate(rtps);
        rtp.add(process);
    }

    /**
     * Get the rule flows for this package. The key is the ruleflow id. It will
     * be Collections.EMPTY_MAP if none have been added.
     */
    public Map<String, Process> getRuleFlows() {
        ProcessPackage rtp = (ProcessPackage) getResourceTypePackages().get(ResourceType.BPMN2);
        return rtp == null? Collections.emptyMap() : rtp.getRuleFlows();
    }

    /**
     * Rule flows can be removed by ID.
     */
    public void removeRuleFlow(String id) {
        ProcessPackage rtp = (ProcessPackage) getResourceTypePackages().get(ResourceType.BPMN2);
        if (rtp == null || rtp.lookup(id) == null) {
            throw new IllegalArgumentException("The rule flow with id [" + id + "] is not part of this package.");
        }
        rtp.remove(id);
    }

    public void removeRule(RuleImpl rule) {
        this.rules.remove(rule.getName());
        this.dialectRuntimeRegistry.removeRule(this, rule);
    }

    /**
     * Retrieve a <code>Rule</code> by name.
     *
     * @param name The name of the <code>Rule</code> to retrieve.
     * @return The named <code>Rule</code>, or <code>null</code> if not
     * such <code>Rule</code> has been added to this
     * <code>Package</code>.
     */
    public RuleImpl getRule(final String name) {
        return this.rules.get(name);
    }

    public String toString() {
        return "[Package name=" + this.name + "]";
    }

    /**
     * Once this is called, the package will be marked as invalid
     */
    public void setError(final String summary) {
        this.errorSummary = summary;
        this.valid = false;
    }

    /**
     * Once this is called, the package will be marked as invalid
     */
    public void resetErrors() {
        this.errorSummary = "";
        this.valid = true;
    }

    /**
     * @return true (default) if there are no build/structural problems.
     */
    public boolean isValid() {
        return this.valid;
    }

    /**
     * This will throw an exception if the package is not valid
     */
    public void checkValidity() {
        if (!isValid()) {
            throw new InvalidRulePackage(this.getErrorSummary());
        }
    }

    /**
     * This will return the error summary (if any) if the package is invalid.
     */
    public String getErrorSummary() {
        return this.errorSummary;
    }

    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof KnowledgePackageImpl)) {
            return false;
        }

        KnowledgePackageImpl other = (KnowledgePackageImpl) object;

        return this.name.equals(other.name);
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public void clear() {
        this.rules.clear();
        this.dialectRuntimeRegistry.clear();
        this.imports.clear();
        this.functions.clear();
        this.accumulateFunctions.clear();
        this.staticImports.clear();
        this.globals.clear();
        this.factTemplates.clear();
        this.typeDeclarations.clear();
        this.windowDeclarations.clear();
    }

    public FactType getFactType(final String typeName) {
        if (typeName == null || (this.name != null && !typeName.startsWith(this.name + "."))) {
            return null;
        }
        // in case the package name is != null, remove the package name from the
        // beginning of the type name
        String key = this.name == null ? typeName : typeName.substring(this.name.length() + 1);
        TypeDeclaration decl = this.typeDeclarations.get(key);
        if (decl == null) {
            return null;
        } else {
            if (decl.isDefinition() || decl.isGeneratedFact()) {
                return decl.getTypeClassDef();
            } else {
                throw new UnsupportedOperationException("KieBase.getFactType should only be used to retrieve declared beans. Class " + typeName + " exists outside DRL ");
            }
        }
    }

    public void wireTypeDeclarations() {
        for (TypeDeclaration typeDeclaration : typeDeclarations.values()) {
            Class<?> typeClass = null;
            try {
                typeClass = typeDeclaration.getTypeClass();
                if (typeClass != null || !typeClass.isPrimitive()) {
                    Class<?> cls = getPackageClassLoader().loadClass(typeClass.getName());
                    typeDeclaration.setTypeClass(cls);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to load typeClass '" + typeClass.getName() + "'");
            }
        }
    }

    public ClassFieldAccessorStore getClassFieldAccessorStore() {
        return classFieldAccessorStore;
    }

    public void setClassFieldAccessorCache(ClassFieldAccessorCache classFieldAccessorCache) {
        this.classFieldAccessorStore.setClassFieldAccessorCache(classFieldAccessorCache);
    }

    public Set<String> getEntryPointIds() {
        return entryPointsIds;
    }

    public void addEntryPointId(String id) {
        if (entryPointsIds == Collections.EMPTY_SET) {
            entryPointsIds = new HashSet<>();
        }
        entryPointsIds.add(id);
    }

    public TypeResolver getTypeResolver() {
        return typeResolver;
    }

    public void setClassLoader(ClassLoader classLoader) {
        if (typeResolver != null && typeResolver.getClassLoader() == classLoader) {
            return;
        }
        this.typeResolver = new ClassTypeResolver(new HashSet<String>(getImports().keySet()), classLoader, getName());
        typeResolver.addImport(getName() + ".*");
        for (String implicitImport : getImplicitImports()) {
            typeResolver.addImplicitImport(implicitImport);
        }
        this.ruleUnitDescriptionLoader = new RuleUnitDescriptionLoader(this);
    }

    protected String[] getImplicitImports() {
        return implicitImports;
    }

    public RuleUnitDescriptionLoader getRuleUnitDescriptionLoader() {
        return ruleUnitDescriptionLoader;
    }

    public void addWindowDeclaration(WindowDeclaration window) {
        if (windowDeclarations == Collections.EMPTY_MAP) {
            windowDeclarations = new HashMap<>();
        }
        this.windowDeclarations.put(window.getName(), window);
    }

    public Map<String, WindowDeclaration> getWindowDeclarations() {
        return windowDeclarations;
    }

    public boolean removeObjectsGeneratedFromResource(Resource resource) {
        List<RuleImpl> rulesToBeRemoved = removeRulesGeneratedFromResource(resource);
        List<TypeDeclaration> typesToBeRemoved = removeTypesGeneratedFromResource(resource);
        List<Function> functionsToBeRemoved = removeFunctionsGeneratedFromResource(resource);
        boolean resourceTypePackageSomethingRemoved = removeFromResourceTypePackageGeneratedFromResource(resource);
        return !rulesToBeRemoved.isEmpty()
                || !typesToBeRemoved.isEmpty()
                || !functionsToBeRemoved.isEmpty()
                || resourceTypePackageSomethingRemoved;
    }

    @Override
    public boolean removeFromResourceTypePackageGeneratedFromResource(Resource resource) {
        return resourceTypePackages.remove(resource);
    }

    public List<TypeDeclaration> removeTypesGeneratedFromResource(Resource resource) {
        List<TypeDeclaration> typesToBeRemoved = getTypesGeneratedFromResource(resource);
        if (!typesToBeRemoved.isEmpty()) {
            JavaDialectRuntimeData dialect = (JavaDialectRuntimeData) getDialectRuntimeRegistry().getDialectData("java");
            for (TypeDeclaration type : typesToBeRemoved) {
                if (type.getTypeClassName() != null) {
                    // the type declaration might not have been built up to actual class, if an error was found first
                    // in this case, no accessor would have been wired
                    classFieldAccessorStore.removeType(type);
                    dialect.remove(type.getTypeClassName());
                    if (typeResolver != null) {
                        typeResolver.registerClass( type.getTypeClassName(), null );
                    }
                }
                removeTypeDeclaration(type.getTypeName());
            }
            dialect.reload();
        }
        return typesToBeRemoved;
    }

    public List<RuleImpl> removeRulesGeneratedFromResource(Resource resource) {
        List<RuleImpl> rulesToBeRemoved = getRulesGeneratedFromResource(resource);
        for (RuleImpl rule : rulesToBeRemoved) {
            removeRule(rule);
        }
        return rulesToBeRemoved;
    }

    public List<RuleImpl> getRulesGeneratedFromResource(Resource resource) {
        List<RuleImpl> rulesFromResource = new ArrayList<>();
        for (RuleImpl rule : rules.values()) {
            if (resource.equals(rule.getResource())) {
                rulesFromResource.add(rule);
            }
        }
        return rulesFromResource;
    }

    private List<TypeDeclaration> getTypesGeneratedFromResource(Resource resource) {
        List<TypeDeclaration> typesFromResource = new ArrayList<>();
        for (TypeDeclaration type : typeDeclarations.values()) {
            if (resource.equals(type.getResource())) {
                typesFromResource.add(type);
            }
        }
        return typesFromResource;
    }

    public List<Function> removeFunctionsGeneratedFromResource(Resource resource) {
        List<Function> functionsToBeRemoved = getFunctionsGeneratedFromResource(resource);
        for (Function function : functionsToBeRemoved) {
            removeFunction(function.getName());
        }
        return functionsToBeRemoved;
    }

    private List<Function> getFunctionsGeneratedFromResource(Resource resource) {
        List<Function> functionsFromResource = new ArrayList<>();
        for (Function function : functions.values()) {
            if (resource.equals(function.getResource())) {
                functionsFromResource.add(function);
            }
        }
        return functionsFromResource;
    }

    public List<Process> removeProcessesGeneratedFromResource(Resource resource) {
        List<Process> processesToBeRemoved = getProcessesGeneratedFromResource(resource);
        for (Process process : processesToBeRemoved) {
            removeProcess(process);
        }
        return processesToBeRemoved;
    }

    private void removeProcess(Process process) {
        ProcessPackage rtp = (ProcessPackage) getResourceTypePackages().get(ResourceType.BPMN2);
        if (rtp != null) rtp.remove(process.getId());
    }

    private List<Process> getProcessesGeneratedFromResource(Resource resource) {
        ProcessPackage rtp = (ProcessPackage) getResourceTypePackages().get(ResourceType.BPMN2);
        if (rtp == null) {
            return Collections.emptyList();
        }
        List<Process> processesFromResource = new ArrayList<>();
        for (Process process : rtp) {
            if (resource.equals(process.getResource())) {
                processesFromResource.add(process);
            }
        }
        return processesFromResource;
    }

    public boolean needsStreamMode() {
        return needStreamMode;
    }

    public void setNeedStreamMode() {
        this.needStreamMode = true;
    }

    public KnowledgePackageImpl deepCloneIfAlreadyInUse(ClassLoader classLoader) {
        if (inUse.compareAndSet(false, true)) {
            return this;
        }

        if (classLoader instanceof ProjectClassLoader ) {
            JavaDialectRuntimeData javaDialectRuntimeData = (JavaDialectRuntimeData) dialectRuntimeRegistry.getDialectData("java");
            if (javaDialectRuntimeData == null) {
                // using the canonical model there's no runtime registry and no need for any clone
                return this;
            }
            ClassLoader originalClassLoader = javaDialectRuntimeData.getRootClassLoader();
            if (classLoader == originalClassLoader) {
                // if the classloader isn't changed there's no need for a clone
                return this;
            }
            if (originalClassLoader instanceof ProjectClassLoader) {
                ((ProjectClassLoader) classLoader).initFrom((ProjectClassLoader) originalClassLoader);
            }
        }

        KnowledgePackageImpl clonedPkg = ClassUtils.deepClone(this, classLoader, cloningResources);
        clonedPkg.setClassLoader( classLoader );

        if (ruleUnitDescriptionLoader != null) {
            for (String ruleUnit : ruleUnitDescriptionLoader.getDescriptions().keySet()) {
                clonedPkg.getRuleUnitDescriptionLoader().getDescription( ruleUnit );
            }
        }
        
        return clonedPkg;
    }

    @Override
    public boolean hasTraitRegistry() {
        return false;
    }

    @Override
    public TraitRegistry getTraitRegistry() {
        return null;
    }

    @Override
    public void addCloningResource(String key, Object resource) {
        this.cloningResources.put(key, resource);
    }
}
