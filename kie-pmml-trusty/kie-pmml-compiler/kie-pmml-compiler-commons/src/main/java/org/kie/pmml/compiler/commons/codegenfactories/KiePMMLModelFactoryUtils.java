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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.utils.Pair;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.TransformationDictionary;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.FIELD_USAGE_TYPE;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.api.enums.RESULT_FEATURE;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.api.exceptions.KiePMMLInternalException;
import org.kie.pmml.api.models.Interval;
import org.kie.pmml.api.models.MiningField;
import org.kie.pmml.api.models.OutputField;
import org.kie.pmml.commons.model.KiePMMLOutputField;
import org.kie.pmml.commons.transformations.KiePMMLLocalTransformations;
import org.kie.pmml.commons.transformations.KiePMMLTransformationDictionary;
import org.kie.pmml.compiler.commons.utils.CommonCodegenUtils;

import static org.kie.pmml.commons.Constants.MISSING_CONSTRUCTOR_IN_BODY;
import static org.kie.pmml.commons.Constants.MISSING_DEFAULT_CONSTRUCTOR;
import static org.kie.pmml.commons.utils.KiePMMLModelUtils.getSanitizedClassName;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLLocalTransformationsFactory.LOCAL_TRANSFORMATIONS;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLOutputFieldFactory.getOutputFieldVariableDeclaration;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLTransformationDictionaryFactory.TRANSFORMATION_DICTIONARY;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.addListPopulation;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.addMapPopulationExpressions;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getReturnStmt;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getTypedClassOrInterfaceType;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.literalExprFrom;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.setAssignExpressionValue;

/**
 * Class to provide shared, helper methods to be invoked by model-specific
 * <b>factories</b> (e.g. KiePMMLTreeModelFactory, KiePMMLScorecardModelFactory, KiePMMLRegressionModelFactory)
 */
public class KiePMMLModelFactoryUtils {

    public static final String GET_CREATED_KIEPMMLOUTPUTFIELDS = "getCreatedKiePMMLOutputFields";

    private KiePMMLModelFactoryUtils() {
        // Avoid instantiation
    }

    /**
     * Set the <b>name</b> parameter on <b>super</b> invocation
     * @param generatedClassName
     * @param constructorDeclaration
     * @param name
     */
    public static void setConstructorSuperNameInvocation(final String generatedClassName,
                                                         final ConstructorDeclaration constructorDeclaration,
                                                         final String name) {
        constructorDeclaration.setName(generatedClassName);
        final BlockStmt body = constructorDeclaration.getBody();
        final ExplicitConstructorInvocationStmt superStatement =
                CommonCodegenUtils.getExplicitConstructorInvocationStmt(body)
                .orElseThrow(() -> new KiePMMLException(String.format(MISSING_CONSTRUCTOR_IN_BODY, body)));
        CommonCodegenUtils.setExplicitConstructorInvocationStmtArgument(superStatement, "name", String.format("\"%s\"",
                                                                                                              name));
    }

