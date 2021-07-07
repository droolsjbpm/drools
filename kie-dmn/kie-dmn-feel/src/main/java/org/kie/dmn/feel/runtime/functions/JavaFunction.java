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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.api.feel.runtime.events.FEELEvent.Severity;
import org.kie.dmn.feel.lang.EvaluationContext;
import org.kie.dmn.feel.runtime.events.FEELEventBase;
import org.kie.dmn.feel.runtime.events.InvalidInputEvent;
import org.kie.dmn.feel.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaFunction
        extends BaseFEELFunction {

    private static final Logger logger = LoggerFactory.getLogger( JavaFunction.class );

    private final List<Param> parameters;
    private final Class        clazz;
    private final Method       method;

    public JavaFunction(String name, List<Param> params, Class clazz, Method method) {
        super( name );
        this.parameters = params;
        this.clazz = clazz;
        this.method = method;
    }

    @Override
    public List<List<Param>> getParameters() {
        return Arrays.asList( parameters );
    }

    public FEELFnResult<Object> invoke(EvaluationContext ctx, Object[] params) {
        if( params.length != parameters.size() ) {
            return FEELFnResult.ofError(new InvalidInputEvent(Severity.ERROR, "Illegal invocation of function", getName(), getName() + "( " + Arrays.asList(params)+" )", getSignature()));
        }
        
        FEELEvent capturedException = null;
        try {
            ctx.enterFrame();
            for ( int i = 0; i < parameters.size(); i++ ) {
                ctx.setValue(parameters.get(i).name, params[i]);
            }
            Object[] actualParams = prepareParams( params );
            Object result = method.invoke( clazz, actualParams );
            return FEELFnResult.ofResult( result );
        } catch ( InvocationTargetException e ) {
            String message = e.getTargetException().getMessage();
            capturedException = buildCaptured(e, message);
        } catch ( IllegalAccessException e ) {
            String message = e.getCause().getMessage();
            capturedException = buildCaptured(e, message);
        } catch (Exception e) {
            String message = e.getMessage();
            capturedException = buildCaptured(e, message);
        } finally {
            ctx.exitFrame();
        }
        return FEELFnResult.ofError( capturedException );
    }

    private FEELEventBase buildCaptured(Exception e, String message) {
        return new FEELEventBase(Severity.ERROR, "Error invoking " + toString() + ": " + message, new RuntimeException("Error invoking function " + getSignature() + ".", e));
    }

    private Object[] prepareParams(Object[] params) {
        Object[] actual = new Object[ params.length ];
        Class[] paramTypes = method.getParameterTypes();
        params = adjustForVariableParameters( paramTypes, params );
        for( int i = 0; i < paramTypes.length; i++ ) {
            if (params[i] != null && ClassUtil.isAssignableFrom(paramTypes[i], params[i].getClass())) {
                actual[i] = params[i];
            } else {
                // try to coerce
                if( params[i] == null ) {
                    actual[i] = null;
                } else if( params[i] instanceof Number ) {
                    if( paramTypes[i] == byte.class || paramTypes[i] == Byte.class ) {
                        actual[i] = ((Number)params[i]).byteValue();
                    } else if( paramTypes[i] == short.class || paramTypes[i] == Short.class ) {
                        actual[i] = ((Number) params[i]).shortValue();
                    } else if( paramTypes[i] == int.class || paramTypes[i] == Integer.class ) {
                        actual[i] = ((Number) params[i]).intValue();
                    } else if( paramTypes[i] == long.class || paramTypes[i] == Long.class ) {
                        actual[i] = ((Number) params[i]).longValue();
                    } else if( paramTypes[i] == float.class || paramTypes[i] == Float.class ) {
                        actual[i] = ((Number) params[i]).floatValue();
                    } else if( paramTypes[i] == double.class || paramTypes[i] == Double.class ) {
                        actual[i] = ((Number) params[i]).doubleValue();
                    } else {
                        throw new IllegalArgumentException("Unable to coerce parameter: '" + parameters.get(i).prettyFEEL() + "'. Expected " + paramTypes[i] + " but found " + params[i].getClass());
                    }
                } else if ( params[i] instanceof String
                        && ((String) params[i]).length() == 1
                        && (paramTypes[i] == char.class || paramTypes[i] == Character.class) ) {
                    actual[i] = ((String) params[i]).charAt(0);
                } else if ( params[i] instanceof Boolean && paramTypes[i] == boolean.class ) {
                    // Because Boolean can be also null, boolean.class is not assignable from Boolean.class. So we must coerce this.
                    actual[i] = params[i];
                } else {
                    throw new IllegalArgumentException("Unable to coerce parameter: '" + parameters.get(i).prettyFEEL() + "'. Expected " + paramTypes[i] + " but found " + params[i].getClass());
                }
            }
        }
        return actual;
    }

    private Object[] adjustForVariableParameters(Class[] paramTypes, Object[] params) {
        if ( paramTypes.length > 0 && paramTypes[paramTypes.length - 1].isArray() && params.length >= paramTypes.length ) {
            // then it is a variable parameters function call
            Object[] newParams = new Object[paramTypes.length];
            if ( newParams.length > 1 ) {
                System.arraycopy( params, 0, newParams, 0, newParams.length - 1 );
            }
            Object[] remaining = new Object[params.length - paramTypes.length + 1];
            newParams[newParams.length - 1] = remaining;
            System.arraycopy( params, paramTypes.length - 1, remaining, 0, remaining.length );
            return newParams;
        }
        return params;
    }

    private String getSignature() {
        return getName() + "( " + parameters.stream().map(Param::getName).collect(Collectors.joining(", ")) + " )";
    }

    @Override
    protected boolean isCustomFunction() {
        return true;
    }

    @Override
    public String toString() {
        return "function " + getSignature();
    }
}
