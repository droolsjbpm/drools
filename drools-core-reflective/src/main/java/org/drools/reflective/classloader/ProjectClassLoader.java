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

package org.drools.reflective.classloader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.drools.reflective.ComponentsFactory;
import org.drools.reflective.ResourceProvider;
import org.drools.reflective.util.ClassUtils;
import org.kie.internal.utils.KieTypeResolver;
import org.kie.memorycompiler.StoreClassLoader;
import org.kie.memorycompiler.WritableClassLoader;

public abstract class ProjectClassLoader extends ClassLoader implements KieTypeResolver, StoreClassLoader, WritableClassLoader {

    private static final boolean CACHE_NON_EXISTING_CLASSES = true;
    private static final ClassNotFoundException dummyCFNE = CACHE_NON_EXISTING_CLASSES ?
                                                            new ClassNotFoundException("This is just a cached Exception. Disable non existing classes cache to see the actual one.") :
                                                            null;

    private static boolean enableStoreFirst = Boolean.valueOf(System.getProperty("drools.projectClassLoader.enableStoreFirst", "true"));

    static {
        registerAsParallelCapable();
    }

    private Map<String, byte[]> store;

    private Map<String, ClassBytecode> definedTypes;

    private final Set<String> nonExistingClasses = ConcurrentHashMap.newKeySet();

    private ClassLoader droolsClassLoader;

    private InternalTypesClassLoader typesClassLoader;

    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<String, Class<?>>();

    protected Set<String> generatedClassNames = new HashSet<>();

    private ResourceProvider resourceProvider;

    protected ProjectClassLoader( ClassLoader parent, ResourceProvider resourceProvider) {
        super(parent);
        this.resourceProvider = resourceProvider;
    }

    public static ClassLoader getClassLoader(final ClassLoader classLoader,
                                             final Class< ? > cls,
                                             final boolean enableCache) {
        ProjectClassLoader projectClassLoader = createProjectClassLoader(classLoader);
        if (cls != null) {
            projectClassLoader.setDroolsClassLoader(cls.getClassLoader());
        }
        return projectClassLoader;
    }

    public ClassLoader getTypesClassLoader() {
        return typesClassLoader instanceof ClassLoader ? (( ClassLoader ) typesClassLoader) : this;
    }

