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

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.drools.scenariosimulation.api.model.ScenarioSimulationModel;
import org.junit.Test;
import org.kie.soup.project.datamodel.imports.Import;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static org.drools.scenariosimulation.api.utils.ConstantsHolder.BACKGROUND_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.DMO_SESSION_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.FACT_MAPPINGS_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.FACT_MAPPING_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.FACT_MAPPING_VALUE_TYPE_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.NOT_EXPRESSION;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SCENARIO_SIMULATION_MODEL_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SETTINGS;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SETTINGS_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SIMULATION_DESCRIPTOR_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SIMULATION_NODE;
import static org.drools.scenariosimulation.backend.TestUtils.getFileContent;
import static org.drools.scenariosimulation.backend.util.ScenarioSimulationXMLPersistence.getColumnWidth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ScenarioSimulationXMLPersistenceTest {

    protected ScenarioSimulationXMLPersistence instance = ScenarioSimulationXMLPersistence.getInstance();
    protected String currentVersion = new ScenarioSimulationModel().getVersion();
    protected MigrationStrategy migrationInstance = new InMemoryMigrationStrategy();

    @Test
    public void noFQCNUsed() {
        final ScenarioSimulationModel simulationModel = new ScenarioSimulationModel();
        simulationModel.getImports().addImport(new Import("org.test.Test"));

        final String xml = instance.marshal(simulationModel);

        assertFalse(xml.contains("org.drools.scenariosimulation.api.model"));
        assertFalse(xml.contains("org.kie.soup.project.datamodel.imports"));
    }

    @Test
    public void versionAttributeExists() {
        final String xml = instance.marshal(new ScenarioSimulationModel());
        assertTrue(xml.startsWith("<ScenarioSimulationModel version=\"" + ScenarioSimulationXMLPersistence.getCurrentVersion() + "\">"));
    }

    @Test
    public void migrateIfNecessary_1_0_to_1_1() throws Exception {
        String toMigrate = getFileContent("scesim-1-0.scesim");
        Document document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_0to1_1().accept(document);
        Map<Node, List<Node>> retrieved = DOMParserUtil.getChildrenNodesMap(document, "expressionIdentifier", "type");
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        retrieved.forEach((node, typeNodes) -> {
            assertEquals(1, typeNodes.size());
            assertEquals("EXPECT", typeNodes.get(0).getTextContent());
        });
        commonCheck(toMigrate, document, "1.1");
    }

    @Test
    public void migrateIfNecessary_1_1_to_1_2() throws Exception {
        String toMigrate = getFileContent("scesim-1-1.scesim");
        Document document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_1to1_2().accept(document);
        Map<Node, List<Node>> retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "dmoSession");
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        assertEquals(1, retrieved.values().iterator().next().size());
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "type");
        commonVerifySingleNodeSingleChild(retrieved, "RULE");
        commonCheck(toMigrate, document, "1.2");
    }

    @Test
    public void migrateIfNecessary_1_2_to_1_3() throws Exception {
        String toMigrate = getFileContent("scesim-1-2.scesim");
        Document document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_2to1_3().accept(document);
        List<Node> factMappingsNodes = DOMParserUtil.getNestedChildrenNodesList(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, FACT_MAPPINGS_NODE);
        assertNotNull(factMappingsNodes);
        assertEquals(1, factMappingsNodes.size());
        List<Node> factMappingNodes = DOMParserUtil.getChildrenNodesList(factMappingsNodes.get(0), FACT_MAPPING_NODE);
        for (Node factMappingNode : factMappingNodes) {
            List<Node> expressionElementsNodes = DOMParserUtil.getChildrenNodesList(factMappingNode, "expressionElements");
            assertEquals(1, expressionElementsNodes.size());
            List<Node> stepNodes = DOMParserUtil.getNestedChildrenNodesList(expressionElementsNodes.get(0), "ExpressionElement", "step");
            assertEquals(1, stepNodes.size());
        }
        commonCheck(toMigrate, document, "1.3");
    }

    @Test
    public void migrateIfNecessary_1_3_to_1_4() throws Exception {
        String toMigrate = getFileContent("scesim-1-3-rule.scesim");
        Document document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_3to1_4().accept(document);
        Map<Node, List<Node>> retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "fileName");
        commonVerifySingleNodeSingleChild(retrieved, null);
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "kieSession");
        commonVerifySingleNodeSingleChild(retrieved, "default");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "kieBase");
        commonVerifySingleNodeSingleChild(retrieved, "default");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "ruleFlowGroup");
        commonVerifySingleNodeSingleChild(retrieved, "default");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "dmoSession");
        commonVerifySingleNodeSingleChild(retrieved, "default");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "skipFromBuild");
        commonVerifySingleNodeSingleChild(retrieved, "false");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "type");
        commonVerifySingleNodeSingleChild(retrieved, "RULE");
        commonCheck(toMigrate, document, "1.4");

        toMigrate = getFileContent("scesim-1-3-dmn_1.scesim");
        document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_3to1_4().accept(document);
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "dmnNamespace");
        commonVerifySingleNodeSingleChild(retrieved, null);
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "dmnName");
        commonVerifySingleNodeSingleChild(retrieved, null);
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "skipFromBuild");
        commonVerifySingleNodeSingleChild(retrieved, "false");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "type");
        commonVerifySingleNodeSingleChild(retrieved, "DMN");
        commonCheck(toMigrate, document, "1.4");

        toMigrate = getFileContent("scesim-1-3-dmn_2.scesim");
        document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_3to1_4().accept(document);
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "dmnNamespace");
        commonVerifySingleNodeSingleChild(retrieved, null);
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "dmnName");
        commonVerifySingleNodeSingleChild(retrieved, null);
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "skipFromBuild");
        commonVerifySingleNodeSingleChild(retrieved, "false");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "type");
        commonVerifySingleNodeSingleChild(retrieved, "DMN");
        commonCheck(toMigrate, document, "1.4");
    }

    @Test
    public void migrateIfNecessary_1_4_to_1_5() throws Exception {
        String toMigrate = getFileContent("scesim-1-4-rule.scesim");
        Document document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_4to1_5().accept(document);
        List<Node> retrieved = DOMParserUtil.getNestedChildrenNodesList(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "dmoSession");
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
        commonCheck(toMigrate, document, "1.5");
    }

    @Test
    public void migrateIfNecessary_1_5_to_1_6() throws Exception {
        String toMigrate = getFileContent("scesim-1-5-dmn.scesim");
        Document document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_5to1_6().accept(document);
        Map<Node, String> retrieved = DOMParserUtil.getAttributeValues(document, "reference");
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
        commonCheck(toMigrate, document, "1.6");
    }

    @Test
    public void migrateIfNecessary_1_6_to_1_7() throws Exception {
        String toMigrate = getFileContent("scesim-1-6-rule.scesim");
        Document document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_6to1_7().accept(document);
        List<Node> factMappingsNodes = DOMParserUtil.getNestedChildrenNodesList(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, FACT_MAPPINGS_NODE);
        assertNotNull(factMappingsNodes);
        assertEquals(1, factMappingsNodes.size());
        List<Node> factMappingNodes = DOMParserUtil.getChildrenNodesList(factMappingsNodes.get(0), FACT_MAPPING_NODE);
        for (Node factMappingNode : factMappingNodes) {
            List<Node> expressionIdentifierNamesNodes = DOMParserUtil.getNestedChildrenNodesList(factMappingNode, "expressionIdentifier", "name");
            String expressionIdentifierName = expressionIdentifierNamesNodes.get(0).getTextContent();
            assertNotNull(expressionIdentifierName);
            List<Node> columnWidthNodes = DOMParserUtil.getChildrenNodesList(factMappingNode, "columnWidth");
            assertEquals(1, columnWidthNodes.size());
            String columnWidth = columnWidthNodes.get(0).getTextContent();
            assertNotNull(columnWidth);
            assertFalse(columnWidth.isEmpty());
            double columnWidthDouble = Double.parseDouble(columnWidth);
            assertEquals(getColumnWidth(expressionIdentifierName), columnWidthDouble, 0.0);
        }
        commonCheck(toMigrate, document, "1.7");
        toMigrate = getFileContent("scesim-1-6-dmn.scesim");
        document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_6to1_7().accept(document);
        factMappingsNodes = DOMParserUtil.getNestedChildrenNodesList(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, FACT_MAPPINGS_NODE);
        assertNotNull(factMappingsNodes);
        assertEquals(1, factMappingsNodes.size());
        factMappingNodes = DOMParserUtil.getChildrenNodesList(factMappingsNodes.get(0), FACT_MAPPING_NODE);
        for (Node factMappingNode : factMappingNodes) {
            List<Node> expressionIdentifierNamesNodes = DOMParserUtil.getNestedChildrenNodesList(factMappingNode, "expressionIdentifier", "name");
            String expressionIdentifierName = expressionIdentifierNamesNodes.get(0).getTextContent();
            assertNotNull(expressionIdentifierName);
            List<Node> columnWidthNodes = DOMParserUtil.getChildrenNodesList(factMappingNode, "columnWidth");
            assertEquals(1, columnWidthNodes.size());
            String columnWidth = columnWidthNodes.get(0).getTextContent();
            assertNotNull(columnWidth);
            assertFalse(columnWidth.isEmpty());
            double columnWidthDouble = Double.parseDouble(columnWidth);
            assertEquals(getColumnWidth(expressionIdentifierName), columnWidthDouble, 0.0);
        }
        commonCheck(toMigrate, document, "1.7");
    }

    @Test
    public void migrateIfNecessary_1_7_to_1_8() throws Exception {
        String toMigrate = getFileContent("scesim-1-7-dmn.scesim");
        Document document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_7to1_8().accept(document);
        Map<Node, List<Node>> retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SCENARIO_SIMULATION_MODEL_NODE, SETTINGS_NODE, "dmnFilePath");
        commonVerifySingleNodeSingleChild(retrieved, "src/main/resources/com/list.dmn");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SCENARIO_SIMULATION_MODEL_NODE, SETTINGS_NODE, "type");
        commonVerifySingleNodeSingleChild(retrieved, "DMN");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SCENARIO_SIMULATION_MODEL_NODE, SETTINGS_NODE, "dmnNamespace");
        commonVerifySingleNodeSingleChild(retrieved, "https://github.com/kiegroup/drools/kie-dmn/_CC8924B0-D729-4D70-9588-039B5824FFE9");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SCENARIO_SIMULATION_MODEL_NODE, SETTINGS_NODE, "dmnName");
        commonVerifySingleNodeSingleChild(retrieved, "a1Collection");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SCENARIO_SIMULATION_MODEL_NODE, SETTINGS_NODE, "skipFromBuild");
        commonVerifySingleNodeSingleChild(retrieved, "false");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SCENARIO_SIMULATION_MODEL_NODE, SETTINGS_NODE, "stateless");
        commonVerifySingleNodeSingleChild(retrieved, "false");
        for (String setting : SETTINGS) {
            retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, setting);
            assertTrue(retrieved.values().iterator().next().isEmpty());
        }
        commonCheck(toMigrate, document, "1.8");
        commonCheckBackground(document);
        commonCheckFactMappingValueType(document, SIMULATION_NODE);
        commonCheckFactMappingValueType(document, BACKGROUND_NODE);
        toMigrate = getFileContent("scesim-1-7-rule.scesim");
        document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_7to1_8().accept(document);
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SCENARIO_SIMULATION_MODEL_NODE, SETTINGS_NODE, "type");
        commonVerifySingleNodeSingleChild(retrieved, "RULE");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SCENARIO_SIMULATION_MODEL_NODE, SETTINGS_NODE, DMO_SESSION_NODE);
        commonVerifySingleNodeSingleChild(retrieved, "default");
        retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SCENARIO_SIMULATION_MODEL_NODE, SETTINGS_NODE, "skipFromBuild");
        commonVerifySingleNodeSingleChild(retrieved, "false");
        for (String setting : SETTINGS) {
            retrieved = DOMParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, setting);
            assertTrue(retrieved.values().iterator().next().isEmpty());
        }
        commonCheck(toMigrate, document, "1.8");
        commonCheckBackground(document);
        commonCheckFactMappingValueType(document, SIMULATION_NODE);
        commonCheckFactMappingValueType(document, BACKGROUND_NODE);
    }

    @Test
    public void migrateIfNecessary_1_8_to_1_9() throws Exception {
        String toMigrate = getFileContent("scesim-1-8-dmn.scesim");
        Document document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_8to1_9().accept(document);
        commonCheck(toMigrate, document, "1.9");
        commonCheckBackground(document);
        commonCheckFactMappingValueType(document, SIMULATION_NODE);
        commonCheckFactMappingValueType(document, BACKGROUND_NODE);
        toMigrate = getFileContent("scesim-1-8-rule.scesim");
        document = DOMParserUtil.getDocument(toMigrate);
        migrationInstance.from1_8to1_9().accept(document);
        commonCheck(toMigrate, document, "1.9");
        commonCheckBackground(document);
        commonCheckFactMappingValueType(document, SIMULATION_NODE);
        commonCheckFactMappingValueType(document, BACKGROUND_NODE);
    }

    private void commonCheckFactMappingValueType(Document document, String scesimModel) {
        List<Node> factMappingsNodes = DOMParserUtil.getNestedChildrenNodesList(document, scesimModel, SIMULATION_DESCRIPTOR_NODE, FACT_MAPPINGS_NODE);
        assertNotNull(factMappingsNodes);
        assertEquals(1, factMappingsNodes.size());
        List<Node> factMappingNodes = DOMParserUtil.getChildrenNodesList(factMappingsNodes.get(0), FACT_MAPPING_NODE);
        for (Node factMappingNode : factMappingNodes) {
            List<Node> factMappingValueTypeNodes = DOMParserUtil.getChildrenNodesList(factMappingNode, FACT_MAPPING_VALUE_TYPE_NODE);
            assertEquals(1, factMappingValueTypeNodes.size());
            String factMappingValueTypeText = factMappingValueTypeNodes.get(0).getTextContent();
            assertNotNull(factMappingValueTypeText);
            assertFalse(factMappingValueTypeText.isEmpty());
            assertEquals(NOT_EXPRESSION, factMappingValueTypeText);
        }
    }

    @Test
    public void migrateIfNecessary() throws Exception {
        Assertions.assertThatThrownBy(() -> instance.migrateIfNecessary("<ScenarioSimulationModel version=\"9999999999.99999999999\" />"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Version 9999999999.99999999999 of the file is not supported. Current version is " + ScenarioSimulationXMLPersistence.getCurrentVersion());

        String noMigrationNeeded = "<ScenarioSimulationModel version=\"" + currentVersion + "\" />";

        String afterMigration = instance.migrateIfNecessary(noMigrationNeeded);
        Document document = DOMParserUtil.getDocument(afterMigration);
        commonCheckVersion(document, ScenarioSimulationXMLPersistence.getCurrentVersion());
    }

    @Test
    public void extractVersion() {
        String version = instance.extractVersion("<ScenarioSimulationModel version=\"1.0\" version=\"1.1\">");
        assertEquals("1.0", version);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unmarshalEmptyContent() throws Exception {
        ScenarioSimulationXMLPersistence.getInstance().unmarshal("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unmarshalNullContent() throws Exception {
        ScenarioSimulationXMLPersistence.getInstance().unmarshal(null);
    }

    @Test
    public void unmarshalRULE() throws Exception {
        String toUnmarshal = getFileContent("scesim-rule.scesim");
        final ScenarioSimulationModel retrieved = ScenarioSimulationXMLPersistence.getInstance().unmarshal(toUnmarshal);
        assertEquals(ScenarioSimulationModel.Type.RULE, retrieved.getSettings().getType());
        commonCheckSimulation(retrieved);
    }

    @Test
    public void unmarshalDMN() throws Exception {
        String toUnmarshal = getFileContent("scesim-dmn.scesim");
        final ScenarioSimulationModel retrieved = ScenarioSimulationXMLPersistence.getInstance().unmarshal(toUnmarshal);
        assertEquals(ScenarioSimulationModel.Type.DMN, retrieved.getSettings().getType());
        commonCheckSimulation(retrieved);
    }

    @Test
    public void unmarshalPMML() throws Exception {
        String toUnmarshal = getFileContent("scesim-pmml.scesim");
        final ScenarioSimulationModel retrieved = ScenarioSimulationXMLPersistence.getInstance().unmarshal(toUnmarshal);
        assertEquals(ScenarioSimulationModel.Type.PMML, retrieved.getSettings().getType());
        commonCheckSimulation(retrieved);
    }

    /**
     * Verify the given <code>Map</code> has one single entry, whose <code>List</code> value also has a single children.
     * If <b>expectedTextContent</b> is given, it also check the children text content match
     * @param toCheck
     * @param expectedTextContent
     */
    private void commonVerifySingleNodeSingleChild(Map<Node, List<Node>> toCheck, String expectedTextContent) {
        assertNotNull(toCheck);
        assertEquals(1, toCheck.size());
        assertNotNull(toCheck.values());
        assertEquals(1, toCheck.values().iterator().next().size());
        if (expectedTextContent != null) {
            assertEquals(expectedTextContent, toCheck.values().iterator().next().get(0).getTextContent());
        }
    }

    private void commonCheck(String toMigrate, Document document, String expectedVersion) throws Exception {
        commonCheckVersion(document, expectedVersion);
        commonCheckSimulation(document);
        instance.migrateIfNecessary(toMigrate);
    }

    private void commonCheckVersion(Document document, String expectedVersion) {
        final Map<Node, String> attributeValues = DOMParserUtil.getAttributeValues(document, "ScenarioSimulationModel", "version");
        assertNotNull(attributeValues);
        assertEquals(1, attributeValues.size());
        assertEquals(expectedVersion, attributeValues.values().toArray()[0]);
    }

    private void commonCheckSimulation(Document toCheck) throws Exception {
        String migrated = DOMParserUtil.getString(toCheck);
        ScenarioSimulationModel scenarioSimulationModel = instance.internalUnmarshal(migrated);
        commonCheckSimulation(scenarioSimulationModel);
    }

    private void commonCheckSimulation(ScenarioSimulationModel toCheck) {
        assertNotNull(toCheck);
        assertNotNull(toCheck.getSimulation());
        assertNotNull(toCheck.getSimulation().getScesimModelDescriptor());
    }

    private void commonCheckBackground(Document toCheck) throws Exception {
        String migrated = DOMParserUtil.getString(toCheck);
        ScenarioSimulationModel scenarioSimulationModel = instance.internalUnmarshal(migrated);
        commonCheckBackground(scenarioSimulationModel);
    }

    private void commonCheckBackground(ScenarioSimulationModel toCheck) {
        assertNotNull(toCheck);
        assertNotNull(toCheck.getBackground());
        assertNotNull(toCheck.getBackground().getScesimModelDescriptor());
        assertFalse(toCheck.getBackground().getUnmodifiableData().isEmpty());
    }
}
