/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.mvel.compiler.lang.dsl;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.drools.compiler.lang.ExpanderException;
import org.drools.compiler.lang.dsl.DSLMappingFile;
import org.drools.compiler.lang.dsl.DSLTokenizedMappingFile;
import org.drools.compiler.lang.dsl.DefaultExpander;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultExpanderTest {

    private static final String     NL            = System.getProperty("line.separator");

    private DSLMappingFile file          = null;
    private DSLTokenizedMappingFile tokenizedFile = null;
    private DefaultExpander expander      = null;

    @Before
    public void setUp() throws Exception {
        final String filename = "test_metainfo.dsl";
        final Reader reader = new InputStreamReader(this.getClass().getResourceAsStream(filename));
        this.file = new DSLTokenizedMappingFile();
        this.tokenizedFile = new DSLTokenizedMappingFile();
        this.file.parseAndLoad(reader);
        reader.close();

        final Reader reader2 = new InputStreamReader(this.getClass().getResourceAsStream(filename));
        this.tokenizedFile.parseAndLoad(reader2);
        reader2.close();

        this.expander = new DefaultExpander();
    }

    @Test
    public void testAddDSLMapping() {
        this.expander.addDSLMapping(this.file.getMapping());
        // should not raise any exception
    }

    @Test
    public void testANTLRAddDSLMapping() {
        this.expander.addDSLMapping(this.tokenizedFile.getMapping());
        // should not raise any exception
    }

    @Test
    public void testRegexp() throws Exception {
        this.expander.addDSLMapping(this.file.getMapping());
        final Reader rules = new InputStreamReader(this.getClass().getResourceAsStream("test_expansion.dslr"));
        final String result = this.expander.expand(rules);
    }

    @Test
    public void testANTLRRegexp() throws Exception {
        this.expander.addDSLMapping(this.tokenizedFile.getMapping());
        final Reader rules = new InputStreamReader(this.getClass().getResourceAsStream("test_expansion.dslr"));
        final String result = this.expander.expand(rules);
    }

    @Test
    public void testExpandParts() throws Exception {
        DSLMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]foo=Foo()" + NL + "[then]bar {num}=baz({num});";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());
        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        //System.err.println(ex.expand( "rule 'x' " + NL + " when " + NL + " foo " + NL + " then " + NL + " end" ));
    }

    @Test
    public void testExpandKeyword() throws Exception {
        DSLMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[keyword]key {param}=Foo( attr=={param} )";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());
        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        String source = "rule x" + NL + "when" + NL + " key 1 " + NL + " key 2 " + NL + "then" + NL + "end";
        String drl = ex.expand(source);
        System.out.println(drl);

        assertTrue(drl.contains("attr==1"));
        assertTrue(drl.contains("attr==2"));
        //System.err.println(ex.expand( "rule 'x' " + NL + " when " + NL + " foo " + NL + " then " + NL + " end" ));
    }

    @Test
    public void testANTLRExpandParts() throws Exception {
        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]foo=Foo()" + NL + "[then]bar {num}=baz({num});";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());
        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        //System.err.println(ex.expand( "rule 'x' " + NL + " when " + NL + " foo " + NL + " then " + NL + " end" ));
    }

    @Test
    public void testExpandFailure() throws Exception {

        DSLMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]foo=Foo()" + NL + "[then]bar {num}=baz({num});";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());
        String source = "rule 'q'" + NL + "agenda-group 'x'" + NL + "when" + NL + "    foo  " + NL + "then" + NL + "    bar 42" + NL + "end";
        String drl = ex.expand(source);
        assertFalse(ex.hasErrors());

        ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        source = "rule 'q' agenda-group 'x'" + NL + "when" + NL + "    foos " + NL + "then" + NL + "    bar 42" + NL + " end";
        drl = ex.expand(source);
        //System.out.println( drl );
        assertTrue(ex.hasErrors());
        assertEquals(1,
                ex.getErrors().size());
        //System.err.println(( (ExpanderException) ex.getErrors().get( 0 )).getMessage());
    }

    @Test
    public void testANTLRExpandFailure() throws Exception {

        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]foo=Foo()" + NL + "[then]bar {num}=baz({num});";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());
        String source = "rule 'q'" + NL + "agenda-group 'x'" + NL + "when" + NL + "    foo  " + NL + "then" + NL + "    bar 42" + NL + "end";
        String drl = ex.expand(source);
        assertFalse(ex.hasErrors());

        ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        source = "rule 'q' agenda-group 'x'" + NL + "when" + NL + "    foos " + NL + "then" + NL + "    bar 42" + NL + " end";
        drl = ex.expand(source);
        //System.out.println( drl );
        assertTrue(ex.hasErrors());
        assertEquals(1,
                ex.getErrors().size());
        //System.err.println(( (ExpanderException) ex.getErrors().get( 0 )).getMessage());
    }

    @Test
    public void testExpandWithKeywordClashes() throws Exception {

        DSLMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]Invoke rule executor=ruleExec: RuleExecutor()" + NL + "[then]Execute rule \"{id}\"=ruleExec.ExecuteSubRule( new Long({id}));";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());
        String source = "package something;" + NL + NL + "rule \"1\"" + NL + "when" + NL + "    Invoke rule executor" + NL + "then" + NL + "    Execute rule \"5\"" + NL + "end";
        String expected = "package something;" + NL + NL + "rule \"1\"" + NL + "when" + NL + "   ruleExec: RuleExecutor()" + NL + "then" + NL + "   ruleExec.ExecuteSubRule( new Long(5));" + NL + "end" + NL;
        String drl = ex.expand(source);
        //        System.out.println("["+drl+"]" );
        //        System.out.println("["+expected+"]" );
        assertFalse(ex.hasErrors());
        equalsIgnoreWhiteSpace(expected,
                drl);

    }

    @Test
    public void testANTLRExpandWithKeywordClashes() throws Exception {

        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]Invoke rule executor=ruleExec: RuleExecutor()" + NL + "[then]Execute rule \"{id}\"=ruleExec.ExecuteSubRule( new Long({id}));";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());
        String source = "package something;" + NL + NL + "rule \"1\"" + NL + "when" + NL + "    Invoke rule executor" + NL + "then" + NL + "    Execute rule \"5\"" + NL + "end";
        String expected = "package something;" + NL + NL + "rule \"1\"" + NL + "when" + NL + "    ruleExec: RuleExecutor()" + NL + "then" + NL + "    ruleExec.ExecuteSubRule( new Long(5));" + NL + "end";
        String drl = ex.expand(source);
        //        System.out.println("["+drl+"]" );
        //        System.out.println("["+expected+"]" );
        assertFalse(ex.hasErrors());
        assertEquals(expected,
                drl);
    }

    @Test
    public void testLineNumberError() throws Exception {
        DSLMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]foo=Foo()" + NL + "[then]bar {num}=baz({num});";
        file.parseAndLoad(new StringReader(dsl));

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());
        String source = "rule 'q'" + NL + "agenda-group 'x'" + NL + "when" + NL + "    __  " + NL +
                "then" + NL + "    bar 42" + NL + "\tgoober" + NL + "end";
        ex.expand(source);
        assertTrue(ex.hasErrors());
        assertEquals(2,
                ex.getErrors().size());
        ExpanderException err = (ExpanderException) ex.getErrors().get(0);
        assertEquals(4,
                err.getLine());
        err = (ExpanderException) ex.getErrors().get(1);
        assertEquals(7,
                err.getLine());

    }

    @Test
    public void testANTLRLineNumberError() throws Exception {
        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]foo=Foo()" + NL + "[then]bar {num}=baz({num});";
        file.parseAndLoad(new StringReader(dsl));

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());
        String source = "rule 'q'" + NL + "agenda-group 'x'" + NL + "when" + NL + "    __  " + NL +
                "then" + NL + "    bar 42" + NL + "\tgoober" + NL + "end";
        ex.expand(source);
        assertTrue(ex.hasErrors());
        assertEquals(2,
                ex.getErrors().size());
        ExpanderException err = (ExpanderException) ex.getErrors().get(0);
        assertEquals(4,
                err.getLine());
        err = (ExpanderException) ex.getErrors().get(1);
        assertEquals(7,
                err.getLine());

    }

    @Test
    public void testANTLREnumExpand() throws Exception {
        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]When the credit rating is {rating:ENUM:Applicant.creditRating} = applicant:Applicant(credit=={rating})";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());
        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());
        String source = "rule \"TestNewDslSetup\"" + NL + "dialect \"mvel\"" + NL + "when" + NL + "When the credit rating is AA" + NL + "then " + NL + "end";

        //        String source="rule \"TestNewDslSetup\"" + NL+
        //                        "dialect \"mvel\"" + NL+
        //                        "when" + NL+
        //                            "When the credit rating is OK" + NL+
        //                        "then" + NL+
        //                        "end" + NL;

        String drl = ex.expand(source);

        String expected = "rule \"TestNewDslSetup\"" + NL +
                "dialect \"mvel\"" + NL +
                "when" + NL +
                "applicant:Applicant(credit==AA)" + NL +
                "then  " + NL + "end";

        assertFalse(ex.getErrors().toString(),
                ex.hasErrors());
        assertEquals(expected,
                drl);

        //System.err.println(ex.expand( "rule 'x' " + NL + " when " + NL + " foo " + NL + " then " + NL + " end" ));
    }

    private boolean equalsIgnoreWhiteSpace(String expected,
            String actual) {
        String patternStr = expected.replaceAll("\\s+",
                "(\\\\s|\\\\n|\\\\r)*");//.replaceAll( "\\n", "\\s*\\$" );
        Pattern pattern = Pattern.compile(patternStr,
                Pattern.DOTALL);
        Matcher m = pattern.matcher(actual);
        return m.matches();
    }

    @Test
    public void testExpandComplex() throws Exception {
        String source = "rule \"R\"" + NL
                + "dialect \"mvel\"" + NL
                + "when" + NL
                + "There is an TestObject" + NL
                + "-startDate is before 01-Jul-2011" + NL
                + "-endDate is after 01-Jul-2011" + NL
                + "then" + NL
                + "end" + NL;

        String expected = "rule \"R\"" + NL
                + "dialect \"mvel\"" + NL
                + "when" + NL
                + "TestObject(startDate>DateUtils.parseDate(\"01-Jul-2011\"), endDate>DateUtils.parseDate(\"01-Jul-2011\"))" + NL
                + "then" + NL
                + "end" + NL;

        checkExpansion(source, expected);
    }

    @Test
    public void testDontExpandCommentedLines() throws Exception {
        String source = "rule \"R\"" + NL
                + "dialect \"mvel\"" + NL
                + "when" + NL
                + "// There is an TestObject" + NL
                + "// -startDate is before 01-Jul-2011" + NL
                + "// -endDate is after 01-Jul-2011" + NL
                + "then" + NL
                + "end" + NL;

        String expected = "rule \"R\"" + NL
                + "dialect \"mvel\"" + NL
                + "when" + NL
                + "// There is an TestObject" + NL
                + "// -startDate is before 01-Jul-2011" + NL
                + "// -endDate is after 01-Jul-2011" + NL
                + "then" + NL
                + "end" + NL;

        checkExpansion(source, expected);
    }

    @Test
    public void testDontExpandCommentedBlocks() throws Exception {
        String source = "rule \"R\"" + NL
                + "dialect \"mvel\"" + NL
                + "when" + NL
                + "/*" + NL
                + "There is an TestObject" + NL
                + "-startDate is before 01-Jul-2011" + NL
                + "-endDate is after 01-Jul-2011" + NL
                + "*/" + NL
                + "then" + NL
                + "end" + NL;

        String expected = "rule \"R\"" + NL
                + "dialect \"mvel\"" + NL
                + "when" + NL
                + "" + NL
                + "then" + NL
                + "end" + NL;

        checkExpansion(source, expected);
    }

    private void checkExpansion(String source, String expected) throws Exception {
        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]There is an TestObject=TestObject()" + NL
                + "[when]-startDate is before {date}=startDate>DateUtils.parseDate(\"{date}\")" + NL
                + "[when]-endDate is after {date}=endDate>DateUtils.parseDate(\"{date}\")";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        String drl = ex.expand(source);
        assertFalse(ex.hasErrors());

        assertEquals(expected, drl);
    }

    @Test
    public void testExpandQuery() throws Exception {
        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]There is a person=Person()" + NL +
                "[when]- {field:\\w*} {operator} {value:\\d*}={field} {operator} {value}" + NL +
                "[when]is greater than=>";

        String source = "query \"isMature\"" + NL +
                "There is a person" + NL +
                "- age is greater than 18" + NL +
                "end" + NL;

        String expected = "query \"isMature\"" + NL +
                "Person(age  >  18)" + NL +
                "end" + NL;

        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        String drl = ex.expand(source);
        assertFalse(ex.hasErrors());

        assertEquals(expected, drl);
    }

    @Test
    public void testExpandExpr() throws Exception {
        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]Name of Applicant {nameVar:CF:Applicant.age}= System.out.println({nameVar})";

        String source = "rule \"test rule for custom form in DSL\"" + NL +
                "     dialect \"mvel\"" + NL +
                "     when" + NL +
                "         Name of Applicant Bojan Oklahoma and NJ,Andrew AMW Test" + NL +
                "     then" + NL +
                "end";

        String expected = "rule \"test rule for custom form in DSL\"" + NL +
                "     dialect \"mvel\"" + NL +
                "     when" + NL +
                "         System.out.println(Bojan Oklahoma and NJ,Andrew AMW Test)" + NL +
                "     then" + NL +
                "end";

        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0,
                file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        String drl = ex.expand(source);

        assertFalse(ex.hasErrors());

        assertEquals(expected, drl);
    }

    @Test(timeout = 1000)
    public void testExpandInfiniteLoop() throws Exception {
        // DROOLS-73
        DSLMappingFile file = new DSLTokenizedMappingFile();
        String dsl = "[when]Foo with {var} bars=Foo( bars == {var} )";
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(0, file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());
        String source = "rule 'dsl rule'" + NL + "when" + NL + " Foo with {var} bars" + NL + "then" + NL + NL + "end";
        ex.expand(source);
        assertFalse(ex.hasErrors());
    }

    @Test
    public void testEqualSignInTernaryOp() throws Exception {
        // BZ-1013960
        String source =
                "declare Person" + NL +
                        "    age : int" + NL +
                        "    name : String" + NL +
                        "end" + NL +
                        "" + NL +
                        "rule \"Your First Rule\"" + NL +
                        "    when" + NL +
                        "        There is a Person" + NL +
                        "            - with a negative age" + NL +
                        "            - with a positive age" + NL +
                        "            - with a zero age" + NL +
                        "    then" + NL +
                        "        print \"Your First Rule\"" + NL +
                        "" + NL +
                        "end" + NL;

        String dsl =
                "[when][]There is an? {entity}=${entity!lc}: {entity!ucfirst}()" + NL +
                        "[when][]- with an? {attr} greater than {amount}={attr} > {amount!num}" + NL +
                        "[then]print \"{text}\"=System.out.println(\"{text}\");" + NL +
                        "" + NL +
                        "[when]- with a {what} {attr}={attr} {what!zero?==0/!=0}" + NL;

        String expected =
                "declare Person" + NL +
                        "    age : int" + NL +
                        "    name : String" + NL +
                        "end" + NL +
                        "" + NL +
                        "rule \"Your First Rule\"" + NL +
                        "    when" + NL +
                        "        $person: Person(age  !=0, age  !=0, age  ==0)" + NL +
                        "    then" + NL +
                        "        System.out.println(\"Your First Rule\");" + NL +
                        "" + NL +
                        "end" + NL;

        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(file.getErrors().toString(),
                0,
                file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        String drl = ex.expand(source);
        assertFalse(ex.hasErrors());

        assertEquals(expected, drl);
    }

    @Test
    public void testDotInPattern() throws Exception {
        // BZ-1013960
        String source =
                "import org.drools.mvel.compiler.Person;" + NL
                        + "global java.util.List list" + NL
                        + "rule R1" + NL
                        + "when" + NL
                        + "then" + NL
                        + "Log X" + NL
                        + "end" + NL;

        String dsl =
                "[then]Log {message:.}=list.add(\"{message}\");";

        String expected =
                "import org.drools.mvel.compiler.Person;" + NL
                        + "global java.util.List list" + NL
                        + "rule R1" + NL
                        + "when" + NL
                        + "then" + NL
                        + "list.add(\"X\");" + NL
                        + "end" + NL;

        DSLTokenizedMappingFile file = new DSLTokenizedMappingFile();
        file.parseAndLoad(new StringReader(dsl));
        assertEquals(file.getErrors().toString(),
                0,
                file.getErrors().size());

        DefaultExpander ex = new DefaultExpander();
        ex.addDSLMapping(file.getMapping());

        String drl = ex.expand(source);
        assertFalse(ex.hasErrors());

        assertEquals(expected, drl);
    }

}
