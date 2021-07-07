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
package org.kie.pmml.compiler.commons.codegenfactories;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.MapValues;
import org.kie.pmml.api.enums.BOOLEAN_OPERATOR;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;

import static org.kie.pmml.commons.Constants.MISSING_BODY_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_INITIALIZER_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_IN_BODY;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLFieldColumnPairFactory.getFieldColumnPairVariableDeclaration;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLInlineTableFactory.getInlineTableVariableDeclaration;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getChainedMethodCallExprFrom;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getExpressionForObject;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getVariableDeclarator;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.MAIN_CLASS_NOT_FOUND;

/**
 * Class meant to provide <i>helper</i> method to retrieve <code>KiePMMLMapValues</code> code-generators
 * out of <code>MapValues</code>s
 */
public class KiePMMLMapValuesFactory {

    static final String KIE_PMML_MAPVALUES_TEMPLATE_JAVA = "KiePMMLMapValuesTemplate.tmpl";
    static final String KIE_PMML_MAPVALUES_TEMPLATE = "KiePMMLMapValuesTemplate";
    static final String GETKIEPMMLMAPVALUES = "getKiePMMLMapValues";
    static final String MAPVALUES = "mapValues";
    static final ClassOrInterfaceDeclaration MAPVALUES_TEMPLATE;

    static {
        CompilationUnit cloneCU = JavaParserUtils.getFromFileName(KIE_PMML_MAPVALUES_TEMPLATE_JAVA);
        MAPVALUES_TEMPLATE = cloneCU.getClassByName(KIE_PMML_MAPVALUES_TEMPLATE)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + KIE_PMML_MAPVALUES_TEMPLATE));
        MAPVALUES_TEMPLATE.getMethodsByName(GETKIEPMMLMAPVALUES).get(0).clone();
    }

    private KiePMMLMapValuesFactory() {
        // Avoid instantiation
    }

    static BlockStmt getMapValuesVariableDeclaration(final String variableName, final MapValues mapValues) {
        if (mapValues.getInlineTable() == null && mapValues.getTableLocator() != null) {
            throw new UnsupportedOperationException("TableLocator not supported, yet");
        }
        final MethodDeclaration methodDeclaration =
                MAPVALUES_TEMPLATE.getMethodsByName(GETKIEPMMLMAPVALUES).get(0).clone();
        final BlockStmt mapValuesBody =
                methodDeclaration.getBody().orElseThrow(() -> new KiePMMLException(String.format(MISSING_BODY_TEMPLATE, methodDeclaration)));
        final VariableDeclarator variableDeclarator =
                getVariableDeclarator(mapValuesBody, MAPVALUES).orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_IN_BODY, MAPVALUES, mapValuesBody)));
        variableDeclarator.setName(variableName);
        final BlockStmt toReturn = new BlockStmt();
        int counter = 0;
        final NodeList<Expression> arguments = new NodeList<>();
        if (mapValues.hasFieldColumnPairs()) {
            for (FieldColumnPair fieldColumnPair : mapValues.getFieldColumnPairs()) {
                String nestedVariableName = String.format("%s_%s", variableName, counter);
                arguments.add(new NameExpr(nestedVariableName));
                BlockStmt toAdd = getFieldColumnPairVariableDeclaration(nestedVariableName, fieldColumnPair);
                toAdd.getStatements().forEach(toReturn::addStatement);
                counter++;
            }
        }
        String inlineTableVariableName = String.format("%s_InlineTable", variableName);
        BlockStmt toAdd = getInlineTableVariableDeclaration(inlineTableVariableName, mapValues.getInlineTable());
        toAdd.getStatements().forEach(toReturn::addStatement);
        final MethodCallExpr initializer = variableDeclarator.getInitializer()
                .orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_INITIALIZER_TEMPLATE,
                MAPVALUES, toReturn)))
                .asMethodCallExpr();
        final MethodCallExpr builder = getChainedMethodCallExprFrom("builder", initializer);
        final StringLiteralExpr nameExpr = new StringLiteralExpr(variableName);
        final StringLiteralExpr outputColumnExpr = new StringLiteralExpr(mapValues.getOutputColumn());
        builder.setArgument(0, nameExpr);
        builder.setArgument(2, outputColumnExpr);
        final Expression dataTypeExpression;
        if (mapValues.getDataType() != null) {
            final DATA_TYPE dataType = DATA_TYPE.byName(mapValues.getDataType().value());
            dataTypeExpression = new NameExpr(DATA_TYPE.class.getName() + "." + dataType.name());
        } else {
            dataTypeExpression = new NullLiteralExpr();
        }
        getChainedMethodCallExprFrom("withDefaultValue", initializer).setArgument(0, getExpressionForObject
        (mapValues.getDefaultValue()));
        getChainedMethodCallExprFrom("withMapMissingTo", initializer).setArgument(0, getExpressionForObject
                (mapValues.getMapMissingTo()));
        getChainedMethodCallExprFrom("withDataType", initializer).setArgument(0, dataTypeExpression);
        getChainedMethodCallExprFrom("withKiePMMLInlineTable", initializer).setArgument(0,
        new NameExpr(inlineTableVariableName));
        getChainedMethodCallExprFrom("asList", initializer).setArguments(arguments);
        mapValuesBody.getStatements().forEach(toReturn::addStatement);
        return toReturn;
    }
}
