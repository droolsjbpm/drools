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
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.dmg.pmml.Interval;
import org.kie.pmml.api.enums.CLOSURE;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;

import static org.kie.pmml.commons.Constants.MISSING_BODY_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_INITIALIZER_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_IN_BODY;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getExpressionForObject;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getVariableDeclarator;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.MAIN_CLASS_NOT_FOUND;

/**
 * Class meant to provide <i>helper</i> method to retrieve <code>KiePMMLInterval</code> code-generators
 * out of <code>Interval</code>s
 */
public class KiePMMLIntervalFactory {

    static final String KIE_PMML_INTERVAL_TEMPLATE_JAVA = "KiePMMLIntervalTemplate.tmpl";
    static final String KIE_PMML_INTERVAL_TEMPLATE = "KiePMMLIntervalTemplate";
    static final String GETKIEPMMLINTERVAL = "getKiePMMLInterval";
    static final String INTERVAL = "interval";
    static final ClassOrInterfaceDeclaration INTERVAL_TEMPLATE;

    static {
        CompilationUnit cloneCU = JavaParserUtils.getFromFileName(KIE_PMML_INTERVAL_TEMPLATE_JAVA);
        INTERVAL_TEMPLATE = cloneCU.getClassByName(KIE_PMML_INTERVAL_TEMPLATE)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + KIE_PMML_INTERVAL_TEMPLATE));
        INTERVAL_TEMPLATE.getMethodsByName(GETKIEPMMLINTERVAL).get(0).clone();
    }

    private KiePMMLIntervalFactory() {
        // Avoid instantiation
    }

    static BlockStmt getIntervalVariableDeclaration(final String variableName,
                                                        final Interval interval) {
        final MethodDeclaration methodDeclaration =
                INTERVAL_TEMPLATE.getMethodsByName(GETKIEPMMLINTERVAL).get(0).clone();
        final BlockStmt toReturn =
                methodDeclaration.getBody().orElseThrow(() -> new KiePMMLException(String.format(MISSING_BODY_TEMPLATE, methodDeclaration)));
        final VariableDeclarator variableDeclarator =
                getVariableDeclarator(toReturn, INTERVAL).orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_IN_BODY, INTERVAL, toReturn)));
        variableDeclarator.setName(variableName);
        final ObjectCreationExpr objectCreationExpr = variableDeclarator.getInitializer()
                .orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_INITIALIZER_TEMPLATE,
                                                                      INTERVAL, toReturn)))
                .asObjectCreationExpr();
        final Expression leftMarginExpr = getExpressionForObject(interval.getLeftMargin());
        final Expression rightMarginExpr = getExpressionForObject(interval.getRightMargin());
        final CLOSURE closure = CLOSURE.byName(interval.getClosure().value());
        final NameExpr closureExpr = new NameExpr(CLOSURE.class.getName() + "." + closure.name());
        objectCreationExpr.getArguments().set(0, leftMarginExpr);
        objectCreationExpr.getArguments().set(1, rightMarginExpr);
        objectCreationExpr.getArguments().set(2, closureExpr);
        return toReturn;
    }

}
