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

package org.drools.scenariosimulation.backend.runner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.scenariosimulation.api.model.ExpressionIdentifier;
import org.drools.scenariosimulation.api.model.FactIdentifier;
import org.drools.scenariosimulation.api.model.FactMapping;
import org.drools.scenariosimulation.api.model.FactMappingType;
import org.drools.scenariosimulation.api.model.FactMappingValue;
import org.drools.scenariosimulation.api.model.FactMappingValueStatus;
import org.drools.scenariosimulation.api.model.Scenario;
import org.drools.scenariosimulation.api.model.ScenarioSimulationModel;
import org.drools.scenariosimulation.api.model.Settings;
import org.drools.scenariosimulation.api.model.Simulation;
import org.drools.scenariosimulation.backend.expression.BaseExpressionEvaluator;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluator;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluatorFactory;
import org.drools.scenariosimulation.backend.fluent.PMMLScenarioExecutableBuilder;
import org.drools.scenariosimulation.backend.runner.model.InstanceGiven;
import org.drools.scenariosimulation.backend.runner.model.ScenarioExpect;
import org.drools.scenariosimulation.backend.runner.model.ScenarioRunnerData;
import org.drools.scenariosimulation.backend.runner.model.ValueWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.RequestContext;
import org.kie.pmml.api.enums.ResultCode;
import org.kie.pmml.api.models.PMMLModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PMMLScenarioRunnerHelperTest {

    private static final String NAME = "NAME";
    private static final String EXPRESSION_NAME = "NAME";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10);
    private static final String PMML_FILE_PATH = "pmmlFilePath";
    private static final String TEST_DESCRIPTION = "Test description";
    private static final ClassLoader classLoader = RuleScenarioRunnerHelperTest.class.getClassLoader();
    private static final ExpressionEvaluatorFactory expressionEvaluatorFactory = ExpressionEvaluatorFactory.create(classLoader, ScenarioSimulationModel.Type.PMML);
    private static final ExpressionEvaluator expressionEvaluator = new BaseExpressionEvaluator(classLoader);

    private final PMMLScenarioRunnerHelper runnerHelper = new PMMLScenarioRunnerHelper() {
        @Override
        protected PMMLScenarioExecutableBuilder createBuilderWrapper(KieContainer kieContainer, String pmmlFilePath, String pmmlModelName) {
            return pmmlScenarioExecutableBuilderMock;
        }
    };
    @Mock
    protected Map<String, Object> resultVariablesMock;
    @Mock
    protected Map<String, Object> requestContextMock;
    @Mock
    protected PMML4Result pmml4ResultMock;
    @Mock
    protected PMMLModel pmmlModelMock;
    @Mock
    protected PMMLScenarioExecutableBuilder pmmlScenarioExecutableBuilderMock;
    @Mock
    protected KieContainer kieContainerMock;
    private Simulation simulation;
    private Settings settings;
    private FactIdentifier fact1FactIdentifier;
    private ExpressionIdentifier fact1GivenExpressionIdentifier;
    private FactMapping fact1GivenFactMapping;
    private Scenario scenario1;
    private Scenario scenario2;
    private ExpressionIdentifier fact1ExpectedExpressionIdentifier;
    private FactMapping fact1ExpectedFactMapping;
    private FactIdentifier fact2FactIdentifier;
    private ExpressionIdentifier fact2GivenExpressionIdentifier;
    private FactMapping fact2GivenFactMapping;
    private ExpressionIdentifier fact2ExpectedExpressionIdentifier;
    private FactMapping fact2ExpectedFactMapping;
    private FactMappingValue fact2ExpectedFactMappingValue;
    private FactMappingValue fact1ExpectedValue;

    @Before
    public void init() {
        when(pmmlScenarioExecutableBuilderMock.run()).thenReturn(mock(RequestContext.class));

        simulation = new Simulation();
        settings = new Settings();
        settings.setType(ScenarioSimulationModel.Type.PMML);
        settings.setPmmlFilePath(PMML_FILE_PATH);
        fact1FactIdentifier = FactIdentifier.create("Fact 1", String.class.getCanonicalName());
        fact1GivenExpressionIdentifier = ExpressionIdentifier.create("Fact 1 Given", FactMappingType.GIVEN);
        fact1GivenFactMapping = simulation.getScesimModelDescriptor().addFactMapping(fact1FactIdentifier, fact1GivenExpressionIdentifier);
        fact1GivenFactMapping.addExpressionElement("Fact 1", String.class.getCanonicalName());

        fact2FactIdentifier = FactIdentifier.create("Fact 2", Double.class.getCanonicalName());
        fact2GivenExpressionIdentifier = ExpressionIdentifier.create("Fact 2 Given", FactMappingType.GIVEN);
        fact2GivenFactMapping = simulation.getScesimModelDescriptor().addFactMapping(fact2FactIdentifier, fact2GivenExpressionIdentifier);
        fact2GivenFactMapping.addExpressionElement("Fact 2", BigDecimal.class.getCanonicalName());

        fact1ExpectedExpressionIdentifier = ExpressionIdentifier.create("Fact 1 Expected", FactMappingType.EXPECT);
        fact1ExpectedFactMapping = simulation.getScesimModelDescriptor().addFactMapping(fact1FactIdentifier, fact1ExpectedExpressionIdentifier);
        fact1ExpectedFactMapping.addExpressionElement("Fact 1", String.class.getCanonicalName());

        fact2ExpectedExpressionIdentifier = ExpressionIdentifier.create("Fact 2 Expected", FactMappingType.EXPECT);
        fact2ExpectedFactMapping = simulation.getScesimModelDescriptor().addFactMapping(fact2FactIdentifier, fact2ExpectedExpressionIdentifier);
        fact2ExpectedFactMapping.addExpressionElement("Fact 2", Double.class.getCanonicalName());

        scenario1 = simulation.addData();
        scenario1.setDescription(TEST_DESCRIPTION);
        scenario1.addMappingValue(fact1FactIdentifier, fact1GivenExpressionIdentifier, EXPRESSION_NAME);
        fact1ExpectedValue = scenario1.addMappingValue(fact1FactIdentifier, fact1ExpectedExpressionIdentifier, EXPRESSION_NAME);

        scenario2 = simulation.addData();
        scenario2.setDescription(TEST_DESCRIPTION);
        scenario2.addMappingValue(fact1FactIdentifier, fact1GivenExpressionIdentifier, EXPRESSION_NAME);
        scenario2.addMappingValue(fact1FactIdentifier, fact1ExpectedExpressionIdentifier, EXPRESSION_NAME);
        scenario2.addMappingValue(fact2FactIdentifier, fact2GivenExpressionIdentifier, AMOUNT);
        fact2ExpectedFactMappingValue = scenario2.addMappingValue(fact2FactIdentifier, fact2ExpectedExpressionIdentifier, AMOUNT);

        when(pmml4ResultMock.getResultVariables()).thenReturn(resultVariablesMock);

        when(requestContextMock.get(PMMLScenarioExecutableBuilder.PMML_RESULT)).thenReturn(pmml4ResultMock);
        when(requestContextMock.get(PMMLScenarioExecutableBuilder.PMML_MODEL)).thenReturn(pmmlModelMock);
    }

    @Test
    public void verifyConditions() {
        ScenarioRunnerData scenarioRunnerData1 = new ScenarioRunnerData();
        scenarioRunnerData1.addExpect(new ScenarioExpect(fact1FactIdentifier, singletonList(fact1ExpectedValue)));

        // test 1 - no result generated for specific requestedField
        assertThatThrownBy(() -> runnerHelper.verifyConditions(simulation.getScesimModelDescriptor(), scenarioRunnerData1, expressionEvaluatorFactory, requestContextMock))
                .isInstanceOf(ScenarioException.class)
                .hasMessage("PMML execution has not generated a result with name Fact 1");

        // test 2 - when requestedField contains a null value skip the steps and just do the comparison (that should be false in this case)
        when(resultVariablesMock.containsKey(anyString())).thenReturn(true);
        when(resultVariablesMock.get(anyString())).thenReturn(null);
        when(pmml4ResultMock.getResultCode()).thenReturn(ResultCode.OK.getName());

        runnerHelper.verifyConditions(simulation.getScesimModelDescriptor(), scenarioRunnerData1, expressionEvaluatorFactory, requestContextMock);
        assertEquals(1, scenarioRunnerData1.getResults().size());
        assertFalse(scenarioRunnerData1.getResults().get(0).getResult());

        // test 3 - check are performed (but fail)
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("Fact 1", "WrongValue");
        when(pmml4ResultMock.getResultVariables()).thenReturn(resultMap);
        ScenarioRunnerData scenarioRunnerData2 = new ScenarioRunnerData();
        scenarioRunnerData2.addExpect(new ScenarioExpect(fact1FactIdentifier, singletonList(fact1ExpectedValue)));
        runnerHelper.verifyConditions(simulation.getScesimModelDescriptor(), scenarioRunnerData2, expressionEvaluatorFactory, requestContextMock);
        assertEquals(1, scenarioRunnerData2.getResults().size());
        assertFalse(scenarioRunnerData2.getResults().get(0).getResult());

        // test 4 - check are performed (but success)
        ScenarioRunnerData scenarioRunnerData3 = new ScenarioRunnerData();
        scenarioRunnerData3.addExpect(new ScenarioExpect(fact1FactIdentifier, singletonList(fact1ExpectedValue)));
        resultMap.put("Fact 1", NAME);
        runnerHelper.verifyConditions(simulation.getScesimModelDescriptor(), scenarioRunnerData3, expressionEvaluatorFactory, requestContextMock);
        assertEquals(1, scenarioRunnerData3.getResults().size());
        assertTrue(scenarioRunnerData3.getResults().get(0).getResult());

        // test 5 - verify that when expression evaluation fails the corresponding expression is marked as error
        ExpressionEvaluatorFactory expressionEvaluatorFactoryMock = mock(ExpressionEvaluatorFactory.class);
        when(expressionEvaluatorFactoryMock.getOrCreate(any())).thenReturn(mock(ExpressionEvaluator.class));
        runnerHelper.verifyConditions(simulation.getScesimModelDescriptor(),
                                      scenarioRunnerData3,
                                      expressionEvaluatorFactoryMock,
                                      requestContextMock);
        assertEquals(FactMappingValueStatus.FAILED_WITH_ERROR, scenarioRunnerData3.getResults().get(0).getFactMappingValue().getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createObjectInvalid() {
        // test 1 singleton lists
        Map<List<String>, Object> params = new HashMap<>();
        params.put(singletonList("name"), "TestName");
        params.put(singletonList("age"), BigDecimal.valueOf(10));

        ValueWrapper<Object> initialInstance1 = runnerHelper.getDirectMapping(params);
        assertThatThrownBy(() -> runnerHelper.createObject(
                initialInstance1,
                String.class.getCanonicalName(),
                params,
                this.getClass().getClassLoader()))
                .isInstanceOf(ScenarioException.class)
                .hasMessage("Only simple types allowed for PMML");

        // test 2 lists
        params.clear();
        params.put(asList("creation", "name"), "TestName");
        params.put(singletonList("age"), BigDecimal.valueOf(10));

        ValueWrapper<Object>  initialInstance2 = runnerHelper.getDirectMapping(params);
        assertThatThrownBy(() -> runnerHelper.createObject(
                initialInstance2,
                String.class.getCanonicalName(),
                params,
                this.getClass().getClassLoader()))
                .isInstanceOf(ScenarioException.class)
                .hasMessage("Only simple types allowed for PMML");

        // test 3 complex types
        params.clear();
        Map<String, Object> directMappingComplexTypeValue = new HashMap<>();
        directMappingComplexTypeValue.put("key1", "value1");
        params.put(emptyList(), directMappingComplexTypeValue);
        params.put(singletonList("key2"), "value2");

        ValueWrapper<Object> initialInstance3 = runnerHelper.getDirectMapping(params);
        assertThatThrownBy(() -> runnerHelper.createObject(
                initialInstance3,
                Map.class.getCanonicalName(),
                params,
                this.getClass().getClassLoader()))
                .isInstanceOf(ScenarioException.class)
                .hasMessage("Only simple types allowed for PMML");
    }

    @Test
    public void createObjectDirectMappingSimpleType() {
        Map<List<String>, Object> params = new HashMap<>();
        String directMappingSimpleTypeValue = "TestName";
        params.put(emptyList(), directMappingSimpleTypeValue);

        ValueWrapper<Object> initialInstance = runnerHelper.getDirectMapping(params);
        Object objectRaw = runnerHelper.createObject(
                initialInstance,
                String.class.getCanonicalName(),
                params,
                this.getClass().getClassLoader());

        assertTrue(objectRaw instanceof String);

        assertEquals(directMappingSimpleTypeValue, objectRaw);
    }

    @Test
    public void createObjectDirectMappingSimpleTypeNull() {
        Map<List<String>, Object> params = new HashMap<>();
        params.put(emptyList(), null);

        ValueWrapper<Object> initialInstance = runnerHelper.getDirectMapping(params);
        Object objectRaw = runnerHelper.createObject(
                initialInstance,
                String.class.getCanonicalName(),
                params,
                this.getClass().getClassLoader());

        assertNull(objectRaw);
    }

    @Test
    public void getSingleFactValueResultNullPrediction() {
        String factName = "Fact 1";
        FactMapping factMappingMock = mock(FactMapping.class);
        when(factMappingMock.getFactAlias()).thenReturn(factName);
        ValueWrapper<?> failedResult = runnerHelper.getSingleFactValueResult(factMappingMock,
                                                                              null,
                                                                             null,
                                                                              expressionEvaluator);
        assertFalse(failedResult.isValid());
        assertEquals("The prediction " +
                             factName +
                             " has not been successfully evaluated.",
                     failedResult.getErrorMessage().get());
    }

    @Test
    public void executeScenario() {
        ArgumentCaptor<Object> setValueCaptor = ArgumentCaptor.forClass(Object.class);

        ScenarioRunnerData scenarioRunnerData = new ScenarioRunnerData();
        scenarioRunnerData.addBackground(new InstanceGiven(fact1FactIdentifier, ""));
        scenarioRunnerData.addBackground(new InstanceGiven(fact2FactIdentifier, new BigDecimal(10)));
        scenarioRunnerData.addGiven(new InstanceGiven(fact1FactIdentifier, NAME));
        FactMappingValue factMappingValue = new FactMappingValue(fact1FactIdentifier, fact1ExpectedExpressionIdentifier, NAME);
        scenarioRunnerData.addExpect(new ScenarioExpect(fact1FactIdentifier, singletonList(factMappingValue), false));
        scenarioRunnerData.addExpect(new ScenarioExpect(fact1FactIdentifier, singletonList(factMappingValue), true));

        int inputObjects = scenarioRunnerData.getBackgrounds().size() + scenarioRunnerData.getGivens().size();

        runnerHelper.executeScenario(kieContainerMock, scenarioRunnerData, expressionEvaluatorFactory, simulation.getScesimModelDescriptor(), settings);

        verify(pmmlScenarioExecutableBuilderMock, times(inputObjects)).setValue(anyString(), setValueCaptor.capture());
        for (Object value : setValueCaptor.getAllValues()) {
            assertTrue(value instanceof String || value instanceof BigDecimal);
        }

        verify(pmmlScenarioExecutableBuilderMock, times(1)).run();

        // test not pmml error
        settings.setType(ScenarioSimulationModel.Type.RULE);
        assertThatThrownBy(() -> runnerHelper.executeScenario(kieContainerMock, scenarioRunnerData, expressionEvaluatorFactory, simulation.getScesimModelDescriptor(), settings))
                .isInstanceOf(ScenarioException.class)
                .hasMessageStartingWith("Impossible to run");
    }

}