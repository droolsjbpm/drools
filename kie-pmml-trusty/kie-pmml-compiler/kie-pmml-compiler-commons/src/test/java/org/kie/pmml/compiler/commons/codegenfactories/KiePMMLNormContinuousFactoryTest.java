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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.LinearNorm;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.OutlierTreatmentMethod;
import org.junit.Test;
import org.kie.pmml.api.enums.OUTLIER_TREATMENT_METHOD;
import org.kie.pmml.commons.model.expressions.KiePMMLLinearNorm;
import org.kie.pmml.commons.model.expressions.KiePMMLNormContinuous;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kie.pmml.compiler.commons.testutils.CodegenTestUtils.commonValidateCompilationWithImports;

public class KiePMMLNormContinuousFactoryTest {

    @Test
    public void getNormContinuousVariableDeclaration() {
        String variableName = "variableName";
        String fieldName = "fieldName";
        double mapMissingTo = 45.32;
        LinearNorm ln0 = new LinearNorm(24, 26);
        LinearNorm ln1 = new LinearNorm(30, 32);
        LinearNorm ln2 = new LinearNorm(36, 34);
        LinearNorm ln3 = new LinearNorm(40, 39);
        NormContinuous normContinuous = new NormContinuous();
        normContinuous.setField(FieldName.create(fieldName));
        normContinuous.addLinearNorms(ln0, ln1, ln2, ln3);
        normContinuous.setOutliers(OutlierTreatmentMethod.AS_EXTREME_VALUES);
        normContinuous.setMapMissingTo(mapMissingTo);

        BlockStmt retrieved = KiePMMLNormContinuousFactory.getNormContinuousVariableDeclaration(variableName,
                                                                                                normContinuous);
        String outlierString =
                OUTLIER_TREATMENT_METHOD.class.getName() + "." + OUTLIER_TREATMENT_METHOD.byName(normContinuous.getOutliers().value()).name();
        Statement expected = JavaParserUtils.parseBlock(String.format("{\n" +
                                                                              "    KiePMMLNormContinuous " +
                                                                              "%s = new " +
                                                                              "KiePMMLNormContinuous(\"%s\", " +
                                                                              "Collections.emptyList(), Arrays" +
                                                                              ".asList(new KiePMMLLinearNorm" +
                                                                              "(\"LinearNorm-0\", Collections" +
                                                                              ".emptyList(), 24, 26), new " +
                                                                              "KiePMMLLinearNorm(\"LinearNorm-0\", " +
                                                                              "Collections.emptyList(), 30, 32), new" +
                                                                              " KiePMMLLinearNorm(\"LinearNorm-0\", " +
                                                                              "Collections.emptyList(), 36, 34), new" +
                                                                              " KiePMMLLinearNorm(\"LinearNorm-0\", " +
                                                                              "Collections.emptyList(), 40, 39)), " +
                                                                              "%s, " +
                                                                              "%s);\n" +
                                                                              "}", variableName, fieldName,
                                                                      outlierString, mapMissingTo));
        assertTrue(JavaParserUtils.equalsNode(expected, retrieved));
        List<Class<?>> imports = Arrays.asList(Arrays.class, Collections.class, KiePMMLLinearNorm.class,
                                               KiePMMLNormContinuous.class, OUTLIER_TREATMENT_METHOD.class);
        commonValidateCompilationWithImports(retrieved, imports);
    }

    @Test
    public void getNewKiePMMLLinearNormExpression() {
        double orig = 324.3;
        double norm = 325;
        String name = "name";
        LinearNorm linearNorm = new LinearNorm(orig, norm);
        Expression retrieved = KiePMMLNormContinuousFactory.getNewKiePMMLLinearNormExpression(linearNorm, name);
        Expression expected = JavaParserUtils.parseExpression(String.format("new KiePMMLLinearNorm(\"%s\", " +
                                                                                    "Collections" +
                                                                                    ".emptyList(), %s, %s)", name,
                                                                            orig, norm));
        assertTrue(JavaParserUtils.equalsNode(expected, retrieved));
    }
}