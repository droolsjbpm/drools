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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.drools.scenariosimulation.api.model.Background;
import org.drools.scenariosimulation.api.model.BackgroundData;
import org.drools.scenariosimulation.api.model.ExpressionElement;
import org.drools.scenariosimulation.api.model.ExpressionIdentifier;
import org.drools.scenariosimulation.api.model.FactIdentifier;
import org.drools.scenariosimulation.api.model.FactMapping;
import org.drools.scenariosimulation.api.model.FactMappingType;
import org.drools.scenariosimulation.api.model.FactMappingValue;
import org.drools.scenariosimulation.api.model.FactMappingValueStatus;
import org.drools.scenariosimulation.api.model.Scenario;
import org.drools.scenariosimulation.api.model.ScenarioWithIndex;
import org.drools.scenariosimulation.api.model.ScesimModelDescriptor;
import org.drools.scenariosimulation.api.model.Settings;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluator;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluatorFactory;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluatorResult;
import org.drools.scenariosimulation.backend.runner.model.InstanceGiven;
import org.drools.scenariosimulation.backend.runner.model.ScenarioExpect;
import org.drools.scenariosimulation.backend.runner.model.ScenarioResult;
import org.drools.scenariosimulation.backend.runner.model.ScenarioResultMetadata;
import org.drools.scenariosimulation.backend.runner.model.ScenarioRunnerData;
import org.drools.scenariosimulation.backend.runner.model.ValueWrapper;
import org.drools.scenariosimulation.backend.util.ScenarioSimulationServerMessages;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static org.drools.scenariosimulation.api.utils.ScenarioSimulationSharedUtils.isCollectionOrMap;
import static org.drools.scenariosimulation.backend.runner.model.ValueWrapper.errorWithValidValue;
import static org.drools.scenariosimulation.backend.runner.model.ValueWrapper.errorWithMessage;
import static org.drools.scenariosimulation.backend.runner.model.ValueWrapper.errorWithCollectionPathToValue;
import static org.drools.scenariosimulation.backend.runner.model.ValueWrapper.of;

public abstract class AbstractRunnerHelper {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public void run(KieContainer kieContainer,
                    ScesimModelDescriptor scesimModelDescriptor,
                    ScenarioWithIndex scenarioWithIndex,
                    ExpressionEvaluatorFactory expressionEvaluatorFactory,
                    ClassLoader classLoader,
                    ScenarioRunnerData scenarioRunnerData,
                    Settings settings,
                    Background background) {

        Scenario scenario = scenarioWithIndex.getScesimData();

        extractBackgroundValues(background,
                                classLoader,
                                expressionEvaluatorFactory)
                .forEach(scenarioRunnerData::addBackground);

        extractGivenValues(scesimModelDescriptor,
                           scenario.getUnmodifiableFactMappingValues(),
                           classLoader,
                           expressionEvaluatorFactory)
                .forEach(scenarioRunnerData::addGiven);

        extractExpectedValues(scenario.getUnmodifiableFactMappingValues()).forEach(scenarioRunnerData::addExpect);

        Map<String, Object> requestContext = executeScenario(kieContainer,
                                                             scenarioRunnerData,
                                                             expressionEvaluatorFactory,
                                                             scesimModelDescriptor, settings);

        scenarioRunnerData.setMetadata(extractResultMetadata(requestContext, scenarioWithIndex));

        verifyConditions(scesimModelDescriptor,
                         scenarioRunnerData,
                         expressionEvaluatorFactory,
                         requestContext);

        validateAssertion(scenarioRunnerData.getResults(),
                          scesimModelDescriptor);
    }

    protected List<InstanceGiven> extractBackgroundValues(Background background,
                                                          ClassLoader classLoader,
                                                          ExpressionEvaluatorFactory expressionEvaluatorFactory) {
        List<InstanceGiven> backgrounds = new ArrayList<>();
        boolean hasError = false;
        for (BackgroundData row : background.getUnmodifiableData()) {
            try {
                List<InstanceGiven> givens = extractGivenValues(background.getScesimModelDescriptor(),
                                                                row.getUnmodifiableFactMappingValues(),
                                                                classLoader,
                                                                expressionEvaluatorFactory);
                backgrounds.addAll(givens);
            } catch (ScenarioException e) {
                hasError = true;
            }
        }
        if (hasError) {
            throw new ScenarioException("Error in BACKGROUND data");
        }
        return backgrounds;
    }

