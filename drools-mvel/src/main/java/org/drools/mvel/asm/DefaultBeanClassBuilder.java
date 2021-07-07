/*
 * Copyright (c) 2020. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.mvel.asm;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.core.factmodel.AnnotationDefinition;
import org.drools.core.factmodel.BeanClassBuilder;
import org.drools.core.factmodel.BuildUtils;
import org.drools.core.factmodel.ClassDefinition;
import org.drools.core.factmodel.FieldDefinition;
import org.drools.core.factmodel.GeneratedFact;
import org.drools.core.factmodel.traits.Thing;
import org.drools.core.factmodel.traits.TraitFieldTMS;
import org.drools.core.factmodel.traits.TraitTypeMapConstants;
import org.drools.core.factmodel.traits.TraitableBean;
import org.drools.core.phreak.ReactiveObject;
import org.kie.api.definition.type.FactField;
import org.mvel2.asm.AnnotationVisitor;
import org.mvel2.asm.ClassVisitor;
import org.mvel2.asm.ClassWriter;
import org.mvel2.asm.FieldVisitor;
import org.mvel2.asm.Label;
import org.mvel2.asm.MethodVisitor;
import org.mvel2.asm.Opcodes;
import org.mvel2.asm.Type;

import static org.drools.mvel.asm.ClassGenerator.createClassWriter;

/**
 * A builder to dynamically build simple Javabean(TM) classes
 */
public class DefaultBeanClassBuilder implements Opcodes, BeanClassBuilder, Serializable {
    protected boolean     debug  = false;

    public DefaultBeanClassBuilder() {
        this( "true".equalsIgnoreCase( System.getProperty( "org.drools.classbuilder.debug" ) ) );
    }

    public DefaultBeanClassBuilder(final boolean debug) {
        this.debug = debug;
    }