    /**
     * Set the <b>name</b> parameter on <b>super</b> invocation and populate the <b>miningFields/outputFields</b>
     * @param generatedClassName
     * @param constructorDeclaration
     * @param name
     */
    public static void setKiePMMLModelConstructor(final String generatedClassName,
                                                  final ConstructorDeclaration constructorDeclaration,
                                                  final String name,
                                                  final List<MiningField> miningFields,
                                                  final List<OutputField> outputFields,
                                                  final Map<String, Pair<DATA_TYPE, String>> missingValueReplacements) {
        setConstructorSuperNameInvocation(generatedClassName, constructorDeclaration, name);
        final BlockStmt body = constructorDeclaration.getBody();
        final List<ObjectCreationExpr> miningFieldsObjectCreations = getMiningFieldsObjectCreations(miningFields);
        addListPopulation(miningFieldsObjectCreations, body, "miningFields");
        final List<ObjectCreationExpr> outputFieldsObjectCreations = getOutputFieldsObjectCreations(outputFields);
        addListPopulation(outputFieldsObjectCreations, body, "outputFields");

        Map<String, Expression> missingValueReplacementsExpr = missingValueReplacements.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> literalExprFrom(entry.getValue().a, entry.getValue().b)
        ));
        addMapPopulationExpressions(missingValueReplacementsExpr, body, "missingValueReplacementMap");
    }

    public static void addGetCreatedKiePMMLOutputFieldsMethod(final ClassOrInterfaceDeclaration modelTemplate, final List<org.dmg.pmml.OutputField> outputFields) {
        final MethodDeclaration methodDeclaration = modelTemplate.addMethod(GET_CREATED_KIEPMMLOUTPUTFIELDS,
                                                                            Modifier.Keyword.PRIVATE);
        final ClassOrInterfaceType returnedType =
                getTypedClassOrInterfaceType(List.class.getSimpleName(),
                                             Collections.singletonList(KiePMMLOutputField.class.getSimpleName()));
        methodDeclaration.setType(returnedType);
        BlockStmt body = new BlockStmt();
        methodDeclaration.setBody(body);
        NodeList<Expression> arguments = new NodeList<>();
        for (org.dmg.pmml.OutputField outputField : outputFields) {
            String outputFieldVariableName = getSanitizedClassName(outputField.getName().getValue()).toLowerCase();
            BlockStmt toAdd = getOutputFieldVariableDeclaration(outputFieldVariableName, outputField);
            toAdd.getStatements().forEach(body::addStatement);
            arguments.add(new NameExpr(outputFieldVariableName));
        }
        MethodCallExpr methodCallExpr = new MethodCallExpr();
        methodCallExpr.setScope(new NameExpr(Arrays.class.getSimpleName()));
        methodCallExpr.setName("asList");
        methodCallExpr.setArguments(arguments);
        ReturnStmt returnStmt = new ReturnStmt();
        returnStmt.setExpression(methodCallExpr);
        body.addStatement(returnStmt);
    }

    /**
     * Add <b>common</b> and <b>local</b> transformations management inside the given
     * <code>ClassOrInterfaceDeclaration</code>
     * @param toPopulate
     * @param transformationDictionary
     * @param localTransformations
     */
    public static void addTransformationsInClassOrInterfaceDeclaration(final ClassOrInterfaceDeclaration toPopulate,
                                                                       final TransformationDictionary transformationDictionary,
                                                                       final LocalTransformations localTransformations) {
        String createTransformationDictionary = null;
        if (transformationDictionary != null) {
            BlockStmt createTransformationDictionaryBody = KiePMMLTransformationDictionaryFactory.getKiePMMLTransformationDictionaryVariableDeclaration(transformationDictionary);
            createTransformationDictionaryBody.addStatement(getReturnStmt(TRANSFORMATION_DICTIONARY));
            createTransformationDictionary = "createTransformationDictionary";
            MethodDeclaration createTransformationDictionaryMethod = toPopulate.addMethod(createTransformationDictionary, Modifier.Keyword.PRIVATE);
            createTransformationDictionaryMethod.setType(KiePMMLTransformationDictionary.class.getName());
            createTransformationDictionaryMethod.setBody(createTransformationDictionaryBody);
        }
        String createLocalTransformations = null;
        if (localTransformations != null) {
            BlockStmt createLocalTransformationsBody = KiePMMLLocalTransformationsFactory.getKiePMMLLocalTransformationsVariableDeclaration(localTransformations);
            createLocalTransformationsBody.addStatement(getReturnStmt(LOCAL_TRANSFORMATIONS));
            createLocalTransformations = "createLocalTransformations";
            MethodDeclaration createLocalTransformationsMethod = toPopulate.addMethod(createLocalTransformations, Modifier.Keyword.PRIVATE);
            createLocalTransformationsMethod.setType(KiePMMLLocalTransformations.class.getName());
            createLocalTransformationsMethod.setBody(createLocalTransformationsBody);
        }
        final ConstructorDeclaration constructorDeclaration =
                toPopulate.getDefaultConstructor().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_DEFAULT_CONSTRUCTOR, toPopulate.getName())));
        populateTransformationsInConstructor(constructorDeclaration, createTransformationDictionary, createLocalTransformations);
    }

    /**
     * Create a <code>List&lt;ObjectCreationExpr&gt;</code> for the given <code>List&lt;MiningField&gt;</code>
     * @param miningFields
     * @return
     */
    static List<ObjectCreationExpr> getMiningFieldsObjectCreations(final List<MiningField> miningFields) {
        return miningFields.stream()
                .map(miningField -> {
                    ObjectCreationExpr toReturn = new ObjectCreationExpr();
                    toReturn.setType(MiningField.class.getCanonicalName());
                    Expression name = miningField.getName() != null ?
                            new StringLiteralExpr(miningField.getName())
                            : new NullLiteralExpr();
                    FIELD_USAGE_TYPE fieldUsageType = miningField.getUsageType();
                    Expression usageType = fieldUsageType != null ?
                            new NameExpr(fieldUsageType.getClass().getName() + "." + fieldUsageType.name())
                            : new NullLiteralExpr();
                    OP_TYPE oPT = miningField.getOpType();
                    Expression opType = oPT != null ?
                            new NameExpr(oPT.getClass().getName() + "." + oPT.name())
                            : new NullLiteralExpr();
                    DATA_TYPE dtT = miningField.getDataType();
                    Expression dataType = dtT != null ?
                            new NameExpr(dtT.getClass().getName() + "." + dtT.name())
                            : new NullLiteralExpr();
                    Expression missingValueReplacement = miningField.getMissingValueReplacement() != null ?
                            new StringLiteralExpr(miningField.getMissingValueReplacement())
                            : new NullLiteralExpr();
                    Expression allowedValues = miningField.getAllowedValues() != null ?
                            CommonCodegenUtils.createArraysAsListFromList(miningField.getAllowedValues()).getExpression()
                            : new NullLiteralExpr();
                    Expression intervals = miningField.getIntervals() != null ?
                            createIntervalsExpression(miningField.getIntervals())
                            : new NullLiteralExpr();
                    toReturn.setArguments(NodeList.nodeList(name, usageType, opType, dataType, missingValueReplacement, allowedValues, intervals));
                    return toReturn;
                })
                .collect(Collectors.toList());
    }

    static Expression createIntervalsExpression(List<Interval> intervals) {
        ExpressionStmt arraysAsListStmt = CommonCodegenUtils.createArraysAsListExpression();
        MethodCallExpr arraysCallExpression = arraysAsListStmt.getExpression().asMethodCallExpr();
        NodeList<Expression> arguments = new NodeList<>();
        intervals.forEach(value -> arguments.add(getObjectCreationExprFromInterval(value)));
        arraysCallExpression.setArguments(arguments);
        arraysAsListStmt.setExpression(arraysCallExpression);
        return arraysAsListStmt.getExpression();
    }

    static ObjectCreationExpr getObjectCreationExprFromInterval(Interval source) {
        ObjectCreationExpr toReturn = new ObjectCreationExpr();
        toReturn.setType(Interval.class.getCanonicalName());
        NodeList<Expression> arguments = new NodeList<>();
        if (source.getLeftMargin() != null) {
            arguments.add(new NameExpr(source.getLeftMargin().toString()));
        } else {
            arguments.add(new NullLiteralExpr());
        }
        if (source.getRightMargin() != null) {
            arguments.add(new NameExpr(source.getRightMargin().toString()));
        } else {
            arguments.add(new NullLiteralExpr());
        }
        toReturn.setArguments(arguments);
        return toReturn;
    }



    /**
     * Create a <code>List&lt;ObjectCreationExpr&gt;</code> for the given <code>List&lt;OutputField&gt;</code>
     * @param outputFields
     * @return
     */
    static List<ObjectCreationExpr> getOutputFieldsObjectCreations(final List<OutputField> outputFields) {
        return outputFields.stream()
                .map(outputField -> {
                    ObjectCreationExpr toReturn = new ObjectCreationExpr();
                    toReturn.setType(OutputField.class.getCanonicalName());
                    Expression name = outputField.getName() != null ?
                            new StringLiteralExpr(outputField.getName())
                            : new NullLiteralExpr();
                    OP_TYPE oPT = outputField.getOpType();
                    Expression opType = oPT != null ?
                            new NameExpr(oPT.getClass().getName() + "." + oPT.name())
                            : new NullLiteralExpr();
                    DATA_TYPE datT = outputField.getDataType();
                    Expression dataType = datT != null ?
                            new NameExpr(datT.getClass().getName() + "." + datT.name())
                            : new NullLiteralExpr();
                    Expression targetField = outputField.getTargetField() != null ?
                            new StringLiteralExpr(outputField.getTargetField())
                            : new NullLiteralExpr();
                    RESULT_FEATURE rsltF = outputField.getResultFeature();
                    Expression resultFeature = rsltF != null ?
                            new NameExpr(rsltF.getClass().getName() + "." + rsltF.name())
                            : new NullLiteralExpr();
                    Expression allowedValues = outputField.getAllowedValues() != null ?
                            CommonCodegenUtils.createArraysAsListFromList(outputField.getAllowedValues()).getExpression()
                            : new NullLiteralExpr();
                    toReturn.setArguments(NodeList.nodeList(name, opType, dataType, targetField, resultFeature, allowedValues));
                    return toReturn;
                })
                .collect(Collectors.toList());
    }


    /**
     * Populating the <b>transformationDictionary</b> and <b>localTransformations</b> variables inside the constructor
     * @param constructorDeclaration
     * @param createTransformationDictionary
     * @param createLocalTransformations
     */
    static void populateTransformationsInConstructor(final ConstructorDeclaration constructorDeclaration,
                                                     final String createTransformationDictionary, final String createLocalTransformations) {
        Expression createTransformationDictionaryInitializer = createTransformationDictionary != null ?
                new MethodCallExpr(new NameExpr("this"), createTransformationDictionary, NodeList.nodeList()) : new NullLiteralExpr();
        setAssignExpressionValue(constructorDeclaration.getBody(), TRANSFORMATION_DICTIONARY, createTransformationDictionaryInitializer);
        Expression createLocalTransformationsInitializer =  createLocalTransformations != null ?
                new MethodCallExpr(new NameExpr("this"), createLocalTransformations, NodeList.nodeList()) : new NullLiteralExpr();
        setAssignExpressionValue(constructorDeclaration.getBody(), LOCAL_TRANSFORMATIONS, createLocalTransformationsInitializer);
    }

}
