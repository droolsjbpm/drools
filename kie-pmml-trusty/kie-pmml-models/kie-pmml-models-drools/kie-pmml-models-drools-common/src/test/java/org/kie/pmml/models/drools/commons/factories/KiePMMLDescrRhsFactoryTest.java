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

package org.kie.pmml.models.drools.commons.factories;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ResultFeature;
import org.drools.compiler.lang.api.DescrFactory;
import org.drools.compiler.lang.api.PackageDescrBuilder;
import org.drools.compiler.lang.api.RuleDescrBuilder;
import org.junit.Before;
import org.junit.Test;
import org.kie.pmml.api.enums.ResultCode;
import org.kie.pmml.commons.model.KiePMMLOutputField;
import org.kie.pmml.models.drools.ast.KiePMMLDroolsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.pmml.commons.Constants.DONE;
import static org.kie.pmml.api.enums.RESULT_FEATURE.PREDICTED_VALUE;
import static org.kie.pmml.models.drools.commons.factories.KiePMMLDescrRhsFactory.ADD_PMML4_OUTPUT_FIELD;
import static org.kie.pmml.models.drools.commons.factories.KiePMMLDescrRhsFactory.ADD_PMML4_RESULT_VARIABLE;
import static org.kie.pmml.models.drools.commons.factories.KiePMMLDescrRhsFactory.FOCUS_AGENDA_GROUP;
import static org.kie.pmml.models.drools.commons.factories.KiePMMLDescrRhsFactory.SET_PMML4_RESULT_CODE;
import static org.kie.pmml.models.drools.commons.factories.KiePMMLDescrRhsFactory.UPDATE_STATUS_HOLDER_STATUS;
import static org.kie.pmml.models.drools.commons.factories.KiePMMLDescrRulesFactory.BREAK_LABEL;

public class KiePMMLDescrRhsFactoryTest {

    private static final String PACKAGE_NAME = "package";
    private static final String CURRENT_RULE = "currentRule";
    private RuleDescrBuilder ruleBuilder;

    @Before
    public void setUp() throws Exception {
        PackageDescrBuilder builder = DescrFactory.newPackage().name(PACKAGE_NAME);
        ruleBuilder = builder.newRule().name(CURRENT_RULE);
        assertEquals(CURRENT_RULE, ruleBuilder.getDescr().getName());
    }

    @Test
    public void declareRhsWithoutIfBreak() {
        String name = "NAME";
        String statusToSet = "STATUS_TO_SET";
        KiePMMLDroolsRule rule = KiePMMLDroolsRule.builder(name, statusToSet, Collections.emptyList()).build();
        KiePMMLDescrRhsFactory.factory(ruleBuilder).declareRhs(rule);
        assertNotNull(ruleBuilder.getDescr().getConsequence());
        String expectedConsequence = String.format(UPDATE_STATUS_HOLDER_STATUS, statusToSet);
        assertTrue(ruleBuilder.getDescr().getConsequence().toString().contains(expectedConsequence));
        assertNotNull(ruleBuilder.getDescr().getNamedConsequences());
        assertTrue(ruleBuilder.getDescr().getNamedConsequences().isEmpty());
    }

    @Test
    public void declareRhsWithIfBreak() {
        String name = "NAME";
        String statusToSet = "STATUS_TO_SET";
        String ifBreakField = "ifBreakField";
        String ifBreakOperator = "ifBreakOperator";
        Object ifBreakValue = "ifBreakValue";
        KiePMMLDroolsRule rule = KiePMMLDroolsRule.builder(name, statusToSet, Collections.emptyList())
                .withIfBreak(ifBreakField, ifBreakOperator, ifBreakValue)
                .build();
        KiePMMLDescrRhsFactory.factory(ruleBuilder).declareRhs(rule);
        assertNotNull(ruleBuilder.getDescr().getConsequence());
        String expectedConsequence = String.format(UPDATE_STATUS_HOLDER_STATUS, statusToSet);
        assertTrue(ruleBuilder.getDescr().getConsequence().toString().contains(expectedConsequence));
        assertNotNull(ruleBuilder.getDescr().getNamedConsequences());
        assertEquals(1, ruleBuilder.getDescr().getNamedConsequences().size());
        assertNotNull(ruleBuilder.getDescr().getNamedConsequences().get(BREAK_LABEL));
    }

