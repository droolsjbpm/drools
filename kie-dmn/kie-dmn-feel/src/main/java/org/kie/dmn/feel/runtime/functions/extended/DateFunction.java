package org.kie.dmn.feel.runtime.functions.extended;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Pattern;

import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.feel.runtime.events.InvalidParametersEvent;
import org.kie.dmn.feel.runtime.functions.BaseFEELFunction;
import org.kie.dmn.feel.runtime.functions.BuiltInFunctions;
import org.kie.dmn.feel.runtime.functions.DateAndTimeFunction;
import org.kie.dmn.feel.runtime.functions.FEELConversionFunctionNames;
import org.kie.dmn.feel.runtime.functions.FEELFnResult;
import org.kie.dmn.feel.runtime.functions.ParameterName;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

public class DateFunction extends BaseFEELFunction {

    public static final DateFunction INSTANCE = new DateFunction();

    private static final Pattern BEGIN_YEAR = Pattern.compile("^-?(([1-9]\\d\\d\\d+)|(0\\d\\d\\d))-"); // FEEL spec, "specified by XML Schema Part 2 Datatypes", hence: yearFrag ::= '-'? (([1-9] digit digit digit+)) | ('0' digit digit digit))
    private static final DateTimeFormatter FEEL_DATE;

    static {
        FEEL_DATE = new DateTimeFormatterBuilder().appendValue(YEAR, 4, 9, SignStyle.NORMAL)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(DAY_OF_MONTH, 2)
                .toFormatter()
                .withResolverStyle(ResolverStyle.STRICT);
    }

    DateFunction() {
        super(FEELConversionFunctionNames.DATE);
    }

    public FEELFnResult<TemporalAccessor> invoke(@ParameterName("from") String val) {
        if (val == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "cannot be null"));
        }
        if (!BEGIN_YEAR.matcher(val).find()) { // please notice the regex strictly requires the beginning, so we can use find.
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "year not compliant with XML Schema Part 2 Datatypes"));
        }

        try {
            return FEELFnResult.ofResult(LocalDate.from(FEEL_DATE.parse(val)));
        } catch (DateTimeException e) {
            // try to parse it as a date time and extract the date component
            // NOTE: this is an extension to the standard
            return BuiltInFunctions.getFunction(DateAndTimeFunction.class).invoke(val)
                    .cata(overrideLeft -> FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "date-parsing exception", e)),
                            this::invoke
                    );
        }
    }

    public FEELFnResult<TemporalAccessor> invoke(@ParameterName("year") Number year, @ParameterName("month") Number month, @ParameterName("day") Number day) {
        if (year == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "year", "cannot be null"));
        }
        if (month == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "month", "cannot be null"));
        }
        if (day == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "day", "cannot be null"));
        }

        try {
            return FEELFnResult.ofResult(LocalDate.of(year.intValue(), month.intValue(), day.intValue()));
        } catch (DateTimeException e) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "input parameters date-parsing exception", e));
        }
    }

    public FEELFnResult<TemporalAccessor> invoke(@ParameterName("from") TemporalAccessor date) {
        if (date == null) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "cannot be null"));
        }

        try {
            return FEELFnResult.ofResult(LocalDate.from(date));
        } catch (DateTimeException e) {
            return FEELFnResult.ofError(new InvalidParametersEvent(FEELEvent.Severity.ERROR, "from", "date-parsing exception", e));
        }
    }
}
