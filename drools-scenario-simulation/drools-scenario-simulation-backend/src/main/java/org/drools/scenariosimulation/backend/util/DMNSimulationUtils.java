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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.drools.scenariosimulation.backend.exceptions.ImpossibleToFindDMNException;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieRuntimeFactory;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;

public class DMNSimulationUtils {

    private static String delimiter = "/";

    private DMNSimulationUtils() {
    }

    public static DMNModel extractDMNModel(DMNRuntime dmnRuntime, String path) {
        List<String> pathSplit = Arrays.asList(new StringBuilder(path).reverse().toString().split(delimiter));
        List<DMNModel> dmnModels = dmnRuntime.getModels();

        return findDMNModel(dmnModels, pathSplit, 1);
    }

    public static DMNRuntime extractDMNRuntime(KieContainer kieContainer) {
        return KieRuntimeFactory.of(kieContainer.getKieBase()).get(DMNRuntime.class);
    }

    public static DMNModel findDMNModel(List<DMNModel> dmnModels, List<String> pathToFind, int step) {
        List<DMNModel> result = new ArrayList<>();
        String pathToCompare = String.join(delimiter, pathToFind.subList(0, step));
        for (DMNModel dmnModel : dmnModels) {
            String modelPath = new StringBuilder(dmnModel.getResource().getSourcePath()).reverse().toString();
            if (modelPath.startsWith(pathToCompare)) {
                result.add(dmnModel);
            }
        }
        if (result.size() == 0) {
            throw new ImpossibleToFindDMNException("Retrieving the DMNModel has failed. Make sure the used DMN asset does not " +
                                                           "produce any compilation errors and that the project does not " +
                                                           "contain multiple DMN assets with the same name and namespace. " +
                                                           "In addition, check if the reference to the DMN file is correct " +
                                                           "in the Settings panel. " +
                                                           "After addressing the issues, build the project again.");
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            return findDMNModel(dmnModels, pathToFind, step + 1);
        }
    }
}
