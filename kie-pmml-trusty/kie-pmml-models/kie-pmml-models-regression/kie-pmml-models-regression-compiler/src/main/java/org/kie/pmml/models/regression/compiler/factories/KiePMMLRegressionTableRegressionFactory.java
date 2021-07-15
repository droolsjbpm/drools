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
package org.kie.pmml.models.regression.compiler.factories;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.dmg.pmml.regression.NumericPredictor;
import org.dmg.pmml.regression.PredictorTerm;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.api.exceptions.KiePMMLInternalException;
import org.kie.pmml.compiler.commons.utils.CommonCodegenUtils;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;
import org.kie.pmml.models.regression.model.tuples.KiePMMLTableSourceCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;
import static org.kie.pmml.commons.Constants.MISSING_BODY_TEMPLATE;
import static org.kie.pmml.commons.Constants.MISSING_DEFAULT_CONSTRUCTOR;
import static org.kie.pmml.commons.Constants.MISSING_VARIABLE_IN_BODY;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.addMapPopulation;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.addMethod;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.getVariableDeclarator;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.getFromFileName;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.getFullClassName;

public class KiePMMLRegressionTableRegressionFactory {

    public static final String KIE_PMML_REGRESSION_TABLE_REGRESSION_TEMPLATE_JAVA =
            "KiePMMLRegressionTableRegressionTemplate.tmpl";
    public static final String KIE_PMML_REGRESSION_TABLE_REGRESSION_TEMPLATE =
            "KiePMMLRegressionTableRegressionTemplate";
    private static final Logger logger =
            LoggerFactory.getLogger(KiePMMLRegressionTableRegressionFactory.class.getName());
    static final String MAIN_CLASS_NOT_FOUND = "Main class not found";
    static final String KIE_PMML_EVALUATE_METHOD_TEMPLATE_JAVA = "KiePMMLEvaluateMethodTemplate.tmpl";
    static final String KIE_PMML_EVALUATE_METHOD_TEMPLATE = "KiePMMLEvaluateMethodTemplate";
    private static final String KIE_PMML_UPDATE_RESULT_METHOD_TEMPLATE_JAVA = "KiePMMLUpdateResultMethodTemplate.tmpl";
    private static final String KIE_PMML_UPDATE_RESULT_METHOD_TEMPLATE = "KiePMMLUpdateResultMethodTemplate";
    private static final String COEFFICIENT = "coefficient";
    private static final String EXPONENT = "exponent";
    private static AtomicInteger classArity = new AtomicInteger(0);
    private static AtomicInteger predictorsArity = new AtomicInteger(0);
    private static CompilationUnit templateEvaluate;
    private static CompilationUnit cloneEvaluate;

    private KiePMMLRegressionTableRegressionFactory() {
        // Avoid instantiation
    }

    /**
     *
     * @param regressionTables
     * @param normalizationMethod
     * @param outputFields
     * @param targetField
     * @param packageName
     * @return Explicitly returning a <code>LinkedHashMap</code> because insertion order matters
     */
    public static LinkedHashMap<String, KiePMMLTableSourceCategory> getRegressionTables(final List<RegressionTable> regressionTables,
                                                                                        final RegressionModel.NormalizationMethod normalizationMethod,
                                                                                        final List<OutputField> outputFields,
                                                                                        final String targetField,
                                                                                        final String packageName) {
        logger.trace("getRegressionTables {}", regressionTables);
        LinkedHashMap<String, KiePMMLTableSourceCategory> toReturn = new LinkedHashMap<>();
        for (RegressionTable regressionTable : regressionTables) {
            final Map.Entry<String, String> regressionTableEntry = getRegressionTable(regressionTable,
                                                                                      normalizationMethod,
                                                                                      outputFields,
                                                                                      targetField,
                                                                                      packageName);
            String targetCategory = regressionTable.getTargetCategory() != null ?
                    regressionTable.getTargetCategory().toString() : "";
            toReturn.put(regressionTableEntry.getKey(),
                         new KiePMMLTableSourceCategory(regressionTableEntry.getValue(), targetCategory));
        }
        return toReturn;
    }