    /**
     * Dynamically builds, defines and loads a class based on the given class definition
     *
     * @param classDef the class definition object structure
     *
     * @return the Class instance for the given class definition
     *
     * @throws IOException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     */
    public byte[] buildClass( ClassDefinition classDef, ClassLoader classLoader ) throws IOException,
            SecurityException,
            IllegalArgumentException,
            ClassNotFoundException,
            NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException,
            InstantiationException,
            NoSuchFieldException {

        ClassWriter cw = this.buildClassHeader( classLoader, classDef );

        buildMetaData( cw, classDef );
        buildFields( cw, classDef );

        if ( classDef.isTraitable() ) {
            buildDynamicPropertyMap( cw, classDef );
            buildTraitMap( cw, classDef );
            buildFieldTMS( cw, classDef );
        }

        buildConstructors( cw, classDef );
        buildGettersAndSetters( cw, classDef );
        buildEqualityMethods( cw, classDef );
        buildToString( cw, classDef );

        if ( classDef.isTraitable() ) {
            // must guarantee serialization order when enhancing fields are present
            buildSerializationMethods(cw, classDef);
        }

        if ( classDef.isReactive() ) {
            implementReactivity( cw, classDef );
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    private void buildSerializationMethods(ClassWriter cw, ClassDefinition classDef) {
        MethodVisitor mv;
        {
            mv = cw.visitMethod(ACC_PUBLIC, "writeExternal", "(Ljava/io/ObjectOutput;)V", null, new String[]{"java/io/IOException"});
            mv.visitCode();

            for (  FieldDefinition field : classDef.getFieldsDefinitions() ) {

                mv.visitVarInsn( ALOAD, 1 );
                mv.visitVarInsn( ALOAD, 0 );
                visitFieldOrGetter( mv, classDef, field );
                mv.visitMethodInsn( INVOKEINTERFACE,
                                    "java/io/ObjectOutput",
                                    BuildUtils.serializationWriterName( field.getTypeName() ),
                                    "(" + ( BuildUtils.isPrimitive( field.getTypeName() ) ?
                                            BuildUtils.getTypeDescriptor( BuildUtils.serializationType( field.getTypeName() ) ) :
                                            "Ljava/lang/Object;" ) + ")V");

            }

            if ( classDef.isTraitable() ) {
                mv.visitVarInsn( ALOAD, 1 );
                mv.visitVarInsn( ALOAD, 0 );
                mv.visitFieldInsn( Opcodes.GETFIELD,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   TraitableBean.MAP_FIELD_NAME,
                                   Type.getDescriptor( Map.class ) );
                mv.visitMethodInsn( INVOKEINTERFACE,
                                    "java/io/ObjectOutput",
                                    "writeObject",
                                    "(Ljava/lang/Object;)V" );

                mv.visitVarInsn( ALOAD, 1 );
                mv.visitVarInsn( ALOAD, 0 );
                mv.visitFieldInsn( Opcodes.GETFIELD,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   TraitableBean.TRAITSET_FIELD_NAME,
                                   Type.getDescriptor( Map.class ) );
                mv.visitMethodInsn( INVOKEINTERFACE,
                                    "java/io/ObjectOutput",
                                    "writeObject",
                                    "(Ljava/lang/Object;)V" );

                if ( classDef.isFullTraiting() ) {
                    mv.visitVarInsn( ALOAD, 1 );
                    mv.visitVarInsn( ALOAD, 0 );
                    mv.visitFieldInsn( Opcodes.GETFIELD,
                                       BuildUtils.getInternalType( classDef.getClassName() ),
                                       TraitableBean.FIELDTMS_FIELD_NAME,
                                       Type.getDescriptor( TraitFieldTMS.class ) );
                    mv.visitMethodInsn( INVOKEINTERFACE,
                                        "java/io/ObjectOutput",
                                        "writeObject",
                                        "(Ljava/lang/Object;)V" );
                }
            }

            mv.visitInsn(RETURN);
            mv.visitMaxs( 0, 0 );
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "readExternal", "(Ljava/io/ObjectInput;)V", null, new String[]{"java/io/IOException", "java/lang/ClassNotFoundException"});
            mv.visitCode();

            for (  FieldDefinition field : classDef.getFieldsDefinitions() ) {
                mv.visitVarInsn( ALOAD, 0 );
                mv.visitVarInsn( ALOAD, 1 );
                mv.visitMethodInsn( INVOKEINTERFACE,
                                    "java/io/ObjectInput",
                                    BuildUtils.serializationReaderName( field.getTypeName() ),
                                    "()" + ( BuildUtils.isPrimitive( field.getTypeName() ) ?
                                            BuildUtils.getTypeDescriptor( field.getTypeName() ) :
                                            "Ljava/lang/Object;" ) );
                if  ( !  BuildUtils.isPrimitive( field.getTypeName() ) ) {
                    mv.visitTypeInsn( CHECKCAST, BuildUtils.getInternalType( field.getTypeName() ) );
                }
                mv.visitMethodInsn( INVOKEVIRTUAL,
                                    BuildUtils.getInternalType( classDef.getName() ),
                                    BuildUtils.setterName( field.getName()),
                                    "(" + BuildUtils.getTypeDescriptor( field.getTypeName() )+ ")V");
            }

            if ( classDef.isTraitable() ) {
                mv.visitVarInsn( ALOAD, 0 );
                mv.visitVarInsn( ALOAD, 1 );
                mv.visitMethodInsn( INVOKEINTERFACE,
                                    "java/io/ObjectInput",
                                    "readObject",
                                    "()Ljava/lang/Object;");
                mv.visitTypeInsn( CHECKCAST, "java/util/Map" );
                mv.visitFieldInsn( Opcodes.PUTFIELD,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   TraitableBean.MAP_FIELD_NAME,
                                   Type.getDescriptor( Map.class ) );
//
                mv.visitVarInsn( ALOAD, 0 );
                mv.visitVarInsn( ALOAD, 1 );
                mv.visitMethodInsn( INVOKEINTERFACE,
                                    "java/io/ObjectInput",
                                    "readObject",
                                    "()Ljava/lang/Object;");
                mv.visitTypeInsn( CHECKCAST, "java/util/Map" );
                mv.visitFieldInsn( Opcodes.PUTFIELD,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   TraitableBean.TRAITSET_FIELD_NAME,
                                   Type.getDescriptor( Map.class ) );

                if ( classDef.isFullTraiting() ) {
                    mv.visitVarInsn( ALOAD, 0 );
                    mv.visitVarInsn( ALOAD, 1 );
                    mv.visitMethodInsn( INVOKEINTERFACE,
                                        "java/io/ObjectInput",
                                        "readObject",
                                        "()Ljava/lang/Object;");
                    mv.visitTypeInsn( CHECKCAST, Type.getInternalName( TraitFieldTMS.class ) );
                    mv.visitFieldInsn( Opcodes.PUTFIELD,
                                       BuildUtils.getInternalType( classDef.getClassName() ),
                                       TraitableBean.FIELDTMS_FIELD_NAME,
                                       Type.getDescriptor( TraitFieldTMS.class ) );
                }
            }

            mv.visitInsn( RETURN );
            mv.visitMaxs( 0, 0 );
            mv.visitEnd();
        }

    }

    protected void buildGettersAndSetters(ClassWriter cw, ClassDefinition classDef) {
        // Building methods
        for ( FieldDefinition fieldDef : classDef.getFieldsDefinitions() ) {
            if (! fieldDef.isInherited() || fieldDef.hasOverride()) {
                this.buildGetMethod( cw,
                                     classDef,
                                     fieldDef );
                this.buildSetMethod( cw,
                                     classDef,
                                     fieldDef );
            }
        }
    }

    protected void buildEqualityMethods(ClassWriter cw, ClassDefinition classDef) {
        boolean hasKey = false;
        for ( FieldDefinition fld : classDef.getFieldsDefinitions() ) {
            if ( fld.isKey() ) {
                hasKey = true;
                break;
            }
        }

        if (hasKey) {
            this.buildEquals( cw,
                              classDef );
            this.buildHashCode( cw,
                                classDef );
        }
    }

    protected void buildMetaData(ClassWriter cw, ClassDefinition classDef) {
        Object serialVersionUID = classDef.getMetaData("serialVersionUID");
        if (serialVersionUID != null) {
            FieldVisitor fv = cw.visitField( Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL,
                                             "serialVersionUID",
                                             BuildUtils.getTypeDescriptor( "long" ),
                                             null,
                                             Long.parseLong( serialVersionUID.toString() ) );
            fv.visitEnd();
        }
    }

    protected void buildFields(ClassWriter cw, ClassDefinition classDef) {
        // Building fields
        for ( FieldDefinition fieldDef : classDef.getFieldsDefinitions() ) {
            if (! fieldDef.isInherited())
                this.buildField( cw, fieldDef );
        }
    }

    private void implementReactivity(ClassWriter cw, ClassDefinition classDef) {
        final String LEFT_TUPLES_FIELD_NAME = "_lts";
        final String TYPE_NAME = BuildUtils.getInternalType( classDef.getClassName() );

        FieldVisitor fv;
        /*
            private Collection<Tuple> _lts;
         */
        {
            fv = cw.visitField( ACC_PRIVATE, LEFT_TUPLES_FIELD_NAME, "Ljava/util/Collection;", "Ljava/util/Collection<Lorg/drools/core/spi/Tuple;>;", null );
            fv.visitEnd();
        }

        MethodVisitor mv;
        /*
            public void addLeftTuple(Tuple leftTuple) {
                if (_lts == null) {
                    _lts = new HashSet<Tuple>();
                }
                _lts.add(leftTuple);
            }
         */
        {
            mv = cw.visitMethod( ACC_PUBLIC, "addLeftTuple", "(Lorg/drools/core/spi/Tuple;)V", null, null );
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel( l0 );
            mv.visitLineNumber( 30, l0 );
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitFieldInsn( GETFIELD, TYPE_NAME, LEFT_TUPLES_FIELD_NAME, "Ljava/util/Collection;" );
            Label l1 = new Label();
            mv.visitJumpInsn( IFNONNULL, l1 );
            Label l2 = new Label();
            mv.visitLabel( l2 );
            mv.visitLineNumber( 31, l2 );
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitTypeInsn( NEW, "java/util/HashSet" );
            mv.visitInsn( DUP );
            mv.visitMethodInsn( INVOKESPECIAL, "java/util/HashSet", "<init>", "()V", false );
            mv.visitFieldInsn( PUTFIELD, TYPE_NAME, LEFT_TUPLES_FIELD_NAME, "Ljava/util/Collection;" );
            mv.visitLabel( l1 );
            mv.visitLineNumber( 33, l1 );
            mv.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitFieldInsn( GETFIELD, TYPE_NAME, LEFT_TUPLES_FIELD_NAME, "Ljava/util/Collection;" );
            mv.visitVarInsn( ALOAD, 1 );
            mv.visitMethodInsn( INVOKEINTERFACE, "java/util/Collection", "add", "(Ljava/lang/Object;)Z", true );
            mv.visitInsn( POP );
            Label l3 = new Label();
            mv.visitLabel( l3 );
            mv.visitLineNumber( 34, l3 );
            mv.visitInsn( RETURN );
            Label l4 = new Label();
            mv.visitLabel( l4 );
            mv.visitLocalVariable( "this", "L" + TYPE_NAME + ";", null, l0, l4, 0 );
            mv.visitLocalVariable( "leftTuple", "Lorg/drools/core/spi/Tuple;", null, l0, l4, 1 );
            mv.visitMaxs( 3, 2 );
            mv.visitEnd();
        }
        /*
            public Collection<Tuple> getLeftTuples() {
                return _lts != null ? _lts : Collections.emptyList();
            }
         */
        {
            mv = cw.visitMethod( ACC_PUBLIC, "getLeftTuples", "()Ljava/util/Collection;", "()Ljava/util/Collection<Lorg/drools/core/spi/Tuple;>;", null );
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel( l0 );
            mv.visitLineNumber( 37, l0 );
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitFieldInsn( GETFIELD, TYPE_NAME, LEFT_TUPLES_FIELD_NAME, "Ljava/util/Collection;");
            Label l1 = new Label();
            mv.visitJumpInsn( IFNULL, l1 );
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitFieldInsn( GETFIELD, TYPE_NAME, LEFT_TUPLES_FIELD_NAME, "Ljava/util/Collection;" );
            Label l2 = new Label();
            mv.visitJumpInsn( GOTO, l2 );
            mv.visitLabel( l1 );
            mv.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
            mv.visitMethodInsn( INVOKESTATIC, "java/util/Collections", "emptyList", "()Ljava/util/List;", false );
            mv.visitLabel( l2 );
            mv.visitFrame( Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/util/Collection"} );
            mv.visitInsn( ARETURN );
            Label l3 = new Label();
            mv.visitLabel( l3 );
            mv.visitLocalVariable( "this", "L" + TYPE_NAME + ";", null, l0, l3, 0 );
            mv.visitMaxs( 1, 1 );
            mv.visitEnd();
        }
        /*
            protected void notifyModification() {
                ReactiveObjectUtil.notifyModification(this);
            }
         */
        {
            mv = cw.visitMethod( ACC_PROTECTED, "notifyModification", "()V", null, null );
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel( l0 );
            mv.visitLineNumber( 41, l0 );
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitMethodInsn( INVOKESTATIC, "org/drools/core/phreak/ReactiveObjectUtil", "notifyModification", "(Lorg/drools/core/phreak/ReactiveObject;)V", false );
            Label l1 = new Label();
            mv.visitLabel( l1 );
            mv.visitLineNumber( 42, l1 );
            mv.visitInsn( RETURN );
            Label l2 = new Label();
            mv.visitLabel( l2 );
            mv.visitLocalVariable( "this", "L" + TYPE_NAME + ";", null, l0, l2, 0 );
            mv.visitMaxs( 1, 1 );
            mv.visitEnd();
        }
        /*
            public void removeLeftTuple(Tuple leftTuple) {
                _lts.remove(leftTuple);
            }
         */
        {
            mv = cw.visitMethod( ACC_PUBLIC, "removeLeftTuple", "(Lorg/drools/core/spi/Tuple;)V", null, null );
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel( l0 );
            mv.visitLineNumber( 46, l0 );
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitFieldInsn( GETFIELD, TYPE_NAME, LEFT_TUPLES_FIELD_NAME, "Ljava/util/Collection;" );
            mv.visitVarInsn( ALOAD, 1 );
            mv.visitMethodInsn( INVOKEINTERFACE, "java/util/Collection", "remove", "(Ljava/lang/Object;)Z", true );
            mv.visitInsn( POP );
            Label l1 = new Label();
            mv.visitLabel( l1 );
            mv.visitLineNumber( 47, l1 );
            mv.visitInsn(RETURN);
            Label l2 = new Label();
            mv.visitLabel( l2 );
            mv.visitLocalVariable( "this", "Lorg/drools/core/phreak/AbstractReactiveObject;", null, l0, l2, 0 );
            mv.visitLocalVariable( "leftTuple", "Lorg/drools/core/spi/Tuple;", null, l0, l2, 1 );
            mv.visitMaxs( 2, 2 );
            mv.visitEnd();
        }
    }


    protected void buildConstructors(ClassWriter cw, ClassDefinition classDef) {
        // Building default constructor
        try {
            this.buildDefaultConstructor( cw,
                                          classDef );
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Building constructor with all fields
        if (classDef.getFieldsDefinitions().size() > 0 && classDef.getFieldsDefinitions().size() < 120) {
            this.buildConstructorWithFields( cw,
                                             classDef,
                                             classDef.getFieldsDefinitions() );
        }

        // Building constructor with key fields only
        List<FieldDefinition> keys = new ArrayList<FieldDefinition>();
        for ( FieldDefinition fieldDef : classDef.getFieldsDefinitions() ) {
            if ( fieldDef.isKey() ) {
                keys.add( fieldDef );
            }
        }
        if ( !keys.isEmpty() && keys.size() != classDef.getFieldsDefinitions().size() ) {
            this.buildConstructorWithFields( cw,
                                             classDef,
                                             keys );
        }
    }


    /**
     * A traitable class is a special class with support for dynamic properties and types.
     *
     * This method builds the trait map, containing the references to the proxies
     * for each trait carried by an object at a given time.
     *
     * @param cw
     * @param classDef
     */
    protected void buildTraitMap(ClassWriter cw, ClassDefinition classDef) {

        FieldVisitor fv = cw.visitField( ACC_PRIVATE,
                                         TraitableBean.TRAITSET_FIELD_NAME,
                                         Type.getDescriptor( Map.class ),
                                         "Ljava/util/Map<Ljava/lang/String;Lorg/drools/core/factmodel/traits/Thing;>;",
                                         null );
        fv.visitEnd();

        MethodVisitor mv;

        mv = cw.visitMethod( ACC_PUBLIC,
                             "_getTraitMap",
                             Type.getMethodDescriptor( Type.getType( Map.class ), new Type[] {} ),
                             "()Ljava/util/Map<Ljava/lang/String;Lorg/drools/factmodel/traits/Thing;>;",
                             null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD,
                           BuildUtils.getInternalType( classDef.getName() ),
                           TraitableBean.TRAITSET_FIELD_NAME,
                           Type.getDescriptor( Map.class ) );
        mv.visitInsn(ARETURN);
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();

        mv = cw.visitMethod( ACC_PUBLIC, "_setTraitMap", Type.getMethodDescriptor( Type.getType( void.class ), new Type[] { Type.getType( Map.class ) } ), null, null);
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitVarInsn( ALOAD, 1 );
        mv.visitFieldInsn( PUTFIELD, BuildUtils.getInternalType( classDef.getName() ), TraitableBean.TRAITSET_FIELD_NAME, Type.getDescriptor( Map.class ));
        mv.visitInsn( RETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();


        mv = cw.visitMethod( ACC_PUBLIC, "addTrait",
                             Type.getMethodDescriptor( Type.getType( void.class ), new Type[] { Type.getType( String.class ), Type.getType( Thing.class ) } ),
                             "(Ljava/lang/String;Lorg/drools/core/factmodel/traits/Thing;)V", null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitMethodInsn( INVOKEVIRTUAL,
                            BuildUtils.getInternalType( classDef.getName() ),
                            "_getTraitMap",
                            Type.getMethodDescriptor( Type.getType( Map.class ), new Type[] {} ) );
        mv.visitVarInsn( ALOAD, 1 );
        mv.visitVarInsn( ALOAD, 2 );
        mv.visitMethodInsn( INVOKEINTERFACE,
                            Type.getInternalName( Map.class ),
                            "put",
                            Type.getMethodDescriptor( Type.getType( Object.class ), new Type[] { Type.getType( Object.class ), Type.getType( Object.class ) } ) );
        mv.visitInsn( POP );
        mv.visitInsn( RETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();


        mv = cw.visitMethod( ACC_PUBLIC,
                             "getTrait",
                             Type.getMethodDescriptor( Type.getType( Thing.class ), new Type[] { Type.getType( String.class ) } ),
                             Type.getMethodDescriptor( Type.getType( Thing.class ), new Type[] { Type.getType( String.class ) } ),
                             null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitMethodInsn( INVOKEVIRTUAL,
                            BuildUtils.getInternalType( classDef.getName() ),
                            "_getTraitMap",
                            Type.getMethodDescriptor( Type.getType( Map.class ), new Type[] {} ) );
        mv.visitVarInsn( ALOAD, 1 );
        mv.visitMethodInsn( INVOKEINTERFACE,
                            Type.getInternalName( Map.class ),
                            "get",
                            Type.getMethodDescriptor( Type.getType( Object.class ), new Type[] { Type.getType( Object.class ) } ) );
        mv.visitTypeInsn( CHECKCAST, Type.getInternalName( Thing.class ) );
        mv.visitInsn( ARETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();


        mv = cw.visitMethod( ACC_PUBLIC,
                             "hasTrait",
                             Type.getMethodDescriptor( Type.getType( boolean.class ), new Type[] { Type.getType( String.class ) } ),
                             null,
                             null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitMethodInsn( INVOKEVIRTUAL,
                            BuildUtils.getInternalType( classDef.getName() ),
                            "_getTraitMap",
                            Type.getMethodDescriptor( Type.getType( Map.class ), new Type[] {} ) );
        Label l0 = new Label();
        mv.visitJumpInsn( IFNONNULL, l0 );
        mv.visitInsn( ICONST_0 );
        mv.visitInsn( IRETURN );
        mv.visitLabel( l0 );
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitMethodInsn( INVOKEVIRTUAL,
                            BuildUtils.getInternalType( classDef.getName() ),
                            "_getTraitMap",
                            Type.getMethodDescriptor( Type.getType( Map.class ), new Type[] {} ) );
        mv.visitVarInsn( ALOAD, 1 );
        mv.visitMethodInsn( INVOKEINTERFACE,
                            Type.getInternalName( Map.class ),
                            "containsKey",
                            Type.getMethodDescriptor( Type.getType( boolean.class ), new Type[] { Type.getType( Object.class ) } ) );
        mv.visitInsn( IRETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();


        mv = cw.visitMethod( ACC_PUBLIC,
                             "hasTraits",
                             Type.getMethodDescriptor( Type.getType( boolean.class ), new Type[] {} ),
                             null,
                             null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD, BuildUtils.getInternalType( classDef.getName() ),TraitableBean.TRAITSET_FIELD_NAME, Type.getDescriptor( Map.class ) );
        Label l5 = new Label();
        mv.visitJumpInsn( IFNULL, l5 );
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD, BuildUtils.getInternalType( classDef.getName() ), TraitableBean.TRAITSET_FIELD_NAME, Type.getDescriptor( Map.class ) );
        mv.visitMethodInsn( INVOKEINTERFACE, Type.getInternalName( Map.class ), "isEmpty", Type.getMethodDescriptor( Type.BOOLEAN_TYPE, new Type[] {} ) );
        mv.visitJumpInsn( IFNE, l5 );
        mv.visitInsn( ICONST_1 );
        Label l4 = new Label();
        mv.visitJumpInsn( GOTO, l4 );
        mv.visitLabel( l5 );
        mv.visitInsn( ICONST_0 );
        mv.visitLabel( l4 );
        mv.visitInsn( IRETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();


        mv = cw.visitMethod( ACC_PUBLIC, "removeTrait",
                             Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { Type.getType( String.class ) } ),
                             Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { Type.getType( String.class ) } ),
                             null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitMethodInsn( INVOKEVIRTUAL, BuildUtils.getInternalType( classDef.getName() ), "_getTraitMap", Type.getMethodDescriptor( Type.getType( Map.class ), new Type[] {} ) );
        mv.visitTypeInsn(CHECKCAST, TraitTypeMapConstants.TYPE_NAME );
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn( INVOKEVIRTUAL, TraitTypeMapConstants.TYPE_NAME, "removeCascade",
                            Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { Type.getType( String.class )} ) );
        mv.visitInsn( ARETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();

        mv = cw.visitMethod( ACC_PUBLIC, "removeTrait",
                             Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { Type.getType( BitSet.class ) } ),
                             Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { Type.getType( BitSet.class ) } ),
                             null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitMethodInsn( INVOKEVIRTUAL, BuildUtils.getInternalType( classDef.getName() ), "_getTraitMap", Type.getMethodDescriptor( Type.getType( Map.class ), new Type[] {} ) );
        mv.visitTypeInsn( CHECKCAST, TraitTypeMapConstants.TYPE_NAME );
        mv.visitVarInsn( ALOAD, 1 );
        mv.visitMethodInsn( INVOKEVIRTUAL, TraitTypeMapConstants.TYPE_NAME, "removeCascade",
                            Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { Type.getType( BitSet.class )} ) );
        mv.visitInsn( ARETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();


        mv = cw.visitMethod( ACC_PUBLIC,
                             "getTraits",
                             Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { } ),
                             "()Ljava/util/Collection<Ljava/lang/String;>;",
                             null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitMethodInsn( INVOKEVIRTUAL,
                            BuildUtils.getInternalType( classDef.getName() ),
                            "_getTraitMap",
                            Type.getMethodDescriptor( Type.getType( Map.class ), new Type[] {} ) );
        mv.visitMethodInsn( INVOKEINTERFACE,
                            Type.getInternalName( Map.class ),
                            "keySet",
                            Type.getMethodDescriptor( Type.getType( Set.class ), new Type[] {} ) );
        mv.visitInsn( ARETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();


        mv = cw.visitMethod( ACC_PUBLIC,
                             "_setBottomTypeCode",
                             Type.getMethodDescriptor( Type.getType( void.class ), new Type[] { Type.getType( BitSet.class ) } ),
                             null, null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD, BuildUtils.getInternalType( classDef.getName() ), TraitableBean.TRAITSET_FIELD_NAME , Type.getDescriptor( Map.class ) );
        mv.visitTypeInsn( CHECKCAST, TraitTypeMapConstants.TYPE_NAME );
        mv.visitVarInsn( ALOAD, 1 );
        mv.visitMethodInsn( INVOKEVIRTUAL,
                            TraitTypeMapConstants.TYPE_NAME,
                            "setBottomCode",
                            Type.getMethodDescriptor( Type.getType( void.class ), new Type[] { Type.getType( BitSet.class ) } ) );
        mv.visitInsn( RETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();

        mv = cw.visitMethod( ACC_PUBLIC,
                             "getMostSpecificTraits",
                             Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { } ) ,
                             "()Ljava/util/Collection<Lorg/drools/core/factmodel/traits/Thing;>;",
                             null );
        mv.visitCode();

        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD, BuildUtils.getInternalType( classDef.getName() ),
                           TraitableBean.TRAITSET_FIELD_NAME ,
                           Type.getDescriptor( Map.class ) );


        Label l99 = new Label();
        mv.visitJumpInsn( IFNULL, l99 );
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD, BuildUtils.getInternalType( classDef.getName() ),
                           TraitableBean.TRAITSET_FIELD_NAME ,
                           Type.getDescriptor( Map.class ) );
        mv.visitTypeInsn( CHECKCAST, TraitTypeMapConstants.TYPE_NAME );
        mv.visitMethodInsn( INVOKEVIRTUAL,
                            TraitTypeMapConstants.TYPE_NAME,
                            "getMostSpecificTraits",
                            Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { } )  );
        mv.visitInsn( ARETURN );
        mv.visitLabel( l99 );
        mv.visitMethodInsn( INVOKESTATIC,
                            Type.getInternalName( Collections.class ),
                            "emptySet",
                            Type.getMethodDescriptor( Type.getType( Set.class ), new Type[] { } )  );
        mv.visitMethodInsn( INVOKESTATIC,
                            Type.getInternalName( Collections.class ),
                            "unmodifiableCollection",
                            Type.getMethodDescriptor( Type.getType( Collection.class ), new Type[] { Type.getType( Collection.class ) } )  );
        mv.visitInsn( ARETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();

        mv = cw.visitMethod( ACC_PUBLIC, "getCurrentTypeCode", Type.getMethodDescriptor( Type.getType( BitSet.class ), new Type[] { } ) , null, null );
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD,
                           BuildUtils.getInternalType( classDef.getName() ),
                           TraitableBean.TRAITSET_FIELD_NAME,
                           Type.getDescriptor( Map.class ) );
        Label l3 = new Label();
        mv.visitJumpInsn( IFNONNULL, l3 );
        mv.visitInsn( ACONST_NULL );
        mv.visitInsn( ARETURN );
        mv.visitLabel( l3 );
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD,
                           BuildUtils.getInternalType( classDef.getName() ),
                           TraitableBean.TRAITSET_FIELD_NAME,
                           Type.getDescriptor( Map.class ) );
        mv.visitTypeInsn( CHECKCAST, TraitTypeMapConstants.TYPE_NAME );
        mv.visitMethodInsn( INVOKEVIRTUAL,
                            TraitTypeMapConstants.TYPE_NAME,
                            "getCurrentTypeCode",
                            Type.getMethodDescriptor( Type.getType( BitSet.class ), new Type[] { } ) );
        mv.visitInsn( ARETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();


    }


    /**
     * A traitable class is a special class with support for dynamic properties and types.
     *
     * This method builds the property map, containing the key/values pairs to implement
     * any property defined in a trait interface but not supported by the traited class
     * fields.
     *
     * @param cw
     * @param def
     */
    protected void buildDynamicPropertyMap( ClassWriter cw, ClassDefinition def ) {

        FieldVisitor fv = cw.visitField( Opcodes.ACC_PRIVATE,
                                         TraitableBean.MAP_FIELD_NAME,
                                         Type.getDescriptor( Map.class ) ,
                                         "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;",
                                         null);
        fv.visitEnd();

        MethodVisitor mv = cw.visitMethod( Opcodes.ACC_PUBLIC,
                                           "_getDynamicProperties",
                                           Type.getMethodDescriptor( Type.getType( Map.class ), new Type[] {} ),
                                           "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;",
                                           null);
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD, BuildUtils.getInternalType(def.getName()), TraitableBean.MAP_FIELD_NAME, Type.getDescriptor( Map.class ) );
        mv.visitInsn( ARETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();



        mv = cw.visitMethod( ACC_PUBLIC,
                             "_setDynamicProperties",
                             Type.getMethodDescriptor( Type.getType( void.class ), new Type[] { Type.getType( Map.class ) } ),
                             "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V",
                             null);
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitVarInsn( ALOAD, 1 );
        mv.visitFieldInsn ( PUTFIELD, BuildUtils.getInternalType( def.getName() ), TraitableBean.MAP_FIELD_NAME, Type.getDescriptor( Map.class ) );
        mv.visitInsn( RETURN) ;
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();

    }

    /**
     * A traitable class is a special class with support for dynamic properties and types.
     *
     * A traitable class in logical mode provides additional control over the values
     * and type(s) of its fields.
     *
     * @param cw
     * @param def
     */
    protected void buildFieldTMS( ClassWriter cw, ClassDefinition def ) {

        FieldVisitor fv = cw.visitField( Opcodes.ACC_PRIVATE,
                                         TraitableBean.FIELDTMS_FIELD_NAME,
                                         Type.getDescriptor( TraitFieldTMS.class ),
                                         null,
                                         null );
        fv.visitEnd();

        MethodVisitor mv = cw.visitMethod( Opcodes.ACC_PUBLIC,
                                           "_getFieldTMS",
                                           Type.getMethodDescriptor( Type.getType( TraitFieldTMS.class ), new Type[] {} ),
                                           null,
                                           null);
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD, BuildUtils.getInternalType( def.getName() ), TraitableBean.FIELDTMS_FIELD_NAME, Type.getDescriptor( TraitFieldTMS.class ) );
        mv.visitInsn( ARETURN );
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();

        mv = cw.visitMethod( ACC_PUBLIC,
                             "_setFieldTMS",
                             Type.getMethodDescriptor( Type.VOID_TYPE, new Type[] { Type.getType( TraitFieldTMS.class ) } ),
                             null,
                             null);
        mv.visitCode();
        mv.visitVarInsn( ALOAD, 0 );
        mv.visitVarInsn( ALOAD, 1 );
        mv.visitFieldInsn ( PUTFIELD, BuildUtils.getInternalType( def.getName() ), TraitableBean.FIELDTMS_FIELD_NAME, Type.getDescriptor( TraitFieldTMS.class ) );
        mv.visitInsn( RETURN) ;
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();

    }

    /**
     * Defines the class header for the given class definition
     */
    protected ClassWriter buildClassHeader(ClassLoader classLoader,
                                           ClassDefinition classDef) {
        boolean reactive = classDef.isReactive();

        String[] original = classDef.getInterfaces();
        int interfacesNr = original.length + (reactive ? 2 : 1);
        String[] interfaces = new String[interfacesNr];
        for ( int i = 0; i < original.length; i++ ) {
            interfaces[i] = BuildUtils.getInternalType( original[i] );
        }
        interfaces[original.length] = BuildUtils.getInternalType( GeneratedFact.class.getName() );
        if (reactive) {
            interfaces[original.length+1] = BuildUtils.getInternalType( ReactiveObject.class.getName() );
        }

        int classModifiers = Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER;
        if ( classDef.isAbstrakt() ) {
            classModifiers += Opcodes.ACC_ABSTRACT;
        }

        ClassWriter cw = createClassWriter( classLoader,
                                            classModifiers,
                                            BuildUtils.getInternalType( classDef.getClassName() ),
                                            null,
                                            BuildUtils.getInternalType( classDef.getSuperClass() ),
                                            interfaces );

        buildClassAnnotations(classDef, cw);

        cw.visitSource( classDef.getClassName() + ".java",
                        null );

        return cw;
    }



    /**
     * Creates the field defined by the given FieldDefinition
     *
     * @param cw
     * @param fieldDef
     */
    protected void buildField( ClassVisitor cw,
                               FieldDefinition fieldDef) {
        FieldVisitor fv = cw.visitField( Opcodes.ACC_PROTECTED,
                                         fieldDef.getName(),
                                         fieldDef.getGenericType().getDescriptor(),
                                         fieldDef.getGenericType().getSignature(),
                                         null );


        buildFieldAnnotations( fieldDef, fv );

        fv.visitEnd();
    }



    /**
     * Creates a default constructor for the class
     *
     * @param cw
     */
    protected void buildDefaultConstructor(ClassVisitor cw,
                                           ClassDefinition classDef) {


        MethodVisitor mv = cw.visitMethod( Opcodes.ACC_PUBLIC,
                                           "<init>",
                                           Type.getMethodDescriptor( Type.VOID_TYPE,
                                                                     new Type[]{} ),
                                           null,
                                           null );
        mv.visitCode();

        Label l0 = null;
        if ( this.debug ) {
            l0 = new Label();
            mv.visitLabel( l0 );
        }

        boolean hasObjects = defaultConstructorStart( mv, classDef );

        mv.visitInsn(Opcodes.RETURN);
        Label l1 = null;
        if ( this.debug ) {
            l1 = new Label();
            mv.visitLabel( l1 );
            mv.visitLocalVariable( "this",
                                   BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                   null,
                                   l0,
                                   l1,
                                   0 );
        }
        mv.visitMaxs( 0, 0 );
        mv.visitEnd();

    }


    protected boolean defaultConstructorStart( MethodVisitor mv, ClassDefinition classDef ) {
        // Building default constructor

        mv.visitVarInsn( Opcodes.ALOAD,
                         0 );

        String sup = "";
        try {
            sup = Type.getInternalName(Class.forName(classDef.getSuperClass()));
        } catch (ClassNotFoundException e) {
            sup = BuildUtils.getInternalType( classDef.getSuperClass() );
        }
        mv.visitMethodInsn( Opcodes.INVOKESPECIAL,
                            sup,
                            "<init>",
                            Type.getMethodDescriptor( Type.VOID_TYPE,
                                                      new Type[]{} ) );

        boolean hasObjects = false;
        for (FieldDefinition field : classDef.getFieldsDefinitions()) {

            hasObjects = hasObjects || initFieldWithDefaultValue( mv, classDef, field );
        }


        if ( classDef.isTraitable() ) {
            initializeDynamicTypeStructures( mv, classDef );
        }

        return hasObjects;
    }


    protected boolean initFieldWithDefaultValue( MethodVisitor mv, ClassDefinition classDef, FieldDefinition field ) {
        if ( field.getInitExpr() == null && field.isInherited() ) {
            return false;
        }
        // get simple init expression value
        Object val = BuildUtils.getDefaultValue(field);
        boolean hasObjects = false;

        if (val != null) {
            // there's a simple init expression
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            if ( BuildUtils.isPrimitive( field.getTypeName() )
                    || BuildUtils.isBoxed( field.getTypeName() )
                    || String.class.getName().equals( field.getTypeName() ) ) {
                mv.visitLdcInsn(val);
                if ( BuildUtils.isBoxed(field.getTypeName()) ) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                       BuildUtils.getInternalType(field.getTypeName()),
                                       "valueOf",
                                       "("+BuildUtils.unBox(field.getTypeName())+")"+BuildUtils.getTypeDescriptor(field.getTypeName()));
                }
            } else {
                hasObjects = true;
                String type = BuildUtils.getInternalType( val.getClass().getName() );
                mv.visitTypeInsn( NEW, type );
                mv.visitInsn(DUP);
                mv.visitMethodInsn( INVOKESPECIAL,
                                    type,
                                    "<init>",
                                    "()V");
            }
        } else {
            // there's a complex init expression
            if ( field.getInitExpr() != null ) {
                mv.visitVarInsn( ALOAD, 0 );
                mv.visitLdcInsn( field.getInitExpr() );
                mv.visitMethodInsn( INVOKESTATIC,
                                    "org/mvel2/MVEL",
                                    "eval",
                                    "(Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitTypeInsn( CHECKCAST, BuildUtils.getInternalType( field.getTypeName() ) );
                val = field.getInitExpr();
            }
        }


        if ( val != null ) {
            if (! field.isInherited()) {
                mv.visitFieldInsn( Opcodes.PUTFIELD,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   field.getName(),
                                   BuildUtils.getTypeDescriptor( field.getTypeName() ) );
            } else {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   field.getWriteMethod(),
                                   Type.getMethodDescriptor(Type.VOID_TYPE,
                                                            new Type[]{Type.getType(BuildUtils.getTypeDescriptor(field.getTypeName()))}
                                   ));
            }

        }

        return hasObjects;
    }


    /**
     * Initializes the trait map and dynamic property map to empty values
     * @param mv
     * @param classDef
     */
    protected void initializeDynamicTypeStructures( MethodVisitor mv, ClassDefinition classDef) {
        if ( classDef.isFullTraiting() ) {

            mv.visitVarInsn( ALOAD, 0 );
            mv.visitTypeInsn( NEW, TraitFieldTMS.TYPE_NAME );
            mv.visitInsn( DUP );
            mv.visitMethodInsn( INVOKESPECIAL, TraitFieldTMS.TYPE_NAME, "<init>", "()V" );
            mv.visitFieldInsn( PUTFIELD, BuildUtils.getInternalType( classDef.getClassName() ), TraitableBean.FIELDTMS_FIELD_NAME, Type.getDescriptor( TraitFieldTMS.class ) );

            for ( FactField hardField : classDef.getFields() ) {
                FieldDefinition fld = (FieldDefinition) hardField;
                mv.visitVarInsn( ALOAD, 0 );
                mv.visitFieldInsn( GETFIELD, BuildUtils.getInternalType( classDef.getClassName() ), TraitableBean.FIELDTMS_FIELD_NAME, Type.getDescriptor( TraitFieldTMS.class ) );

                mv.visitLdcInsn( Type.getType( BuildUtils.getTypeDescriptor( classDef.getClassName() ) ) );

                mv.visitLdcInsn( fld.resolveAlias() );

                if ( BuildUtils.isPrimitive( fld.getTypeName() ) ) {
//                    mv.visitFieldInsn( GETSTATIC, BuildUtils.getInternalType( BuildUtils.box( fld.getTypeName() ) ), "TYPE", Type.getDescriptor( Class.class ) );
                    mv.visitLdcInsn( Type.getType( BuildUtils.getTypeDescriptor( BuildUtils.box( fld.getTypeName() ) ) ) );
                } else {
                    mv.visitLdcInsn( Type.getType( BuildUtils.getTypeDescriptor( fld.getTypeName() ) ) );
                }

                mv.visitVarInsn( ALOAD, 0 );
                mv.visitMethodInsn( INVOKEVIRTUAL, BuildUtils.getInternalType( classDef.getClassName() ), BuildUtils.getterName( fld.getName(), fld.getTypeName() ), "()" + BuildUtils.getTypeDescriptor( fld.getTypeName() ) );
                if ( BuildUtils.isPrimitive( fld.getTypeName() ) ) {
                    mv.visitMethodInsn( INVOKESTATIC, BuildUtils.getInternalType( BuildUtils.box( fld.getTypeName() ) ), "valueOf", "(" + BuildUtils.getTypeDescriptor( fld.getTypeName() ) + ")" + BuildUtils.getTypeDescriptor( BuildUtils.box( fld.getTypeName() ) ) );
                }

                if ( fld.getInitExpr() != null ) {
                    mv.visitLdcInsn( fld.getInitExpr() );
                } else {
                    mv.visitInsn( ACONST_NULL );
                }

                mv.visitMethodInsn( INVOKEINTERFACE,
                                    Type.getInternalName( TraitFieldTMS.class ),
                                    "registerField",
                                    Type.getMethodDescriptor( Type.VOID_TYPE, new Type[] { Type.getType( Class.class ), Type.getType( String.class ), Type.getType( Class.class ), Type.getType( Object.class ), Type.getType( String.class ) } ) );
            }
        }
    }

    /**
     * Creates a constructor that takes and assigns values to all
     * fields in the order they are declared.
     *
     * @param cw
     * @param classDef
     */
    protected void buildConstructorWithFields(ClassVisitor cw,
                                              ClassDefinition classDef,
                                              Collection<FieldDefinition> fieldDefs) {


        Type[] params = new Type[fieldDefs.size()];
        int index = 0;
        for ( FieldDefinition field : fieldDefs ) {
            params[index++] = Type.getType( BuildUtils.getTypeDescriptor( field.getTypeName() ) );
        }

        MethodVisitor mv = cw.visitMethod( Opcodes.ACC_PUBLIC,
                                           "<init>",
                                           Type.getMethodDescriptor( Type.VOID_TYPE,
                                                                     params ),
                                           null,
                                           null );
        mv.visitCode();
        Label l0 = null;
        if ( this.debug ) {
            l0 = new Label();
            mv.visitLabel( l0 );
        }

        fieldConstructorStart( mv, classDef, fieldDefs );

        mv.visitInsn( Opcodes.RETURN );
        Label l1 = null;
        if ( this.debug ) {
            l1 = new Label();
            mv.visitLabel( l1 );
            mv.visitLocalVariable( "this",
                                   BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                   null,
                                   l0,
                                   l1,
                                   0 );
            for ( FieldDefinition field : classDef.getFieldsDefinitions() ) {
                Label l11 = new Label();
                mv.visitLabel( l11 );
                mv.visitLocalVariable( field.getName(),
                                       BuildUtils.getTypeDescriptor( field.getTypeName() ),
                                       null,
                                       l0,
                                       l1,
                                       0 );
            }
        }
        mv.visitMaxs( 0,
                      0 );
        mv.visitEnd();

    }

    protected void fieldConstructorStart(MethodVisitor mv, ClassDefinition classDef, Collection<FieldDefinition> fieldDefs) {
        mv.visitVarInsn( Opcodes.ALOAD,
                         0 );

        String sup = "";
        try {
            sup = Type.getInternalName(Class.forName(classDef.getSuperClass()));
        } catch (ClassNotFoundException e) {
            sup = BuildUtils.getInternalType( classDef.getSuperClass() );
        }

        mv.visitMethodInsn( Opcodes.INVOKESPECIAL,
                            sup,
                            "<init>",
                            Type.getMethodDescriptor( Type.VOID_TYPE,
                                                      new Type[]{} ) );

        int index = 1; // local vars start at 1, as 0 is "this"
        for ( FieldDefinition field : fieldDefs ) {
            if ( this.debug ) {
                Label l11 = new Label();
                mv.visitLabel( l11 );
            }
            mv.visitVarInsn( Opcodes.ALOAD,
                             0 );
            mv.visitVarInsn( Type.getType( BuildUtils.getTypeDescriptor( field.getTypeName() ) ).getOpcode( Opcodes.ILOAD ),
                             index++ );
            if ( field.getTypeName().equals( "long" ) || field.getTypeName().equals( "double" ) ) {
                // long and double variables use 2 words on the variables table
                index++;
            }

            if (! field.isInherited()) {
                mv.visitFieldInsn( Opcodes.PUTFIELD,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   field.getName(),
                                   BuildUtils.getTypeDescriptor( field.getTypeName() ) );
            } else {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                   BuildUtils.getInternalType(classDef.getClassName()),
                                   field.getWriteMethod(),
                                   Type.getMethodDescriptor(Type.VOID_TYPE,
                                                            new Type[]{Type.getType(BuildUtils.getTypeDescriptor(field.getTypeName()))}
                                   ));
            }

        }



        for ( FieldDefinition field : classDef.getFieldsDefinitions() ) {
            if ( ! fieldDefs.contains( field ) && field.getInitExpr() != null && ! "".equals( field.getInitExpr().trim() ) ) {

                initFieldWithDefaultValue( mv, classDef, field );

            }
        }

        if ( classDef.isTraitable() ) {
            initializeDynamicTypeStructures( mv, classDef );
        }

    }




    /**
     * Creates the set method for the given field definition
     *
     * @param cw
     * @param classDef
     * @param fieldDef
     */
    protected void buildSetMethod(ClassVisitor cw,
                                  ClassDefinition classDef,
                                  FieldDefinition fieldDef) {
        MethodVisitor mv;
        // set method
        {
            mv = cw.visitMethod( Opcodes.ACC_PUBLIC,
                                 fieldDef.getWriteMethod(),
                                 "(" + fieldDef.getGenericType().getDescriptor() + ")V",
                                 fieldDef.getGenericType().hasGenerics() ? "(" + fieldDef.getGenericType().getSignature() + ")V" : null,
                                 null );
            mv.visitCode();
            Label l0 = null;
            if ( this.debug ) {
                l0 = new Label();
                mv.visitLabel( l0 );
            }
            mv.visitVarInsn( Opcodes.ALOAD, 0 );

            if ( classDef.isTraitable() && classDef.isFullTraiting() ) {
                updateTraitableField( mv, classDef, fieldDef );
            } else {
                mv.visitVarInsn( Type.getType( BuildUtils.getTypeDescriptor( fieldDef.getTypeName() ) ).getOpcode( Opcodes.ILOAD ), 1 );
            }

            if ( ! fieldDef.hasOverride() ) {
                mv.visitFieldInsn( Opcodes.PUTFIELD,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   fieldDef.getName(),
                                   BuildUtils.getTypeDescriptor( fieldDef.getTypeName() ) );
            } else {
                mv.visitMethodInsn( INVOKESPECIAL,
                                    BuildUtils.getInternalType( classDef.getSuperClass() ),
                                    BuildUtils.setterName( fieldDef.getName()),
                                    Type.getMethodDescriptor( Type.VOID_TYPE,
                                                              new Type[]{Type.getType( BuildUtils.getTypeDescriptor( fieldDef.getOverriding()) )} ),
                                    false );
            }

            if (classDef.isReactive()) {
                mv.visitVarInsn( ALOAD, 0 );
                mv.visitMethodInsn( INVOKEVIRTUAL, BuildUtils.getInternalType( classDef.getClassName() ), "notifyModification", "()V", false );
            }

            mv.visitInsn( Opcodes.RETURN );
            Label l1 = null;
            if ( this.debug ) {
                l1 = new Label();
                mv.visitLabel( l1 );
                mv.visitLocalVariable( "this",
                                       BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                       null,
                                       l0,
                                       l1,
                                       0 );
            }
            mv.visitMaxs( 0, 0 );
            mv.visitEnd();
        }
    }

    /**
     * Creates the get method for the given field definition
     *
     * @param cw
     * @param classDef
     * @param fieldDef
     */
    protected void buildGetMethod(ClassVisitor cw,
                                  ClassDefinition classDef,
                                  FieldDefinition fieldDef) {
        MethodVisitor mv;
        // Get method
        {
            mv = cw.visitMethod( Opcodes.ACC_PUBLIC,
                                 fieldDef.getReadMethod(),
                                 "()" + Type.getType( fieldDef.getGenericType().getDescriptor() ),
                                 fieldDef.getGenericType().hasGenerics() ? "()" + fieldDef.getGenericType().getSignature() : null,
                                 null );
            mv.visitCode();
            Label l0 = null;
            if ( this.debug ) {
                l0 = new Label();
                mv.visitLabel( l0 );
            }
            mv.visitVarInsn( Opcodes.ALOAD,
                             0 );
            if ( ! fieldDef.hasOverride() ) {
                mv.visitFieldInsn( Opcodes.GETFIELD,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   fieldDef.getName(),
                                   BuildUtils.getTypeDescriptor( fieldDef.getTypeName() ) );
                mv.visitInsn( Type.getType( BuildUtils.getTypeDescriptor( fieldDef.getTypeName() ) ).getOpcode( Opcodes.IRETURN ) );
            } else {
                mv.visitMethodInsn( INVOKESPECIAL,
                                    BuildUtils.getInternalType( classDef.getSuperClass() ),
                                    BuildUtils.getterName( fieldDef.getName(), fieldDef.getOverriding() ),
                                    Type.getMethodDescriptor( Type.getType( BuildUtils.getTypeDescriptor( fieldDef.getOverriding() ) ), new Type[]{} ),
                                    false );
                mv.visitTypeInsn( CHECKCAST, BuildUtils.getInternalType( fieldDef.getTypeName() ) );
                mv.visitInsn( AsmUtil.returnType( fieldDef.getTypeName() ) );
            }

            Label l1 = null;
            if ( this.debug ) {
                l1 = new Label();
                mv.visitLabel( l1 );
                mv.visitLocalVariable( "this",
                                       BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                       null,
                                       l0,
                                       l1,
                                       0 );
            }
            mv.visitMaxs( 0,
                          0 );
            mv.visitEnd();
        }
    }

    protected void buildEquals(ClassVisitor cw,
                               ClassDefinition classDef) {
        MethodVisitor mv;
        // Building equals method
        {
            mv = cw.visitMethod( Opcodes.ACC_PUBLIC,
                                 "equals",
                                 "(Ljava/lang/Object;)Z",
                                 null,
                                 null );
            mv.visitCode();
            Label l0 = null;
            if ( this.debug ) {
                l0 = new Label();
                mv.visitLabel( l0 );
            }

            // if ( this == obj ) return true;
            mv.visitVarInsn( Opcodes.ALOAD,
                             0 );
            mv.visitVarInsn( Opcodes.ALOAD,
                             1 );
            Label l1 = new Label();
            mv.visitJumpInsn( Opcodes.IF_ACMPNE,
                              l1 );
            mv.visitInsn( Opcodes.ICONST_1 );
            mv.visitInsn( Opcodes.IRETURN );

            // if ( obj == null ) return false;
            mv.visitLabel( l1 );
            mv.visitVarInsn( Opcodes.ALOAD,
                             1 );
            Label l2 = new Label();
            mv.visitJumpInsn( Opcodes.IFNONNULL,
                              l2 );
            mv.visitInsn( Opcodes.ICONST_0 );
            mv.visitInsn( Opcodes.IRETURN );

            // if ( getClass() != obj.getClass() ) return false;
            mv.visitLabel( l2 );
            mv.visitVarInsn( Opcodes.ALOAD,
                             0 );
            mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                Type.getInternalName( Object.class ),
                                "getClass",
                                Type.getMethodDescriptor( Type.getType( Class.class ),
                                                          new Type[]{} ) );
            mv.visitVarInsn( Opcodes.ALOAD,
                             1 );
            mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                Type.getInternalName( Object.class ),
                                "getClass",
                                Type.getMethodDescriptor( Type.getType( Class.class ),
                                                          new Type[]{} ) );
            Label l3 = new Label();
            mv.visitJumpInsn( Opcodes.IF_ACMPEQ,
                              l3 );
            mv.visitInsn( Opcodes.ICONST_0 );
            mv.visitInsn( Opcodes.IRETURN );

            // final <classname> other = (<classname>) obj;
            mv.visitLabel( l3 );
            mv.visitVarInsn( Opcodes.ALOAD,
                             1 );
            mv.visitTypeInsn( Opcodes.CHECKCAST,
                              BuildUtils.getInternalType( classDef.getClassName() ) );
            mv.visitVarInsn( Opcodes.ASTORE,
                             2 );

            // for each key field
            int count = 0;
            for ( FieldDefinition field : classDef.getFieldsDefinitions() ) {
                if ( field.isKey() ) {
                    count++;

                    Label goNext = new Label();

                    if ( BuildUtils.isPrimitive(field.getTypeName()) ) {
                        // if attr is primitive

                        // if ( this.<attr> != other.<booleanAttr> ) return false;
                        mv.visitVarInsn( Opcodes.ALOAD,
                                         0 );


                        visitFieldOrGetter(mv, classDef, field);

                        mv.visitVarInsn(Opcodes.ALOAD,
                                        2);

                        visitFieldOrGetter(mv, classDef, field);

                        if ( field.getTypeName().equals( "long" ) ) {
                            mv.visitInsn( Opcodes.LCMP );
                            mv.visitJumpInsn( Opcodes.IFEQ,
                                              goNext );
                        } else if ( field.getTypeName().equals( "double" ) ) {
                            mv.visitInsn( Opcodes.DCMPL );
                            mv.visitJumpInsn( Opcodes.IFEQ,
                                              goNext );
                        } else if ( field.getTypeName().equals( "float" ) ) {
                            mv.visitInsn( Opcodes.FCMPL );
                            mv.visitJumpInsn( Opcodes.IFEQ,
                                              goNext );
                        } else {
                            // boolean, byte, char, short, int
                            mv.visitJumpInsn( Opcodes.IF_ICMPEQ,
                                              goNext );
                        }
                        mv.visitInsn( Opcodes.ICONST_0 );
                        mv.visitInsn( Opcodes.IRETURN );
                    } else {
                        // if attr is not a primitive

                        // if ( this.<attr> == null && other.<attr> != null ||
                        //      this.<attr> != null && ! this.<attr>.equals( other.<attr> ) ) return false;
                        mv.visitVarInsn( Opcodes.ALOAD,
                                         0 );

                        visitFieldOrGetter(mv, classDef, field);

                        Label secondIfPart = new Label();
                        mv.visitJumpInsn( Opcodes.IFNONNULL,
                                          secondIfPart );

                        // if ( other.objAttr != null ) return false;
                        mv.visitVarInsn( Opcodes.ALOAD,
                                         2 );

                        visitFieldOrGetter(mv, classDef, field);

                        Label returnFalse = new Label();
                        mv.visitJumpInsn( Opcodes.IFNONNULL,
                                          returnFalse );

                        mv.visitLabel( secondIfPart );
                        mv.visitVarInsn( Opcodes.ALOAD,
                                         0 );

                        visitFieldOrGetter(mv, classDef, field);

                        mv.visitJumpInsn( Opcodes.IFNULL,
                                          goNext );

                        mv.visitVarInsn( Opcodes.ALOAD,
                                         0 );

                        visitFieldOrGetter(mv, classDef, field);

                        mv.visitVarInsn( Opcodes.ALOAD,
                                         2 );

                        visitFieldOrGetter(mv, classDef, field);

                        if ( ! BuildUtils.isArray( field.getTypeName() ) ) {
                            mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                                "java/lang/Object",
                                                "equals",
                                                "(Ljava/lang/Object;)Z" );
                        } else {
                            mv.visitMethodInsn( Opcodes.INVOKESTATIC,
                                                "java/util/Arrays",
                                                "equals",
                                                "(" +
                                                        BuildUtils.arrayType( field.getTypeName() ) +
                                                        BuildUtils.arrayType( field.getTypeName() ) +
                                                        ")Z" );
                        }
                        mv.visitJumpInsn( Opcodes.IFNE,
                                          goNext );

                        mv.visitLabel( returnFalse );
                        mv.visitInsn( Opcodes.ICONST_0 );
                        mv.visitInsn( Opcodes.IRETURN );
                    }
                    mv.visitLabel( goNext );
                }
            }
            if ( count > 0 ) {
                mv.visitInsn( Opcodes.ICONST_1 );
            } else {
                mv.visitInsn( Opcodes.ICONST_0 );
            }
            mv.visitInsn( Opcodes.IRETURN );
            Label lastLabel = null;
            if ( this.debug ) {
                lastLabel = new Label();
                mv.visitLabel( lastLabel );
                mv.visitLocalVariable( "this",
                                       BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                       null,
                                       l0,
                                       lastLabel,
                                       0 );
                mv.visitLocalVariable( "obj",
                                       Type.getDescriptor( Object.class ),
                                       null,
                                       l0,
                                       lastLabel,
                                       1 );
                mv.visitLocalVariable( "other",
                                       BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                       null,
                                       l0,
                                       lastLabel,
                                       2 );
            }
            mv.visitMaxs( 0,
                          0 );
            mv.visitEnd();
        }
    }


    protected void buildSystemHashCode(ClassWriter cw) {
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs( 0, 0 );
            mv.visitEnd();
        }
    }


    protected void buildHashCode(ClassVisitor cw,
                                 ClassDefinition classDef) {

        MethodVisitor mv;
        // Building hashCode() method
        {
            mv = cw.visitMethod( Opcodes.ACC_PUBLIC,
                                 "hashCode",
                                 "()I",
                                 null,
                                 null );
            mv.visitCode();
            Label l0 = null;
            if ( this.debug ) {
                l0 = new Label();
                mv.visitLabel( l0 );
            }

            // int result = 1;
            mv.visitInsn( Opcodes.ICONST_1 );
            mv.visitVarInsn( Opcodes.ISTORE,
                             1 );

            // for each key field
            for ( FieldDefinition field : classDef.getFieldsDefinitions() ) {
                if ( field.isKey() ) {

                    // result = result * 31 + <attr_hash>
                    mv.visitVarInsn( Opcodes.ILOAD,
                                     1 );
                    mv.visitIntInsn( Opcodes.BIPUSH,
                                     31 );
                    mv.visitVarInsn( Opcodes.ILOAD,
                                     1 );
                    mv.visitInsn( Opcodes.IMUL );

                    mv.visitVarInsn(Opcodes.ALOAD,
                                    0);

                    visitFieldOrGetter(mv, classDef, field);

                    if ( "boolean".equals( field.getTypeName() ) ) {
                        // attr_hash ::== <boolean_attr> ? 1231 : 1237;
                        Label blabel1 = new Label();
                        mv.visitJumpInsn( Opcodes.IFEQ,
                                          blabel1 );
                        mv.visitIntInsn( Opcodes.SIPUSH,
                                         1231 );
                        Label blabel2 = new Label();
                        mv.visitJumpInsn( Opcodes.GOTO,
                                          blabel2 );
                        mv.visitLabel( blabel1 );
                        mv.visitIntInsn( Opcodes.SIPUSH,
                                         1237 );
                        mv.visitLabel( blabel2 );
                    } else if ( "long".equals( field.getTypeName() ) ) {
                        // attr_hash ::== (int) (longAttr ^ (longAttr >>> 32))
                        mv.visitVarInsn( Opcodes.ALOAD,
                                         0 );

                        visitFieldOrGetter(mv, classDef, field);

                        mv.visitIntInsn( Opcodes.BIPUSH,
                                         32 );
                        mv.visitInsn( Opcodes.LUSHR );
                        mv.visitInsn( Opcodes.LXOR );
                        mv.visitInsn( Opcodes.L2I );

                    } else if ( "float".equals( field.getTypeName() ) ) {
                        // attr_hash ::== Float.floatToIntBits( floatAttr );
                        mv.visitMethodInsn( Opcodes.INVOKESTATIC,
                                            Type.getInternalName( Float.class ),
                                            "floatToIntBits",
                                            "(F)I" );
                    } else if ( "double".equals( field.getTypeName() ) ) {
                        // attr_hash ::== (int) (Double.doubleToLongBits( doubleAttr ) ^ (Double.doubleToLongBits( doubleAttr ) >>> 32));
                        mv.visitMethodInsn( Opcodes.INVOKESTATIC,
                                            Type.getInternalName( Double.class ),
                                            "doubleToLongBits",
                                            "(D)J" );
                        mv.visitInsn( Opcodes.DUP2 );
                        mv.visitIntInsn( Opcodes.BIPUSH,
                                         32 );
                        mv.visitInsn( Opcodes.LUSHR );
                        mv.visitInsn( Opcodes.LXOR );
                        mv.visitInsn( Opcodes.L2I );
                    } else if ( !BuildUtils.isPrimitive(field.getTypeName()) ) {
                        // attr_hash ::== ((objAttr == null) ? 0 : objAttr.hashCode());
                        Label olabel1 = new Label();
                        mv.visitJumpInsn( Opcodes.IFNONNULL,
                                          olabel1 );
                        mv.visitInsn( Opcodes.ICONST_0 );
                        Label olabel2 = new Label();
                        mv.visitJumpInsn( Opcodes.GOTO,
                                          olabel2 );
                        mv.visitLabel( olabel1 );
                        mv.visitVarInsn( Opcodes.ALOAD,
                                         0 );

                        visitFieldOrGetter(mv, classDef, field);

                        if ( ! BuildUtils.isArray( field.getTypeName() ) ) {
                            mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                                "java/lang/Object",
                                                "hashCode",
                                                "()I" );
                        } else {
                            mv.visitMethodInsn( INVOKESTATIC,
                                                "java/util/Arrays",
                                                "hashCode",
                                                "(" + BuildUtils.arrayType( field.getTypeName() ) + ")I");
                        }
                        mv.visitLabel( olabel2 );
                    }

                    mv.visitInsn( Opcodes.IADD );
                    mv.visitVarInsn( Opcodes.ISTORE,
                                     1 );
                }
            }
            mv.visitVarInsn( Opcodes.ILOAD,
                             1 );
            mv.visitInsn( Opcodes.IRETURN );

            Label lastLabel = null;
            if ( this.debug ) {
                lastLabel = new Label();
                mv.visitLabel( lastLabel );
                mv.visitLocalVariable( "this",
                                       BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                       null,
                                       l0,
                                       lastLabel,
                                       0 );
                mv.visitLocalVariable( "hash",
                                       Type.getDescriptor( int.class ),
                                       null,
                                       l0,
                                       lastLabel,
                                       1 );
            }
            mv.visitMaxs( 0,
                          0 );
            mv.visitEnd();
        }
    }

    protected void buildToString(ClassVisitor cw,
                                 ClassDefinition classDef) {
        MethodVisitor mv;
        {
            mv = cw.visitMethod( Opcodes.ACC_PUBLIC,
                                 "toString",
                                 "()Ljava/lang/String;",
                                 null,
                                 null );
            mv.visitCode();

            Label l0 = null;
            if ( this.debug ) {
                l0 = new Label();
                mv.visitLabel( l0 );
            }

            // StringBuilder buf = new StringBuilder();
            mv.visitTypeInsn( Opcodes.NEW,
                              Type.getInternalName( StringBuilder.class ) );
            mv.visitInsn( Opcodes.DUP );
            mv.visitMethodInsn( Opcodes.INVOKESPECIAL,
                                Type.getInternalName( StringBuilder.class ),
                                "<init>",
                                "()V" );
            mv.visitVarInsn( Opcodes.ASTORE,
                             1 );

            // buf.append(this.getClass().getSimpleName())
            mv.visitVarInsn( Opcodes.ALOAD,
                             1 );
            mv.visitVarInsn( Opcodes.ALOAD,
                             0 );
            mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                BuildUtils.getInternalType( classDef.getClassName() ),
                                "getClass",
                                "()Ljava/lang/Class;" );
            mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                Type.getInternalName( Class.class ),
                                "getSimpleName",
                                "()Ljava/lang/String;" );
            mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                Type.getInternalName( StringBuilder.class ),
                                "append",
                                "(Ljava/lang/String;)Ljava/lang/StringBuilder;" );

            appendToStringBuilder(mv, "( ");
            buildFieldsToString( classDef, mv, false );
            appendToStringBuilder(mv, " )");

            mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                Type.getInternalName( StringBuilder.class ),
                                "toString",
                                "()Ljava/lang/String;" );
            mv.visitInsn( Opcodes.ARETURN );

            Label lastLabel = null;
            if ( this.debug ) {
                lastLabel = new Label();
                mv.visitLabel( lastLabel );
                mv.visitLocalVariable( "this",
                                       BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                       null,
                                       l0,
                                       lastLabel,
                                       0 );
                mv.visitLocalVariable( "buf",
                                       Type.getDescriptor( StringBuilder.class ),
                                       null,
                                       l0,
                                       lastLabel,
                                       1 );
            }
            mv.visitMaxs( 0, 0 );
            mv.visitEnd();
        }
    }

    protected boolean buildFieldsToString( ClassDefinition classDef, MethodVisitor mv, boolean previous ) {
        boolean first = true;
        for ( FieldDefinition field : classDef.getFieldsDefinitions() ) {
            buildFieldToString( field, classDef, mv, first );
            first = false;
        }
        return previous;
    }

    protected void buildFieldToString(FieldDefinition field, ClassDefinition classDef, MethodVisitor mv, boolean first) {
        if ( !first ) {
            // buf.append(", ");
            appendToStringBuilder(mv, ", ");
        }

        // buf.append(attrName)
        appendToStringBuilder(mv, field.getName());

        // buf.append("=");
        appendToStringBuilder(mv, "=");

        // buf.append(attrValue)
        if (field.isRecursive()) {
            appendToStringBuilder( mv, field.getTypeName() + " [recursive]");
        } else {
            mv.visitVarInsn(Opcodes.ALOAD,
                            0);

            visitFieldOrGetter( mv, classDef, field );

            if ( BuildUtils.isPrimitive(field.getTypeName()) ) {
                String type = field.getTypeName().matches( "(byte|short)" ) ? "int" : field.getTypeName();
                mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                    Type.getInternalName( StringBuilder.class ),
                                    "append",
                                    Type.getMethodDescriptor( Type.getType( StringBuilder.class ), Type.getType( BuildUtils.getTypeDescriptor( type ) ) ) );
            } else if ( BuildUtils.isArray( field.getTypeName() ) && BuildUtils.arrayDimSize( field.getTypeName() ) == 1 ) {


                mv.visitMethodInsn( INVOKESTATIC,
                                    "java/util/Arrays",
                                    "toString",
                                    "(" + BuildUtils.getTypeDescriptor( BuildUtils.arrayType( field.getTypeName() ) ) + ")Ljava/lang/String;" );

                mv.visitMethodInsn( INVOKEVIRTUAL,
                                    "java/lang/StringBuilder",
                                    "append",
                                    "(Ljava/lang/Object;)Ljava/lang/StringBuilder;" );

            } else {
                mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                    Type.getInternalName( StringBuilder.class ),
                                    "append",
                                    Type.getMethodDescriptor( Type.getType( StringBuilder.class ), Type.getType( Object.class ) ) );
            }
        }
    }

    private void appendToStringBuilder(MethodVisitor mv, String s) {
        mv.visitLdcInsn( s );
        mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                            Type.getInternalName(StringBuilder.class),
                            "append",
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;" );
    }


    protected void buildClassAnnotations(ClassDefinition classDef, ClassVisitor cw) {
        for (AnnotationDefinition ad : classDef.getAnnotations()) {
            AnnotationVisitor av = cw.visitAnnotation("L"+BuildUtils.getInternalType(ad.getName())+";", true);
            addAnnotationAttribute( ad, av );
            av.visitEnd();
        }
    }

    protected void buildFieldAnnotations(FieldDefinition fieldDef, FieldVisitor fv) {
        if (fieldDef.getAnnotations() != null) {
            for (AnnotationDefinition ad : fieldDef.getAnnotations()) {
                AnnotationVisitor av = fv.visitAnnotation("L"+BuildUtils.getInternalType(ad.getName())+";", true);
                addAnnotationAttribute( ad, av );
                av.visitEnd();
            }
        }
    }

    public static void addAnnotationAttribute( AnnotationDefinition ad, AnnotationVisitor av ) {
        for (String key : ad.getValues().keySet()) {
            AnnotationDefinition.AnnotationPropertyVal apv = ad.getValues().get(key);

            switch (apv.getValType()) {
                case STRINGARRAY:
                    AnnotationVisitor subAv = av.visitArray(apv.getProperty());
                    Object[] array = (Object[]) apv.getValue();
                    for (Object o : array) {
                        subAv.visit(null,o);
                    }
                    subAv.visitEnd();
                    break;
                case PRIMARRAY:
                    av.visit(apv.getProperty(),apv.getValue());
                    break;
                case ENUMARRAY:
                    AnnotationVisitor subEnav = av.visitArray(apv.getProperty());
                    Enum[] enArray = (Enum[]) apv.getValue();
                    String aenumType = "L" + BuildUtils.getInternalType( enArray[0].getClass().getName() ) + ";";
                    for (Enum enumer : enArray) {
                        subEnav.visitEnum(null,aenumType,enumer.name());
                    }
                    subEnav.visitEnd();
                    break;
                case CLASSARRAY:
                    AnnotationVisitor subKlav = av.visitArray(apv.getProperty());
                    Class[] klarray = (Class[]) apv.getValue();
                    for (Class klass : klarray) {
                        subKlav.visit( null, Type.getType( "L" + BuildUtils.getInternalType( klass.getName() ) + ";" ) );
                    }
                    subKlav.visitEnd();
                    break;
                case ENUMERATION:
                    String enumType = "L" + BuildUtils.getInternalType(apv.getType().getName()) + ";";
                    av.visitEnum(apv.getProperty(),enumType,((Enum) apv.getValue()).name());
                    break;
                case KLASS:
                    String klassName = BuildUtils.getInternalType(((Class) apv.getValue()).getName());
                    av.visit(apv.getProperty(),Type.getType("L"+klassName+";"));
                    break;
                case PRIMITIVE:
                    av.visit(apv.getProperty(),apv.getValue());
                    break;
                case STRING:
                    av.visit(apv.getProperty(),apv.getValue());
                    break;
            }

        }
    }


    protected void visitFieldOrGetter(MethodVisitor mv, ClassDefinition classDef, FieldDefinition field) {
        if (! field.isInherited()) {
            mv.visitFieldInsn( Opcodes.GETFIELD,
                               BuildUtils.getInternalType( classDef.getClassName() ),
                               field.getName(),
                               BuildUtils.getTypeDescriptor( field.getTypeName() ) );
        } else {
            mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                                BuildUtils.getInternalType( classDef.getClassName()),
                                field.getReadMethod(),
                                Type.getMethodDescriptor(Type.getType(BuildUtils.getTypeDescriptor(field.getTypeName())),
                                                         new Type[]{})
            );
        }
    }


    protected  void updateTraitableField( MethodVisitor mv, ClassDefinition classDef, FieldDefinition fieldDef ) {
        String fieldType = fieldDef.getTypeName();

        mv.visitVarInsn( ALOAD, 0 );
        mv.visitFieldInsn( GETFIELD, BuildUtils.getInternalType( classDef.getClassName() ), TraitableBean.FIELDTMS_FIELD_NAME, Type.getDescriptor( TraitFieldTMS.class ) );

        mv.visitLdcInsn( fieldDef.resolveAlias() );

        if ( BuildUtils.isPrimitive( fieldType ) ) {
            mv.visitVarInsn( AsmUtil.varType( fieldType ), 1 );
            mv.visitMethodInsn( INVOKESTATIC, BuildUtils.getInternalType( BuildUtils.box( fieldType ) ), "valueOf", "(" + BuildUtils.getTypeDescriptor( fieldType ) + ")" + BuildUtils.getTypeDescriptor( BuildUtils.box( fieldType ) ) );
        } else {
            mv.visitVarInsn( ALOAD, 1 );
        }

        if ( BuildUtils.isPrimitive( fieldType ) ) {
//            mv.visitFieldInsn( GETSTATIC, BuildUtils.getInternalType( BuildUtils.box( fieldType ) ), "TYPE", Type.getDescriptor( Class.class ) );
            mv.visitLdcInsn( Type.getType( BuildUtils.getTypeDescriptor( BuildUtils.box( fieldDef.getTypeName() ) ) ) );
        } else {
            mv.visitLdcInsn( Type.getType( BuildUtils.getTypeDescriptor( fieldType ) ) );
        }

        mv.visitMethodInsn( INVOKEINTERFACE,
                            Type.getInternalName( TraitFieldTMS.class ),
                            "set",
                            Type.getMethodDescriptor( Type.getType( Object.class ), new Type[] { Type.getType( String.class ), Type.getType( Object.class ), Type.getType( Class.class ) } ) );

        if ( BuildUtils.isPrimitive( fieldType ) ) {
            mv.visitTypeInsn( CHECKCAST, BuildUtils.getInternalType( BuildUtils.box( fieldType ) ) );
            mv.visitMethodInsn( INVOKEVIRTUAL, BuildUtils.getInternalType( BuildUtils.box( fieldType ) ), BuildUtils.numericMorph( BuildUtils.box( fieldType ) ), "()" + BuildUtils.getTypeDescriptor( fieldType ) );
        } else {
            mv.visitTypeInsn( CHECKCAST, BuildUtils.getInternalType( fieldType ) );
        }

    }

}

