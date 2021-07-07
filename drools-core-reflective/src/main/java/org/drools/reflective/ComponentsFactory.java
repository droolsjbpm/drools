/*
 * Copyright 2005 JBoss Inc
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

package org.drools.reflective;

import java.io.IOException;

import org.drools.reflective.classloader.ProjectClassLoader;
import org.drools.reflective.util.ByteArrayClassLoader;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceConfiguration;
import org.kie.internal.builder.KnowledgeBuilder;

import static org.kie.api.internal.utils.ServiceUtil.instanceFromNames;

public class ComponentsFactory {

    private static final String DYNAMIC_IMPL = "org.drools.dynamic.DynamicComponentsSupplier";
    private static final String STATIC_IMPL = "org.drools.statics.StaticComponentsSupplier";

    private static ComponentsSupplier supplier;

    public static ProjectClassLoader createProjectClassLoader( ClassLoader parent, ResourceProvider resourceProvider ) {
        return getComponentsSupplier().createProjectClassLoader(parent, resourceProvider);
    }

    public static ByteArrayClassLoader createByteArrayClassLoader( ClassLoader parent ) {
        return getComponentsSupplier().createByteArrayClassLoader(parent);
    }

    public static Object createConsequenceExceptionHandler(String className, ClassLoader classLoader) {
        return getComponentsSupplier().createConsequenceExceptionHandler(className, classLoader);
    }

    public static Object createTimerService( String className ) {
        return getComponentsSupplier().createTimerService( className );
    }

    public static void addPackageFromXSD( KnowledgeBuilder kBuilder, Resource resource, ResourceConfiguration configuration) throws IOException {
        getComponentsSupplier().addPackageFromXSD(kBuilder, resource, configuration);
    }

    public static void setComponentsSupplier( ComponentsSupplier supplier ) {
        ComponentsFactory.supplier = supplier;
    }

    private static ComponentsSupplier getComponentsSupplier() {
        if (supplier == null) {
            ComponentsFactory.supplier = Holder.supplier;
        }
        return ComponentsFactory.supplier;
    }

    private static class Holder {
        private static ComponentsSupplier supplier = instanceFromNames(DYNAMIC_IMPL, STATIC_IMPL);
    }
}