    protected List<InstanceGiven> extractGivenValues(ScesimModelDescriptor scesimModelDescriptor,
                                                     List<FactMappingValue> factMappingValues,
                                                     ClassLoader classLoader,
                                                     ExpressionEvaluatorFactory expressionEvaluatorFactory) {
        List<InstanceGiven> instanceGiven = new ArrayList<>();

        Map<FactIdentifier, List<FactMappingValue>> groupByFactIdentifier =
                groupByFactIdentifierAndFilter(factMappingValues, FactMappingType.GIVEN);

        boolean hasError = false;

        for (Map.Entry<FactIdentifier, List<FactMappingValue>> entry : groupByFactIdentifier.entrySet()) {

            try {

                FactIdentifier factIdentifier = entry.getKey();

                // for each fact, create a map of path to fields and values to set
                Map<List<String>, Object> paramsForBean = getParamsForBean(scesimModelDescriptor,
                                                                           factIdentifier,
                                                                           entry.getValue(),
                                                                           expressionEvaluatorFactory);

                Object bean = createObject(getDirectMapping(paramsForBean), factIdentifier.getClassName(), paramsForBean, classLoader);

                instanceGiven.add(new InstanceGiven(factIdentifier, bean));
            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getCanonicalName();
                logger.error("Error in GIVEN data " + entry.getKey() + ": " + errorMessage, e);
                hasError = true;
            }
        }

        if (hasError) {
            throw new ScenarioException("Error in GIVEN data");
        }

        return instanceGiven;
    }

    protected ValueWrapper<Object> getDirectMapping(Map<List<String>, Object> params) {
        // if a direct mapping exists (no steps to reach the field) the value itself is the object (just converted)
        for (Map.Entry<List<String>, Object> entry : params.entrySet()) {
            if (entry.getKey().isEmpty()) {
                return of(entry.getValue());
            }
        }
        return errorWithMessage("No direct mapping available");
    }

    protected List<ScenarioExpect> extractExpectedValues(List<FactMappingValue> factMappingValues) {
        List<ScenarioExpect> scenarioExpect = new ArrayList<>();

        Map<FactIdentifier, List<FactMappingValue>> groupByFactIdentifier =
                groupByFactIdentifierAndFilter(factMappingValues, FactMappingType.EXPECT);

        Set<FactIdentifier> inputFacts = factMappingValues.stream()
                .filter(elem -> FactMappingType.GIVEN.equals(elem.getExpressionIdentifier().getType()))
                .filter(elem -> !isFactMappingValueToSkip(elem))
                .map(FactMappingValue::getFactIdentifier)
                .collect(Collectors.toSet());

        for (Map.Entry<FactIdentifier, List<FactMappingValue>> entry : groupByFactIdentifier.entrySet()) {

            FactIdentifier factIdentifier = entry.getKey();

            scenarioExpect.add(new ScenarioExpect(factIdentifier, entry.getValue(), !inputFacts.contains(factIdentifier)));
        }

        return scenarioExpect;
    }

    protected Map<FactIdentifier, List<FactMappingValue>> groupByFactIdentifierAndFilter(List<FactMappingValue> factMappingValues,
                                                                                         FactMappingType type) {
        Map<FactIdentifier, List<FactMappingValue>> groupByFactIdentifier = new HashMap<>();
        for (FactMappingValue factMappingValue : factMappingValues) {
            FactIdentifier factIdentifier = factMappingValue.getFactIdentifier();

            if (isFactMappingValueToSkip(factMappingValue)) {
                continue;
            }

            ExpressionIdentifier expressionIdentifier = factMappingValue.getExpressionIdentifier();
            if (expressionIdentifier == null) {
                throw new IllegalArgumentException("ExpressionIdentifier malformed");
            }

            if (!Objects.equals(expressionIdentifier.getType(), type)) {
                continue;
            }

            groupByFactIdentifier.computeIfAbsent(factIdentifier, key -> new ArrayList<>())
                    .add(factMappingValue);
        }
        return groupByFactIdentifier;
    }

    protected boolean isFactMappingValueToSkip(FactMappingValue factMappingValue) {
        return factMappingValue.getRawValue() == null;
    }

    protected Map<List<String>, Object> getParamsForBean(ScesimModelDescriptor scesimModelDescriptor,
                                                         FactIdentifier factIdentifier,
                                                         List<FactMappingValue> factMappingValues,
                                                         ExpressionEvaluatorFactory expressionEvaluatorFactory) {
        Map<List<String>, Object> paramsForBean = new HashMap<>();

        boolean hasError = false;

        for (FactMappingValue factMappingValue : factMappingValues) {
            ExpressionIdentifier expressionIdentifier = factMappingValue.getExpressionIdentifier();

            FactMapping factMapping = scesimModelDescriptor.getFactMapping(factIdentifier, expressionIdentifier)
                    .orElseThrow(() -> new IllegalStateException("Wrong expression, this should not happen"));

            List<String> pathToField = factMapping.getExpressionElementsWithoutClass().stream()
                    .map(ExpressionElement::getStep).collect(toList());

            ExpressionEvaluator expressionEvaluator = expressionEvaluatorFactory.getOrCreate(factMappingValue);

            try {
                Object value = expressionEvaluator.evaluateLiteralExpression((String) factMappingValue.getRawValue(), factMapping.getClassName(),
                                                                             factMapping.getGenericTypes()
                );
                paramsForBean.put(pathToField, value);
            } catch (RuntimeException e) {
                factMappingValue.setExceptionMessage(e.getMessage());
                hasError = true;
            }
        }

        if (hasError) {
            throw new ScenarioException("Error in one or more input values");
        }

        return paramsForBean;
    }

