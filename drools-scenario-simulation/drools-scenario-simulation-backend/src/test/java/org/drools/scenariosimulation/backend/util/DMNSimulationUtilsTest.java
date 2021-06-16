/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
package org.drools.scenariosimulation.backend.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.drools.scenariosimulation.backend.exceptions.ImpossibleToFindDMNException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.io.Resource;
import org.kie.dmn.api.core.DMNModel;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DMNSimulationUtilsTest {

    @Test
    public void findDMNModel() {
        List<String> pathToFind = Arrays.asList(new StringBuilder("to/find").reverse().toString().split("/"));

        List<DMNModel> models = Stream.of("this/should/not/match", "find", "something/to/find")
                .map(this::createDMNModelMock).collect(Collectors.toList());

        DMNSimulationUtils.findDMNModel(models, pathToFind, 1);

        List<String> impossibleToFind = Arrays.asList(new StringBuilder("not/find").reverse().toString().split("/"));

        Assertions.assertThatThrownBy(() -> DMNSimulationUtils.findDMNModel(models, impossibleToFind, 1))
                .isInstanceOf(ImpossibleToFindDMNException.class)
                .hasMessage("Retrieving the DMNModel has failed. Make sure the used DMN asset does not " +
                                    "produce any compilation errors and that the project does not " +
                                    "contain multiple DMN assets with the same name and namespace. " +
                                    "In addition, check if the reference to the DMN file is correct " +
                                    "in the Settings panel. " +
                                    "After addressing the issues, build the project again.");
    }

    private DMNModel createDMNModelMock(String path) {
        DMNModel modelMock = mock(DMNModel.class);
        Resource resourceMock = mock(Resource.class);
        when(resourceMock.getSourcePath()).thenReturn(path);
        when(modelMock.getResource()).thenReturn(resourceMock);
        return modelMock;
    }
}