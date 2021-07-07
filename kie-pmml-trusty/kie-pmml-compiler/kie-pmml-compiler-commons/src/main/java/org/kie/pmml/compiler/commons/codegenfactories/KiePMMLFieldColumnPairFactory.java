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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.dmg.pmml.FieldColumnPair;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;
import org.w3c.dom.Element;

import static org.kie.pmml.commons.Constants.MISSING_BODY_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_INITIALIZER_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_IN_BODY;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getExpressionForObject;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getVariableDeclarator;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.MAIN_CLASS_NOT_FOUND;

/**
 * Class meant to provide <i>helper</i> method to retrieve <code>KiePMMLFieldColumnPair</code> code-generators
 * out of <code>FieldColumnPair</code>s
 */
public class KiePMMLFieldColumnPairFactory {

    private KiePMMLFieldColumnPairFactory() {
        // Avoid instantiation
    }

    static final String KIE_PMML_FIELDCOLUMNPAIR_TEMPLATE_JAVA = "KiePMMLFieldColumnPairTemplate.tmpl";
    static final String KIE_PMML_FIELDCOLUMNPAIR_TEMPLATE = "KiePMMLFieldColumnPairTemplate";
    static final String GETKIEPMMLFIELDCOLUMNPAIR = "getKiePMMLFieldColumnPair";
    static final String FIELDCOLUMNPAIR = "fieldColumnPair";
    static final ClassOrInterfaceDeclaration FIELDCOLUMNPAIR_TEMPLATE;


    static {
        CompilationUnit cloneCU = JavaParserUtils.getFromFileName(KIE_PMML_FIELDCOLUMNPAIR_TEMPLATE_JAVA);
        FIELDCOLUMNPAIR_TEMPLATE = cloneCU.getClassByName(KIE_PMML_FIELDCOLUMNPAIR_TEMPLATE)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + KIE_PMML_FIELDCOLUMNPAIR_TEMPLATE));
        FIELDCOLUMNPAIR_TEMPLATE.getMethodsByName(GETKIEPMMLFIELDCOLUMNPAIR).get(0).clone();
    }

    static BlockStmt getFieldColumnPairVariableDeclaration(final String variableName, final FieldColumnPair fieldColumnPair) {
        final MethodDeclaration methodDeclaration = FIELDCOLUMNPAIR_TEMPLATE.getMethodsByName(GETKIEPMMLFIELDCOLUMNPAIR).get(0).clone();
        final BlockStmt toReturn = methodDeclaration.getBody().orElseThrow(() -> new KiePMMLException(String.format(MISSING_BODY_TEMPLATE, methodDeclaration)));


        final VariableDeclarator variableDeclarator = getVariableDeclarator(toReturn, FIELDCOLUMNPAIR) .orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_IN_BODY, FIELDCOLUMNPAIR, toReturn)));
        variableDeclarator.setName(variableName);
        final ObjectCreationExpr objectCreationExpr = variableDeclarator.getInitializer()
                .orElseThrow(() -> new KiePMMLException(String.format(MISSING_VARIABLE_INITIALIZER_TEMPLATE, FIELDCOLUMNPAIR, toReturn)))
        .asObjectCreationExpr();
        objectCreationExpr.getArguments().set(0, new StringLiteralExpr(fieldColumnPair.getField().getValue()));
        objectCreationExpr.getArguments().set(2, new StringLiteralExpr(fieldColumnPair.getColumn()));
        return toReturn;
    }


}
