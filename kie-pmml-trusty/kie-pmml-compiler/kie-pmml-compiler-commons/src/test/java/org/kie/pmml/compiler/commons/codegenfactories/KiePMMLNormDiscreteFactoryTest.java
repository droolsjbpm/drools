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

import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.NormDiscrete;
import org.junit.Test;
import org.kie.pmml.api.enums.OUTLIER_TREATMENT_METHOD;
import org.kie.pmml.commons.model.expressions.KiePMMLLinearNorm;
import org.kie.pmml.commons.model.expressions.KiePMMLNormDiscrete;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kie.pmml.compiler.commons.testutils.CodegenTestUtils.commonValidateCompilationWithImports;

public class KiePMMLNormDiscreteFactoryTest {

    @Test
    public void getNormDiscreteVariableDeclaration() {
        String variableName = "variableName";
        String fieldName = "fieldName";
        String fieldValue = "fieldValue";
        double mapMissingTo = 45.32;

        NormDiscrete normDiscrete = new NormDiscrete();
        normDiscrete.setField(FieldName.create(fieldName));
        normDiscrete.setValue(fieldValue);
        normDiscrete.setMapMissingTo(mapMissingTo);

        BlockStmt retrieved = KiePMMLNormDiscreteFactory.getNormDiscreteVariableDeclaration(variableName,
                                                                                                normDiscrete);
        Statement expected = JavaParserUtils.parseBlock(String.format("{\n" +
                                                                              "    KiePMMLNormDiscrete " +
                                                                              "%s = new " +
                                                                              "KiePMMLNormDiscrete(\"%s\", " +
                                                                              "Collections.emptyList(), " +
                                                                              "\"%s\", " +
                                                                              "%s);\n" +
                                                                              "}", variableName, fieldName, fieldValue, mapMissingTo));
        assertEquals(expected.toString(), retrieved.toString());
        assertTrue(JavaParserUtils.equalsNode(expected, retrieved));
        List<Class<?>> imports = Arrays.asList(Collections.class, KiePMMLNormDiscrete.class);
        commonValidateCompilationWithImports(retrieved, imports);
    }

}