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

import org.kie.dmn.api.feel.runtime.events.FEELEvent.Severity;
import org.kie.dmn.feel.runtime.events.InvalidParametersEvent;
import org.kie.dmn.feel.util.RegexpUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MatchesFunction
        extends BaseFEELFunction {

    public MatchesFunction() {
        super( "matches" );
    }

    public FEELFnResult<Boolean> invoke(@ParameterName("input") String input, @ParameterName("pattern") String pattern) {
        return invoke( input, pattern, null );
    }

    public FEELFnResult<Boolean> invoke(@ParameterName("input") String input, @ParameterName("pattern") String pattern, @ParameterName("flags") String flags) {
        if ( input == null ) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "input", "cannot be null" ) );
        }
        if ( pattern == null ) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "pattern", "cannot be null" ) );
        }
        try {
            return FEELFnResult.ofResult( RegexpUtil.find(input, pattern, flags));
//            int f = processFlags( flags );
//            Pattern p = Pattern.compile( pattern, f );
//            Matcher m = p.matcher( input );
//            return FEELFnResult.ofResult( m.find() );
        } catch ( PatternSyntaxException e ) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "pattern", "is invalid and can not be compiled", e ) );
        } catch ( IllegalArgumentException t ) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "flags", "contains unknown flags", t ) );
        } catch ( Throwable t) {
            return FEELFnResult.ofError( new InvalidParametersEvent( Severity.ERROR, "pattern", "is invalid and can not be compiled", t ) );
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
