package org.kie.dmn.feel.runtime.functions.extended;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;

import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.feel.runtime.events.InvalidParametersEvent;
import org.kie.dmn.feel.runtime.functions.BaseFEELFunction;
import org.kie.dmn.feel.runtime.functions.BuiltInFunctions;
import org.kie.dmn.feel.runtime.functions.DateAndTimeFunction;
import org.kie.dmn.feel.runtime.functions.FEELConversionFunctionNames;
import org.kie.dmn.feel.runtime.functions.FEELFnResult;
import org.kie.dmn.feel.runtime.functions.ParameterName;

public class TimeFunction extends BaseFEELFunction {
    public static final TimeFunction INSTANCE = new TimeFunction();

    private static final DateTimeFormatter FEEL_TIME;

    static {
        FEEL_TIME = new DateTimeFormatterBuilder().parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .optionalStart()
                .appendLiteral("@")
                .appendZoneRegionId()
                .optionalEnd()
                .optionalStart()
                .appendOffsetId()
                .optionalEnd()
                .toFormatter()
                .withResolverStyle(ResolverStyle.STRICT);
    }

    TimeFunction() {
        super(FEELConversionFunctionNames.TIME);
    }

    public FEELFnResult<TemporalAccessor> invoke(@ParameterName("from") String val) {
        if (val == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "cannot be null"));
        }

        try {
            TemporalAccessor parsed = FEEL_TIME.parse(val);

            if (parsed.query(TemporalQueries.offset()) != null) {
                // it is an offset-zoned time, so I can know for certain an OffsetTime
                OffsetTime asOffSetTime = parsed.query(OffsetTime::from);
                return FEELFnResult.ofResult(asOffSetTime);
            } else if (parsed.query(TemporalQueries.zone()) == null) {
                // if it does not contain any zone information at all, then I know for certain is a local time.
                LocalTime asLocalTime = parsed.query(LocalTime::from);
                return FEELFnResult.ofResult(asLocalTime);
            }

            return FEELFnResult.ofResult(parsed);
        } catch (DateTimeException e) {
            // try to parse it as a date time and extract the date component
            // NOTE: this is an extension to the standard
            return BuiltInFunctions.getFunction(DateAndTimeFunction.class).invoke(val)
                    .cata(overrideLeft -> FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "time-parsing exception", e)),
                            this::invoke
                    );
        }
    }

    private static final BigDecimal NANO_MULT = BigDecimal.valueOf(1000000000);

    public FEELFnResult<TemporalAccessor> invoke(
            @ParameterName("hour") Number hour, @ParameterName("minute") Number minute,
            @ParameterName("second") Number seconds) {
        return invoke(hour, minute, seconds, null);
    }

    public FEELFnResult<TemporalAccessor> invoke(
            @ParameterName("hour") Number hour, @ParameterName("minute") Number minute,
            @ParameterName("second") Number seconds, @ParameterName("offset") Duration offset) {
        if (hour == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "hour", "cannot be null"));
        }
        if (minute == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "minute", "cannot be null"));
        }
        if (seconds == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "seconds", "cannot be null"));
        }

        try {
            int nanosecs = 0;
            if (seconds instanceof BigDecimal) {
                BigDecimal secs = (BigDecimal) seconds;
                nanosecs = secs.subtract(secs.setScale(0, BigDecimal.ROUND_DOWN)).multiply(NANO_MULT).intValue();
            }

            if (offset == null) {
                return FEELFnResult.ofResult(LocalTime.of(hour.intValue(), minute.intValue(), seconds.intValue(),
                        nanosecs));
            } else {
                return FEELFnResult.ofResult(OffsetTime.of(hour.intValue(), minute.intValue(), seconds.intValue(),
                        nanosecs,
                        ZoneOffset.ofTotalSeconds((int) offset.getSeconds())));
            }
        } catch (DateTimeException e) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "time-parsing exception", e));
        }
    }

    public FEELFnResult<TemporalAccessor> invoke(@ParameterName("from") TemporalAccessor date) {
        if (date == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "cannot be null"));
        }

        try {
            // If the temporal accessor type doesn't support time, try to parse it as a date with UTC midnight.
            if (!date.isSupported(ChronoField.HOUR_OF_DAY)) {
                return BuiltInFunctions.getFunction( DateAndTimeFunction.class ).invoke( date, OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC) )
                        .cata( overrideLeft -> FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "time-parsing exception")),
                                this::invoke
                        );
            } else if( date.query( TemporalQueries.offset() ) == null ) {
                return FEELFnResult.ofResult(LocalTime.from(date));
            } else {
                ZoneId zone = date.query(TemporalQueries.zoneId());
                if (!(zone instanceof ZoneOffset)) {
                    // TZ is a ZoneRegion, so do NOT normalize (although the result will be unreversible, but will keep what was supplied originally).
                    // Unfortunately java.time.Parsed is a package-private class, hence will need to re-parse in order to have it instantiated.
                    return invoke(date.query(TemporalQueries.localTime()) + "@" + zone);
                } else {
                    return FEELFnResult.ofResult(OffsetTime.from(date));
                }
            }
        } catch (DateTimeException e) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "time-parsing exception", e));
        }
    }
}
