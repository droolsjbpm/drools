/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
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

package org.drools.traits.core.factmodel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.drools.core.base.ClassFieldAccessorCache;
import org.drools.core.base.ClassFieldAccessorStore;
import org.drools.core.factmodel.ClassBuilder;
import org.drools.core.factmodel.ClassDefinition;
import org.drools.core.factmodel.FieldDefinition;
import org.drools.core.rule.JavaDialectRuntimeData;
import org.drools.core.rule.JavaDialectRuntimeData.PackageClassLoader;
import org.drools.reflective.classloader.ProjectClassLoader;
import org.junit.Before;
import org.junit.Test;

import static org.drools.core.util.ClassUtils.convertClassToResourcePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClassBuilderTest {

    ClassFieldAccessorStore store = new ClassFieldAccessorStore();
    ClassLoader classLoader;
    JavaDialectRuntimeData data;
    
    @Before
    public void setUp() throws Exception {
        data = new JavaDialectRuntimeData();
    }

    private Class build(ClassBuilder builder, ClassDefinition classDef) throws Exception {
        classLoader = new PackageClassLoader(data, ProjectClassLoader.createProjectClassLoader());
        byte[] d = builder.buildClass( classDef, classLoader);
                     
        data.write( convertClassToResourcePath(classDef.getClassName()), d );

        
        this.store = new ClassFieldAccessorStore();
        store.setClassFieldAccessorCache( new ClassFieldAccessorCache( classLoader ) );
        store.setEagerWire( true );
        
        Class clazz = classLoader.loadClass( classDef.getClassName() );
        classDef.setDefinedClass( clazz );
        
        return clazz;
        
    }
    
    /*
     * Test method for 'org.drools.core.common.asm.ClassBuilder.buildClass(ClassDefinition)'
     */
    @Test
    public void testBuildClass() {
        try {
            ClassBuilder builder = new TraitClassBuilderFactory().getBeanClassBuilder();

            ClassDefinition classDef = new ClassDefinition( "org.drools.TestClass1",
                                                            null,
                                                            new String[]{"java.io.Serializable"} );
            FieldDefinition intDef = new FieldDefinition("intAttr",
                                                         "int" );

            FieldDefinition stringDef = new FieldDefinition( "stringAttr",
                                                             "java.lang.String" );//"java.lang.String" );
            classDef.addField( intDef );
            classDef.addField( stringDef );
            
            Class clazz = build(builder, classDef);

            
            
            intDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                            intDef.getName() ) );
            stringDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                               stringDef.getName() ) );

            byte[] d = builder.buildClass( classDef, classLoader );

            assertSame( "Returned class should be the same",
                               clazz,
                               classDef.getDefinedClass() );
            assertEquals( "Class name should be equal",
                                 classDef.getClassName(),
                                 clazz.getName() );

            Serializable instance = (Serializable) clazz.newInstance();

            String stringValue = "Atributo String ok";
            stringDef.setValue( instance,
                                stringValue );
            assertEquals( "Attribute should have been correctly set",
                                 stringValue,
                                 stringDef.getValue( instance ) );

            int intValue = 50;
            intDef.setValue( instance,
                             new Integer( intValue ) );
            assertEquals( "Attribute should have been correctly set",
                                 intValue,
                                 ((Integer) intDef.getValue( instance )).intValue() );

            // testing class rebuilding
            clazz = build(builder, classDef);

        } catch ( Exception e ) {
            e.printStackTrace();
            fail( "Error creating class" );
        }
    }

    /**
     * how to write to a jar.
     * @param data
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void writeJar(byte[] data) throws FileNotFoundException,
                                      IOException {
        FileOutputStream out = new FileOutputStream( new File( "/Users/michaelneale/edson.jar" ) );
        JarOutputStream jout = new JarOutputStream( out );
        JarEntry je = new JarEntry( "br/com/auster/TestClass1.class" );
        jout.putNextEntry( je );
        jout.write( data );
        jout.closeEntry();
        jout.close();
    }

    @Test
    public void testEquals() {
        try {
            ClassBuilder builder = new TraitClassBuilderFactory().getBeanClassBuilder();

            ClassDefinition classDef = new ClassDefinition( "org.drools.TestClass2",
                                                            null,
                                                            new String[]{} );
            FieldDefinition long1Def = new FieldDefinition( "longAttr1",
                                                            "long",
                                                            true );
            FieldDefinition long2Def = new FieldDefinition( "longAttr2",
                                                            "long",
                                                            true );
            FieldDefinition doubleDef = new FieldDefinition( "doubleAttr",
                                                             "double",
                                                             true );
            FieldDefinition intDef = new FieldDefinition( "intAttr",
                                                          "int",
                                                          true );
            FieldDefinition strDef = new FieldDefinition( "stringAttr",
                                                          "java.lang.String",
                                                          true );
            FieldDefinition dateDef = new FieldDefinition( "dateAttr",
                                                           "java.util.Date",
                                                           true );
            FieldDefinition str2Def = new FieldDefinition( "stringAttr2",
                                                           "java.lang.String" );
            classDef.addField( long1Def );
            classDef.addField( long2Def );
            classDef.addField( doubleDef );
            classDef.addField( intDef );
            classDef.addField( strDef );
            classDef.addField( dateDef );
            classDef.addField( str2Def );

            Class clazz = build(builder, classDef);
            long1Def.setReadWriteAccessor( store.getAccessor( clazz,
                                                              long1Def.getName() ) );
            long2Def.setReadWriteAccessor( store.getAccessor( clazz,
                                                              long2Def.getName() ) );
            doubleDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                               doubleDef.getName() ) );
            intDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                            intDef.getName() ) );
            strDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                            strDef.getName( ) ) );
            dateDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                             dateDef.getName() ) );
            str2Def.setReadWriteAccessor( store.getAccessor( clazz,
                                                             str2Def.getName() ) );

            Object x = clazz.newInstance();
            Object y = clazz.newInstance();

            long1Def.setValue( x,
                               new Long( 20 ) );
            long2Def.setValue( x,
                               new Long( 30 ) );
            doubleDef.setValue( x,
                                new Double( 50.0 ) );
            intDef.setValue( x,
                             new Integer( 10 ) );
            strDef.setValue( x,
                             "abc" );
            dateDef.setValue( x,
                              new Date( 1000 ) );
            str2Def.setValue( x,
                              "instance1" );

            long1Def.setValue( y,
                               new Long( 20 ) );
            long2Def.setValue( y,
                               new Long( 30 ) );
            doubleDef.setValue( y,
                                new Double( 50.0 ) );
            intDef.setValue( y,
                             new Integer( 10 ) );
            strDef.setValue( y,
                             "abc" );
            dateDef.setValue( y,
                              new Date( 1000 ) );
            str2Def.setValue( y,
                              "instance2" );

            Object o = new Object();

            assertTrue( x.equals( x ) );
            assertFalse( x.equals( null ) );
            assertFalse( x.equals( o ) );

            assertTrue( x.equals( y ) );

            intDef.setValue( y,
                             new Integer( 1 ) );
            assertFalse( x.equals( y ) );

            intDef.setValue( y,
                             new Integer( 10 ) );
            strDef.setValue( y,
                             "xyz" );
            assertFalse( x.equals( y ) );

            strDef.setValue( y,
                             null );
            assertFalse( x.equals( y ) );

            strDef.setValue( y,
                             "abc" );
            dateDef.setValue( y,
                              new Date( 1 ) );
            assertFalse( x.equals( y ) );

            dateDef.setValue( y,
                              null );
            assertFalse( x.equals( y ) );

        } catch ( Exception e ) {
            e.printStackTrace();
            fail( "Exception not expected" );
        }
    }

    @Test
    public void testHashCode() {
        try {
            ClassBuilder builder = new TraitClassBuilderFactory().getBeanClassBuilder();

            ClassDefinition classDef = new ClassDefinition( "org.drools.TestClass3",
                                                            null,
                                                            new String[]{} );
            FieldDefinition intDef = new FieldDefinition( "intAttr",
                                                          "int",
                                                          true );
            FieldDefinition strDef = new FieldDefinition( "stringAttr",
                                                          "java.lang.String",
                                                          false );
            classDef.addField( intDef );
            classDef.addField( strDef );

            Class clazz = build(builder, classDef);
            intDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                            intDef.getName() ) );
            strDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                            strDef.getName() ) );

            Object x = clazz.newInstance();

            intDef.setValue( x,
                             new Integer( 10 ) );
            strDef.setValue( x,
                             "abc" );

            assertEquals( "Wrong hashcode calculation",
                                 31 + 10,
                                 x.hashCode() );
            assertEquals( "Wrong hashcode calculation",
                                 x.hashCode(),
                                 x.hashCode() );

        } catch ( Exception e ) {
            e.printStackTrace();
            fail( "Exception not expected" );
        }
    }

    @Test
    public void testToString() {
        try {
            ClassBuilder builder = new TraitClassBuilderFactory().getBeanClassBuilder();

            ClassDefinition classDef = new ClassDefinition( "org.drools.TestClass4",
                                                            null,
                                                            new String[]{} );
            FieldDefinition long1Def = new FieldDefinition( "longAttr1",
                                                            "long",
                                                            true );
            FieldDefinition long2Def = new FieldDefinition( "longAttr2",
                                                            "long",
                                                            true );
            FieldDefinition doubleDef = new FieldDefinition( "doubleAttr",
                                                             "double",
                                                             true );
            FieldDefinition intDef = new FieldDefinition( "intAttr",
                                                          "int",
                                                          true );
            FieldDefinition strDef = new FieldDefinition( "stringAttr",
                                                          "java.lang.String",
                                                          true );
            FieldDefinition dateDef = new FieldDefinition( "dateAttr",
                                                           "java.util.Date",
                                                           true );
            FieldDefinition str2Def = new FieldDefinition( "stringAttr2",
                                                           "java.lang.String" );
            classDef.addField( long1Def );
            classDef.addField( long2Def );
            classDef.addField( doubleDef );
            classDef.addField( intDef );
            classDef.addField( strDef );
            classDef.addField( dateDef );
            classDef.addField( str2Def );

            Class clazz = build(builder, classDef);
            long1Def.setReadWriteAccessor( store.getAccessor( clazz,
                                                              long1Def.getName() ) );
            long2Def.setReadWriteAccessor( store.getAccessor( clazz,
                                                              long2Def.getName() ) );
            doubleDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                               doubleDef.getName() ) );
            intDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                            intDef.getName() ) );
            strDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                            strDef.getName() ) );
            dateDef.setReadWriteAccessor( store.getAccessor( clazz,
                                                             dateDef.getName() ) );
            str2Def.setReadWriteAccessor( store.getAccessor( clazz,
                                                             str2Def.getName() ) );

            Object x = clazz.newInstance();

            long1Def.setValue( x,
                               new Long( 20 ) );
            long2Def.setValue( x,
                               new Long( 30 ) );
            doubleDef.setValue( x,
                                new Double( 50.0 ) );
            intDef.setValue( x,
                             new Integer( 10 ) );
            strDef.setValue( x,
                             "abc" );
            dateDef.setValue( x,
                              new Date( 1000 ) );
            str2Def.setValue( x,
                              "instance1" );

            String result = x.toString();

            assertTrue( result.contains( long1Def.getName() ) );
            assertTrue( result.contains( long2Def.getName() ) );
            assertTrue( result.contains( doubleDef.getName() ) );
            assertTrue( result.contains( intDef.getName() ) );
            assertTrue( result.contains( strDef.getName() ) );
            assertTrue( result.contains( dateDef.getName() ) );
            assertTrue( result.contains( str2Def.getName() ) );

        } catch ( Exception e ) {
            e.printStackTrace();
            fail( "Exception not expected" );
        }

    }

    @Test
    public void testConstructorWithFields() {
        try {
            ClassBuilder builder = new TraitClassBuilderFactory().getBeanClassBuilder();

            ClassDefinition classDef = new ClassDefinition( "org.drools.TestClass5",
                                                            null,
                                                            new String[]{} );

            String[] types = new String[]{"byte", "short", "int", "long", "float", "double", "char", "java.lang.String", "boolean"};
            FieldDefinition[] fields = new FieldDefinition[types.length];
            for ( int i = 0; i < types.length; i++ ) {
                String attrName = types[i].substring( types[i].lastIndexOf( '.' ) + 1 );
                attrName = attrName.substring( 0,
                                               1 ).toLowerCase() + attrName.substring( 1 ) + "Attr";
                fields[i] = new FieldDefinition( attrName, // attr name
                                                 types[i], // attr type
                                                 i % 2 == 0 ); // half of them are key
                classDef.addField( fields[i] );
            }

            Class clazz = build(builder, classDef);

            for ( FieldDefinition field : fields ) {
                field.setReadWriteAccessor( store.getAccessor( clazz,
                                                               field.getName() ) );
            }

            Constructor< ? >[] cons = clazz.getConstructors();

            assertEquals( 3,
                                 cons.length );
            for ( Constructor< ? > c : cons ) {
                Class< ? >[] ptypes = c.getParameterTypes();
                if ( ptypes.length == 0 ) {
                    // default constructor
                } else if ( ptypes.length == fields.length ) {
                    // constructor with fields
                    for ( int i = 0; i < ptypes.length; i++ ) {
                        if ( !ptypes[i].equals( fields[i].getType() ) ) {
                            fail( "Wrong parameter in constructor. index=" + i + " expected=" + fields[i].getType() + " found=" + ptypes[i] );
                        }
                    }

                    // test actual invocation
                    Object instance = c.newInstance( (byte) 1,
                                                     (short) 2,
                                                     3,
                                                     4l,
                                                     5.0f,
                                                     6.0d,
                                                     'a',
                                                     "xyz",
                                                     true );

                    assertEquals( (byte) 1,
                                  fields[0].getValue( instance ) );
                    assertEquals( (short) 2,
                                  fields[1].getValue( instance ) );
                    assertEquals( 3,
                                  fields[2].getValue( instance ) );
                    assertEquals( 4l,
                                  fields[3].getValue( instance ) );
                    assertEquals( 5.0f,
                                  fields[4].getValue( instance ) );
                    assertEquals( 6.0d,
                                  fields[5].getValue( instance ) );
                    assertEquals( 'a',
                                  fields[6].getValue( instance ) );
                    assertEquals( "xyz",
                                  fields[7].getValue( instance ) );
                    assertEquals( true,
                                  fields[8].getValue( instance ) );
                } else if ( ptypes.length == ( fields.length / 2 +1 ) ) { // as defined in the beginning of the test
                    // constructor with key fields
                    int i = 0;
                    for ( FieldDefinition field : fields ) {
                        if ( field.isKey() && !ptypes[i++].equals( field.getType() ) ) {
                            fail( "Wrong parameter in constructor. index=" + i + " expected=" + field.getType() + " found=" + ptypes[i] );
                        }
                    }
                    // test actual invocation
                    Object instance = c.newInstance( (byte) 1,
                                                     3,
                                                     5.0f,
                                                     'a',
                                                     true );

                    assertEquals( (byte) 1,
                                  fields[0].getValue( instance ) );
                    assertEquals( 3,
                                  fields[2].getValue( instance ) );
                    assertEquals( 5.0f,
                                  fields[4].getValue( instance ) );
                    assertEquals( 'a',
                                  fields[6].getValue( instance ) );
                    assertEquals( true,
                                  fields[8].getValue( instance ) );
                    
                } else {
                    fail( "Unexpected constructor: " + c.toString() );
                }
            }

        } catch ( Exception e ) {
            e.printStackTrace();
            fail( "Unexpected Exception: " + e.getMessage() );
        }

    }
    
    @Test
    public void testGetResourcesJBRULES3122() {
        try {
            ClassBuilder builder = new TraitClassBuilderFactory().getBeanClassBuilder();

            ClassDefinition classDef = new ClassDefinition("org.drools.TestClass4", null, new String[] {});
            Class clazz = build(builder, classDef);
            ClassLoader cl = clazz.getClassLoader();

            // We expect normal classloader stuff to work
            assertFalse(cl.getResources("not-there.txt").hasMoreElements());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception not expected: " + e.getMessage());
        }
    }
}
