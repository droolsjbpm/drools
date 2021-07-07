/*
 * Copyright 2006 Red Hat, Inc. and/or its affiliates.
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

package org.drools.mvel.compiler.rule.builder.dialect.java;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import org.drools.compiler.builder.impl.KnowledgeBuilderImpl;
import org.drools.compiler.compiler.DrlParser;
import org.drools.compiler.lang.descr.AndDescr;
import org.drools.compiler.lang.descr.AttributeDescr;
import org.drools.compiler.lang.descr.ExprConstraintDescr;
import org.drools.compiler.lang.descr.PackageDescr;
import org.drools.compiler.lang.descr.PatternDescr;
import org.drools.compiler.lang.descr.RuleDescr;
import org.drools.compiler.rule.builder.RuleBuildContext;
import org.drools.compiler.rule.builder.RuleBuilder;
import org.drools.core.base.EnabledBoolean;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.Pattern;
import org.drools.core.time.TimeUtils;
import org.drools.core.time.impl.IntervalTimer;
import org.drools.core.util.DateUtils;
import org.drools.mvel.MVELConstraint;
import org.junit.Test;
import org.kie.internal.builder.conf.LanguageLevelOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RuleBuilderTest {

    @Test
    public void testBuild() throws Exception {
        final DrlParser parser = new DrlParser(LanguageLevelOption.DRL5);

        final KnowledgeBuilderImpl kBuilder = new KnowledgeBuilderImpl();
        kBuilder.addPackage(new PackageDescr("org.drools"));
        InternalKnowledgePackage pkg = kBuilder.getPackage("org.drools");

        final PackageDescr pkgDescr = parser.parse( new InputStreamReader( getClass().getResourceAsStream( "nestedConditionalElements.drl" ) ) );

        // just checking there is no parsing errors
        assertFalse( parser.getErrors().toString(),
                            parser.hasErrors() );
        
        pkg.addGlobal("results", List.class);

        final RuleDescr ruleDescr = pkgDescr.getRules().get( 0 );
        final String ruleClassName = "RuleClassName.java";
        ruleDescr.setClassName( ruleClassName );
        ruleDescr.addAttribute(new AttributeDescr("dialect",
                                                  "java"));
        
        kBuilder.addPackage(pkgDescr);

        assertTrue(kBuilder.getErrors().toString(),
                   kBuilder.getErrors().isEmpty());

        final RuleImpl rule = kBuilder.getPackage("org.drools.mvel.compiler").getRule( "test nested CEs" );

        assertEquals( "There should be 2 rule level declarations",
                      2,
                      rule.getDeclarations().size() );

        // second GE should be a not
        final GroupElement not = (GroupElement) rule.getLhs().getChildren().get( 1 );
        assertTrue( not.isNot() );
        // not has no outer declarations
        assertTrue( not.getOuterDeclarations().isEmpty() );
        assertEquals( 1,
                      not.getInnerDeclarations().size() );
        assertTrue( not.getInnerDeclarations().keySet().contains( "$state" ) );

        // second not
        final GroupElement not2 = (GroupElement) ((GroupElement) not.getChildren().get( 0 )).getChildren().get( 1 );
        assertTrue( not2.isNot() );
        // not has no outer declarations
        assertTrue( not2.getOuterDeclarations().isEmpty() );
        assertEquals( 1,
                      not2.getInnerDeclarations().size() );
        assertTrue( not2.getInnerDeclarations().keySet().contains( "$likes" ) );
    }

    @Test
    public void testBuildAttributes() throws Exception {
        // creates mock objects
        final RuleBuildContext context = mock( RuleBuildContext.class );
        final RuleImpl rule = mock( RuleImpl.class );

        // creates input object
        final RuleDescr ruleDescr = new RuleDescr( "my rule" );
        ruleDescr.addAttribute( new AttributeDescr( "no-loop",
                                                    "true" ) );
        ruleDescr.addAttribute( new AttributeDescr( "auto-focus",
                                                    "false" ) );
        ruleDescr.addAttribute( new AttributeDescr( "agenda-group",
                                                    "my agenda" ) );
        ruleDescr.addAttribute( new AttributeDescr( "activation-group",
                                                    "my activation" ) );
        ruleDescr.addAttribute( new AttributeDescr( "lock-on-active",
                                                    "" ) );
        ruleDescr.addAttribute( new AttributeDescr( "enabled",
                                                    "false" ) );
        ruleDescr.addAttribute( new AttributeDescr( "duration",
                                                    "60" ) );
        ruleDescr.addAttribute( new AttributeDescr( "calendars",
                                                    "\"cal1\"" ) );
        ruleDescr.addAttribute( new AttributeDescr( "date-effective",
                                                    "10-Jul-1974" ) );
        ruleDescr.addAttribute( new AttributeDescr( "date-expires",
                                                    "10-Jul-2040" ) );

        // creates expected results
        final Calendar effective = Calendar.getInstance();
        effective.setTime( DateUtils.parseDate( "10-Jul-1974" ) );
        final Calendar expires = Calendar.getInstance();
        expires.setTime( DateUtils.parseDate( "10-Jul-2040" ) );

        // defining expectations on the mock object
        when( context.getRule() ).thenReturn( rule );
        when( context.getRuleDescr() ).thenReturn( ruleDescr );
        when( context.getKnowledgeBuilder() ).thenReturn( new KnowledgeBuilderImpl() );

        // calling the build method
        RuleBuilder.buildAttributes( context );

        // check expectations
        verify( rule ).setNoLoop( true );
        verify( rule ).setAutoFocus( false );
        verify( rule ).setAgendaGroup( "my agenda" );
        verify( rule ).setActivationGroup( "my activation" );
        verify( rule ).setLockOnActive( true );
        verify( rule ).setEnabled( EnabledBoolean.ENABLED_FALSE );
        verify( rule ).setTimer( new IntervalTimer( null,
                                                    null,
                                                    -1,
                                                    TimeUtils.parseTimeString( "60" ),
                                                    0 ) );
        verify( rule ).setCalendars( new String[]{"cal1"} );
        verify( rule ).setDateEffective( effective );
        verify( rule ).setDateExpires( expires );
    }

    @Test
    public void testBuildMetaAttributes() throws Exception {
        // creates mock objects
        final RuleBuildContext context = mock( RuleBuildContext.class );
        final RuleImpl rule = mock( RuleImpl.class );

        // creates input object
        final RuleDescr ruleDescr = new RuleDescr( "my rule" );
        ruleDescr.addAnnotation( "ruleId",
                                 "123" );
        ruleDescr.addAnnotation( "author",
                                 "Bob Doe" );
        ruleDescr.addAnnotation( "text",
                                 "\"It's a quoted\\\" string\"" );

        // creates expected results
        // defining expectations on the mock object
        when( context.getRule() ).thenReturn( rule );
        when( context.getRuleDescr() ).thenReturn( ruleDescr );
        when( context.getKnowledgeBuilder() ).thenReturn( new KnowledgeBuilderImpl() );

        // calling the build method
        RuleBuilder.buildMetaAttributes( context );

        // check expectations
        verify( rule ).addMetaAttribute( "ruleId",
                                         123 );
        verify( rule ).addMetaAttribute( "author",
                                         "Bob Doe" );
        verify( rule ).addMetaAttribute( "text",
                                         "It's a quoted\" string" );
    }

    @Test
    public void testBuildDurationExpression() throws Exception {
        // creates mock objects
        final RuleBuildContext context = mock( RuleBuildContext.class );
        final RuleImpl rule = mock( RuleImpl.class );

        // creates input object
        final RuleDescr ruleDescr = new RuleDescr( "my rule" );
        ruleDescr.addAttribute( new AttributeDescr( "duration",
                                                    "( 1h30m )" ) );
        ruleDescr.addAttribute( new AttributeDescr( "calendars",
                                                    "[\"cal1\", \"cal2\"]" ) );

        // defining expectations on the mock object
        when( context.getRule() ).thenReturn( rule );
        when( context.getRuleDescr() ).thenReturn( ruleDescr );

        // calling the build method
        RuleBuilder.buildAttributes( context );

        // check expectations
        verify( rule ).setTimer( new IntervalTimer( null,
                                                    null,
                                                    -1,
                                                    TimeUtils.parseTimeString( "1h30m" ),
                                                    0 ) );
        verify( rule ).setCalendars( new String[]{"cal1", "cal2"} );
    }

    @Test
    public void testBuildBigDecimalLiteralConstraint() throws Exception {
        final PackageDescr pkgDescr = new PackageDescr( "org.drools" );
        final RuleDescr ruleDescr = new RuleDescr( "Test Rule" );
        AndDescr andDescr = new AndDescr();
        PatternDescr patDescr = new PatternDescr( "java.math.BigDecimal",
                                                  "$bd" );
        ExprConstraintDescr fcd = new ExprConstraintDescr( "this == 10" );
        patDescr.addConstraint( fcd );
        andDescr.addDescr( patDescr );
        ruleDescr.setLhs( andDescr );
        ruleDescr.setConsequence( "" );
        pkgDescr.addRule( ruleDescr );

        final KnowledgeBuilderImpl kBuilder = new KnowledgeBuilderImpl();
        kBuilder.addPackage(pkgDescr);

        assertTrue(kBuilder.getErrors().toString(),
                   kBuilder.getErrors().isEmpty());

        final RuleImpl rule = kBuilder.getPackages()[0].getRule( "Test Rule" );
        final GroupElement and = rule.getLhs();
        final Pattern pat = (Pattern) and.getChildren().get( 0 );
        if (pat.getConstraints().get(0) instanceof MVELConstraint) {
            final MVELConstraint fc = (MVELConstraint) pat.getConstraints().get( 0 );
            assertTrue( "Wrong class. Expected java.math.BigDecimal. Found: " + fc.getField().getValue().getClass(),
                        fc.getField().getValue() instanceof BigDecimal );
        }
    }

    @Test
    public void testInvalidDialect() throws Exception {
        final PackageDescr pkgDescr = new PackageDescr( "org.drools" );
        final RuleDescr ruleDescr = new RuleDescr( "Test Rule" );
        ruleDescr.addAttribute( new AttributeDescr( "dialect", "mvl" ) );
        ruleDescr.setConsequence( "" );
        pkgDescr.addRule( ruleDescr );

        final KnowledgeBuilderImpl kBuilder = new KnowledgeBuilderImpl();
        kBuilder.addPackage(pkgDescr);

        assertFalse(kBuilder.getErrors().toString(),
                    kBuilder.getErrors().isEmpty());
    }    
    
    @Test
    public void testBuildBigIntegerLiteralConstraint() throws Exception {
        final PackageDescr pkgDescr = new PackageDescr( "org.drools" );
        final RuleDescr ruleDescr = new RuleDescr( "Test Rule" );
        AndDescr andDescr = new AndDescr();
        PatternDescr patDescr = new PatternDescr( "java.math.BigInteger",
                                                  "$bd" );
        ExprConstraintDescr fcd = new ExprConstraintDescr( "this==10" );
        patDescr.addConstraint( fcd );
        andDescr.addDescr( patDescr );
        ruleDescr.setLhs( andDescr );
        ruleDescr.setConsequence( "" );
        pkgDescr.addRule( ruleDescr );

        final KnowledgeBuilderImpl kBuilder = new KnowledgeBuilderImpl();
        kBuilder.addPackage(pkgDescr);

        assertTrue(kBuilder.getErrors().toString(),
                   kBuilder.getErrors().isEmpty());

        final RuleImpl rule = kBuilder.getPackages()[0].getRule( "Test Rule" );
        final GroupElement and = rule.getLhs();
        final Pattern pat = (Pattern) and.getChildren().get( 0 );
        if (pat.getConstraints().get(0) instanceof MVELConstraint) {
            final MVELConstraint fc = (MVELConstraint) pat.getConstraints().get( 0 );
            assertTrue( "Wrong class. Expected java.math.BigInteger. Found: " + fc.getField().getValue().getClass(),
                        fc.getField().getValue() instanceof BigInteger );
        }
    }

}
