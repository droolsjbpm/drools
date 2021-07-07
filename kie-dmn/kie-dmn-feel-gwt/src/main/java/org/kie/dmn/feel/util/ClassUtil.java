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
package org.kie.dmn.feel.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gwt.core.client.GWT;
import org.kie.dmn.feel.gwt.functions.api.FunctionOverrideVariation;
import org.kie.dmn.feel.gwt.functions.client.FEELFunctionProvider;
import org.kie.dmn.feel.gwtreflection.MethodProvider;
import org.kie.dmn.feel.runtime.functions.BaseFEELFunction;

public class ClassUtil {

    private static FEELFunctionProvider FUNCTION_PROVIDER = GWT.create(FEELFunctionProvider.class);
    private static MethodProvider METHOD_PROVIDER = GWT.create(MethodProvider.class);

    public static boolean isAssignableFrom(final Class thisClass,
                                           final Class otherClass) {

        if (otherClass == null) {
            return false;
        }

        if (otherClass.equals(thisClass)) {
            return true;
        }

        return isAssignableFrom(thisClass, otherClass.getSuperclass());
    }

    public static Method[] getMethods(final Class<?> clazz) {
        return null;
    }

    public static Method getMethod(final Class<?> clazz, final String methodName) throws NoSuchMethodException,
            SecurityException {
        return null;
    }

    public static Method getMethod(final Class<?> clazz, final String methodName, final Class[] paramTypes) throws NoSuchMethodException {
        return null;
    }

    public static Class<?> forName(final String typeName, final boolean initialize, final ClassLoader classLoader) throws ClassNotFoundException {
        return null;
    }

    public static boolean isInstance(final Class<?> clazz, final Object o) {
        return isAssignableFrom(clazz, o.getClass());
    }

    public static Method[] getDeclaredMethods(final Class<? extends BaseFEELFunction> clazz) {

        final List<Method> result = new ArrayList<>();

        for (FunctionOverrideVariation definition : FUNCTION_PROVIDER.getDefinitions()) {
            if (Objects.equals(clazz.getName(), definition.getFunctionReturnTypeFQCNName())) {
                final Method method = METHOD_PROVIDER.get(definition);
                result.add(method);
            }
        }

        return result.toArray(new Method[result.size()]);
    }
}
