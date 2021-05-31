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

package org.kie.dmn.feel.gwt.functions.rebind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.kie.dmn.feel.runtime.FEELFunction;
import org.kie.dmn.feel.runtime.functions.BuiltInFunctions;
import org.kie.dmn.feel.runtime.functions.DateAndTimeFunction;
import org.kie.dmn.feel.runtime.functions.DateFunction;
import org.kie.dmn.feel.runtime.functions.GetValueFunction;
import org.kie.dmn.feel.runtime.functions.ModeFunction;
import org.kie.dmn.feel.runtime.functions.ParameterName;
import org.kie.dmn.feel.runtime.functions.ReplaceFunction;
import org.kie.dmn.feel.runtime.functions.TimeFunction;
import org.kie.dmn.feel.runtime.functions.extended.KieExtendedDMNFunctions;

public class MethodTemplates {

    public static String getTemplate() {

        final StringBuilder builder = new StringBuilder();

        builder.append("private ArrayList definitions = null;\n");

        builder.append("public List<FunctionOverrideVariation> getDefinitions() {\n");

        builder.append("    if(definitions == null) {\n");
        builder.append("        definitions = new ArrayList();\n");
        for (final String signature : getFunctionSignatures()) {
            builder.append(String.format("      definitions.add( %s );\n", signature));
        }
        builder.append("    }\n");

        builder.append("    return definitions;\n");
        builder.append("}");

        return builder.toString();
    }

    private static List<String> getFunctionSignatures() {
        return getFeelFunctions()
                .stream()
                .flatMap(function -> getFunctionSignatures(function).stream())
                .collect(Collectors.toList());
    }

    static List<FEELFunction> getFeelFunctions() {
        final List<FEELFunction> feelFunctions = new ArrayList<>();
        feelFunctions.addAll(Arrays.asList(BuiltInFunctions.getFunctions()));
        feelFunctions.addAll(Arrays.asList(KieExtendedDMNFunctions.getFunctions()));
        return feelFunctions;
    }

    private static List<String> getFunctionSignatures(final FEELFunction function) {

        final List<String> result = new ArrayList<>();

        for (Method declaredMethod : function.getClass().getDeclaredMethods()) {
            if (!Modifier.isPublic(declaredMethod.getModifiers()) || !declaredMethod.getName().equals("invoke")) {
                continue;
            }

            StringBuilder paramBuilder = new StringBuilder();
            final Annotation[][] parameterAnnotations = declaredMethod.getParameterAnnotations();

            int i = 0;
            for (Annotation[] parameterAnnotation : parameterAnnotations) {
                if (i != 0) {
                    paramBuilder.append(", ");
                }

                final Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                paramBuilder.append(String.format("new Parameter( \"%s\", %s, %s )",
                                                  getParameterNameAnnotation(parameterAnnotation),
                                                  getClass(parameterTypes[i]),
                                                  getType(parameterTypes[i].getTypeName())));
                i++;
            }

            if (paramBuilder.length() == 0) {
                result.add(String.format("new FunctionOverrideVariation( %s, \"%s\", \"%s\" )",
                                         getReturnType(function,
                                                       declaredMethod.getGenericReturnType()),
                                         function.getClass().getName(),
                                         function.getName()));
            } else {

                result.add(String.format("new FunctionOverrideVariation( %s, \"%s\", \"%s\", %s )",
                                         getReturnType(function,
                                                       declaredMethod.getGenericReturnType()),
                                         function.getClass().getName(),
                                         function.getName(),
                                         paramBuilder.toString()));
            }
        }

        return result;
    }

    private static String getClass(final Class<?> parameterType) {
        if (Objects.equals("[Ljava.lang.Object;", parameterType.getName())) {
            return "Object[].class";
        } else {
            return parameterType.getName() + ".class";
        }
    }

    private static String getParameterNameAnnotation(final Annotation[] parameterAnnotation) {
        for (Annotation annotation : parameterAnnotation) {
            if (annotation instanceof ParameterName) {
                return ((ParameterName) annotation).value();
            }
        }
        return "";
    }

    private static String getReturnType(final FEELFunction function,
                                        final Type genericReturnType) {

        if (genericReturnType instanceof ParameterizedType) {
            final Type[] actualTypeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
            if (actualTypeArguments.length == 1) {

                /*
                 * Getting dates by function since the argument type is the same for them all.
                 * Other than that, better to use the type name so there is better support
                 * for new functions we do not need to write an if for each function.
                 */
                if (function instanceof DateFunction) {
                    return "BuiltInType.DATE";
                } else if (function instanceof TimeFunction) {
                    return "BuiltInType.TIME";
                } else if (function instanceof DateAndTimeFunction) {
                    return "BuiltInType.DATE_TIME";
                } else if (function instanceof GetValueFunction) {
                    return "BuiltInType.UNKNOWN";
                } else if (function instanceof ModeFunction) {
                    return "BuiltInType.LIST";
                } else if (function instanceof ReplaceFunction) {
                    return "BuiltInType.STRING";
                }
                return getType(actualTypeArguments[0].getTypeName());
            }
        }

        return "BuiltInType.UNKNOWN";
    }

    private static String getType(final String typeName) {
        if (typeName.equals("java.time.temporal.TemporalAmount")) {
            return "BuiltInType.DURATION";
        } else if (typeName.equals("java.time.temporal.TemporalAccessor")) {
            return "BuiltInType.DATE_TIME";
        } else if (typeName.equals("java.time.temporal.Temporal")) {
            return "BuiltInType.DATE_TIME";
        } else if (typeName.equals("java.lang.String")) {
            return "BuiltInType.STRING";
        } else if (typeName.equals("java.lang.Boolean")) {
            return "BuiltInType.BOOLEAN";
        } else if (typeName.startsWith("java.util.List")) {
            return "BuiltInType.LIST";
        } else if (typeName.startsWith("java.time.Duration")) {
            return "BuiltInType.DURATION";
        } else if (typeName.equals("java.math.BigDecimal")) {
            return "BuiltInType.NUMBER";
        } else if (typeName.equals("java.lang.Number")) {
            return "BuiltInType.NUMBER";
        } else if (typeName.endsWith("Range")) {
            return "BuiltInType.RANGE";
        } else if (typeName.endsWith("ComparablePeriod")) {
            return "BuiltInType.DURATION";
        } else if (typeName.endsWith("FEELFunction")) {
            return "BuiltInType.FUNCTION";
        }
        return "BuiltInType.UNKNOWN";
    }
}
