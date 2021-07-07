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

package org.drools.mvel.java;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.drools.compiler.compiler.AnalysisResult;
import org.drools.compiler.compiler.BoundIdentifiers;
import org.drools.compiler.compiler.DescrBuildError;
import org.drools.compiler.lang.descr.AccumulateDescr;
import org.drools.compiler.lang.descr.AccumulateDescr.AccumulateFunctionCallDescr;
import org.drools.compiler.lang.descr.AndDescr;
import org.drools.compiler.lang.descr.BaseDescr;
import org.drools.compiler.rule.builder.AccumulateBuilder;
import org.drools.compiler.rule.builder.RuleBuildContext;
import org.drools.compiler.rule.builder.RuleConditionBuilder;
import org.drools.compiler.rule.builder.dialect.java.parser.JavaLocalDeclarationDescr;
import org.drools.compiler.rule.builder.util.AccumulateUtil;
import org.drools.compiler.rule.builder.util.PackageBuilderUtil;
import org.drools.core.base.accumulators.JavaAccumulatorFunctionExecutor;
import org.drools.core.base.extractors.ArrayElementReader;
import org.drools.core.base.extractors.SelfReferenceClassFieldReader;
import org.drools.core.reteoo.RuleTerminalNode;
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
import org.drools.core.util.index.IndexUtil;
import org.drools.mvel.MVELConstraint;
import org.drools.mvel.builder.MVELExprAnalyzer;
import org.kie.api.runtime.rule.AccumulateFunction;

import static org.drools.mvel.java.JavaRuleBuilderHelper.createVariableContext;
import static org.drools.mvel.java.JavaRuleBuilderHelper.generateTemplates;

/**
 * A builder for the java dialect accumulate version
 */
