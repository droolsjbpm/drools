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
import java.util.List;

import org.drools.core.factmodel.BuildUtils;
import org.drools.core.factmodel.ClassDefinition;
import org.drools.core.factmodel.EnumClassBuilder;
import org.drools.core.factmodel.EnumClassDefinition;
import org.drools.core.factmodel.EnumLiteralDefinition;
import org.drools.core.factmodel.FieldDefinition;
import org.mvel2.asm.ClassWriter;
import org.mvel2.asm.FieldVisitor;
import org.mvel2.asm.MethodVisitor;
import org.mvel2.asm.Opcodes;
import org.mvel2.asm.Type;

import static org.drools.mvel.asm.ClassGenerator.createClassWriter;

/**
 * A builder to dynamically build simple Javabean(TM) classes
 */
public class DefaultEnumClassBuilder implements Opcodes, EnumClassBuilder, Serializable {


    /**
     * Dynamically builds, defines and loads a class based on the given class definition
     *
     * @param classDef the class definition object structure
     *
     * @return the Class instance for the given class definition
     *
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SecurityException
     */
    public byte[] buildClass( ClassDefinition classDef, ClassLoader classLoader ) throws IOException,
            SecurityException,
            IllegalArgumentException,
            ClassNotFoundException {

        if ( ! ( classDef instanceof EnumClassDefinition) ) {
            throw new RuntimeException( "FATAL : Trying to create an enum out of a bean class definition  " + classDef );
        }

        EnumClassDefinition edef = (EnumClassDefinition) classDef;

        ClassWriter cw = this.buildClassHeader( classLoader, edef );

        this.buildLiterals(cw,
                           edef);

        this.buildFields( cw,
                          edef );


        this.buildConstructors( cw,
                                edef );

        this.buildGettersAndSetters( cw,
                                     edef );

        this.buildEqualityMethods( cw,
                                   edef );

        this.buildToString( cw,
                            edef );

        cw.visitEnd();

        byte[] serializedClass = cw.toByteArray();

        return serializedClass;
    }




    protected ClassWriter buildClassHeader(ClassLoader classLoader, EnumClassDefinition classDef) {
        return createClassWriter( classLoader,
                                  ACC_PUBLIC + ACC_FINAL + ACC_SUPER + ACC_ENUM,
                                  BuildUtils.getInternalType( classDef.getClassName() ),
                                  "Ljava/lang/Enum<" + BuildUtils.getTypeDescriptor( classDef.getClassName() ) + ">;",
                                  BuildUtils.getInternalType( classDef.getSuperClass() ),
                                  BuildUtils.getInternalTypes( classDef.getInterfaces() ) );
    }

    protected void buildLiterals(ClassWriter cw, EnumClassDefinition classDef) {
        FieldVisitor fv;
        for ( EnumLiteralDefinition lit : classDef.getEnumLiterals() ) {
            fv = cw.visitField( ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM,
                                lit.getName(),
                                BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                null,
                                null);
            fv.visitEnd();
        }

        {
            fv = cw.visitField( ACC_PRIVATE + ACC_FINAL + ACC_STATIC + ACC_SYNTHETIC,
                                "$VALUES",
                                "[" + BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                null,
                                null);
            fv.visitEnd();
        }
    }


    protected void buildFields(ClassWriter cw, EnumClassDefinition classDef) {
        FieldVisitor fv;
        for ( FieldDefinition fld : classDef.getFieldsDefinitions() ) {
            fv = cw.visitField( ACC_PRIVATE + ACC_FINAL,
                                fld.getName(),
                                BuildUtils.getTypeDescriptor( fld.getTypeName() ),
                                null,
                                null);
            fv.visitEnd();
        }
    }


