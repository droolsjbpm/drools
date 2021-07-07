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
import org.dmg.pmml.DataType;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.DiscretizeBin;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Interval;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.commons.model.expressions.KiePMMLDiscretize;
import org.kie.pmml.commons.model.expressions.KiePMMLDiscretizeBin;
import org.kie.pmml.commons.model.expressions.KiePMMLInterval;
import org.kie.pmml.compiler.commons.utils.JavaParserUtils;

import static org.junit.Assert.assertTrue;
import static org.kie.pmml.compiler.commons.testutils.CodegenTestUtils.commonValidateCompilationWithImports;

public class KiePMMLDiscretizeFactoryTest {

    private static final String NAME = "name";
    private static final String MAP_MISSING_TO = "mapMissingTo";
    private static final String DEFAULTVALUE = "defaultValue";
    private static final DataType dataType = DataType.INTEGER;

    private static DiscretizeBin discretizeBin1;
    private static DiscretizeBin discretizeBin2;
    private static List<DiscretizeBin> discretizeBins;

    @BeforeClass
    public static void setup() {
        discretizeBin1 = getDiscretizeBin(getInterval(null, 20, Interval.Closure.OPEN_OPEN), "discretizeBin1");
        discretizeBin2 = getDiscretizeBin(getInterval(21, 30,
                                                      Interval.Closure.OPEN_CLOSED), "discretizeBin2");
        discretizeBins = Arrays.asList(discretizeBin1, discretizeBin2);
    }

    @Test
    public void getDiscretizeBinVariableDeclaration() {
        String variableName = "variableName";
        Discretize discretize = new Discretize();
        discretize.setField(FieldName.create(NAME));
        discretize.setDataType(dataType);
        discretize.setMapMissingTo(MAP_MISSING_TO);
        discretize.setDefaultValue(DEFAULTVALUE);
        discretize.addDiscretizeBins(discretizeBins.toArray(new DiscretizeBin[0]));

        BlockStmt retrieved = KiePMMLDiscretizeFactory.getDiscretizeVariableDeclaration(variableName,
                                                                                    discretize);
        String dataTypeString =
                DATA_TYPE.class.getName() + "." + DATA_TYPE.byName(discretize.getDataType().value()).name();

        Statement expected = JavaParserUtils.parseBlock(String.format("{\n" +
                                                                              "    KiePMMLInterval " +
                                                                              "%1$s_0_Interval = new " +
                                                                              "KiePMMLInterval(null, 20, org.kie.pmml" +
                                                                              ".api.enums.CLOSURE.OPEN_OPEN);\n" +
                                                                              "    KiePMMLDiscretizeBin " +
                                                                              "variableName_0 = new " +
                                                                              "KiePMMLDiscretizeBin" +
                                                                              "(\"%1$s_0\", Collections" +
                                                                              ".emptyList(), \"discretizeBin1\", " +
                                                                              "%1$s_0_Interval);\n" +
                                                                              "    KiePMMLInterval " +
                                                                              "%1$s_1_Interval = new " +
                                                                              "KiePMMLInterval(21, 30, org.kie.pmml" +
                                                                              ".api.enums.CLOSURE.OPEN_CLOSED);\n" +
                                                                              "    KiePMMLDiscretizeBin " +
                                                                              "%1$s_1 = new " +
                                                                              "KiePMMLDiscretizeBin" +
                                                                              "(\"%1$s_1\", Collections" +
                                                                              ".emptyList(), \"discretizeBin2\", " +
                                                                              "%1$s_1_Interval);\n" +
                                                                              "    KiePMMLDiscretize %1$s = " +
                                                                              "new KiePMMLDiscretize" +
                                                                              "(\"%2$s\", Collections" +
                                                                              ".emptyList(), Arrays.asList" +
                                                                              "(%1$s_0, %1$s_1), " +
                                                                              "\"%3$s\", \"%4$s\", " +
                                                                              "%5$s);\n" +
                                                                              "}", variableName, NAME, MAP_MISSING_TO, DEFAULTVALUE, dataTypeString));
        assertTrue(JavaParserUtils.equalsNode(expected, retrieved));
        List<Class<?>> imports = Arrays.asList(Arrays.class, Collections.class, KiePMMLDiscretize.class, KiePMMLDiscretizeBin.class, KiePMMLInterval.class);
        commonValidateCompilationWithImports(retrieved, imports);
    }

    private static Interval getInterval(Number leftMargin, Number rightMargin, Interval.Closure closure) {
        Interval toReturn = new Interval();
        toReturn.setLeftMargin(leftMargin);
        toReturn.setRightMargin(rightMargin);
        toReturn.setClosure(closure);
        return toReturn;
    }

    private static DiscretizeBin getDiscretizeBin(Interval interval, String binValue) {
        DiscretizeBin toReturn = new DiscretizeBin();
        toReturn.setBinValue(binValue);
        toReturn.setInterval(interval);
        return toReturn;
    }

}