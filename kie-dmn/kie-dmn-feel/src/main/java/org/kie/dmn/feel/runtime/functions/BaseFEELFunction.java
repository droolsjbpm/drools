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

package org.kie.dmn.feel.runtime.functions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.api.feel.runtime.events.FEELEvent.Severity;
import org.kie.dmn.feel.lang.EvaluationContext;
import org.kie.dmn.feel.lang.Symbol;
import org.kie.dmn.feel.lang.impl.NamedParameter;
import org.kie.dmn.feel.lang.types.FunctionSymbol;
import org.kie.dmn.feel.runtime.FEELFunction;
import org.kie.dmn.feel.runtime.events.FEELEventBase;
import org.kie.dmn.feel.runtime.events.InvalidParametersEvent;
import org.kie.dmn.feel.util.ClassUtil;
import org.kie.dmn.feel.util.Either;
import org.kie.dmn.feel.util.EvalHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseFEELFunction
        implements FEELFunction {

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private String name;
    private Symbol symbol;

    public BaseFEELFunction(String name) {
        this.name = name;
        this.symbol = new FunctionSymbol( name, this );
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        ((FunctionSymbol) this.symbol).setId( name );
    }

    @Override
    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public Object invokeReflectively(EvaluationContext ctx, Object[] params) {
        // use reflection to call the appropriate invoke method
        try {
            boolean isNamedParams = params.length > 0 && params[0] instanceof NamedParameter;
            if ( !isCustomFunction() ) {
                List<String> available = null;
                if ( isNamedParams ) {
                    available = Stream.of( params ).map( p -> ((NamedParameter) p).getName() ).collect( Collectors.toList() );
                }


                CandidateMethod cm = getCandidateMethod( ctx, params, isNamedParams, available );

                if ( cm != null ) {
                    Object result = cm.apply.invoke( this, cm.actualParams );

                    if ( result instanceof Either ) {
                        @SuppressWarnings("unchecked")
                        Either<FEELEvent, Object> either = (Either<FEELEvent, Object>) result;

                        Object eitherResult = either.cata( (left) -> {
                           ctx.notifyEvt( () -> {
                                                       if ( left instanceof InvalidParametersEvent ) {
                                                           InvalidParametersEvent invalidParametersEvent = (InvalidParametersEvent) left;
                                                           invalidParametersEvent.setNodeName( getName() );
                                                           invalidParametersEvent.setActualParameters(
                                                                   Stream.of( cm.apply.getParameters() ).map( p -> p.getAnnotation( ParameterName.class ).value() ).collect( Collectors.toList() ),
                                                                   Arrays.asList( cm.actualParams )
                                                           );
                                                       }
                                                       return left;
                                                   }
                            );
                            return null;
                        }, Function.identity() );

                        return eitherResult;
                    }

                    return result;
                } else {
                    // CandidateMethod cm could be null also if reflection failed on Platforms not supporting getClass().getDeclaredMethods()
                    String ps = getClass().toString();
                    logger.error( "Unable to find function '" + getName() + "( " + ps.substring( 1, ps.length() - 1 ) + " )'" );
                    ctx.notifyEvt(() -> {
                        return new FEELEventBase(Severity.ERROR, "Unable to find function '" + getName() + "( " + ps.substring(1, ps.length() - 1) + " )'", null);
                    });
                }
            } else {
                if ( isNamedParams ) {
                    params = rearrangeParameters(params, this.getParameters().get(0).stream().map(Param::getName).collect(Collectors.toList()));
                }
                Object result = invoke( ctx, params );
                if ( result instanceof Either ) {
                    @SuppressWarnings("unchecked")
                    Either<FEELEvent, Object> either = (Either<FEELEvent, Object>) result;

                    final Object[] usedParams = params;

                    Object eitherResult = either.cata( (left) -> {
                       ctx.notifyEvt( () -> {
                                                   if ( left instanceof InvalidParametersEvent ) {
                                                       InvalidParametersEvent invalidParametersEvent = (InvalidParametersEvent) left;
                                                       invalidParametersEvent.setNodeName( getName() );
                                                       invalidParametersEvent.setActualParameters( IntStream.of( 0, usedParams.length ).mapToObj( i -> "arg" + i ).collect( Collectors.toList() ), Arrays.asList( usedParams ) );
                                                   }
                                                   return left;
                                               }
                        );
                        return null;
                    }, Function.identity() );

                    return normalizeResult( eitherResult );
                }
                return normalizeResult( result );
            }
        } catch ( Exception e ) {
            logger.error( "Error trying to call function " + getName() + ".", e );
            ctx.notifyEvt( () -> {
                                       return new FEELEventBase( Severity.ERROR, "Error trying to call function " + getName() + ".", e );
                                   }
            );
        }
        return null;
    }

    /**
     * this method should be overriden by custom function implementations that should be invoked reflectively
     * @param ctx
     * @param params
     * @return
     */
    public Object invoke(EvaluationContext ctx, Object[] params) {
        throw new RuntimeException( "This method should be overriden by classes that implement custom feel functions" );
    }

    private Object[] rearrangeParameters(Object[] params, List<String> pnames) {
        if ( pnames.size() > 0 ) {
            Object[] actualParams = new Object[pnames.size()];
            for ( int i = 0; i < actualParams.length; i++ ) {
                for ( int j = 0; j < params.length; j++ ) {
                    if ( ((NamedParameter) params[j]).getName().equals( pnames.get( i ) ) ) {
                        actualParams[i] = ((NamedParameter) params[j]).getValue();
                        break;
                    }
                }
            }
            params = actualParams;
        }
        return params;
    }

    private CandidateMethod getCandidateMethod(EvaluationContext ctx, Object[] params, boolean isNamedParams, List<String> available) {
        CandidateMethod candidate = null;
        // first, look for exact matches
        for ( Method m : ClassUtil.getDeclaredMethods(getClass()) ) {
            if ( !Modifier.isPublic(m.getModifiers()) || !m.getName().equals( "invoke" ) ) {
                continue;
            }

            Object[] actualParams = null;
            boolean injectCtx = Arrays.stream( m.getParameterTypes() ).anyMatch( p -> ClassUtil.isAssignableFrom( EvaluationContext.class, p ) );
            if( injectCtx ) {
                actualParams = new Object[ params.length + 1 ];
                int j = 0;
                for (int i = 0; i < m.getParameterCount(); i++) {
                    if( ClassUtil.isAssignableFrom(EvaluationContext.class, m.getParameterTypes()[i] ) ) {
                        if( isNamedParams ) {
                            actualParams[i] = new NamedParameter( "ctx", ctx );
                        } else {
                            actualParams[i] = ctx;
                        }
                    } else if (j < params.length) {
                        actualParams[i] = params[j];
                        j++;
                    }
                }
            } else {
                actualParams = params;
            }
            if( isNamedParams ) {
                actualParams = calculateActualParams( ctx, m, actualParams, available );
                if( actualParams == null ) {
                    // incompatible method
                    continue;
                }
            }
            CandidateMethod cm = new CandidateMethod( actualParams );

            Class<?>[] parameterTypes = m.getParameterTypes();
            if (!isNamedParams && actualParams.length > 0) {
                // if named parameters, then it has been adjusted already in the calculateActualParams method,
                // otherwise adjust here
                adjustForVariableParameters( cm, parameterTypes );
            }

            if ( parameterTypes.length != cm.getActualParams().length ) {
                continue;
            }

            boolean found = true;
            for ( int i = 0; i < parameterTypes.length; i++ ) {
                Class<?> currentIdxActualParameterType = cm.getActualClasses()[i];
                if ( currentIdxActualParameterType != null && !ClassUtil.isAssignableFrom(parameterTypes[i], currentIdxActualParameterType ) ) {
                    // singleton list spec defines that "a=[a]", i.e., singleton collections should be treated as the single element
                    // and vice-versa
                    if ( ClassUtil.isAssignableFrom( Collection.class, currentIdxActualParameterType ) ) {
                        Collection<?> valueCollection = (Collection<?>) actualParams[i];                    
                        if ( valueCollection.size() == 1 ) {
                            Object singletonValue = valueCollection.iterator().next();
                            // re-perform the assignable-from check, this time using the element itself the singleton value from the original parameter list
                            if ( singletonValue != null && ClassUtil.isAssignableFrom(parameterTypes[i], singletonValue.getClass() ) ) {
                                Object[] newParams = new Object[cm.getActualParams().length];
                                System.arraycopy( cm.getActualParams(), 0, newParams, 0, cm.getActualParams().length ); // can't rely on adjustForVariableParameters() have actually copied
                                newParams[i] = singletonValue;
                                cm.setActualParams(newParams);
                                continue;
                            }
                        }
                    }
                    found = false;
                    break;
                }
            }
            if ( found ) {
                cm.setApply( m );
                if (candidate == null) {
                    candidate = cm;
                } else {
                    if (cm.getScore() > candidate.getScore()) {
                        candidate = cm;
                    } else if (cm.getScore() == candidate.getScore() 
                            && candidate.getApply().getParameterTypes().length == 1
                            && cm.getApply().getParameterTypes().length == 1
                            && candidate.getApply().getParameterTypes()[0].equals(Object.class)
                            && !cm.getApply().getParameterTypes()[0].equals(Object.class)) {
                        candidate = cm; // `cm` is more narrowed, hence reflect `candidate` to be now `cm`.
                    } else {
                        // do nothing.
                    }
                }
            }
        }
        return candidate;
    }

    @Override
    public List<List<Param>> getParameters() {
        // TODO: we could implement this method using reflection, just for consistency,
        // but it is not used at the moment
        return Collections.emptyList();
    }

    /**
     * Adjust CandidateMethod considering var args signature. 
     */
    private void adjustForVariableParameters(CandidateMethod cm, Class<?>[] parameterTypes) {
        if ( parameterTypes.length > 0 && parameterTypes[parameterTypes.length - 1].isArray() ) {
            // then it is a variable parameters function call
            Object[] newParams = new Object[parameterTypes.length];
            if ( newParams.length > 1 ) {
                System.arraycopy( cm.getActualParams(), 0, newParams, 0, newParams.length - 1 );
            }
            Object[] remaining = new Object[cm.getActualParams().length - parameterTypes.length + 1];
            newParams[newParams.length - 1] = remaining;
            System.arraycopy( cm.getActualParams(), parameterTypes.length - 1, remaining, 0, remaining.length );
            cm.setActualParams( newParams );
        }
    }

    private Object[] calculateActualParams(EvaluationContext ctx, Method m, Object[] params, List<String> available) {
        Annotation[][] pas = m.getParameterAnnotations();
        List<String> names = new ArrayList<>( m.getParameterCount() );
        for ( int i = 0; i < m.getParameterCount(); i++ ) {
            for ( int p = 0; p < pas[i].length; i++ ) {
                if ( pas[i][p] instanceof ParameterName ) {
                    names.add( ((ParameterName) pas[i][p]).value() );
                    break;
                }
            }
            if ( names.get( i ) == null ) {
                // no name found
                return null;
            }
        }
        Object[] actualParams = new Object[names.size()];
        boolean isVariableParameters = m.getParameterCount() > 0 && m.getParameterTypes()[m.getParameterCount()-1].isArray();
        String variableParamPrefix = isVariableParameters ? names.get( names.size()-1 ) : null;
        List<Object> variableParams = isVariableParameters ? new ArrayList<>(  ) : null;
        for ( Object o : params ) {
            NamedParameter np = (NamedParameter) o;
            if( names.contains( np.getName() ) ) {
                actualParams[names.indexOf( np.getName() )] = np.getValue();
            } else if( isVariableParameters ) {
                // check if it is a variable parameters method
                if( np.getName().matches( variableParamPrefix + "\\d+" ) ) {
                    int index = Integer.parseInt( np.getName().substring( variableParamPrefix.length() ) ) - 1;
                    if( variableParams.size() <= index ) {
                        for( int i = variableParams.size(); i < index; i++ ) {
                            // need to add nulls in case the user skipped indexes
                            variableParams.add( null );
                        }
                        variableParams.add( np.getValue() );
                    } else {
                        variableParams.set( index, np.getValue() );
                    }
                } else {
                    // invalid parameter, method is incompatible
                    return null;
                }
            } else {
                // invalid parameter, method is incompatible
                return null;
            }
        }
        if( isVariableParameters ) {
            actualParams[ actualParams.length - 1 ] = variableParams.toArray();
        }

        return actualParams;
    }

    private Object normalizeResult(Object result) {
        // this is to normalize types returned by external functions
        return result != null && result instanceof Number && !(result instanceof BigDecimal) ? EvalHelper.getBigDecimalOrNull( result.toString() ) : result;
    }

    protected boolean isCustomFunction() {
        return false;
    }

    private static class CandidateMethod {
        private Method   apply         = null;
        private Object[] actualParams  = null;
        private Class[]  actualClasses = null;
        private int score;

        public CandidateMethod(Object[] actualParams) {
            this.actualParams = actualParams;
            populateActualClasses();
        }

        private void calculateScore() {
            if ( actualClasses.length > 0 && actualClasses[actualClasses.length - 1] != null && actualClasses[actualClasses.length - 1].isArray() ) {
                score = 1;
            } else {
                score = 10;
            }
        }

        public Method getApply() {
            return apply;
        }

        public void setApply(Method apply) {
            this.apply = apply;
            calculateScore();
        }

        public Object[] getActualParams() {
            return actualParams;
        }

        public void setActualParams(Object[] actualParams) {
            this.actualParams = actualParams;
            populateActualClasses();
        }

        private void populateActualClasses() {
            this.actualClasses = Stream.of( this.actualParams ).map( p -> p != null ? p.getClass() : null ).toArray( Class[]::new );
        }

        public Class[] getActualClasses() {
            return actualClasses;
        }

        public int getScore() {
            return score;
        }

    }

}
