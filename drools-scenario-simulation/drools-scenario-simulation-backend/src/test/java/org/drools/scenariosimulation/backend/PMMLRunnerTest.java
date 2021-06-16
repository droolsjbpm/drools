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
package org.drools.scenariosimulation.backend;

import java.io.File;
import java.util.Scanner;

import org.drools.core.impl.KnowledgeBaseImpl;
import org.drools.scenariosimulation.api.model.ScenarioSimulationModel;
import org.drools.scenariosimulation.backend.runner.PMMLScenarioRunner;
import org.drools.scenariosimulation.backend.runner.model.ScenarioRunnerDTO;
import org.drools.scenariosimulation.backend.util.ScenarioSimulationXMLPersistence;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.kie.api.runtime.KieContainer;
import org.kie.pmml.evaluator.assembler.factories.PMMLRuntimeFactoryImpl;
import org.kie.pmml.evaluator.core.service.PMMLRuntimeInternalImpl;
import org.kie.test.util.filesystem.FileUtils;

import static org.junit.Assert.fail;

public class PMMLRunnerTest {

    private final String PMML_FILE = "CategoricalVariablesRegression.pmml";
    private final String SCESIM_FILE = "scesim-pmml.scesim";

    @Test
    public void runSuccessScenario() throws Exception {
        KieContainer kieContainer = getKieContainer();
        ScenarioRunnerDTO scenarioRunnerDTO = getScenarioRunnerDTO();
        PMMLScenarioRunner runner = new PMMLScenarioRunner(kieContainer, scenarioRunnerDTO);
        runner.run(getRunNotifier());
    }

    private RunNotifier getRunNotifier() {
        RunNotifier toReturn = new RunNotifier();
        toReturn.addListener(new RunListenerForTest());
        return toReturn;
    }


    private KieContainer getKieContainer() {
        File pmmlFile =  FileUtils.getFile(PMML_FILE);
        PMMLRuntimeInternalImpl pmmlRuntime = (PMMLRuntimeInternalImpl) new PMMLRuntimeFactoryImpl().getPMMLRuntimeFromFile(pmmlFile);
        KnowledgeBaseImpl kieBase = (KnowledgeBaseImpl) pmmlRuntime.getKnowledgeBase();
        return kieBase.getKieContainer();
    }

    private ScenarioRunnerDTO getScenarioRunnerDTO() throws Exception {
        File scesimFile =  FileUtils.getFile(SCESIM_FILE);
        final Scanner scanner = new Scanner(scesimFile);
        String rawFile = scanner.useDelimiter("\\Z").next();
        ScenarioSimulationModel scenarioSimulationModel = getXmlReader().unmarshal(rawFile);
        return new ScenarioRunnerDTO(scenarioSimulationModel, scesimFile.getPath());
    }

    private ScenarioSimulationXMLPersistence getXmlReader() {
        return ScenarioSimulationXMLPersistence.getInstance();
    }


    private class RunListenerForTest extends RunListener {

        @Override
        public void testFinished(Description description) throws Exception {
            super.testFinished(description);
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            super.testFailure(failure);
            fail(failure.getMessage());
        }
    }
}
