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

package org.drools.core.spi;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.drools.core.WorkingMemory;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.rule.Declaration;
import org.kie.internal.security.KiePolicyHelper;

/**
 * Accumulator
 *
 * Created: 04/06/2006
 *
 * @version $Id$
 */
public interface Accumulator
    extends
    Invoker {
    
    /**
     * Creates and return a context object for each working memory instance
     * 
     * @return
     */
    Object createWorkingMemoryContext();
    
    /**
     * Creates the context object for an accumulator session.
     * The context is passed as a parameter to every subsequent accumulator
     * method call in the same session.
     * 
     * @return
     */
    Object createContext();

    /**
     * Executes the initialization block of code
     * 
     * @param leftTuple tuple causing the rule fire
     * @param declarations previous declarations
     * @param workingMemory
     * @throws Exception
     */
    Object init(Object workingMemoryContext,
                Object context,
                Tuple leftTuple,
                Declaration[] declarations,
                WorkingMemory workingMemory);

    /**
     * Executes the accumulate (action) code for the given fact handle
     * 
     * @param leftTuple
     * @param handle
     * @param declarations
     * @param innerDeclarations
     * @param workingMemory
     * @throws Exception
     */
    Object accumulate(Object workingMemoryContext,
                      Object context,
                      Tuple leftTuple,
                      InternalFactHandle handle,
                      Declaration[] declarations,
                      Declaration[] innerDeclarations,
                      WorkingMemory workingMemory);
    
    /**
     * Returns true if this accumulator supports operation reversal
     * 
     * @return
     */
    boolean supportsReverse();
    
    /**
     * Reverses the accumulate action for the given fact handle
     * 
     * @param context
     * @param leftTuple
     * @param handle
     * @param declarations
     * @param innerDeclarations
     * @param workingMemory
     * @throws Exception
     */
    boolean tryReverse(Object workingMemoryContext,
                       Object context,
                       Tuple leftTuple,
                       InternalFactHandle handle,
                       Object value,
                       Declaration[] declarations,
                       Declaration[] innerDeclarations,
                       WorkingMemory workingMemory);

    /**
     * Gets the result of the accummulation
     * 
     * @param leftTuple
     * @param declarations
     * @param workingMemory
     * @return
     * @throws Exception
     */
    Object getResult(Object workingMemoryContext,
                     Object context,
                     Tuple leftTuple,
                     Declaration[] declarations,
                     WorkingMemory workingMemory);

    /**
     * This class is used as a wrapper delegate when a security 
     * policy is in place.
     */
    class SafeAccumulator implements Accumulator, Serializable {
        private static final long serialVersionUID = -2845820209337318924L;
        private Accumulator delegate;

        public SafeAccumulator(Accumulator delegate) {
            super();
            this.delegate = delegate;
        }

        public Object createWorkingMemoryContext() {
            return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return delegate.createWorkingMemoryContext();
                }
            }, KiePolicyHelper.getAccessContext());
        }

        public Object createContext() {
            return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return delegate.createContext();
                }
            }, KiePolicyHelper.getAccessContext());
        }

        public Object init(final Object workingMemoryContext,
                           final Object context,
                           final Tuple leftTuple,
                           final Declaration[] declarations,
                           final WorkingMemory workingMemory) {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        return delegate.init(workingMemoryContext, context, leftTuple, declarations, workingMemory);
                    }
                }, KiePolicyHelper.getAccessContext());
            } catch (PrivilegedActionException e) {
                throw new RuntimeException( e );
            }
        }

        public Object accumulate(final Object workingMemoryContext,
                final Object context, 
                final Tuple leftTuple, 
                final InternalFactHandle handle, 
                final Declaration[] declarations, 
                final Declaration[] innerDeclarations, 
                final WorkingMemory workingMemory) {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        return delegate.accumulate(workingMemoryContext, context, leftTuple, handle, declarations, innerDeclarations, workingMemory);
                    }
                }, KiePolicyHelper.getAccessContext());
            } catch (PrivilegedActionException e) {
                throw new RuntimeException( e );
            }
            throw new IllegalStateException("Should not reach here, as it should return from the prior 'run'");
        }

        public boolean supportsReverse() {
            // we have to secure even this call because it might run untrusted code 
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return delegate.supportsReverse();
                }
            }, KiePolicyHelper.getAccessContext());
        }

        public boolean tryReverse(final Object workingMemoryContext,
                final Object context, 
                final Tuple leftTuple, 
                final InternalFactHandle handle,
                final Object value,
                final Declaration[] declarations, 
                final Declaration[] innerDeclarations, 
                final WorkingMemory workingMemory) {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                    @Override
                    public Boolean run() throws Exception {
                        return delegate.tryReverse(workingMemoryContext, context, leftTuple, handle, value,
                                         declarations, innerDeclarations, workingMemory);
                    }
                }, KiePolicyHelper.getAccessContext());
            } catch (PrivilegedActionException e) {
                throw new RuntimeException( e );
            }
        }

        public Object getResult(final Object workingMemoryContext, 
                final Object context, 
                final Tuple leftTuple, 
                final Declaration[] declarations, 
                final WorkingMemory workingMemory) {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        return delegate.getResult(workingMemoryContext, context, leftTuple, declarations, workingMemory);
                    }
                }, KiePolicyHelper.getAccessContext());
            } catch (PrivilegedActionException e) {
                throw new RuntimeException( e );
            }
        }
        
        public boolean wrapsCompiledInvoker() {
            return delegate instanceof CompiledInvoker;
        }
    }

    static boolean isCompiledInvoker(final Accumulator accumulator) {
        return (accumulator instanceof CompiledInvoker)
                || (accumulator instanceof SafeAccumulator && ((SafeAccumulator) accumulator).wrapsCompiledInvoker());
    }

}