    protected void validateAssertion(List<ScenarioResult> scenarioResults, ScesimModelDescriptor scesimModelDescriptor) {

        for (ScenarioResult scenarioResult : scenarioResults) {
            if (!scenarioResult.getResult()) {
                throwScenarioException(scenarioResult.getFactMappingValue(), scesimModelDescriptor);
            }
        }

    }

    private void throwScenarioException(FactMappingValue factMappingValue,
                                        ScesimModelDescriptor scesimModelDescriptor) {
        FactMapping factMapping = scesimModelDescriptor.getFactMapping(factMappingValue.getFactIdentifier(),
                                                                       factMappingValue.getExpressionIdentifier())
                .orElseThrow(() -> new IllegalStateException("Wrong expression, this should not happen"));
        String factName = String.join(".", factMapping.getExpressionElements().stream()
                .map(ExpressionElement::getStep).collect(Collectors.toList()));
        if (FactMappingValueStatus.FAILED_WITH_ERROR == factMappingValue.getStatus()) {
            throw new ScenarioException(determineExceptionMessage(factMappingValue, factName), true);
        } else if (FactMappingValueStatus.FAILED_WITH_EXCEPTION == factMappingValue.getStatus()) {
            throw new ScenarioException(ScenarioSimulationServerMessages.getGenericScenarioExceptionMessage(factMappingValue.getExceptionMessage()));
        } else {
            throw new IllegalStateException("Illegal FactMappingValue status");
        }
    }

    private String determineExceptionMessage(FactMappingValue factMappingValue, String factName) {
        if (factMappingValue.getCollectionPathToValue() == null) {
            return ScenarioSimulationServerMessages.getFactWithWrongValueExceptionMessage(factName,
                                                                                          factMappingValue.getRawValue(),
                                                                                          factMappingValue.getErrorValue());
        }
        return ScenarioSimulationServerMessages.getCollectionFactExceptionMessage(factName,
                                                                                  factMappingValue.getCollectionPathToValue(),
                                                                                  factMappingValue.getErrorValue());
    }

    protected ScenarioResult fillResult(FactMappingValue expectedResult,
                                        Supplier<ValueWrapper<?>> resultSupplier,
                                        ExpressionEvaluator expressionEvaluator) {
        ValueWrapper<?> resultValue = resultSupplier.get();

        if (resultValue.isValid()) {
            // result is satisfied so clean up previous error state
            expectedResult.resetStatus();
        } else if (resultValue.getErrorMessage().isPresent()) {
            // propagate error message
            expectedResult.setExceptionMessage(resultValue.getErrorMessage().get());
        } else if (resultValue.getCollectionPathToValue() != null) {
            expectedResult.setCollectionPathToValue(resultValue.getCollectionPathToValue());
            expectedResult.setErrorValue(resultValue.getValue());
        } else {
            try {
                // set actual as proposed value
                expectedResult.setErrorValue(expressionEvaluator.fromObjectToExpression(resultValue.getValue()));
            } catch (Exception e) {
                // otherwise generic error message
                expectedResult.setExceptionMessage(e.getMessage());
            }
        }

        return new ScenarioResult(expectedResult, resultValue.getValue()).setResult(resultValue.isValid());
    }

    protected ValueWrapper getResultWrapper(String className,
                                            FactMappingValue expectedResult,
                                            ExpressionEvaluator expressionEvaluator,
                                            Object expectedResultRaw,
                                            Object resultRaw,
                                            Class<?> resultClass) {
        try {
            ExpressionEvaluatorResult evaluationResult = expressionEvaluator.evaluateUnaryExpression((String) expectedResultRaw,
                                                                                                     resultRaw,
                                                                                                     resultClass);
            if (evaluationResult.isSuccessful()) {
                return of(resultRaw);
            } else if (isCollectionOrMap(className)) {
                return errorWithCollectionPathToValue(evaluationResult.getWrongValue(), evaluationResult.getPathToWrongValue());
            } else {
                return errorWithValidValue(resultRaw, expectedResultRaw);
            }
        } catch (Exception e) {
            expectedResult.setExceptionMessage(e.getMessage());
            return errorWithMessage(e.getMessage());
        }
    }

    protected abstract ScenarioResultMetadata extractResultMetadata(Map<String, Object> requestContext,
                                                                    ScenarioWithIndex scenarioWithIndex);

    protected abstract Map<String, Object> executeScenario(KieContainer kieContainer,
                                                           ScenarioRunnerData scenarioRunnerData,
                                                           ExpressionEvaluatorFactory expressionEvaluatorFactory,
                                                           ScesimModelDescriptor scesimModelDescriptor,
                                                           Settings settings);

    protected abstract void verifyConditions(ScesimModelDescriptor scesimModelDescriptor,
                                             ScenarioRunnerData scenarioRunnerData,
                                             ExpressionEvaluatorFactory expressionEvaluatorFactory,
                                             Map<String, Object> requestContext);

    /**
     * Create and fill object with params. InitialInstance can contain the initial
     * instance to use generated by an expression
     * @param initialInstance
     * @param className
     * @param params
     * @param classLoader
     * @return
     */
    protected abstract Object createObject(ValueWrapper<Object> initialInstance,
                                           String className,
                                           Map<List<String>, Object> params,
                                           ClassLoader classLoader);
}
