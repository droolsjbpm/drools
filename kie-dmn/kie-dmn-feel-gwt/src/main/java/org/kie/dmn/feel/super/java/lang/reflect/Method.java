/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package java.lang.reflect;

import java.lang.annotation.Annotation;

import com.google.gwt.core.client.GWT;
import org.kie.dmn.feel.gwt.functions.api.FunctionOverrideVariation;
import org.kie.dmn.feel.gwtreflection.MethodInvoker;
import org.kie.dmn.feel.runtime.functions.FEELFnResult;

public class Method {

    private final static MethodInvoker METHOD_INVOKER = GWT.create(MethodInvoker.class);

    private FunctionOverrideVariation definition;

    public Method(final FunctionOverrideVariation definition) {
        this.definition = definition;
    }

    public String getName() {
        return "invoke";
    }

    public int getParameterCount() {
        return definition.getParameters().size();
    }

    public Object invoke(Object obj, Object... args)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        return METHOD_INVOKER.invoke(obj, args);
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }

    public Class<?> getReturnType() {
        return FEELFnResult.class; // TODO debug to see what this is
    }

    public Class<?>[] getParameterTypes() {
        final Class[] classes = new Class[definition.getParameters().size()];

        int i = 0;
        for (final org.kie.dmn.feel.gwt.functions.api.Parameter parameter : definition.getParameters()) {
            classes[i++] = parameter.getParameterTypeClass();
        }

        return classes;
    }

    public Annotation[][] getParameterAnnotations() {
        return null; // TODO debug to see what this is
    }

    public int getModifiers() {
        return Modifier.PUBLIC;
    }

    public Parameter[] getParameters() {
        return new Parameter[0];// TODO make these NamedParameter?
    }
}