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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.drools.compiler.lang.dsl.DSLMappingEntry;
import org.drools.compiler.lang.dsl.DSLTokenizedMappingFile;
import org.drools.compiler.lang.dsl.DefaultDSLMapping;
import org.drools.compiler.lang.dsl.DefaultExpander;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DSLMappingEntryTest {

    // Due to a bug in JDK 5, a workaround for zero-width lookbehind has to be used.
    // JDK works correctly with "(?<=^|\\W)"
    private static final String lookbehind = "(?:(?<=^)|(?<=\\W))";
    private static final String NL = System.getProperty("line.separator");

    private DSLMappingEntry createEntry( final String inputKey,
                                         final String inputValue) throws IOException {
        String mapping = "[condition][]" + inputKey + "=" + inputValue;
        StringReader dsl = new StringReader( mapping );
        DSLMappingEntry entry = null;
        try {
            DSLTokenizedMappingFile parser = new DSLTokenizedMappingFile();
            if ( parser.parseAndLoad( dsl ) ) {
                entry = parser.getMapping().getEntries().get( 0 );
            } else {
                throw new RuntimeException( "Error parsing entry: " + mapping + ": " + parser.getErrors().toString() );
            }
        } finally {
            dsl.close();
        }
        return entry;
    }

    @Test
    public void testPatternCalculation() throws IOException {
        final String inputKey = "The Customer name is {name} and surname is {surname} and it has US$ 50,00 on his {pocket}";
        final String inputValue = "Customer( name == \"{name}\", surname == \"{surname}\", money > $money )";

        final String expectedKeyP = lookbehind + "The\\s+Customer\\s+name\\s+is\\s+(.*?)\\s+and\\s+surname\\s+is\\s+(.*?)\\s+and\\s+it\\s+has\\s+US\\$\\s+50,00\\s+on\\s+his\\s+(.*?)$";
        final String expectedValP = "Customer( name == \"{name}\", surname == \"{surname}\", money > $money )";

        final DSLMappingEntry entry = createEntry( inputKey,
                                                   inputValue );

        assertEquals( inputKey,
                      entry.getMappingKey() );
        assertEquals( expectedKeyP,
                      entry.getKeyPattern().pattern() );
        assertEquals( inputValue,
                      entry.getMappingValue() );
        assertEquals( expectedValP,
                      entry.getValuePattern() );
    }

    @Test
    public void testPatternCalculation2() throws IOException {
        final String inputKey = "-name is {name}";
        final String inputValue = "name == \"{name}\"";

        final String expectedKeyP = lookbehind + "-\\s*name\\s+is\\s+(.*?)$";
        final String expectedValP = "name == \"{name}\"";

        final DSLMappingEntry entry = createEntry( inputKey,
                                                   inputValue );

        assertEquals( inputKey,
                      entry.getMappingKey() );
        assertEquals( expectedKeyP,
                      entry.getKeyPattern().pattern() );
        assertEquals( inputValue,
                      entry.getMappingValue() );
        assertEquals( expectedValP,
                      entry.getValuePattern() );

    }

    @Test
    public void testPatternCalculation3() throws IOException {
        final String inputKey = "- name is {name}";
        final String inputValue = "name == \"{name}\"";

        final String expectedKeyP = lookbehind + "-\\s*name\\s+is\\s+(.*?)$";
        final String expectedValP = "name == \"{name}\"";

        final DSLMappingEntry entry = createEntry( inputKey,
                                                   inputValue );

        assertEquals( inputKey,
                      entry.getMappingKey() );
        assertEquals( entry.getKeyPattern().pattern(),
                      expectedKeyP,
                      entry.getKeyPattern().pattern() );
        assertEquals( inputValue,
                      entry.getMappingValue() );
        assertEquals( expectedValP,
                      entry.getValuePattern() );
    }

    private DSLMappingEntry setupEntry() throws IOException {
        final String inputKey = "String is \"{value}\"";
        final String inputValue = "SomeFact(value==\"{value}\")";

        return createEntry( inputKey,
                            inputValue );
    }
        
    private DefaultExpander makeExpander( DSLMappingEntry... entries ){
        DefaultExpander expander = new DefaultExpander();
        DefaultDSLMapping mapping = new DefaultDSLMapping();
        for( DSLMappingEntry entry: entries ){
            mapping.addEntry( entry );
        }
        List<String> options = new ArrayList<String>();
        options.add("result");
        options.add("when");
        options.add("steps");
        mapping.setOptions(options);
        expander.addDSLMapping(mapping);
        return expander;
    }
    

    @Test
    public void testExpandSpaces() throws IOException {
        DSLMappingEntry entry = this.setupEntry();
        DefaultExpander ex = makeExpander( entry );
        String[] strs = new String[]{ "0_sp", " 1_sp", "   3_sp", "0_sp_1 ", 
                                      "0_sp_3   ", "0_sp 1_sp 2_sp", "   3_sp   3_sp 1_sp 1_sp_2  " };
        StringBuilder sb = new StringBuilder( "rule x\n" + "when\n" );
        for( String str: strs ){
            sb.append( "String is \"" + str + "\"" + NL );
        }
        sb.append( "then\n" + "end\n" );
        String dslr = sb.toString();
        String drl = ex.expand( dslr );
        
        for( String str: strs ){
            assertTrue( drl.contains( '"' + str + '"' ) );
        }
    }

    @Test
    public void testExpandWithDots() throws IOException {
        DSLMappingEntry entry1 = this.createEntry( "- {prop} is not {val} ", "{prop} != {val}" );
        DSLMappingEntry entry2 = this.createEntry( "- {prop} is {val} ",     "{prop} == {val}" );
        DSLMappingEntry entry3 = this.createEntry( "- {prop} is_not {val} ", "{prop} != {val}" );
        DefaultExpander ex = makeExpander( entry1, entry2, entry3 );
        StringBuilder sb = new StringBuilder( "rule x\n" ).append( "when\n" );
        sb.append( "> Foo()").append( NL );
        sb.append( "- type1 is ClientServiceType.TypeGOLD" ).append( NL );
        sb.append( "- type2 is_not ClientServiceType.TypeGOLD" ).append( NL );
        sb.append( "- type3 is not ClientServiceType.TypeGOLD" ).append( NL );
        sb.append( "then\n" ).append( "end\n" );
        String dslr = sb.toString();
        String drl = ex.expand( dslr );
        System.out.println( dslr );
        System.out.println( drl );
        assertTrue( "failure type1", drl.contains( "type1 == ClientServiceType.TypeGOLD" ) );
        assertTrue( "failure type2", drl.contains( "type2 != ClientServiceType.TypeGOLD" ) );
        assertTrue( "failure type3", drl.contains( "type3 != ClientServiceType.TypeGOLD" ) );
    }



    @Test
    public void testExpandWithBrackets() throws IOException {
        DSLMappingEntry entry1 = this.createEntry( "attr {attr_name} is in \\[ {values} \\]",
                                                   "{attr_name} in ( {values} )" );
        DSLMappingEntry entry2 = this.createEntry( "((H|h)e|(S|s)he) \\(is\\) (a|an) $xx {attribute} (man|woman)",
                                                   "Person( attribute == \"{attribute}\" )" );
        DSLMappingEntry entry3 = this.createEntry( "DSL sentence with {key1} {key2}",
                                                   "Sentence( {key1} == {key2} )" );
        DSLMappingEntry entry4 = this.createEntry( "When the credit rating is {rating:ENUM:Applicant.creditRating}",
                                                   "applicant:Applicant(credit=={rating})" );
        DSLMappingEntry entry5 = this.createEntry( "When the credit rating is {rating:regex:\\d{3}}",
                                                   "applicant:Applicant(credit=={rating})" );
        
        assertEquals( lookbehind + "When\\s+the\\s+credit\\s+rating\\s+is\\s+(\\d{3})(?=\\W|$)",
                      entry5.getKeyPattern().toString() );
        assertEquals( "applicant:Applicant(credit=={rating})",
                      entry5.getValuePattern() );

        DSLMappingEntry entry6 = this.createEntry( "This is a sentence with line breaks",
                                                   "Cheese\\n(price == 10)" );
        
        assertEquals( lookbehind + "This\\s+is\\s+a\\s+sentence\\s+with\\s+line\\s+breaks(?=\\W|$)",
                      entry6.getKeyPattern().toString() );
        assertEquals( "Cheese\n(price == 10)",
                      entry6.getValuePattern());

        DSLMappingEntry entry7 = this.createEntry( "Bedingung-\\#19-MKM4",
                                                   "eval ( $p.getTempVal(\"\\#UML-ATZ-1\") < $p.getZvUmlStfr() )" );
        
        assertEquals( lookbehind  + "Bedingung-#19-MKM4(?=\\W|$)",
                      entry7.getKeyPattern().toString() );
        assertEquals( "eval ( $p.getTempVal(\"#UML-ATZ-1\") < $p.getZvUmlStfr() )",
                       entry7.getValuePattern());
  
        DefaultExpander ex = makeExpander( entry1, entry2, entry3, entry4,
                                           entry5, entry6, entry7 );
        StringBuilder sb = new StringBuilder( "rule x\n" ).append( "when\n" );
        
        sb.append( "attr name is in [ 'Edson', 'Bob' ]" ).append( NL );
        sb.append( "he (is) a $xx handsome man" ).append( NL );
        sb.append( "DSL sentence with mykey myvalue" ).append( NL );
        sb.append( "When the credit rating is AA" ).append( NL );
        sb.append( "When the credit rating is 555" ).append( NL );
        sb.append( "This is a sentence with line breaks" ).append( NL );
        sb.append( "Bedingung-#19-MKM4" ).append( NL );
        sb.append( "then\n" + "end\n" );
        String dslr = sb.toString();
        String drl = ex.expand( dslr );

        for( String exp: new String[]{
                "name in ( 'Edson', 'Bob' )",
                "Person( attribute == \"handsome\" )",
                "Sentence( mykey == myvalue )",
//                "applicant:Applicant(credit==AA)",
                "applicant:Applicant(credit==555)",
                "Cheese\n(price == 10)",
                "eval ( $p.getTempVal(\"#UML-ATZ-1\") < $p.getZvUmlStfr() )" } ){

            assertTrue( "failed to expand to: " + exp, drl.contains( exp ) );
        }
    }
    
    @Test
    public void testCreateWithTilde() throws IOException {
    	//If the statement parses it passes. To test expressions in dsl with a ~
    	try{
        DSLMappingEntry entry1 = this.createEntry( "the benefit HAS an EXACT message of {ALLMSG}", 
        											"$benefit :Map(this['ALLMSG'] matches \"(?i).*~{ALLMSG}~.*\") from $benefits");
    	}catch(Exception e){
    		fail(e.getMessage());
    	}


    }
}
