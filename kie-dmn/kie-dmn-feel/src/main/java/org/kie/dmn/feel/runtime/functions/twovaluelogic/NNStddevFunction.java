/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.kie.dmn.feel.runtime.functions.twovaluelogic;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.feel.runtime.events.InvalidParametersEvent;
import org.kie.dmn.feel.runtime.functions.BaseFEELFunction;
import org.kie.dmn.feel.runtime.functions.FEELFnResult;
import org.kie.dmn.feel.runtime.functions.ParameterName;
import org.kie.dmn.feel.runtime.functions.SqrtFunction;
import org.kie.dmn.feel.util.EvalHelper;

// based on the examples of calculations, stddev is supposed to return sample standard deviation, not population standard deviation
public class NNStddevFunction
        extends BaseFEELFunction {
    public static final NNStddevFunction INSTANCE = new NNStddevFunction();

    NNStddevFunction() {
        super("nn stddev");
    }

    public FEELFnResult<BigDecimal> invoke(@ParameterName("list") List<?> list) {
        if ( list == null ) {
            return FEELFnResult.ofResult( null );
        }
        List<BigDecimal> numbers = new ArrayList<>( list.size() );
        for ( int i = 0; i < list.size(); i++ ) {
            Object value = list.get(i);
            if( value == null ) {
                // ignore null elements
                continue;
            }
            final BigDecimal val = EvalHelper.getBigDecimalOrNull(value);
            if ( val == null ) {
                // coercion to number failed
                return FEELFnResult.ofError( new InvalidParametersEvent( FEELEvent.Severity.ERROR, "list", "an element in the list is not suitable for the stddev" ) );
            }
            numbers.add( val );
        }
        if( numbers.isEmpty() ) {
            return FEELFnResult.ofResult( null );
        }
        int count = numbers.size();
        BigDecimal total = BigDecimal.ZERO;
        for ( int i = 0; i < count; i++ ) {
            total = total.add( numbers.get( i ) );
        }
        BigDecimal mean = total.divide( BigDecimal.valueOf( numbers.size() ), MathContext.DECIMAL128 );
        total = BigDecimal.ZERO;
        for ( int i = 0; i < count; i++ ) {
            BigDecimal distanceSquared = numbers.get(i).subtract( mean ).pow( 2, MathContext.DECIMAL128 );
            total = total.add( distanceSquared );
        }
        mean = total.divide( BigDecimal.valueOf( count - 1L ), MathContext.DECIMAL128 );
        return FEELFnResult.ofResult( SqrtFunction.sqrt( mean ) );
    }

    public FEELFnResult<BigDecimal> invoke(@ParameterName("list") Object sole) {
        if ( sole == null ) {
            return FEELFnResult.ofResult( null );
        } else if( EvalHelper.getBigDecimalOrNull( sole ) == null ) {
            return FEELFnResult.ofError( new InvalidParametersEvent( FEELEvent.Severity.ERROR, "list",
                    "the value can not be converted to a number" ) );
        }
        return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "list",
                                                               "sample standard deviation of a single sample is undefined"));
    }

    public FEELFnResult<BigDecimal> invoke(@ParameterName("n") Object[] list) {
        if ( list == null ) {
            return FEELFnResult.ofResult( null );
        }

        return invoke( Arrays.asList( list ) );
    }
}
