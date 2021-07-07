/*
 * Copyright 2008 Red Hat
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
package org.drools.mvel.compiler.builder;

import org.drools.core.base.accumulators.AverageAccumulateFunction;
import org.drools.core.base.accumulators.MaxAccumulateFunction;
import org.drools.core.base.evaluators.AfterEvaluatorDefinition;
import org.drools.core.base.evaluators.BeforeEvaluatorDefinition;
import org.drools.core.base.evaluators.EvaluatorDefinition;
import org.junit.Before;
import org.junit.Test;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.builder.conf.AccumulateFunctionOption;
import org.kie.internal.builder.conf.DefaultDialectOption;
import org.kie.internal.builder.conf.DefaultPackageNameOption;
import org.kie.internal.builder.conf.DumpDirOption;
import org.kie.internal.builder.conf.EvaluatorOption;
import org.kie.internal.builder.conf.LanguageLevelOption;
import org.kie.internal.builder.conf.ProcessStringEscapesOption;
import org.kie.api.runtime.rule.AccumulateFunction;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KnowledgeBuilderConfigurationTest {

    private KnowledgeBuilderConfiguration config;

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        config = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
    }

    @Test
    public void testDefaultDialectConfiguration() {
        // setting the default dialect using the type safe method
        config.setOption( DefaultDialectOption.get( "mvel" ) );

        // checking the type safe getOption() method
        assertEquals( DefaultDialectOption.get( "mvel" ),
                      config.getOption( DefaultDialectOption.class ) );
        // checking string conversion
        assertEquals( "mvel",
                      config.getOption( DefaultDialectOption.class ).getName() );
        // checking the string based getProperty() method
        assertEquals( "mvel",
                      config.getProperty( DefaultDialectOption.PROPERTY_NAME ) );

        // setting the default dialect using the string based setProperty() method
        config.setProperty( DefaultDialectOption.PROPERTY_NAME,
                            "java" );
        
        // checking the type safe getOption() method
        assertEquals( DefaultDialectOption.get( "java" ),
                      config.getOption( DefaultDialectOption.class ) );
        assertEquals( "DefaultDialectOption( name=java )",
                      config.getOption( DefaultDialectOption.class ).toString() );
        // checking string conversion
        assertEquals( "java",
                      config.getOption( DefaultDialectOption.class ).getName() );
        // checking the string based getProperty() method
        assertEquals( "java",
                      config.getProperty( DefaultDialectOption.PROPERTY_NAME ) );
    }
    
    @Test
    public void testLanguageLevelConfiguration() {
        // setting the language level using the type safe method
        config.setOption( LanguageLevelOption.DRL5 );

        // checking the type safe getOption() method
        assertEquals( LanguageLevelOption.DRL5,
                      config.getOption( LanguageLevelOption.class ) );
        // checking string conversion
        assertEquals( LanguageLevelOption.DRL5,
                      config.getOption( LanguageLevelOption.class ) );
        // checking the string based getProperty() method
        assertEquals( "DRL5",
                      config.getProperty( LanguageLevelOption.PROPERTY_NAME ) );

        // setting the default dialect using the string based setProperty() method
        config.setProperty( LanguageLevelOption.PROPERTY_NAME,
                            "DRL6" );

        // checking the type safe getOption() method
        assertEquals(LanguageLevelOption.DRL6,
                config.getOption(LanguageLevelOption.class));
        assertEquals( "DRL6",
                      config.getOption( LanguageLevelOption.class ).toString() );
        // checking string conversion
        assertEquals( LanguageLevelOption.DRL6,
                      config.getOption( LanguageLevelOption.class ) );
        // checking the string based getProperty() method
        assertEquals( "DRL6",
                      config.getProperty( LanguageLevelOption.PROPERTY_NAME ) );
    }

    @Test
    public void testAccumulateFunctionConfiguration() {
        Set<String> keySet = new HashSet<String>();
        // in this use case, the application already has the instance of the accumulate function
        AccumulateFunction function = new AverageAccumulateFunction();
        
        // creating the option and storing in a local var just to make test easier
        AccumulateFunctionOption option = AccumulateFunctionOption.get( "avg", function );
        
        // wiring the accumulate function using the type safe method
        config.setOption( option );

        // checking the type safe getOption() method
        assertEquals( option,
                      config.getOption( AccumulateFunctionOption.class, "avg" ) );
        // checking string conversion
        assertEquals( "avg",
                      config.getOption( AccumulateFunctionOption.class, "avg" ).getName() );
        assertEquals( function,
                      config.getOption( AccumulateFunctionOption.class, "avg" ).getFunction() );
        // checking the string based getProperty() method
        assertEquals( AverageAccumulateFunction.class.getName(),
                      config.getProperty( AccumulateFunctionOption.PROPERTY_NAME+"avg" ) );
        // check the key set
        keySet.add( "avg" );
        assertTrue( config.getOptionKeys(AccumulateFunctionOption.class).contains( "avg" ) );

        // wiring the accumulate function using the string based setProperty() method
        config.setProperty( AccumulateFunctionOption.PROPERTY_NAME+"maximum",
                            MaxAccumulateFunction.class.getName() );
        
        MaxAccumulateFunction max = new MaxAccumulateFunction();
        // checking the type safe getOption() method
        assertEquals( AccumulateFunctionOption.get( "maximum", max ),
                      config.getOption( AccumulateFunctionOption.class, "maximum" ) );
        // checking string conversion
        assertEquals( "maximum",
                      config.getOption( AccumulateFunctionOption.class, "maximum" ).getName() );
        assertEquals( max.getClass().getName(),
                      config.getOption( AccumulateFunctionOption.class, "maximum" ).getFunction().getClass().getName() );
        // checking the string based getProperty() method
        assertEquals( MaxAccumulateFunction.class.getName(),
                      config.getProperty( AccumulateFunctionOption.PROPERTY_NAME+"maximum" ) );
        keySet.add( "avg" );
        
        // wiring the inner class accumulate function using the string based setProperty() method
        config.setProperty( AccumulateFunctionOption.PROPERTY_NAME+"inner",
                            InnerAccumulateFuncion.class.getName() );
        
        InnerAccumulateFuncion inner = new InnerAccumulateFuncion();
        // checking the type safe getOption() method
        assertEquals( AccumulateFunctionOption.get( "inner", inner ),
                      config.getOption( AccumulateFunctionOption.class, "inner" ) );
        // checking string conversion
        assertEquals( "inner",
                      config.getOption( AccumulateFunctionOption.class, "inner" ).getName() );
        assertEquals( inner.getClass().getName(),
                      config.getOption( AccumulateFunctionOption.class, "inner" ).getFunction().getClass().getName() );
        // checking the string based getProperty() method
        assertEquals( InnerAccumulateFuncion.class.getName(),
                      config.getProperty( AccumulateFunctionOption.PROPERTY_NAME+"inner" ) );
        keySet.add( "avg" );

        assertTrue( config.getOptionKeys(AccumulateFunctionOption.class).containsAll( keySet ) );
//        for( String key: config.getOptionKeys(AccumulateFunctionOption.class ) ){
//            System.out.println( key + "->" + config.getOption(AccumulateFunctionOption.class, key).getClass().getName() );
//        }
    }
    
    @Test
    public void testDumpDirectoryConfiguration() {
        File dumpDir = new File("target");
        // setting the dump directory using the type safe method
        config.setOption( DumpDirOption.get( dumpDir ) );

        // checking the type safe getOption() method
        assertEquals( DumpDirOption.get( dumpDir ),
                      config.getOption( DumpDirOption.class ) );
        // checking string conversion
        assertEquals( dumpDir,
                      config.getOption( DumpDirOption.class ).getDirectory() );
        // checking the string based getProperty() method
        assertEquals( dumpDir.toString(),
                      config.getProperty( DumpDirOption.PROPERTY_NAME ) );

        // setting the dump dir using the string based setProperty() method
        dumpDir = new File( System.getProperty( "java.io.tmpdir" ) );
        config.setProperty( DumpDirOption.PROPERTY_NAME,
                            System.getProperty( "java.io.tmpdir" ) );
        
        // checking the type safe getOption() method
        assertEquals( DumpDirOption.get( dumpDir ),
                      config.getOption( DumpDirOption.class ) );
        // checking string conversion
        assertEquals( dumpDir,
                      config.getOption( DumpDirOption.class ).getDirectory() );
        // checking the string based getProperty() method
        assertEquals( dumpDir.toString(),
                      config.getProperty( DumpDirOption.PROPERTY_NAME ) );
    }
    
    @Test
    public void testEvaluatorConfiguration() {
        // in this use case, the application already has the instance of the evaluator definition
        EvaluatorDefinition afterDef = new AfterEvaluatorDefinition();
        assertNotNull( afterDef );
        
        // creating the option and storing in a local var just to make test easier
        EvaluatorOption option = EvaluatorOption.get( "after", afterDef );
        
        // wiring the evaluator definition using the type safe method
        config.setOption( option );

        // checking the type safe getOption() method
        assertEquals( option,
                      config.getOption( EvaluatorOption.class, "after" ) );
        // checking string conversion
        assertEquals( "after",
                      config.getOption( EvaluatorOption.class, "after" ).getName() );
        assertEquals( afterDef,
                      config.getOption( EvaluatorOption.class, "after" ).getEvaluatorDefinition() );
        // checking the string based getProperty() method
        assertEquals( AfterEvaluatorDefinition.class.getName(),
                      config.getProperty( EvaluatorOption.PROPERTY_NAME+"after" ) );

        // wiring the evaluator definition using the string based setProperty() method
        config.setProperty( EvaluatorOption.PROPERTY_NAME+"before",
                            BeforeEvaluatorDefinition.class.getName() );
        
        BeforeEvaluatorDefinition beforeDef = new BeforeEvaluatorDefinition();
        // checking the type safe getOption() method
        assertEquals( EvaluatorOption.get( "before", beforeDef ),
                      config.getOption( EvaluatorOption.class, "before" ) );
        // checking string conversion
        assertEquals( "before",
                      config.getOption( EvaluatorOption.class, "before" ).getName() );
        assertEquals( beforeDef.getClass().getName(),
                      config.getOption( EvaluatorOption.class, "before" ).getEvaluatorDefinition().getClass().getName() );
        // checking the string based getProperty() method
        assertEquals( beforeDef.getClass().getName(),
                      config.getProperty( EvaluatorOption.PROPERTY_NAME+"before" ) );
    }
    
    @Test
    public void testProcessStringEscapesConfiguration() {
        // setting the process string escapes option using the type safe method
        config.setOption( ProcessStringEscapesOption.YES );

        // checking the type safe getOption() method
        assertEquals( ProcessStringEscapesOption.YES,
                      config.getOption( ProcessStringEscapesOption.class ) );
        // checking the string based getProperty() method
        assertEquals( "true",
                      config.getProperty( ProcessStringEscapesOption.PROPERTY_NAME ) );

        // setting the default dialect using the string based setProperty() method
        config.setProperty( ProcessStringEscapesOption.PROPERTY_NAME,
                            "false" );
        
        // checking the type safe getOption() method
        assertEquals( ProcessStringEscapesOption.NO,
                      config.getOption( ProcessStringEscapesOption.class ) );
        // checking the string based getProperty() method
        assertEquals( "false",
                      config.getProperty( ProcessStringEscapesOption.PROPERTY_NAME ) );
    }

    @Test
    public void testDefaultPackageNameConfiguration() {
        // setting the default dialect using the type safe method
        config.setOption( DefaultPackageNameOption.get( "org.drools.mvel.compiler.test" ) );

        // checking the type safe getOption() method
        assertEquals( DefaultPackageNameOption.get( "org.drools.mvel.compiler.test" ),
                      config.getOption( DefaultPackageNameOption.class ) );
        // checking string conversion
        assertEquals( "org.drools.mvel.compiler.test",
                      config.getOption( DefaultPackageNameOption.class ).getPackageName() );
        // checking the string based getProperty() method
        assertEquals( "org.drools.mvel.compiler.test",
                      config.getProperty( DefaultPackageNameOption.PROPERTY_NAME ) );

        // setting the default dialect using the string based setProperty() method
        config.setProperty( DefaultPackageNameOption.PROPERTY_NAME,
                            "org.drools" );
        
        // checking the type safe getOption() method
        assertEquals( DefaultPackageNameOption.get( "org.drools" ),
                      config.getOption( DefaultPackageNameOption.class ) );
        // checking string conversion
        assertEquals( "org.drools",
                      config.getOption( DefaultPackageNameOption.class ).getPackageName() );
        // checking the string based getProperty() method
        assertEquals( "org.drools",
                      config.getProperty( DefaultPackageNameOption.PROPERTY_NAME ) );
    }
    
    /**
     * an accumulate function implemented as an inner class
     */
    public static class InnerAccumulateFuncion implements AccumulateFunction {

        public void accumulate(Serializable context,
                               Object value) {
        }
        public Serializable createContext() {
            return null;
        }
        public Object getResult(Serializable context) throws Exception {
            return null;
        }
        public void init(Serializable context) throws Exception {
        }
        public void reverse(Serializable context,
                            Object value) throws Exception {
        }
        public boolean supportsReverse() {
            return false;
        }
        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            // TODO Auto-generated method stub

        }
        public void writeExternal(ObjectOutput out) throws IOException {
            // TODO Auto-generated method stub

        }
        @Override
        public Class< ? > getResultType() {
            return Object.class;
        }
    }

}
