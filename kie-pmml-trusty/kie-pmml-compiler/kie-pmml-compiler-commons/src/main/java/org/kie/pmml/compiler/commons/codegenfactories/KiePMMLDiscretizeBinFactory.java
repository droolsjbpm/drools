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
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.dmg.pmml.DiscretizeBin;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;

import static org.kie.pmml.commons.Constants.MISSING_BODY_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_INITIALIZER_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_IN_BODY;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLIntervalFactory.getIntervalVariableDeclaration;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getExpressionForObject;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getVariableDeclarator;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.MAIN_CLASS_NOT_FOUND;

/**
 * Class meant to provide <i>helper</i> method to retrieve <code>KiePMMLDiscretizeBin</code> code-generators
 * out of <code>DiscretizeBin</code>s
 */
public class KiePMMLDiscretizeBinFactory {

    static final String KIE_PMML_DISCRETIZE_BIN_TEMPLATE_JAVA = "KiePMMLDiscretizeBinTemplate.tmpl";
    static final String KIE_PMML_DISCRETIZE_BIN_TEMPLATE = "KiePMMLDiscretizeBinTemplate";
    static final String GETKIEPMMLDISCRETIZE_BIN = "getKiePMMLDiscretizeBin";
    static final String DISCRETIZE_BIN = "discretizeBin";
    static final ClassOrInterfaceDeclaration DISCRETIZE_BIN_TEMPLATE;

    static {
        CompilationUnit cloneCU = JavaParserUtils.getFromFileName(KIE_PMML_DISCRETIZE_BIN_TEMPLATE_JAVA);
        DISCRETIZE_BIN_TEMPLATE = cloneCU.getClassByName(KIE_PMML_DISCRETIZE_BIN_TEMPLATE)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + KIE_PMML_DISCRETIZE_BIN_TEMPLATE));
        DISCRETIZE_BIN_TEMPLATE.getMethodsByName(GETKIEPMMLDISCRETIZE_BIN).get(0).clone();
    }

    private KiePMMLDiscretizeBinFactory() {
        // Avoid instantiation
    }

    static BlockStmt getDiscretizeBinVariableDeclaration(final String variableName,
                                                        final DiscretizeBin discretizeBin) {
        final MethodDeclaration methodDeclaration =
                DISCRETIZE_BIN_TEMPLATE.getMethodsByName(GETKIEPMMLDISCRETIZE_BIN).get(0).clone();
        final BlockStmt discretizeBinBody =
                methodDeclaration.getBody().orElseThrow(() -> new KiePMMLException(String.format(MISSING_BODY_TEMPLATE, methodDeclaration)));
        final VariableDeclarator variableDeclarator =
                getVariableDeclarator(discretizeBinBody, DISCRETIZE_BIN).orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_IN_BODY, DISCRETIZE_BIN, discretizeBinBody)));
        variableDeclarator.setName(variableName);
        final BlockStmt toReturn = new BlockStmt();
        String nestedVariableName = String.format("%s_Interval", variableName);

        BlockStmt toAdd = getIntervalVariableDeclaration(nestedVariableName, discretizeBin.getInterval());
        toAdd.getStatements().forEach(toReturn::addStatement);


        final ObjectCreationExpr objectCreationExpr = variableDeclarator.getInitializer()
                .orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_INITIALIZER_TEMPLATE,
                                                                      DISCRETIZE_BIN, discretizeBinBody)))
                .asObjectCreationExpr();

        final Expression nameExpr = new StringLiteralExpr(variableName);
        final Expression binValueExpr = getExpressionForObject(discretizeBin.getBinValue());
        final NameExpr intervalExpr = new NameExpr(nestedVariableName);
        objectCreationExpr.getArguments().set(0, nameExpr);
        objectCreationExpr.getArguments().set(2, binValueExpr);
        objectCreationExpr.getArguments().set(3, intervalExpr);
        discretizeBinBody.getStatements().forEach(toReturn::addStatement);
        return toReturn;
    }

}
