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

package org.drools.dynamic;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Vector;

import org.drools.reflective.ResourceProvider;
import org.drools.reflective.classloader.ProjectClassLoader;
import org.drools.reflective.util.ClassUtils;

public class DynamicProjectClassLoader extends ProjectClassLoader {

    private static boolean isIBM_JVM = System.getProperty("java.vendor").toLowerCase().contains("ibm");

    protected DynamicProjectClassLoader(ClassLoader parent, ResourceProvider resourceProvider) {
        super(parent, resourceProvider);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    protected boolean isStoreFirst(String name) {
        return isEnableStoreFirst() && generatedClassNames.contains(name);
    }

    public static class IBMDynamicClassLoader extends DynamicProjectClassLoader {

        private final boolean parentImplementsFindResources;

        private static final Enumeration<URL> EMPTY_RESOURCE_ENUM = new Vector<URL>().elements();

        private IBMDynamicClassLoader(ClassLoader parent, ResourceProvider resourceProvider) {
            super(parent, resourceProvider);
            Method m = null;
            try {
                m = parent.getClass().getMethod("findResources", String.class);
            } catch (NoSuchMethodException e) {
            }
            parentImplementsFindResources = m != null && m.getDeclaringClass() == parent.getClass();
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            // if the parent doesn't implemnt this method call getResources directly on it
            // see https://blogs.oracle.com/bhaktimehta/entry/ibm_jdk_and_classloader_getresources
            return parentImplementsFindResources ? EMPTY_RESOURCE_ENUM : getParent().getResources(name);
        }
    }

    public static DynamicProjectClassLoader create(ClassLoader parent, ResourceProvider resourceProvider) {
        return isIBM_JVM ? new IBMDynamicClassLoader(parent, resourceProvider) : new DynamicProjectClassLoader(parent, resourceProvider);
    }

    @Override
    public InternalTypesClassLoader makeClassLoader() {
        return AccessController.doPrivileged(
                                             (PrivilegedAction<InternalTypesClassLoader>) () -> ClassUtils.isAndroid() ? (InternalTypesClassLoader) ClassUtils.instantiateObject(
                                                                                                                                                                                 "org.drools.android.DexInternalTypesClassLoader",
                                                                                                                                                                                 null, this)
                                                     : new DefaultInternalTypesClassLoader(this));
    }

    private static class DefaultInternalTypesClassLoader extends ClassLoader implements InternalTypesClassLoader {

        static {
            registerAsParallelCapable();
        }

        private final ProjectClassLoader projectClassLoader;

        private DefaultInternalTypesClassLoader(ProjectClassLoader projectClassLoader) {
            super(projectClassLoader.getParent());
            this.projectClassLoader = projectClassLoader;
        }

        public Class<?> defineClass(String name, byte[] bytecode) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                String pkgName = name.substring(0, lastDot);
                if (getPackage(pkgName) == null) {
                    definePackage(pkgName, "", "", "", "", "", "", null);
                }
            }
            return defineClass(name, bytecode, 0, bytecode.length);
        }

        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (isStoreFirst(name)) {
                Class<?> clazz = findLoadedClass(name); // skip parent classloader
                if (clazz != null) {
                    return clazz;
                }
                // if generated class, go straight to defineType
                return projectClassLoader.tryDefineType(name, null);
            }
            try {
                return loadType(name, resolve);
            } catch (ClassNotFoundException cnfe) {
                try {
                    return projectClassLoader.internalLoadClass(name, resolve);
                } catch (ClassNotFoundException cnfe2) {
                    return projectClassLoader.tryDefineType(name, cnfe);
                }
            }
        }

        private boolean isStoreFirst(String name) {
            return ProjectClassLoader.isEnableStoreFirst() && projectClassLoader.getGeneratedClassNames().contains(name);
        }

        public Class<?> loadType(String name, boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }

        @Override
        public Class<?> findLoadedClassWithoutParent( String name ) {
            return findLoadedClass(name);
        }

        @Override
        public URL getResource(String name) {
            return projectClassLoader.getResource(name);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            return projectClassLoader.getResourceAsStream(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return projectClassLoader.getResources(name);
        }
    }
}
