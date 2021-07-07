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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.kie.dmn.api.feel.runtime.events.FEELEvent.Severity;
import org.kie.dmn.feel.lang.types.impl.InterceptNotComparableComparator;
import org.kie.dmn.feel.runtime.events.InvalidParametersEvent;

public class MaxFunction
        extends BaseFEELFunction {

    public MaxFunction() {
        super( "max" );
    }

    public FEELFnResult<Object> invoke(@ParameterName("list") List list) {
        if ( list == null || list.isEmpty() ) {
            return FEELFnResult.ofError(new InvalidParametersEvent(Severity.ERROR, "list", "cannot be null or empty"));
        } else {
            try {
                return FEELFnResult.ofResult(Collections.max(list, new InterceptNotComparableComparator()));
            } catch (ClassCastException e) {
                return FEELFnResult.ofError(new InvalidParametersEvent(Severity.ERROR, "list", "contains items that are not comparable"));
            }
        }
    }

    public FEELFnResult<Object> invoke(@ParameterName("c") Object[] list) {
        if ( list == null || list.length == 0 ) {
            // Arrays.asList does not accept null as parameter
            return FEELFnResult.ofError(new InvalidParametersEvent(Severity.ERROR, "c", "cannot be null or empty"));
        }
        
        return invoke( Arrays.asList( list ) );
    }

}
