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

package org.drools.mvel.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.drools.compiler.compiler.AnalysisResult;
import org.drools.compiler.compiler.BoundIdentifiers;
import org.drools.compiler.compiler.DescrBuildError;
import org.drools.compiler.lang.descr.AccumulateDescr;
import org.drools.compiler.lang.descr.AccumulateDescr.AccumulateFunctionCallDescr;
import org.drools.compiler.lang.descr.BaseDescr;
import org.drools.compiler.rule.builder.AccumulateBuilder;
import org.drools.compiler.rule.builder.RuleBuildContext;
import org.drools.compiler.rule.builder.RuleConditionBuilder;
import org.drools.compiler.rule.builder.util.AccumulateUtil;
import org.drools.compiler.rule.builder.util.PackageBuilderUtil;
import org.drools.core.base.extractors.ArrayElementReader;
import org.drools.core.base.extractors.SelfReferenceClassFieldReader;
import org.drools.core.reteoo.RuleTerminalNode.SortDeclarations;
import org.drools.core.rule.Accumulate;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.MultiAccumulate;
import org.drools.core.rule.MutableTypeConstraint;
import org.drools.core.rule.Pattern;
import org.drools.core.rule.RuleConditionElement;
import org.drools.core.rule.SingleAccumulate;
import org.drools.core.spi.Accumulator;
import org.drools.core.spi.Constraint;
import org.drools.core.spi.DeclarationScopeResolver;
import org.drools.core.spi.InternalReadAccessor;
import org.drools.core.spi.KnowledgeHelper;
import org.drools.core.spi.MvelAccumulator;
import org.drools.core.util.index.IndexUtil;
import org.drools.mvel.MVELConstraint;
import org.drools.mvel.MVELDialectRuntimeData;
import org.drools.mvel.asm.AsmUtil;
import org.drools.mvel.expr.MVELAccumulator;
import org.drools.mvel.expr.MVELAccumulatorFunctionExecutor;
import org.drools.mvel.expr.MVELCompilationUnit;
import org.drools.mvel.expr.MVELCompileable;
import org.kie.api.runtime.rule.AccumulateFunction;

/**
 * A builder for the java dialect accumulate version
 */
