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
package  org.kie.pmml.models.tree.model;

import java.util.Collections;
import java.util.Map;

import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.commons.model.predicates.KiePMMLSimpleSetPredicate;

public class KiePMMLTreeModel extends KiePMMLModel {

    private static final long serialVersionUID = -5158590062736070465L;
    protected KiePMMLNode node;

    public KiePMMLTreeModel(String modelName) {
        super(modelName, Collections.emptyList());
    }

    @Override
    public Object evaluate(final Object knowledgeBase, final Map<String, Object> requestData) {
        return node.evaluate(requestData);
    }

    @Override
    public Map<String, Object> getOutputFieldsMap() {
        // TODO
        throw new UnsupportedOperationException();
    }

}
