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

package org.drools.scenariosimulation.backend.exceptions;

import org.drools.scenariosimulation.api.model.Scenario;
import org.drools.scenariosimulation.backend.runner.model.ScenarioResult;
import org.junit.AssumptionViolatedException;

public class ScenarioAssumptionViolatedException extends AssumptionViolatedException {

    private final Scenario scenario;
    private final ScenarioResult scenarioResult;

    public ScenarioAssumptionViolatedException(Scenario scenario, ScenarioResult scenarioResult, String message) {
        super(message);
        this.scenario = scenario;
        this.scenarioResult = scenarioResult;
    }

    public ScenarioAssumptionViolatedException(Scenario scenario, ScenarioResult scenarioResult, String assumption, Throwable t) {
        super(assumption, t);
        this.scenario = scenario;
        this.scenarioResult = scenarioResult;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public ScenarioResult getScenarioResult() {
        return scenarioResult;
    }
}
