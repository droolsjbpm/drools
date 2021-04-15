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

import org.kie.dmn.feel.runtime.functions.BaseFEELFunction;

public class ClassUtil {

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
        return true;
    }

    public static Method[] getDeclaredMethods(final Class<? extends BaseFEELFunction> clazz) {
        return new Method[0];
    }
}
