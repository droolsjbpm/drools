/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.kie.pmml.models.regression.api.model.enums;

import java.util.Arrays;

import org.kie.pmml.api.exceptions.KieEnumException;

/**
 *
 * @see <a href=http://dmg.org/pmml/v4-4/Regression.html#xsdType_REGRESSIONNORMALIZATIONMETHOD>REGRESSIONNORMALIZATIONMETHOD</a>
 */
public enum REGRESSION_NORMALIZATION_METHOD {
    NONE("none"),
    SIMPLEMAX("simplemax"),
    SOFTMAX("softmax"),
    LOGIT("logit"),
    PROBIT("probit"),
    CLOGLOG("cloglog"),
    EXP("exp"),
    LOGLOG("loglog"),
    CAUCHIT("cauchit");

    private String name;

    REGRESSION_NORMALIZATION_METHOD(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static REGRESSION_NORMALIZATION_METHOD byName(String name) throws KieEnumException {
        return  Arrays.stream(REGRESSION_NORMALIZATION_METHOD.values()).filter(value -> name.equals(value.name)).findFirst().orElseThrow(() -> new KieEnumException("Failed to find REGRESSION_NORMALIZATION_METHOD with name: " + name));
    }
}