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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.TransformationDictionary;
import org.dmg.pmml.regression.RegressionModel;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.api.exceptions.KiePMMLInternalException;
import org.kie.pmml.commons.model.HasClassLoader;
import org.kie.pmml.compiler.commons.builders.KiePMMLModelCodegenUtils;
import org.kie.pmml.compiler.commons.utils.CommonCodegenUtils;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;
import org.kie.pmml.models.regression.model.KiePMMLRegressionModel;
import org.kie.pmml.models.regression.model.tuples.KiePMMLTableSourceCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.commons.Constants.MISSING_DEFAULT_CONSTRUCTOR;
import static org.kie.pmml.commons.utils.KiePMMLModelUtils.getSanitizedClassName;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.MAIN_CLASS_NOT_FOUND;
import static org.kie.pmml.compiler.commons.utils.JavaParserUtils.getFullClassName;
import static org.kie.pmml.compiler.commons.utils.ModelUtils.getTargetFieldName;

public class KiePMMLRegressionModelFactory {

    static final String KIE_PMML_REGRESSION_MODEL_TEMPLATE_JAVA = "KiePMMLRegressionModelTemplate.tmpl";
    static final String KIE_PMML_REGRESSION_MODEL_TEMPLATE = "KiePMMLRegressionModelTemplate";
    private static final Logger logger = LoggerFactory.getLogger(KiePMMLRegressionModelFactory.class.getName());

    private KiePMMLRegressionModelFactory() {
    }

    public static KiePMMLRegressionModel getKiePMMLRegressionModelClasses(final DataDictionary dataDictionary,
                                                                          final TransformationDictionary transformationDictionary,
                                                                          final RegressionModel model,
                                                                          final String packageName,
                                                                          final HasClassLoader hasClassLoader) throws IOException, IllegalAccessException, InstantiationException {
        logger.trace("getKiePMMLRegressionModelClasses {} {}", dataDictionary, model);
        String className = getSanitizedClassName(model.getModelName());
        Map<String, String> sourcesMap = getKiePMMLRegressionModelSourcesMap(dataDictionary, transformationDictionary
                , model, packageName);
        String fullClassName = packageName + "." + className;
        try {
            Class<?> kiePMMLRegressionModelClass = hasClassLoader.compileAndLoadClass(sourcesMap, fullClassName);
            return (KiePMMLRegressionModel) kiePMMLRegressionModelClass.newInstance();
        } catch (Exception e) {
            throw new KiePMMLException(e);
        }
    }

    public static Map<String, String> getKiePMMLRegressionModelSourcesMap(final DataDictionary dataDictionary,
                                                                          final TransformationDictionary transformationDictionary,
                                                                          final RegressionModel model,
                                                                          final String packageName) throws IOException {
        logger.trace("getKiePMMLRegressionModelSourcesMap {} {} {}", dataDictionary, model, packageName);

        String className = getSanitizedClassName(model.getModelName());
        CompilationUnit cloneCU = JavaParserUtils.getKiePMMLModelCompilationUnit(className, packageName,
                                                                                 KIE_PMML_REGRESSION_MODEL_TEMPLATE_JAVA, KIE_PMML_REGRESSION_MODEL_TEMPLATE);
        ClassOrInterfaceDeclaration modelTemplate = cloneCU.getClassByName(className)
                .orElseThrow(() -> new KiePMMLException(MAIN_CLASS_NOT_FOUND + ": " + className));
        String targetFieldName = getTargetFieldName(dataDictionary, model).orElse(null);
        List<OutputField> outputFields =  model.getOutput() != null ? model.getOutput().getOutputFields() : Collections.emptyList();
        Map<String, KiePMMLTableSourceCategory> tablesSourceMap = getRegressionTablesMap(dataDictionary, model,
                                                                                         targetFieldName,
                                                                                         outputFields, packageName);
        String nestedTable = tablesSourceMap.size() == 1 ? tablesSourceMap.keySet().iterator().next() :
                tablesSourceMap
                        .keySet()
                        .stream()
                        .filter(tableName -> tableName.startsWith(packageName +
                                                                          ".KiePMMLRegressionTableClassification"))
                        .findFirst()
                        .orElseThrow(() -> new KiePMMLException("Failed to find expected " +
                                                                        "KiePMMLRegressionTableClassification"));
        setConstructor(model,
                       dataDictionary,
                       transformationDictionary,
                       modelTemplate,
                       nestedTable);
        Map<String, String> toReturn = tablesSourceMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getSource()));
        toReturn.put(getFullClassName(cloneCU), cloneCU.toString());
        return toReturn;
    }

    static Map<String, KiePMMLTableSourceCategory> getRegressionTablesMap(final DataDictionary dataDictionary,
                                                                          final RegressionModel model,
                                                                          final String targetFieldName,
                                                                          final List<OutputField> outputFields,
                                                                          final String packageName) {
        final DataField targetDataField = dataDictionary.getDataFields().stream()
                .filter(field -> Objects.equals(targetFieldName, field.getName().getValue()))
                .findFirst().orElse(null);
        final OpType opType = targetDataField != null ? targetDataField.getOpType() : null;
        Map<String, KiePMMLTableSourceCategory> toReturn;
        if (isRegression(model.getMiningFunction(), targetFieldName, opType)) {
            toReturn =
                    KiePMMLRegressionTableRegressionFactory.getRegressionTables(Collections.singletonList(model.getRegressionTables().get(0)),
                                                                                   model.getNormalizationMethod(),
                                                                                   outputFields,
                                                                                   targetFieldName,
                                                                                   packageName);
        } else {
            toReturn = KiePMMLRegressionTableClassificationFactory.getRegressionTables(model.getRegressionTables(),
                                                                                       model.getNormalizationMethod(),
                                                                                       opType,
                                                                                       outputFields,
                                                                                       targetFieldName,
                                                                                       packageName);
        }
        return toReturn;
    }

    static void setConstructor(final RegressionModel regressionModel,
                               final DataDictionary dataDictionary,
                               final TransformationDictionary transformationDictionary,
                               final ClassOrInterfaceDeclaration modelTemplate,
                               final String nestedTable) {
        KiePMMLModelCodegenUtils.init(modelTemplate,
                                      dataDictionary,
                                      transformationDictionary,
                                      regressionModel);
        final ConstructorDeclaration constructorDeclaration =
                modelTemplate.getDefaultConstructor().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_DEFAULT_CONSTRUCTOR, modelTemplate.getName())));
        final BlockStmt body = constructorDeclaration.getBody();
        ObjectCreationExpr objectCreationExpr = new ObjectCreationExpr();
        objectCreationExpr.setType(nestedTable);
        CommonCodegenUtils.setAssignExpressionValue(body, "regressionTable", objectCreationExpr);
    }

    static boolean isRegression(final MiningFunction miningFunction, final String targetField,
                                final OpType targetOpType) {
        return Objects.equals(MiningFunction.REGRESSION, miningFunction) && (targetField == null || Objects.equals(OpType.CONTINUOUS, targetOpType));
    }
}
