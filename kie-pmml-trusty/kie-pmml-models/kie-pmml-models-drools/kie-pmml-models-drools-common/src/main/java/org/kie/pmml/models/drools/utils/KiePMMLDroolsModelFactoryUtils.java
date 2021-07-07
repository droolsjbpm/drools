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
package org.kie.pmml.models.drools.utils;

import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.Model;
import org.kie.pmml.api.enums.MINING_FUNCTION;
import org.kie.pmml.api.enums.PMML_MODEL;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.api.exceptions.KiePMMLInternalException;
import org.kie.pmml.compiler.commons.utils.CommonCodegenUtils;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;
import org.kie.pmml.models.drools.tuples.KiePMMLOriginalTypeGeneratedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.commons.Constants.MISSING_DEFAULT_CONSTRUCTOR;
import static org.kie.pmml.commons.utils.KiePMMLModelUtils.getSanitizedClassName;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.MAIN_CLASS_NOT_FOUND;
import static org.kie.pmml.compiler.commons.utils.ModelUtils.getTargetFieldName;

/**
 * Utility class to provide common methods for KiePMMLDroolsModel-specific <b>factories</b>
 */
public class KiePMMLDroolsModelFactoryUtils {

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLDroolsModelFactoryUtils.class.getName());
    static final String GETKMODULEPACKAGENAME_METHOD = "getKModulePackageName";

    private KiePMMLDroolsModelFactoryUtils() {
        // Avoid instantiation
    }

    /**
     * @param dataDictionary
     * @param model
     * @param fieldTypeMap
     * @param packageName
     * @param javaTemplate the name of the <b>file</b> to be used as template source
     * @param modelClassName the name of the class used in the provided template
     * @return
     */
    public static CompilationUnit getKiePMMLModelCompilationUnit(final DataDictionary dataDictionary,
                                                                 final Model model,
                                                                 final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap,
                                                                 final String packageName,
                                                                 final String javaTemplate,
                                                                 final String modelClassName) {
        logger.trace("getKiePMMLModelCompilationUnit {} {} {}", dataDictionary, model, packageName);
        String className = getSanitizedClassName(model.getModelName());
        CompilationUnit cloneCU = JavaParserUtils.getKiePMMLModelCompilationUnit(className, packageName, javaTemplate, modelClassName);
        ClassOrInterfaceDeclaration modelTemplate = cloneCU.getClassByName(className)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + className));
        MINING_FUNCTION miningFunction = MINING_FUNCTION.byName(model.getMiningFunction().value());
        final ConstructorDeclaration constructorDeclaration = modelTemplate.getDefaultConstructor().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_DEFAULT_CONSTRUCTOR, modelTemplate.getName())));
        String targetField = getTargetFieldName(dataDictionary, model).orElse(null);
        setConstructor(model, constructorDeclaration, modelTemplate.getName(), targetField, miningFunction);
        addFieldTypeMapPopulation(constructorDeclaration.getBody(), fieldTypeMap);
        final MethodDeclaration getKModulePackageNameMethod = modelTemplate.getMethodsByName(GETKMODULEPACKAGENAME_METHOD).get(0);
        populateGetKModulePackageName(getKModulePackageNameMethod, packageName);
        return cloneCU;
    }

    /**
     * Define the <b>targetField</b>, the <b>miningFunction</b> and the <b>pmmlMODEL</b> inside the constructor
     * @param model
     * @param constructorDeclaration
     * @param tableName
     * @param targetField
     * @param miningFunction
     */
    static void setConstructor(final Model model,
                               final ConstructorDeclaration constructorDeclaration,
                               final SimpleName tableName,
                               final String targetField,
                               final MINING_FUNCTION miningFunction) {
        constructorDeclaration.setName(tableName);
        final BlockStmt body = constructorDeclaration.getBody();
        CommonCodegenUtils.setAssignExpressionValue(body, "targetField", new StringLiteralExpr(targetField));
        CommonCodegenUtils.setAssignExpressionValue(body, "miningFunction", new NameExpr(miningFunction.getClass().getName() + "." + miningFunction.name()));
        PMML_MODEL pmmlModel = PMML_MODEL.byName(model.getClass().getSimpleName());
        CommonCodegenUtils.setAssignExpressionValue(body, "pmmlMODEL", new NameExpr(pmmlModel.getClass().getName() + "." + pmmlModel.name()));
    }

    /**
     * Populate the <b>fieldTypeMap</b> <code>Map&lt;String, KiePMMLOriginalTypeGeneratedType&gt;</code>
     * @param body
     * @param fieldTypeMap
     */
    static void addFieldTypeMapPopulation(BlockStmt body, Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap) {
        for (Map.Entry<String, KiePMMLOriginalTypeGeneratedType> entry : fieldTypeMap.entrySet()) {
            KiePMMLOriginalTypeGeneratedType kiePMMLOriginalTypeGeneratedType = entry.getValue();
            NodeList<Expression> expressions = NodeList.nodeList(new StringLiteralExpr(kiePMMLOriginalTypeGeneratedType.getOriginalType()), new StringLiteralExpr(kiePMMLOriginalTypeGeneratedType.getGeneratedType()));
            ObjectCreationExpr objectCreationExpr = new ObjectCreationExpr();
            objectCreationExpr.setType(KiePMMLOriginalTypeGeneratedType.class.getName());
            objectCreationExpr.setArguments(expressions);
            expressions = NodeList.nodeList(new StringLiteralExpr(entry.getKey()), objectCreationExpr);
            body.addStatement(new MethodCallExpr(new NameExpr("fieldTypeMap"), "put", expressions));
        }
    }

    /**
     * Populate the <b>getKModulePackageName</b> method override
     */
    static void populateGetKModulePackageName(final MethodDeclaration getKModulePackageNameMethod, final String packageName) {
        final BlockStmt blockStmt = getKModulePackageNameMethod.getBody()
                .orElseThrow(() -> new KiePMMLException("Missing body inside " + getKModulePackageNameMethod));
        blockStmt.getStatements().forEach(statement -> {
            if (statement instanceof ReturnStmt) {
                ReturnStmt returnStmt = (ReturnStmt)statement;
                returnStmt.setExpression(new StringLiteralExpr(packageName));
            }
        });
    }
}