    public static Map.Entry<String, String> getRegressionTable(final RegressionTable regressionTable,
                                                               final RegressionModel.NormalizationMethod normalizationMethod,
                                                               final List<OutputField> outputFields,
                                                               final String targetField,
                                                               final String packageName) {
        logger.trace("getRegressionTable {}", regressionTable);
        String className = "KiePMMLRegressionTableRegression" + classArity.addAndGet(1);
        CompilationUnit cloneCU = JavaParserUtils.getKiePMMLModelCompilationUnit(className, packageName,
                                                                                 KIE_PMML_REGRESSION_TABLE_REGRESSION_TEMPLATE_JAVA, KIE_PMML_REGRESSION_TABLE_REGRESSION_TEMPLATE);
        ClassOrInterfaceDeclaration tableTemplate = cloneCU.getClassByName(className)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + className));
        final ConstructorDeclaration constructorDeclaration =
                tableTemplate.getDefaultConstructor().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_DEFAULT_CONSTRUCTOR, tableTemplate.getName())));
        setConstructor(regressionTable, constructorDeclaration, tableTemplate.getName(), targetField);
        final Map<String, MethodDeclaration> numericPredictorsMap =
                addNumericPredictors(regressionTable.getNumericPredictors(), tableTemplate);
        final Map<String, MethodDeclaration> categoricalPredictorsMap =
                addCategoricalPredictors(regressionTable.getCategoricalPredictors(), tableTemplate);
        final Map<String, MethodDeclaration> predictorTermsMap =
                addPredictorTerms(regressionTable.getPredictorTerms(), tableTemplate);
        final BlockStmt body = constructorDeclaration.getBody();
        addMapPopulation(numericPredictorsMap, body, "numericFunctionMap");
        addMapPopulation(categoricalPredictorsMap, body, "categoricalFunctionMap");
        addMapPopulation(predictorTermsMap, body, "predictorTermsFunctionMap");
        populateGetTargetCategory(tableTemplate, regressionTable.getTargetCategory());
        populateUpdateResult(tableTemplate, normalizationMethod);
        return new AbstractMap.SimpleEntry<>(getFullClassName(cloneCU), cloneCU.toString());
    }

    /**
     * Set the <b>intercept</b> and <b>targetField</b> values inside the constructor
     * @param regressionTable
     * @param constructorDeclaration
     * @param tableName
     * @param targetField
     */
    static void setConstructor(final RegressionTable regressionTable,
                               final ConstructorDeclaration constructorDeclaration,
                               final SimpleName tableName,
                               final String targetField) {
        constructorDeclaration.setName(tableName);
        final BlockStmt body = constructorDeclaration.getBody();
        CommonCodegenUtils.setAssignExpressionValue(body, "intercept",
                                                    new DoubleLiteralExpr(String.valueOf(regressionTable.getIntercept().doubleValue())));
        CommonCodegenUtils.setAssignExpressionValue(body, "targetField", new StringLiteralExpr(targetField));
    }

    /**
     * Add entries <b>output field/output value</b> inside <b>populateOutputFieldsMapWithResult</b> method
     * @param body
     * @param outputFields
     */
    static void populateOutputFieldsMapWithResult(final BlockStmt body,
                                                  final List<OutputField> outputFields) {
        outputFields.stream()
                .filter(outputField -> ResultFeature.PREDICTED_VALUE.equals(outputField.getResultFeature()))
                .forEach(outputField -> {
                    StringLiteralExpr key = new StringLiteralExpr(outputField.getName().getValue());
                    Expression value = new NameExpr("result");
                    NodeList<Expression> expressions = NodeList.nodeList(key, value);
                    body.addStatement(new MethodCallExpr(new NameExpr("outputFieldsMap"), "put", expressions));
                });
    }

    /**
     * Add <b>NumericPredictor</b>s <code>MethodDeclaration</code> to the class
     * @param numericPredictors
     * @param tableTemplate
     * @return
     */
    static Map<String, MethodDeclaration> addNumericPredictors(final List<NumericPredictor> numericPredictors
            , final ClassOrInterfaceDeclaration tableTemplate) {
        predictorsArity.set(0);
        return numericPredictors.stream()
                .map(numericPredictor -> new AbstractMap.SimpleEntry<>(numericPredictor.getName().getValue(),
                                                                       addNumericPredictor(numericPredictor,
                                                                                           tableTemplate,
                                                                                           predictorsArity.addAndGet(1))))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey,
                                          AbstractMap.SimpleEntry::getValue));
    }

    /**
     * Add a <b>NumericPredictor</b> <code>MethodDeclaration</code> to the class
     * @param numericPredictor
     * @param tableTemplate
     * @param predictorArity
     * @return
     */
    static MethodDeclaration addNumericPredictor(final NumericPredictor numericPredictor,
                                                         final ClassOrInterfaceDeclaration tableTemplate,
                                                         int predictorArity) {
        try {
            templateEvaluate = getFromFileName(KIE_PMML_EVALUATE_METHOD_TEMPLATE_JAVA);
            cloneEvaluate = templateEvaluate.clone();
            ClassOrInterfaceDeclaration evaluateTemplateClass =
                    cloneEvaluate.getClassByName(KIE_PMML_EVALUATE_METHOD_TEMPLATE)
                            .orElseThrow(() -> new RuntimeException(MAIN_CLASS_NOT_FOUND));
            MethodDeclaration methodTemplate;
            if (Objects.equals(1, numericPredictor.getExponent())) {
                methodTemplate = getNumericPredictorWithoutExponentTemplate(numericPredictor, evaluateTemplateClass);
            } else {
                methodTemplate = getNumericPredictorWithExponentTemplate(numericPredictor, evaluateTemplateClass);
            }
            return addMethod(methodTemplate, tableTemplate, "evaluateNumericPredictor" + predictorArity);
        } catch (Exception e) {
            throw new KiePMMLInternalException(String.format("Failed to add NumericPredictor %s",
                                                             numericPredictor.getName()), e);
        }
    }

    /**
     * Add a <b>NumericPredictor</b> <code>MethodDeclaration</code> with <b>exponent != 1</b> to the class
     * @param numericPredictor
     * @param evaluateTemplateClass
     * @return
     */
    static MethodDeclaration getNumericPredictorWithExponentTemplate(final NumericPredictor numericPredictor,
                                                                             final ClassOrInterfaceDeclaration evaluateTemplateClass) {
        final MethodDeclaration toReturn = evaluateTemplateClass.getMethodsByName("evaluateNumericWithExponent").get(0);
        final BlockStmt body =
                toReturn.getBody().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_BODY_TEMPLATE
                        , toReturn.getName())));
        VariableDeclarator variableDeclarator = getVariableDeclarator(body, COEFFICIENT)
                .orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_VARIABLE_IN_BODY, COEFFICIENT,
                                                                              body)));
        variableDeclarator.setInitializer(String.valueOf(numericPredictor.getCoefficient().doubleValue()));
        variableDeclarator = getVariableDeclarator(body, EXPONENT)
                .orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_VARIABLE_IN_BODY, EXPONENT,
                                                                              body)));
        variableDeclarator.setInitializer(String.valueOf(numericPredictor.getExponent().doubleValue()));
        return toReturn;
    }

    /**
     * Add a <b>NumericPredictor</b> <code>MethodDeclaration</code> with <b>exponent == 1</b> to the class
     * @param numericPredictor
     * @param evaluateTemplateClass
     * @return
     */
    static MethodDeclaration getNumericPredictorWithoutExponentTemplate(final NumericPredictor numericPredictor, final ClassOrInterfaceDeclaration evaluateTemplateClass) {
        final MethodDeclaration toReturn =
                evaluateTemplateClass.getMethodsByName("evaluateNumericWithoutExponent").get(0);
        final BlockStmt body =
                toReturn.getBody().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_BODY_TEMPLATE
                        , toReturn.getName())));
        VariableDeclarator variableDeclarator = getVariableDeclarator(body, COEFFICIENT)
                .orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_VARIABLE_IN_BODY, COEFFICIENT,
                                                                              body)));
        variableDeclarator.setInitializer(String.valueOf(numericPredictor.getCoefficient().doubleValue()));
        return toReturn;
    }

    /**
     * Add <b>CategoricalPredictor</b>s <code>MethodDeclaration</code> to the class
     * @param categoricalPredictors
     * @param tableTemplate
     * @return
     */
    static Map<String, MethodDeclaration> addCategoricalPredictors(final List<CategoricalPredictor> categoricalPredictors, final ClassOrInterfaceDeclaration tableTemplate) {
        predictorsArity.set(0);
        final Map<String, List<CategoricalPredictor>> groupedCollectors = categoricalPredictors.stream()
                .collect(groupingBy(categoricalPredictor -> categoricalPredictor.getField().getValue()));
        return groupedCollectors.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(),
                                                            addGroupedCategoricalPredictor(entry.getValue(),
                                                                                           tableTemplate,
                                                                                           predictorsArity.addAndGet(1))))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey,
                                          AbstractMap.SimpleEntry::getValue));
    }

    /**
     * Add a <b>CategoricalPredictor</b> <code>MethodDeclaration</code> to the class
     * @param categoricalPredictors
     * @param tableTemplate
     * @param predictorArity
     * @return
     */
    static MethodDeclaration addGroupedCategoricalPredictor(final List<CategoricalPredictor> categoricalPredictors, final ClassOrInterfaceDeclaration tableTemplate, int predictorArity) {
        try {
            templateEvaluate = getFromFileName(KIE_PMML_EVALUATE_METHOD_TEMPLATE_JAVA);
            cloneEvaluate = templateEvaluate.clone();
            ClassOrInterfaceDeclaration evaluateTemplateClass =
                    cloneEvaluate.getClassByName(KIE_PMML_EVALUATE_METHOD_TEMPLATE)
                            .orElseThrow(() -> new RuntimeException(MAIN_CLASS_NOT_FOUND));
            MethodDeclaration methodTemplate = evaluateTemplateClass.getMethodsByName("evaluateCategorical").get(0);
            final BlockStmt body =
                    methodTemplate.getBody().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_BODY_TEMPLATE, methodTemplate.getName())));
            IfStmt ifStmt = new IfStmt();
            for (int i = 0; i < categoricalPredictors.size(); i++) {
                CategoricalPredictor categoricalPredictor = categoricalPredictors.get(i);
                Expression lhe;
                if (categoricalPredictor.getValue() instanceof String) {
                    lhe = new StringLiteralExpr((String) categoricalPredictor.getValue());
                } else {
                    lhe = new NameExpr(categoricalPredictor.getValue().toString());
                }
                NodeList<Expression> expressions = NodeList.nodeList(lhe, new NameExpr("input"));
                MethodCallExpr conditionExpr = new MethodCallExpr(new NameExpr("Objects"), "equals", expressions);
                if (i == 0) {
                    ifStmt.setCondition(conditionExpr);
                    ifStmt.setThenStmt(new ReturnStmt(new DoubleLiteralExpr(String.valueOf(categoricalPredictor.getCoefficient()))));
                    body.addStatement(ifStmt);
                } else {
                    IfStmt elseIf = new IfStmt();
                    elseIf.setCondition(conditionExpr);
                    elseIf.setThenStmt(new ReturnStmt(new DoubleLiteralExpr(String.valueOf(categoricalPredictor.getCoefficient()))));
                    ifStmt.setElseStmt(elseIf);
                    ifStmt = elseIf;
                }
            }
            ifStmt.setElseStmt(new ReturnStmt(new DoubleLiteralExpr("0.0")));
            return addMethod(methodTemplate, tableTemplate, "evaluateCategoricalPredictor" + predictorArity);
        } catch (Exception e) {
            throw new KiePMMLInternalException("Failed to add CategoricalPredictors", e);
        }
    }

    /**
     * Add <b>PredictorTerm</b>s <code>MethodDeclaration</code> to the class
     * @param predictorTerms
     * @param tableTemplate
     * @return
     */
    static Map<String, MethodDeclaration> addPredictorTerms(final List<PredictorTerm> predictorTerms,
                                                                    final ClassOrInterfaceDeclaration tableTemplate) {
        predictorsArity.set(0);
        return predictorTerms.stream()
                .map(predictorTerm -> {
                    int arity = predictorsArity.addAndGet(1);
                    return new AbstractMap.SimpleEntry<>(predictorTerm.getName() != null ?
                                                                 predictorTerm.getName().getValue() :
                                                                 "predictorTerm" + arity,
                                                         addPredictorTerm(predictorTerm, tableTemplate, arity));
                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey,
                                          AbstractMap.SimpleEntry::getValue));
    }

    /**
     * Add a <b>PredictorTerm</b> <code>MethodDeclaration</code> to the class
     * @param predictorTerm
     * @param tableTemplate
     * @param predictorArity
     * @return
     */
    static MethodDeclaration addPredictorTerm(final PredictorTerm predictorTerm,
                                              final ClassOrInterfaceDeclaration tableTemplate,
                                              int predictorArity) {
        try {
            templateEvaluate = getFromFileName(KIE_PMML_EVALUATE_METHOD_TEMPLATE_JAVA);
            cloneEvaluate = templateEvaluate.clone();
            ClassOrInterfaceDeclaration evaluateTemplateClass =
                    cloneEvaluate.getClassByName(KIE_PMML_EVALUATE_METHOD_TEMPLATE)
                            .orElseThrow(() -> new RuntimeException(MAIN_CLASS_NOT_FOUND));
            MethodDeclaration methodTemplate = evaluateTemplateClass.getMethodsByName("evaluatePredictor").get(0);
            final BlockStmt body =
                    methodTemplate.getBody().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_BODY_TEMPLATE, methodTemplate.getName())));
            VariableDeclarator variableDeclarator = getVariableDeclarator(body, "fieldRefs")
                    .orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_VARIABLE_IN_BODY,
                                                                                  "fieldRefs", body)));
            final List<Expression> nodeList = predictorTerm.getFieldRefs().stream()
                    .map(fieldRef -> new StringLiteralExpr(fieldRef.getField().getValue()))
                    .collect(Collectors.toList());
            NodeList<Expression> expressions = NodeList.nodeList(nodeList);
            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("Arrays"), "asList", expressions);
            variableDeclarator.setInitializer(methodCallExpr);
            variableDeclarator = getVariableDeclarator(body, COEFFICIENT)
                    .orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_VARIABLE_IN_BODY,
                                                                                  COEFFICIENT, body)));
            variableDeclarator.setInitializer(String.valueOf(predictorTerm.getCoefficient().doubleValue()));
            return addMethod(methodTemplate, tableTemplate, "evaluatePredictorTerm" + predictorArity);
        } catch (Exception e) {
            throw new KiePMMLInternalException(String.format("Failed to add PredictorTerm %s", predictorTerm), e);
        }
    }

    /**
     * Populate the <b>getTargetCategory</b> method of the class
     * @param tableTemplate
     * @param targetCategory
     * @return
     */
    static void populateGetTargetCategory(final ClassOrInterfaceDeclaration tableTemplate,
                                                    final Object targetCategory) {
        MethodDeclaration methodDeclaration = tableTemplate.getMethodsByName("getTargetCategory").get(0);
        final BlockStmt body =
                methodDeclaration.getBody().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_BODY_TEMPLATE, methodDeclaration.getName())));
        ReturnStmt returnStmt = new ReturnStmt();
        if (targetCategory == null) {
            returnStmt.setExpression(new NameExpr("null"));
        } else if (targetCategory instanceof String) {
            returnStmt.setExpression(new StringLiteralExpr((String) targetCategory));
        } else {
            returnStmt.setExpression(new NameExpr(targetCategory.toString()));
        }
        body.addStatement(returnStmt);
    }

    /**
     * Populate the <b>getTargetCategory</b> method of the class
     * @param tableTemplate
     * @param normalizationMethod
     * @return
     */
    static void populateUpdateResult(final ClassOrInterfaceDeclaration tableTemplate,
                                               final RegressionModel.NormalizationMethod normalizationMethod) {
        try {
            templateEvaluate = getFromFileName(KIE_PMML_UPDATE_RESULT_METHOD_TEMPLATE_JAVA);
            cloneEvaluate = templateEvaluate.clone();
            ClassOrInterfaceDeclaration evaluateTemplateClass =
                    cloneEvaluate.getClassByName(KIE_PMML_UPDATE_RESULT_METHOD_TEMPLATE)
                            .orElseThrow(() -> new RuntimeException(MAIN_CLASS_NOT_FOUND));
            String methodName = String.format("update%sResult", normalizationMethod.name());
            MethodDeclaration methodDeclaration = evaluateTemplateClass.getMethodsByName(methodName).get(0);
            final BlockStmt body =
                    methodDeclaration.getBody().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_BODY_TEMPLATE, methodDeclaration.getName())));
            MethodDeclaration targetMethod = tableTemplate.getMethodsByName("updateResult").get(0);
            targetMethod.setBody(body);
        } catch (Exception e) {
            throw new KiePMMLInternalException("Failed to populate UpdateResult", e);
        }
    }

}
