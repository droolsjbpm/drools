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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ScenarioSimulationSharedUtils {

    public static final String FILE_EXTENSION = "scesim";

    private ScenarioSimulationSharedUtils() {
        // Utils Class
    }

    /**
     * Returns true if given string isCollection or isMap
     * @param className
     * @return
     */
    public static boolean isCollectionOrMap(String className) {
        return isCollection(className) || isMap(className);
    }

    /**
     * Returns true if given string equals to canonical name of Collection or isList
     * @param className
     * @return
     */
    public static boolean isCollection(String className) {
        return Collection.class.getCanonicalName().equals(className) ||  isList(className);
    }

    /**
     * Returns true if given string equals to canonical name of List, ArrayList or LinkedList
     * @param className
     * @return
     */
    public static boolean isList(String className) {
        return List.class.getCanonicalName().equals(className) ||
                ArrayList.class.getCanonicalName().equals(className) ||
                LinkedList.class.getCanonicalName().equals(className);
    }

    /**
     * Returns true if given string equals to canonical name of Map or HashMap
     * @param className
     * @return
     */
    public static boolean isMap(String className) {
        return Map.class.getCanonicalName().equals(className) ||
                HashMap.class.getCanonicalName().equals(className) ||
                LinkedHashMap.class.getCanonicalName().equals(className) ||
                TreeMap.class.getCanonicalName().equals(className);
    }

    /**
     * Returns true if given string equals to canonical name of <code>java.lang.Enum</code>
     * @param className
     * @return
     */
    public static boolean isEnumCanonicalName(String className) {
        return Enum.class.getCanonicalName().equals(className);
    }
}
