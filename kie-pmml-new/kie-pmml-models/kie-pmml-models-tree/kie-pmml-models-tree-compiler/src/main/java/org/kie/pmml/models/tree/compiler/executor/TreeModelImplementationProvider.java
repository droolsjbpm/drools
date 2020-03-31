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
package org.kie.pmml.models.tree.compiler.executor;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.tree.TreeModel;
import org.kie.pmml.commons.model.enums.PMML_MODEL;
import org.kie.pmml.models.drooled.provider.DrooledModelProvider;
import org.kie.pmml.models.tree.model.KiePMMLTreeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.models.tree.compiler.factories.KiePMMLTreeModelFactory.getKiePMMLTreeModel;
import static org.kie.pmml.models.tree.model.KiePMMLTreeModel.PMML_MODEL_TYPE;

/**
 * Default <code>ModelImplementationProvider</code> for <b>Tree</b>
 */
public class TreeModelImplementationProvider extends DrooledModelProvider<TreeModel, KiePMMLTreeModel> {

    private static final Logger logger = LoggerFactory.getLogger(TreeModelImplementationProvider.class.getName());

    @Override
    public PMML_MODEL getPMMLModelType() {
        logger.info("getPMMLModelType");
        return PMML_MODEL_TYPE;
    }

    @Override
    public KiePMMLTreeModel getKiePMMLDrooledModel(DataDictionary dataDictionary, TreeModel model) {
        logger.info("getKiePMMLDrooledModel {} {}", dataDictionary, model);
        return getKiePMMLTreeModel(dataDictionary, model);
    }
}