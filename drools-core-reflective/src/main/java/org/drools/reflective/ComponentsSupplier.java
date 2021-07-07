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

public interface ComponentsSupplier {
    ProjectClassLoader createProjectClassLoader( ClassLoader parent, ResourceProvider resourceProvider );
    ByteArrayClassLoader createByteArrayClassLoader( ClassLoader parent );

    Object createConsequenceExceptionHandler(String className, ClassLoader classLoader);

    Object createTimerService( String className );

    default void addPackageFromXSD(KnowledgeBuilder kBuilder, Resource resource, ResourceConfiguration configuration) throws IOException { }
}
