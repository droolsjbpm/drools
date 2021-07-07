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
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.DiscretizeBin;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;

import static org.kie.pmml.commons.Constants.MISSING_BODY_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_INITIALIZER_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_IN_BODY;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLDiscretizeBinFactory.getDiscretizeBinVariableDeclaration;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getExpressionForObject;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getVariableDeclarator;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.MAIN_CLASS_NOT_FOUND;

/**
 * Class meant to provide <i>helper</i> method to retrieve <code>KiePMMLDiscretize</code> code-generators
 * out of <code>Discretize</code>s
 */
public class KiePMMLDiscretizeFactory {

    private KiePMMLDiscretizeFactory() {
        // Avoid instantiation
    }

    static final String KIE_PMML_DISCRETIZE_TEMPLATE_JAVA = "KiePMMLDiscretizeTemplate.tmpl";
    static final String KIE_PMML_DISCRETIZE_TEMPLATE = "KiePMMLDiscretizeTemplate";
    static final String GETKIEPMMLDISCRETIZE = "getKiePMMLDiscretize";
    static final String DISCRETIZE = "discretize";
    static final ClassOrInterfaceDeclaration DISCRETIZE_TEMPLATE;


    static {
        CompilationUnit cloneCU = JavaParserUtils.getFromFileName(KIE_PMML_DISCRETIZE_TEMPLATE_JAVA);
        DISCRETIZE_TEMPLATE = cloneCU.getClassByName(KIE_PMML_DISCRETIZE_TEMPLATE)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + KIE_PMML_DISCRETIZE_TEMPLATE));
        DISCRETIZE_TEMPLATE.getMethodsByName(GETKIEPMMLDISCRETIZE).get(0).clone();
    }

    static BlockStmt getDiscretizeVariableDeclaration(final String variableName, final Discretize discretize) {
        final MethodDeclaration methodDeclaration = DISCRETIZE_TEMPLATE.getMethodsByName(GETKIEPMMLDISCRETIZE).get(0).clone();
        final BlockStmt discretizeBody = methodDeclaration.getBody().orElseThrow(() -> new KiePMMLException(String.format(MISSING_BODY_TEMPLATE, methodDeclaration)));
        final VariableDeclarator variableDeclarator = getVariableDeclarator(discretizeBody, DISCRETIZE) .orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_IN_BODY, DISCRETIZE, discretizeBody)));
        variableDeclarator.setName(variableName);
        final BlockStmt toReturn = new BlockStmt();
        int counter = 0;
        final NodeList<Expression> arguments = new NodeList<>();
        for (DiscretizeBin discretizeBin : discretize.getDiscretizeBins()) {
            String nestedVariableName = String.format("%s_%s", variableName, counter);
            arguments.add(new NameExpr(nestedVariableName));
            BlockStmt toAdd = getDiscretizeBinVariableDeclaration(nestedVariableName, discretizeBin);
            toAdd.getStatements().forEach(toReturn::addStatement);
            counter ++;
        }
        final ObjectCreationExpr objectCreationExpr = variableDeclarator.getInitializer()
                .orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_INITIALIZER_TEMPLATE,
                                                                      DISCRETIZE, toReturn)))
                .asObjectCreationExpr();
        final Expression nameExpr = new StringLiteralExpr(discretize.getField().getValue());
        final Expression mapMissingToExpr = getExpressionForObject(discretize.getMapMissingTo());
        final Expression defaultValueExpr = getExpressionForObject(discretize.getDefaultValue());

        final DATA_TYPE dataType = DATA_TYPE.byName(discretize.getDataType().value());
        final NameExpr dataTypeExpr = new NameExpr(DATA_TYPE.class.getName() + "." + dataType.name());
        objectCreationExpr.getArguments().set(0, nameExpr);
        objectCreationExpr.getArguments().get(2).asMethodCallExpr().setArguments(arguments);
        objectCreationExpr.getArguments().set(3, mapMissingToExpr);
        objectCreationExpr.getArguments().set(4, defaultValueExpr);
        objectCreationExpr.getArguments().set(5, dataTypeExpr);
        discretizeBody.getStatements().forEach(toReturn::addStatement);
        return toReturn;
    }
}