    public static ClassLoader findParentClassLoader() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }
        if (parent == null) {
            parent = ProjectClassLoader.class.getClassLoader();
        }
        return parent;
    }

    public static ProjectClassLoader createProjectClassLoader() {
        return ComponentsFactory.createProjectClassLoader(findParentClassLoader(), null);
    }

    public static ProjectClassLoader createProjectClassLoader(ClassLoader parent) {
        return createProjectClassLoader(parent, (ResourceProvider)null);
    }

    public static ProjectClassLoader createProjectClassLoader(ClassLoader parent, ResourceProvider resourceProvider) {
        if (parent == null) {
            return ComponentsFactory.createProjectClassLoader(findParentClassLoader(), resourceProvider);
        }
        return parent instanceof ProjectClassLoader ? (ProjectClassLoader)parent : ComponentsFactory.createProjectClassLoader(parent, resourceProvider);
    }

    public static ProjectClassLoader createProjectClassLoader(ClassLoader parent, Map<String, byte[]> store) {
        ProjectClassLoader projectClassLoader = createProjectClassLoader(parent);
        projectClassLoader.store = store;
        return projectClassLoader;
    }

    public abstract boolean isDynamic();

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> cls = loadedClasses.get(name);
        if (cls != null) {
            return cls;
        }

        if (isStoreFirst(name)) {
            Class<?> clazz = findLoadedClass(name); // skip parent classloader
            if (clazz != null) {
                return clazz;
            }
            if (typesClassLoader != null) {
                clazz = typesClassLoader.findLoadedClassWithoutParent(name);
                if (clazz != null) {
                    return clazz;
                }
            }
            // if generated class, go straight to defineType
            cls = tryDefineType(name, null);
        } else {
            try {
                cls = internalLoadClass(name, resolve);
            } catch (ClassNotFoundException e2) {
                // for stored classes which are not in generatedClassNames
                cls = loadType(name, resolve);
            }
        }

        loadedClasses.put(name, cls);
        return cls;
    }

    protected boolean isStoreFirst(String name) {
        return false;
    }

    // This method has to be public because is also used by the android ClassLoader
    public Class<?> internalLoadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (CACHE_NON_EXISTING_CLASSES && nonExistingClasses.contains(name)) {
            throw dummyCFNE;
        }

        if (droolsClassLoader != null) {
            try {
                return Class.forName(name, resolve, droolsClassLoader);
            } catch (ClassNotFoundException e) { }
        }
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(name, resolve, getParent());
            } catch (ClassNotFoundException e1) {
                if (CACHE_NON_EXISTING_CLASSES) {
                    nonExistingClasses.add(name);
                }
                throw e1;
            }
        }
    }

    private Class<?> loadType(String name, boolean resolve) throws ClassNotFoundException {
        ClassNotFoundException cnfe = null;
        if (typesClassLoader != null) {
            try {
                return typesClassLoader.loadType(name, resolve);
            } catch (ClassNotFoundException e) {
                cnfe = e;
            }
        }
        return tryDefineType(name, cnfe);
    }

    // This method has to be public because is also used by the android ClassLoader
    public Class<?> tryDefineType(String name, ClassNotFoundException cnfe) throws ClassNotFoundException {
        byte[] bytecode = getBytecode( ClassUtils.convertClassToResourcePath(name));
        if (bytecode == null) {
            if (CACHE_NON_EXISTING_CLASSES) {
                nonExistingClasses.add(name);
            }
            throw cnfe != null ? cnfe : new ClassNotFoundException(name);
        }
        return defineType(name, bytecode);
    }

    private synchronized Class<?> defineType(String name, byte[] bytecode) {
        if (definedTypes == null) {
            definedTypes = new HashMap<String, ClassBytecode>();
        } else {
            ClassBytecode existingClass = definedTypes.get(name);
            if (existingClass != null && Arrays.equals(bytecode, existingClass.bytes)) {
                return existingClass.clazz;
            }
        }

        if (typesClassLoader == null) {
            typesClassLoader = makeClassLoader();
        }
        Class<?> clazz = typesClassLoader.defineClass(name, bytecode);
        definedTypes.put(name, new ClassBytecode(clazz, bytecode));
        loadedClasses.put(name, clazz);
        return clazz;
    }

    @Override
    public Class<?> writeClass( String name, byte[] bytecode ) {
        return defineClass( name, bytecode, 0, bytecode.length );
    }

    public Class<?> defineClass(String name, byte[] bytecode) {
        return defineClass(name, ClassUtils.convertClassToResourcePath(name), bytecode);
    }

    public synchronized Class<?> defineClass(String name, String resourceName, byte[] bytecode) {
        storeClass(name, resourceName, bytecode);
        return defineType(name, bytecode);
    }

    public synchronized void undefineClass(String name) {
        String resourceName = ClassUtils.convertClassToResourcePath(name);
        if (store.remove(resourceName) != null) {
            if (CACHE_NON_EXISTING_CLASSES) {
                nonExistingClasses.add(name);
            }
            typesClassLoader = null;
        }
    }

    public void storeClass(String name, byte[] bytecode) {
        storeClass(name, ClassUtils.convertClassToResourcePath(name), bytecode);
    }

    public void storeClasses(Map<String, byte[]> classesMap) {
        for ( Map.Entry<String, byte[]> entry : classesMap.entrySet() ) {
            if ( entry.getValue() != null ) {
                String resourceName = entry.getKey();
                String className = ClassUtils.convertResourceToClassName( resourceName );
                storeClass( className, resourceName, entry.getValue() );
            }
        }
    }

    public void storeClass(String name, String resourceName, byte[] bytecode) {
        if (store == null) {
            store = new HashMap<String, byte[]>();
        }
        store.put(resourceName, bytecode);
        if (CACHE_NON_EXISTING_CLASSES) {
            nonExistingClasses.remove(name);
        }
    }

    public boolean isClassInUse(String className, Class<? extends Annotation> annotationClazz) {
        Class<?> clazz = loadedClasses.get(className);

        boolean clazzFound = clazz != null;
        if (annotationClazz != null) {
            return clazzFound && !clazz.isAnnotationPresent(annotationClazz);
        } else {
            return clazzFound;
        }
    }

    public boolean isClassInUse(String className) {
        return loadedClasses.containsKey(className);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] bytecode = getBytecode(name);
        if (bytecode != null) {
            return new ByteArrayInputStream( bytecode );
        }
        if (resourceProvider != null) {
            try {
                InputStream is = resourceProvider.getResourceAsStream(name);
                if (is != null) {
                    return is;
                }
            } catch (IOException e) { }
        }
        return super.getResourceAsStream(name);
    }

    @Override
    public URL getResource(String name) {
        if (droolsClassLoader != null) {
            URL resource = droolsClassLoader.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        if (resourceProvider != null) {
            URL resource = resourceProvider.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> resources = super.getResources(name);
        if (resourceProvider != null) {
            URL providedResource = resourceProvider.getResource(name);
            if (resources != null) {
                return new ResourcesEnum(providedResource, resources);
            }
        }
        return resources;
    }

    public Set<String> getGeneratedClassNames() {
        return generatedClassNames;
    }

    public void setGeneratedClassNames(Set<String> generatedClassNames) {
        this.generatedClassNames = generatedClassNames;
    }

    public static boolean isEnableStoreFirst() {
        return enableStoreFirst;
    }

    // test purpose
    static void setEnableStoreFirst(boolean enableStoreFirst) {
        ProjectClassLoader.enableStoreFirst = enableStoreFirst;
    }

    private static class ResourcesEnum implements Enumeration<URL> {

        private URL providedResource;
        private final Enumeration<URL> resources;

        private ResourcesEnum(URL providedResource, Enumeration<URL> resources) {
            this.providedResource = providedResource;
            this.resources = resources;
        }

        @Override
        public boolean hasMoreElements() {
            return providedResource != null || resources.hasMoreElements();
        }

        @Override
        public URL nextElement() {
            if (providedResource != null) {
                URL result = providedResource;
                providedResource = null;
                return result;
            }
            return resources.nextElement();
        }
    }

    public byte[] getBytecode(String resourceName) {
        return store == null ? null : store.get(resourceName);
    }

    @Override
    public Map<String, byte[]> getStore() {
        return store;
    }

    public void clearStore() {
        if (store != null) {
            store.clear();
        }
    }

    public void setDroolsClassLoader(ClassLoader droolsClassLoader) {
        if (getParent() != droolsClassLoader && isModularClassLoader(droolsClassLoader)) {
            this.droolsClassLoader = droolsClassLoader;
            if (CACHE_NON_EXISTING_CLASSES) {
                nonExistingClasses.clear();
            }
        }
    }

    private static boolean isModularClassLoader(ClassLoader cl) {
        return isOsgiClassLoader(cl) || isJbossModuleClassLoader(cl);
    }

    private static boolean isJbossModuleClassLoader(ClassLoader cl) {
        return "org.jboss.modules".equals( cl.getClass().getPackage().getName() );
    }

    private static boolean isOsgiClassLoader(ClassLoader cl) {
        for (Class<?> clc = cl.getClass(); clc != null && !clc.equals(ClassLoader.class); clc = clc.getSuperclass()) {
            if (Stream.of(clc.getInterfaces()).map(Class::getSimpleName).anyMatch(name -> name.equals("BundleReference"))) {
                return true;
            }
        }
        return false;
    }

    // WARNING: This is and should be used just for testing purposes.
    // If not, dragons will come to the Earth, eat all cookies and
    // hijack all kittens and puppies.
    public void setInternalClassLoader(InternalTypesClassLoader classLoader) {
        typesClassLoader = classLoader;
    }

    public void setResourceProvider(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public void initFrom(ProjectClassLoader other) {
        if (other.store != null) {
            if (store == null) {
                store = new HashMap<String, byte[]>();
            }
            store.putAll(other.store);
        }
        nonExistingClasses.addAll(other.nonExistingClasses);
    }

    public abstract InternalTypesClassLoader makeClassLoader();

    public interface InternalTypesClassLoader extends KieTypeResolver {
        Class<?> defineClass( String name, byte[] bytecode );
        Class<?> loadType( String name, boolean resolve ) throws ClassNotFoundException;
        default Class<?> findLoadedClassWithoutParent(String name) {
            throw new UnsupportedOperationException();
        };
    }

    public synchronized List<String> reinitTypes() {
        typesClassLoader = null;
        nonExistingClasses.clear();
        loadedClasses.clear();
        if (definedTypes != null) {
            List<String> removedTypes = new ArrayList<>(definedTypes.keySet());
            definedTypes.clear();
            return removedTypes;
        }
        return Collections.emptyList();
    }

    private static class ClassBytecode {
        private final Class<?> clazz;
        private final byte[] bytes;

        private ClassBytecode(Class<?> clazz, byte[] bytes) {
            this.clazz = clazz;
            this.bytes = bytes;
        }
    }
}
