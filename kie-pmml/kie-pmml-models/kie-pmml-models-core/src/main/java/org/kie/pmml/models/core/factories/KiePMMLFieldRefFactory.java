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
package org.kie.pmml.models.core.factories;

import java.util.List;
import java.util.stream.Collectors;

import org.dmg.pmml.FieldRef;
import org.kie.pmml.api.model.expressions.KiePMMLFieldRef;

import static org.kie.pmml.models.core.factories.KiePMMLExtensionFactory.getKiePMMLExtensions;

public class KiePMMLFieldRefFactory {


    public static List<KiePMMLFieldRef> getKiePMMLFieldRefs(List<FieldRef> fieldRefs) {
        return fieldRefs.stream().map(KiePMMLFieldRefFactory::getKiePMMLFieldRef).collect(Collectors.toList());
    }

    public static KiePMMLFieldRef getKiePMMLFieldRef(FieldRef fieldRef) {
        return new KiePMMLFieldRef(fieldRef.getField().getValue(),
                                   getKiePMMLExtensions(fieldRef.getExtensions()),
                                   fieldRef.getMapMissingTo());
    }

    private KiePMMLFieldRefFactory() {
    }

}
