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

package org.drools.scenariosimulation.backend.util;

import org.drools.scenariosimulation.backend.interfaces.ThrowingConsumer;
import org.w3c.dom.Document;

/**
 * Interface to define the migration strategy for scesim files
 */
public interface MigrationStrategy {

    /**
     * Method to initialize migration strategy composition
     * @return
     */
    default ThrowingConsumer<Document> start() {
        return ThrowingConsumer.identity();
    }

    /**
     * Method to obtain the migration function from 1.0 to 1.1
     * @return
     */
    ThrowingConsumer<Document> from1_0to1_1();

    /**
     * Method to obtain the migration function from 1.1 to 1.2
     * @return
     */
    ThrowingConsumer<Document> from1_1to1_2();

    /**
     * Method to obtain the migration function from 1.2 to 1.3
     * @return
     */
    ThrowingConsumer<Document> from1_2to1_3();

    /**
     * Method to obtain the migration function from 1.3 to 1.4
     * @return
     */
    ThrowingConsumer<Document> from1_3to1_4();

    /**
     * Method to obtain the migration function from 1.4 to 1.5
     * @return
     */
    ThrowingConsumer<Document> from1_4to1_5();

    /**
     * Method to obtain the migration function from 1.5 to 1.6
     * @return
     */
    ThrowingConsumer<Document> from1_5to1_6();

    /**
     * Method to obtain the migration function from 1.6 to 1.7
     * @return
     */
    ThrowingConsumer<Document> from1_6to1_7();

    /**
     * Method to obtain the migration function from 1.7 to 1.8
     * @return
     */
    ThrowingConsumer<Document> from1_7to1_8();

    /**
     * Method to obtain the migration function from 1.8 to 1.9
     * @return
     */
    ThrowingConsumer<Document> from1_8to1_9();

    /**
     * Method to complete the migration. For instance it can be used to store the new value
     * @return
     */
    default ThrowingConsumer<Document> end() {
        return ThrowingConsumer.identity();
    }

    default void updateVersion(Document document, String newVersion) {
        DOMParserUtil.setAttributeValue(document, "ScenarioSimulationModel", "version", newVersion);
    }
}
