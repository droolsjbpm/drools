/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.modelcompiler.builder.generator.declaredtype;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.drools.compiler.builder.impl.KnowledgeBuilderImpl;
import org.drools.compiler.lang.descr.AnnotationDescr;
import org.drools.compiler.lang.descr.EnumDeclarationDescr;
import org.drools.compiler.lang.descr.PackageDescr;
import org.drools.compiler.lang.descr.TypeDeclarationDescr;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.factmodel.GeneratedFact;
import org.drools.modelcompiler.builder.GeneratedClassWithPackage;
import org.drools.modelcompiler.builder.ModelBuilderImpl;
import org.drools.modelcompiler.builder.PackageModel;
import org.drools.modelcompiler.builder.errors.InvalidExpressionErrorResult;
import org.drools.modelcompiler.util.MvelUtil;
import org.drools.core.addon.TypeResolver;
import org.kie.api.definition.type.Duration;
import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;
import org.kie.api.definition.type.Timestamp;

import static org.drools.modelcompiler.builder.JavaParserCompiler.compileAll;
import static org.drools.modelcompiler.builder.generator.DslMethodNames.ADD_ANNOTATION_CALL;
import static org.drools.modelcompiler.builder.generator.DslMethodNames.ANNOTATION_VALUE_CALL;
import static org.drools.modelcompiler.builder.generator.DslMethodNames.TYPE_META_DATA_CALL;

public class POJOGenerator {

    private ModelBuilderImpl builder;
    private InternalKnowledgePackage pkg;
    private PackageDescr packageDescr;
    private PackageModel packageModel;

    private static final Map<String, Class<?>> predefinedClassLevelAnnotation = new HashMap<>();

    static {
        predefinedClassLevelAnnotation.put("role", Role.class);
        predefinedClassLevelAnnotation.put("duration", Duration.class);
        predefinedClassLevelAnnotation.put("expires", Expires.class);
        predefinedClassLevelAnnotation.put("timestamp", Timestamp.class);
    }

    private static final List<String> exprAnnotations = Arrays.asList("duration", "timestamp");

    public POJOGenerator(ModelBuilderImpl builder, InternalKnowledgePackage pkg, PackageDescr packageDescr, PackageModel packageModel) {
        this.builder = builder;
        this.pkg = pkg;
        this.packageDescr = packageDescr;
        this.packageModel = packageModel;
    }

    public static Map<String, Class<?>> compileType(KnowledgeBuilderImpl kbuilder,
                                                    ClassLoader packageClassLoader,
                                                    List<GeneratedClassWithPackage> classesWithPackage) {
        return compileAll(kbuilder, packageClassLoader, classesWithPackage);
    }

    public static void registerType(TypeResolver typeResolver, Map<String, Class<?>> classMap) {
        for (Map.Entry<String, Class<?>> entry : classMap.entrySet()) {
            typeResolver.registerClass(entry.getKey(), entry.getValue());
            typeResolver.registerClass(entry.getValue().getSimpleName(), entry.getValue());
        }
    }

    public void generatePOJO() {
        TypeResolver typeResolver = pkg.getTypeResolver();

        for (TypeDeclarationDescr typeDescr : packageDescr.getTypeDeclarations()) {
            try {
                Class<?> type = typeResolver.resolveType(typeDescr.getFullTypeName());
                processTypeMetadata(type, typeDescr.getAnnotations());
            } catch (ClassNotFoundException e) {
                ClassOrInterfaceDeclaration generatedClass = new GeneratedClassDeclaration(error -> builder.addBuilderResult(error),
                                                                                           null,
                                                                                           null, // TODO type resolver
                                                                                           predefinedClassLevelAnnotation,
                                                                                           Collections.emptyList(),
                                                                                           Collections.singletonList(GeneratedFact.class))
                        .toClassDeclaration();
                packageModel.addGeneratedPOJO(generatedClass);
                addTypeMetadata(typeDescr.getTypeName());
            }
        }

        for (EnumDeclarationDescr enumDescr : packageDescr.getEnumDeclarations()) {
            try {
                Class<?> type = typeResolver.resolveType(enumDescr.getFullTypeName());
                processTypeMetadata(type, enumDescr.getAnnotations());
            } catch (ClassNotFoundException e) {
                TypeDeclaration generatedEnum = new EnumGenerator()
                        .generate(enumDescr);
                packageModel.addGeneratedPOJO(generatedEnum);
                addTypeMetadata(enumDescr.getTypeName());
            }
        }
    }

    private void addTypeMetadata(String typeName) {
        packageModel.addTypeMetaDataExpressions(registerTypeMetaData(pkg.getName() + "." + typeName));
    }

    private void processTypeMetadata(Class<?> type, Collection<AnnotationDescr> annotations) {
        MethodCallExpr typeMetaDataCall = registerTypeMetaData(type.getCanonicalName());

        for (AnnotationDescr ann : annotations) {
            typeMetaDataCall = new MethodCallExpr(typeMetaDataCall, ADD_ANNOTATION_CALL);
            typeMetaDataCall.addArgument(new StringLiteralExpr(ann.getName()));
            for (Map.Entry<String, Object> entry : ann.getValueMap().entrySet()) {
                MethodCallExpr annotationValueCall = new MethodCallExpr(null, ANNOTATION_VALUE_CALL);
                annotationValueCall.addArgument(new StringLiteralExpr(entry.getKey()));
                String expr = entry.getValue().toString();
                if (exprAnnotations.contains(ann.getName()) && MvelUtil.analyzeExpression(type, expr) == null) {
                    builder.addBuilderResult(new InvalidExpressionErrorResult("Unable to analyze expression '" + expr + "' for " + ann.getName() + " attribute"));
                }
                annotationValueCall.addArgument(quote(expr));
                typeMetaDataCall.addArgument(annotationValueCall);
            }
        }

        packageModel.addTypeMetaDataExpressions(typeMetaDataCall);
    }

    private static MethodCallExpr registerTypeMetaData(String className) {
        MethodCallExpr typeMetaDataCall = new MethodCallExpr(null, TYPE_META_DATA_CALL);
        typeMetaDataCall.addArgument(className + ".class");
        return typeMetaDataCall;
    }

    static String quote(String str) {
        return "\"" + str.replace("\"", "\\\"") + "\"";
    }
}
