package org.kie.dmn.feel.runtime.functions.extended;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.List;

import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.feel.lang.types.impl.ComparablePeriod;
import org.kie.dmn.feel.runtime.events.InvalidParametersEvent;
import org.kie.dmn.feel.runtime.functions.BaseFEELFunction;
import org.kie.dmn.feel.runtime.functions.FEELConversionFunctionNames;
import org.kie.dmn.feel.runtime.functions.FEELFnResult;
import org.kie.dmn.feel.runtime.functions.ParameterName;

public class DurationFunction extends BaseFEELFunction {
    public static final DurationFunction INSTANCE = new DurationFunction();

    DurationFunction() {
        super(FEELConversionFunctionNames.DURATION);
    }

    public FEELFnResult<TemporalAmount> invoke(@ParameterName( "from" ) String val) {
        if ( val == null ) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "cannot be null"));
        }

        try {
            // try to parse as days/hours/minute/seconds
            return FEELFnResult.ofResult( Duration.parse( val ) );
        } catch( DateTimeParseException e ) {
            // if it failed, try to parse as years/months
            try {
                return FEELFnResult.ofResult(ComparablePeriod.parse(val).normalized());
            } catch( DateTimeParseException e2 ) {
                // failed to parse, so return null according to the spec
                return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "date-parsing exception",
                        new RuntimeException(new Throwable() { public final List<Throwable> causes = Arrays.asList( new Throwable[]{e, e2} );  } )));
            }
        }

    }

    public FEELFnResult<TemporalAmount> invoke(@ParameterName( "from" ) TemporalAmount val) {
        if ( val == null ) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "cannot be null"));
        }
        return FEELFnResult.ofResult( val );
    }

}
