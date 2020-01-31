#set( $str = "" )
#set( $dt = $str.getClass().forName("java.util.Date").newInstance() )
#set( $year = $dt.getYear() + 1900 )
/*
 * Copyright ${year} Red Hat, Inc. and/or its affiliates.
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

package org.kie.pmml.models.${artifactModelName}.executor;

import org.junit.Test;
import org.kie.pmml.api.model.enums.PMML_MODEL;

import static org.junit.Assert.assertEquals;

public class ${modelName}ModelImplementationProviderTest {

    private final static ${modelName}ModelImplementationProvider PROVIDER = new ${modelName}ModelImplementationProvider();

    @Test
    public void getPMMLModelType() {
        assertEquals(PMML_MODEL.${modelNameUppercase}_MODEL, PROVIDER.getPMMLModelType());
    }

    @Test
    public void getKiePMMLModel() throws Exception {
        // TODO
    }

}