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
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.kie.dmn.api.feel.runtime.events.FEELEvent.Severity;
import org.kie.dmn.feel.runtime.events.InvalidParametersEvent;

public class SplitFunction
        extends BaseFEELFunction {
    public static final SplitFunction INSTANCE = new SplitFunction();

    SplitFunction() {
        super( "split" );
    }

    public FEELFnResult<List<String>> invoke(@ParameterName("string") String string, @ParameterName("delimiter") String delimiter) {
        return invoke(string, delimiter, null);
    }

    public FEELFnResult<List<String>> invoke(@ParameterName("string") String string, @ParameterName("delimiter") String delimiter, @ParameterName("flags") String flags) {
        if (string == null) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "string", "cannot be null" ) );
        }
        if ( delimiter == null ) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "delimiter", "cannot be null" ) );
        }
        try {
            int f = processFlags( flags );
            Pattern p = Pattern.compile( delimiter, f );
            String[] split = p.split(string, -1);
            return FEELFnResult.ofResult( Arrays.asList( split ) );
        } catch ( PatternSyntaxException e ) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "delimiter", "is invalid and can not be compiled", e ) );
        } catch ( IllegalArgumentException t ) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "flags", "contains unknown flags", t ) );
        } catch ( Throwable t) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "delimiter", "is invalid and can not be compiled", t ) );
        }
    }

    private int processFlags(String flags) {
        int f = 0;
        if( flags != null ) {
            if( flags.contains( "s" ) ) {
                f |= Pattern.DOTALL;
            }
            if( flags.contains( "m" ) ) {
                f |= Pattern.MULTILINE;
            }
            if( flags.contains( "i" ) ) {
                f |= Pattern.CASE_INSENSITIVE;
            }
        }
        return f;
    }

}
