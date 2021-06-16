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

package org.drools.scenariosimulation.backend.fluent;

import java.util.Objects;

import org.drools.core.command.RequestContextImpl;
import org.kie.api.KieBase;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieRuntimeFactory;
import org.kie.api.runtime.RequestContext;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.api.models.PMMLModel;
import org.kie.pmml.api.runtime.PMMLRuntime;
import org.kie.pmml.evaluator.core.PMMLContextImpl;
import org.kie.pmml.evaluator.core.utils.PMMLRequestDataBuilder;

public class PMMLScenarioExecutableBuilder {

    public static final String DEFAULT_APPLICATION = "defaultApplication";
    public static final String PMML_RESULT = "pmmlResult";
    public static final String PMML_MODEL = "pmmlModel";

    private final KieContainer kieContainer;
    private final PMMLRequestDataBuilder pmmlRequestDataBuilder;
    private final String pmmlFilePath;

    private PMMLScenarioExecutableBuilder(KieContainer kieContainer, String pmmlFilePath, String pmmlModelName) {
        this.kieContainer = kieContainer;
        this.pmmlFilePath = pmmlFilePath;
        String fileName = pmmlFilePath.contains("/") ? pmmlFilePath.substring(pmmlFilePath.lastIndexOf("/") +1) : pmmlFilePath;
        pmmlRequestDataBuilder = new PMMLRequestDataBuilder("correlationid", pmmlModelName, fileName);
    }

    public static PMMLScenarioExecutableBuilder createBuilder(KieContainer kieContainer, String pmmlFilePath,
                                                              String pmmlModelName) {
        return new PMMLScenarioExecutableBuilder(kieContainer, pmmlFilePath, pmmlModelName);
    }

    public void setValue(String key, Object value) {
        Class class1 = value.getClass();
        pmmlRequestDataBuilder.addParameter(key, value, class1);
    }

    public RequestContext run() {
        Objects.requireNonNull(kieContainer, "KieContainer is null");
        final PMMLRequestData pmmlRequestData = pmmlRequestDataBuilder.build();
        final KieBase kieBase =  kieContainer.getKieBase();
        final KieRuntimeFactory kieRuntimeFactory = KieRuntimeFactory.of(kieBase);
        final PMMLRuntime pmmlRuntime = kieRuntimeFactory.get(PMMLRuntime.class);
        final PMMLModel pmmlModel =  pmmlRuntime.getPMMLModel(pmmlRequestData.getModelName())
                .orElseThrow(() -> new KiePMMLException("Failed to retrieve model with name " + pmmlRequestData.getModelName()));
        final PMML4Result pmml4Result = pmmlRuntime.evaluate(pmmlRequestData.getModelName(), new PMMLContextImpl(pmmlRequestData));
        final RequestContext toReturn = new RequestContextImpl();
        toReturn.setOutput(PMML_RESULT, pmml4Result);
        toReturn.setOutput(PMML_MODEL, pmmlModel);
        return toReturn;
    }
}