    @Test
    public void declareDefaultThenWithoutResult() {
        String name = "NAME";
        String statusToSet = "STATUS_TO_SET";
        KiePMMLDroolsRule rule = KiePMMLDroolsRule.builder(name, statusToSet, Collections.emptyList()).build();
        KiePMMLDescrRhsFactory.factory(ruleBuilder).declareDefaultThen(rule);
        assertNotNull(ruleBuilder.getDescr().getConsequence());
        String expectedConsequence = String.format(UPDATE_STATUS_HOLDER_STATUS, statusToSet);
        assertTrue(ruleBuilder.getDescr().getConsequence().toString().contains(expectedConsequence));
        assertNotNull(ruleBuilder.getDescr().getNamedConsequences());
        assertTrue(ruleBuilder.getDescr().getNamedConsequences().isEmpty());
    }

    @Test
    public void declareDefaultThenWithResult() {
        String name = "NAME";
        String statusToSet = "STATUS_TO_SET";
        String result = "RESULT";
        ResultCode resultCode = ResultCode.OK;
        KiePMMLDroolsRule rule = KiePMMLDroolsRule.builder(name, statusToSet, Collections.emptyList())
                .withResultCode(resultCode)
                .withResult(result)
                .build();
        KiePMMLDescrRhsFactory.factory(ruleBuilder).declareDefaultThen(rule);
        assertNotNull(ruleBuilder.getDescr().getConsequence());
        String retrievedConsequence = ruleBuilder.getDescr().getConsequence().toString();
        assertTrue(retrievedConsequence.contains(String.format(UPDATE_STATUS_HOLDER_STATUS, statusToSet)));
        assertTrue(retrievedConsequence.contains(String.format(SET_PMML4_RESULT_CODE, resultCode)));
        assertTrue(retrievedConsequence.contains(String.format(ADD_PMML4_RESULT_VARIABLE, result)));
        assertNotNull(ruleBuilder.getDescr().getNamedConsequences());
        assertTrue(ruleBuilder.getDescr().getNamedConsequences().isEmpty());
    }

    @Test
    public void declareIfThenWithoutResult() {
        String name = "NAME";
        String statusToSet = "STATUS_TO_SET";
        KiePMMLDroolsRule rule = KiePMMLDroolsRule.builder(name, statusToSet, Collections.emptyList()).build();
        KiePMMLDescrRhsFactory.factory(ruleBuilder).declareIfThen(rule);
        assertNotNull(ruleBuilder.getDescr().getConsequence());
        String expectedConsequence = String.format(UPDATE_STATUS_HOLDER_STATUS, statusToSet);
        assertEquals(expectedConsequence, ruleBuilder.getDescr().getConsequence());
        assertNotNull(ruleBuilder.getDescr().getNamedConsequences());
        assertEquals(1, ruleBuilder.getDescr().getNamedConsequences().size());
        assertNotNull(ruleBuilder.getDescr().getNamedConsequences().get(BREAK_LABEL));
        expectedConsequence = String.format(UPDATE_STATUS_HOLDER_STATUS, DONE);
        assertTrue(expectedConsequence, ruleBuilder.getDescr().getNamedConsequences().get(BREAK_LABEL).toString().contains(expectedConsequence));
    }

