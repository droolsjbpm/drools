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
package org.drools.scenariosimulation.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScenarioSimulationSharedUtilsTest {

    List<String> listValues = Arrays.asList(List.class.getCanonicalName(),
                                            ArrayList.class.getCanonicalName(),
                                            LinkedList.class.getCanonicalName());

    List<String> mapValues = Arrays.asList(Map.class.getCanonicalName(),
                                           HashMap.class.getCanonicalName(),
                                           LinkedHashMap.class.getCanonicalName(),
                                           TreeMap.class.getCanonicalName());

    @Test
    public void isCollectionOrMap() {
        assertTrue(listValues.stream().allMatch(ScenarioSimulationSharedUtils::isCollectionOrMap));
        assertTrue(mapValues.stream().allMatch(ScenarioSimulationSharedUtils::isCollectionOrMap));
        assertTrue(ScenarioSimulationSharedUtils.isCollectionOrMap(Collection.class.getCanonicalName()));
    }

    @Test
    public void isCollection() {
        assertTrue(listValues.stream().allMatch(ScenarioSimulationSharedUtils::isCollection));
        assertFalse(mapValues.stream().allMatch(ScenarioSimulationSharedUtils::isCollection));
        assertTrue(ScenarioSimulationSharedUtils.isCollectionOrMap(Collection.class.getCanonicalName()));
    }

    @Test
    public void isList() {
        assertTrue(listValues.stream().allMatch(ScenarioSimulationSharedUtils::isList));
        assertFalse(mapValues.stream().allMatch(ScenarioSimulationSharedUtils::isList));
        assertFalse(ScenarioSimulationSharedUtils.isList(Collection.class.getCanonicalName()));
    }

    @Test
    public void isMap() {
        assertTrue(mapValues.stream().allMatch(ScenarioSimulationSharedUtils::isMap));
    }

    @Test
    public void isEnumCanonicalName() {
        assertTrue(ScenarioSimulationSharedUtils.isEnumCanonicalName(Enum.class.getCanonicalName()));
        assertFalse(ScenarioSimulationSharedUtils.isEnumCanonicalName(Enum.class.getSimpleName()));
    }
}