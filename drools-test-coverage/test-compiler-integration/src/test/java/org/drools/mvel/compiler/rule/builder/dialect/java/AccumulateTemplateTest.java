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

package org.drools.mvel.compiler.rule.builder.dialect.java;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.base.ClassFieldAccessorCache;
import org.drools.core.base.ClassFieldAccessorStore;
import org.drools.core.base.ClassObjectType;
import org.drools.core.rule.Declaration;
import org.drools.core.spi.PatternExtractor;
import org.drools.core.util.StringUtils;
import org.drools.mvel.compiler.Cheese;
import org.drools.mvel.compiler.Person;
import org.drools.mvel.java.JavaRuleBuilderHelper;
import org.junit.Before;
import org.junit.Test;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.SimpleTemplateRegistry;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRegistry;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.res.Node;

public class AccumulateTemplateTest {

    ClassFieldAccessorStore store = new ClassFieldAccessorStore();

    @Before
    public void setUp() throws Exception {
        store.setClassFieldAccessorCache( new ClassFieldAccessorCache( Thread.currentThread().getContextClassLoader() ) );
        store.setEagerWire( true );
    }

    @Test
    public void testMethodGeneration() {
        final String className = "accumulate0";

        final String[] declarationTypes = new String[]{"String", "int"};
        final Declaration[] declarations = new Declaration[]{new Declaration( "name",
                                                                              null,
                                                                              null ), new Declaration( "age",
                                                                                                       null,
                                                                                                       null )};
        final Declaration[] inner = new Declaration[]{new Declaration( "cheese",
                                                                       new PatternExtractor( new ClassObjectType( Cheese.class ) ),
                                                                       null ), new Declaration( "price",
                                                                                                store.getReader( Cheese.class,
                                                                                                                 "price" ),
                                                                                                null )};
        final String[] globals = new String[]{"aGlobal", "anotherGlobal"};
        final List globalTypes = Arrays.asList( new String[]{"String", "String"} );

        final Map map = new HashMap();

        map.put( "className",
                 StringUtils.ucFirst( className ) );

        map.put( "instanceName",
                 className );

        map.put( "package",
                 "org.drools" );

        map.put( "ruleClassName",
                 "Rule0" );

        map.put( "invokerClassName",
                 "Rule0" + StringUtils.ucFirst( className ) + "Invoker" );

        map.put( "declarations",
                 declarations );

        map.put( "declarationTypes",
                 declarationTypes );

        map.put( "globals",
                 globals );

        map.put( "globalTypes",
                 globalTypes );

        map.put( "innerDeclarations",
                 inner );

        map.put( "attributes",
                 new String[]{"x"} );
        map.put( "attributesTypes",
                 new String[]{"int"} );
        map.put( "initCode",
                 "x = 0;" );
        map.put( "actionCode",
                 "x += 1;" );
        map.put( "reverseCode",
                 "x -= 1;" );
        map.put( "resultCode",
                 "x + 10" );
        map.put( "supportsReverse",
                 "true" );

        map.put( "resultType",
                 Integer.class );

        map.put( "hashCode",
                 new Integer( 10 ) );

        TemplateRegistry registry = getRuleTemplateRegistry();

        Object method = TemplateRuntime.execute( registry.getNamedTemplate( "accumulateInnerClass" ),
                                                 null,
                                                 new MapVariableResolverFactory( map ),
                                                 registry );

        //System.out.println( method );
    }

    @Test
    public void testInvokerGenerationSinglePattern() {
        final String className = "accumulate0";

        final String[] declarationTypes = new String[]{"String", "int"};
        final Declaration[] declarations = new Declaration[]{new Declaration( "name",
                                                                              store.getReader( Person.class,
                                                                                               "name" ),
                                                                              null ), new Declaration( "age",
                                                                                                       store.getReader( Person.class,
                                                                                                                        "age" ),
                                                                                                       null )};
        final Declaration[] inner = new Declaration[]{new Declaration( "cheese",
                                                                       new PatternExtractor( new ClassObjectType( Cheese.class ) ),
                                                                       null ), new Declaration( "price",
                                                                                                store.getReader( Cheese.class,
                                                                                                                 "price" ),
                                                                                                null )};
        final String[] globals = new String[]{"aGlobal", "anotherGlobal"};
        final List globalTypes = Arrays.asList( new String[]{"String", "String"} );

        final Map map = new HashMap();

        map.put( "className",
                 StringUtils.ucFirst( className ) );

        map.put( "instanceName",
                 className );

        map.put( "package",
                 "org.drools" );

        map.put( "ruleClassName",
                 "Rule0" );

        map.put( "invokerClassName",
                 "Rule0" + StringUtils.ucFirst( className ) + "Invoker" );

        map.put( "declarations",
                 declarations );

        map.put( "declarationTypes",
                 declarationTypes );

        map.put( "globals",
                 globals );

        map.put( "globalTypes",
                 globalTypes );

        map.put( "innerDeclarations",
                 inner );

        map.put( "attributes",
                 new Attribute[]{new Attribute( "int",
                                                "x" )} );
        map.put( "initCode",
                 "x = 0;" );
        map.put( "actionCode",
                 "x += 1;" );
        map.put( "reverseCode",
                 "" );
        map.put( "resultCode",
                 "x + 10" );

        map.put( "supportsReverse",
                 "false" );

        map.put( "resultType",
                 Integer.class );

        map.put( "hashCode",
                 new Integer( 10 ) );
        map.put( "isMultiPattern",
                 Boolean.FALSE );

        TemplateRegistry registry = getInvokerTemplateRegistry();
        Object method = TemplateRuntime.execute( registry.getNamedTemplate( "accumulateInvoker" ),
                                                 null,
                                                 new MapVariableResolverFactory( map ),
                                                 registry );

        //System.out.println( method );
    }