    @Test
    public void declareIfThenWithResult() {
        String name = "NAME";
        String statusToSet = "STATUS_TO_SET";
        String result = "RESULT";
        ResultCode resultCode = ResultCode.OK;
        KiePMMLDroolsRule rule = KiePMMLDroolsRule.builder(name, statusToSet, Collections.emptyList())
                .withResultCode(resultCode)
                .withResult(result)
                .build();
        KiePMMLDescrRhsFactory.factory(ruleBuilder).declareIfThen(rule);
        assertNotNull(ruleBuilder.getDescr().getConsequence());
        String expectedConsequence = String.format(UPDATE_STATUS_HOLDER_STATUS, statusToSet);
        assertEquals(expectedConsequence, ruleBuilder.getDescr().getConsequence());
        assertNotNull(ruleBuilder.getDescr().getNamedConsequences());
        assertEquals(1, ruleBuilder.getDescr().getNamedConsequences().size());
        assertNotNull(ruleBuilder.getDescr().getNamedConsequences().get(BREAK_LABEL));
        String retrievedConsequence = ruleBuilder.getDescr().getNamedConsequences().get(BREAK_LABEL).toString();
        assertTrue(retrievedConsequence.contains(String.format(UPDATE_STATUS_HOLDER_STATUS, DONE)));
        assertTrue(retrievedConsequence.contains(String.format(SET_PMML4_RESULT_CODE, resultCode)));
        assertTrue(retrievedConsequence.contains(String.format(ADD_PMML4_RESULT_VARIABLE, result)));
    }

    @Test
    public void commonDeclareThen() {
        String ruleName = "RULENAME";
        String statusToSet = "STATUSTOSET";
        String outputFieldName = "OUTPUTFIELDNAME";
        Object result = "RESULT";
        OutputField outputField = new OutputField();
        outputField.setName(FieldName.create(outputFieldName));
        outputField.setResultFeature(ResultFeature.PREDICTED_VALUE);
        List<OutputField> outputFields = Collections.singletonList(outputField);
        KiePMMLDroolsRule.Builder builder = KiePMMLDroolsRule.builder(ruleName, statusToSet, outputFields);
        KiePMMLDroolsRule rule = builder.build();
        StringJoiner joiner = new StringJoiner("");
        KiePMMLDescrRhsFactory.factory(ruleBuilder).commonDeclareThen(rule, joiner);
        String retrieved = joiner.toString();
        assertTrue(retrieved.isEmpty());
        //
        ResultCode resultCode = ResultCode.OK;
        builder = builder.withResultCode(resultCode);
        rule = builder.build();
        joiner = new StringJoiner("");
        KiePMMLDescrRhsFactory.factory(ruleBuilder).commonDeclareThen(rule, joiner);
        retrieved = joiner.toString();
        String expected = String.format(SET_PMML4_RESULT_CODE, resultCode);
        assertTrue(retrieved.contains(expected));
        //
        builder = builder.withResult(result);
        rule = builder.build();
        joiner = new StringJoiner("");
        KiePMMLDescrRhsFactory.factory(ruleBuilder).commonDeclareThen(rule, joiner);
        retrieved = joiner.toString();
        expected = String.format(ADD_PMML4_RESULT_VARIABLE, result);
        assertTrue(retrieved.contains(expected));
        expected = String.format(ADD_PMML4_OUTPUT_FIELD, outputFieldName, result);
        assertTrue(retrieved.contains(expected));
        //
        String focusedAgendaGroup = "FOCUSEDAGENDAGROUP";
        builder = builder.withFocusedAgendaGroup(focusedAgendaGroup);
        rule = builder.build();
        joiner = new StringJoiner("");
        KiePMMLDescrRhsFactory.factory(ruleBuilder).commonDeclareThen(rule, joiner);
        retrieved = joiner.toString();
        expected = String.format(FOCUS_AGENDA_GROUP, focusedAgendaGroup);
        assertTrue(retrieved.contains(expected));
        //
    }

    @Test
    public void commonDeclareOutputFields() {
        String outputFieldName = "OUTPUTFIELDNAME";
        Object result = "RESULT";
        OutputField outputField = new OutputField();
        outputField.setName(FieldName.create(outputFieldName));
        outputField.setResultFeature(ResultFeature.PREDICTED_VALUE);
        List<OutputField> outputFields = Collections.singletonList(outputField); StringJoiner joiner = new StringJoiner("");
        KiePMMLDescrRhsFactory.factory(ruleBuilder).commonDeclareOutputFields(outputFields, result, joiner);
        String retrieved = joiner.toString();
        String expected = String.format(ADD_PMML4_OUTPUT_FIELD, outputFieldName, result);
        assertEquals(expected, retrieved);
    }
}