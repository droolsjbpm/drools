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

import org.kie.dmn.api.feel.runtime.events.FEELEvent.Severity;
import org.kie.dmn.feel.runtime.events.InvalidParametersEvent;
import org.kie.dmn.feel.runtime.functions.BaseFEELFunction;
import org.kie.dmn.feel.runtime.functions.FEELFnResult;
import org.kie.dmn.feel.runtime.functions.ParameterName;
import org.kie.dmn.feel.util.EvalHelper;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NNMedianFunction
        extends BaseFEELFunction {

    public static final NNMedianFunction INSTANCE = new NNMedianFunction();

    NNMedianFunction() {
        super("nn median");
    }

    public FEELFnResult<BigDecimal> invoke(@ParameterName("list") List<?> list) {
        if (list == null || list.isEmpty() ) {
            return FEELFnResult.ofResult(null);
        }

        List<BigDecimal> sorted = new ArrayList<>();
        for( int i = 0; i < list.size(); i++ ) {
            Object element = list.get( i );
            if( element != null && element instanceof Number ) {
                sorted.add(EvalHelper.getBigDecimalOrNull( element ) );
            } else if( element != null ) {
                return FEELFnResult.ofError(new InvalidParametersEvent(Severity.ERROR, "list", "contains element that is not a number"));
            }
        }
        Collections.sort( sorted );

        if (sorted.size() % 2 == 0) {
            // even sized list
            int medianPos0 = (sorted.size() / 2) - 1;
            int medianPos1 = medianPos0 + 1;
            if (!(sorted.get(medianPos0) instanceof BigDecimal) || !(sorted.get(medianPos1) instanceof BigDecimal)) {
                return FEELFnResult.ofError(new InvalidParametersEvent(Severity.ERROR, "list", "does not contain number"));
            }
            BigDecimal median0 = (BigDecimal) sorted.get(medianPos0);
            BigDecimal median1 = (BigDecimal) sorted.get(medianPos1);
            BigDecimal medianAvg = median0.add(median1).divide(new BigDecimal(2, MathContext.DECIMAL128), MathContext.DECIMAL128);
            return FEELFnResult.ofResult(medianAvg);
        } else {
            int medianPos = sorted.size() / 2;
            Object median = sorted.get(medianPos);
            if (!(median instanceof BigDecimal)) {
                return FEELFnResult.ofError(new InvalidParametersEvent(Severity.ERROR, "list", "does not contain number"));
            }
            return FEELFnResult.ofResult((BigDecimal) median);
        }
    }

    public FEELFnResult<BigDecimal> invoke(@ParameterName("n") Object[] list) {
        if ( list == null ) {
            return FEELFnResult.ofResult(null);
        }

        return invoke( Arrays.asList( list ) );
    }
}