    protected void buildConstructors(ClassWriter cw, EnumClassDefinition classDef) {
        MethodVisitor mv;
        final StringBuilder argTypesBuilder = new StringBuilder();
        int size = 0;
        for ( FieldDefinition fld : classDef.getFieldsDefinitions() ) {
            argTypesBuilder.append(BuildUtils.getTypeDescriptor( fld.getTypeName() ));
            size += BuildUtils.sizeOf( fld.getTypeName() );
        }


        {
            int ofs = 3;

            mv = cw.visitMethod( ACC_PRIVATE,
                                 "<init>",
                                 "(Ljava/lang/String;I" + argTypesBuilder.toString() +")V",
                                 "(" + argTypesBuilder.toString() + ")V",
                                 null );
            mv.visitCode();
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitVarInsn( ALOAD, 1 );
            mv.visitVarInsn( ILOAD, 2 );
            mv.visitMethodInsn( INVOKESPECIAL,
                                "java/lang/Enum",
                                "<init>",
                                "(Ljava/lang/String;I)V" );
            for ( FieldDefinition fld : classDef.getFieldsDefinitions() ) {
                mv.visitVarInsn( ALOAD, 0 );
                mv.visitVarInsn( AsmUtil.varType( fld.getTypeName() ), ofs );
                mv.visitFieldInsn( PUTFIELD,
                                   BuildUtils.getInternalType( classDef.getName() ),
                                   fld.getName(),
                                   BuildUtils.getTypeDescriptor( fld.getTypeName() ) );
                ofs += BuildUtils.sizeOf( fld.getTypeName() );
            }
            mv.visitInsn( RETURN );
            mv.visitMaxs( 3, ofs );
            mv.visitEnd();
        }


        {


            mv = cw.visitMethod( ACC_STATIC,
                                 "<clinit>",
                                 "()V",
                                 null,
                                 null);
            mv.visitCode();

            int N = classDef.getEnumLiterals().size();

            mv.visitTypeInsn( NEW,
                              BuildUtils.getInternalType( classDef.getClassName() ) );

            for ( int j = 0; j < N; j++ ) {
                EnumLiteralDefinition lit = classDef.getEnumLiterals().get( j );
                mv.visitInsn( DUP );
                mv.visitLdcInsn( lit.getName() );
                AsmUtil.pushInt( mv, j );

                List<String> args = lit.getConstructorArgs();
                for ( int k = 0; k < args.size(); k++ ) {
                    String argType = classDef.getField( k ).getTypeName();

                    mv.visitLdcInsn( args.get( k ) );
                    mv.visitMethodInsn( INVOKESTATIC,
                                        "org/mvel2/MVEL",
                                        "eval",
                                        "(Ljava/lang/String;)Ljava/lang/Object;");

                    if ( BuildUtils.isPrimitive( argType ) ) {
                        mv.visitTypeInsn( CHECKCAST,
                                          BuildUtils.getInternalType( BuildUtils.box( argType ) ) );
                        mv.visitMethodInsn( INVOKEVIRTUAL,
                                            BuildUtils.getInternalType( BuildUtils.box( argType ) ),
                                            BuildUtils.numericMorph( BuildUtils.box( argType ) ),
                                            "()" + BuildUtils.getTypeDescriptor( argType ) );
                    } else {
                        mv.visitTypeInsn( CHECKCAST,
                                          BuildUtils.getInternalType( argType ) );
                    }
                }

                mv.visitMethodInsn( INVOKESPECIAL,
                                    BuildUtils.getInternalType( classDef.getClassName() ),
                                    "<init>",
                                    "(Ljava/lang/String;I" + argTypesBuilder.toString() + ")V" );
                mv.visitFieldInsn(PUTSTATIC,
                                  BuildUtils.getInternalType(classDef.getClassName()),
                                  lit.getName(),
                                  BuildUtils.getTypeDescriptor(classDef.getClassName()));
                mv.visitTypeInsn( NEW, BuildUtils.getInternalType( classDef.getClassName() ) );
            }


            AsmUtil.pushInt( mv, N );
            mv.visitTypeInsn( ANEWARRAY, BuildUtils.getInternalType( classDef.getClassName() ));

            for ( int j = 0; j < N; j++ ) {
                EnumLiteralDefinition lit = classDef.getEnumLiterals().get( j );
                mv.visitInsn(DUP);
                AsmUtil.pushInt( mv, j );
                mv.visitFieldInsn( GETSTATIC,
                                   BuildUtils.getInternalType( classDef.getClassName() ),
                                   lit.getName(),
                                   BuildUtils.getTypeDescriptor( classDef.getClassName() ) );
                mv.visitInsn(AASTORE);
            }


            mv.visitFieldInsn( PUTSTATIC,
                               BuildUtils.getInternalType( classDef.getClassName() ),
                               "$VALUES",
                               "[" + BuildUtils.getTypeDescriptor( classDef.getClassName() ));

            mv.visitInsn( RETURN );
            mv.visitMaxs( 4 + size, 0 );
            mv.visitEnd();
        }



        {
            mv = cw.visitMethod( ACC_PUBLIC + ACC_STATIC,
                                 "valueOf",
                                 "(Ljava/lang/String;)" + BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                 null,
                                 null );
            mv.visitCode();
            mv.visitLdcInsn( Type.getType( BuildUtils.getTypeDescriptor( classDef.getClassName() ) ) );
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitMethodInsn( INVOKESTATIC,
                                "java/lang/Enum",
                                "valueOf",
                                "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;" );
            mv.visitTypeInsn( CHECKCAST,
                              BuildUtils.getInternalType( classDef.getClassName() ) );
            mv.visitInsn( ARETURN );
            mv.visitMaxs( 2, 1 );
            mv.visitEnd();
        }
    }




