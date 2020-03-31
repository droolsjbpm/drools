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

package org.kie.pmml.models.tree.compiler.factories;

import java.util.HashMap;
import java.util.Map;

import org.dmg.pmml.PMML;
import org.dmg.pmml.tree.TreeModel;
import org.junit.Before;
import org.junit.Test;
import org.kie.pmml.compiler.testutils.TestUtils;
import org.kie.pmml.models.drooled.ast.KiePMMLDrooledAST;
import org.kie.pmml.models.drooled.tuples.KiePMMLOriginalTypeGeneratedType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KiePMMLTreeModelASTFactoryTest {

    private static final String SOURCE_GOLFING = "TreeSample.pmml";
    private static final String SOURCE_IRIS = "irisTree.pmml";
    private PMML golfingPmml;
    private TreeModel golfingModel;
    private PMML irisPmml;
    private TreeModel irisModel;

    @Before
    public void setUp() throws Exception {
        golfingPmml = TestUtils.loadFromFile(SOURCE_GOLFING);
        assertNotNull(golfingPmml);
        assertEquals(1, golfingPmml.getModels().size());
        assertTrue(golfingPmml.getModels().get(0) instanceof TreeModel);
        golfingModel = ((TreeModel) golfingPmml.getModels().get(0));
        irisPmml = TestUtils.loadFromFile(SOURCE_IRIS);
        assertNotNull(irisPmml);
        assertEquals(1, irisPmml.getModels().size());
        assertTrue(irisPmml.getModels().get(0) instanceof TreeModel);
        irisModel = ((TreeModel) irisPmml.getModels().get(0));
    }

    @Test
    public void getKiePMMLDrooledGolfingAST() {
        final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap = new HashMap<>();
        KiePMMLDrooledAST retrieved = KiePMMLTreeModelASTFactory.getKiePMMLDrooledAST(golfingPmml.getDataDictionary(), golfingModel, fieldTypeMap);
        assertNotNull(retrieved);
        assertFalse(retrieved.getTypes().isEmpty());
        assertFalse(retrieved.getRules().isEmpty());
    }

    @Test
    public void getKiePMMLDrooledIrisAST() {
        final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap = new HashMap<>();
        KiePMMLDrooledAST retrieved = KiePMMLTreeModelASTFactory.getKiePMMLDrooledAST(irisPmml.getDataDictionary(), irisModel, fieldTypeMap);
        assertNotNull(retrieved);
        assertFalse(retrieved.getTypes().isEmpty());
        assertFalse(retrieved.getRules().isEmpty());
    }
}