    @Test
    public void testInvokerGenerationMultiPattern() {
        final String className = "accumulate0";

        final String[] declarationTypes = new String[]{"String", "int"};
        final Declaration[] declarations = new Declaration[]{new Declaration( "name",
                                                                              store.getReader( Person.class,
                                                                                               "name" ),
                                                                              null ), new Declaration( "age",
                                                                                                       store.getReader( Person.class,
                                                                                                                        "age" ),
                                                                                                       null )};
        final Declaration[] inner = new Declaration[]{new Declaration( "$cheese",
                                                                       new PatternExtractor( new ClassObjectType( Cheese.class ) ),
                                                                       null ), new Declaration( "$person",
                                                                                                new PatternExtractor( new ClassObjectType( Person.class ) ),
                                                                                                null )};
        final String[] globals = new String[]{"aGlobal", "anotherGlobal"};
        final List globalTypes = Arrays.asList( new String[]{"String", "String"} );

        final Map map = new HashMap();

        map.put( "className",
                 StringUtils.ucFirst( className ) );

        map.put( "instanceName",
                 className );

        map.put( "package",
                 "org.drools" );

        map.put( "ruleClassName",
                 "Rule0" );

        map.put( "invokerClassName",
                 "Rule0" + StringUtils.ucFirst( className ) + "Invoker" );

        map.put( "declarations",
                 declarations );

        map.put( "declarationTypes",
                 declarationTypes );

        map.put( "globals",
                 globals );

        map.put( "globalTypes",
                 globalTypes );

        map.put( "innerDeclarations",
                 inner );

        map.put( "attributes",
                 new Attribute[]{new Attribute( "int",
                                                "x" )} );
        map.put( "initCode",
                 "x = 0;" );
        map.put( "actionCode",
                 "x += 1;" );
        map.put( "reverseCode",
                 "" );
        map.put( "resultCode",
                 "x + 10" );

        map.put( "supportsReverse",
                 "false" );

        map.put( "resultType",
                 Integer.class );

        map.put( "hashCode",
                 new Integer( 10 ) );
        map.put( "isMultiPattern",
                 Boolean.TRUE );

        TemplateRegistry registry = getInvokerTemplateRegistry();
        Object method = TemplateRuntime.execute( registry.getNamedTemplate( "accumulateInvoker" ),
                                                 null,
                                                 new MapVariableResolverFactory( map ),
                                                 registry );

        //System.out.println( method );
    }

    private TemplateRegistry getRuleTemplateRegistry() {
        TemplateRegistry ruleRegistry = new SimpleTemplateRegistry();
        CompiledTemplate compiled = TemplateCompiler.compileTemplate( JavaRuleBuilderHelper.class.getResourceAsStream( "javaRule.mvel" ),
                                                                      (Map<String, Class<? extends Node>>) null );
        TemplateRuntime.execute( compiled,
                                 null,
                                 ruleRegistry );

        return ruleRegistry;
    }

    private TemplateRegistry getInvokerTemplateRegistry() {
        TemplateRegistry invokerRegistry = new SimpleTemplateRegistry();
        CompiledTemplate compiled = TemplateCompiler.compileTemplate( JavaRuleBuilderHelper.class.getResourceAsStream( "javaInvokers.mvel" ),
                                                                      (Map<String, Class<? extends Node>>) null );
        TemplateRuntime.execute( compiled,
                                 null,
                                 invokerRegistry );

        return invokerRegistry;
    }

    public static class Attribute {
        private String type;
        private String name;

        public Attribute(String type,
                         String name) {
            this.type = type;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

}
