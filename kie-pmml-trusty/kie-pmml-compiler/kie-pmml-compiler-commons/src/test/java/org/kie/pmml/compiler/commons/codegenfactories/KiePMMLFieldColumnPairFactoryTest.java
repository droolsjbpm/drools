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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Row;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.pmml.commons.model.expressions.KiePMMLFieldColumnPair;
import org.kie.pmml.commons.model.expressions.KiePMMLRow;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;
import org.kie.pmml.compiler.commons.utils.KiePMMLUtil;

import static org.junit.Assert.assertTrue;
import static org.kie.pmml.compiler.commons.testutils.CodegenTestUtils.commonValidateCompilationWithImports;
import static org.kie.test.util.filesystem.FileUtils.getFileInputStream;

public class KiePMMLFieldColumnPairFactoryTest {

    @Test
    public void getRowVariableDeclaration() {
        String variableName = "variableName";
        String fieldName = "fieldName";
        String column = "column";
        FieldColumnPair fieldColumnPair = new FieldColumnPair();
        fieldColumnPair.setField(FieldName.create(fieldName));
        fieldColumnPair.setColumn(column);

        BlockStmt retrieved = KiePMMLFieldColumnPairFactory.getFieldColumnPairVariableDeclaration(variableName,
                                                                                                  fieldColumnPair);
        Statement expected = JavaParserUtils.parseBlock(String.format("{\n" +
                                                                              "    KiePMMLFieldColumnPair %s = " +
                                                                              "new KiePMMLFieldColumnPair(\"%s\", Collections.emptyList(), \"%s\");" +
                                                                              "\n" +
                                                                              "}", variableName, fieldName, column));
        assertTrue(JavaParserUtils.equalsNode(expected, retrieved));
        List<Class<?>> imports = Arrays.asList(Collections.class, KiePMMLFieldColumnPair.class);
        commonValidateCompilationWithImports(retrieved, imports);
    }
}