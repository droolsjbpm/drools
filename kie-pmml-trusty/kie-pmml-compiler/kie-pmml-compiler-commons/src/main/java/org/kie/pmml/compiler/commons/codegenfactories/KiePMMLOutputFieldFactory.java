/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.dmg.pmml.OutputField;
import org.kie.pmml.api.enums.RESULT_FEATURE;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;

import static org.kie.pmml.commons.Constants.MISSING_BODY_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_INITIALIZER_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_IN_BODY;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLExpressionFactory.getKiePMMLExpression;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getChainedMethodCallExprFrom;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getExpressionForObject;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getVariableDeclarator;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.MAIN_CLASS_NOT_FOUND;

public class KiePMMLOutputFieldFactory {

    private KiePMMLOutputFieldFactory() {
    }

    static final String KIE_PMML_OUTPUTFIELD_TEMPLATE_JAVA = "KiePMMLOutputFieldTemplate.tmpl";
    static final String KIE_PMML_OUTPUTFIELD_TEMPLATE = "KiePMMLOutputFieldTemplate";
    static final String GETKIEPMMLOUTPUTFIELD = "getKiePMMLOutputField";
    static final String OUTPUTFIELD = "outputField";
    static final ClassOrInterfaceDeclaration OUTPUTFIELD_TEMPLATE;


    static {
        CompilationUnit cloneCU = JavaParserUtils.getFromFileName(KIE_PMML_OUTPUTFIELD_TEMPLATE_JAVA);
        OUTPUTFIELD_TEMPLATE = cloneCU.getClassByName(KIE_PMML_OUTPUTFIELD_TEMPLATE)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + KIE_PMML_OUTPUTFIELD_TEMPLATE));
        OUTPUTFIELD_TEMPLATE.getMethodsByName(GETKIEPMMLOUTPUTFIELD).get(0).clone();
    }

    static BlockStmt getOutputFieldVariableDeclaration(final String variableName, final OutputField outputField) {
        final MethodDeclaration methodDeclaration = OUTPUTFIELD_TEMPLATE.getMethodsByName(GETKIEPMMLOUTPUTFIELD).get(0).clone();
        final BlockStmt outputFieldBody = methodDeclaration.getBody().orElseThrow(() -> new KiePMMLException(String.format(MISSING_BODY_TEMPLATE, methodDeclaration)));
        final VariableDeclarator variableDeclarator = getVariableDeclarator(outputFieldBody, OUTPUTFIELD) .orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_IN_BODY, OUTPUTFIELD, outputFieldBody)));
        variableDeclarator.setName(variableName);
        final BlockStmt toReturn = new BlockStmt();
        final Expression expressionExpr;
        if (outputField.getExpression() != null) {
            String nestedVariableName = String.format("%s_Expression", variableName);
            BlockStmt toAdd = getKiePMMLExpression(nestedVariableName, outputField.getExpression());
            toAdd.getStatements().forEach(toReturn::addStatement);
            expressionExpr = new NameExpr(nestedVariableName);
        } else {
            expressionExpr = new NullLiteralExpr();
        }
        final MethodCallExpr initializer = variableDeclarator.getInitializer()
                .orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_INITIALIZER_TEMPLATE, OUTPUTFIELD, toReturn)))
                .asMethodCallExpr();
        final MethodCallExpr builder = getChainedMethodCallExprFrom("builder", initializer);
        final StringLiteralExpr nameExpr = new StringLiteralExpr(outputField.getName().getValue());
        final RESULT_FEATURE resultFeature = RESULT_FEATURE.byName(outputField.getResultFeature().value());
        final NameExpr resultFeatureExpr = new NameExpr(RESULT_FEATURE.class.getName() + "." + resultFeature.name());
        final Expression targetFieldExpr = outputField.getTargetField() != null ? getExpressionForObject(outputField.getTargetField().getValue()) : new NullLiteralExpr();
        final Expression valueExpr = outputField.getValue() != null ? getExpressionForObject(outputField.getValue()) : new NullLiteralExpr();
        final Expression rankExpr = outputField.getRank() != null ? getExpressionForObject(outputField.getRank()) : new NullLiteralExpr();
        builder.setArgument(0, nameExpr);
        getChainedMethodCallExprFrom("withResultFeature", initializer).setArgument(0, resultFeatureExpr);
        getChainedMethodCallExprFrom("withTargetField", initializer).setArgument(0, targetFieldExpr);
        getChainedMethodCallExprFrom("withValue", initializer).setArgument(0, valueExpr);
        getChainedMethodCallExprFrom("withRank", initializer).setArgument(0, rankExpr);
        getChainedMethodCallExprFrom("withKiePMMLExpression", initializer).setArgument(0, expressionExpr);

        outputFieldBody.getStatements().forEach(toReturn::addStatement);
        return toReturn;
    }
}
