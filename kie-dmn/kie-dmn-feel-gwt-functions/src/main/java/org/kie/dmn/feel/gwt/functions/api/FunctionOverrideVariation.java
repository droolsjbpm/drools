/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.feel.gwt.functions.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.dmn.feel.lang.Type;

public class FunctionOverrideVariation implements HumanReadable {

    private final Type returnType;
    private String functionReturnTypeFQCNName;
    private String functionName;
    private final Set<Parameter> parameters;

    public FunctionOverrideVariation(final Type returnType,
                                     final String functionReturnTypeFQCNName,
                                     final String functionName,
                                     final Parameter... parameters) {
        this.returnType = returnType;
        this.functionReturnTypeFQCNName = functionReturnTypeFQCNName;
        this.functionName = functionName;
        this.parameters = new HashSet<>(Arrays.asList(parameters));
    }

    public Type getReturnType() {
        return returnType;
    }

    public String getFunctionReturnTypeFQCNName() {
        return functionReturnTypeFQCNName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Set<Parameter> getParameters() {
        return Collections.unmodifiableSet(parameters);
    }

    @Override
    public String toHumanReadableString() {
        return parameters.stream().map(Parameter::toHumanReadableString).collect(Collectors.joining(", "));
    }

    public FunctionDefinitionStrings toHumanReadableStrings() {

        final StringBuilder humanReadableBuilder = new StringBuilder();
        final StringBuilder templateBuilder = new StringBuilder();

        humanReadableBuilder.append(functionName);
        templateBuilder.append(functionName);
        humanReadableBuilder.append("(");
        templateBuilder.append("(");
        humanReadableBuilder.append(toHumanReadableString());

        templateBuilder.append(IntStream.rangeClosed(1, getParameters().size())
                                       .mapToObj(i -> "$" + i)
                                       .collect(Collectors.joining(", ")));

        humanReadableBuilder.append(")");
        templateBuilder.append(")");

        return new FunctionDefinitionStrings(humanReadableBuilder.toString(),
                                             templateBuilder.toString());
    }
}
