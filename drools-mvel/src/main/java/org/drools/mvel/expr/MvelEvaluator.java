/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.drools.mvel.expr;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.drools.mvel.MVELSafeHelper;
import org.mvel2.MVEL;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.debug.DebugTools;
import org.mvel2.integration.VariableResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.mvel.MVELConditionEvaluator.isFullyEvaluated;

public class MvelEvaluator<T> {

    private static final boolean THREAD_UNSAFE = Boolean.valueOf( System.getProperty("drools.mvel.thread.unsafe") );

    private static final EvaluatorType DEFAULT_EVALUATOR_TYPE = EvaluatorType.THREAD_SAFE_ON_FIRST_EVAL;

    private static final Logger logger = LoggerFactory.getLogger(MvelEvaluator.class);

    protected final Serializable expr;

    public enum EvaluatorType {
        THREAD_UNSAFE, THREAD_SAFE_ON_FIRST_EVAL, SYNCHRONIZED_TILL_EVALUATED, FULLY_SYNCHRONIZED;

        public <T> MvelEvaluator<T> createMvelEvaluator(Serializable expr) {
            switch (this) {
                case THREAD_UNSAFE: return new MvelEvaluator<>(expr);
                case THREAD_SAFE_ON_FIRST_EVAL: return new MvelEvaluator.ThreadSafe<>(expr);
                case SYNCHRONIZED_TILL_EVALUATED: return new MvelEvaluator.SynchronizedTillEvaluated<>(expr);
                case FULLY_SYNCHRONIZED: return new MvelEvaluator.FullySynchronized<>(expr);
            }
            throw new UnsupportedOperationException();
        }
    }

    private MvelEvaluator(Serializable expr) {
        this.expr = expr;
    }

    public static <T> MvelEvaluator<T> createMvelEvaluator(Serializable expr) {
        return createMvelEvaluator(DEFAULT_EVALUATOR_TYPE, expr);
    }

    public static <T> MvelEvaluator<T> createMvelEvaluator(EvaluatorType evaluatorType, Serializable expr) {
        return THREAD_UNSAFE ? EvaluatorType.THREAD_UNSAFE.createMvelEvaluator(expr) : evaluatorType.createMvelEvaluator(expr);
    }

    public T evaluate(Object ctx) {
        return evaluate(ctx, (VariableResolverFactory) null);
    }

    public T evaluate(VariableResolverFactory factory) {
        return evaluate(null, factory);
    }

    public T evaluate(Object ctx, VariableResolverFactory factory) {
        return internalEvaluate(ctx, factory);
    }

    public T evaluate(Object ctx, Map<String, Object> vars) {
        return (T) MVELSafeHelper.getEvaluator().executeExpression(this.expr, ctx, vars);
    }

    protected <T> T internalEvaluate(Object ctx, VariableResolverFactory factory) {
        if (MVELDebugHandler.isDebugMode() && this.expr instanceof CompiledExpression) {
            CompiledExpression compexpr = (CompiledExpression) this.expr;
            if (MVELDebugHandler.verbose) {
                logger.info(DebugTools.decompile(compexpr));
            }
            return (T) MVEL.executeDebugger(compexpr, ctx, factory);
        }

        return (T) MVELSafeHelper.getEvaluator().executeExpression(this.expr, ctx, factory);
    }

    public Serializable getExpr() {
        return expr;
    }

    private static class ThreadSafe<T> extends MvelEvaluator<T> {
        private enum State {
            NEW, INITIALIZING, CONTENTED, INITIALIZED
        }

        private final AtomicReference<State> state = new AtomicReference(State.NEW);

        public ThreadSafe(Serializable expr) {
            super(expr);
        }

        @Override
        public synchronized T evaluate(Object ctx, Map<String, Object> vars) {
            if (state.get() != State.INITIALIZED && isFirstEvaluation()) {
                T result = super.evaluate(ctx, vars);
                notifyFirstEvaluationDone();
                return result;
            }

            return super.evaluate(ctx, vars);
        }

        @Override
        public T evaluate(Object ctx, VariableResolverFactory factory) {
            if (state.get() != State.INITIALIZED && isFirstEvaluation()) {
                T result = internalEvaluate(ctx, factory);
                notifyFirstEvaluationDone();
                return result;
            }

            return internalEvaluate(ctx, factory);
        }

        private void notifyFirstEvaluationDone() {
            synchronized (state) {
                boolean shouldNotify = state.get() == State.CONTENTED;
                state.set(State.INITIALIZED);
                if (shouldNotify) {
                    state.notifyAll();
                }
            }
        }

        private boolean isFirstEvaluation() {
            if (state.compareAndSet(State.NEW, State.INITIALIZING)) {
                return true;
            }
            waitForFirstEvaluation();
            return false;
        }

        private void waitForFirstEvaluation() {
            synchronized (state) {
                if (state.get() != State.INITIALIZED) {
                    try {
                        state.set(State.CONTENTED);
                        state.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private static class SynchronizedTillEvaluated<T> extends MvelEvaluator<T> {

        private volatile boolean fullyEvaluated;

        public SynchronizedTillEvaluated(Serializable expr) {
            super(expr);
        }

        @Override
        public T evaluate(Object ctx, VariableResolverFactory factory) {
            if (fullyEvaluated) {
                return internalEvaluate(ctx, factory);
            }

            synchronized (this) {
                T result = internalEvaluate(ctx, factory);
                fullyEvaluated = isFullyEvaluated(expr);
                return result;
            }
        }

        @Override
        public T evaluate(Object ctx, Map<String, Object> vars) {
            if (fullyEvaluated) {
                return super.evaluate(ctx, vars);
            }

            synchronized (this) {
                T result = super.evaluate(ctx, vars);
                fullyEvaluated = isFullyEvaluated(expr);
                return result;
            }
        }
    }

    private static class FullySynchronized<T> extends MvelEvaluator<T> {

        public FullySynchronized(Serializable expr) {
            super(expr);
        }

        @Override
        public synchronized T evaluate(Object ctx, VariableResolverFactory factory) {
            return internalEvaluate(ctx, factory);
        }

        @Override
        public synchronized T evaluate(Object ctx, Map<String, Object> vars) {
            return super.evaluate(ctx, vars);
        }
    }
}
