/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.models.commons.backend.rule;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.drools.compiler.lang.Expander;
import org.drools.compiler.lang.dsl.DSLMappingFile;
import org.drools.compiler.lang.dsl.DSLTokenizedMappingFile;
import org.drools.compiler.lang.dsl.DefaultExpander;
import org.drools.core.base.evaluators.Operator;
import org.drools.workbench.models.datamodel.rule.ActionCallMethod;
import org.drools.workbench.models.datamodel.rule.ActionFieldValue;
import org.drools.workbench.models.datamodel.rule.ActionGlobalCollectionAdd;
import org.drools.workbench.models.datamodel.rule.ActionInsertFact;
import org.drools.workbench.models.datamodel.rule.ActionInsertLogicalFact;
import org.drools.workbench.models.datamodel.rule.ActionSetField;
import org.drools.workbench.models.datamodel.rule.ActionUpdateField;
import org.drools.workbench.models.datamodel.rule.BaseSingleFieldConstraint;
import org.drools.workbench.models.datamodel.rule.CEPWindow;
import org.drools.workbench.models.datamodel.rule.CompositeFactPattern;
import org.drools.workbench.models.datamodel.rule.CompositeFieldConstraint;
import org.drools.workbench.models.datamodel.rule.ConnectiveConstraint;
import org.drools.workbench.models.datamodel.rule.DSLComplexVariableValue;
import org.drools.workbench.models.datamodel.rule.DSLSentence;
import org.drools.workbench.models.datamodel.rule.DSLVariableValue;
import org.drools.workbench.models.datamodel.rule.ExpressionCollection;
import org.drools.workbench.models.datamodel.rule.ExpressionField;
import org.drools.workbench.models.datamodel.rule.ExpressionFormLine;
import org.drools.workbench.models.datamodel.rule.ExpressionMethod;
import org.drools.workbench.models.datamodel.rule.ExpressionMethodParameterDefinition;
import org.drools.workbench.models.datamodel.rule.ExpressionPart;
import org.drools.workbench.models.datamodel.rule.ExpressionText;
import org.drools.workbench.models.datamodel.rule.ExpressionUnboundFact;
import org.drools.workbench.models.datamodel.rule.ExpressionVariable;
import org.drools.workbench.models.datamodel.rule.FactPattern;
import org.drools.workbench.models.datamodel.rule.FieldConstraint;
import org.drools.workbench.models.datamodel.rule.FieldNatureType;
import org.drools.workbench.models.datamodel.rule.FreeFormLine;
import org.drools.workbench.models.datamodel.rule.FromAccumulateCompositeFactPattern;
import org.drools.workbench.models.datamodel.rule.FromCollectCompositeFactPattern;
import org.drools.workbench.models.datamodel.rule.FromCompositeFactPattern;
import org.drools.workbench.models.datamodel.rule.IAction;
import org.drools.workbench.models.datamodel.rule.IPattern;
import org.drools.workbench.models.datamodel.rule.RuleAttribute;
import org.drools.workbench.models.datamodel.rule.RuleModel;
import org.drools.workbench.models.datamodel.rule.SingleFieldConstraint;
import org.drools.workbench.models.datamodel.rule.SingleFieldConstraintEBLeftSide;
import org.junit.Test;
import org.kie.soup.project.datamodel.oracle.DataType;
import org.kie.soup.project.datamodel.oracle.MethodInfo;
import org.kie.soup.project.datamodel.oracle.PackageDataModelOracle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleModelDRLPersistenceUnmarshallingTest extends BaseRuleModelTest {

    private static final Logger logger = LoggerFactory.getLogger(RuleModelDRLPersistenceUnmarshallingTest.class);

    @Test
    public void testFactPattern() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant()\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(0,
                     fp.getNumberOfConstraints());
    }

    @Test
    public void testFactPatternWithBinding() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "$a : Applicant()\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());
        assertEquals("$a",
                     fp.getBoundName());

        assertEquals(0,
                     fp.getNumberOfConstraints());
    }

    @Test
    public void testBigIntegerField() {

        addModelField("org.test.Applicant",
                      "reallyLongInteger",
                      "java.math.BigInteger",
                      "BigInteger");

        String drl = "package org.test; \n"
                + "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( reallyLongInteger < 12345I )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint reallyLongInteger = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     reallyLongInteger.getFactType());
        assertEquals("BigInteger",
                     reallyLongInteger.getFieldType());
        assertEquals("reallyLongInteger",
                     reallyLongInteger.getFieldName());
        assertEquals("<",
                     reallyLongInteger.getOperator());
        assertEquals("12345",
                     reallyLongInteger.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     reallyLongInteger.getConstraintValueType());
    }

    @Test
    public void testBigDecimalField() {

        addModelField("org.test.Applicant",
                      "reallyLongDecimal",
                      "java.math.BigDecimal",
                      "BigDecimal");

        String drl = "package org.test; \n"
                + "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( reallyLongDecimal > 54321B )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint reallyLongDecimal = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     reallyLongDecimal.getFactType());
        assertEquals("BigDecimal",
                     reallyLongDecimal.getFieldType());
        assertEquals("reallyLongDecimal",
                     reallyLongDecimal.getFieldName());
        assertEquals(">",
                     reallyLongDecimal.getOperator());
        assertEquals("54321",
                     reallyLongDecimal.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     reallyLongDecimal.getConstraintValueType());
    }

    @Test
    public void testSingleFieldConstraint() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( age < 55 )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("age",
                     sfp.getFieldName());
        assertEquals("<",
                     sfp.getOperator());
        assertEquals("55",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testSingleFieldConstraintWithBinding() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( $a : age < 55 )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("age",
                     sfp.getFieldName());
        assertEquals("<",
                     sfp.getOperator());
        assertEquals("55",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
        assertEquals("$a",
                     sfp.getFieldBinding());
    }

    @Test
    public void testSingleFieldConstraintWithTwoFieldsBinding() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( $a : age, $n : name )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(2,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp0 = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp0.getFactType());
        assertEquals("age",
                     sfp0.getFieldName());
        assertEquals(BaseSingleFieldConstraint.TYPE_UNDEFINED,
                     sfp0.getConstraintValueType());
        assertEquals("$a",
                     sfp0.getFieldBinding());

        assertTrue(fp.getConstraint(1) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp1 = (SingleFieldConstraint) fp.getConstraint(1);
        assertEquals("Applicant",
                     sfp1.getFactType());
        assertEquals("name",
                     sfp1.getFieldName());
        assertEquals(BaseSingleFieldConstraint.TYPE_UNDEFINED,
                     sfp1.getConstraintValueType());
        assertEquals("$n",
                     sfp1.getFieldBinding());
    }

    @Test
    public void testCompositeFieldConstraint() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( age < 55 || age > 70 )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof CompositeFieldConstraint);

        CompositeFieldConstraint cfp = (CompositeFieldConstraint) fp.getConstraint(0);
        assertEquals("||",
                     cfp.getCompositeJunctionType());
        assertEquals(2,
                     cfp.getNumberOfConstraints());
        assertTrue(cfp.getConstraint(0) instanceof SingleFieldConstraint);
        assertTrue(cfp.getConstraint(1) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp1 = (SingleFieldConstraint) cfp.getConstraint(0);
        assertEquals("Applicant",
                     sfp1.getFactType());
        assertEquals("age",
                     sfp1.getFieldName());
        assertEquals("<",
                     sfp1.getOperator());
        assertEquals("55",
                     sfp1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp1.getConstraintValueType());

        SingleFieldConstraint sfp2 = (SingleFieldConstraint) cfp.getConstraint(1);
        assertEquals("Applicant",
                     sfp2.getFactType());
        assertEquals("age",
                     sfp2.getFieldName());
        assertEquals(">",
                     sfp2.getOperator());
        assertEquals("70",
                     sfp2.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp2.getConstraintValueType());
    }

    @Test
    public void testSingleFieldConstraintIsNullOperator() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( age == null )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("age",
                     sfp.getFieldName());
        assertEquals("== null",
                     sfp.getOperator());
        assertNull(sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_UNDEFINED,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testSingleFieldConstraintIsNotNullOperator() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( age != null )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("age",
                     sfp.getFieldName());
        assertEquals("!= null",
                     sfp.getOperator());
        assertNull(sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_UNDEFINED,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testCompositeFieldConstraintWithNotNullOperator() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( age != null && age > 70 )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof CompositeFieldConstraint);

        CompositeFieldConstraint cfp = (CompositeFieldConstraint) fp.getConstraint(0);
        assertEquals("&&",
                     cfp.getCompositeJunctionType());
        assertEquals(2,
                     cfp.getNumberOfConstraints());
        assertTrue(cfp.getConstraint(0) instanceof SingleFieldConstraint);
        assertTrue(cfp.getConstraint(1) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp1 = (SingleFieldConstraint) cfp.getConstraint(0);
        assertEquals("Applicant",
                     sfp1.getFactType());
        assertEquals("age",
                     sfp1.getFieldName());
        assertEquals("!= null",
                     sfp1.getOperator());
        assertNull(sfp1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_UNDEFINED,
                     sfp1.getConstraintValueType());

        SingleFieldConstraint sfp2 = (SingleFieldConstraint) cfp.getConstraint(1);
        assertEquals("Applicant",
                     sfp2.getFactType());
        assertEquals("age",
                     sfp2.getFieldName());
        assertEquals(">",
                     sfp2.getOperator());
        assertEquals("70",
                     sfp2.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp2.getConstraintValueType());
    }

    @Test
    public void testSingleFieldConstraintCEPOperator() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( dob after \"26-Jun-2013\" )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getNumberOfConstraints());
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("dob",
                     sfp.getFieldName());
        assertEquals("after",
                     sfp.getOperator());
        assertEquals("26-Jun-2013",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testSingleFieldConstraintCEPOperator1Parameter() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "$e : Event()\n"
                + "Event( this after[1d] $e )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(2,
                     m.lhs.length);

        IPattern p1 = m.lhs[0];
        assertTrue(p1 instanceof FactPattern);
        FactPattern fp1 = (FactPattern) p1;
        assertEquals("Event",
                     fp1.getFactType());
        assertEquals("$e",
                     fp1.getBoundName());
        assertEquals(0,
                     fp1.getNumberOfConstraints());

        IPattern p2 = m.lhs[1];
        assertTrue(p2 instanceof FactPattern);
        FactPattern fp2 = (FactPattern) p2;
        assertEquals("Event",
                     fp2.getFactType());
        assertNull(fp2.getBoundName());
        assertEquals(1,
                     fp2.getNumberOfConstraints());
        SingleFieldConstraint sfp = (SingleFieldConstraint) fp2.getConstraint(0);
        assertEquals("Event",
                     sfp.getFactType());
        assertEquals("this",
                     sfp.getFieldName());
        assertEquals("after",
                     sfp.getOperator());
        assertEquals("$e",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_VARIABLE,
                     sfp.getConstraintValueType());
        assertEquals(3,
                     sfp.getParameters().size());
        assertEquals("1d",
                     sfp.getParameter("0"));
        assertEquals("1",
                     sfp.getParameter("org.drools.workbench.models.commons.backend.rule.visibleParameterSet"));
        assertEquals("org.drools.workbench.models.commons.backend.rule.CEPOperatorParameterDRLBuilder",
                     sfp.getParameter("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator"));
    }

    @Test
    public void testReciprocal_SingleFieldConstraintCEPOperator1Parameter() {
        //This is the inverse of "SingleFieldConstraintCEPOperator1Parameter"
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$e : Event()\n"
                + "Event( this after[1d] $e )\n"
                + "then\n"
                + "end";

        RuleModel m = new RuleModel();
        m.name = "rule1";

        FactPattern fp1 = new FactPattern();
        fp1.setFactType("Event");
        fp1.setBoundName("$e");

        FactPattern fp2 = new FactPattern();
        fp2.setFactType("Event");

        SingleFieldConstraint sfp = new SingleFieldConstraint();
        sfp.setFactType("Event");
        sfp.setFieldName("this");
        sfp.setOperator("after");
        sfp.setValue("$e");
        sfp.setConstraintValueType(BaseSingleFieldConstraint.TYPE_VARIABLE);
        sfp.getParameters().put("0",
                                "1d");
        sfp.getParameters().put("org.drools.workbench.models.commons.backend.rule.visibleParameterSet",
                                "1");
        sfp.getParameters().put("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator",
                                "org.drools.workbench.models.commons.backend.rule.CEPOperatorParameterDRLBuilder");

        fp2.addConstraint(sfp);
        m.addLhsItem(fp1);
        m.addLhsItem(fp2);

        String actualDrl = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     actualDrl);
    }

    @Test
    public void testSingleFieldConstraintCEPOperator2Parameters() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "$e : Event()\n"
                + "Event( this after[1d, 2d] $e )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(2,
                     m.lhs.length);

        IPattern p1 = m.lhs[0];
        assertTrue(p1 instanceof FactPattern);
        FactPattern fp1 = (FactPattern) p1;
        assertEquals("Event",
                     fp1.getFactType());
        assertEquals("$e",
                     fp1.getBoundName());
        assertEquals(0,
                     fp1.getNumberOfConstraints());

        IPattern p2 = m.lhs[1];
        assertTrue(p2 instanceof FactPattern);
        FactPattern fp2 = (FactPattern) p2;
        assertEquals("Event",
                     fp2.getFactType());
        assertNull(fp2.getBoundName());
        assertEquals(1,
                     fp2.getNumberOfConstraints());
        SingleFieldConstraint sfp = (SingleFieldConstraint) fp2.getConstraint(0);
        assertEquals("Event",
                     sfp.getFactType());
        assertEquals("this",
                     sfp.getFieldName());
        assertEquals("after",
                     sfp.getOperator());
        assertEquals("$e",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_VARIABLE,
                     sfp.getConstraintValueType());
        assertEquals(4,
                     sfp.getParameters().size());
        assertEquals("1d",
                     sfp.getParameter("0"));
        assertEquals("2d",
                     sfp.getParameter("1"));
        assertEquals("2",
                     sfp.getParameter("org.drools.workbench.models.commons.backend.rule.visibleParameterSet"));
        assertEquals("org.drools.workbench.models.commons.backend.rule.CEPOperatorParameterDRLBuilder",
                     sfp.getParameter("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator"));
    }

    @Test
    public void testReciprocal_SingleFieldConstraintCEPOperator2Parameters() {
        //This is the inverse of "SingleFieldConstraintCEPOperator2Parameters"
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$e : Event()\n"
                + "Event( this after[1d, 2d] $e )\n"
                + "then\n"
                + "end";

        RuleModel m = new RuleModel();
        m.name = "rule1";

        FactPattern fp1 = new FactPattern();
        fp1.setFactType("Event");
        fp1.setBoundName("$e");

        FactPattern fp2 = new FactPattern();
        fp2.setFactType("Event");

        SingleFieldConstraint sfp = new SingleFieldConstraint();
        sfp.setFactType("Event");
        sfp.setFieldName("this");
        sfp.setOperator("after");
        sfp.setValue("$e");
        sfp.setConstraintValueType(BaseSingleFieldConstraint.TYPE_VARIABLE);
        sfp.getParameters().put("0",
                                "1d");
        sfp.getParameters().put("1",
                                "2d");
        sfp.getParameters().put("org.drools.workbench.models.commons.backend.rule.visibleParameterSet",
                                "2");
        sfp.getParameters().put("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator",
                                "org.drools.workbench.models.commons.backend.rule.CEPOperatorParameterDRLBuilder");

        fp2.addConstraint(sfp);
        m.addLhsItem(fp1);
        m.addLhsItem(fp2);

        String actualDrl = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     actualDrl);
    }

    @Test
    public void testSingleFieldConstraintCEPOperatorTimeWindow() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Event() over window:time (1d)\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);

        IPattern p1 = m.lhs[0];
        assertTrue(p1 instanceof FactPattern);
        FactPattern fp1 = (FactPattern) p1;
        assertEquals("Event",
                     fp1.getFactType());
        assertNull(fp1.getBoundName());
        assertEquals(0,
                     fp1.getNumberOfConstraints());

        assertNotNull(fp1.getWindow());
        CEPWindow window = fp1.getWindow();
        assertEquals("over window:time",
                     window.getOperator());
        assertEquals(2,
                     window.getParameters().size());
        assertEquals("1d",
                     window.getParameter("1"));
        assertEquals("org.drools.workbench.models.commons.backend.rule.CEPWindowOperatorParameterDRLBuilder",
                     window.getParameter("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator"));
    }

    @Test
    public void testReciprocal_SingleFieldConstraintCEPOperatorTimeWindow() {
        //This is the inverse of "SingleFieldConstraintCEPOperatorTimeWindow"
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Event() over window:time (1d)\n"
                + "then\n"
                + "end";

        RuleModel m = new RuleModel();
        m.name = "rule1";

        FactPattern fp1 = new FactPattern();
        fp1.setFactType("Event");

        CEPWindow window = new CEPWindow();
        window.setOperator("over window:time");
        window.getParameters().put("1",
                                   "1d");
        window.getParameters().put("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator",
                                   "org.drools.workbench.models.commons.backend.rule.CEPWindowOperatorParameterDRLBuilder");
        fp1.setWindow(window);

        m.addLhsItem(fp1);

        String actualDrl = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     actualDrl);
    }

    @Test
    public void testSingleFieldConstraintCEPOperatorTimeLength() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Event() over window:length (10)\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);

        IPattern p1 = m.lhs[0];
        assertTrue(p1 instanceof FactPattern);
        FactPattern fp1 = (FactPattern) p1;
        assertEquals("Event",
                     fp1.getFactType());
        assertNull(fp1.getBoundName());
        assertEquals(0,
                     fp1.getNumberOfConstraints());

        assertNotNull(fp1.getWindow());
        CEPWindow window = fp1.getWindow();
        assertEquals("over window:length",
                     window.getOperator());
        assertEquals(2,
                     window.getParameters().size());
        assertEquals("10",
                     window.getParameter("1"));
        assertEquals("org.drools.workbench.models.commons.backend.rule.CEPWindowOperatorParameterDRLBuilder",
                     window.getParameter("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator"));
    }

    @Test
    public void testReciprocal_SingleFieldConstraintCEPOperatorTimeLength() {
        //This is the inverse of "SingleFieldConstraintCEPOperatorTimeLength"
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Event() over window:length (10)\n"
                + "then\n"
                + "end";

        RuleModel m = new RuleModel();
        m.name = "rule1";

        FactPattern fp1 = new FactPattern();
        fp1.setFactType("Event");

        CEPWindow window = new CEPWindow();
        window.setOperator("over window:length");
        window.getParameters().put("1",
                                   "10");
        window.getParameters().put("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator",
                                   "org.drools.workbench.models.commons.backend.rule.CEPWindowOperatorParameterDRLBuilder");
        fp1.setWindow(window);

        m.addLhsItem(fp1);

        String actualDrl = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     actualDrl);
    }

    @Test
    public void testExtends() {
        String drl = "rule \"rule1\" extends \"rule2\" \n"
                + "when\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals("rule2",
                     m.parentName);
    }

    @Test
    public void testRuleNameWithoutTheQuotes() {
        String drl = "rule rule1\n"
                + "when\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);
    }

    @Test
    public void testMetaData() {
        String drl = "rule rule1\n"
                + "@author( Bob )\n"
                + "when\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals(1,
                     m.metadataList.length);
        assertEquals("author",
                     m.metadataList[0].getAttributeName());
        assertEquals("Bob",
                     m.metadataList[0].getValue());
    }

    @Test
    public void testAttributes() {
        String drl = "rule rule1\n"
                + "salience 42\n"
                + "when\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals(1,
                     m.attributes.length);
        assertEquals("salience",
                     m.attributes[0].getAttributeName());
        assertEquals("42",
                     m.attributes[0].getValue());
    }

    @Test
    public void testEval() {
        String drl = "rule rule1\n"
                + "when\n"
                + "eval( true )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals(1,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof FreeFormLine);
        assertEquals("eval( true )",
                     ((FreeFormLine) m.lhs[0]).getText());
    }
    @Test
    public void testInsertFreeFormLine() {
        String drl = "package com.myspace;\n" +
                "\n" +
                "import java.util.logging.Logger;\n" +
                "\n" +
                "rule \"initialize\"\n" +
                "\tdialect \"mvel\"\n" +
                "\twhen\n" +
                "\tthen\n" +
                "\t\tinsert(Logger log = Logger.getLogger(\"com.somepkg\"));\n" +
                "\t\tinsert(new String(\"hello\"));\n" +
                "end\n";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals(2,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof FreeFormLine);
        assertEquals("insert(Logger log = Logger.getLogger(\"com.somepkg\"));",
                     ((FreeFormLine) m.rhs[0]).getText());
        assertTrue(m.rhs[1] instanceof FreeFormLine);
        assertEquals("insert(new String(\"hello\"));",
                     ((FreeFormLine) m.rhs[1]).getText());
    }

    @Test
    public void testEval2() {
        String drl = "rule rule1\n"
                + "when\n"
                + "Double( eval( functionTrue() && functionFalse()  ) )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals(1,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof FactPattern);
        SingleFieldConstraint constraint = (SingleFieldConstraint) ((FactPattern) m.lhs[0]).getConstraint(0);
        assertEquals("functionTrue() && functionFalse()",
                     constraint.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_PREDICATE,
                     constraint.getConstraintValueType());
    }

    @Test
    public void testLHSFreeFormLine() {
        String drl = "rule rule1\n"
                + "when\n"
                + "//A comment\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertNotNull(m);
        assertEquals(1,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof FreeFormLine);
        assertEquals("//A comment",
                     ((FreeFormLine) m.lhs[0]).getText());
    }

    @Test
    public void testRHSFreeFormLine() {
        String drl = "rule rule1\n"
                + "when\n"
                + "then\n"
                + "int test = (int)(1-0.8);\n"
                + "System.out.println( \"Hello Mario!\" );\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertNotNull(m);
        assertEquals(2,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof FreeFormLine);
        assertEquals("int test = (int)(1-0.8);",
                     ((FreeFormLine) m.rhs[0]).getText());
        assertTrue(m.rhs[1] instanceof FreeFormLine);
        assertEquals("System.out.println( \"Hello Mario!\" );",
                     ((FreeFormLine) m.rhs[1]).getText());
    }

    @Test
    public void testDotInString() {
        String drl = "import java.lang.Number;\n"
                + "import java.util.ArrayList;\n"
                + "rule rule1\n"
                + "when\n"
                + "a : ArrayList( )\n"
                + "bar : Number( )\n"
                + "foo : Number( )\n"
                + "then\n"
                + "a.add( foo + \", \" + bar );\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertNotNull(m);
        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        ActionCallMethod actionCallMethod = (ActionCallMethod) m.rhs[0];
        assertEquals("add",
                     actionCallMethod.getMethodName());
        assertEquals("a",
                     actionCallMethod.getVariable());
        assertEquals(1,
                     actionCallMethod.getFieldValues().length);
        assertEquals("add",
                     actionCallMethod.getFieldValues()[0].getField());
        assertEquals("foo + \", \" + bar",
                     actionCallMethod.getFieldValues()[0].getValue());
    }

    @Test
    public void testDotInStringWithEscapedQuotes() {
        String drl = "import java.lang.Number;\n"
                + "import java.util.ArrayList;\n"
                + "rule rule1\n"
                + "when\n"
                + "a : ArrayList( )\n"
                + "bar : Number( )\n"
                + "foo : Number( )\n"
                + "then\n"
                + "a.add( \"\\\"foo, bar\\\"\" );\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertNotNull(m);
        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        ActionCallMethod actionCallMethod = (ActionCallMethod) m.rhs[0];
        assertEquals("add",
                     actionCallMethod.getMethodName());
        assertEquals("a",
                     actionCallMethod.getVariable());
        assertEquals(1,
                     actionCallMethod.getFieldValues().length);
        assertEquals("add",
                     actionCallMethod.getFieldValues()[0].getField());
        assertEquals("\\\"foo, bar\\\"",
                     actionCallMethod.getFieldValues()[0].getValue());
    }

    @Test
    public void testDotInStringComplex() {
        String drl = "import java.lang.Number;\n"
                + "import java.util.ArrayList;\n"
                + "rule rule1\n"
                + "when\n"
                + "a : ArrayList( )\n"
                + "bar : Number( )\n"
                + "foo : Number( )\n"
                + "then\n"
                + "a.add( bar, foo + \", \" + bar ,  foo);\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertNotNull(m);
        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        ActionCallMethod actionCallMethod = (ActionCallMethod) m.rhs[0];
        assertEquals("add",
                     actionCallMethod.getMethodName());
        assertEquals("a",
                     actionCallMethod.getVariable());
        assertEquals(3,
                     actionCallMethod.getFieldValues().length);
        assertEquals("add",
                     actionCallMethod.getFieldValues()[0].getField());
        assertEquals("bar",
                     actionCallMethod.getFieldValues()[0].getValue());
        assertEquals("add",
                     actionCallMethod.getFieldValues()[1].getField());
        assertEquals("foo + \", \" + bar",
                     actionCallMethod.getFieldValues()[1].getValue());
        assertEquals("add",
                     actionCallMethod.getFieldValues()[2].getField());
        assertEquals("foo",
                     actionCallMethod.getFieldValues()[2].getValue());
    }

    @Test
    public void testLHSFreeFormLineWithDsl() {
        String drl = "rule rule1\n"
                + "when\n"
                + ">//A comment\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                  Collections.emptyList(),
                                                                                  dmo);
        assertNotNull(m);
        assertEquals(1,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof FreeFormLine);
        assertEquals("//A comment",
                     ((FreeFormLine) m.lhs[0]).getText());
    }

    @Test
    public void testRHSFreeFormLineWithDsl() {
        String drl = "rule rule1\n"
                + "when\n"
                + "then\n"
                + ">int test = (int)(1-0.8);\n"
                + ">System.out.println( \"Hello Mario!\" );\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                  Collections.emptyList(),
                                                                                  dmo);
        assertNotNull(m);
        assertEquals(2,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof FreeFormLine);
        assertEquals("int test = (int)(1-0.8);",
                     ((FreeFormLine) m.rhs[0]).getText());
        assertTrue(m.rhs[1] instanceof FreeFormLine);
        assertEquals("System.out.println( \"Hello Mario!\" );",
                     ((FreeFormLine) m.rhs[1]).getText());
    }

    @Test
    public void testVarAssignment() {
        String drl = "rule rule1\n"
                + "when\n"
                + " d : Double()\n"
                + "then\n"
                + "double test = d.doubleValue();\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertNotNull(m);
        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof FreeFormLine);
        assertEquals("double test = d.doubleValue();",
                     ((FreeFormLine) m.rhs[0]).getText());
    }

    @Test
    public void testRHSOrder() {
        String drl =
                "rule \"Low Down Payment based on Appraisal\"\n" +
                        "  dialect \"mvel\"\n" +
                        "  ruleflow-group \"apr-calculation\"\n" +
                        "  salience -3\n" +
                        "  no-loop true\n" +
                        " when\n" +
                        "    appraised : Appraisal( )\n" +
                        "    application : Application( mortgageAmount > ( appraised.value * 8 / 10 ) )\n" +
                        " then\n" +
                        "    double ratio = application.getMortgageAmount().doubleValue() / appraised.getValue().doubleValue();\n" +
                        "    int brackets = (int)((ratio - 0.8) / 0.05);\n" +
                        "    brackets++;\n" +
                        "    double aprSurcharge = 0.75 * brackets;\n" +
                        "    System.out.println( \"aprSurcharge added is \" + aprSurcharge );\n" +
                        "    application.setApr(  application.getApr() + aprSurcharge );\n" +
                        "    System.out.println(\"Executed Rule: \" + drools.getRule().getName() );\n" +
                        "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertNotNull(m);
        assertEquals(7,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof FreeFormLine);
        assertTrue(m.rhs[1] instanceof FreeFormLine);
        assertTrue(m.rhs[2] instanceof FreeFormLine);
        assertTrue(m.rhs[3] instanceof FreeFormLine);
        assertTrue(m.rhs[4] instanceof FreeFormLine);
        assertTrue(m.rhs[5] instanceof ActionSetField);
        assertTrue(m.rhs[6] instanceof FreeFormLine);
    }

    @Test
    public void testNestedFieldExpressions() {
        String drl =
                "rule rule1\n"
                        + "when\n"
                        + "Person( address.postalCode == 12345 )\n"
                        + "then\n"
                        + "end";

        addModelField("org.test.Person",
                      "address",
                      "org.test.Address",
                      "Address");
        addModelField("org.test.Address",
                      "postalCode",
                      "java.lang.Integer",
                      "Integer");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals(1,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof FactPattern);
        assertTrue(((FactPattern) m.lhs[0]).getFieldConstraints()[0] instanceof SingleFieldConstraintEBLeftSide);

        SingleFieldConstraintEBLeftSide ebLeftSide = (SingleFieldConstraintEBLeftSide) ((FactPattern) m.lhs[0]).getFieldConstraints()[0];
        assertEquals("postalCode",
                     ebLeftSide.getFieldName());
        assertEquals("java.lang.Integer",
                     ebLeftSide.getFieldType());
        assertEquals("==",
                     ebLeftSide.getOperator());
        assertEquals("12345",
                     ebLeftSide.getValue());

        assertEquals(3,
                     ebLeftSide.getExpressionLeftSide().getParts().size());
        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        ExpressionUnboundFact expressionUnboundFact = ((ExpressionUnboundFact) ebLeftSide.getExpressionLeftSide().getParts().get(0));
        assertEquals("Person",
                     expressionUnboundFact.getName());
        assertEquals("Person",
                     expressionUnboundFact.getClassType());
        assertEquals("Person",
                     expressionUnboundFact.getGenericType());
        assertEquals(((FactPattern) m.lhs[0]).getFactType(),
                     expressionUnboundFact.getFactType());

        assertNull(expressionUnboundFact.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionUnboundFact.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(1) instanceof ExpressionField);
        ExpressionField expressionField1 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(1);
        assertEquals("address",
                     expressionField1.getName());
        assertEquals("org.test.Address",
                     expressionField1.getClassType());
        assertEquals("Address",
                     expressionField1.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(0),
                     expressionField1.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(2),
                     expressionField1.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(2) instanceof ExpressionField);
        ExpressionField expressionField2 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(2);
        assertEquals("postalCode",
                     expressionField2.getName());
        assertEquals("java.lang.Integer",
                     expressionField2.getClassType());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     expressionField2.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionField2.getPrevious());
        assertNull(expressionField2.getNext());
    }

    @Test
    public void testNestedFieldExpressionsWithAFunction() {
        String drl =
                "rule rule1\n"
                        + "when\n"
                        + "Person( address.postalCode == myFunction() )\n"
                        + "then\n"
                        + "end";

        addModelField("org.test.Person",
                      "address",
                      "org.test.Address",
                      "Address");
        addModelField("org.test.Address",
                      "postalCode",
                      "java.lang.Integer",
                      "Integer");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals(1,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof FactPattern);
        assertTrue(((FactPattern) m.lhs[0]).getFieldConstraints()[0] instanceof SingleFieldConstraintEBLeftSide);

        SingleFieldConstraintEBLeftSide ebLeftSide = (SingleFieldConstraintEBLeftSide) ((FactPattern) m.lhs[0]).getFieldConstraints()[0];
        assertEquals("postalCode",
                     ebLeftSide.getFieldName());
        assertEquals("java.lang.Integer",
                     ebLeftSide.getFieldType());
        assertEquals("==",
                     ebLeftSide.getOperator());
        assertEquals("myFunction()",
                     ebLeftSide.getValue());

        assertEquals(3,
                     ebLeftSide.getExpressionLeftSide().getParts().size());
        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        ExpressionUnboundFact expressionUnboundFact = ((ExpressionUnboundFact) ebLeftSide.getExpressionLeftSide().getParts().get(0));
        assertEquals("Person",
                     expressionUnboundFact.getName());
        assertEquals("Person",
                     expressionUnboundFact.getClassType());
        assertEquals("Person",
                     expressionUnboundFact.getGenericType());
        assertEquals(((FactPattern) m.lhs[0]).getFactType(),
                     expressionUnboundFact.getFactType());

        assertNull(expressionUnboundFact.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionUnboundFact.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(1) instanceof ExpressionField);
        ExpressionField expressionField1 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(1);
        assertEquals("address",
                     expressionField1.getName());
        assertEquals("org.test.Address",
                     expressionField1.getClassType());
        assertEquals("Address",
                     expressionField1.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(0),
                     expressionField1.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(2),
                     expressionField1.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(2) instanceof ExpressionField);
        ExpressionField expressionField2 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(2);
        assertEquals("postalCode",
                     expressionField2.getName());
        assertEquals("java.lang.Integer",
                     expressionField2.getClassType());
        assertEquals("Integer",
                     expressionField2.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionField2.getPrevious());
        assertNull(expressionField2.getNext());
    }

    @Test
    public void testNestedFieldExpressionsWithAnotherExpression() {
        String drl =
                "rule rule1\n"
                        + "when\n"
                        + "p : Person( address.postalCode == p.address.postalCode )\n"
                        + "then\n"
                        + "end";

        addModelField("org.test.Person",
                      "address",
                      "org.test.Address",
                      "Address");
        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Address",
                      "postalCode",
                      "java.lang.Integer",
                      "Integer");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals(1,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof FactPattern);
        assertTrue(((FactPattern) m.lhs[0]).getFieldConstraints()[0] instanceof SingleFieldConstraintEBLeftSide);

        SingleFieldConstraintEBLeftSide ebLeftSide = (SingleFieldConstraintEBLeftSide) ((FactPattern) m.lhs[0]).getFieldConstraints()[0];
        assertEquals("postalCode",
                     ebLeftSide.getFieldName());
        assertEquals("java.lang.Integer",
                     ebLeftSide.getFieldType());
        assertEquals("==",
                     ebLeftSide.getOperator());
        assertEquals("",
                     ebLeftSide.getValue());

        assertEquals(3,
                     ebLeftSide.getExpressionValue().getParts().size());

        assertTrue(ebLeftSide.getExpressionValue().getParts().get(0) instanceof ExpressionVariable);
        ExpressionVariable expressionVariable = (ExpressionVariable) ebLeftSide.getExpressionValue().getParts().get(0);
        assertEquals("p",
                     expressionVariable.getName());
        assertEquals("org.test.Person",
                     expressionVariable.getClassType());
        assertEquals(DataType.TYPE_THIS,
                     expressionVariable.getGenericType());

        assertTrue(ebLeftSide.getExpressionValue().getParts().get(1) instanceof ExpressionField);
        ExpressionField ef1 = (ExpressionField) ebLeftSide.getExpressionValue().getParts().get(1);
        assertEquals("address",
                     ef1.getName());
        assertEquals("org.test.Address",
                     ef1.getClassType());
        assertEquals("Address",
                     ef1.getGenericType());

        assertTrue(ebLeftSide.getExpressionValue().getParts().get(2) instanceof ExpressionField);
        ExpressionField ef2 = (ExpressionField) ebLeftSide.getExpressionValue().getParts().get(2);
        assertEquals("postalCode",
                     ef2.getName());
        assertEquals("java.lang.Integer",
                     ef2.getClassType());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     ef2.getGenericType());

        assertEquals(3,
                     ebLeftSide.getExpressionLeftSide().getParts().size());
        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        ExpressionUnboundFact expressionUnboundFact = ((ExpressionUnboundFact) ebLeftSide.getExpressionLeftSide().getParts().get(0));
        assertEquals("Person",
                     expressionUnboundFact.getName());
        assertEquals("Person",
                     expressionUnboundFact.getClassType());
        assertEquals("Person",
                     expressionUnboundFact.getGenericType());
        assertEquals(((FactPattern) m.lhs[0]).getFactType(),
                     expressionUnboundFact.getFactType());

        assertNull(expressionUnboundFact.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionUnboundFact.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(1) instanceof ExpressionField);
        ExpressionField expressionField1 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(1);
        assertEquals("address",
                     expressionField1.getName());
        assertEquals("org.test.Address",
                     expressionField1.getClassType());
        assertEquals("Address",
                     expressionField1.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(0),
                     expressionField1.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(2),
                     expressionField1.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(2) instanceof ExpressionField);
        ExpressionField expressionField2 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(2);
        assertEquals("postalCode",
                     expressionField2.getName());
        assertEquals("java.lang.Integer",
                     expressionField2.getClassType());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     expressionField2.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionField2.getPrevious());
        assertNull(expressionField2.getNext());
    }

    @Test
    public void testSingleFieldConstraintContainsOperator() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "$is : IncomeSource( )\n"
                + "Applicant( incomes contains $is )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(2,
                     m.lhs.length);
        IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);

        FactPattern fp0 = (FactPattern) p0;
        assertEquals("IncomeSource",
                     fp0.getFactType());
        assertEquals("$is",
                     fp0.getBoundName());

        IPattern p1 = m.lhs[1];
        assertTrue(p1 instanceof FactPattern);

        FactPattern fp1 = (FactPattern) p1;
        assertEquals("Applicant",
                     fp1.getFactType());

        assertEquals(0,
                     fp0.getNumberOfConstraints());

        assertEquals(1,
                     fp1.getNumberOfConstraints());
        assertTrue(fp1.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp1.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("incomes",
                     sfp.getFieldName());
        assertEquals("contains",
                     sfp.getOperator());
        assertEquals("$is",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_VARIABLE,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testCompositeFactPatternWithOr() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "( Person( age == 42 ) or Person( age == 43 ) )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof CompositeFactPattern);
        CompositeFactPattern cfp = (CompositeFactPattern) p;
        assertEquals(CompositeFactPattern.COMPOSITE_TYPE_OR,
                     cfp.getType());

        //LHS sub-patterns
        assertEquals(2,
                     cfp.getPatterns().length);
        IPattern cfp_p1 = cfp.getPatterns()[0];
        assertTrue(cfp_p1 instanceof FactPattern);
        FactPattern fp1 = (FactPattern) cfp_p1;
        assertEquals("Person",
                     fp1.getFactType());
        assertEquals(1,
                     fp1.getConstraintList().getConstraints().length);
        assertTrue(fp1.getConstraint(0) instanceof SingleFieldConstraint);
        SingleFieldConstraint cfp_sfp1 = (SingleFieldConstraint) fp1.getConstraint(0);
        assertEquals("Person",
                     cfp_sfp1.getFactType());
        assertEquals("age",
                     cfp_sfp1.getFieldName());
        assertEquals("==",
                     cfp_sfp1.getOperator());
        assertEquals("42",
                     cfp_sfp1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cfp_sfp1.getConstraintValueType());

        IPattern cfp_p2 = cfp.getPatterns()[1];
        assertTrue(cfp_p2 instanceof FactPattern);
        FactPattern fp2 = (FactPattern) cfp_p2;
        assertEquals("Person",
                     fp2.getFactType());
        assertTrue(fp2.getConstraint(0) instanceof SingleFieldConstraint);
        SingleFieldConstraint cfp_sfp2 = (SingleFieldConstraint) fp2.getConstraint(0);
        assertEquals("Person",
                     cfp_sfp2.getFactType());
        assertEquals("age",
                     cfp_sfp2.getFieldName());
        assertEquals("==",
                     cfp_sfp2.getOperator());
        assertEquals("43",
                     cfp_sfp2.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cfp_sfp2.getConstraintValueType());
    }

    @Test
    public void testReciprocal_CompositeFactPatternWithOr() {
        //This is the inverse of "CompositeFactPatternWithOr"
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "( Person( age == 42 ) or Person( age == 43 ) )\n"
                + "then\n"
                + "end";

        RuleModel m = new RuleModel();
        m.name = "rule1";

        //LHS Patterns
        CompositeFactPattern cfp = new CompositeFactPattern();
        cfp.setType(CompositeFactPattern.COMPOSITE_TYPE_OR);

        //LHS sub-patterns
        FactPattern fp1 = new FactPattern();
        fp1.setFactType("Person");

        SingleFieldConstraint cfp_sfp1 = new SingleFieldConstraint();
        cfp_sfp1.setFactType("Person");
        cfp_sfp1.setFieldName("age");
        cfp_sfp1.setOperator("==");
        cfp_sfp1.setValue("42");
        cfp_sfp1.setFieldType(DataType.TYPE_NUMERIC_INTEGER);
        cfp_sfp1.setConstraintValueType(BaseSingleFieldConstraint.TYPE_LITERAL);
        fp1.addConstraint(cfp_sfp1);
        cfp.addFactPattern(fp1);

        FactPattern fp2 = new FactPattern();
        fp2.setFactType("Person");

        SingleFieldConstraint cfp_sfp2 = new SingleFieldConstraint();
        cfp_sfp2.setFactType("Person");
        cfp_sfp2.setFieldName("age");
        cfp_sfp2.setOperator("==");
        cfp_sfp2.setValue("43");
        cfp_sfp2.setFieldType(DataType.TYPE_NUMERIC_INTEGER);
        cfp_sfp2.setConstraintValueType(BaseSingleFieldConstraint.TYPE_LITERAL);
        fp2.addConstraint(cfp_sfp2);
        cfp.addFactPattern(fp2);

        m.addLhsItem(cfp);

        String actualDrl = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     actualDrl);
    }

    @Test
    public void testCompositeFactPatternWithOrAndCompositeFieldConstraint() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "( Person( age == 42 ) or Person( age == 43 || age == 44) )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof CompositeFactPattern);

        CompositeFactPattern cfp = (CompositeFactPattern) p;
        assertEquals(CompositeFactPattern.COMPOSITE_TYPE_OR,
                     cfp.getType());

        //LHS sub-patterns
        assertEquals(2,
                     cfp.getPatterns().length);
        IPattern cfp_p1 = cfp.getPatterns()[0];
        assertTrue(cfp_p1 instanceof FactPattern);
        FactPattern fp1 = (FactPattern) cfp_p1;
        assertEquals("Person",
                     fp1.getFactType());
        assertEquals(1,
                     fp1.getConstraintList().getConstraints().length);
        assertTrue(fp1.getConstraint(0) instanceof SingleFieldConstraint);
        SingleFieldConstraint cfp_sfp1 = (SingleFieldConstraint) fp1.getConstraint(0);
        assertEquals("Person",
                     cfp_sfp1.getFactType());
        assertEquals("age",
                     cfp_sfp1.getFieldName());
        assertEquals("==",
                     cfp_sfp1.getOperator());
        assertEquals("42",
                     cfp_sfp1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cfp_sfp1.getConstraintValueType());

        IPattern cfp_p2 = cfp.getPatterns()[1];
        assertTrue(cfp_p2 instanceof FactPattern);
        FactPattern fp2 = (FactPattern) cfp_p2;
        assertEquals(1,
                     fp2.getConstraintList().getConstraints().length);
        assertTrue(fp2.getConstraint(0) instanceof CompositeFieldConstraint);

        CompositeFieldConstraint cfp_p2_cfp = (CompositeFieldConstraint) fp2.getConstraint(0);
        assertEquals("||",
                     cfp_p2_cfp.getCompositeJunctionType());
        assertEquals(2,
                     cfp_p2_cfp.getNumberOfConstraints());
        assertTrue(cfp_p2_cfp.getConstraint(0) instanceof SingleFieldConstraint);
        assertTrue(cfp_p2_cfp.getConstraint(1) instanceof SingleFieldConstraint);

        SingleFieldConstraint cfp_p2_sfp1 = (SingleFieldConstraint) cfp_p2_cfp.getConstraint(0);
        assertEquals("Person",
                     cfp_p2_sfp1.getFactType());
        assertEquals("age",
                     cfp_p2_sfp1.getFieldName());
        assertEquals("==",
                     cfp_p2_sfp1.getOperator());
        assertEquals("43",
                     cfp_p2_sfp1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cfp_p2_sfp1.getConstraintValueType());

        SingleFieldConstraint cfp_p2_sfp2 = (SingleFieldConstraint) cfp_p2_cfp.getConstraint(1);
        assertEquals("Person",
                     cfp_p2_sfp2.getFactType());
        assertEquals("age",
                     cfp_p2_sfp2.getFieldName());
        assertEquals("==",
                     cfp_p2_sfp2.getOperator());
        assertEquals("44",
                     cfp_p2_sfp2.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cfp_p2_sfp2.getConstraintValueType());
    }

    @Test
    public void testReciprocal_CompositeFactPatternWithOrAndCompositeFieldConstraint() {
        //This is the inverse of "CompositeFactPatternWithOrAndCompositeFieldConstraint"
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "( Person( age == 42 ) or Person( age == 43 || age == 44) )\n"
                + "then\n"
                + "end";

        RuleModel m = new RuleModel();
        m.name = "rule1";

        //LHS Pattern
        CompositeFactPattern cfp = new CompositeFactPattern();
        cfp.setType(CompositeFactPattern.COMPOSITE_TYPE_OR);

        //LHS sub-patterns
        FactPattern fp1 = new FactPattern();
        fp1.setFactType("Person");

        SingleFieldConstraint fp1_sfp1 = new SingleFieldConstraint();
        fp1_sfp1.setFactType("Person");
        fp1_sfp1.setFieldName("age");
        fp1_sfp1.setOperator("==");
        fp1_sfp1.setValue("42");
        fp1_sfp1.setFieldType(DataType.TYPE_NUMERIC_INTEGER);
        fp1_sfp1.setConstraintValueType(BaseSingleFieldConstraint.TYPE_LITERAL);
        fp1.addConstraint(fp1_sfp1);

        FactPattern fp2 = new FactPattern();
        fp2.setFactType("Person");

        CompositeFieldConstraint fp2_cfp = new CompositeFieldConstraint();
        fp2_cfp.setCompositeJunctionType(CompositeFieldConstraint.COMPOSITE_TYPE_OR);
        fp2.addConstraint(fp2_cfp);

        SingleFieldConstraint fp2_sfp1 = new SingleFieldConstraint();
        fp2_sfp1.setFactType("Person");
        fp2_sfp1.setFieldName("age");
        fp2_sfp1.setOperator("==");
        fp2_sfp1.setValue("43");
        fp2_sfp1.setFieldType(DataType.TYPE_NUMERIC_INTEGER);
        fp2_sfp1.setConstraintValueType(BaseSingleFieldConstraint.TYPE_LITERAL);
        fp2_cfp.addConstraint(fp2_sfp1);

        SingleFieldConstraint fp2_sfp2 = new SingleFieldConstraint();
        fp2_sfp2.setFactType("Person");
        fp2_sfp2.setFieldName("age");
        fp2_sfp2.setOperator("==");
        fp2_sfp2.setValue("44");
        fp2_sfp2.setFieldType(DataType.TYPE_NUMERIC_INTEGER);
        fp2_sfp2.setConstraintValueType(BaseSingleFieldConstraint.TYPE_LITERAL);
        fp2_cfp.addConstraint(fp2_sfp2);

        cfp.addFactPattern(fp1);
        cfp.addFactPattern(fp2);
        m.addLhsItem(cfp);

        String actualDrl = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     actualDrl);
    }

    @Test
    public void testNestedFieldConstraints() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "ParentType( this != null, this.parentChildField != null, this.parentChildField.childField == \"hello\" )\n"
                + "then\n"
                + "end";

        addModelField("org.test.ParentType",
                      "parentChildField",
                      "org.test.ChildType",
                      "ChildType");
        addModelField("org.test.ChildType",
                      "childField",
                      "java.lang.String",
                      "String");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("ParentType",
                     fp.getFactType());

        assertEquals(3,
                     fp.getConstraintList().getConstraints().length);

        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);
        SingleFieldConstraint sfp0 = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("ParentType",
                     sfp0.getFactType());
        assertEquals("this",
                     sfp0.getFieldName());
        assertEquals(DataType.TYPE_THIS,
                     sfp0.getFieldType());
        assertEquals("!= null",
                     sfp0.getOperator());
        assertNull(sfp0.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_UNDEFINED,
                     sfp0.getConstraintValueType());
        assertNull(sfp0.getParent());

        assertTrue(fp.getConstraint(1) instanceof SingleFieldConstraintEBLeftSide);
        SingleFieldConstraintEBLeftSide sfp1 = (SingleFieldConstraintEBLeftSide) fp.getConstraint(1);
        assertEquals("ParentType",
                     sfp1.getFactType());
        assertEquals("parentChildField",
                     sfp1.getFieldName());
        assertEquals("org.test.ChildType",
                     sfp1.getFieldType());
        assertEquals("!= null",
                     sfp1.getOperator());
        assertNull(sfp1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_UNDEFINED,
                     sfp1.getConstraintValueType());
        assertNull(sfp1.getParent());

        assertTrue(fp.getConstraint(2) instanceof SingleFieldConstraintEBLeftSide);
        SingleFieldConstraintEBLeftSide sfp2 = (SingleFieldConstraintEBLeftSide) fp.getConstraint(2);
        assertEquals("childField",
                     sfp2.getFieldName());
        assertEquals("java.lang.String",
                     sfp2.getFieldType());
        assertEquals("==",
                     sfp2.getOperator());
        assertEquals("hello",
                     sfp2.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp2.getConstraintValueType());
        assertNull(sfp2.getParent());
    }

    @Test
    public void testReciprocal_NestedFieldConstraints() {
        //This is the inverse of "NestedFieldConstraints"
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "ParentType( this != null, this.parentChildField != null, this.parentChildField.childField == \"hello\" )\n"
                + "then\n"
                + "end";

        RuleModel m = new RuleModel();
        m.name = "rule1";

        //LHS Pattern
        FactPattern fp1 = new FactPattern();
        fp1.setFactType("ParentType");

        SingleFieldConstraint fp1_sfp1 = new SingleFieldConstraint();
        fp1_sfp1.setFactType("ParentType");
        fp1_sfp1.setFieldName("this");
        fp1_sfp1.setFieldType(DataType.TYPE_THIS);
        fp1_sfp1.setOperator("!= null");
        fp1_sfp1.setConstraintValueType(BaseSingleFieldConstraint.TYPE_UNDEFINED);
        fp1.addConstraint(fp1_sfp1);

        SingleFieldConstraint fp1_sfp2 = new SingleFieldConstraint();
        fp1_sfp2.setFactType("ParentType");
        fp1_sfp2.setFieldName("parentChildField");
        fp1_sfp2.setFieldType("ChildType");
        fp1_sfp2.setOperator("!= null");
        fp1_sfp2.setConstraintValueType(BaseSingleFieldConstraint.TYPE_UNDEFINED);
        fp1.addConstraint(fp1_sfp2);
        fp1_sfp2.setParent(fp1_sfp1);

        SingleFieldConstraint fp1_sfp3 = new SingleFieldConstraint();
        fp1_sfp3.setFactType("ChildType");
        fp1_sfp3.setFieldName("childField");
        fp1_sfp3.setFieldType(DataType.TYPE_STRING);
        fp1_sfp3.setOperator("==");
        fp1_sfp3.setValue("hello");
        fp1_sfp3.setConstraintValueType(BaseSingleFieldConstraint.TYPE_LITERAL);
        fp1.addConstraint(fp1_sfp3);
        fp1_sfp3.setParent(fp1_sfp2);

        m.addLhsItem(fp1);

        String actualDrl = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     actualDrl);
    }

    @Test
    public void testNestedFieldConstraintsAsExpression() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Person( contact.telephone > 12345 )\n"
                + "then\n"
                + "end";

        addModelField("org.test.Person",
                      "contact",
                      "org.test.Contact",
                      "Contact");
        addModelField("org.test.Contact",
                      "telephone",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);

        assertTrue(((FactPattern) m.lhs[0]).getFieldConstraints()[0] instanceof SingleFieldConstraintEBLeftSide);

        SingleFieldConstraintEBLeftSide ebLeftSide = (SingleFieldConstraintEBLeftSide) ((FactPattern) m.lhs[0]).getFieldConstraints()[0];
        assertEquals("telephone",
                     ebLeftSide.getFieldName());
        assertEquals("java.lang.Integer",
                     ebLeftSide.getFieldType());
        assertEquals(">",
                     ebLeftSide.getOperator());
        assertEquals("12345",
                     ebLeftSide.getValue());

        assertEquals(3,
                     ebLeftSide.getExpressionLeftSide().getParts().size());
        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        ExpressionUnboundFact expressionUnboundFact = ((ExpressionUnboundFact) ebLeftSide.getExpressionLeftSide().getParts().get(0));
        assertEquals("Person",
                     expressionUnboundFact.getName());
        assertEquals("Person",
                     expressionUnboundFact.getClassType());
        assertEquals("Person",
                     expressionUnboundFact.getGenericType());
        assertEquals(((FactPattern) m.lhs[0]).getFactType(),
                     expressionUnboundFact.getFactType());

        assertEquals(null,
                     expressionUnboundFact.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionUnboundFact.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(1) instanceof ExpressionField);
        ExpressionField expressionField1 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(1);
        assertEquals("contact",
                     expressionField1.getName());
        assertEquals("org.test.Contact",
                     expressionField1.getClassType());
        assertEquals("Contact",
                     expressionField1.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(0),
                     expressionField1.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(2),
                     expressionField1.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(2) instanceof ExpressionField);
        ExpressionField expressionField2 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(2);
        assertEquals("telephone",
                     expressionField2.getName());
        assertEquals("java.lang.Integer",
                     expressionField2.getClassType());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     expressionField2.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionField2.getPrevious());
        assertNull(expressionField2.getNext());
    }

    @Test
    public void testNestedFieldConstraintsOnlyLeafOperator() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "ParentType( parentChildField.childField == \"hello\" )\n"
                + "then\n"
                + "end";

        addModelField("org.test.ParentType",
                      "parentChildField",
                      "org.test.ChildType",
                      "ChildType");
        addModelField("org.test.ChildType",
                      "childField",
                      "java.lang.String",
                      "String");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals(1,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof FactPattern);
        assertTrue(((FactPattern) m.lhs[0]).getFieldConstraints()[0] instanceof SingleFieldConstraintEBLeftSide);

        SingleFieldConstraintEBLeftSide ebLeftSide = (SingleFieldConstraintEBLeftSide) ((FactPattern) m.lhs[0]).getFieldConstraints()[0];
        assertEquals("childField",
                     ebLeftSide.getFieldName());
        assertEquals("java.lang.String",
                     ebLeftSide.getFieldType());
        assertEquals("==",
                     ebLeftSide.getOperator());
        assertEquals("hello",
                     ebLeftSide.getValue());

        assertEquals(3,
                     ebLeftSide.getExpressionLeftSide().getParts().size());
        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        ExpressionUnboundFact expressionUnboundFact = ((ExpressionUnboundFact) ebLeftSide.getExpressionLeftSide().getParts().get(0));
        assertEquals("ParentType",
                     expressionUnboundFact.getName());
        assertEquals("ParentType",
                     expressionUnboundFact.getClassType());
        assertEquals("ParentType",
                     expressionUnboundFact.getGenericType());
        assertEquals(((FactPattern) m.lhs[0]).getFactType(),
                     expressionUnboundFact.getFactType());

        assertNull(expressionUnboundFact.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionUnboundFact.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(1) instanceof ExpressionField);
        ExpressionField expressionField1 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(1);
        assertEquals("parentChildField",
                     expressionField1.getName());
        assertEquals("org.test.ChildType",
                     expressionField1.getClassType());
        assertEquals("ChildType",
                     expressionField1.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(0),
                     expressionField1.getPrevious());
        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(2),
                     expressionField1.getNext());

        assertTrue(ebLeftSide.getExpressionLeftSide().getParts().get(2) instanceof ExpressionField);
        ExpressionField expressionField2 = (ExpressionField) ebLeftSide.getExpressionLeftSide().getParts().get(2);
        assertEquals("childField",
                     expressionField2.getName());
        assertEquals("java.lang.String",
                     expressionField2.getClassType());
        assertEquals("String",
                     expressionField2.getGenericType());

        assertEquals(ebLeftSide.getExpressionLeftSide().getParts().get(1),
                     expressionField2.getPrevious());
        assertNull(expressionField2.getNext());
    }

    @Test
    public void testReciprocal_NestedFieldConstraintsOnlyLeafOperator() {
        //This is the inverse of "NestedFieldConstraintsOnlyLeafOperator"
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "ParentType( this.parentChildField.childField == \"hello\" )\n"
                + "then\n"
                + "end";

        RuleModel m = new RuleModel();
        m.name = "rule1";

        //LHS Pattern
        FactPattern fp1 = new FactPattern();
        fp1.setFactType("ParentType");

        SingleFieldConstraint fp1_sfp1 = new SingleFieldConstraint();
        fp1_sfp1.setFactType("ParentType");
        fp1_sfp1.setFieldName("this");
        fp1_sfp1.setFieldType(DataType.TYPE_THIS);
        fp1_sfp1.setConstraintValueType(BaseSingleFieldConstraint.TYPE_UNDEFINED);
        fp1.addConstraint(fp1_sfp1);

        SingleFieldConstraint fp1_sfp2 = new SingleFieldConstraint();
        fp1_sfp2.setFactType("ParentType");
        fp1_sfp2.setFieldName("parentChildField");
        fp1_sfp2.setFieldType("ChildType");
        fp1_sfp2.setConstraintValueType(BaseSingleFieldConstraint.TYPE_UNDEFINED);
        fp1.addConstraint(fp1_sfp2);
        fp1_sfp2.setParent(fp1_sfp1);

        SingleFieldConstraint fp1_sfp3 = new SingleFieldConstraint();
        fp1_sfp3.setFactType("ChildType");
        fp1_sfp3.setFieldName("childField");
        fp1_sfp3.setFieldType(DataType.TYPE_STRING);
        fp1_sfp3.setOperator("==");
        fp1_sfp3.setValue("hello");
        fp1_sfp3.setConstraintValueType(BaseSingleFieldConstraint.TYPE_LITERAL);
        fp1.addConstraint(fp1_sfp3);
        fp1_sfp3.setParent(fp1_sfp2);

        m.addLhsItem(fp1);

        String actualDrl = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     actualDrl);
    }

    @Test
    public void testCalendarsAttribute() {
        String drl = "rule \"rule1\"\n"
                + "calendars \"myCalendar\", \"Yet Another Calendar\"\n"
                + "when\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertEquals(1,
                     m.attributes.length);
        assertEquals("calendars",
                     m.attributes[0].getAttributeName());
        assertEquals("myCalendar, Yet Another Calendar",
                     m.attributes[0].getValue());
    }

    @Test
    public void testFunctionCall() {
        // BZ-1013682
        String drl = "" +
                "package org.mortgages;\n" +
                "import org.mortgages.LoanApplication;\n" +
                "\n" +
                "rule \"my rule\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    a : LoanApplication( )\n" +
                "  then\n" +
                "    keke.clear(  );\n" +
                "end\n";

        HashMap<String, String> globals = new HashMap<>();
        globals.put("keke",
                    "java.util.ArrayList");

        when(
                dmo.getPackageGlobals()
        ).thenReturn(
                globals
        );

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        ActionCallMethod actionGlobalCollectionAdd = (ActionCallMethod) m.rhs[0];
        assertEquals("clear",
                     actionGlobalCollectionAdd.getMethodName());
        assertEquals("keke",
                     actionGlobalCollectionAdd.getVariable());
        assertEquals(1,
                     actionGlobalCollectionAdd.getState());
        assertEquals(0,
                     actionGlobalCollectionAdd.getFieldValues().length);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testMethodCall() {
        // BZ-1042511
        String drl = "" +
                "package org.mortgages;\n" +
                "import org.mortgages.LoanApplication;\n" +
                "import java.util.Map;\n" +
                "rule \"my rule\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    a : LoanApplication( )\n" +
                "    m : Map()\n" +
                "  then\n" +
                "    m.put(\"key\", a );\n" +
                "end\n";

        HashMap<String, String> globals = new HashMap<>();

        when(dmo.getPackageGlobals()).thenReturn(globals);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertEquals(2,
                     m.getImports().getImports().size());

        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        ActionCallMethod mc = (ActionCallMethod) m.rhs[0];
        assertEquals("put",
                     mc.getMethodName());
        assertEquals("m",
                     mc.getVariable());
        assertEquals(1,
                     mc.getState());
        assertEquals(2,
                     mc.getFieldValues().length);

        ActionFieldValue f1 = mc.getFieldValue(0);
        assertEquals("key",
                     f1.getValue());
        ActionFieldValue f2 = mc.getFieldValue(1);
        assertEquals("a",
                     f2.getValue());

        String marshalled = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        logger.debug(marshalled);

        assertEqualsIgnoreWhitespace(drl,
                                     marshalled);
    }

    @Test
    public void testMethodCallCheckParameterDataTypes1() {
        // BZ-1045423
        String drl = "" +
                "package org.mortgages;\n" +
                "import org.mortgages.LoanApplication;\n" +
                "import java.util.Map;\n" +
                "rule \"my rule\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    a : LoanApplication( )\n" +
                "    m : Map()\n" +
                "  then\n" +
                "    m.put(\"key\", a );\n" +
                "end\n";

        Map<String, List<MethodInfo>> methodInformation = new HashMap<>();
        List<MethodInfo> mapMethodInformation = new ArrayList<>();
        mapMethodInformation.add(new MethodInfo("put",
                                                Arrays.asList("java.lang.Object",
                                                              "java.lang.Object"),
                                                "void",
                                                "void",
                                                "java.util.Map"));

        methodInformation.put("java.util.Map",
                              mapMethodInformation);

        when(dmo.getModuleMethodInformation()).thenReturn(methodInformation);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        ActionCallMethod mc = (ActionCallMethod) m.rhs[0];
        assertEquals("put",
                     mc.getMethodName());
        assertEquals("m",
                     mc.getVariable());
        assertEquals(1,
                     mc.getState());
        assertEquals(2,
                     mc.getFieldValues().length);

        ActionFieldValue f1 = mc.getFieldValue(0);
        assertEquals("\"key\"",
                     f1.getValue());
        assertEquals("java.lang.Object",
                     f1.getType());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     f1.getNature());
        ActionFieldValue f2 = mc.getFieldValue(1);
        assertEquals("a",
                     f2.getValue());
        assertEquals("java.lang.Object",
                     f2.getType());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     f2.getNature());

        String marshalled = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        logger.debug(marshalled);

        assertEqualsIgnoreWhitespace(drl,
                                     marshalled);
    }

    @Test
    public void testMethodCallCheckParameterDataTypes2() {
        // BZ-1045423
        String drl = "" +
                "package org.mortgages;\n" +
                "import org.mortgages.MyType;\n" +
                "rule \"my rule\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    t : MyType( )\n" +
                "  then\n" +
                "    t.doSomething( 1 * 2 );\n" +
                "end\n";

        Map<String, List<MethodInfo>> methodInformation = new HashMap<>();
        List<MethodInfo> mapMethodInformation = new ArrayList<>();
        mapMethodInformation.add(new MethodInfo("doSomething",
                                                Collections.singletonList(DataType.TYPE_NUMERIC_INTEGER),
                                                "void",
                                                "void",
                                                "org.mortgages.MyType"));

        methodInformation.put("org.mortgages.MyType",
                              mapMethodInformation);

        when(dmo.getModuleMethodInformation()).thenReturn(methodInformation);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        ActionCallMethod mc = (ActionCallMethod) m.rhs[0];
        assertEquals("doSomething",
                     mc.getMethodName());
        assertEquals("t",
                     mc.getVariable());
        assertEquals(1,
                     mc.getState());
        assertEquals(1,
                     mc.getFieldValues().length);

        ActionFieldValue f1 = mc.getFieldValue(0);
        assertEquals("1 * 2",
                     f1.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     f1.getType());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     f1.getNature());

        String marshalled = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        logger.debug(marshalled);

        assertEqualsIgnoreWhitespace(drl,
                                     marshalled);
    }

    @Test
    public void testMethodCallCheckParameterDataTypes3() {
        // BZ-1045423
        String drl = "" +
                "package org.mortgages;\n" +
                "import org.mortgages.MyType;\n" +
                "rule \"my rule\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    i : Integer( )\n" +
                "    t : MyType( )\n" +
                "  then\n" +
                "    t.doSomething( i );\n" +
                "end\n";

        Map<String, List<MethodInfo>> methodInformation = new HashMap<>();
        List<MethodInfo> mapMethodInformation = new ArrayList<>();
        mapMethodInformation.add(new MethodInfo("doSomething",
                                                Collections.singletonList(DataType.TYPE_NUMERIC_INTEGER),
                                                "void",
                                                "void",
                                                "org.mortgages.MyType"));

        methodInformation.put("org.mortgages.MyType",
                              mapMethodInformation);

        when(dmo.getModuleMethodInformation()).thenReturn(methodInformation);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        ActionCallMethod mc = (ActionCallMethod) m.rhs[0];
        assertEquals("doSomething",
                     mc.getMethodName());
        assertEquals("t",
                     mc.getVariable());
        assertEquals(1,
                     mc.getState());
        assertEquals(1,
                     mc.getFieldValues().length);

        ActionFieldValue f1 = mc.getFieldValue(0);
        assertEquals("i",
                     f1.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     f1.getType());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     f1.getNature());

        String marshalled = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        logger.debug(marshalled);
        assertEqualsIgnoreWhitespace(drl,
                                     marshalled);
    }

    @Test
    public void testGlobalCollectionAdd() {
        // BZ-1013682
        String drl = "package org.mortgages;\n" +
                "\n" +
                "import org.mortgages.LoanApplication;\n" +
                "\n" +
                "rule \"Bankruptcy history\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    a : LoanApplication( )\n" +
                "  then\n" +
                "    keke.add( a );\n" +
                "end";

        HashMap<String, String> globals = new HashMap<>();
        globals.put("keke",
                    "java.util.ArrayList");

        when(
                dmo.getPackageGlobals()
        ).thenReturn(
                globals
        );

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertTrue(m.rhs[0] instanceof ActionGlobalCollectionAdd);
        ActionGlobalCollectionAdd actionGlobalCollectionAdd = (ActionGlobalCollectionAdd) m.rhs[0];
        assertEquals("keke",
                     actionGlobalCollectionAdd.getGlobalName());
        assertEquals("a",
                     actionGlobalCollectionAdd.getFactName());
    }

    @Test
    public void testFieldConstraintLessThanOrEqualTo() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( age <= 22 )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getNumberOfConstraints());

        FieldConstraint fc = fp.getConstraint(0);
        assertNotNull(fc);
        assertTrue(fc instanceof SingleFieldConstraint);

        SingleFieldConstraint sfc = (SingleFieldConstraint) fc;
        assertEquals("<=",
                     sfc.getOperator());
        assertEquals("22",
                     sfc.getValue());
    }

    @Test
    public void testExpressionWithListSize() throws Exception {
        String drl = "" +
                "rule \"Borked\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Company( emps.size() == 0 )\n" +
                "  then\n" +
                "end";

        addModelField("Company",
                      "emps",
                      "java.util.List",
                      "List");

        addMethodInformation("java.util.List",
                             "size",
                             Collections.emptyList(),
                             "int",
                             null,
                             DataType.TYPE_NUMERIC_INTEGER);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertEquals(1,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof FactPattern);
        FactPattern factPattern = (FactPattern) m.lhs[0];
        assertEquals(1,
                     factPattern.getConstraintList().getConstraints().length);
        assertTrue(factPattern.getConstraintList().getConstraints()[0] instanceof SingleFieldConstraintEBLeftSide);
        SingleFieldConstraintEBLeftSide constraint = (SingleFieldConstraintEBLeftSide) factPattern.getConstraintList().getConstraints()[0];
        assertEquals("size",
                     constraint.getFieldName());
        assertEquals("int",
                     constraint.getFieldType());
        assertEquals("0",
                     constraint.getValue());
        assertEquals("==",
                     constraint.getOperator());
        assertEquals(1,
                     constraint.getConstraintValueType());
    }

    @Test
    public void testMVELInlineList() throws Exception {
        String drl = "" +
                "rule \"Borked\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    c : Company( )\n" +
                "  then\n" +
                "    c.setEmps( [\"item1\", \"item2\"] );\n" +
                "end";

        addModelField("Company",
                      "emps",
                      "java.util.List",
                      DataType.TYPE_COLLECTION);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionSetField);
        ActionSetField actionSetField = (ActionSetField) m.rhs[0];

        assertEquals("c",
                     actionSetField.getVariable());

        assertEquals(1,
                     actionSetField.getFieldValues().length);

        ActionFieldValue actionFieldValue = actionSetField.getFieldValues()[0];

        assertEquals("[\"item1\", \"item2\"]",
                     actionFieldValue.getValue());
        assertEquals("emps",
                     actionFieldValue.getField());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     actionFieldValue.getNature());
        assertEquals(DataType.TYPE_COLLECTION,
                     actionFieldValue.getType());
    }

    @Test
    public void testFunctionInRHS() throws Exception {
        String drl = "" +
                "rule \"Borked\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    application : Application( )\n" +
                "  then\n" +
                "    application.setApr( application.getApr() + 5 );\n" +
                "    update( application )" +
                "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertTrue(m.rhs[0] instanceof ActionUpdateField);

        ActionUpdateField field = (ActionUpdateField) m.rhs[0];
        assertNotNull(field.getFieldValues()[0]);
        ActionFieldValue value = field.getFieldValues()[0];
        assertEquals("apr",
                     value.getField());
        assertEquals("application.getApr() + 5",
                     value.getValue());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     value.getNature());
        assertEquals(DataType.TYPE_NUMERIC,
                     value.getType());
    }

    @Test
    public void testFieldVars() throws Exception {
        String drl = "" +
                "rule \"Borked\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Customer( var:contact )\n" +
                "  then\n" +
                "end";

        addModelField("Customer",
                      "contact",
                      "Contact",
                      "Contact");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        FactPattern pattern = (FactPattern) m.lhs[0];
        SingleFieldConstraint constraint = (SingleFieldConstraint) pattern.getFieldConstraints()[0];

        assertEquals("var",
                     constraint.getFieldBinding());
        assertEquals("Customer",
                     constraint.getFactType());
        assertEquals("contact",
                     constraint.getFieldName());
        assertEquals("Contact",
                     constraint.getFieldType());
    }

    @Test
    public void testFieldVarsWithImports() throws Exception {
        String drl = "" +
                "import org.test.Customer\n" +
                "rule \"Borked\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Customer( var:contact )\n" +
                "  then\n" +
                "end";

        addModelField("org.test.Customer",
                      "contact",
                      "org.test.Contact",
                      "org.test.Contact");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        FactPattern pattern = (FactPattern) m.lhs[0];
        SingleFieldConstraint constraint = (SingleFieldConstraint) pattern.getFieldConstraints()[0];

        assertEquals("var",
                     constraint.getFieldBinding());
        assertEquals("Customer",
                     constraint.getFactType());
        assertEquals("contact",
                     constraint.getFieldName());
        assertEquals("org.test.Contact",
                     constraint.getFieldType());
    }

    @Test
    public void testFieldVarsFactTypeInTheSamePackage() throws Exception {
        String drl = "" +
                "package org.test\n" +
                "rule \"Borked\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Customer( var:contact )\n" +
                "  then\n" +
                "end";

        addModelField("org.test.Customer",
                      "contact",
                      "org.test.Contact",
                      "org.test.Contact");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        FactPattern pattern = (FactPattern) m.lhs[0];
        SingleFieldConstraint constraint = (SingleFieldConstraint) pattern.getFieldConstraints()[0];

        assertEquals("var",
                     constraint.getFieldBinding());
        assertEquals("Customer",
                     constraint.getFactType());
        assertEquals("contact",
                     constraint.getFieldName());
        assertEquals("org.test.Contact",
                     constraint.getFieldType());
    }

    @Test
    public void testSingleFieldConstraintEBLeftSide() throws Exception {
        String drl = "" +
                "rule \" broken \"\n"
                + "dialect \"mvel\"\n"
                + "  when\n"
                + "    Customer( contact != null , contact.tel1 > \"15\" )\n"
                + "  then\n"
                + "end";

        addModelField("Customer",
                      "contact",
                      "Contact",
                      "Contact");
        addModelField("Contact",
                      "tel1",
                      "String",
                      "String");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        FactPattern pattern = (FactPattern) m.lhs[0];
        SingleFieldConstraint constraint = (SingleFieldConstraint) pattern.getFieldConstraints()[0];

        assertEquals("Customer",
                     constraint.getFactType());
        assertEquals("contact",
                     constraint.getFieldName());
        assertEquals("Contact",
                     constraint.getFieldType());

        SingleFieldConstraintEBLeftSide constraint2 = (SingleFieldConstraintEBLeftSide) pattern.getFieldConstraints()[1];
        assertEquals("tel1",
                     constraint2.getFieldName());
        assertEquals("String",
                     constraint2.getFieldType());
        assertEquals("15",
                     constraint2.getValue());
        assertEquals(">",
                     constraint2.getOperator());

        assertEquals(3,
                     constraint2.getExpressionLeftSide().getParts().size());
        ExpressionPart part1 = constraint2.getExpressionLeftSide().getParts().get(0);
        assertEquals("Customer",
                     part1.getName());
        assertEquals("Customer",
                     part1.getClassType());
        assertEquals("Customer",
                     part1.getGenericType());

        ExpressionPart part2 = constraint2.getExpressionLeftSide().getParts().get(1);
        assertEquals("contact",
                     part2.getName());
        assertEquals("Contact",
                     part2.getClassType());
        assertEquals("Contact",
                     part2.getGenericType());

        ExpressionPart part3 = constraint2.getExpressionLeftSide().getParts().get(2);
        assertEquals("tel1",
                     part3.getName());
        assertEquals("String",
                     part3.getClassType());
        assertEquals("String",
                     part3.getGenericType());
    }

    @Test
    public void testExpressionEditorLeftToOperator() throws Exception {
        String drl = ""
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + " a: Applicant()\n"
                + " Applicant( age == a.age )\n"
                + "then\n"
                + "end";

        addModelField("Applicant",
                      "this",
                      "Applicant",
                      DataType.TYPE_THIS);
        addModelField("Applicant",
                      "age",
                      "java.lang.Integer",
                      "Integer");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        FactPattern pattern1 = (FactPattern) m.lhs[0];
        assertEquals("a",
                     pattern1.getBoundName());

        FactPattern pattern2 = (FactPattern) m.lhs[1];

        assertEquals(1,
                     pattern2.getConstraintList().getNumberOfConstraints());

        assertTrue(pattern2.getConstraintList().getConstraint(0) instanceof SingleFieldConstraint);
        SingleFieldConstraint constraint = (SingleFieldConstraint) pattern2.getConstraintList().getConstraint(0);
        assertEquals("age",
                     constraint.getFieldName());
        assertEquals("==",
                     constraint.getOperator());
        assertEquals(BaseSingleFieldConstraint.TYPE_EXPR_BUILDER_VALUE,
                     constraint.getConstraintValueType());
        assertEquals("",
                     constraint.getValue());
        assertEquals(2,
                     constraint.getExpressionValue().getParts().size());

        assertTrue(constraint.getExpressionValue().getParts().get(0) instanceof ExpressionVariable);
        ExpressionVariable expressionVariable = (ExpressionVariable) constraint.getExpressionValue().getParts().get(0);
        assertEquals("a",
                     expressionVariable.getName());
        assertEquals("Applicant",
                     expressionVariable.getClassType());
        assertEquals("this",
                     expressionVariable.getGenericType());
        assertEquals(constraint.getExpressionValue().getParts().get(1),
                     expressionVariable.getNext());

        assertTrue(constraint.getExpressionValue().getParts().get(1) instanceof ExpressionField);
        ExpressionField expressionField = (ExpressionField) constraint.getExpressionValue().getParts().get(1);
        assertEquals("age",
                     expressionField.getName());
        assertEquals("java.lang.Integer",
                     expressionField.getClassType());
        assertEquals("Integer",
                     expressionField.getGenericType());
    }

    @Test
    public void testEnumeration() throws Exception {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1047879
        String drl = "import org.drools.workbench.models.commons.backend.rule.TestEnum;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "OuterClassWithEnums( outerField == TestEnum.VALUE1 )\n"
                + "then\n"
                + "end";

        addModelField("OuterClassWithEnums",
                      "outerField",
                      TestEnum.class.getSimpleName(),
                      DataType.TYPE_COMPARABLE);

        addJavaEnumDefinition("OuterClassWithEnums",
                              "outerField",
                              new String[]{"TestEnum.VALUE1=TestEnum.VALUE1", "TestEnum.VALUE2=TestEnum.VALUE2"});

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals(1,
                     pattern.getNumberOfConstraints());
        assertTrue(pattern.getConstraint(0) instanceof SingleFieldConstraint);
        SingleFieldConstraint constraint = (SingleFieldConstraint) pattern.getConstraint(0);

        assertEquals("OuterClassWithEnums",
                     constraint.getFactType());
        assertEquals("outerField",
                     constraint.getFieldName());
        assertEquals(DataType.TYPE_COMPARABLE,
                     constraint.getFieldType());
        assertEquals("==",
                     constraint.getOperator());
        assertEquals("TestEnum.VALUE1",
                     constraint.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_ENUM,
                     constraint.getConstraintValueType());

        final String drl2 = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     drl2);
    }

    @Test
    public void testFullyQualifiedClassNameEnumeration() throws Exception {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1047879
        String drl = "import org.drools.workbench.models.commons.backend.rule.TestEnum;\n"
                + "import org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "OuterClassWithEnums( outerField == TestEnum.VALUE1 )\n"
                + "then\n"
                + "end";

        addModelField("org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums",
                      "outerField",
                      TestEnum.class.getSimpleName(),
                      DataType.TYPE_COMPARABLE);

        addJavaEnumDefinition("org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums",
                              "outerField",
                              new String[]{"TestEnum.VALUE1=TestEnum.VALUE1", "TestEnum.VALUE2=TestEnum.VALUE2"});

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals(1,
                     pattern.getNumberOfConstraints());
        assertTrue(pattern.getConstraint(0) instanceof SingleFieldConstraint);
        SingleFieldConstraint constraint = (SingleFieldConstraint) pattern.getConstraint(0);

        assertEquals("OuterClassWithEnums",
                     constraint.getFactType());
        assertEquals("outerField",
                     constraint.getFieldName());
        assertEquals(DataType.TYPE_COMPARABLE,
                     constraint.getFieldType());
        assertEquals("==",
                     constraint.getOperator());
        assertEquals("TestEnum.VALUE1",
                     constraint.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_ENUM,
                     constraint.getConstraintValueType());

        final String drl2 = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     drl2);
    }

    @Test
    public void testEnumerationNestedClasses() throws Exception {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1047879
        String drl = "import org.drools.workbench.models.commons.backend.rule.TestEnum;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "OuterClassWithEnums( innerClass.innerField == TestEnum.VALUE1 )\n"
                + "then\n"
                + "end";

        addModelField("OuterClassWithEnums",
                      "innerClass",
                      "InnerClassWithEnums",
                      "InnerClassWithEnums");
        addModelField("InnerClassWithEnums",
                      "innerField",
                      TestEnum.class.getSimpleName(),
                      DataType.TYPE_COMPARABLE);

        addJavaEnumDefinition("InnerClassWithEnums",
                              "innerField",
                              new String[]{"TestEnum.VALUE1=TestEnum.VALUE1", "TestEnum.VALUE2=TestEnum.VALUE2"});

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals(1,
                     pattern.getNumberOfConstraints());
        assertTrue(pattern.getConstraint(0) instanceof SingleFieldConstraintEBLeftSide);

        final SingleFieldConstraintEBLeftSide constraint = (SingleFieldConstraintEBLeftSide) pattern.getConstraint(0);
        assertEquals(3,
                     constraint.getExpressionLeftSide().getParts().size());

        assertTrue(constraint.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        final ExpressionUnboundFact eubf = ((ExpressionUnboundFact) constraint.getExpressionLeftSide().getParts().get(0));
        assertEquals("OuterClassWithEnums",
                     eubf.getName());
        assertEquals("OuterClassWithEnums",
                     eubf.getClassType());
        assertEquals("OuterClassWithEnums",
                     eubf.getGenericType());

        assertTrue(constraint.getExpressionLeftSide().getParts().get(1) instanceof ExpressionField);
        final ExpressionField ef1 = ((ExpressionField) constraint.getExpressionLeftSide().getParts().get(1));
        assertEquals("innerClass",
                     ef1.getName());
        assertEquals("InnerClassWithEnums",
                     ef1.getClassType());
        assertEquals("InnerClassWithEnums",
                     ef1.getGenericType());

        assertTrue(constraint.getExpressionLeftSide().getParts().get(2) instanceof ExpressionField);
        final ExpressionField ef2 = ((ExpressionField) constraint.getExpressionLeftSide().getParts().get(2));
        assertEquals("innerField",
                     ef2.getName());
        assertEquals("TestEnum",
                     ef2.getClassType());
        assertEquals(DataType.TYPE_COMPARABLE,
                     ef2.getGenericType());

        assertEquals("OuterClassWithEnums",
                     constraint.getFactType());
        assertEquals("innerField",
                     constraint.getFieldName());
        assertEquals("TestEnum",
                     constraint.getFieldType());
        assertEquals("==",
                     constraint.getOperator());
        assertEquals("TestEnum.VALUE1",
                     constraint.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_ENUM,
                     constraint.getConstraintValueType());

        final String drl2 = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     drl2);
    }

    @Test
    public void testFullyQualifiedClassNameEnumerationNestedClasses() throws Exception {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1047879
        String drl = "import org.drools.workbench.models.commons.backend.rule.TestEnum;\n"
                + "import org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums;\n"
                + "import org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums.InnerClassWithEnums;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "OuterClassWithEnums( innerClass.innerField == TestEnum.VALUE1 )\n"
                + "then\n"
                + "end";

        addModelField("org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums",
                      "innerClass",
                      "org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums$InnerClassWithEnums",
                      "OuterClassWithEnums$InnerClassWithEnums");
        addModelField("org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums$InnerClassWithEnums",
                      "innerField",
                      TestEnum.class.getSimpleName(),
                      DataType.TYPE_COMPARABLE);

        addJavaEnumDefinition("org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums$InnerClassWithEnums",
                              "innerField",
                              new String[]{"TestEnum.VALUE1=TestEnum.VALUE1", "TestEnum.VALUE2=TestEnum.VALUE2"});

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals(1,
                     pattern.getNumberOfConstraints());
        assertTrue(pattern.getConstraint(0) instanceof SingleFieldConstraintEBLeftSide);

        final SingleFieldConstraintEBLeftSide constraint = (SingleFieldConstraintEBLeftSide) pattern.getConstraint(0);
        assertEquals(3,
                     constraint.getExpressionLeftSide().getParts().size());

        assertTrue(constraint.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        final ExpressionUnboundFact eubf = ((ExpressionUnboundFact) constraint.getExpressionLeftSide().getParts().get(0));
        assertEquals("OuterClassWithEnums",
                     eubf.getName());
        assertEquals("OuterClassWithEnums",
                     eubf.getClassType());
        assertEquals("OuterClassWithEnums",
                     eubf.getGenericType());

        assertTrue(constraint.getExpressionLeftSide().getParts().get(1) instanceof ExpressionField);
        final ExpressionField ef1 = ((ExpressionField) constraint.getExpressionLeftSide().getParts().get(1));
        assertEquals("innerClass",
                     ef1.getName());
        assertEquals("org.drools.workbench.models.commons.backend.rule.OuterClassWithEnums$InnerClassWithEnums",
                     ef1.getClassType());
        assertEquals("OuterClassWithEnums$InnerClassWithEnums",
                     ef1.getGenericType());

        assertTrue(constraint.getExpressionLeftSide().getParts().get(2) instanceof ExpressionField);
        final ExpressionField ef2 = ((ExpressionField) constraint.getExpressionLeftSide().getParts().get(2));
        assertEquals("innerField",
                     ef2.getName());
        assertEquals("TestEnum",
                     ef2.getClassType());
        assertEquals(DataType.TYPE_COMPARABLE,
                     ef2.getGenericType());

        assertEquals("OuterClassWithEnums",
                     constraint.getFactType());
        assertEquals("innerField",
                     constraint.getFieldName());
        assertEquals("TestEnum",
                     constraint.getFieldType());
        assertEquals("==",
                     constraint.getOperator());
        assertEquals("TestEnum.VALUE1",
                     constraint.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_ENUM,
                     constraint.getConstraintValueType());

        final String drl2 = RuleModelDRLPersistenceImpl.getInstance().marshal(m);
        assertEqualsIgnoreWhitespace(drl,
                                     drl2);
    }

    @Test
    public void testCalendars() {
        //BZ1059232 - Guided rule editor: calendars attribute is broken when a list of calendars is used
        String drl = "package org.mortgages;\n" +
                "\n" +
                "import java.lang.Number;\n" +
                "rule \"Test\"\n" +
                "  calendars \"a\" ,\"b\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "  then\n" +
                "end\n";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m.attributes[0]);
        RuleAttribute attribute = m.attributes[0];
        assertEquals("calendars",
                     attribute.getAttributeName());
        assertEquals("a, b",
                     attribute.getValue());
    }

    @Test
    public void testFromRestrictions() {
        String drl = "package org.mortgages;\n" +
                "\n" +
                "import java.lang.Number;\n" +
                "rule \"Test\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    reserva : Reserva( )\n" +
                "    itinerario : Itinerario( destino == \"USA\" ) from reserva.itinerarios\n" +
                "  then\n" +
                "end\n";

        addModelField("org.mortgages.Reserva",
                      "itinerarios",
                      "java.lang.List",
                      DataType.TYPE_COLLECTION);
        addModelField("org.mortgages.Itinerario",
                      "destino",
                      "String",
                      DataType.TYPE_STRING);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertTrue(m.lhs[1] instanceof FromCompositeFactPattern);
        FromCompositeFactPattern factPattern = (FromCompositeFactPattern) m.lhs[1];
        assertNotNull(factPattern.getFactPattern().getConstraintList());
        assertEquals(1,
                     factPattern.getFactPattern().getConstraintList().getNumberOfConstraints());
        SingleFieldConstraint constraint = (SingleFieldConstraint) factPattern.getFactPattern().getFieldConstraints()[0];
        assertEquals("Itinerario",
                     constraint.getFactType());
        assertEquals("destino",
                     constraint.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     constraint.getFieldType());
        assertEquals("USA",
                     constraint.getValue());
        assertEquals("==",
                     constraint.getOperator());
    }

    @Test
    public void testFactsWithSameName() {
        String drl = "package org.pkg1;\n" +
                "\n" +
                "import java.lang.Number;\n" +
                "rule \"Test\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Fact( field.field )\n" +
                "  then\n" +
                "end\n";

        addModelField("org.pkg1.Fact",
                      "field",
                      "org.pkg1.SubFact",
                      "SubFact");
        addModelField("org.pkg1.SubFact",
                      "field",
                      "String",
                      DataType.TYPE_STRING);
        addModelField("org.pkg2.Fact",
                      "someOtherField",
                      "org.pkg2.SubFact",
                      "SubFact");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertEquals(1,
                     m.lhs.length);
    }

    @Test
    public void testFactsWithSameNameImports() {
        String drl = "package org.test;\n" +
                "\n" +
                "import org.pkg1.Fact;\n" +
                "rule \"Test\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Fact( field.field )\n" +
                "  then\n" +
                "end\n";

        addModelField("org.pkg1.Fact",
                      "field",
                      "org.pkg1.SubFact",
                      "SubFact");
        addModelField("org.pkg1.SubFact",
                      "field",
                      "String",
                      DataType.TYPE_STRING);
        addModelField("org.pkg2.Fact",
                      "someOtherField",
                      "org.pkg2.SubFact",
                      "SubFact");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);
        assertEquals(1,
                     m.lhs.length);
    }

    @Test
    public void testFromBoundVariable() {
        String drl = "import java.lang.Number;\n"
                + "import org.drools.workbench.models.commons.backend.rule.Counter;\n"
                + "rule \"rule1\"\n"
                + "when\n"
                + "cc : Counter()\n"
                + "Number() from cc.number\n"
                + "then\n"
                + "end";

        addModelField("org.drools.workbench.models.commons.backend.rule.Counter",
                      "number",
                      "java.lang.Number",
                      DataType.TYPE_NUMERIC);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(2,
                     m.lhs.length);
        IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);

        FactPattern fp0 = (FactPattern) p0;
        assertEquals("Counter",
                     fp0.getFactType());
        assertEquals("cc",
                     fp0.getBoundName());
        assertEquals(0,
                     fp0.getNumberOfConstraints());

        IPattern p1 = m.lhs[1];
        assertTrue(p1 instanceof FromCompositeFactPattern);

        FromCompositeFactPattern fcfp1 = (FromCompositeFactPattern) p1;
        FactPattern fp1 = fcfp1.getFactPattern();
        ExpressionFormLine efl1 = fcfp1.getExpression();

        assertNotNull(fp1);
        assertNotNull(efl1);

        assertEquals("Number",
                     fp1.getFactType());
        assertEquals(0,
                     fp1.getNumberOfConstraints());

        assertEquals(2,
                     efl1.getParts().size());
        assertTrue(efl1.getParts().get(0) instanceof ExpressionVariable);
        assertTrue(efl1.getParts().get(1) instanceof ExpressionField);

        ExpressionVariable eflv1 = (ExpressionVariable) efl1.getParts().get(0);
        assertEquals("cc",
                     eflv1.getName());
        assertEquals("Counter",
                     eflv1.getClassType());
        assertEquals(DataType.TYPE_NUMERIC,
                     eflv1.getGenericType());

        ExpressionField eflf1 = (ExpressionField) efl1.getParts().get(1);
        assertEquals("number",
                     eflf1.getName());
        assertEquals("java.lang.Number",
                     eflf1.getClassType());
        assertEquals(DataType.TYPE_NUMERIC,
                     eflf1.getGenericType());
    }

    @Test
    public void testFromAccumulate() {
        String drl = "package org.test;\n"
                + "import java.lang.Number;\n"
                + "rule \"rule1\"\n"
                + "when\n"
                + "  total : Number( intValue > 0 ) from accumulate ( Applicant( age < 30 ), count() )\n"
                + "then\n"
                + "end";

        addModelField("java.lang.Number",
                      "intValue",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        addModelField("org.test.Applicant",
                      "this",
                      "org.test.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.test.Applicant",
                      "age",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);

        assertTrue(m.lhs[0] instanceof FromAccumulateCompositeFactPattern);

        FromAccumulateCompositeFactPattern pattern = (FromAccumulateCompositeFactPattern) m.lhs[0];
        assertNotNull(pattern.getFactPattern());
        FactPattern factPattern = pattern.getFactPattern();
        assertEquals("total",
                     factPattern.getBoundName());
        assertNotNull(factPattern.getConstraintList());
        assertEquals(1,
                     factPattern.getConstraintList().getNumberOfConstraints());
        FieldConstraint constraint = factPattern.getConstraintList().getConstraint(0);
        assertTrue(constraint instanceof SingleFieldConstraint);
        SingleFieldConstraint fieldConstraint = (SingleFieldConstraint) constraint;
        assertEquals("Number",
                     fieldConstraint.getFactType());
        assertEquals("intValue",
                     fieldConstraint.getFieldName());
        assertEquals("Integer",
                     fieldConstraint.getFieldType());
        assertEquals(">",
                     fieldConstraint.getOperator());
        assertEquals("0",
                     fieldConstraint.getValue());
    }

    @Test
    public void testSimpleDSLExpansionLHS() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Es gibt einen Vertrag\n"
                + "- Rabatt nicht mehr als 123\n"
                + "then\n"
                + "end\n";

        final String dslDefinition = "Es gibt einen Vertrag";
        final String dslFile = "[condition][vertrag]" + dslDefinition + "=vertrag : Vertrag()";

        final String dslDefinition2 = "- Rabatt nicht mehr als {rabatt}";
        final String dslFile2 = "[condition][vertrag]" + dslDefinition2 + "=rabatt < {rabatt}";

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                        Collections.emptyList(),
                                                                                        dmo,
                                                                                        dslFile,
                                                                                        dslFile2);

        assertNotNull(m);

        assertTrue(m.lhs[0] instanceof DSLSentence);

        DSLSentence dslSentence = (DSLSentence) m.lhs[0];
        assertEquals("vertrag : Vertrag()",
                     dslSentence.getDrl());
        assertEquals(dslDefinition,
                     dslSentence.getDefinition());
        assertEquals(0,
                     dslSentence.getValues().size());

        DSLSentence dslSentence2 = (DSLSentence) m.lhs[1];
        assertEquals("rabatt < {rabatt}",
                     dslSentence2.getDrl());
        assertEquals(dslDefinition2,
                     dslSentence2.getDefinition());
        assertEquals(1,
                     dslSentence2.getValues().size());

        assertNotNull(dslSentence2.getValues().get(0));

        DSLVariableValue dslComplexVariableValue = dslSentence2.getValues().get(0);
        assertEquals("123",
                     dslComplexVariableValue.getValue());
    }

    @Test
    public void testDSL() {
        String drl = "package org.mortgages;\n" +
                "rule \"testdsl\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    There is a test rated applicant older than 111 years\n" +
                "  then\n" +
                "end";

        String dslDefinition = "There is a {rating} rated applicant older than {age} years";
        String dslFile = "[when]" + dslDefinition + "= Applicant( creditRating == \"{rating}\", age > {age} )";

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                            Collections.emptyList(),
                                                                                            dmo,
                                                                                            dslFile);

        assertEquals(1,
                     model.lhs.length);
        DSLSentence dslSentence = (DSLSentence) model.lhs[0];
        assertEquals("Applicant( creditRating == \"{rating}\", age > {age} )",
                     dslSentence.getDrl());
        assertEquals("test",
                     dslSentence.getValues().get(0).getValue());
        assertEquals("111",
                     dslSentence.getValues().get(1).getValue());
    }

    @Test
    public void testDSLDollar() {
        String drl = "package org.mortgages;\n" +
                "rule \"testdsl\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Price is $111\n" +
                "  then\n" +
                "end";

        String dslDefinition = "Price is ${p}";
        String dslFile = "[when]" + dslDefinition + "= Item( price == \"{p}\" )";

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                            Collections.emptyList(),
                                                                                            dmo,
                                                                                            dslFile);

        assertEquals(1,
                     model.lhs.length);
        DSLSentence dslSentence = (DSLSentence) model.lhs[0];

        assertEquals("Price is ${p}",
                     dslSentence.getDefinition());
        assertEquals("111",
                     dslSentence.getValues().get(0).getValue());
    }

    @Test
    public void testDSLExpansionLHS() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "The credit rating is AA\n"
                + "then\n"
                + "end\n";

        final String dslDefinition = "The credit rating is {rating:ENUM:Applicant.creditRating}";
        final String dslFile = "[when]" + dslDefinition + "=Applicant( creditRating == {rating} )";

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                        Collections.emptyList(),
                                                                                        dmo,
                                                                                        dslFile);

        assertNotNull(m);

        assertTrue(m.lhs[0] instanceof DSLSentence);

        DSLSentence dslSentence = (DSLSentence) m.lhs[0];
        assertEquals(dslDefinition,
                     dslSentence.getDefinition());
        assertEquals(1,
                     dslSentence.getValues().size());
        assertTrue(dslSentence.getValues().get(0) instanceof DSLComplexVariableValue);
        DSLComplexVariableValue dslComplexVariableValue = (DSLComplexVariableValue) dslSentence.getValues().get(0);
        assertEquals("AA",
                     dslComplexVariableValue.getValue());
        assertEquals("ENUM:Applicant.creditRating",
                     dslComplexVariableValue.getId());
    }

    @Test
    public void testDSLExpansionLHS2() {
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "There is an Applicant\n"
                + "- credit rating is AA\n"
                + "then\n"
                + "end\n";

        final String dslDefinition1 = "There is an Applicant";
        final String dslFile1 = "[when]" + dslDefinition1 + "=Applicant( )";

        final String dslDefinition2 = "- credit rating is {rating:ENUM:Applicant.creditRating}";
        final String dslFile2 = "[when]" + dslDefinition2 + "=creditRating == {rating}";

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                        Collections.emptyList(),
                                                                                        dmo,
                                                                                        dslFile1,
                                                                                        dslFile2);

        assertNotNull(m);

        assertEquals(2,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof DSLSentence);
        assertTrue(m.lhs[1] instanceof DSLSentence);

        DSLSentence dslSentence1 = (DSLSentence) m.lhs[0];
        assertEquals(dslDefinition1,
                     dslSentence1.getDefinition());
        assertEquals(0,
                     dslSentence1.getValues().size());

        DSLSentence dslSentence2 = (DSLSentence) m.lhs[1];
        assertEquals(dslDefinition2,
                     dslSentence2.getDefinition());
        assertEquals(1,
                     dslSentence2.getValues().size());
        assertTrue(dslSentence2.getValues().get(0) instanceof DSLComplexVariableValue);
        DSLComplexVariableValue dslComplexVariableValue = (DSLComplexVariableValue) dslSentence2.getValues().get(0);
        assertEquals("AA",
                     dslComplexVariableValue.getValue());
        assertEquals("ENUM:Applicant.creditRating",
                     dslComplexVariableValue.getId());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1173842
    public void testDSLExpansionLHS_WithKeyword_then() {
        String expected_dslr = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "There is an Applicant\n"
                + "- age more then 55\n"
                + "then\n"
                + "end\n";
        String expected_drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Applicant( age > 55 )\n"
                + "then\n"
                + "end\n";

        final String dslDefinition1 = "There is an Applicant";
        final String dslFile1 = "[when]" + dslDefinition1 + "=Applicant( )";

        final String dslDefinition2 = "- age more then {age}";
        final String dslFile2 = "[when]" + dslDefinition2 + "=age > {age}";

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(expected_dslr,
                                                                                        Collections.emptyList(),
                                                                                        dmo,
                                                                                        dslFile1,
                                                                                        dslFile2);

        assertNotNull(m);

        assertEquals(2,
                     m.lhs.length);
        assertTrue(m.lhs[0] instanceof DSLSentence);
        assertTrue(m.lhs[1] instanceof DSLSentence);

        DSLSentence dslSentence1 = (DSLSentence) m.lhs[0];
        assertEquals(dslDefinition1,
                     dslSentence1.getDefinition());
        assertEquals(0,
                     dslSentence1.getValues().size());

        DSLSentence dslSentence2 = (DSLSentence) m.lhs[1];
        assertEquals(dslDefinition2,
                     dslSentence2.getDefinition());
        assertEquals(1,
                     dslSentence2.getValues().size());
        DSLVariableValue dslVariableValue = dslSentence2.getValues().get(0);
        assertEquals("55",
                     dslVariableValue.getValue());

        //Check round-trip
        assertEqualsIgnoreWhitespace(expected_dslr,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));

        //Check DSL expansion (as BZ stated runtime was flawed as well)
        final Expander expander = new DefaultExpander();
        final List<DSLMappingFile> dsls = new ArrayList<>();
        try {
            final DSLTokenizedMappingFile dslTokenizer1 = new DSLTokenizedMappingFile();
            if (dslTokenizer1.parseAndLoad(new StringReader(dslFile1))) {
                dsls.add(dslTokenizer1);
            } else {
                fail();
            }
            final DSLTokenizedMappingFile dslTokenizer2 = new DSLTokenizedMappingFile();
            if (dslTokenizer2.parseAndLoad(new StringReader(dslFile2))) {
                dsls.add(dslTokenizer2);
            } else {
                fail();
            }
        } catch (IOException e) {
            fail();
        }
        for (DSLMappingFile dsl : dsls) {
            expander.addDSLMapping(dsl.getMapping());
        }
        final String actual_drl = expander.expand(expected_dslr);
        assertEqualsIgnoreWhitespace(expected_drl,
                                     actual_drl);
    }

    @Test
    public void testDSLExpansionRHS() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "> $a : Applicant()\n"
                + "then\n"
                + "Set applicant name to Bob\n"
                + "end\n";

        final String dslDefinition = "Set applicant name to {name:\\w+ \\w+}";
        final String dslFile = "[then]" + dslDefinition + "=$a.setName( \"{name}\" )";

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                        Collections.emptyList(),
                                                                                        dmo,
                                                                                        dslFile);

        assertNotNull(m);

        assertTrue(m.lhs[0] instanceof FactPattern);
        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals("Applicant",
                     pattern.getFactType());
        assertEquals("$a",
                     pattern.getBoundName());

        assertTrue(m.rhs[0] instanceof DSLSentence);

        DSLSentence dslSentence = (DSLSentence) m.rhs[0];
        assertEquals(dslDefinition,
                     dslSentence.getDefinition());
        assertEquals(1,
                     dslSentence.getValues().size());
        assertTrue(dslSentence.getValues().get(0) instanceof DSLComplexVariableValue);
        DSLComplexVariableValue dslComplexVariableValue = (DSLComplexVariableValue) dslSentence.getValues().get(0);
        assertEquals("Bob",
                     dslComplexVariableValue.getValue());
        assertEquals("\\w+ \\w+",
                     dslComplexVariableValue.getId());
    }

    /*
     * https://issues.jboss.org/browse/GUVNOR-3451
     */
    @Test
    public void testDSLWithMultilineBrackets() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "> Applicant(\n"
                + "> )\n"
                + "then\n"
                + "end\n";

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                        Collections.emptyList(),
                                                                                        dmo,
                                                                                        "");

        assertNotNull(m);

        assertTrue(m.lhs[0] instanceof FactPattern);
        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals("Applicant",
                     pattern.getFactType());
    }

    @Test
    public void testDSLWithMultilineBracketsOneField() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "> Applicant( name == \n"
                + "> \"John\")\n"
                + "then\n"
                + "end\n";

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                        Collections.emptyList(),
                                                                                        dmo,
                                                                                        "");

        assertNotNull(m);

        assertTrue(m.lhs[0] instanceof FactPattern);
        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals("Applicant",
                     pattern.getFactType());
        assertEquals(1,
                     pattern.getNumberOfConstraints());
        assertEquals("name",
                     ((SingleFieldConstraint) pattern.getConstraint(0)).getFieldName());
        assertEquals("==",
                     ((SingleFieldConstraint) pattern.getConstraint(0)).getOperator());
        assertEquals("John",
                     ((SingleFieldConstraint) pattern.getConstraint(0)).getValue());
    }

    @Test
    public void testDSLWithMultilineBracketsTwoFields() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "> Applicant( name == \n"
                + "> \"John\"\n"
                + "> , \n"
                + "> age > 18 )\n"
                + "then\n"
                + "end\n";

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                        Collections.emptyList(),
                                                                                        dmo,
                                                                                        "");

        assertNotNull(m);

        assertTrue(m.lhs[0] instanceof FactPattern);
        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals("Applicant",
                     pattern.getFactType());
        assertEquals(2,
                     pattern.getNumberOfConstraints());
        assertEquals("name",
                     ((SingleFieldConstraint) pattern.getConstraint(0)).getFieldName());
        assertEquals("==",
                     ((SingleFieldConstraint) pattern.getConstraint(0)).getOperator());
        assertEquals("John",
                     ((SingleFieldConstraint) pattern.getConstraint(0)).getValue());

        assertEquals("age",
                     ((SingleFieldConstraint) pattern.getConstraint(1)).getFieldName());
        assertEquals(">",
                     ((SingleFieldConstraint) pattern.getConstraint(1)).getOperator());
        assertEquals("18",
                     ((SingleFieldConstraint) pattern.getConstraint(1)).getValue());
    }

    @Test
    public void testFunctionCalls() {
        String drl =
                "package org.mortgages;\n"
                        + "import java.lang.Number;\n"
                        + "import java.lang.String;\n"
                        + "rule \"rule1\"\n"
                        + "dialect \"mvel\"\n"
                        + "when\n"
                        + "  s : String()\n"
                        + "then\n"
                        + "  s.indexOf( s );\n"
                        + "  s.indexOf( 0 );\n"
                        + "end\n";

        Map<String, List<MethodInfo>> methodInformation = new HashMap<>();
        List<MethodInfo> mapMethodInformation = new ArrayList<>();
        mapMethodInformation.add(new MethodInfo("indexOf",
                                                Collections.singletonList(DataType.TYPE_STRING),
                                                "int",
                                                null,
                                                DataType.TYPE_STRING));
        mapMethodInformation.add(new MethodInfo("indexOf",
                                                Collections.singletonList(DataType.TYPE_NUMERIC_INTEGER),
                                                "int",
                                                null,
                                                DataType.TYPE_STRING));

        methodInformation.put("java.lang.String",
                              mapMethodInformation);

        when(dmo.getModuleMethodInformation()).thenReturn(methodInformation);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(2,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        assertTrue(m.rhs[1] instanceof ActionCallMethod);

        ActionCallMethod actionCallMethod1 = (ActionCallMethod) m.rhs[0];
        assertEquals(1,
                     actionCallMethod1.getState());
        assertEquals("indexOf",
                     actionCallMethod1.getMethodName());
        assertEquals("s",
                     actionCallMethod1.getVariable());
        assertEquals(1,
                     actionCallMethod1.getFieldValues().length);
        assertEquals("indexOf",
                     actionCallMethod1.getFieldValues()[0].getField());
        assertEquals("s",
                     actionCallMethod1.getFieldValues()[0].getValue());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     actionCallMethod1.getFieldValues()[0].getNature());
        assertEquals("String",
                     actionCallMethod1.getFieldValues()[0].getType());

        ActionCallMethod actionCallMethod2 = (ActionCallMethod) m.rhs[1];
        assertEquals(1,
                     actionCallMethod2.getState());
        assertEquals("indexOf",
                     actionCallMethod2.getMethodName());
        assertEquals("s",
                     actionCallMethod2.getVariable());
        assertEquals(1,
                     actionCallMethod2.getFieldValues().length);
        assertEquals("indexOf",
                     actionCallMethod2.getFieldValues()[0].getField());
        assertEquals("0",
                     actionCallMethod2.getFieldValues()[0].getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     actionCallMethod2.getFieldValues()[0].getNature());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     actionCallMethod2.getFieldValues()[0].getType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testFunctionCalls2() {
        String drl =
                "package org.mortgages;\n"
                        + "import java.lang.Number;\n"
                        + "import java.lang.String;\n"
                        + "rule \"rule1\"\n"
                        + "dialect \"mvel\"\n"
                        + "when\n"
                        + "  s : String()\n"
                        + "then\n"
                        + "  s.indexOf( 0 );\n"
                        + "end\n";

        Map<String, List<MethodInfo>> methodInformation = new HashMap<>();
        List<MethodInfo> mapMethodInformation = new ArrayList<>();
        mapMethodInformation.add(new MethodInfo("indexOf",
                                                Collections.singletonList(DataType.TYPE_STRING),
                                                "int",
                                                null,
                                                DataType.TYPE_STRING));
        mapMethodInformation.add(new MethodInfo("indexOf",
                                                Collections.singletonList(DataType.TYPE_NUMERIC_INTEGER),
                                                "int",
                                                null,
                                                DataType.TYPE_STRING));

        methodInformation.put("java.lang.String",
                              mapMethodInformation);

        when(dmo.getModuleMethodInformation()).thenReturn(methodInformation);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionCallMethod);

        ActionCallMethod actionCallMethod1 = (ActionCallMethod) m.rhs[0];
        assertEquals(1,
                     actionCallMethod1.getState());
        assertEquals("indexOf",
                     actionCallMethod1.getMethodName());
        assertEquals("s",
                     actionCallMethod1.getVariable());
        assertEquals(1,
                     actionCallMethod1.getFieldValues().length);
        assertEquals("indexOf",
                     actionCallMethod1.getFieldValues()[0].getField());
        assertEquals("0",
                     actionCallMethod1.getFieldValues()[0].getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     actionCallMethod1.getFieldValues()[0].getNature());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     actionCallMethod1.getFieldValues()[0].getType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testFunctionCalls3() {
        String drl =
                "package org.mortgages;\n"
                        + "import java.lang.Number;\n"
                        + "import java.lang.String;\n"
                        + "rule \"rule1\"\n"
                        + "dialect \"mvel\"\n"
                        + "when\n"
                        + "  $var : String()\n"
                        + "then\n"
                        + "  $var.indexOf( $var );\n"
                        + "  $var.endsWith( \".\" );\n"
                        + "  $var.substring( 0, 1 );\n"
                        + "end\n";

        Map<String, List<MethodInfo>> methodInformation = new HashMap<>();
        List<MethodInfo> mapMethodInformation = new ArrayList<>();
        mapMethodInformation.add(new MethodInfo("indexOf",
                                                Collections.singletonList(DataType.TYPE_STRING),
                                                "int",
                                                null,
                                                DataType.TYPE_STRING));
        mapMethodInformation.add(new MethodInfo("indexOf",
                                                Collections.singletonList(DataType.TYPE_NUMERIC_INTEGER),
                                                "int",
                                                null,
                                                DataType.TYPE_STRING));
        mapMethodInformation.add(new MethodInfo("endsWith",
                                                Collections.singletonList(DataType.TYPE_STRING),
                                                "boolean",
                                                null,
                                                DataType.TYPE_BOOLEAN));
        mapMethodInformation.add(new MethodInfo("substring",
                                                Collections.singletonList(DataType.TYPE_NUMERIC_INTEGER),
                                                "String",
                                                null,
                                                DataType.TYPE_STRING));
        mapMethodInformation.add(new MethodInfo("substring",
                                                Arrays.asList(DataType.TYPE_NUMERIC_INTEGER,
                                                              DataType.TYPE_NUMERIC_INTEGER),
                                                "String",
                                                null,
                                                DataType.TYPE_STRING));

        methodInformation.put("java.lang.String",
                              mapMethodInformation);

        when(dmo.getModuleMethodInformation()).thenReturn(methodInformation);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(3,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionCallMethod);
        assertTrue(m.rhs[1] instanceof ActionCallMethod);
        assertTrue(m.rhs[2] instanceof ActionCallMethod);

        ActionCallMethod actionCallMethod1 = (ActionCallMethod) m.rhs[0];
        assertEquals(1,
                     actionCallMethod1.getState());
        assertEquals("indexOf",
                     actionCallMethod1.getMethodName());
        assertEquals("$var",
                     actionCallMethod1.getVariable());
        assertEquals(1,
                     actionCallMethod1.getFieldValues().length);
        assertEquals("indexOf",
                     actionCallMethod1.getFieldValues()[0].getField());
        assertEquals("$var",
                     actionCallMethod1.getFieldValues()[0].getValue());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     actionCallMethod1.getFieldValues()[0].getNature());
        assertEquals(DataType.TYPE_STRING,
                     actionCallMethod1.getFieldValues()[0].getType());

        ActionCallMethod actionCallMethod2 = (ActionCallMethod) m.rhs[1];
        assertEquals(1,
                     actionCallMethod2.getState());
        assertEquals("endsWith",
                     actionCallMethod2.getMethodName());
        assertEquals("$var",
                     actionCallMethod2.getVariable());
        assertEquals(1,
                     actionCallMethod2.getFieldValues().length);
        assertEquals("endsWith",
                     actionCallMethod2.getFieldValues()[0].getField());
        assertEquals(".",
                     actionCallMethod2.getFieldValues()[0].getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     actionCallMethod2.getFieldValues()[0].getNature());
        assertEquals(DataType.TYPE_STRING,
                     actionCallMethod2.getFieldValues()[0].getType());

        ActionCallMethod actionCallMethod3 = (ActionCallMethod) m.rhs[2];
        assertEquals(1,
                     actionCallMethod3.getState());
        assertEquals("substring",
                     actionCallMethod3.getMethodName());
        assertEquals("$var",
                     actionCallMethod3.getVariable());
        assertEquals(2,
                     actionCallMethod3.getFieldValues().length);
        assertEquals("substring",
                     actionCallMethod3.getFieldValues()[0].getField());
        assertEquals("0",
                     actionCallMethod3.getFieldValues()[0].getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     actionCallMethod3.getFieldValues()[0].getNature());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     actionCallMethod3.getFieldValues()[0].getType());
        assertEquals("substring",
                     actionCallMethod3.getFieldValues()[1].getField());
        assertEquals("1",
                     actionCallMethod3.getFieldValues()[1].getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     actionCallMethod3.getFieldValues()[1].getNature());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     actionCallMethod3.getFieldValues()[1].getType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testFunctionCalls4_MultiParameterSetter() {
        String drl =
                "package org.mortgages;\n"
                        + "import org.mortgages.classes.MyClass;\n"
                        + "rule \"rule1\"\n"
                        + "dialect \"mvel\"\n"
                        + "when\n"
                        + "  $c : MyClass()\n"
                        + "then\n"
                        + "  $c.setSomething(0, 1);\n"
                        + "end\n";

        Map<String, List<MethodInfo>> methodInformation = new HashMap<>();
        List<MethodInfo> mapMethodInformation = new ArrayList<>();
        mapMethodInformation.add(new MethodInfo("setSomething",
                                                Arrays.asList(DataType.TYPE_NUMERIC_INTEGER,
                                                              DataType.TYPE_NUMERIC_INTEGER),
                                                "void",
                                                null,
                                                DataType.TYPE_STRING));

        methodInformation.put("org.mortgages.classes.MyClass",
                              mapMethodInformation);

        when(dmo.getModuleMethodInformation()).thenReturn(methodInformation);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionCallMethod);

        ActionCallMethod actionCallMethod1 = (ActionCallMethod) m.rhs[0];
        assertEquals(1,
                     actionCallMethod1.getState());
        assertEquals("setSomething",
                     actionCallMethod1.getMethodName());
        assertEquals("$c",
                     actionCallMethod1.getVariable());
        assertEquals(2,
                     actionCallMethod1.getFieldValues().length);
        assertEquals("setSomething",
                     actionCallMethod1.getFieldValues()[0].getField());
        assertEquals("0",
                     actionCallMethod1.getFieldValues()[0].getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     actionCallMethod1.getFieldValues()[0].getNature());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     actionCallMethod1.getFieldValues()[0].getType());
        assertEquals("setSomething",
                     actionCallMethod1.getFieldValues()[1].getField());
        assertEquals("1",
                     actionCallMethod1.getFieldValues()[1].getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     actionCallMethod1.getFieldValues()[1].getNature());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     actionCallMethod1.getFieldValues()[1].getType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testRHSInsertFactWithFieldAsVariable() {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1077212

        String drl = "package org.mortgages\n"
                + "import org.test.Person\n"
                + "rule \"variable\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Person( $f : field1 == 44 )\n"
                + "then\n"
                + "Person fact0 = new Person();\n"
                + "fact0.setField1( $f );\n"
                + "insert( fact0 );\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("field1",
                     sfp.getFieldName());
        assertEquals("==",
                     sfp.getOperator());
        assertEquals("44",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
        assertEquals("$f",
                     sfp.getFieldBinding());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionInsertFact);

        ActionInsertFact ap = (ActionInsertFact) a;
        assertEquals("Person",
                     ap.getFactType());
        assertEquals("fact0",
                     ap.getBoundName());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("field1",
                     afv.getField());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     afv.getNature());
        assertEquals("=$f",
                     afv.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     afv.getType());
    }

    @Test
    public void testRHSInsertFactWithFieldAsLiteral() {
        String drl = "package org.mortgages\n"
                + "import org.test.Person\n"
                + "rule \"variable\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Person( field1 == 44 )\n"
                + "then\n"
                + "Person fact0 = new Person();\n"
                + "fact0.setField1( 55 );\n"
                + "insert( fact0 );\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("field1",
                     sfp.getFieldName());
        assertEquals("==",
                     sfp.getOperator());
        assertEquals("44",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionInsertFact);

        ActionInsertFact ap = (ActionInsertFact) a;
        assertEquals("Person",
                     ap.getFactType());
        assertEquals("fact0",
                     ap.getBoundName());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("field1",
                     afv.getField());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     afv.getNature());
        assertEquals("55",
                     afv.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     afv.getType());
    }

    @Test
    public void testRHSUpdateFactWithFieldAsVariable() {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1077212

        String drl = "package org.mortgages\n"
                + "import org.test.Person\n"
                + "rule \"variable\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$p : Person( $f : field1 == 44 )\n"
                + "then\n"
                + "$p.setField1( $f );\n"
                + "update( $p );\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());
        assertEquals("$p",
                     fp.getBoundName());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("field1",
                     sfp.getFieldName());
        assertEquals("==",
                     sfp.getOperator());
        assertEquals("44",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
        assertEquals("$f",
                     sfp.getFieldBinding());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionUpdateField);

        ActionUpdateField ap = (ActionUpdateField) a;
        assertEquals("$p",
                     ap.getVariable());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("field1",
                     afv.getField());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     afv.getNature());
        assertEquals("=$f",
                     afv.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     afv.getType());
    }

    @Test
    public void testRHSUpdateFactWithFieldAsLiteral() {
        String drl = "package org.mortgages\n"
                + "import org.test.Person\n"
                + "rule \"variable\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$p : Person()\n"
                + "then\n"
                + "$p.setField1( 55 );\n"
                + "update( $p );\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionUpdateField);

        ActionUpdateField ap = (ActionUpdateField) a;
        assertEquals("$p",
                     ap.getVariable());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("field1",
                     afv.getField());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     afv.getNature());
        assertEquals("55",
                     afv.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     afv.getType());
    }

    @Test
    public void testRHSInsertFactWithFieldAsVariableSamePackage() {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1077212

        String drl = "package org.test\n"
                + "rule \"variable\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Person( $f : field1 == 44 )\n"
                + "then\n"
                + "Person fact0 = new Person();\n"
                + "fact0.setField1( $f );\n"
                + "insert( fact0 );\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("field1",
                     sfp.getFieldName());
        assertEquals("==",
                     sfp.getOperator());
        assertEquals("44",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
        assertEquals("$f",
                     sfp.getFieldBinding());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionInsertFact);

        ActionInsertFact ap = (ActionInsertFact) a;
        assertEquals("Person",
                     ap.getFactType());
        assertEquals("fact0",
                     ap.getBoundName());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("field1",
                     afv.getField());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     afv.getNature());
        assertEquals("=$f",
                     afv.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     afv.getType());
    }

    @Test
    public void testRHSInsertFactWithFieldAsLiteralSamePackage() {
        String drl = "package org.test\n"
                + "rule \"variable\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Person( field1 == 44 )\n"
                + "then\n"
                + "Person fact0 = new Person();\n"
                + "fact0.setField1( 55 );\n"
                + "insert( fact0 );\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("field1",
                     sfp.getFieldName());
        assertEquals("==",
                     sfp.getOperator());
        assertEquals("44",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionInsertFact);

        ActionInsertFact ap = (ActionInsertFact) a;
        assertEquals("Person",
                     ap.getFactType());
        assertEquals("fact0",
                     ap.getBoundName());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("field1",
                     afv.getField());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     afv.getNature());
        assertEquals("55",
                     afv.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     afv.getType());
    }

    @Test
    public void testRHSUpdateFactWithFieldAsVariableSamePackage() {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1077212

        String drl = "package org.test\n"
                + "rule \"variable\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$p : Person( $f : field1 == 44 )\n"
                + "then\n"
                + "$p.setField1( $f );\n"
                + "update( $p );\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());
        assertEquals("$p",
                     fp.getBoundName());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("field1",
                     sfp.getFieldName());
        assertEquals("==",
                     sfp.getOperator());
        assertEquals("44",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
        assertEquals("$f",
                     sfp.getFieldBinding());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionUpdateField);

        ActionUpdateField ap = (ActionUpdateField) a;
        assertEquals("$p",
                     ap.getVariable());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("field1",
                     afv.getField());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     afv.getNature());
        assertEquals("=$f",
                     afv.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     afv.getType());
    }

    @Test
    public void testRHSUpdateFactWithFieldAsLiteralSamePackage() {
        String drl = "package org.test\n"
                + "rule \"variable\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$p : Person()\n"
                + "then\n"
                + "$p.setField1( 55 );\n"
                + "update( $p );\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionUpdateField);

        ActionUpdateField ap = (ActionUpdateField) a;
        assertEquals("$p",
                     ap.getVariable());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("field1",
                     afv.getField());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     afv.getNature());
        assertEquals("55",
                     afv.getValue());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     afv.getType());
    }

    @Test
    public void testRHSUpdateFactWithFormula() {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1079253
        String drl = "package org.mortgages;\n"
                + "import org.test.ShoppingCart\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$sc : ShoppingCart( )\n"
                + "then\n"
                + "$sc.setCartItemPromoSavings( ($sc.cartItemPromoSavings == 0.0) ? 0.0 : $sc.cartItemPromoSavings * -1 );\n"
                + "update( $sc );\n"
                + "end\n";

        addModelField("org.test.ShoppingCart",
                      "cartItemPromoSavings",
                      "java.lang.Double",
                      DataType.TYPE_NUMERIC_DOUBLE);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("ShoppingCart",
                     fp.getFactType());
        assertEquals("$sc",
                     fp.getBoundName());

        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionUpdateField);

        ActionUpdateField ap = (ActionUpdateField) a;
        assertEquals("$sc",
                     ap.getVariable());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("cartItemPromoSavings",
                     afv.getField());
        assertEquals("($sc.cartItemPromoSavings == 0.0) ? 0.0 : $sc.cartItemPromoSavings * -1",
                     afv.getValue());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     afv.getNature());
    }

    @Test
    public void testRHSUpdateFactWithFormulaSamePackage() {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1079253
        String drl = "package org.test;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$sc : ShoppingCart( )\n"
                + "then\n"
                + "$sc.setCartItemPromoSavings( ($sc.cartItemPromoSavings == 0.0) ? 0.0 : $sc.cartItemPromoSavings * -1 );\n"
                + "update( $sc );\n"
                + "end\n";

        addModelField("org.test.ShoppingCart",
                      "cartItemPromoSavings",
                      "java.lang.Double",
                      DataType.TYPE_NUMERIC_DOUBLE);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("ShoppingCart",
                     fp.getFactType());
        assertEquals("$sc",
                     fp.getBoundName());

        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);
        IAction a = m.rhs[0];
        assertTrue(a instanceof ActionUpdateField);

        ActionUpdateField ap = (ActionUpdateField) a;
        assertEquals("$sc",
                     ap.getVariable());

        assertEquals(1,
                     ap.getFieldValues().length);
        ActionFieldValue afv = ap.getFieldValues()[0];
        assertEquals("cartItemPromoSavings",
                     afv.getField());
        assertEquals("($sc.cartItemPromoSavings == 0.0) ? 0.0 : $sc.cartItemPromoSavings * -1",
                     afv.getValue());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     afv.getNature());
    }

    @Test
    public void testFunctionsWithVariableParameters() throws Exception {
        String drl = "package org.mortgages;\n" +
                "rule \"test\"\n" +
                " dialect \"mvel\"\n" +
                " when\n" +
                "  Calculator( s : summer)\n" +
                "  Applicant( $age : age)\n" +
                " then\n" +
                "  s.sum( $age, $age );\n" +
                "end";

        addModelField(
                "Calculator",
                "summer",
                "Summer",
                "Summer");

        addModelField(
                "Applicant",
                "age",
                "java.lang.Integer",
                DataType.TYPE_NUMERIC_INTEGER);

        HashMap<String, List<MethodInfo>> map = new HashMap<>();
        ArrayList<MethodInfo> methodInfos = new ArrayList<>();
        ArrayList<String> params = new ArrayList<>();
        params.add("Integer");
        params.add("Integer");
        methodInfos.add(new MethodInfo("sum",
                                       params,
                                       "java.lang.Integer",
                                       null,
                                       "Summer"));
        map.put("Calculator",
                methodInfos);

        when(
                dmo.getModuleMethodInformation()
        ).thenReturn(
                map
        );

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(2,
                     m.lhs.length);

        assertEquals(1,
                     m.rhs.length);

        ActionCallMethod actionCallMethod = (ActionCallMethod) m.rhs[0];
        assertEquals("sum",
                     actionCallMethod.getMethodName());
        assertEquals("s",
                     actionCallMethod.getVariable());
        assertEquals(2,
                     actionCallMethod.getFieldValues().length);

        assertEquals("sum",
                     actionCallMethod.getFieldValue(0).getField());
        assertEquals("$age",
                     actionCallMethod.getFieldValue(0).getValue());
        assertEquals(2,
                     actionCallMethod.getFieldValue(0).getNature());
        assertEquals("java.lang.Integer",
                     actionCallMethod.getFieldValue(0).getType());

        assertEquals("sum",
                     actionCallMethod.getFieldValue(1).getField());
        assertEquals("$age",
                     actionCallMethod.getFieldValue(1).getValue());
        assertEquals(2,
                     actionCallMethod.getFieldValue(1).getNature());
        assertEquals("java.lang.Integer",
                     actionCallMethod.getFieldValue(1).getType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testListSize() throws Exception {
        String drl = "" +
                "package org.mortgages;\n" +
                "import java.lang.Number;\n" +
                "rule \"Test\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Person( addresses.size() == 0 )\n" +
                "  then\n" +
                "end\n";

        addModelField("Person",
                      "addresses",
                      "java.util.List",
                      DataType.TYPE_COLLECTION);

        addMethodInformation("java.util.List",
                             "size",
                             Collections.emptyList(),
                             "int",
                             null,
                             DataType.TYPE_NUMERIC_INTEGER);

        HashMap<String, String> map = new HashMap<>();
        map.put("Person#addresses",
                "Address");
        when(dmo.getModuleFieldParametersType()).thenReturn(map);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertEquals(1,
                     m.lhs.length);
        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals(1,
                     pattern.getConstraintList().getConstraints().length);
        SingleFieldConstraintEBLeftSide constraint = (SingleFieldConstraintEBLeftSide) pattern.getConstraint(0);
        assertEquals("size",
                     constraint.getFieldName());
        assertEquals("int",
                     constraint.getFieldType());
        assertEquals("0",
                     constraint.getValue());
        assertEquals("==",
                     constraint.getOperator());
        assertEquals(1,
                     constraint.getConstraintValueType());

        assertTrue(constraint.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        assertTrue(constraint.getExpressionLeftSide().getParts().get(1) instanceof ExpressionCollection);
        assertTrue(constraint.getExpressionLeftSide().getParts().get(2) instanceof ExpressionMethod);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testCollectWithFreeFormDRL_MethodsDefined() throws Exception {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1060816
        String drl = "package org.sample.resourceassignment;\n" +
                "rule \"r1\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "$trans : Transactions()\n" +
                "$transactions : java.util.List( eval( size > 0 ) ) from collect ( Transaction() from $trans.getRecCategorization().get(\"APES-01\") )\n" +
                "then\n" +
                "end";

        addModelField("Transactions",
                      "recCategorization",
                      "java.util.Map",
                      DataType.TYPE_COLLECTION);

        addMethodInformation("Transactions",
                             "getRecCategorization()",
                             Collections.emptyList(),
                             "java.util.Map",
                             null,
                             DataType.TYPE_COLLECTION);

        addMethodInformation("java.util.Map",
                             "get",
                             new ArrayList<String>() {{
                                 add("p0");
                             }},
                             "java.lang.String",
                             null,
                             DataType.TYPE_STRING);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(2,
                     m.lhs.length);
        IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        FactPattern fp0 = (FactPattern) p0;
        assertEquals("Transactions",
                     fp0.getFactType());
        assertEquals("$trans",
                     fp0.getBoundName());
        assertEquals(0,
                     fp0.getNumberOfConstraints());

        IPattern p1 = m.lhs[1];
        assertTrue(p1 instanceof FromCollectCompositeFactPattern);
        FromCollectCompositeFactPattern fp1 = (FromCollectCompositeFactPattern) p1;
        assertEquals("java.util.List",
                     fp1.getFactPattern().getFactType());
        assertEquals("$transactions",
                     fp1.getFactPattern().getBoundName());
        assertEquals(1,
                     fp1.getFactPattern().getNumberOfConstraints());
        assertTrue(fp1.getFactPattern().getConstraint(0) instanceof SingleFieldConstraint);
        SingleFieldConstraint fp1sfc = (SingleFieldConstraint) fp1.getFactPattern().getConstraint(0);
        assertEquals("size > 0",
                     fp1sfc.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_PREDICATE,
                     fp1sfc.getConstraintValueType());

        assertTrue(fp1.getRightPattern() instanceof FromCompositeFactPattern);
        FromCompositeFactPattern fp2 = (FromCompositeFactPattern) fp1.getRightPattern();
        assertNotNull(fp2.getFactPattern());

        FactPattern fp3 = fp2.getFactPattern();
        assertEquals("Transaction",
                     fp3.getFactType());
        assertEquals(0,
                     fp3.getNumberOfConstraints());

        assertNotNull(fp2.getExpression());
        ExpressionFormLine efl = fp2.getExpression();
        assertEquals(3,
                     efl.getParts().size());
        assertTrue(efl.getParts().get(0) instanceof ExpressionVariable);
        ExpressionVariable ev = (ExpressionVariable) efl.getParts().get(0);
        assertEquals("$trans",
                     ev.getName());
        assertEquals("Transactions",
                     ev.getClassType());
        assertTrue(efl.getParts().get(1) instanceof ExpressionMethod);
        ExpressionMethod em = (ExpressionMethod) efl.getParts().get(1);
        assertEquals("getRecCategorization()",
                     em.getName());
        assertEquals("java.util.Map",
                     em.getClassType());
        assertEquals(DataType.TYPE_COLLECTION,
                     em.getGenericType());
        assertTrue(efl.getParts().get(2) instanceof ExpressionText);
        ExpressionText et = (ExpressionText) efl.getParts().get(2);
        assertEquals("get(\"APES-01\")",
                     et.getName());
        assertEquals("java.lang.String",
                     et.getClassType());
        assertEquals(DataType.TYPE_STRING,
                     et.getGenericType());

        assertEquals(0,
                     m.rhs.length);
    }

    @Test
    public void testCollectWithFreeFormDRL_MethodsUndefined() throws Exception {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1060816
        String drl = "package org.sample.resourceassignment;\n" +
                "rule \"r1\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "$trans : Transactions()\n" +
                "$transactions : java.util.List( eval( size > 0 ) ) from collect ( Transaction() from $trans.getRecCategorization().get(\"APES-01\") )\n" +
                "then\n" +
                "end";

        addModelField("Transactions",
                      "recCategorization",
                      "java.util.Map",
                      DataType.TYPE_COLLECTION);

        addMethodInformation("java.util.Map",
                             "get",
                             new ArrayList<String>() {{
                                 add("p0");
                             }},
                             "java.lang.String",
                             null,
                             DataType.TYPE_STRING);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(2,
                     m.lhs.length);
        IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        FactPattern fp0 = (FactPattern) p0;
        assertEquals("Transactions",
                     fp0.getFactType());
        assertEquals("$trans",
                     fp0.getBoundName());
        assertEquals(0,
                     fp0.getNumberOfConstraints());

        IPattern p1 = m.lhs[1];
        assertTrue(p1 instanceof FromCollectCompositeFactPattern);
        FromCollectCompositeFactPattern fp1 = (FromCollectCompositeFactPattern) p1;
        assertEquals("java.util.List",
                     fp1.getFactPattern().getFactType());
        assertEquals("$transactions",
                     fp1.getFactPattern().getBoundName());
        assertEquals(1,
                     fp1.getFactPattern().getNumberOfConstraints());
        assertTrue(fp1.getFactPattern().getConstraint(0) instanceof SingleFieldConstraint);
        SingleFieldConstraint fp1sfc = (SingleFieldConstraint) fp1.getFactPattern().getConstraint(0);
        assertEquals("size > 0",
                     fp1sfc.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_PREDICATE,
                     fp1sfc.getConstraintValueType());

        assertTrue(fp1.getRightPattern() instanceof FromCompositeFactPattern);
        FromCompositeFactPattern fp2 = (FromCompositeFactPattern) fp1.getRightPattern();
        assertNotNull(fp2.getFactPattern());

        FactPattern fp3 = fp2.getFactPattern();
        assertEquals("Transaction",
                     fp3.getFactType());
        assertEquals(0,
                     fp3.getNumberOfConstraints());

        assertNotNull(fp2.getExpression());
        ExpressionFormLine efl = fp2.getExpression();
        assertEquals(3,
                     efl.getParts().size());
        assertTrue(efl.getParts().get(0) instanceof ExpressionVariable);
        ExpressionVariable ev = (ExpressionVariable) efl.getParts().get(0);
        assertEquals("$trans",
                     ev.getName());
        assertEquals("Transactions",
                     ev.getClassType());
        assertTrue(efl.getParts().get(1) instanceof ExpressionText);
        ExpressionText et1 = (ExpressionText) efl.getParts().get(1);
        assertEquals("getRecCategorization()",
                     et1.getName());
        assertEquals("java.lang.String",
                     et1.getClassType());
        assertEquals(DataType.TYPE_STRING,
                     et1.getGenericType());
        assertTrue(efl.getParts().get(2) instanceof ExpressionText);
        ExpressionText et2 = (ExpressionText) efl.getParts().get(2);
        assertEquals("get(\"APES-01\")",
                     et2.getName());
        assertEquals("java.lang.String",
                     et2.getClassType());
        assertEquals(DataType.TYPE_STRING,
                     et2.getGenericType());

        assertEquals(0,
                     m.rhs.length);
    }

    @Test
    public void testLHSInOperatorFieldNameNotContainingInLiteral() {
        String drl = "package org.test\n"
                + "rule \"in\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Person( field1 in (1, 2) )\n"
                + "then\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("field1",
                     sfp.getFieldName());
        assertEquals("in",
                     sfp.getOperator());
        assertEquals("1, 2",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testLHSInOperatorFieldNameContainingInLiteral() {
        String drl = "package org.test\n"
                + "rule \"in\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Person( rating in (1, 2) )\n"
                + "then\n"
                + "end";

        addModelField("org.test.Person",
                      "rating",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("rating",
                     sfp.getFieldName());
        assertEquals("in",
                     sfp.getOperator());
        assertEquals("1, 2",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testLHSInOperatorFieldNameNotContainingNotInLiteral() {
        String drl = "package org.test\n"
                + "rule \"in\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Person( field1 not in (1, 2) )\n"
                + "then\n"
                + "end";

        addModelField("org.test.Person",
                      "field1",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("field1",
                     sfp.getFieldName());
        assertEquals("not in",
                     sfp.getOperator());
        assertEquals("1, 2",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testLHSInOperatorFieldNameContainingNotInLiteral() {
        String drl = "package org.test\n"
                + "rule \"in\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Person( rating not in (1, 2) )\n"
                + "then\n"
                + "end";

        addModelField("org.test.Person",
                      "rating",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Person",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Person",
                     sfp.getFactType());
        assertEquals("rating",
                     sfp.getFieldName());
        assertEquals("not in",
                     sfp.getOperator());
        assertEquals("1, 2",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testRHSModifyBlockSingleFieldSingleLine() throws Exception {
        //The value used in the "set" is intentionally yucky to catch extraction of the field's value errors!
        String drl = "rule \"modify1\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    $p : Person( )\n" +
                "  then\n" +
                "  modify( $p ) { setFirstName( \",)\" ) }\n" +
                "end";

        addModelField("Person",
                      "firstName",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionUpdateField);

        ActionUpdateField field = (ActionUpdateField) m.rhs[0];
        assertEquals("$p",
                     field.getVariable());

        assertNotNull(field.getFieldValues()[0]);
        assertEquals(1,
                     field.getFieldValues().length);

        ActionFieldValue value = field.getFieldValues()[0];
        assertEquals("firstName",
                     value.getField());
        assertEquals(",)",
                     value.getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     value.getNature());
        assertEquals(DataType.TYPE_STRING,
                     value.getType());
    }

    @Test
    public void testRHSModifyBlockSingleFieldMultipleLines() throws Exception {
        //The value used in the "set" is intentionally yucky to catch extraction of the field's value errors!
        String drl = "rule \"modify1\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    $p : Person( )\n" +
                "  then\n" +
                "  modify( $p ) {\n" +
                "    setFirstName( \",)\" )\n" +
                "  }\n" +
                "end";

        addModelField("Person",
                      "firstName",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionUpdateField);

        ActionUpdateField field = (ActionUpdateField) m.rhs[0];
        assertEquals("$p",
                     field.getVariable());

        assertNotNull(field.getFieldValues()[0]);
        assertEquals(1,
                     field.getFieldValues().length);

        ActionFieldValue value = field.getFieldValues()[0];
        assertEquals("firstName",
                     value.getField());
        assertEquals(",)",
                     value.getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     value.getNature());
        assertEquals(DataType.TYPE_STRING,
                     value.getType());
    }

    @Test
    public void testRHSModifyBlockMultipleFieldsSingleLine() throws Exception {
        //The value used in the "set" is intentionally yucky to catch extraction of the field's value errors!
        String drl = "rule \"modify1\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    $p : Person( )\n" +
                "  then\n" +
                "  modify( $p ) { setFirstName( \",)\" ), setLastName( \",)\" ) }\n" +
                "end";

        addModelField("Person",
                      "firstName",
                      "java.lang.String",
                      DataType.TYPE_STRING);
        addModelField("Person",
                      "lastName",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionUpdateField);

        ActionUpdateField field = (ActionUpdateField) m.rhs[0];
        assertEquals("$p",
                     field.getVariable());

        assertNotNull(field.getFieldValues()[0]);
        assertEquals(2,
                     field.getFieldValues().length);

        ActionFieldValue value1 = field.getFieldValues()[0];
        assertEquals("firstName",
                     value1.getField());
        assertEquals(",)",
                     value1.getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     value1.getNature());
        assertEquals(DataType.TYPE_STRING,
                     value1.getType());

        ActionFieldValue value2 = field.getFieldValues()[1];
        assertEquals("lastName",
                     value2.getField());
        assertEquals(",)",
                     value2.getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     value2.getNature());
        assertEquals(DataType.TYPE_STRING,
                     value2.getType());
    }

    @Test
    public void testRHSModifyBlockMultipleFieldsMultipleLines() throws Exception {
        //The value used in the "set" is intentionally yucky to catch extraction of the field's value errors!
        String drl = "rule \"modify1\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    $p : Person( )\n" +
                "  then\n" +
                "  modify( $p ) { \n" +
                "    setFirstName( \",)\" ), \n" +
                "    setLastName( \",)\" )\n" +
                "  }\n" +
                "end";

        addModelField("Person",
                      "firstName",
                      "java.lang.String",
                      DataType.TYPE_STRING);
        addModelField("Person",
                      "lastName",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertEquals(1,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof ActionUpdateField);

        ActionUpdateField field = (ActionUpdateField) m.rhs[0];
        assertEquals("$p",
                     field.getVariable());

        assertNotNull(field.getFieldValues()[0]);
        assertEquals(2,
                     field.getFieldValues().length);

        ActionFieldValue value1 = field.getFieldValues()[0];
        assertEquals("firstName",
                     value1.getField());
        assertEquals(",)",
                     value1.getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     value1.getNature());
        assertEquals(DataType.TYPE_STRING,
                     value1.getType());

        ActionFieldValue value2 = field.getFieldValues()[1];
        assertEquals("lastName",
                     value2.getField());
        assertEquals(",)",
                     value2.getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     value2.getNature());
        assertEquals(DataType.TYPE_STRING,
                     value2.getType());
    }

    @Test
    public void testLiteralStrFieldNames() throws Exception {
        //The issue is fields that contain the "str" operator literal value
        String drl = "rule \"rule1\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Room( decoration == \"tapestry\" , strangeField == 11 )\n" +
                "  then\n" +
                "end";

        addModelField("Room",
                      "decoration",
                      "java.lang.String",
                      DataType.TYPE_STRING);
        addModelField("Room",
                      "strangeField",
                      "java.lang.Integer",
                      DataType.TYPE_NUMERIC_INTEGER);

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Room",
                     fp.getFactType());

        assertEquals(2,
                     fp.getNumberOfConstraints());
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp0 = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Room",
                     sfp0.getFactType());
        assertEquals("decoration",
                     sfp0.getFieldName());
        assertEquals("==",
                     sfp0.getOperator());
        assertEquals("tapestry",
                     sfp0.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp0.getConstraintValueType());
        assertEquals(DataType.TYPE_STRING,
                     sfp0.getFieldType());

        SingleFieldConstraint sfp1 = (SingleFieldConstraint) fp.getConstraint(1);
        assertEquals("Room",
                     sfp1.getFactType());
        assertEquals("strangeField",
                     sfp1.getFieldName());
        assertEquals("==",
                     sfp1.getOperator());
        assertEquals("11",
                     sfp1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp1.getConstraintValueType());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     sfp1.getFieldType());
    }

    @Test
    public void testMethodCallWithTwoParametersIntegerAndString() throws Exception {
        String drl = "package org.mortgages;\n" +
                "rule \"test\"\n" +
                " dialect \"mvel\"\n" +
                " when\n" +
                "  $t : TestClass()\n" +
                " then\n" +
                "  $t.testFunction( 123, \"hello\" );\n" +
                "end";

        final HashMap<String, List<MethodInfo>> map = new HashMap<>();
        final ArrayList<MethodInfo> methodInfos = new ArrayList<>();
        final ArrayList<String> params = new ArrayList<>();
        params.add("Integer");
        params.add("String");
        methodInfos.add(new MethodInfo("testFunction",
                                       params,
                                       "java.lang.Void",
                                       null,
                                       "TestClass"));
        map.put("TestClass",
                methodInfos);

        when(dmo.getModuleMethodInformation()).thenReturn(map);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);

        assertEquals(1,
                     m.rhs.length);

        ActionCallMethod actionCallMethod = (ActionCallMethod) m.rhs[0];
        assertEquals("testFunction",
                     actionCallMethod.getMethodName());
        assertEquals("$t",
                     actionCallMethod.getVariable());
        assertEquals(2,
                     actionCallMethod.getFieldValues().length);

        assertEquals("testFunction",
                     actionCallMethod.getFieldValue(0).getField());
        assertEquals("123",
                     actionCallMethod.getFieldValue(0).getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     actionCallMethod.getFieldValue(0).getNature());
        assertEquals("Integer",
                     actionCallMethod.getFieldValue(0).getType());

        assertEquals("testFunction",
                     actionCallMethod.getFieldValue(1).getField());
        assertEquals("hello",
                     actionCallMethod.getFieldValue(1).getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     actionCallMethod.getFieldValue(1).getNature());
        assertEquals("String",
                     actionCallMethod.getFieldValue(1).getType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testLHSNumberExpressionWithoutThisPrefix() throws Exception {
        String drl = "package org.mortgages;\n" +
                "import java.lang.Number\n" +
                "rule \"test\"\n" +
                " dialect \"mvel\"\n" +
                " when\n" +
                "  Number( intValue() > 5 )\n" +
                " then\n" +
                "end";

        final HashMap<String, List<MethodInfo>> map = new HashMap<>();
        final ArrayList<MethodInfo> methodInfos = new ArrayList<>();
        methodInfos.add(new MethodInfo("intValue",
                                       Collections.emptyList(),
                                       "int",
                                       null,
                                       DataType.TYPE_NUMERIC_INTEGER));
        map.put("java.lang.Number",
                methodInfos);

        when(dmo.getModuleMethodInformation()).thenReturn(map);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);

        assertTrue(m.lhs[0] instanceof FactPattern);
        final FactPattern fp = (FactPattern) m.lhs[0];
        assertEquals("Number",
                     fp.getFactType());

        assertEquals(1,
                     fp.getNumberOfConstraints());
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraintEBLeftSide);
        final SingleFieldConstraintEBLeftSide exp = (SingleFieldConstraintEBLeftSide) fp.getConstraint(0);
        assertEquals("int",
                     exp.getFieldType());
        assertEquals(">",
                     exp.getOperator());
        assertEquals("5",
                     exp.getValue());

        assertEquals(2,
                     exp.getExpressionLeftSide().getParts().size());
        assertTrue(exp.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        final ExpressionUnboundFact expPart0 = (ExpressionUnboundFact) exp.getExpressionLeftSide().getParts().get(0);
        assertEquals("Number",
                     expPart0.getFactType());

        assertTrue(exp.getExpressionLeftSide().getParts().get(1) instanceof ExpressionMethod);
        final ExpressionMethod expPart1 = (ExpressionMethod) exp.getExpressionLeftSide().getParts().get(1);
        assertEquals("intValue",
                     expPart1.getName());
    }

    @Test
    public void testLHSNumberExpressionWithThisPrefix() throws Exception {
        String drl = "package org.mortgages;\n" +
                "import java.lang.Number\n" +
                "rule \"test\"\n" +
                " dialect \"mvel\"\n" +
                " when\n" +
                "  Number( this.intValue() > 5 )\n" +
                " then\n" +
                "end";

        final HashMap<String, List<MethodInfo>> map = new HashMap<>();
        final ArrayList<MethodInfo> methodInfos = new ArrayList<>();
        methodInfos.add(new MethodInfo("intValue",
                                       Collections.emptyList(),
                                       "int",
                                       null,
                                       DataType.TYPE_NUMERIC_INTEGER));
        map.put("java.lang.Number",
                methodInfos);

        when(dmo.getModuleMethodInformation()).thenReturn(map);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);

        assertTrue(m.lhs[0] instanceof FactPattern);
        final FactPattern fp = (FactPattern) m.lhs[0];
        assertEquals("Number",
                     fp.getFactType());

        assertEquals(1,
                     fp.getNumberOfConstraints());
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraintEBLeftSide);
        final SingleFieldConstraintEBLeftSide exp = (SingleFieldConstraintEBLeftSide) fp.getConstraint(0);
        assertEquals("int",
                     exp.getFieldType());
        assertEquals(">",
                     exp.getOperator());
        assertEquals("5",
                     exp.getValue());

        assertEquals(3,
                     exp.getExpressionLeftSide().getParts().size());
        assertTrue(exp.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        final ExpressionUnboundFact expPart0 = (ExpressionUnboundFact) exp.getExpressionLeftSide().getParts().get(0);
        assertEquals("Number",
                     expPart0.getFactType());

        assertTrue(exp.getExpressionLeftSide().getParts().get(1) instanceof ExpressionField);
        final ExpressionField expPart1 = (ExpressionField) exp.getExpressionLeftSide().getParts().get(1);
        assertEquals("this",
                     expPart1.getName());

        assertTrue(exp.getExpressionLeftSide().getParts().get(2) instanceof ExpressionMethod);
        final ExpressionMethod expPart2 = (ExpressionMethod) exp.getExpressionLeftSide().getParts().get(2);
        assertEquals("intValue",
                     expPart2.getName());
    }

    @Test
    public void testLHSNestedMethodCalls() throws Exception {
        String drl = "package org.mortgages;\n" +
                "rule \"test\"\n" +
                " dialect \"mvel\"\n" +
                " when\n" +
                "  Parent( methodToGetChild1().methodToGetChild2().field1 > 5 )\n" +
                " then\n" +
                "end";

        addMethodInformation("Parent",
                             "methodToGetChild1",
                             Collections.emptyList(),
                             "Child1",
                             null,
                             "Child1");
        addMethodInformation("Child1",
                             "methodToGetChild2",
                             Collections.emptyList(),
                             "Child2",
                             null,
                             "Child2");
        addModelField("Child2",
                      "field1",
                      "int",
                      DataType.TYPE_NUMERIC_INTEGER);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);

        assertTrue(m.lhs[0] instanceof FactPattern);
        final FactPattern fp = (FactPattern) m.lhs[0];
        assertEquals("Parent",
                     fp.getFactType());

        assertEquals(1,
                     fp.getNumberOfConstraints());
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraintEBLeftSide);
        final SingleFieldConstraintEBLeftSide exp = (SingleFieldConstraintEBLeftSide) fp.getConstraint(0);
        assertEquals("int",
                     exp.getFieldType());
        assertEquals(">",
                     exp.getOperator());
        assertEquals("5",
                     exp.getValue());

        assertEquals(4,
                     exp.getExpressionLeftSide().getParts().size());
        assertTrue(exp.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        final ExpressionUnboundFact expPart0 = (ExpressionUnboundFact) exp.getExpressionLeftSide().getParts().get(0);
        assertEquals("Parent",
                     expPart0.getFactType());

        assertTrue(exp.getExpressionLeftSide().getParts().get(1) instanceof ExpressionMethod);
        final ExpressionMethod expPart1 = (ExpressionMethod) exp.getExpressionLeftSide().getParts().get(1);
        assertEquals("methodToGetChild1",
                     expPart1.getName());

        assertTrue(exp.getExpressionLeftSide().getParts().get(2) instanceof ExpressionMethod);
        final ExpressionMethod expPart2 = (ExpressionMethod) exp.getExpressionLeftSide().getParts().get(2);
        assertEquals("methodToGetChild2",
                     expPart2.getName());

        assertTrue(exp.getExpressionLeftSide().getParts().get(3) instanceof ExpressionField);
        final ExpressionField expPart3 = (ExpressionField) exp.getExpressionLeftSide().getParts().get(3);
        assertEquals("field1",
                     expPart3.getName());
    }

    @Test
    public void testLHSMissingConstraints() {
        String drl = "package org.mortgages;\n" +
                "import java.lang.Number;\n" +
                "import org.drools.workbench.models.commons.backend.rule.classes.SearchContext;\n" +
                "import org.drools.workbench.models.commons.backend.rule.classes.ProducerMasterForRules;\n" +
                "import org.drools.workbench.models.commons.backend.rule.classes.RuleFactor;\n" +

                "rule \"SecondaryCuisineRepeatUsage\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  $searchContext : SearchContext( lastThreeCuisines != null )\n" +
                "  ProducerMasterForRules( primaryCuisine != null, primaryCuisine != $searchContext.lastThreeCuisines, secondaryCuisine != null, secondaryCuisine == $searchContext.lastThreeCuisines )\n" +
                "  $secondaryCuisineRepeatUsageFactor : RuleFactor( )\n" +
                "then\n" +
                "  modify( $secondaryCuisineRepeatUsageFactor ) {\n" +
                "    setWeightageImpact(-30)\n" +
                "  }\n" +
                "end\n";

        addModelField("org.drools.workbench.models.commons.backend.rule.classes.SearchContext",
                      "this",
                      "org.drools.workbench.models.commons.backend.rule.classes.SearchContext",
                      DataType.TYPE_THIS);

        addModelField("org.drools.workbench.models.commons.backend.rule.classes.SearchContext",
                      "lastThreeCuisines",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        addModelField("org.drools.workbench.models.commons.backend.rule.classes.ProducerMasterForRules",
                      "primaryCuisine",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        addModelField("org.drools.workbench.models.commons.backend.rule.classes.ProducerMasterForRules",
                      "secondaryCuisine",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testLHSNestedMethodOneParameter() {
        String drl = "package org.mortgages;\n" +
                "import java.lang.Number;\n" +
                "import java.util.List;\n" +
                "import org.drools.workbench.models.commons.backend.rule.classes.MyListContainerClass;\n" +
                "import org.drools.workbench.models.commons.backend.rule.classes.MyStringContainerClass;\n" +
                "rule \"r1\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  $foo : MyListContainerClass()\n" +
                "  $bar : MyStringContainerClass( myString == $foo.myList.get(1))\n" +
                "then\n" +
                "end\n";

        addModelField("org.drools.workbench.models.commons.backend.rule.classes.MyListContainerClass",
                      "this",
                      "org.drools.workbench.models.commons.backend.rule.classes.MyListContainerClass",
                      DataType.TYPE_THIS);
        addModelField("org.drools.workbench.models.commons.backend.rule.classes.MyListContainerClass",
                      "myList",
                      "java.util.List",
                      "java.util.List");

        addModelField("org.drools.workbench.models.commons.backend.rule.classes.MyStringContainerClass",
                      "this",
                      "org.drools.workbench.models.commons.backend.rule.classes.MyStringContainerClass",
                      DataType.TYPE_THIS);
        addModelField("org.drools.workbench.models.commons.backend.rule.classes.MyStringContainerClass",
                      "myString",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        addMethodInformation("java.util.List",
                             "get",
                             new ArrayList<String>() {{
                                 add("Integer");
                             }},
                             "java.lang.Object",
                             null,
                             "java.lang.Object");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testLHSNestedMethodTwoParameters() {
        String drl = "package org.mortgages;\n" +
                "import java.lang.Number;\n" +
                "import java.util.List;\n" +
                "import org.drools.workbench.models.commons.backend.rule.classes.MyListContainerClass;\n" +
                "import org.drools.workbench.models.commons.backend.rule.classes.MyStringContainerClass;\n" +
                "rule \"r1\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  $foo : MyListContainerClass()\n" +
                "  $bar : MyStringContainerClass( $foo.myList.set(1, \"hello\" ) == true )\n" +
                "then\n" +
                "end\n";

        addModelField("org.drools.workbench.models.commons.backend.rule.classes.MyListContainerClass",
                      "this",
                      "org.drools.workbench.models.commons.backend.rule.classes.MyListContainerClass",
                      DataType.TYPE_THIS);
        addModelField("org.drools.workbench.models.commons.backend.rule.classes.MyListContainerClass",
                      "myList",
                      "java.util.List",
                      "java.util.List");

        addModelField("org.drools.workbench.models.commons.backend.rule.classes.MyStringContainerClass",
                      "this",
                      "org.drools.workbench.models.commons.backend.rule.classes.MyStringContainerClass",
                      DataType.TYPE_THIS);
        addModelField("org.drools.workbench.models.commons.backend.rule.classes.MyStringContainerClass",
                      "myString",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        addModelField("java.util.List",
                      "this",
                      "java.util.List",
                      DataType.TYPE_THIS);
        addMethodInformation("java.util.List",
                             "set",
                             new ArrayList<String>() {{
                                 add("Integer");
                                 add("String");
                             }},
                             DataType.TYPE_BOOLEAN,
                             null,
                             DataType.TYPE_BOOLEAN);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1160658
    public void testRHSFormulaTruncation() throws Exception {
        String drl = "package org.test;\n"
                + "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "  $p : Person()\n"
                + "then\n"
                + "  $p.setTextOut( $p.getType() + \"\" );\n"
                + "end";

        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "textOut",
                      "java.lang.String",
                      DataType.TYPE_STRING);
        addModelField("org.test.Person",
                      "type",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        assertEquals(1,
                     m.rhs.length);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1158813
    public void testLHSFromCollectWithoutListDeclared() throws Exception {
        String drl = "package org.test;\n"
                + "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "  c : Customer( )\n"
                + "  items : java.util.List( eval( size == c.items.size )) \n"
                + "  from collect ( var : Item( price > 10 )) \n"
                + "then \n"
                + "end";

        addModelField("org.test.Customer",
                      "this",
                      "org.test.Customer",
                      DataType.TYPE_THIS);
        addModelField("org.test.Item",
                      "this",
                      "org.test.Item",
                      DataType.TYPE_THIS);
        addModelField("org.test.Item",
                      "price",
                      "java.lang.Double",
                      DataType.TYPE_NUMERIC_DOUBLE);

        addMethodInformation("org.test.Customer",
                             "items",
                             new ArrayList<String>() {{
                             }},
                             DataType.TYPE_NUMERIC_INTEGER,
                             null,
                             DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(2,
                     m.lhs.length);
        assertEquals(0,
                     m.rhs.length);

        assertTrue(m.lhs[0] instanceof FactPattern);
        final FactPattern fp = (FactPattern) m.lhs[0];
        assertEquals("Customer",
                     fp.getFactType());
        assertEquals("c",
                     fp.getBoundName());
        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertTrue(m.lhs[1] instanceof FromCollectCompositeFactPattern);
        final FromCollectCompositeFactPattern fcfp = (FromCollectCompositeFactPattern) m.lhs[1];
        assertNotNull(fcfp.getFactPattern());
        assertEquals("java.util.List",
                     fcfp.getFactPattern().getFactType());
        assertEquals("items",
                     fcfp.getFactPattern().getBoundName());
        assertEquals(1,
                     fcfp.getFactPattern().getNumberOfConstraints());
        assertTrue(fcfp.getFactPattern().getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint sfc0 = (SingleFieldConstraint) fcfp.getFactPattern().getConstraint(0);
        assertNull(sfc0.getFactType());
        assertNull(sfc0.getFieldName());
        assertEquals("size == c.items.size",
                     sfc0.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_PREDICATE,
                     sfc0.getConstraintValueType());

        assertNotNull(fcfp.getRightPattern());
        assertTrue(fcfp.getRightPattern() instanceof FactPattern);
        final FactPattern rfp = (FactPattern) fcfp.getRightPattern();
        assertEquals("Item",
                     rfp.getFactType());
        assertEquals("var",
                     rfp.getBoundName());
        assertEquals(1,
                     rfp.getNumberOfConstraints());
        assertTrue(rfp.getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) rfp.getConstraint(0);
        assertEquals("Item",
                     sfc1.getFactType());
        assertEquals("price",
                     sfc1.getFieldName());
        assertEquals("10",
                     sfc1.getValue());
        assertEquals(">",
                     sfc1.getOperator());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfc1.getConstraintValueType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1158813
    public void testLHSFromCollectWithListDeclared() throws Exception {
        String drl = "package org.test;\n"
                + "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "  c : Customer( )\n"
                + "  items : List( eval( size == c.items.size )) \n"
                + "  from collect ( var : Item( price > 10 )) \n"
                + "then \n"
                + "end";

        addModelField("org.test.Customer",
                      "this",
                      "org.test.Customer",
                      DataType.TYPE_THIS);
        addModelField("org.test.Item",
                      "this",
                      "org.test.Item",
                      DataType.TYPE_THIS);
        addModelField("org.test.Item",
                      "price",
                      "java.lang.Double",
                      DataType.TYPE_NUMERIC_DOUBLE);
        addModelField("java.util.List",
                      "this",
                      "java.util.List",
                      DataType.TYPE_THIS);

        addMethodInformation("org.test.Customer",
                             "items",
                             new ArrayList<String>() {{
                             }},
                             DataType.TYPE_NUMERIC_INTEGER,
                             null,
                             DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(2,
                     m.lhs.length);
        assertEquals(0,
                     m.rhs.length);

        assertTrue(m.lhs[0] instanceof FactPattern);
        final FactPattern fp = (FactPattern) m.lhs[0];
        assertEquals("Customer",
                     fp.getFactType());
        assertEquals("c",
                     fp.getBoundName());
        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertTrue(m.lhs[1] instanceof FromCollectCompositeFactPattern);
        final FromCollectCompositeFactPattern fcfp = (FromCollectCompositeFactPattern) m.lhs[1];
        assertNotNull(fcfp.getFactPattern());
        assertEquals("List",
                     fcfp.getFactPattern().getFactType());
        assertEquals("items",
                     fcfp.getFactPattern().getBoundName());
        assertEquals(1,
                     fcfp.getFactPattern().getNumberOfConstraints());
        assertTrue(fcfp.getFactPattern().getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint sfc0 = (SingleFieldConstraint) fcfp.getFactPattern().getConstraint(0);
        assertNull(sfc0.getFactType());
        assertNull(sfc0.getFieldName());
        assertEquals("size == c.items.size",
                     sfc0.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_PREDICATE,
                     sfc0.getConstraintValueType());

        assertNotNull(fcfp.getRightPattern());
        assertTrue(fcfp.getRightPattern() instanceof FactPattern);
        final FactPattern rfp = (FactPattern) fcfp.getRightPattern();
        assertEquals("Item",
                     rfp.getFactType());
        assertEquals("var",
                     rfp.getBoundName());
        assertEquals(1,
                     rfp.getNumberOfConstraints());
        assertTrue(rfp.getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) rfp.getConstraint(0);
        assertEquals("Item",
                     sfc1.getFactType());
        assertEquals("price",
                     sfc1.getFieldName());
        assertEquals("10",
                     sfc1.getValue());
        assertEquals(">",
                     sfc1.getOperator());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfc1.getConstraintValueType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1134067
    public void testRHSFormulaTruncationInsertFact() throws Exception {
        String drl = "package org.test;\n"
                + "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "  Person( $t : name )\n"
                + "then \n"
                + "  Person fact0 = new Person();\n"
                + "  fact0.setName( $t );\n"
                + "  insert( fact0 );\n"
                + "end";

        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "name",
                      "java.lang.String",
                      DataType.TYPE_STRING);
        addModelField("org.test.Person",
                      "type",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        assertEquals(1,
                     m.rhs.length);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/GUVNOR-2144
    public void testRuleModelPersistenceHelperUnwrapParenthesis() throws Exception {
        String drl = "package org.test;\n"
                + "rule \"Load all schedules\"\n"
                + "dialect \"mvel\"\n"
                + "agenda-group \"LoadSchedules\"\n"
                + "salience 900\n"
                + "  when\n"
                + "    $bundle : Bundle( evaluated == false )\n"
                + "  then\n"
                + "    $bundle.setEvaluated( true );\n"
                + "    update( $bundle );\n"
                + "end";

        //Expected is different as we convert "update" to "modify" blocks
        String expected = "package org.test;\n"
                + "rule \"Load all schedules\"\n"
                + "dialect \"mvel\"\n"
                + "agenda-group \"LoadSchedules\"\n"
                + "salience 900\n"
                + "  when\n"
                + "    $bundle : Bundle( evaluated == false )\n"
                + "  then\n"
                + "    modify( $bundle ) {\n"
                + "      setEvaluated( true )\n"
                + "    }\n"
                + "end";

        addModelField("org.test.Bundle",
                      "this",
                      "org.test.Bundle",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "evaluated",
                      Boolean.class.getName(),
                      DataType.TYPE_BOOLEAN);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        assertEquals(1,
                     m.rhs.length);
        assertEquals(3,
                     m.attributes.length);

        assertEqualsIgnoreWhitespace(expected,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/GUVNOR-2141
    public void testSingleFieldConstraintConnectives1() {
        String drl = "package org.test;\n"
                + "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "  when\n"
                + "    Applicant( age < 55 || > 75 )\n"
                + "  then\n"
                + "end";

        addModelField("org.test.Applicant",
                      "this",
                      "org.test.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.test.Applicant",
                      "age",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("age",
                     sfp.getFieldName());
        assertEquals("<",
                     sfp.getOperator());
        assertEquals("55",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());

        assertEquals(1,
                     sfp.getConnectives().length);
        ConnectiveConstraint cc = sfp.getConnectives()[0];
        assertEquals("Applicant",
                     cc.getFactType());
        assertEquals("age",
                     cc.getFieldName());
        assertEquals("|| >",
                     cc.getOperator());
        assertEquals("75",
                     cc.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cc.getConstraintValueType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/GUVNOR-2141
    public void testSingleFieldConstraintConnectives2() {
        String drl = "package org.test;\n"
                + "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "  when\n"
                + "    Applicant( age == 55 || == 75 )\n"
                + "  then\n"
                + "end";

        addModelField("org.test.Applicant",
                      "this",
                      "org.test.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.test.Applicant",
                      "age",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("age",
                     sfp.getFieldName());
        assertEquals("==",
                     sfp.getOperator());
        assertEquals("55",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());

        assertEquals(1,
                     sfp.getConnectives().length);
        ConnectiveConstraint cc = sfp.getConnectives()[0];
        assertEquals("Applicant",
                     cc.getFactType());
        assertEquals("age",
                     cc.getFieldName());
        assertEquals("|| ==",
                     cc.getOperator());
        assertEquals("75",
                     cc.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cc.getConstraintValueType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/RHBRMS-2854
    public void testSingleFieldConstraintConnectives3() {
        String drl = "package org.test;\n" +
                "rule \"rule1\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "    Applicant(age != 1 && < 10 && > 5)\n" +
                "then\n" +
                "end";

        addModelField("org.test.Applicant",
                      "this",
                      "org.test.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.test.Applicant",
                      "age",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("age",
                     sfp.getFieldName());
        assertEquals("!=",
                     sfp.getOperator());
        assertEquals("1",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());

        assertEquals(2,
                     sfp.getConnectives().length);
        ConnectiveConstraint cc1 = sfp.getConnectives()[0];
        assertEquals("Applicant",
                     cc1.getFactType());
        assertEquals("age",
                     cc1.getFieldName());
        assertEquals("&& <",
                     cc1.getOperator());
        assertEquals("10",
                     cc1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cc1.getConstraintValueType());

        ConnectiveConstraint cc2 = sfp.getConnectives()[1];
        assertEquals("Applicant",
                     cc2.getFactType());
        assertEquals("age",
                     cc2.getFieldName());
        assertEquals("&& >",
                     cc2.getOperator());
        assertEquals("5",
                     cc2.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cc2.getConstraintValueType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/RHBRMS-2854
    public void testSingleFieldConstraintConnectives4() {
        String drl = "package org.test;\n" +
                "rule \"rule1\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "    Applicant(age != null && != 1 && < 10 && > 5)\n" +
                "then\n" +
                "end";

        addModelField("org.test.Applicant",
                      "this",
                      "org.test.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.test.Applicant",
                      "age",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("age",
                     sfp.getFieldName());
        assertEquals("!= null",
                     sfp.getOperator());
        assertNull(sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_UNDEFINED,
                     sfp.getConstraintValueType());

        assertEquals(3,
                     sfp.getConnectives().length);
        ConnectiveConstraint cc1 = sfp.getConnectives()[0];
        assertEquals("Applicant",
                     cc1.getFactType());
        assertEquals("age",
                     cc1.getFieldName());
        assertEquals("&& !=",
                     cc1.getOperator());
        assertEquals("1",
                     cc1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cc1.getConstraintValueType());

        ConnectiveConstraint cc2 = sfp.getConnectives()[1];
        assertEquals("Applicant",
                     cc2.getFactType());
        assertEquals("age",
                     cc2.getFieldName());
        assertEquals("&& <",
                     cc2.getOperator());
        assertEquals("10",
                     cc2.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cc2.getConstraintValueType());

        ConnectiveConstraint cc3 = sfp.getConnectives()[2];
        assertEquals("Applicant",
                     cc3.getFactType());
        assertEquals("age",
                     cc3.getFieldName());
        assertEquals("&& >",
                     cc3.getOperator());
        assertEquals("5",
                     cc3.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     cc3.getConstraintValueType());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/GUVNOR-2143
    public void testNewKeywordVariableNamePrefix1() {
        String oldValue = System.getProperty("drools.dateformat");
        try {

            System.setProperty("drools.dateformat",
                               "dd-MMM-yyyy");

            String drl = "package org.test;\n"
                    + "rule \"rule1\"\n"
                    + "  dialect \"java\"\n"
                    + "  when\n"
                    + "    $bundle : Bundle( $treatmentEffectiveDt : treatmentEffectiveDt )\n"
                    + "  then\n"
                    + "    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(\"dd-MMM-yyyy\");\n"
                    + "    DateTime newStartDate = new DateTime();\n"
                    + "    modify( $bundle ) {\n"
                    + "      setTreatmentEffectiveDt( newStartDate.toDate() )"
                    + "    }\n"
                    + "end\n";

            addModelField("org.test.Bundle",
                          "this",
                          "org.test.Bundle",
                          DataType.TYPE_THIS);
            addModelField("org.test.Bundle",
                          "treatmentEffectiveDt",
                          Date.class.getName(),
                          DataType.TYPE_DATE);

            when(dmo.getPackageName()).thenReturn("org.test");

            RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                              Collections.emptyList(),
                                                                              dmo);

            assertNotNull(m);
            assertEquals("rule1",
                         m.name);

            assertEquals(1,
                         m.lhs.length);
            IPattern p = m.lhs[0];
            assertTrue(p instanceof FactPattern);

            FactPattern fp = (FactPattern) p;
            assertEquals("Bundle",
                         fp.getFactType());
            assertEquals("$bundle",
                         fp.getBoundName());

            assertEquals(1,
                         fp.getConstraintList().getConstraints().length);
            assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

            SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
            assertEquals("Bundle",
                         sfp.getFactType());
            assertEquals("treatmentEffectiveDt",
                         sfp.getFieldName());
            assertEquals("$treatmentEffectiveDt",
                         sfp.getFieldBinding());
            assertNull(sfp.getOperator());
            assertNull(sfp.getValue());
            assertEquals(BaseSingleFieldConstraint.TYPE_UNDEFINED,
                         sfp.getConstraintValueType());

            assertEquals(2,
                         m.rhs.length);

            assertTrue(m.rhs[0] instanceof FreeFormLine);
            FreeFormLine ffl = (FreeFormLine) m.rhs[0];
            assertEquals("DateTime newStartDate = new DateTime();",
                         ffl.getText());
            assertDateBoilerPlateCodeIsNotUnmarshaledToFreeFormLine(m.rhs);

            assertTrue(m.rhs[1] instanceof ActionUpdateField);
            ActionUpdateField auf = (ActionUpdateField) m.rhs[1];
            assertEquals("$bundle",
                         auf.getVariable());
            assertEquals(1,
                         auf.getFieldValues().length);
            ActionFieldValue afv = auf.getFieldValues()[0];
            assertEquals("treatmentEffectiveDt",
                         afv.getField());
            assertEquals("newStartDate.toDate()",
                         afv.getValue());
            assertEquals(FieldNatureType.TYPE_FORMULA,
                         afv.getNature());

            assertEqualsIgnoreWhitespace(drl,
                                         RuleModelDRLPersistenceImpl.getInstance().marshal(m));
        } finally {
            if (oldValue == null) {
                System.clearProperty("drools.dateformat");
            } else {
                System.setProperty("drools.dateformat",
                                   oldValue);
            }
        }
    }

    @Test
    //https://issues.jboss.org/browse/GUVNOR-2143
    public void testNewKeywordVariableNamePrefix2() {
        String oldValue = System.getProperty("drools.dateformat");
        try {

            System.setProperty("drools.dateformat",
                               "dd-MMM-yyyy");

            String drl = "package org.test;\n"
                    + "rule \"rule1\"\n"
                    + "  dialect \"java\"\n"
                    + "  when\n"
                    + "    $a : Applicant()\n"
                    + "  then\n"
                    + "    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(\"dd-MMM-yyyy\");\n"
                    + "    java.util.Date newStartDate = new java.util.Date();\n"
                    + "    modify( $a ) {\n"
                    + "      setApplicantDate( newStartDate )"
                    + "    }\n"
                    + "end\n";

            addModelField("org.test.Applicant",
                          "this",
                          "org.test.Applicant",
                          DataType.TYPE_THIS);
            addModelField("org.test.Applicant",
                          "applicantDate",
                          Date.class.getName(),
                          DataType.TYPE_DATE);

            when(dmo.getPackageName()).thenReturn("org.test");

            RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                              Collections.emptyList(),
                                                                              dmo);

            assertNotNull(m);
            assertEquals("rule1",
                         m.name);

            assertEquals(1,
                         m.lhs.length);
            IPattern p = m.lhs[0];
            assertTrue(p instanceof FactPattern);

            FactPattern fp = (FactPattern) p;
            assertEquals("Applicant",
                         fp.getFactType());
            assertEquals("$a",
                         fp.getBoundName());

            assertNull(fp.getConstraintList());

            assertEquals(2,
                         m.rhs.length);

            assertTrue(m.rhs[0] instanceof FreeFormLine);
            FreeFormLine ffl = (FreeFormLine) m.rhs[0];
            assertEquals("java.util.Date newStartDate = new java.util.Date();",
                         ffl.getText());
            assertDateBoilerPlateCodeIsNotUnmarshaledToFreeFormLine(m.rhs);

            assertTrue(m.rhs[1] instanceof ActionUpdateField);
            ActionUpdateField auf = (ActionUpdateField) m.rhs[1];
            assertEquals("$a",
                         auf.getVariable());
            assertEquals(1,
                         auf.getFieldValues().length);
            ActionFieldValue afv = auf.getFieldValues()[0];
            assertEquals("applicantDate",
                         afv.getField());
            assertEquals("newStartDate",
                         afv.getValue());
            assertEquals(FieldNatureType.TYPE_FORMULA,
                         afv.getNature());

            assertEqualsIgnoreWhitespace(drl,
                                         RuleModelDRLPersistenceImpl.getInstance().marshal(m));
        } finally {
            if (oldValue == null) {
                System.clearProperty("drools.dateformat");
            } else {
                System.setProperty("drools.dateformat",
                                   oldValue);
            }
        }
    }

    @Test
    //https://issues.jboss.org/browse/GUVNOR-2143
    public void testNewKeywordVariableNamePrefix3() {
        String oldValue = System.getProperty("drools.dateformat");
        try {

            System.setProperty("drools.dateformat",
                               "dd-MMM-yyyy");

            String drl = "package org.test;\n"
                    + "rule \"rule1\"\n"
                    + "  dialect \"java\"\n"
                    + "  when\n"
                    + "    $a : Applicant()\n"
                    + "  then\n"
                    + "    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(\"dd-MMM-yyyy\");\n"
                    + "    java.util.Date newStartDate = new java.util.Date();\n"
                    + "    modify( $a ) \n"
                    + "    { setApplicantDate( newStartDate ) }\n"
                    + "end\n";

            addModelField("org.test.Applicant",
                          "this",
                          "org.test.Applicant",
                          DataType.TYPE_THIS);
            addModelField("org.test.Applicant",
                          "applicantDate",
                          Date.class.getName(),
                          DataType.TYPE_DATE);

            when(dmo.getPackageName()).thenReturn("org.test");

            RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                              Collections.emptyList(),
                                                                              dmo);

            assertNotNull(m);
            assertEquals("rule1",
                         m.name);

            assertEquals(1,
                         m.lhs.length);
            IPattern p = m.lhs[0];
            assertTrue(p instanceof FactPattern);

            FactPattern fp = (FactPattern) p;
            assertEquals("Applicant",
                         fp.getFactType());
            assertEquals("$a",
                         fp.getBoundName());

            assertNull(fp.getConstraintList());

            assertEquals(2,
                         m.rhs.length);

            assertTrue(m.rhs[0] instanceof FreeFormLine);
            FreeFormLine ffl = (FreeFormLine) m.rhs[0];
            assertEquals("java.util.Date newStartDate = new java.util.Date();",
                         ffl.getText());
            assertDateBoilerPlateCodeIsNotUnmarshaledToFreeFormLine(m.rhs);

            assertTrue(m.rhs[1] instanceof ActionUpdateField);
            ActionUpdateField auf = (ActionUpdateField) m.rhs[1];
            assertEquals("$a",
                         auf.getVariable());
            assertEquals(1,
                         auf.getFieldValues().length);
            ActionFieldValue afv = auf.getFieldValues()[0];
            assertEquals("applicantDate",
                         afv.getField());
            assertEquals("newStartDate",
                         afv.getValue());
            assertEquals(FieldNatureType.TYPE_FORMULA,
                         afv.getNature());

            assertEqualsIgnoreWhitespace(drl,
                                         RuleModelDRLPersistenceImpl.getInstance().marshal(m));
        } finally {
            if (oldValue == null) {
                System.clearProperty("drools.dateformat");
            } else {
                System.setProperty("drools.dateformat",
                                   oldValue);
            }
        }
    }

    @Test
    public void testLocalDateBoilerPlateCodeUnmarshaling() {
        String oldValue = System.getProperty("drools.dateformat");
        try {

            System.setProperty("drools.dateformat",
                               "dd-MMM-yyyy");

            String drl = "package org.test;\n"
                    + "rule \"rule1\"\n"
                    + "  dialect \"java\"\n"
                    + "  when\n"
                    + "    $a : Applicant()\n"
                    + "  then\n"
                    + "    java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern(\"dd-MMM-yyyy\");\n"
                    + "    modify( $a ) {\n"
                    + "      setApplicantDate( java.time.LocalDate.parse(\"31-Jan-2000\", dtf) )"
                    + "    }\n"
                    + "end\n";

            addModelField("org.test.Applicant",
                          "this",
                          "org.test.Applicant",
                          DataType.TYPE_THIS);
            addModelField("org.test.Applicant",
                          "applicantDate",
                          LocalDate.class.getName(),
                          DataType.TYPE_LOCAL_DATE);

            when(dmo.getPackageName()).thenReturn("org.test");

            RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                              Collections.emptyList(),
                                                                              dmo);

            assertNotNull(m);
            assertEquals("rule1",
                         m.name);

            assertEquals(1,
                         m.lhs.length);
            IPattern p = m.lhs[0];
            assertTrue(p instanceof FactPattern);

            FactPattern fp = (FactPattern) p;
            assertEquals("Applicant",
                         fp.getFactType());
            assertEquals("$a",
                         fp.getBoundName());

            assertNull(fp.getConstraintList());

            assertEquals(1,
                         m.rhs.length);

            assertDateBoilerPlateCodeIsNotUnmarshaledToFreeFormLine(m.rhs);

            assertTrue(m.rhs[0] instanceof ActionUpdateField);
            ActionUpdateField auf = (ActionUpdateField) m.rhs[0];
            assertEquals("$a",
                         auf.getVariable());
            assertEquals(1,
                         auf.getFieldValues().length);
            ActionFieldValue afv = auf.getFieldValues()[0];
            assertEquals("applicantDate",
                         afv.getField());
            assertEquals("31-Jan-2000",
                         afv.getValue());
            assertEquals(FieldNatureType.TYPE_LITERAL,
                         afv.getNature());

            assertEqualsIgnoreWhitespace(drl,
                                         RuleModelDRLPersistenceImpl.getInstance().marshal(m));
        } finally {
            if (oldValue == null) {
                System.clearProperty("drools.dateformat");
            } else {
                System.setProperty("drools.dateformat",
                                   oldValue);
            }
        }
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1158176
    public void testRHSEntryPointInsertion() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"AgentBooking Rule\"\n" +
                "dialect \"mvel\"\n" +
                "  when\n" +
                "    agent : Agent( )\n" +
                "    currentDate : TransactionDate( )\n" +
                "    Number( eval( intValue >=2 )) from accumulate (\n" +
                "      resEvent : ReservationEvent( agentId == agent.agentId , resdate == currentDate.eventDate ) from entry-point \"reservationEvent\", \n" +
                "      count(resEvent))\n" +
                "  then\n" +
                "    ReservationThresoldEvent thresoldevent = new ReservationThresoldEvent();\n" +
                "    thresoldevent.setAgentId( agent.getAgentId() );\n" +
                "    thresoldevent.setTxEventDate( currentDate.getEventDate() );\n" +
                "    kcontext.getKnowledgeRuntime().getEntryPoint(\"reservationTraceEvent\").insert( thresoldevent );\n" +
                "end";

        addModelField("org.test.Agent",
                      "this",
                      "org.test.Agent",
                      DataType.TYPE_THIS);
        addModelField("org.test.Agent",
                      "agentId",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);
        addModelField("org.test.TransactionDate",
                      "this",
                      "org.test.TransactionDate",
                      DataType.TYPE_THIS);
        addModelField("org.test.TransactionDate",
                      "eventDate",
                      Date.class.getName(),
                      DataType.TYPE_DATE);
        addModelField("java.lang.Number",
                      "intValue",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);
        addModelField("org.test.ReservationEvent",
                      "this",
                      "org.test.ReservationEvent",
                      DataType.TYPE_THIS);
        addModelField("org.test.ReservationEvent",
                      "agentId",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);
        addModelField("org.test.ReservationEvent",
                      "resdate",
                      Date.class.getName(),
                      DataType.TYPE_DATE);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(3,
                     m.lhs.length);
        assertEquals(3,
                     m.rhs.length);
        assertTrue(m.rhs[0] instanceof FreeFormLine);
        assertTrue(m.rhs[1] instanceof FreeFormLine);
        assertTrue(m.rhs[2] instanceof FreeFormLine);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1136100
    public void testLHSCustomAccumulateFunctions() throws Exception {
        String drl = "package org.test;\n"
                + "rule \"test\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "Double( ) from accumulate ( Applicant( $a : age != null ),\n"
                + "init( double total = 0; ),\n"
                + "action( total += $a; ),\n"
                + "reverse( total -= $a; ),\n"
                + "result( new Double( total ) ) )\n"
                + "then\n"
                + "end";

        addModelField(Double.class.getName(),
                      "this",
                      Double.class.getName(),
                      DataType.TYPE_THIS);
        addModelField("org.test.Applicant",
                      "this",
                      "org.test.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.test.Applicant",
                      "age",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        assertEquals(0,
                     m.rhs.length);
        assertTrue(m.lhs[0] instanceof FromAccumulateCompositeFactPattern);
        final FromAccumulateCompositeFactPattern facfp = (FromAccumulateCompositeFactPattern) m.lhs[0];
        assertEquals("double total = 0;",
                     facfp.getInitCode());
        assertEquals("total += $a;",
                     facfp.getActionCode());
        assertEquals("total -= $a;",
                     facfp.getReverseCode());
        assertEquals("new Double( total )",
                     facfp.getResultCode());

        assertNotNull(facfp.getFactPattern());
        final FactPattern fp = facfp.getFactPattern();
        assertEquals("Double",
                     fp.getFactType());
        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertNotNull(facfp.getSourcePattern());
        assertTrue(facfp.getSourcePattern() instanceof FactPattern);
        final FactPattern fsp = (FactPattern) facfp.getSourcePattern();
        assertEquals("Applicant",
                     fsp.getFactType());
        assertEquals(1,
                     fsp.getNumberOfConstraints());
        assertTrue(fsp.getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint sfc = (SingleFieldConstraint) fsp.getConstraint(0);
        assertEquals("$a",
                     sfc.getFieldBinding());
        assertEquals("Applicant",
                     sfc.getFactType());
        assertEquals("!= null",
                     sfc.getOperator());

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1174360
    public void testLHSMultipleAllOfTheFollowing() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"test\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "PhoneNumber(\n" +
                "( homePhone != null && homePhone matches \"\\\"+9199\\\"\" ) || \n" +
                "( personalPhone != null && personalPhone matches \"\\\"+9188\\\"\" ) || \n" +
                "( workPhone != null && workPhone matches \"\\\"+9177\\\"\") \n" +
                ")\n" +
                "then\n" +
                "end\n";

        addModelField("org.test.PhoneNumber",
                      "this",
                      "org.test.PhoneNumber",
                      DataType.TYPE_THIS);
        addModelField("org.test.PhoneNumber",
                      "homePhone",
                      String.class.getName(),
                      DataType.TYPE_STRING);
        addModelField("org.test.PhoneNumber",
                      "personalPhone",
                      String.class.getName(),
                      DataType.TYPE_STRING);
        addModelField("org.test.PhoneNumber",
                      "workPhone",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        assertEquals(0,
                     m.rhs.length);

        //Check Pattern
        assertTrue(m.lhs[0] instanceof FactPattern);
        final FactPattern fp = (FactPattern) m.lhs[0];
        assertEquals("PhoneNumber",
                     fp.getFactType());
        assertEquals(1,
                     fp.getNumberOfConstraints());

        assertTrue(fp.getConstraint(0) instanceof CompositeFieldConstraint);
        final CompositeFieldConstraint cfc = (CompositeFieldConstraint) fp.getConstraint(0);
        assertEquals("||",
                     cfc.getCompositeJunctionType());
        assertEquals(3,
                     cfc.getNumberOfConstraints());
        assertTrue(cfc.getConstraint(0) instanceof CompositeFieldConstraint);
        assertTrue(cfc.getConstraint(1) instanceof CompositeFieldConstraint);
        assertTrue(cfc.getConstraint(2) instanceof CompositeFieldConstraint);

        //Check first composite field constraint
        final CompositeFieldConstraint cfc_0 = (CompositeFieldConstraint) cfc.getConstraint(0);
        assertEquals("&&",
                     cfc_0.getCompositeJunctionType());
        assertEquals(2,
                     cfc_0.getNumberOfConstraints());
        assertTrue(cfc_0.getConstraint(0) instanceof SingleFieldConstraint);
        assertTrue(cfc_0.getConstraint(1) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc_0_0 = (SingleFieldConstraint) cfc_0.getConstraint(0);
        assertEquals("PhoneNumber",
                     sfc_0_0.getFactType());
        assertEquals("homePhone",
                     sfc_0_0.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc_0_0.getFieldType());
        assertEquals("!= null",
                     sfc_0_0.getOperator());
        assertNull(sfc_0_0.getValue());

        final SingleFieldConstraint sfc_0_1 = (SingleFieldConstraint) cfc_0.getConstraint(1);
        assertEquals("PhoneNumber",
                     sfc_0_1.getFactType());
        assertEquals("homePhone",
                     sfc_0_1.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc_0_1.getFieldType());
        assertEquals("matches",
                     sfc_0_1.getOperator());
        assertEquals("\\\"+9199\\\"",
                     sfc_0_1.getValue());

        //Check second composite field constraint
        final CompositeFieldConstraint cfc_1 = (CompositeFieldConstraint) cfc.getConstraint(1);
        assertEquals("&&",
                     cfc_1.getCompositeJunctionType());
        assertEquals(2,
                     cfc_1.getNumberOfConstraints());
        assertTrue(cfc_1.getConstraint(0) instanceof SingleFieldConstraint);
        assertTrue(cfc_1.getConstraint(1) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc_1_0 = (SingleFieldConstraint) cfc_1.getConstraint(0);
        assertEquals("PhoneNumber",
                     sfc_1_0.getFactType());
        assertEquals("personalPhone",
                     sfc_1_0.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc_1_0.getFieldType());
        assertEquals("!= null",
                     sfc_1_0.getOperator());
        assertNull(sfc_1_0.getValue());

        final SingleFieldConstraint sfc_1_1 = (SingleFieldConstraint) cfc_1.getConstraint(1);
        assertEquals("PhoneNumber",
                     sfc_1_1.getFactType());
        assertEquals("personalPhone",
                     sfc_1_1.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc_1_1.getFieldType());
        assertEquals("matches",
                     sfc_1_1.getOperator());
        assertEquals("\\\"+9188\\\"",
                     sfc_1_1.getValue());

        //Check third composite field constraint
        final CompositeFieldConstraint cfc_2 = (CompositeFieldConstraint) cfc.getConstraint(2);
        assertEquals("&&",
                     cfc_2.getCompositeJunctionType());
        assertEquals(2,
                     cfc_2.getNumberOfConstraints());
        assertTrue(cfc_2.getConstraint(0) instanceof SingleFieldConstraint);
        assertTrue(cfc_2.getConstraint(1) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc_2_0 = (SingleFieldConstraint) cfc_2.getConstraint(0);
        assertEquals("PhoneNumber",
                     sfc_2_0.getFactType());
        assertEquals("workPhone",
                     sfc_2_0.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc_2_0.getFieldType());
        assertEquals("!= null",
                     sfc_2_0.getOperator());
        assertNull(sfc_2_0.getValue());

        final SingleFieldConstraint sfc_2_1 = (SingleFieldConstraint) cfc_2.getConstraint(1);
        assertEquals("PhoneNumber",
                     sfc_2_1.getFactType());
        assertEquals("workPhone",
                     sfc_2_1.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc_2_1.getFieldType());
        assertEquals("matches",
                     sfc_2_1.getOperator());
        assertEquals("\\\"+9177\\\"",
                     sfc_2_1.getValue());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1175333
    public void testRHSFreeFormDRL() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"Validate Down Payment\"\n" +
                "dialect \"mvel\"\n" +
                "ruleflow-group \"validation\"\n" +
                "when\n" +
                "  property : Property( )\n" +
                "  Application( downPayment < 0 || downPayment > property.price )\n" +
                "then\n" +
                "  ValidationError fact0 = new ValidationError();\n" +
                "  fact0.setCause( \"Down payment can't be negative or larger than property value\" );\n" +
                "  insert( fact0 );\n" +
                "  System.out.println(\"Executed Rule: \" + drools.getRule().getName() );\n" +
                "end\n";

        addModelField("org.test.Property",
                      "this",
                      "org.test.Property",
                      DataType.TYPE_THIS);
        addModelField("org.test.Property",
                      "price",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);
        addModelField("org.test.Application",
                      "this",
                      "org.test.Application",
                      DataType.TYPE_THIS);
        addModelField("org.test.Application",
                      "downPayment",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);
        addModelField("org.test.ValidationError",
                      "this",
                      "org.test.ValidationError",
                      DataType.TYPE_THIS);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1189930
    //Formatted as generated from a Guided Rule in the Workbench
    public void testBigDecimal1() throws Exception {
        String drl = "package org.test;\n" +
                "import java.lang.Number;\n" +
                "import java.math.BigDecimal;\n" +
                "import java.util.Calendar;\n" +
                "rule \"BigDecimalRule\"\n" +
                "  dialect \"java\"\n" +
                "when\n" +
                "  $bd : BigDecimal( )\n" +
                "then\n" +
                "  LastRunInformation lastRun = new LastRunInformation();\n" +
                "  lastRun.setLastNumber( $bd );\n" +
                "  insert( lastRun );\n" +
                "end";

        addModelField("org.test.LastRunInformation",
                      "this",
                      "org.test.LastRunInformation",
                      DataType.TYPE_THIS);
        addModelField("org.test.LastRunInformation",
                      "lastNumber",
                      BigDecimal.class.getName(),
                      DataType.TYPE_NUMERIC_BIGDECIMAL);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);
        final FactPattern fp = (FactPattern) p;
        assertEquals("$bd",
                     fp.getBoundName());
        assertEquals("BigDecimal",
                     fp.getFactType());

        assertEquals(1,
                     m.rhs.length);
        final IAction a = m.rhs[0];
        assertTrue(a instanceof ActionInsertFact);
        final ActionInsertFact aif = (ActionInsertFact) a;
        assertEquals("lastRun",
                     aif.getBoundName());
        assertEquals("LastRunInformation",
                     aif.getFactType());

        assertEquals(1,
                     aif.getFieldValues().length);
        final ActionFieldValue afv0 = aif.getFieldValues()[0];
        assertEquals("lastNumber",
                     afv0.getField());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     afv0.getNature());
        assertEquals("=$bd",
                     afv0.getValue());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1189930
    public void testBigDecimal2() throws Exception {
        String drl = "package org.test;\n" +
                "import java.lang.Number;\n" +
                "import java.math.BigDecimal;\n" +
                "import java.util.Calendar;\n" +
                "rule \"BigDecimalRule\"\n" +
                "  dialect \"java\"\n" +
                "when\n" +
                "  $bd : BigDecimal( )\n" +
                "then\n" +
                "  LastRunInformation lastRun = new LastRunInformation();\n" +
                "  lastRun.setLastNumber($bd);\n" +
                "  insert(lastRun);\n" +
                "end";

        addModelField("org.test.LastRunInformation",
                      "this",
                      "org.test.LastRunInformation",
                      DataType.TYPE_THIS);
        addModelField("org.test.LastRunInformation",
                      "lastNumber",
                      BigDecimal.class.getName(),
                      DataType.TYPE_NUMERIC_BIGDECIMAL);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);
        final FactPattern fp = (FactPattern) p;
        assertEquals("$bd",
                     fp.getBoundName());
        assertEquals("BigDecimal",
                     fp.getFactType());

        assertEquals(1,
                     m.rhs.length);
        final IAction a = m.rhs[0];
        assertTrue(a instanceof ActionInsertFact);
        final ActionInsertFact aif = (ActionInsertFact) a;
        assertEquals("lastRun",
                     aif.getBoundName());
        assertEquals("LastRunInformation",
                     aif.getFactType());

        assertEquals(1,
                     aif.getFieldValues().length);
        final ActionFieldValue afv0 = aif.getFieldValues()[0];
        assertEquals("lastNumber",
                     afv0.getField());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     afv0.getNature());
        assertEquals("=$bd",
                     afv0.getValue());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1189930
    public void testBigDecimal3() throws Exception {
        String drl = "package org.test;\n" +
                "import java.lang.Number;\n" +
                "import java.math.BigDecimal;\n" +
                "import java.util.Calendar;\n" +
                "rule \"BigDecimalRule\"\n" +
                "  dialect \"java\"\n" +
                "when\n" +
                "  $bd : BigDecimal( )\n" +
                "then\n" +
                "  LastRunInformation lastRun = new LastRunInformation();\n" +
                "  lastRun.setLastNumber($bd);\n" +
                "\n" +
                "  insert(lastRun);\n" +
                "end";

        addModelField("org.test.LastRunInformation",
                      "this",
                      "org.test.LastRunInformation",
                      DataType.TYPE_THIS);
        addModelField("org.test.LastRunInformation",
                      "lastNumber",
                      BigDecimal.class.getName(),
                      DataType.TYPE_NUMERIC_BIGDECIMAL);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);
        final FactPattern fp = (FactPattern) p;
        assertEquals("$bd",
                     fp.getBoundName());
        assertEquals("BigDecimal",
                     fp.getFactType());

        assertEquals(1,
                     m.rhs.length);
        final IAction a = m.rhs[0];
        assertTrue(a instanceof ActionInsertFact);
        final ActionInsertFact aif = (ActionInsertFact) a;
        assertEquals("lastRun",
                     aif.getBoundName());
        assertEquals("LastRunInformation",
                     aif.getFactType());

        assertEquals(1,
                     aif.getFieldValues().length);
        final ActionFieldValue afv0 = aif.getFieldValues()[0];
        assertEquals("lastNumber",
                     afv0.getField());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     afv0.getNature());
        assertEquals("=$bd",
                     afv0.getValue());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1191737
    public void testMultipleFromKeywords() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"ToyWithoutName \"\n" +
                "dialect \"java\"\n" +
                "when\n" +
                "  $father: Father()\n" +
                "  ($kid: Kid() from $father.kids)\n" +
                "  ($toy: Toy(name == null) from $kid.toys)\n" +
                "then\n" +
                "  System.out.println(\"blabla\");\n" +
                "end";

        addModelField("org.test.Father",
                      "this",
                      "org.test.Father",
                      DataType.TYPE_THIS);
        addModelField("org.test.Father",
                      "kids",
                      "org.test.Kid",
                      DataType.TYPE_COLLECTION);
        addModelField("org.test.Kid",
                      "this",
                      "org.test.Kid",
                      DataType.TYPE_THIS);
        addModelField("org.test.Kid",
                      "toys",
                      "org.test.Toy",
                      DataType.TYPE_COLLECTION);
        addModelField("org.test.Toy",
                      "this",
                      "org.test.Toy",
                      DataType.TYPE_THIS);
        addModelField("org.test.Toy",
                      "name",
                      "java.lang.String",
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(3,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("$father",
                     fp0.getBoundName());
        assertEquals("Father",
                     fp0.getFactType());

        final IPattern p1 = m.lhs[1];
        assertTrue(p1 instanceof FromCompositeFactPattern);
        final FromCompositeFactPattern fp1 = (FromCompositeFactPattern) p1;
        assertEquals("$kid",
                     fp1.getFactPattern().getBoundName());
        assertEquals("Kid",
                     fp1.getFactType());

        final IPattern p2 = m.lhs[2];
        assertTrue(p2 instanceof FromCompositeFactPattern);
        final FromCompositeFactPattern fp2 = (FromCompositeFactPattern) p2;
        assertEquals("$toy",
                     fp2.getFactPattern().getBoundName());
        assertEquals("Toy",
                     fp2.getFactType());

        assertEquals(1,
                     m.rhs.length);
        final IAction a = m.rhs[0];
        assertTrue(a instanceof FreeFormLine);
        final FreeFormLine affl = (FreeFormLine) a;
        assertEquals("System.out.println(\"blabla\");",
                     affl.getText());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/DROOLS-713
    public void testLHSFormula() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"MyRule\"\n" +
                "dialect \"java\"\n" +
                "agenda-group \"MyGroup\"\n" +
                "salience 900\n" +
                "when\n" +
                "  $bundle : MyClass( $protocolSequence : protocolSequence )\n" +
                "  eval( $protocolSequence != null )\n" +
                "  $followupBundle : MyClass( protocolSequence == ( $protocolSequence + 1 ) )\n" +
                "then\n" +
                "end";

        addModelField("org.test.MyClass",
                      "this",
                      "org.test.MyClass",
                      DataType.TYPE_THIS);
        addModelField("org.test.MyClass",
                      "protocolSequence",
                      Integer.class.getName(),
                      DataType.TYPE_NUMERIC_INTEGER);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(3,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("$bundle",
                     fp0.getBoundName());
        assertEquals("MyClass",
                     fp0.getFactType());

        final IPattern p1 = m.lhs[1];
        assertTrue(p1 instanceof FreeFormLine);
        final FreeFormLine ffl1 = (FreeFormLine) p1;
        assertEquals("eval( $protocolSequence != null )",
                     ffl1.getText());

        final IPattern p2 = m.lhs[2];
        assertTrue(p2 instanceof FactPattern);
        final FactPattern fp2 = (FactPattern) p2;
        assertEquals("$followupBundle",
                     fp2.getBoundName());
        assertEquals("MyClass",
                     fp2.getFactType());

        assertEquals(1,
                     fp2.getNumberOfConstraints());
        assertTrue(fp2.getConstraint(0) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) fp2.getConstraint(0);
        assertEquals("MyClass",
                     sfc1.getFactType());
        assertEquals("protocolSequence",
                     sfc1.getFieldName());
        assertEquals(DataType.TYPE_NUMERIC_INTEGER,
                     sfc1.getFieldType());
        assertEquals("==",
                     sfc1.getOperator());
        assertEquals("$protocolSequence + 1",
                     sfc1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_RET_VALUE,
                     sfc1.getConstraintValueType());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1127303
    public void testRHSChainedMethodCalls1() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"MyRule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  Person( $n : name )\n" +
                "then\n" +
                "  $n.toUpperCase().indexOf(\"S\", 1);\n" +
                "end";

        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("Person",
                     fp0.getFactType());

        assertEquals(1,
                     fp0.getNumberOfConstraints());
        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("Person",
                     sfc1.getFactType());
        assertEquals("name",
                     sfc1.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc1.getFieldType());

        assertEquals(1,
                     m.rhs.length);
        final IAction a0 = m.rhs[0];
        assertTrue(a0 instanceof FreeFormLine);
        final FreeFormLine ffl1 = (FreeFormLine) a0;
        assertEquals("$n.toUpperCase().indexOf(\"S\", 1);",
                     ffl1.getText());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1127303
    public void testRHSChainedMethodCalls2() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"MyRule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  Person( $n : name )\n" +
                "then\n" +
                "  $n.toUpperCase().indexOf(\".\", 1);\n" +
                "end";

        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("Person",
                     fp0.getFactType());

        assertEquals(1,
                     fp0.getNumberOfConstraints());
        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("Person",
                     sfc1.getFactType());
        assertEquals("name",
                     sfc1.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc1.getFieldType());

        assertEquals(1,
                     m.rhs.length);
        final IAction a0 = m.rhs[0];
        assertTrue(a0 instanceof FreeFormLine);
        final FreeFormLine ffl1 = (FreeFormLine) a0;
        assertEquals("$n.toUpperCase().indexOf(\".\", 1);",
                     ffl1.getText());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1127303
    public void testRHSChainedMethodCalls3() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"MyRule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  Person( $n : name )\n" +
                "then\n" +
                "  $n.toUpperCase().indexOf(\"(\", 1);\n" +
                "end";

        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("Person",
                     fp0.getFactType());

        assertEquals(1,
                     fp0.getNumberOfConstraints());
        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("Person",
                     sfc1.getFactType());
        assertEquals("name",
                     sfc1.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc1.getFieldType());

        assertEquals(1,
                     m.rhs.length);
        final IAction a0 = m.rhs[0];
        assertTrue(a0 instanceof FreeFormLine);
        final FreeFormLine ffl1 = (FreeFormLine) a0;
        assertEquals("$n.toUpperCase().indexOf(\"(\", 1);",
                     ffl1.getText());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1127303
    public void testRHSChainedMethodCalls4() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"MyRule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  Person( $n : name )\n" +
                "then\n" +
                "  $n.toUpperCase().indexOf(\"\\\").\", 1);\n" +
                "end";

        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("Person",
                     fp0.getFactType());

        assertEquals(1,
                     fp0.getNumberOfConstraints());
        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("Person",
                     sfc1.getFactType());
        assertEquals("name",
                     sfc1.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc1.getFieldType());

        assertEquals(1,
                     m.rhs.length);
        final IAction a0 = m.rhs[0];
        assertTrue(a0 instanceof FreeFormLine);
        final FreeFormLine ffl1 = (FreeFormLine) a0;
        assertEquals("$n.toUpperCase().indexOf(\"\\\").\", 1);",
                     ffl1.getText());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/DROOLS-715
    public void testLHSValidLiteralFieldName() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"MyRule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  MyClass( valid == true )\n" +
                "then\n" +
                "end";

        addModelField("org.test.MyClass",
                      "this",
                      "org.test.MyClass",
                      DataType.TYPE_THIS);
        addModelField("org.test.MyClass",
                      "valid",
                      Boolean.class.getName(),
                      DataType.TYPE_BOOLEAN);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("MyClass",
                     fp0.getFactType());

        assertEquals(1,
                     fp0.getNumberOfConstraints());
        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("MyClass",
                     sfc1.getFactType());
        assertEquals("valid",
                     sfc1.getFieldName());
        assertEquals(DataType.TYPE_BOOLEAN,
                     sfc1.getFieldType());
        assertEquals("==",
                     sfc1.getOperator());
        assertEquals("true",
                     sfc1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_ENUM,
                     sfc1.getConstraintValueType());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1218308
    public void testInvalidFromSyntax() throws Exception {
        String drl = "rule \"test\"\n" +
                "    dialect \"mvel\"\n" +
                "    when\n" +
                "    (obj : MyClass( ) from my.package)\n" +
                "    then\n" +
                "    System.out.println(\"Test\")\n" +
                "    end";

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FromCompositeFactPattern);
        final FromCompositeFactPattern fp0 = (FromCompositeFactPattern) p0;
        assertEquals("MyClass",
                     fp0.getFactType());

        final FactPattern fp1 = fp0.getFactPattern();
        assertEquals("MyClass",
                     fp1.getFactType());
        assertEquals(0,
                     fp1.getNumberOfConstraints());

        final ExpressionFormLine efl = fp0.getExpression();
        assertNotNull(efl);
        assertEquals(2,
                     efl.getParts().size());
        assertTrue(efl.getParts().get(0) instanceof ExpressionVariable);
        final ExpressionVariable ev = (ExpressionVariable) efl.getParts().get(0);
        assertEquals("my",
                     ev.getName());
        assertTrue(efl.getParts().get(1) instanceof ExpressionText);
        final ExpressionText et = (ExpressionText) efl.getParts().get(1);
        assertEquals("package",
                     et.getName());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1218308
    public void testValidSyntaxNonImportedType() throws Exception {
        String drl = "rule \"x\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  var : NotImported( )\n" +
                "  OtherType( field != var.field )\n" +
                "  (MyType( ) from var.collectionField)\n" +
                "then\n" +
                "end";

        addModelField("org.test.OtherType",
                      "this",
                      "org.test.OtherType",
                      DataType.TYPE_THIS);
        addModelField("org.test.OtherType",
                      "field",
                      String.class.getName(),
                      DataType.TYPE_STRING);
        addModelField("org.test.MyType",
                      "this",
                      "org.test.MyType",
                      DataType.TYPE_THIS);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(3,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("NotImported",
                     fp0.getFactType());
        assertEquals("var",
                     fp0.getBoundName());
        assertEquals(0,
                     fp0.getNumberOfConstraints());

        final IPattern p1 = m.lhs[1];
        assertTrue(p1 instanceof FactPattern);
        final FactPattern fp1 = (FactPattern) p1;
        assertEquals("OtherType",
                     fp1.getFactType());
        assertEquals(1,
                     fp1.getNumberOfConstraints());
        assertTrue(fp1.getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint fp1sfc0 = (SingleFieldConstraint) fp1.getConstraint(0);
        assertEquals("OtherType",
                     fp1sfc0.getFactType());
        assertEquals("field",
                     fp1sfc0.getFieldName());
        assertEquals("!=",
                     fp1sfc0.getOperator());
        assertEquals(2,
                     fp1sfc0.getExpressionValue().getParts().size());
        assertTrue(fp1sfc0.getExpressionValue().getParts().get(0) instanceof ExpressionText);
        final ExpressionText fp1sfc0e0 = (ExpressionText) fp1sfc0.getExpressionValue().getParts().get(0);
        assertEquals("var",
                     fp1sfc0e0.getName());
        assertTrue(fp1sfc0.getExpressionValue().getParts().get(1) instanceof ExpressionText);
        final ExpressionText fp1sfc0e1 = (ExpressionText) fp1sfc0.getExpressionValue().getParts().get(1);
        assertEquals("field",
                     fp1sfc0e1.getName());

        final IPattern p2 = m.lhs[2];
        assertTrue(p2 instanceof FromCompositeFactPattern);
        final FromCompositeFactPattern fp2 = (FromCompositeFactPattern) p2;
        assertEquals("MyType",
                     fp2.getFactType());
        assertEquals(2,
                     fp2.getExpression().getParts().size());
        assertTrue(fp2.getExpression().getParts().get(0) instanceof ExpressionVariable);
        final ExpressionVariable fp2e0 = (ExpressionVariable) fp2.getExpression().getParts().get(0);
        assertEquals("var",
                     fp2e0.getName());
        assertTrue(fp2.getExpression().getParts().get(1) instanceof ExpressionText);
        final ExpressionText fp2e1 = (ExpressionText) fp2.getExpression().getParts().get(1);
        assertEquals("collectionField",
                     fp2e1.getName());

        assertEquals(0,
                     m.rhs.length);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1218308
    public void testInvalidSyntax1() throws Exception {
        String drl = "rule \"test\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  Smurf( flange \n" +
                "then\n" +
                "end";

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FreeFormLine);
        final FreeFormLine ffl = (FreeFormLine) p0;
        assertEquals("Smurf( flange",
                     ffl.getText());

        assertEquals(0,
                     m.rhs.length);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1234640
    public void testStringFieldsWithDoubleForwardSlashes() throws Exception {
        String drl = "rule \"test\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  MyType( url == \"http://www.redhat.com\" )\n" +
                "then\n" +
                "end";

        addModelField("org.test.MyType",
                      "this",
                      "org.test.MyType",
                      DataType.TYPE_THIS);
        addModelField("org.test.MyType",
                      "url",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("MyType",
                     fp0.getFactType());
        assertEquals(1,
                     fp0.getNumberOfConstraints());
        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint fp0sfc0 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("MyType",
                     fp0sfc0.getFactType());
        assertEquals("url",
                     fp0sfc0.getFieldName());
        assertEquals("==",
                     fp0sfc0.getOperator());
        assertEquals(DataType.TYPE_STRING,
                     fp0sfc0.getFieldType());
        assertEquals("http://www.redhat.com",
                     fp0sfc0.getValue());

        assertEquals(0,
                     m.rhs.length);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    private void assertEqualsIgnoreWhitespace(final String expected,
                                              final String actual) {
        Assertions.assertThat(expected).isEqualToIgnoringWhitespace(actual);
    }

    @Test
    public void testExpressionWhenMethodNameIsExsistingOperator() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( name.contains(\"test\") ) \n"
                + "then\n"
                + "end\n";

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertTrue(m.lhs[0] instanceof FactPattern);
        FactPattern pattern = (FactPattern) m.lhs[0];
        assertEquals("Applicant",
                     pattern.getFactType());
        assertEquals(1,
                     pattern.getNumberOfConstraints());
        assertEquals("contains(\"test\")",
                     ((SingleFieldConstraint) pattern.getConstraint(0)).getFieldName());
        assertNull(((SingleFieldConstraint) pattern.getConstraint(0)).getOperator());
        assertNull(((SingleFieldConstraint) pattern.getConstraint(0)).getValue());
        SingleFieldConstraintEBLeftSide constraint = (SingleFieldConstraintEBLeftSide) pattern.getConstraint(0);
        List<ExpressionPart> expressionParts = constraint.getExpressionLeftSide().getParts();
        assertEquals(3, expressionParts.size());
        assertTrue(expressionParts.get(0) instanceof ExpressionUnboundFact);
        assertEquals("Applicant", ((ExpressionUnboundFact) expressionParts.get(0)).getFactType());
        assertTrue(expressionParts.get(1) instanceof ExpressionText);
        assertEquals("name", expressionParts.get(1).getName());
        assertTrue(expressionParts.get(2) instanceof ExpressionText);
        assertEquals("contains(\"test\")", expressionParts.get(2).getName());
    }

    @Test
    public void testDoNotAddEqualsToExpressionValues() {
        String drl = "rule \"TestRule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "t1 : Transaction( $transactiontime1 : transactiontime)\n" +
                "Transaction( newtransactiontime.isBefore($transactiontime1))\n" +
                "then\n" +
                "end\n";

        addModelField("Transaction",
                      "this",
                      "Transaction1",
                      DataType.TYPE_THIS);
        addModelField("Transaction",
                      "newtransactiontime",
                      "Transaction",
                      DataType.TYPE_OBJECT);
        addMethodInformation("Transaction",
                             "isBefore",
                             new ArrayList<String>() {{
                                 add("Comparable");
                             }},
                             "boolean",
                             null,
                             DataType.TYPE_BOOLEAN);

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertTrue(m.lhs[1] instanceof FactPattern);
        FactPattern pattern = (FactPattern) m.lhs[1];
        assertEquals("Transaction",
                     pattern.getFactType());
        assertTrue(pattern.getConstraint(0) instanceof SingleFieldConstraintEBLeftSide);
        SingleFieldConstraintEBLeftSide fieldConstraint = (SingleFieldConstraintEBLeftSide) pattern.getConstraint(0);
        assertTrue(fieldConstraint.getExpressionLeftSide().getParts().get(2) instanceof ExpressionMethod);
        ExpressionMethod expressionMethod = (ExpressionMethod) fieldConstraint.getExpressionLeftSide().getParts().get(2);
        ExpressionFormLine expressionFormLine = expressionMethod.getParams().values().iterator().next();
        assertEquals("$transactiontime1",
                     expressionFormLine.getParts().get(0).getName());
    }

    @Test
    public void testStringReplaceExpression() throws Exception {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1264321
        String drl = "rule \"Replace_condition_Issue\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  MyType( myString.replace(\"a\",\"b\"))\n" +
                "then\n" +
                "end";

        addModelField("org.test.MyType",
                      "this",
                      "org.test.MyType",
                      DataType.TYPE_THIS);
        addModelField("org.test.MyType",
                      "myString",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        addMethodInformation("java.lang.String",
                             "replace",
                             new ArrayList<String>() {{
                                 add("String");
                                 add("String");
                             }},
                             "java.lang.String",
                             null,
                             DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("MyType",
                     fp0.getFactType());
        assertEquals(1,
                     fp0.getNumberOfConstraints());
        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraintEBLeftSide);
        final SingleFieldConstraintEBLeftSide fp0sfc0 = (SingleFieldConstraintEBLeftSide) fp0.getConstraint(0);

        assertEquals(3,
                     fp0sfc0.getExpressionLeftSide().getParts().size());

        assertTrue(fp0sfc0.getExpressionLeftSide().getParts().get(0) instanceof ExpressionUnboundFact);
        final ExpressionUnboundFact ep0 = (ExpressionUnboundFact) fp0sfc0.getExpressionLeftSide().getParts().get(0);
        assertEquals("MyType",
                     ep0.getFactType());

        assertTrue(fp0sfc0.getExpressionLeftSide().getParts().get(1) instanceof ExpressionField);
        final ExpressionField ep1 = (ExpressionField) fp0sfc0.getExpressionLeftSide().getParts().get(1);
        assertEquals("myString",
                     ep1.getName());

        assertTrue(fp0sfc0.getExpressionLeftSide().getParts().get(2) instanceof ExpressionMethod);
        final ExpressionMethod ep2 = (ExpressionMethod) fp0sfc0.getExpressionLeftSide().getParts().get(2);
        assertEquals("replace",
                     ep2.getName());
        assertEquals(2,
                     ep2.getParams().size());

        final ExpressionFormLine param0 = ep2.getParams().get(new ExpressionMethodParameterDefinition(0,
                                                                                                      "String"));
        assertNotNull(param0);
        assertEquals(1,
                     param0.getParts().size());
        assertNotNull(param0.getParts().get(0));
        assertEquals("a",
                     param0.getParts().get(0).getName());
        assertEquals("String",
                     param0.getParts().get(0).getClassType());

        final ExpressionFormLine param1 = ep2.getParams().get(new ExpressionMethodParameterDefinition(1,
                                                                                                      "String"));
        assertNotNull(param1);
        assertEquals(1,
                     param1.getParts().size());
        assertNotNull(param1.getParts().get(0));
        assertEquals("b",
                     param1.getParts().get(0).getName());
        assertEquals("String",
                     param1.getParts().get(0).getClassType());

        assertEquals(0,
                     m.rhs.length);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testBoundListConstraint() throws Exception {
        //https://bugzilla.redhat.com/show_bug.cgi?id=1264339
        String drl = "package org.test;\n" +
                "rule \"List_Of_Values_Issue\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  MyTransactionVO( $myData : myData )\n" +
                "  MyDataList( myDataList contains $myData )\n" +
                "     then\n" +
                "end";

        addModelField("org.test.MyTransactionVO",
                      "this",
                      "org.test.MyTransactionVO",
                      DataType.TYPE_THIS);
        addModelField("org.test.MyTransactionVO",
                      "myData",
                      "org.test.MyDataList",
                      "org.test.MyDataList");

        addModelField("org.test.MyDataList",
                      "this",
                      "org.test.MyDataList",
                      DataType.TYPE_THIS);
        addModelField("org.test.MyDataList",
                      "myDataList",
                      List.class.getName(),
                      DataType.TYPE_COLLECTION);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(2,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("MyTransactionVO",
                     fp0.getFactType());
        assertEquals(1,
                     fp0.getNumberOfConstraints());

        final IPattern p1 = m.lhs[1];
        assertTrue(p1 instanceof FactPattern);
        final FactPattern fp1 = (FactPattern) p1;
        assertEquals("MyDataList",
                     fp1.getFactType());
        assertEquals(1,
                     fp1.getNumberOfConstraints());

        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint fp0sfc0 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("MyTransactionVO",
                     fp0sfc0.getFactType());
        assertEquals("myData",
                     fp0sfc0.getFieldName());
        assertEquals("$myData",
                     fp0sfc0.getFieldBinding());
        assertEquals("MyDataList",
                     fp0sfc0.getFieldType());
        assertNull(fp0sfc0.getOperator());
        assertNull(fp0sfc0.getValue());

        assertTrue(fp1.getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint fp1sfc0 = (SingleFieldConstraint) fp1.getConstraint(0);
        assertEquals("MyDataList",
                     fp1sfc0.getFactType());
        assertEquals("myDataList",
                     fp1sfc0.getFieldName());
        assertEquals("contains",
                     fp1sfc0.getOperator());
        assertEquals("$myData",
                     fp1sfc0.getValue());
        assertEquals(SingleFieldConstraint.TYPE_VARIABLE,
                     fp1sfc0.getConstraintValueType());

        assertEquals(0,
                     m.rhs.length);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testRHSAppendToList() throws Exception {
        //https://issues.jboss.org/browse/GUVNOR-2286
        String drl = "package org.test;\n" +
                "import java.lang.Number;\n" +
                "rule \"RuleCheckEmail\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "incomeData : IncomeData( email == \"myemail\" , $list : list != null )\n" +
                "then\n" +
                "Element element = new Element();\n" +
                "element.setId( 2 );\n" +
                "insertLogical( element );\n" +
                "$list.add( element );\n" +
                "end";

        addModelField("org.test.IncomeData",
                      "this",
                      "org.test.IncomeData",
                      DataType.TYPE_THIS);
        addModelField("org.test.IncomeData",
                      "email",
                      String.class.getName(),
                      DataType.TYPE_STRING);
        addModelField("org.test.IncomeData",
                      "list",
                      List.class.getName(),
                      DataType.TYPE_COLLECTION);

        addModelField("java.util.List",
                      "this",
                      "java.util.List",
                      DataType.TYPE_THIS);
        addMethodInformation("java.util.List",
                             "add",
                             new ArrayList<String>() {{
                                 add("java.lang.Object");
                             }},
                             DataType.TYPE_BOOLEAN,
                             null,
                             DataType.TYPE_BOOLEAN);
        addMethodInformation("java.util.List",
                             "add",
                             new ArrayList<String>() {{
                                 add("java.lang.Integer");
                                 add("java.lang.Object");
                             }},
                             DataType.TYPE_BOOLEAN,
                             null,
                             DataType.TYPE_BOOLEAN);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("IncomeData",
                     fp0.getFactType());
        assertEquals(2,
                     fp0.getNumberOfConstraints());

        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);
        final SingleFieldConstraint fp0sfc0 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("IncomeData",
                     fp0sfc0.getFactType());
        assertEquals("email",
                     fp0sfc0.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     fp0sfc0.getFieldType());
        assertEquals("==",
                     fp0sfc0.getOperator());
        assertEquals("myemail",
                     fp0sfc0.getValue());

        assertTrue(fp0.getConstraint(1) instanceof SingleFieldConstraint);
        final SingleFieldConstraint fp0sfc1 = (SingleFieldConstraint) fp0.getConstraint(1);
        assertEquals("IncomeData",
                     fp0sfc1.getFactType());
        assertEquals("list",
                     fp0sfc1.getFieldName());
        assertEquals(DataType.TYPE_COLLECTION,
                     fp0sfc1.getFieldType());
        assertEquals("!= null",
                     fp0sfc1.getOperator());
        assertNull(fp0sfc1.getValue());

        assertEquals(2,
                     m.rhs.length);

        assertTrue(m.rhs[0] instanceof ActionInsertLogicalFact);
        final ActionInsertLogicalFact a0 = (ActionInsertLogicalFact) m.rhs[0];
        assertEquals("Element",
                     a0.getFactType());
        assertEquals("element",
                     a0.getBoundName());
        assertEquals(1,
                     a0.getFieldValues().length);
        final ActionFieldValue a0f0 = a0.getFieldValues()[0];
        assertEquals("id",
                     a0f0.getField());
        assertEquals("2",
                     a0f0.getValue());
        assertEquals(DataType.TYPE_NUMERIC,
                     a0f0.getType());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     a0f0.getNature());

        assertTrue(m.rhs[1] instanceof ActionCallMethod);
        final ActionCallMethod a1 = (ActionCallMethod) m.rhs[1];
        assertEquals("add",
                     a1.getMethodName());
        assertEquals("$list",
                     a1.getVariable());
        assertEquals(1,
                     a1.getFieldValues().length);
        final ActionFieldValue a1f0 = a1.getFieldValues()[0];
        assertEquals("add",
                     a1f0.getField());
        assertEquals("element",
                     a1f0.getValue());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     a1f0.getNature());
        assertEquals("java.lang.Object",
                     a1f0.getType());

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/GUVNOR-2030
    public void testLHSTemplateKeys() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"MyRule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  Person( name == \"@{k1}\" )\n" +
                "then\n" +
                "end";

        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("Person",
                     fp0.getFactType());

        assertEquals(1,
                     fp0.getNumberOfConstraints());
        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("Person",
                     sfc1.getFactType());
        assertEquals("name",
                     sfc1.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc1.getFieldType());
        assertEquals(SingleFieldConstraint.TYPE_TEMPLATE,
                     sfc1.getConstraintValueType());
        assertEquals("k1",
                     sfc1.getValue());

        assertEquals(0,
                     m.rhs.length);
    }

    @Test
    //https://issues.jboss.org/browse/GUVNOR-2030
    public void testRHSTemplateKeys() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"MyRule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  $p : Person( name == \"Fred\" )\n" +
                "then\n" +
                "  modify( $p ) { setName( \"@{k1}\" ) }\n" +
                "end";

        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("Person",
                     fp0.getFactType());

        assertEquals(1,
                     fp0.getNumberOfConstraints());
        assertTrue(fp0.getConstraint(0) instanceof SingleFieldConstraint);

        final SingleFieldConstraint sfc1 = (SingleFieldConstraint) fp0.getConstraint(0);
        assertEquals("Person",
                     sfc1.getFactType());
        assertEquals("name",
                     sfc1.getFieldName());
        assertEquals(DataType.TYPE_STRING,
                     sfc1.getFieldType());
        assertEquals(SingleFieldConstraint.TYPE_LITERAL,
                     sfc1.getConstraintValueType());
        assertEquals("Fred",
                     sfc1.getValue());

        assertEquals(1,
                     m.rhs.length);

        assertTrue(m.rhs[0] instanceof ActionUpdateField);
        ActionUpdateField auf = (ActionUpdateField) m.rhs[0];
        assertEquals("$p",
                     auf.getVariable());
        assertEquals(1,
                     auf.getFieldValues().length);
        ActionFieldValue afv = auf.getFieldValues()[0];
        assertEquals("name",
                     afv.getField());
        assertEquals("k1",
                     afv.getValue());
        assertEquals(FieldNatureType.TYPE_TEMPLATE,
                     afv.getNature());
    }

    @Test
    //See https://issues.jboss.org/browse/GUVNOR-2455
    public void testRHS_DataTypeSuffixes() throws Exception {
        String drl = "package org.test;\n" +
                "rule \"MyRule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "  $p : Person( )\n" +
                "then\n" +
                "  modify( $p ) { setDouble( 25.0d ), setFloat( 25.0f ), setLong( 25L ) }\n" +
                "end";

        addModelField("org.test.Person",
                      "this",
                      "org.test.Person",
                      DataType.TYPE_THIS);
        addModelField("org.test.Person",
                      "double",
                      Double.class.getName(),
                      DataType.TYPE_NUMERIC_DOUBLE);
        addModelField("org.test.Person",
                      "float",
                      Float.class.getName(),
                      DataType.TYPE_NUMERIC_FLOAT);
        addModelField("org.test.Person",
                      "long",
                      Long.class.getName(),
                      DataType.TYPE_NUMERIC_LONG);

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        assertNotNull(m);

        assertEquals(1,
                     m.lhs.length);
        final IPattern p0 = m.lhs[0];
        assertTrue(p0 instanceof FactPattern);
        final FactPattern fp0 = (FactPattern) p0;
        assertEquals("Person",
                     fp0.getFactType());

        assertEquals(0,
                     fp0.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);

        assertTrue(m.rhs[0] instanceof ActionUpdateField);
        ActionUpdateField auf = (ActionUpdateField) m.rhs[0];
        assertEquals("$p",
                     auf.getVariable());
        assertEquals(3,
                     auf.getFieldValues().length);
        ActionFieldValue afv0 = auf.getFieldValues()[0];
        assertEquals("double",
                     afv0.getField());
        assertEquals("25.0",
                     afv0.getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     afv0.getNature());

        ActionFieldValue afv1 = auf.getFieldValues()[1];
        assertEquals("float",
                     afv1.getField());
        assertEquals("25.0",
                     afv1.getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     afv1.getNature());

        ActionFieldValue afv2 = auf.getFieldValues()[2];
        assertEquals("long",
                     afv2.getField());
        assertEquals("25",
                     afv2.getValue());
        assertEquals(FieldNatureType.TYPE_LITERAL,
                     afv2.getNature());
    }

    @Test
    public void compositeFieldConstraintWithTwoPredicates() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Person( eval( age > 18 ) && eval(age < 45) )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;

        //LHS sub-patterns
        assertEquals(1,
                     fp.getNumberOfConstraints());
        assertTrue(fp.getConstraint(0) instanceof CompositeFieldConstraint);
        CompositeFieldConstraint fp_cfp = (CompositeFieldConstraint) fp.getConstraint(0);
        assertEquals("&&",
                     fp_cfp.getCompositeJunctionType());
        assertEquals(2,
                     fp_cfp.getNumberOfConstraints());
        assertTrue(fp_cfp.getConstraint(0) instanceof SingleFieldConstraint);
        assertTrue(fp_cfp.getConstraint(1) instanceof SingleFieldConstraint);

        SingleFieldConstraint fp_cfp_sfp1 = (SingleFieldConstraint) fp_cfp.getConstraint(0);
        assertEquals("age > 18",
                     fp_cfp_sfp1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_PREDICATE,
                     fp_cfp_sfp1.getConstraintValueType());

        SingleFieldConstraint fp_cfp_sfp2 = (SingleFieldConstraint) fp_cfp.getConstraint(1);
        assertEquals("age < 45",
                     fp_cfp_sfp2.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_PREDICATE,
                     fp_cfp_sfp2.getConstraintValueType());
    }

    @Test
    public void compositeFieldConstraintWithOnePredicateAndOneLiteral() {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Person( eval( age > 18 ) && age < 45 )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;

        //LHS sub-patterns
        assertEquals(1,
                     fp.getNumberOfConstraints());
        assertTrue(fp.getConstraint(0) instanceof CompositeFieldConstraint);
        CompositeFieldConstraint fp_cfp = (CompositeFieldConstraint) fp.getConstraint(0);
        assertEquals("&&",
                     fp_cfp.getCompositeJunctionType());
        assertEquals(2,
                     fp_cfp.getNumberOfConstraints());
        assertTrue(fp_cfp.getConstraint(0) instanceof SingleFieldConstraint);
        assertTrue(fp_cfp.getConstraint(1) instanceof SingleFieldConstraint);

        SingleFieldConstraint fp_cfp_sfp1 = (SingleFieldConstraint) fp_cfp.getConstraint(0);
        assertEquals("age > 18",
                     fp_cfp_sfp1.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_PREDICATE,
                     fp_cfp_sfp1.getConstraintValueType());

        SingleFieldConstraint fp_cfp_sfp2 = (SingleFieldConstraint) fp_cfp.getConstraint(1);
        assertEquals("Person",
                     fp_cfp_sfp2.getFactType());
        assertEquals("age",
                     fp_cfp_sfp2.getFieldName());
        assertEquals("<",
                     fp_cfp_sfp2.getOperator());
        assertEquals("45",
                     fp_cfp_sfp2.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     fp_cfp_sfp2.getConstraintValueType());
    }

    @Test
    public void newRHSFactsCanBeUsedInRHSBinding() throws Exception {
        String drl = "package org.mortgages;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$l : LoanApplication()\n"
                + "then\n"
                + "Applicant $a = new Applicant();\n"
                + "insert( $a );\n"
                + "modify( $l ) {\n"
                + "  setApplicant( $a )"
                + "}\n"
                + "end";

        addModelField("org.mortgages.Applicant",
                      "this",
                      "org.mortgages.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.mortgages.LoanApplication",
                      "this",
                      "org.mortgages.LoanApplication",
                      DataType.TYPE_THIS);
        addModelField("org.mortgages.LoanApplication",
                      "applicant",
                      "org.mortgages.Applicant",
                      "org.mortgages.Applicant");

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("r1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("LoanApplication",
                     fp.getFactType());
        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(2,
                     m.rhs.length);

        assertTrue(m.rhs[0] instanceof ActionInsertFact);
        ActionInsertFact aif = (ActionInsertFact) m.rhs[0];
        assertEquals("$a",
                     aif.getBoundName());
        assertEquals("Applicant",
                     aif.getFactType());
        assertEquals(0,
                     aif.getFieldValues().length);

        assertTrue(m.rhs[1] instanceof ActionUpdateField);
        ActionUpdateField auf = (ActionUpdateField) m.rhs[1];
        assertEquals("$l",
                     auf.getVariable());
        assertEquals(1,
                     auf.getFieldValues().length);
        ActionFieldValue afv0 = auf.getFieldValues()[0];
        assertEquals("applicant",
                     afv0.getField());
        assertEquals("=$a",
                     afv0.getValue());
        assertEquals(FieldNatureType.TYPE_VARIABLE,
                     afv0.getNature());
        assertEquals("Applicant",
                     afv0.getType());
    }

    @Test
    public void actionUpdateFieldWithFormula() throws Exception {
        String drl = "package org.mortgages;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$a : Application()\n"
                + "then\n"
                + "modify( $a ) {\n"
                + "  setName( \"Pupa\" + 20 + \"Smurf\" )"
                + "}\n"
                + "end";

        addModelField("org.mortgages.Applicant",
                      "this",
                      "org.mortgages.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.mortgages.Applicant",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("r1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Application",
                     fp.getFactType());
        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);

        assertTrue(m.rhs[0] instanceof ActionUpdateField);
        ActionUpdateField auf = (ActionUpdateField) m.rhs[0];
        assertEquals("$a",
                     auf.getVariable());
        assertEquals(1,
                     auf.getFieldValues().length);
        ActionFieldValue afv0 = auf.getFieldValues()[0];
        assertEquals("name",
                     afv0.getField());
        assertEquals("\"Pupa\" + 20 + \"Smurf\"",
                     afv0.getValue());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     afv0.getNature());
    }

    @Test
    public void actionUpdateFieldWithFormulaNotEndWithString() throws Exception {
        String drl = "package org.mortgages;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$a : Application()\n"
                + "then\n"
                + "modify( $a ) {\n"
                + "  setName( \"Pupa\" + 20 )"
                + "}\n"
                + "end";

        addModelField("org.mortgages.Applicant",
                      "this",
                      "org.mortgages.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.mortgages.Applicant",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("r1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Application",
                     fp.getFactType());
        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);

        assertTrue(m.rhs[0] instanceof ActionUpdateField);
        ActionUpdateField auf = (ActionUpdateField) m.rhs[0];
        assertEquals("$a",
                     auf.getVariable());
        assertEquals(1,
                     auf.getFieldValues().length);
        ActionFieldValue afv0 = auf.getFieldValues()[0];
        assertEquals("name",
                     afv0.getField());
        assertEquals("\"Pupa\" + 20",
                     afv0.getValue());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     afv0.getNature());
    }

    @Test
    public void actionUpdateFieldWithFormulaWithEscapedQuote() throws Exception {
        String drl = "package org.mortgages;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$a : Application()\n"
                + "then\n"
                + "modify( $a ) {\n"
                + "  setName( \"Pupa \\\"\" + 20 + \"\\\" Smurf\" )"
                + "}\n"
                + "end";

        addModelField("org.mortgages.Applicant",
                      "this",
                      "org.mortgages.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.mortgages.Applicant",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("r1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Application",
                     fp.getFactType());
        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);

        assertTrue(m.rhs[0] instanceof ActionUpdateField);
        ActionUpdateField auf = (ActionUpdateField) m.rhs[0];
        assertEquals("$a",
                     auf.getVariable());
        assertEquals(1,
                     auf.getFieldValues().length);
        ActionFieldValue afv0 = auf.getFieldValues()[0];
        assertEquals("name",
                     afv0.getField());
        assertEquals("\"Pupa \\\"\" + 20 + \"\\\" Smurf\"",
                     afv0.getValue());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     afv0.getNature());
    }

    @Test
    public void actionSetFieldWithFormula() throws Exception {
        String drl = "package org.mortgages;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$a : Application()\n"
                + "then\n"
                + "$a.setName( \"Pupa\" + 20 + \"Smurf\" );"
                + "end";

        addModelField("org.mortgages.Applicant",
                      "this",
                      "org.mortgages.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.mortgages.Applicant",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("r1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Application",
                     fp.getFactType());
        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);

        assertTrue(m.rhs[0] instanceof ActionSetField);
        ActionSetField asf = (ActionSetField) m.rhs[0];
        assertEquals("$a",
                     asf.getVariable());
        assertEquals(1,
                     asf.getFieldValues().length);
        ActionFieldValue afv0 = asf.getFieldValues()[0];
        assertEquals("name",
                     afv0.getField());
        assertEquals("\"Pupa\" + 20 + \"Smurf\"",
                     afv0.getValue());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     afv0.getNature());
    }

    @Test
    public void actionInsertFactWithFormula() throws Exception {
        String drl = "package org.mortgages;\n"
                + "rule \"r1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "$a : Application()\n"
                + "then\n"
                + "Applicant $a = new Applicant();\n"
                + "$a.setName( \"Pupa\" + 20 + \"Smurf\" );\n"
                + "insert( $a );\n"
                + "end";

        addModelField("org.mortgages.Applicant",
                      "this",
                      "org.mortgages.Applicant",
                      DataType.TYPE_THIS);
        addModelField("org.mortgages.Applicant",
                      "name",
                      String.class.getName(),
                      DataType.TYPE_STRING);

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("r1",
                     m.name);

        //LHS Pattern
        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Application",
                     fp.getFactType());
        assertEquals(0,
                     fp.getNumberOfConstraints());

        assertEquals(1,
                     m.rhs.length);

        assertTrue(m.rhs[0] instanceof ActionInsertFact);
        ActionInsertFact aif = (ActionInsertFact) m.rhs[0];
        assertEquals("$a",
                     aif.getBoundName());
        assertEquals(1,
                     aif.getFieldValues().length);
        ActionFieldValue afv0 = aif.getFieldValues()[0];
        assertEquals("name",
                     afv0.getField());
        assertEquals("\"Pupa\" + 20 + \"Smurf\"",
                     afv0.getValue());
        assertEquals(FieldNatureType.TYPE_FORMULA,
                     afv0.getNature());
    }

    @Test
    public void testForAll() throws Exception {
        // RHBPMS-4666
        String drl = "package org.test;\n" +
                "import java.util.List;\n" +
                "rule \"MyRule\" dialect \"mvel\" when\n" +
                "  $myList : List( empty == false )\n" +
                "  forall( String( this.startsWith( \"n\" ) ) from $myList )\n" +
                "then\n" +
                "end";

        when(dmo.getPackageName()).thenReturn("org.test");

        final RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                Collections.emptyList(),
                                                                                dmo);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/RHBPMS-4695
    public void testNestedEvalWithIdenticalCompositeOperator() {
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "MyTestDateObject(firstName != null && firstName != \"\" && eval($td.getFirstName().toUpperCase().equals(\"NONAME\") || $td.getFirstName().toUpperCase().equals(\"NAMENOTPROVIDED\")))\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/RHBPMS-4695
    public void testNestedEvalWithDifferentCompositeOperators() {
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "MyTestDateObject(firstName != null && firstName != \"\" || eval($td.getFirstName().toUpperCase().equals(\"NONAME\") || $td.getFirstName().toUpperCase().equals(\"NAMENOTPROVIDED\")))\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/RHBPMS-4695
    public void testNestedEvalWithIdenticalCompositeOperatorsAndTrailingConstraint() {
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "MyTestDateObject(firstName != null && eval($td.getFirstName().toUpperCase().equals(\"NONAME\") || $td.getFirstName().toUpperCase().equals(\"NAMENOTPROVIDED\")) && firstName != \"\" )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    //https://issues.jboss.org/browse/RHBPMS-4695
    public void testNestedEvalWithDifferentCompositeOperatorsAndTrailingConstraint() {
        String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "MyTestDateObject(firstName != null || eval($td.getFirstName().toUpperCase().equals(\"NONAME\") || $td.getFirstName().toUpperCase().equals(\"NAMENOTPROVIDED\")) && firstName != \"\" )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        //Check round-trip
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(m));
    }

    @Test
    public void testNotMatchesOrNotSoundsLike() {
        final String drl = "rule \"rule1\"\n"
                + "dialect \"mvel\"\n"
                + "when\n"
                + "MyTestObject(firstName not matches \"John\" || firstName not soundslike \"Peter\" )\n"
                + "then\n"
                + "end";

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                    Collections.emptyList(),
                                                                                    dmo);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(model));
    }

    @Test
    public void testNotMatchesAndNotSoundsLikeDeclaredInDsl() {
        final String drl = "package org.mortgages;\n" +
                "rule \"testdsl\"\n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    Applicant's name does not matches or does not sounds like John\n" +
                "  then\n" +
                "end";

        final String dslDefinition = "Applicant's name does not matches or does not sounds like {name}";
        final String dslFile = "[when]" + dslDefinition + "= Applicant( name not matches \"{name}\" || name not soundslike \"{name}\" )";

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshalUsingDSL(drl,
                                                                                            Collections.emptyList(),
                                                                                            dmo,
                                                                                            dslFile);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(model));
    }

    @Test
    public void unmarshalStringListsCorrectly() {
        final String drl = "package org.mortgages;\n" +
                "rule \"aaa\"\n" +
                "\tdialect \"mvel\"\n" +
                "\twhen\n" +
                "\t\tApplicant( applicantName : name in ( \"a\", \"b\", \"c\" ) )\n" +
                "\tthen\n" +
                "end";

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                    Collections.emptyList(),
                                                                                    dmo);

        SingleFieldConstraint fieldConstraint = model.getLHSBoundField("applicantName");

        assertEquals("a, b, c", fieldConstraint.getValue());
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(model));
    }

    @Test
    public void unmarshalStringListsCorrectly_ComplexValues() {
        final String drl = "package org.mortgages;\n" +
                "rule \"aaa\"\n" +
                "\tdialect \"mvel\"\n" +
                "\twhen\n" +
                "\t\tApplicant( applicantName : name in ( \"a\", \"b, something\",  \"c()\" ) )\n" +
                "\tthen\n" +
                "end";

        when(dmo.getPackageName()).thenReturn("org.mortgages");

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                    Collections.emptyList(),
                                                                                    dmo);

        SingleFieldConstraint fieldConstraint = model.getLHSBoundField("applicantName");

        assertEquals("a, \"b, something\", c()", fieldConstraint.getValue());
        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(model));
    }

    @Test
    public void testSingleFieldConstraintOperatorWithoutSpace() {
        assertSingleFieldConstraintOperatorNoSpace(Operator.EQUAL.getOperatorString());
        assertSingleFieldConstraintOperatorNoSpace(Operator.NOT_EQUAL.getOperatorString());
        assertSingleFieldConstraintOperatorNoSpace(Operator.LESS.getOperatorString());
        assertSingleFieldConstraintOperatorNoSpace(Operator.LESS_OR_EQUAL.getOperatorString());
        assertSingleFieldConstraintOperatorNoSpace(Operator.GREATER.getOperatorString());
        assertSingleFieldConstraintOperatorNoSpace(Operator.GREATER_OR_EQUAL.getOperatorString());
    }

    private void assertSingleFieldConstraintOperatorNoSpace(final String operator) {
        String drl = "rule \"rule1\"\n"
                + "when\n"
                + "Applicant( age" + operator + "55 )\n"
                + "then\n"
                + "end";

        RuleModel m = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                          Collections.emptyList(),
                                                                          dmo);

        assertNotNull(m);
        assertEquals("rule1",
                     m.name);

        assertEquals(1,
                     m.lhs.length);
        IPattern p = m.lhs[0];
        assertTrue(p instanceof FactPattern);

        FactPattern fp = (FactPattern) p;
        assertEquals("Applicant",
                     fp.getFactType());

        assertEquals(1,
                     fp.getConstraintList().getConstraints().length);
        assertTrue(fp.getConstraint(0) instanceof SingleFieldConstraint);

        SingleFieldConstraint sfp = (SingleFieldConstraint) fp.getConstraint(0);
        assertEquals("Applicant",
                     sfp.getFactType());
        assertEquals("age",
                     sfp.getFieldName());
        assertEquals(operator,
                     sfp.getOperator());
        assertEquals("55",
                     sfp.getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_LITERAL,
                     sfp.getConstraintValueType());
    }

    @Test
    public void testNestedOr() {
        final String drl = "rule \"my rule\"\n" +
                "dialect \"mvel\"\n" +
                "when \n" +
                "(\n" +
                "   (\n" +
                "      Term(effectiveDate < \"30-Sep-2018\") and \n" +
                "      Policy($state : state == \"KS\" || == \"MN\" || == \"NM\" || == \"UT\")\n" +
                "   )\n" +
                "   or\n" +
                "   (\n" +
                "      Term(effectiveDate < \"23-Jun-2019\") and\n" +
                "      ( \n" +
                "         Policy(state == \"AZ\" || == \"IA\" || == \"NE\" || == \"SD\" )\n" +
                "      ) \n" +
                "   )\n" +
                ") \n" +
                "then \n" +
                "end\n";

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                    Collections.emptyList(),
                                                                                    dmo);

        assertEqualsIgnoreWhitespace(drl,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(model));
    }

    @Test
    public void normalDRLWithTemplateKeys() {
        final String drl = "rule \"temp\" \n" +
                "when \n" +
                "$person : Person(gender == \"@{param1}\")\n" +
                "\n" +
                "then \n" +
                "$person.setHelloMsg(@{param2});\n" +
                "end";
        final String expectedDRL = "rule \"temp\" \n" +
                "dialect \"mvel\" \n" +
                "when \n" +
                "$person : Person(gender == \"@{removeDelimitingQuotes(param1)}\")\n" +
                "\n" +
                "then \n" +
                "$person.setHelloMsg(@{removeDelimitingQuotes(param2)});\n" +
                "end";

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                    Collections.emptyList(),
                                                                                    dmo);
        assertTrue(model.rhs[0] instanceof ActionSetField);

        assertEqualsIgnoreWhitespace(expectedDRL,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(model));
    }

    @Test
    public void oneSetterTwoTemplateKeys() {
        final String drl = "rule \"temp\" \n" +
                "when \n" +
                "$person : Person(gender == \"@{param1}\", married == \"@{param2}\")\n" +
                "\n" +
                "then \n" +
                "$person.setHelloMsg(hello($person.getName(), @{param3}, @{param4}));\n" +
                "System.out.println(hello($person.getName(), @{param5}, @{param6}));\n" +
                "end";
        final String expectedDRL = "rule \"temp\" \n" +
                "dialect \"mvel\" \n" +
                "when \n" +
                "$person : Person(gender == \"@{removeDelimitingQuotes(param1)}\", married == \"@{removeDelimitingQuotes(param2)}\")\n" +
                "\n" +
                "then \n" +
                "$person.setHelloMsg(hello($person.getName(), @{param3}, @{param4}));\n" +
                "System.out.println(hello($person.getName(), @{param5}, @{param6}));\n" +
                "end";

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                    Collections.emptyList(),
                                                                                    dmo);

        assertEqualsIgnoreWhitespace(expectedDRL,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(model));
    }

    @Test
    public void methodInSetter() {
        final String drl = "rule \"temp\" \n" +
                "when \n" +
                "$person : Person(gender == \"@{param1}\", married == \"@{param2}\")\n" +
                "\n" +
                "then \n" +
                "$person.setHelloMsg(hello(@{param3}));\n" +
                "end";
        final String expectedDRL = "rule \"temp\" \n" +
                "dialect \"mvel\" \n" +
                "when \n" +
                "$person : Person(gender == \"@{removeDelimitingQuotes(param1)}\", married == \"@{removeDelimitingQuotes(param2)}\")\n" +
                "\n" +
                "then \n" +
                "$person.setHelloMsg(hello(@{param3}));\n" +
                "end";

        final RuleModel model = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                    Collections.emptyList(),
                                                                                    dmo);

        assertEqualsIgnoreWhitespace(expectedDRL,
                                     RuleModelDRLPersistenceImpl.getInstance().marshal(model));
    }

    @Test
    public void testContainsWithFormula() {

        final String drl = "rule \"therule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "CustomerProfile( listValue contains ( String.join( \"-\", \"abc\",\"xyz\") )," +
                "listValue not contains ( String.join( \"hello\", \" \",\"there\") ) )\n" +
                "then\n" +
                "end";

        final PackageDataModelOracle dmo = mock(PackageDataModelOracle.class);
        final RuleModel m =  RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                           Collections.EMPTY_LIST,
                                                           dmo);
        assertNotNull(m);
        assertTrue(m.lhs[0] instanceof FactPattern);

        final FieldConstraint constraint = ((FactPattern) m.lhs[0]).getConstraint(0);
        assertTrue(constraint instanceof SingleFieldConstraint);
        assertEquals("contains", ((SingleFieldConstraint) constraint).getOperator());
        assertEquals("String.join( \"-\", \"abc\",\"xyz\")",((SingleFieldConstraint)constraint).getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_RET_VALUE, ((SingleFieldConstraint) constraint).getConstraintValueType());

        final FieldConstraint constraintNot = ((FactPattern) m.lhs[0]).getConstraint(1);
        assertTrue(constraintNot instanceof SingleFieldConstraint);
        assertEquals("not contains", ((SingleFieldConstraint) constraintNot).getOperator());
        assertEquals("String.join( \"hello\", \" \",\"there\")",((SingleFieldConstraint)constraintNot).getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_RET_VALUE, ((SingleFieldConstraint) constraintNot).getConstraintValueType());
    }

    @Test
    public void testContainsWithFormula_Integer() {

        final String drl = "rule \"therule\"\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "CustomerProfile( listValue contains ( Integer.max( 1, 2 ) )," +
                "listValue not contains ( Integer.min( 1, 2 ) ) )\n" +
                "then\n" +
                "end";

        final PackageDataModelOracle dmo = mock(PackageDataModelOracle.class);
        final RuleModel m =  RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl,
                                                                                 Collections.EMPTY_LIST,
                                                                                 dmo);
        assertNotNull(m);
        assertTrue(m.lhs[0] instanceof FactPattern);

        final FieldConstraint constraint = ((FactPattern) m.lhs[0]).getConstraint(0);
        assertTrue(constraint instanceof SingleFieldConstraint);
        assertEquals("contains", ((SingleFieldConstraint) constraint).getOperator());
        assertEquals("Integer.max( 1, 2 )",((SingleFieldConstraint)constraint).getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_RET_VALUE, ((SingleFieldConstraint) constraint).getConstraintValueType());

        final FieldConstraint constraintNot = ((FactPattern) m.lhs[0]).getConstraint(1);
        assertTrue(constraintNot instanceof SingleFieldConstraint);
        assertEquals("not contains", ((SingleFieldConstraint) constraintNot).getOperator());
        assertEquals("Integer.min( 1, 2 )",((SingleFieldConstraint)constraintNot).getValue());
        assertEquals(BaseSingleFieldConstraint.TYPE_RET_VALUE, ((SingleFieldConstraint) constraintNot).getConstraintValueType());
    }

    /**
     * For more details see https://issues.redhat.com/browse/DROOLS-5249
     */
    private void assertDateBoilerPlateCodeIsNotUnmarshaledToFreeFormLine(final IAction[] actions) {
        Assertions.assertThat(actions)
                .as("SimpleDateFormat and DateTimeFormatter boiler plates shouldn't be unmarshaled")
                .filteredOn(action -> action instanceof FreeFormLine)
                .noneMatch(action -> ((FreeFormLine) action).getText().contains("SimpleDateFormat"))
                .noneMatch(action -> ((FreeFormLine) action).getText().contains("DateTimeFormatter"));
    }
}
