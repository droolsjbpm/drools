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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.drools.scenariosimulation.api.model.ExpressionIdentifier;
import org.drools.scenariosimulation.api.model.FactIdentifier;
import org.drools.scenariosimulation.api.model.FactMapping;
import org.drools.scenariosimulation.api.model.FactMappingValue;
import org.drools.scenariosimulation.api.model.ScenarioSimulationModel;
import org.drools.scenariosimulation.api.model.ScenarioWithIndex;
import org.drools.scenariosimulation.api.model.ScesimModelDescriptor;
import org.drools.scenariosimulation.api.model.Settings;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluator;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluatorFactory;
import org.drools.scenariosimulation.backend.fluent.PMMLScenarioExecutableBuilder;
import org.drools.scenariosimulation.backend.runner.model.InstanceGiven;
import org.drools.scenariosimulation.backend.runner.model.ScenarioExpect;
import org.drools.scenariosimulation.backend.runner.model.ScenarioResult;
import org.drools.scenariosimulation.backend.runner.model.ScenarioResultMetadata;
import org.drools.scenariosimulation.backend.runner.model.ScenarioRunnerData;
import org.drools.scenariosimulation.backend.runner.model.ValueWrapper;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.runtime.KieContainer;
import org.kie.pmml.api.enums.ResultCode;
import org.kie.pmml.api.models.PMMLModel;

import static org.drools.scenariosimulation.backend.runner.model.ValueWrapper.errorWithMessage;

public class PMMLScenarioRunnerHelper extends AbstractRunnerHelper {

    @Override
    protected Map<String, Object> executeScenario(KieContainer kieContainer,
                                                  ScenarioRunnerData scenarioRunnerData,
                                                  ExpressionEvaluatorFactory expressionEvaluatorFactory,
                                                  ScesimModelDescriptor scesimModelDescriptor,
                                                  Settings settings) {
        if (!ScenarioSimulationModel.Type.PMML.equals(settings.getType())) {
            throw new ScenarioException("Impossible to run a not-PMML simulation with PMML runner");
        }
        PMMLScenarioExecutableBuilder executableBuilder = createBuilderWrapper(kieContainer, settings.getPmmlFilePath(), settings.getPmmlModelName());

        loadInputData(scenarioRunnerData.getBackgrounds(), executableBuilder);
        loadInputData(scenarioRunnerData.getGivens(), executableBuilder);

        return executableBuilder.run().getOutputs();
    }

    protected void loadInputData(List<InstanceGiven> dataToLoad, PMMLScenarioExecutableBuilder executableBuilder) {
        for (InstanceGiven input : dataToLoad) {
            executableBuilder.setValue(input.getFactIdentifier().getName(), input.getValue());
        }
    }

    @Override
    protected ScenarioResultMetadata extractResultMetadata(Map<String, Object> requestContext,
                                                           ScenarioWithIndex scenarioWithIndex) {
        PMMLModel pmmlModel = (PMMLModel) requestContext.get(PMMLScenarioExecutableBuilder.PMML_MODEL);
        PMML4Result pmml4Result = (PMML4Result) requestContext.get(PMMLScenarioExecutableBuilder.PMML_RESULT);

        ScenarioResultMetadata scenarioResultMetadata = new ScenarioResultMetadata(scenarioWithIndex);
        if (ResultCode.OK.getName().equals(pmml4Result.getResultCode())) {
            pmml4Result.getResultVariables().keySet().forEach(scenarioResultMetadata::addAvailable);
            scenarioResultMetadata.addExecuted(pmmlModel.getName());
        }
        return scenarioResultMetadata;
    }

    @Override
    protected void verifyConditions(ScesimModelDescriptor scesimModelDescriptor,
                                    ScenarioRunnerData scenarioRunnerData,
                                    ExpressionEvaluatorFactory expressionEvaluatorFactory,
                                    Map<String, Object> requestContext) {
        PMML4Result pmml4Result = (PMML4Result) requestContext.get(PMMLScenarioExecutableBuilder.PMML_RESULT);

        for (ScenarioExpect output : scenarioRunnerData.getExpects()) {
            FactIdentifier factIdentifier = output.getFactIdentifier();
            String requestedField = factIdentifier.getName();
            if (!pmml4Result.getResultVariables().containsKey(requestedField)) {
                throw new ScenarioException("PMML execution has not generated a result with name " + requestedField);
            }
            for (FactMappingValue expectedResult : output.getExpectedResult()) {
                ExpressionIdentifier expressionIdentifier = expectedResult.getExpressionIdentifier();

                FactMapping factMapping = scesimModelDescriptor.getFactMapping(factIdentifier, expressionIdentifier)
                        .orElseThrow(() -> new IllegalStateException("Wrong expression, this should not happen"));

                ExpressionEvaluator expressionEvaluator = expressionEvaluatorFactory.getOrCreate(expectedResult);

                ScenarioResult scenarioResult = fillResult(expectedResult,
                                                           () -> getSingleFactValueResult(factMapping, expectedResult, pmml4Result.getResultVariables().get(requestedField), expressionEvaluator),
                                                           expressionEvaluator);

                scenarioRunnerData.addResult(scenarioResult);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected ValueWrapper getSingleFactValueResult(FactMapping factMapping,
                                                    FactMappingValue expectedResult,
                                                    Object resultRaw,
                                                    ExpressionEvaluator expressionEvaluator) {
        if (resultRaw == null) {
            return errorWithMessage("The prediction " +
                                            factMapping.getFactAlias() +
                                            " has not been successfully evaluated.");
        }
        Class<?> resultClass = resultRaw.getClass();
        Object expectedResultRaw = expectedResult.getRawValue();
        return getResultWrapper(factMapping.getClassName(),
                                expectedResult,
                                expressionEvaluator,
                                expectedResultRaw,
                                resultRaw,
                                resultClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object createObject(ValueWrapper<Object> initialInstance, String className, Map<List<String>, Object> params, ClassLoader classLoader) {
        // simple types
        if (initialInstance.isValid() && !(initialInstance.getValue() instanceof Map && !(initialInstance.getValue() instanceof Collection))) {
            return initialInstance.getValue();
        } else {
            throw new ScenarioException("Only simple types allowed for PMML");
        }
    }

    protected PMMLScenarioExecutableBuilder createBuilderWrapper(KieContainer kieContainer, String pmmlFilePath, String pmmlModelName) {
        return PMMLScenarioExecutableBuilder.createBuilder(kieContainer, pmmlFilePath, pmmlModelName);
    }
}
