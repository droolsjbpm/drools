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
package org.kie.pmml.compiler.commons.builders;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.utils.Pair;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.Model;
import org.dmg.pmml.TransformationDictionary;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.MINING_FUNCTION;
import org.kie.pmml.api.enums.PMML_MODEL;
import org.kie.pmml.api.exceptions.KiePMMLInternalException;
import org.kie.pmml.api.models.MiningField;
import org.kie.pmml.api.models.OutputField;
import org.kie.pmml.compiler.commons.utils.CommonCodegenUtils;
import org.kie.pmml.compiler.commons.utils.ModelUtils;

import static org.kie.pmml.commons.Constants.MISSING_DEFAULT_CONSTRUCTOR;
import static org.kie.pmml.commons.utils.KiePMMLModelUtils.getSanitizedClassName;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLModelFactoryUtils.GET_CREATED_KIEPMMLOUTPUTFIELDS;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLModelFactoryUtils.addGetCreatedKiePMMLOutputFieldsMethod;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLModelFactoryUtils.addTransformationsInClassOrInterfaceDeclaration;
import static org.kie.pmml.compiler.commons.codegenfactories.KiePMMLModelFactoryUtils.setKiePMMLModelConstructor;
import static org.kie.pmml.compiler.commons.utils.ModelUtils.getTargetFieldName;

/**
 * Class meant to implement all the <b>common</b> code needed to generate a <code>KiePMMLModel</code>
 */
public class KiePMMLModelCodegenUtils {

    private KiePMMLModelCodegenUtils() {
        // Avoid instantiation
    }

    /**
     * Initialize the given <code>ClassOrInterfaceDeclaration</code> with all the <b>common</b> code needed to generate a <code>KiePMMLModel</code>
     * @param modelTemplate
     * @param dataDictionary
     * @param transformationDictionary
     * @param pmmlModel
     */
    public static void init(final ClassOrInterfaceDeclaration modelTemplate,
                            final DataDictionary dataDictionary,
                            final TransformationDictionary transformationDictionary,
                            final Model pmmlModel) {
        final ConstructorDeclaration constructorDeclaration = modelTemplate.getDefaultConstructor().orElseThrow(() -> new KiePMMLInternalException(String.format(MISSING_DEFAULT_CONSTRUCTOR, modelTemplate.getName())));
        final String name = pmmlModel.getModelName();
        final String generatedClassName = getSanitizedClassName(name);
        final List<MiningField> miningFields = ModelUtils.convertToKieMiningFieldList(pmmlModel.getMiningSchema(), dataDictionary);
        final List<OutputField> outputFields = ModelUtils.convertToKieOutputFieldList(pmmlModel.getOutput(), dataDictionary);
        final Expression miningFunctionExpression;
        if (pmmlModel.getMiningFunction() != null) {
            MINING_FUNCTION miningFunction = MINING_FUNCTION.byName(pmmlModel.getMiningFunction().value());
            miningFunctionExpression = new NameExpr(miningFunction.getClass().getName() + "." + miningFunction.name());
        } else {
            miningFunctionExpression = new NullLiteralExpr();
        }
        final PMML_MODEL pmmlModelEnum = PMML_MODEL.byName(pmmlModel.getClass().getSimpleName());
        final NameExpr pmmlMODELExpression = new NameExpr(pmmlModelEnum.getClass().getName() + "." + pmmlModelEnum.name());
        String targetFieldName = getTargetFieldName(dataDictionary, pmmlModel).orElse(null);
        final Expression targetFieldExpression;
        if (targetFieldName != null) {
            targetFieldExpression = new StringLiteralExpr(targetFieldName);
        } else {
            targetFieldExpression = new NullLiteralExpr();
        }
        Map<String, Pair<DATA_TYPE, String>> missingValueReplacements = getMissingValueReplacementsMap(dataDictionary, pmmlModel);
        setKiePMMLModelConstructor(generatedClassName, constructorDeclaration, name, miningFields, outputFields, missingValueReplacements);
        addTransformationsInClassOrInterfaceDeclaration(modelTemplate, transformationDictionary, pmmlModel.getLocalTransformations());
        final BlockStmt body = constructorDeclaration.getBody();
        CommonCodegenUtils.setAssignExpressionValue(body, "pmmlMODEL", pmmlMODELExpression);
        CommonCodegenUtils.setAssignExpressionValue(body, "miningFunction", miningFunctionExpression);
        CommonCodegenUtils.setAssignExpressionValue(body, "targetField", targetFieldExpression);
        if (pmmlModel.getOutput() != null) {
            addGetCreatedKiePMMLOutputFieldsMethod(modelTemplate, pmmlModel.getOutput().getOutputFields());
            MethodCallExpr getCreatedKiePMMLOutputFieldsExpr = new MethodCallExpr();
            getCreatedKiePMMLOutputFieldsExpr.setScope(new ThisExpr());
            getCreatedKiePMMLOutputFieldsExpr.setName(GET_CREATED_KIEPMMLOUTPUTFIELDS);
            CommonCodegenUtils.setAssignExpressionValue(body, "kiePMMLOutputFields", getCreatedKiePMMLOutputFieldsExpr);
        }
    }

    static Map<String, Pair<DATA_TYPE, String>> getMissingValueReplacementsMap(DataDictionary dataDictionary, Model pmmlModel) {
        Map<String, DATA_TYPE> dataTypeMap = dataDictionary.getDataFields().stream()
                .collect(Collectors.toMap(i -> i.getName().getValue(), i -> DATA_TYPE.byName(i.getDataType().value())));
        return pmmlModel.getMiningSchema() == null
                ? Collections.emptyMap()
                : pmmlModel.getMiningSchema().getMiningFields().stream()
                        .filter(mf -> mf.getMissingValueReplacement() instanceof String)
                        .collect(Collectors.toMap(
                                mf -> mf.getName().getValue(),
                                mf -> new Pair<>(dataTypeMap.get(mf.getName().getValue()), (String) mf.getMissingValueReplacement())
                        ));
    }

}