    protected void buildGettersAndSetters(ClassWriter cw, EnumClassDefinition classDef) {
        MethodVisitor mv;
        {
            mv = cw.visitMethod( ACC_PUBLIC + ACC_STATIC,
                                 "values",
                                 "()[" + BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                 null,
                                 null);
            mv.visitCode();
            mv.visitFieldInsn( GETSTATIC,
                               BuildUtils.getInternalType( classDef.getClassName() ),
                               "$VALUES",
                               "[" + BuildUtils.getTypeDescriptor( classDef.getClassName() ) );
            mv.visitMethodInsn( INVOKEVIRTUAL,
                                "[" + BuildUtils.getTypeDescriptor( classDef.getClassName() ),
                                "clone",
                                "()Ljava/lang/Object;" );
            mv.visitTypeInsn( CHECKCAST,
                              "[" + BuildUtils.getTypeDescriptor( classDef.getClassName() ) );
            mv.visitInsn( ARETURN );
            mv.visitMaxs( 1, 0 );
            mv.visitEnd();
        }

        for ( FieldDefinition fld : classDef.getFieldsDefinitions() ) {
            mv = cw.visitMethod( ACC_PUBLIC,
                                 BuildUtils.getterName( fld.getName(), fld.getTypeName() ),
                                 "()" + BuildUtils.getTypeDescriptor( fld.getTypeName() ),
                                 null,
                                 null );
            mv.visitCode();
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitFieldInsn( GETFIELD,
                               BuildUtils.getInternalType( classDef.getName() ),
                               fld.getName(),
                               BuildUtils.getTypeDescriptor( fld.getTypeName() ) );
            mv.visitInsn( AsmUtil.returnType( fld.getTypeName() ) );
            mv.visitMaxs( BuildUtils.sizeOf( fld.getTypeName() ), 1 );
            mv.visitEnd();


            mv = cw.visitMethod( ACC_PUBLIC,
                                 BuildUtils.setterName( fld.getName()),
                                 "(" + BuildUtils.getTypeDescriptor( fld.getTypeName() ) + ")V",
                                 null,
                                 null );
            mv.visitCode();
            mv.visitInsn( RETURN );
            mv.visitMaxs( 0, 1 + BuildUtils.sizeOf( fld.getTypeName() ) );
            mv.visitEnd();
        }
    }

    protected void buildToString(ClassWriter cw, EnumClassDefinition classDef) {


    }

    protected void buildEqualityMethods(ClassWriter cw, ClassDefinition classDef) {


    }
}

