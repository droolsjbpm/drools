/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.feel.lang.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.kie.dmn.api.feel.runtime.events.FEELEventListener;
import org.kie.dmn.feel.FEEL;
import org.kie.dmn.feel.codegen.feel11.CompiledFEELExpression;
import org.kie.dmn.feel.codegen.feel11.ProcessedExpression;
import org.kie.dmn.feel.codegen.feel11.ProcessedFEELUnit;
import org.kie.dmn.feel.codegen.feel11.ProcessedUnaryTest;
import org.kie.dmn.feel.lang.CompiledExpression;
import org.kie.dmn.feel.lang.CompilerContext;
import org.kie.dmn.feel.lang.EvaluationContext;
import org.kie.dmn.feel.lang.FEELProfile;
import org.kie.dmn.feel.lang.Type;
import org.kie.dmn.feel.parser.feel11.profiles.DoCompileFEELProfile;
import org.kie.dmn.feel.runtime.FEELFunction;
import org.kie.dmn.feel.runtime.UnaryTest;
import org.kie.dmn.feel.util.ClassLoaderUtil;
import org.kie.dmn.feel.util.ClassUtil;

/**
 * Language runtime entry point
 */
public class FEELImpl
        implements FEEL {

    private static final Map<String,Object> EMPTY_INPUT = Collections.emptyMap();

    private Set<FEELEventListener> instanceEventListeners = new HashSet<>();

    private final ClassLoader classLoader;
    private final List<FEELProfile> profiles;
    // pre-cached results from the above profiles...
    private final Optional<ExecutionFrameImpl> customFrame;
    private final Collection<FEELFunction> customFunctions;
    private final boolean doCompile;

    public FEELImpl() {
        this(ClassLoaderUtil.findDefaultClassLoader(), Collections.emptyList());
    }

    public FEELImpl(ClassLoader cl) {
        this(cl, Collections.emptyList());
    }

    public FEELImpl(List<FEELProfile> profiles) {
        this(ClassLoaderUtil.findDefaultClassLoader(), profiles);
    }

    public FEELImpl(ClassLoader cl, List<FEELProfile> profiles) {
        this.classLoader = cl;
        this.profiles = Collections.unmodifiableList(profiles);
        ExecutionFrameImpl frame = null;
        Map<String, FEELFunction> functions = new HashMap<>();
        for (FEELProfile p : profiles) {
            for (FEELFunction f : p.getFEELFunctions()) {
                if (frame == null) {
                    frame = new ExecutionFrameImpl(null);
                }
                frame.setValue(f.getName(), f);
                functions.put(f.getName(), f);
            }
        }
        doCompile = profiles.stream().anyMatch( x -> ClassUtil.isInstance(DoCompileFEELProfile.class,x));
        customFrame = Optional.ofNullable(frame);
        customFunctions = Collections.unmodifiableCollection(functions.values());
    }

    @Override
    public CompilerContext newCompilerContext() {
        return newCompilerContext(Collections.emptySet());
    }
    
    public CompilerContext newCompilerContext(Collection<FEELEventListener> contextListeners) {
        return new CompilerContextImpl(getEventsManager(contextListeners)).addFEELFunctions(customFunctions);
    }

    public Collection<FEELFunction> getCustomFunctions() {
        return customFunctions;
    }

    @Override
    public CompiledExpression compile(String expression, CompilerContext ctx) {
        return new ProcessedExpression(
                expression,
                ctx,
                ProcessedFEELUnit.DefaultMode.of(doCompile || ctx.isDoCompile()),
                profiles).getResult();
    }

    public ProcessedExpression compileExpression(String expression, CompilerContext ctx) {
        return new ProcessedExpression(
                expression,
                ctx,
                ProcessedFEELUnit.DefaultMode.of(doCompile || ctx.isDoCompile()),
                profiles);
    }

    @Override
    public ProcessedUnaryTest compileUnaryTests(String expressions, CompilerContext ctx) {
        return new ProcessedUnaryTest(expressions, ctx, profiles);
    }

    @Override
    public Object evaluate(String expression) {
        return evaluate( expression, FEELImpl.EMPTY_INPUT );
    }
    
    @Override
    public Object evaluate(String expression, EvaluationContext ctx) {
        CompilerContext compilerCtx = newCompilerContext(ctx.getListeners());
        Map<String, Object> inputVariables = ctx.getAllValues();
        if ( inputVariables != null ) {
            inputVariables.entrySet().stream().forEach( e -> compilerCtx.addInputVariable( e.getKey(), e.getValue() ) );
        }
        CompiledExpression expr = compile( expression, compilerCtx );
        return evaluate( expr, ctx );
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> inputVariables) {
        CompilerContext ctx = newCompilerContext();
        if ( inputVariables != null ) {
            inputVariables.entrySet().stream().forEach( e -> ctx.addInputVariable( e.getKey(), e.getValue() ) );
        }
        CompiledExpression expr = compile( expression, ctx );
        if ( inputVariables == null ) {
            return evaluate( expr, EMPTY_INPUT );
        } else {
            return evaluate( expr, inputVariables );
        }
    }

    @Override
    public Object evaluate(CompiledExpression expr, Map<String, Object> inputVariables) {
        CompiledFEELExpression e = (CompiledFEELExpression) expr;
        return e.apply(newEvaluationContext(Collections.EMPTY_SET, inputVariables));
    }
    
    @Override
    public Object evaluate(CompiledExpression expr, EvaluationContext ctx) {
        CompiledFEELExpression e = (CompiledFEELExpression) expr;
        return e.apply(ctx.current());
    }

    /**
     * Creates a new EvaluationContext using this FEEL instance classloader, and the supplied parameters listeners and inputVariables
     */
    public EvaluationContextImpl newEvaluationContext(Collection<FEELEventListener> listeners, Map<String, Object> inputVariables) {
        return newEvaluationContext(this.classLoader, listeners, inputVariables);
    }

    /**
     * Creates a new EvaluationContext with the supplied classloader, and the supplied parameters listeners and inputVariables
     */
    public EvaluationContextImpl newEvaluationContext(ClassLoader cl, Collection<FEELEventListener> listeners, Map<String, Object> inputVariables) {
        FEELEventListenersManager eventsManager = getEventsManager(listeners);
        EvaluationContextImpl ctx = new EvaluationContextImpl(cl, eventsManager, inputVariables.size());
        if (customFrame.isPresent()) {
            ExecutionFrameImpl globalFrame = (ExecutionFrameImpl) ctx.pop();
            ExecutionFrameImpl interveawedFrame = customFrame.get();
            interveawedFrame.setParentFrame(ctx.peek());
            globalFrame.setParentFrame(interveawedFrame);
            ctx.push(interveawedFrame);
            ctx.push(globalFrame);
        }
        ctx.setValues(inputVariables);
        return ctx;
    }

    @Override
    public List<UnaryTest> evaluateUnaryTests(String expression) {
        return evaluateUnaryTests( expression, Collections.emptyMap() );
    }

    @Override
    public List<UnaryTest> evaluateUnaryTests(String expression, Map<String, Type> variableTypes) {
        CompilerContext ctx = newCompilerContext(getListeners());
        for( Map.Entry<String, Type> e : variableTypes.entrySet() ) {
            ctx.addInputVariableType( e.getKey(), e.getValue() );
        }

        return compileUnaryTests(expression, ctx)
                .apply(newEvaluationContext(ctx.getListeners(), EMPTY_INPUT));
    }

    @Override
    public void addListener(FEELEventListener listener) {
        instanceEventListeners.add( listener );
    }

    @Override
    public void removeListener(FEELEventListener listener) {
        instanceEventListeners.remove( listener );
    }

    @Override
    public Set<FEELEventListener> getListeners() {
        return Collections.unmodifiableSet(instanceEventListeners);
    }

    public FEELEventListenersManager getEventsManager(Collection<FEELEventListener> contextListeners) {
        FEELEventListenersManager listenerMgr = new FEELEventListenersManager();
        listenerMgr.addListeners(instanceEventListeners);
        listenerMgr.addListeners(contextListeners);
        return listenerMgr;
    }

}