public class JavaAccumulateBuilder
    implements
        AccumulateBuilder {

    public RuleConditionElement build( final RuleBuildContext context,
                                       final BaseDescr descr ) {
        return build( context,
                      descr,
                      null );
    }

    public RuleConditionElement build( final RuleBuildContext context,
                                       final BaseDescr descr,
                                       final Pattern prefixPattern ) {

        final AccumulateDescr accumDescr = (AccumulateDescr) descr;
        if ( !accumDescr.hasValidInput() ) {
            return null;
        }

        // build source
        BaseDescr input = accumDescr.getInput();
        if( input instanceof AndDescr && ((AndDescr) input).getDescrs().size() == 1 ) {
            input = ((AndDescr) input).getDescrs().get( 0 );
        }
        
        final RuleConditionBuilder builder = (RuleConditionBuilder) context.getDialect().getBuilder( input.getClass() );
        final RuleConditionElement source = builder.build( context,
                                                           input );
        if ( source == null ) {
            return null;
        }

        final boolean readLocalsFromTuple = PackageBuilderUtil.isReadLocalsFromTuple(context, accumDescr, source);
        
        Map<String, Declaration> declsInScope = context.getDeclarationResolver().getDeclarations( context.getRule() );
        if (prefixPattern != null && prefixPattern.getDeclaration() != null) {
            declsInScope.remove( prefixPattern.getDeclaration().getIdentifier() );
        }
        Map<String, Class< ? >> declCls = DeclarationScopeResolver.getDeclarationClasses( declsInScope );

        Accumulate accumulate;
        if ( accumDescr.isExternalFunction() ) {
            // if it uses 1+ external function, build methods for them
            accumulate = buildExternalFunctionCall( context,
                                                    accumDescr,
                                                    source,
                                                    declsInScope,
                                                    declCls,
                                                    readLocalsFromTuple );
        } else {
            // if it uses inline code, build the class for it
            accumulate = buildInlineAccumulate( context,
                                                accumDescr,
                                                source,
                                                declsInScope,
                                                declCls,
                                                readLocalsFromTuple );
        }

        return accumulate;
    }

    private Accumulate buildExternalFunctionCall( RuleBuildContext context,
                                                  AccumulateDescr accumDescr,
                                                  RuleConditionElement source,
                                                  Map<String, Declaration> declsInScope,
                                                  Map<String, Class< ? >> declCls,
                                                  boolean readLocalsFromTuple) {
        // list of functions to build
        final List<AccumulateFunctionCallDescr> funcCalls = accumDescr.getFunctions();
        // list of available source declarations
        final Declaration[] sourceDeclArr = source.getOuterDeclarations().values().toArray( new Declaration[source.getOuterDeclarations().size()] );
        Arrays.sort( sourceDeclArr, RuleTerminalNode.SortDeclarations.instance );

        // set of required previous declarations
        Set<Declaration> requiredDecl = new HashSet<Declaration>();

        Pattern pattern = (Pattern) context.getDeclarationResolver().peekBuildStack();

        if (accumDescr.isMultiFunction()) {
            // the accumulator array
            Accumulator[] accumulators = new Accumulator[funcCalls.size()];

            // creating the custom array reader
            InternalReadAccessor reader = new SelfReferenceClassFieldReader( Object[].class );

            int index = 0;
            for ( AccumulateFunctionCallDescr fc : funcCalls ) {
                AccumulateFunction function = getAccumulateFunction(context, accumDescr, fc, source, declCls);
                if (function == null) {
                    return null;
                }

                bindReaderToDeclaration(context, accumDescr, pattern, fc, new ArrayElementReader(reader, index, function.getResultType()), function.getResultType(), index);
                accumulators[index++] = buildAccumulator(context, accumDescr, declsInScope, declCls, readLocalsFromTuple, sourceDeclArr, requiredDecl, fc, function);
            }

            return new MultiAccumulate( source,
                                        requiredDecl.toArray(new Declaration[requiredDecl.size()]),
                                        accumulators,
                                        accumulators.length);
        } else {
            AccumulateFunctionCallDescr fc = accumDescr.getFunctions().get(0);
            AccumulateFunction function = getAccumulateFunction(context, accumDescr, fc, source, declCls);
            if (function == null) {
                return null;
            }

            Class<?> returnType = function.getResultType();
            if (!pattern.isCompatibleWithAccumulateReturnType(returnType)) {
                context.addError( new DescrBuildError( accumDescr,
                                                       context.getRuleDescr(),
                                                       null,
                                                       "Pattern of type: '" + pattern.getObjectType() + "' on rule '" + context.getRuleDescr().getName() +
                                                       "' is not compatible with type " + returnType.getCanonicalName() + " returned by accumulate function.") );
                return null;
            }

            bindReaderToDeclaration(context, accumDescr, pattern, fc, new SelfReferenceClassFieldReader( function.getResultType() ), function.getResultType(), -1);
            Accumulator accumulator = buildAccumulator(context, accumDescr, declsInScope, declCls, readLocalsFromTuple, sourceDeclArr, requiredDecl, fc, function);

            return new SingleAccumulate( source,
                                         requiredDecl.toArray(new Declaration[requiredDecl.size()]),
                                         accumulator );
        }
    }

    private void bindReaderToDeclaration( RuleBuildContext context, AccumulateDescr accumDescr, Pattern pattern, AccumulateFunctionCallDescr fc, InternalReadAccessor readAccessor, Class<?> resultType, int index ) {
        if ( fc.getBind() != null ) {
            if ( context.getDeclarationResolver().isDuplicated( context.getRule(), fc.getBind(), resultType.getName() ) ) {
                if ( ! fc.isUnification() ) {
                    context.addError( new DescrBuildError( context.getParentDescr(),
                                                           accumDescr,
                                                           null,
                                                           "Duplicate declaration for variable '" + fc.getBind() + "' in the rule '" + context.getRule().getName() + "'" ) );
                } else {
                    Declaration inner = context.getDeclarationResolver().getDeclaration( fc.getBind() );
                    Constraint c = new MVELConstraint( Collections.singletonList( context.getPkg().getName() ),
                                                       index >= 0
                                                            ? "this[ " + index + " ] == " + fc.getBind()
                                                            : "this == " + fc.getBind(),
                                                       new Declaration[] { inner },
                                                       null,
                                                       null,
                                                       IndexUtil.ConstraintType.EQUAL,
                                                       context.getDeclarationResolver().getDeclaration( fc.getBind() ),
                                                       index >= 0
                                                            ? new ArrayElementReader( readAccessor, index, resultType )
                                                            : readAccessor,
                                                       true);
                    (( MutableTypeConstraint) c).setType( Constraint.ConstraintType.BETA );
                    pattern.addConstraint( c );
                }
            } else {
                Declaration declr = pattern.addDeclaration( fc.getBind() );
                declr.setReadAccessor( readAccessor );
            }
        }
    }

    private AccumulateFunction getAccumulateFunction(RuleBuildContext context,
                                                     AccumulateDescr accumDescr,
                                                     AccumulateFunctionCallDescr fc,
                                                     RuleConditionElement source,
                                                     Map<String, Class< ? >> declCls) {
        String functionName = AccumulateUtil.getFunctionName(() -> MVELExprAnalyzer.getExpressionType(context, declCls, source, fc.getParams()[0]), fc.getFunction());

        // find the corresponding function
        AccumulateFunction function = context.getConfiguration().getAccumulateFunction( functionName );
        if( function == null ) {
            // might have been imported in the package
            function = context.getPkg().getAccumulateFunctions().get( functionName );
        }
        if ( function == null ) {
            context.addError( new DescrBuildError( accumDescr,
                                                   context.getRuleDescr(),
                                                   null,
                                                   "Unknown accumulate function: '" + functionName + "' on rule '" + context.getRuleDescr().getName() + "'. All accumulate functions must be registered before building a resource." ) );
        }
        return function;
    }

    private Accumulator buildAccumulator(RuleBuildContext context, AccumulateDescr accumDescr, Map<String, Declaration> declsInScope, Map<String, Class<?>> declCls, boolean readLocalsFromTuple, Declaration[] sourceDeclArr, Set<Declaration> requiredDecl, AccumulateFunctionCallDescr fc, AccumulateFunction function) {
        // analyze the expression
        final JavaAnalysisResult analysis = (JavaAnalysisResult) context.getDialect().analyzeBlock( context,
                                                                                                    accumDescr,
                                                                                                    fc.getParams().length > 0 ? fc.getParams()[0] : "\"\"",
                                                                                                    new BoundIdentifiers( declCls, context ) );

        if ( analysis == null ) {
            // not possible to get the analysis results - compilation error has been already logged
            return null;
        }

        final BoundIdentifiers usedIdentifiers = analysis.getBoundIdentifiers();

        // create the array of used declarations
        final Declaration[] previousDeclarations = collectRequiredDeclarations( declsInScope,
                                                                                requiredDecl,
                                                                                usedIdentifiers );

        // generate the code template
        return generateFunctionCallCodeTemplate( context,
                                                 accumDescr,
                                                 sourceDeclArr,
                                                 fc,
                                                 function,
                                                 usedIdentifiers,
                                                 previousDeclarations,
                                                 readLocalsFromTuple );
    }

    private Declaration[] collectRequiredDeclarations( Map<String, Declaration> declsInScope,
                                                       Set<Declaration> requiredDecl,
                                                       final BoundIdentifiers usedIdentifiers ) {
        final Declaration[] previousDeclarations = new Declaration[usedIdentifiers.getDeclrClasses().size()];
        int i = 0;
        for ( String key : usedIdentifiers.getDeclrClasses().keySet() ) {
            Declaration d = declsInScope.get( key );
            previousDeclarations[i++] = d;
            requiredDecl.add( d );
        }
        return previousDeclarations;
    }

    private JavaAccumulatorFunctionExecutor generateFunctionCallCodeTemplate( final RuleBuildContext context,
                                                                              final AccumulateDescr accumDescr,
                                                                              final Declaration[] sourceDeclArr,
                                                                              AccumulateFunctionCallDescr fc,
                                                                              AccumulateFunction function,
                                                                              final BoundIdentifiers usedIdentifiers,
                                                                              final Declaration[] previousDeclarations,
                                                                              final boolean readLocalsFromTuple ) {
        final String className = "accumulateExpression" + context.getNextId();
        final Map<String, Object> map = createVariableContext( className,
                                                               fc.getParams().length > 0 ? fc.getParams()[ 0 ] : "\"\"",
                                                               context,
                                                               previousDeclarations,
                                                               sourceDeclArr,
                                                               usedIdentifiers.getGlobals()
        );
        map.put( "readLocalsFromTuple",
                 readLocalsFromTuple ? Boolean.TRUE : Boolean.FALSE );

        JavaAccumulatorFunctionExecutor accumulator = new JavaAccumulatorFunctionExecutor( function );

        generateTemplates("returnValueMethod",
                "returnValueInvoker",
                context,
                className,
                map,
                accumulator,
                accumDescr);
        return accumulator;
    }

    private Accumulate buildInlineAccumulate( final RuleBuildContext context,
                                              final AccumulateDescr accumDescr,
                                              final RuleConditionElement source,
                                              Map<String, Declaration> decls,
                                              Map<String, Class< ? >> declCls,
                                              final boolean readLocalsFromTuple) {
        // ELSE, if it is not an external function, build it using the regular java builder
        final String className = "Accumulate" + context.getNextId();
        accumDescr.setClassName( className );

        BoundIdentifiers available = new BoundIdentifiers( declCls, context );

        final JavaAnalysisResult initCodeAnalysis = (JavaAnalysisResult) context.getDialect().analyzeBlock( context,
                                                                                                            accumDescr,
                                                                                                            accumDescr.getInitCode(),
                                                                                                            available );
        final AnalysisResult actionCodeAnalysis = context.getDialect().analyzeBlock( context,
                                                                                     accumDescr,
                                                                                     accumDescr.getActionCode(),
                                                                                     available );

        final AnalysisResult resultCodeAnalysis = context.getDialect().analyzeExpression( context,
                                                                                          accumDescr,
                                                                                          accumDescr.getResultCode(),
                                                                                          available );

        if ( initCodeAnalysis == null || actionCodeAnalysis == null || resultCodeAnalysis == null ) {
            // not possible to get the analysis results - compilation error has been already logged
            return null;
        }

        final Set<String> requiredDeclarations = new HashSet<String>( initCodeAnalysis.getBoundIdentifiers().getDeclrClasses().keySet() );
        requiredDeclarations.addAll( actionCodeAnalysis.getBoundIdentifiers().getDeclrClasses().keySet() );
        requiredDeclarations.addAll( resultCodeAnalysis.getBoundIdentifiers().getDeclrClasses().keySet() );

        final Map<String, Class< ? >> requiredGlobals = new HashMap<String, Class< ? >>( initCodeAnalysis.getBoundIdentifiers().getGlobals() );
        requiredGlobals.putAll( actionCodeAnalysis.getBoundIdentifiers().getGlobals() );
        requiredGlobals.putAll( resultCodeAnalysis.getBoundIdentifiers().getGlobals() );

        if ( accumDescr.getReverseCode() != null ) {
            final AnalysisResult reverseCodeAnalysis = context.getDialect().analyzeBlock( context,
                                                                                          accumDescr,
                                                                                          accumDescr.getActionCode(),
                                                                                          available );
            requiredDeclarations.addAll( reverseCodeAnalysis.getBoundIdentifiers().getDeclrClasses().keySet() );
            requiredGlobals.putAll( reverseCodeAnalysis.getBoundIdentifiers().getGlobals() );
        }

        final Declaration[] declarations = new Declaration[requiredDeclarations.size()];
        int i = 0;
        for ( Iterator<String> it = requiredDeclarations.iterator(); it.hasNext(); i++ ) {
            declarations[i] = decls.get( it.next() );
        }
        final Declaration[] sourceDeclArr = source.getOuterDeclarations().values().toArray( new Declaration[source.getOuterDeclarations().size()] );
        Arrays.sort( sourceDeclArr, RuleTerminalNode.SortDeclarations.instance );

        final Map<String, Object> map = createVariableContext( className,
                                                               null,
                                                               context,
                                                               declarations,
                                                               null,
                                                               requiredGlobals
        );

        map.put( "className",
                 accumDescr.getClassName() );
        map.put( "innerDeclarations",
                 sourceDeclArr );
        map.put( "isMultiPattern",
                 readLocalsFromTuple ? Boolean.TRUE : Boolean.FALSE );

        final String initCode = this.fixInitCode( initCodeAnalysis,
                                                  accumDescr.getInitCode() );
        final String actionCode = accumDescr.getActionCode();
        final String resultCode = accumDescr.getResultCode();

        String[] attributesTypes = new String[initCodeAnalysis.getLocalVariablesMap().size()];
        String[] attributes = new String[initCodeAnalysis.getLocalVariablesMap().size()];
        int index = 0;
        for ( Map.Entry<String, JavaLocalDeclarationDescr> entry : initCodeAnalysis.getLocalVariablesMap().entrySet() ) {
            attributes[index] = entry.getKey();
            attributesTypes[index] = entry.getValue().getType();
            index++;
        }

        map.put( "attributes",
                 attributes );
        map.put( "attributesTypes",
                 attributesTypes );

        map.put( "initCode",
                 initCode );
        map.put( "actionCode",
                 actionCode );
        map.put( "resultCode",
                 resultCode );
        if ( accumDescr.getReverseCode() == null ) {
            map.put( "reverseCode",
                     "" );
            map.put( "supportsReverse",
                     "false" );
        } else {
            map.put( "reverseCode",
                     accumDescr.getReverseCode() );
            map.put( "supportsReverse",
                     "true" );
        }

        map.put( "hashCode",
                actionCode.hashCode());

        SingleAccumulate accumulate = new SingleAccumulate(source, declarations);

        generateTemplates("accumulateInnerClass",
                "accumulateInvoker",
                context,
                className,
                map,
                accumulate.new Wirer(),
                accumDescr);
        return accumulate;
    }

    public String fixInitCode( JavaAnalysisResult analysis,
                                  final String originalCode ) {
        TreeSet<JavaLocalDeclarationDescr> locals = new TreeSet<JavaLocalDeclarationDescr>( new Comparator<JavaLocalDeclarationDescr>() {
            public int compare( JavaLocalDeclarationDescr o1,
                                JavaLocalDeclarationDescr o2 ) {
                return o1.getStart() - o2.getStart();
            }
        } );

        for ( JavaLocalDeclarationDescr local : analysis.getLocalVariablesMap().values() ) {
            locals.add( local );
        }

        StringBuilder initCode = new StringBuilder();
        int lastAdded = 0;
        for ( JavaLocalDeclarationDescr d : locals ) {
            // adding chunk
            initCode.append( originalCode.substring( lastAdded,
                                                     d.getStart() ) );
            lastAdded = d.getEnd();
            // adding variable initializations
            for ( JavaLocalDeclarationDescr.IdentifierDescr id : d.getIdentifiers() ) {
                initCode.append( originalCode.substring( id.getStart(),
                                                         id.getEnd() ) );
                initCode.append( ";" );
                lastAdded = id.getEnd();
                while ( lastAdded < originalCode.length() && (Character.isWhitespace( originalCode.charAt( lastAdded ) ) || originalCode.charAt( lastAdded ) == ';') ) {
                    lastAdded++;
                }
            }
        }
        initCode.append( originalCode.substring( lastAdded ) );

        return initCode.toString();
    }

}
