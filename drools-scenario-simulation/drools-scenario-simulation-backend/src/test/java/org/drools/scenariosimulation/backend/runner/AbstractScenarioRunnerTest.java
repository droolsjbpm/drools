/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.drools.scenariosimulation.backend.runner;

import org.drools.scenariosimulation.api.model.ScenarioSimulationModel;
import org.drools.scenariosimulation.api.model.Settings;
import org.drools.scenariosimulation.api.model.Simulation;
import org.drools.scenariosimulation.api.model.ScesimModelDescriptor;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluatorFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class AbstractScenarioRunnerTest {

    @Mock
    protected KieContainer kieContainerMock;

    protected AbstractScenarioRunner abstractScenarioRunnerLocal;

    protected Settings settingsLocal;

    @Before
    public void setup() {
        settingsLocal = new Settings();
        abstractScenarioRunnerLocal = spy(
                new AbstractScenarioRunner(kieContainerMock,
                                           new Simulation(),
                                           "",
                                           ExpressionEvaluatorFactory.create(
                                                   this.getClass().getClassLoader(),
                                                   ScenarioSimulationModel.Type.RULE),
                                           settingsLocal) {
                    @Override
                    protected AbstractRunnerHelper newRunnerHelper() {
                        return null;
                    }
                });
    }

    @Test
    public void getSpecificRunnerProvider() {
        ScesimModelDescriptor scesimModelDescriptor = new ScesimModelDescriptor();
        // all existing types should have a dedicated runner
        for (ScenarioSimulationModel.Type value : ScenarioSimulationModel.Type.values()) {
            settingsLocal.setType(value);
            AbstractScenarioRunner.getSpecificRunnerProvider(value);
        }
    }

    @Test
    public void testRun() {
        assertNull(abstractScenarioRunnerLocal.simulationRunMetadataBuilder);
        abstractScenarioRunnerLocal.run(new RunNotifier());
        assertNotNull(abstractScenarioRunnerLocal.simulationRunMetadataBuilder);
    }
}