public class MVELAccumulateBuilder
    implements
        AccumulateBuilder {

    public RuleConditionElement build( final RuleBuildContext context,
                                       final BaseDescr descr ) {
        return build( context,
                      descr,
                      null );
    }

    @SuppressWarnings("unchecked")
    public RuleConditionElement build( final RuleBuildContext context,
                                       final BaseDescr descr,
                                       final Pattern prefixPattern ) {
        boolean typesafe = context.isTypesafe();
        try {
            final AccumulateDescr accumDescr = (AccumulateDescr) descr;

            if ( !accumDescr.hasValidInput() ) {
                return null;
            }

            final RuleConditionBuilder builder = (RuleConditionBuilder) context.getDialect().getBuilder( accumDescr.getInput().getClass() );

            // create source CE
            final RuleConditionElement source = builder.build( context,
                                                               accumDescr.getInput() );

            if ( source == null ) {
                return null;
            }

            MVELDialect dialect = (MVELDialect) context.getDialect();

            Map<String, Declaration> decls = context.getDeclarationResolver().getDeclarations( context.getRule() );
            Map<String, Declaration> sourceOuterDeclr = source.getOuterDeclarations();

            Map<String, Class< ? >> declarationClasses = DeclarationScopeResolver.getDeclarationClasses( decls );
            declarationClasses.putAll( DeclarationScopeResolver.getDeclarationClasses( sourceOuterDeclr ) );

            BoundIdentifiers boundIds = new BoundIdentifiers( declarationClasses,
                                                              context );

            final boolean readLocalsFromTuple = PackageBuilderUtil.isReadLocalsFromTuple(context, accumDescr, source);

            Accumulator[] accumulators;
            if ( accumDescr.isExternalFunction() ) {
                // uses accumulate functions
                accumulators = buildExternalFunctions( context,
                                                       accumDescr,
                                                       dialect,
                                                       decls,
                                                       sourceOuterDeclr,
                                                       boundIds,
                                                       readLocalsFromTuple,
                                                       source,
                                                       declarationClasses);
            } else {
                // it is a custom accumulate
                accumulators = buildCustomAccumulate( context,
                                                      accumDescr,
                                                      dialect,
                                                      decls,
                                                      sourceOuterDeclr,
                                                      boundIds,
                                                      readLocalsFromTuple );
            }

            List<Declaration> requiredDeclarations = new ArrayList<Declaration>();
            for ( Accumulator acc : accumulators ) {
                MvelAccumulator mvelAcc = (MvelAccumulator) acc;
                Collections.addAll( requiredDeclarations, mvelAcc.getRequiredDeclarations() );
            }

            MVELDialectRuntimeData data = ( MVELDialectRuntimeData ) context.getPkg().getDialectRuntimeRegistry().getDialectData( "mvel" );

            Accumulate accumulate;
            if (accumDescr.isMultiFunction()) {
                accumulate = new MultiAccumulate( source,
                                                  requiredDeclarations.toArray( new Declaration[ requiredDeclarations.size() ] ),
                                                  accumulators,
                                                  accumulators.length);
                int index = 0;
                for ( Accumulator accumulator : accumulators ) {
                    data.addCompileable( ((MultiAccumulate)accumulate).new Wirer( index++ ),
                                         ( MVELCompileable ) accumulator );
                    ((MVELCompileable) accumulator).compile( data, context.getRule() );
                }
            } else {
                accumulate = new SingleAccumulate( source,
                                                   requiredDeclarations.toArray( new Declaration[ requiredDeclarations.size() ] ),
                                                   accumulators[0] );
                    data.addCompileable( ((SingleAccumulate)accumulate).new Wirer( ),
                                         (MVELCompileable) accumulators[0] );
                    ((MVELCompileable) accumulators[0]).compile( data, context.getRule() );
            }

            return accumulate;
        } catch ( Exception e ) {
            AsmUtil.copyErrorLocation(e, descr);
            context.addError( new DescrBuildError( context.getParentDescr(),
                                                          descr,
                                                          e,
                                                          "Unable to build expression for 'accumulate' : " + e.getMessage() ) );
            return null;
        } finally {
            context.setTypesafe( typesafe );
        }
    }

    private Accumulator[] buildExternalFunctions(final RuleBuildContext context,
                                                 final AccumulateDescr accumDescr,
                                                 MVELDialect dialect,
                                                 Map<String, Declaration> decls,
                                                 Map<String, Declaration> sourceOuterDeclr,
                                                 BoundIdentifiers boundIds,
                                                 boolean readLocalsFromTuple,
                                                 RuleConditionElement source,
                                                 Map<String, Class<?>> declarationClasses) {
        Accumulator[] accumulators;
        List<AccumulateFunctionCallDescr> functions = accumDescr.getFunctions();

        accumulators = new Accumulator[functions.size()];
        // creating the custom array reader
        InternalReadAccessor arrayReader = new SelfReferenceClassFieldReader( Object[].class );

        int index = 0;
        Pattern pattern = (Pattern) context.getDeclarationResolver().peekBuildStack();
        for ( AccumulateFunctionCallDescr func : functions ) {
            // build an external function executor
            Supplier<Class<?>> classSupplier = () -> MVELExprAnalyzer.getExpressionType(context,
                                                                                        declarationClasses,
                                                                                        source,
                                                                                        func.getParams()[0]);
            String functionName = AccumulateUtil.getFunctionName(classSupplier, func.getFunction());
            AccumulateFunction function = context.getConfiguration().getAccumulateFunction( functionName );
            if( function == null ) {
                // might have been imported in the package
                function = context.getPkg().getAccumulateFunctions().get(func.getFunction());
            }
            if ( function == null ) {
                context.addError( new DescrBuildError( accumDescr,
                                                              context.getRuleDescr(),
                                                              null,
                                                              "Unknown accumulate function: '" + func.getFunction() + "' on rule '" + context.getRuleDescr().getName()
                                                                      + "'. All accumulate functions must be registered before building a resource." ) );
                return null;
            }

            final AnalysisResult analysis = dialect.analyzeExpression( context,
                                                                       accumDescr,
                                                                       func.getParams().length > 0 ? func.getParams()[0] : "\"\"",
                                                                       boundIds );

            MVELCompilationUnit unit = dialect.getMVELCompilationUnit( func.getParams().length > 0 ? func.getParams()[0] : "\"\"",
                                                                       analysis,
                                                                       getUsedDeclarations( decls,
                                                                                            analysis ),
                                                                       getUsedDeclarations( sourceOuterDeclr,
                                                                                            analysis ),
                                                                       null,
                                                                       context,
                                                                       "drools",
                                                                       KnowledgeHelper.class,
                                                                       readLocalsFromTuple,
                                                                       MVELCompilationUnit.Scope.CONSTRAINT );

            accumulators[index] = new MVELAccumulatorFunctionExecutor( unit,
                                                                         function );
            // if there is a binding, create the binding
            if ( func.getBind() != null ) {
                if ( context.getDeclarationResolver().isDuplicated( context.getRule(), func.getBind(), function.getResultType().getName() ) ) {
                    if ( ! func.isUnification() ) {
                        context.addError( new DescrBuildError( context.getParentDescr(),
                                                               accumDescr,
                                                               null,
                                                               "Duplicate declaration for variable '" + func.getBind() + "' in the rule '" + context.getRule().getName() + "'" ) );
                    } else {
                        Declaration inner = context.getDeclarationResolver().getDeclaration( func.getBind() );
                        Constraint c = new MVELConstraint( Collections.singletonList( context.getPkg().getName() ),
                                                           accumDescr.isMultiFunction()
                                                                ? "this[ " + index + " ] == " + func.getBind()
                                                                : "this == " + func.getBind(),
                                                           new Declaration[] { inner },
                                                           null,
                                                           null,
                                                           IndexUtil.ConstraintType.EQUAL,
                                                           context.getDeclarationResolver().getDeclaration( func.getBind() ),
                                                           accumDescr.isMultiFunction()
                                                                ? new ArrayElementReader( arrayReader, index, function.getResultType() )
                                                                : new SelfReferenceClassFieldReader( function.getResultType() ),
                                                           true);
                        ((MutableTypeConstraint) c).setType( Constraint.ConstraintType.BETA );
                        pattern.addConstraint( c );
                        index++;
                    }
                } else {
                    Declaration declr = pattern.addDeclaration( func.getBind() );
                    if (accumDescr.isMultiFunction()) {
                        declr.setReadAccessor(new ArrayElementReader(arrayReader, index, function.getResultType()));
                    } else {
                        declr.setReadAccessor(new SelfReferenceClassFieldReader( function.getResultType() ));
                    }
                }
            }
            index++;
        }
        return accumulators;
    }

    private Accumulator[] buildCustomAccumulate( final RuleBuildContext context,
                                                 final AccumulateDescr accumDescr,
                                                 MVELDialect dialect,
                                                 Map<String, Declaration> decls,
                                                 Map<String, Declaration> sourceOuterDeclr,
                                                 BoundIdentifiers boundIds,
                                                 boolean readLocalsFromTuple ) {

        Accumulator[] accumulators;
        final MVELAnalysisResult initCodeAnalysis = (MVELAnalysisResult) dialect.analyzeBlock( context,
                                                                                               accumDescr,
                                                                                               accumDescr.getInitCode(),
                                                                                               boundIds );

        // need to copy boundIds, as this as a "this" object.
        final MVELAnalysisResult actionCodeAnalysis = (MVELAnalysisResult) dialect.analyzeBlock( context,
                                                                                                 accumDescr.getActionCode(),
                                                                                                 boundIds,
                                                                                                 initCodeAnalysis.getMvelVariables(),
                                                                                                 "drools",
                                                                                                 KnowledgeHelper.class );

        final MVELAnalysisResult resultCodeAnalysis = (MVELAnalysisResult) dialect.analyzeExpression( context,
                                                                                                      accumDescr,
                                                                                                      accumDescr.getResultCode(),
                                                                                                      boundIds,
                                                                                                      initCodeAnalysis.getMvelVariables() );

        context.setTypesafe( initCodeAnalysis.isTypesafe() );
        MVELCompilationUnit initUnit = dialect.getMVELCompilationUnit( accumDescr.getInitCode(),
                                                                       initCodeAnalysis,
                                                                       getUsedDeclarations( decls,
                                                                                            initCodeAnalysis ),
                                                                       getUsedDeclarations( sourceOuterDeclr,
                                                                                            initCodeAnalysis ),
                                                                       initCodeAnalysis.getMvelVariables(),
                                                                       context,
                                                                       "drools",
                                                                       KnowledgeHelper.class,
                                                                       readLocalsFromTuple,
                                                                       MVELCompilationUnit.Scope.CONSTRAINT );

        context.setTypesafe( actionCodeAnalysis.isTypesafe() );
        MVELCompilationUnit actionUnit = dialect.getMVELCompilationUnit( accumDescr.getActionCode(),
                                                                         actionCodeAnalysis,
                                                                         getUsedDeclarations( decls,
                                                                                              actionCodeAnalysis ),
                                                                         getUsedDeclarations( sourceOuterDeclr,
                                                                                              actionCodeAnalysis ),
                                                                         initCodeAnalysis.getMvelVariables(),
                                                                         context,
                                                                         "drools",
                                                                         KnowledgeHelper.class,
                                                                         readLocalsFromTuple,
                                                                         MVELCompilationUnit.Scope.CONSTRAINT );

        MVELCompilationUnit reverseUnit = null;
        if ( accumDescr.getReverseCode() != null ) {
            context.setTypesafe( actionCodeAnalysis.isTypesafe() );
            reverseUnit = dialect.getMVELCompilationUnit( accumDescr.getReverseCode(),
                                                          actionCodeAnalysis,
                                                          getUsedDeclarations( decls,
                                                                               actionCodeAnalysis ),
                                                          getUsedDeclarations( sourceOuterDeclr,
                                                                               actionCodeAnalysis ),
                                                          initCodeAnalysis.getMvelVariables(),
                                                          context,
                                                          "drools",
                                                          KnowledgeHelper.class,
                                                          readLocalsFromTuple,
                                                          MVELCompilationUnit.Scope.CONSTRAINT );
        }

        context.setTypesafe( resultCodeAnalysis.isTypesafe() );
        MVELCompilationUnit resultUnit = dialect.getMVELCompilationUnit( accumDescr.getResultCode(),
                                                                         resultCodeAnalysis,
                                                                         getUsedDeclarations( decls,
                                                                                              resultCodeAnalysis ),
                                                                         getUsedDeclarations( sourceOuterDeclr,
                                                                                              resultCodeAnalysis ),
                                                                         initCodeAnalysis.getMvelVariables(),
                                                                         context,
                                                                         "drools",
                                                                         KnowledgeHelper.class,
                                                                         readLocalsFromTuple,
                                                                         MVELCompilationUnit.Scope.CONSTRAINT );

        accumulators = new Accumulator[]{new MVELAccumulator( initUnit,
                                                              actionUnit,
                                                              reverseUnit,
                                                              resultUnit )};
        return accumulators;
    }

    private Declaration[] getUsedDeclarations( Map<String, Declaration> decls,
                                               AnalysisResult analysis ) {
        final BoundIdentifiers usedIdentifiers = analysis.getBoundIdentifiers();
        List<Declaration> usedDeclarations = new ArrayList<Declaration>();
        for ( String id : usedIdentifiers.getDeclrClasses().keySet() ) {
            if ( decls.containsKey( id ) ) {
                usedDeclarations.add( decls.get( id ) );
            }
        }

        if ( !usedDeclarations.isEmpty() ) {
            Collections.sort( usedDeclarations,
                              SortDeclarations.instance );
        }

        return usedDeclarations.toArray( new Declaration[usedDeclarations.size()] );
    }
}
