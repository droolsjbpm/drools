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

package org.kie.dmn.feel.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.kie.dmn.api.core.FEELPropertyAccessible;
import org.kie.dmn.feel.lang.EvaluationContext;
import org.kie.dmn.feel.lang.FEELProperty;
import org.kie.dmn.feel.lang.types.BuiltInType;
import org.kie.dmn.feel.lang.types.impl.ComparablePeriod;
import org.kie.dmn.feel.runtime.Range;
import org.kie.dmn.feel.runtime.Range.RangeBoundary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvalHelper {
    public static final Logger LOG = LoggerFactory.getLogger( EvalHelper.class );

    private static final Map<String, Method> accessorCache = new ConcurrentHashMap<>();

    public static String normalizeVariableName(String name) {
        // private static final Pattern SPACES_PATTERN = Pattern.compile( "[\\s\u00A0]+" );
        // return SPACES_PATTERN.matcher( name.trim() ).replaceAll( " " );

        // The above code was refactored for performance reasons
        // Check org.drools.benchmarks.dmn.runtime.DMNEvaluateDecisionNameLengthBenchmark
        // This method tries to return the original String whenever possible to avoid allocation of char[]

        if (name == null || name.isEmpty()) {
            return name;
        }

        // Find the first valid char, used to skip leading spaces
        int firstValid = 0, size = name.length();

        for (; firstValid < size; firstValid++) {
            if (isValidChar(name.charAt(firstValid))) {
                break;
            }
        }
        if (firstValid == size) {
            return "";
        }

        // Finds the last valid char, either before a non-regular space, the first of multiple spaces or the last char
        int lastValid = 0, trailing = 0;
        boolean inWhitespace = false;

        for (int i = firstValid; i < size; i++) {
            if (isValidChar(name.charAt(i))) {
                lastValid = i + 1;
                inWhitespace = false;
            } else {
                if (inWhitespace) {
                    break;
                }
                inWhitespace = true;
                if (name.charAt(i) != ' ') {
                    break;
                }
            }
        }

        // Counts the number of spaces after 'lastValid' (to remove possible trailing spaces)
        for (int i = lastValid; i < size && !isValidChar(name.charAt(i)); i++) {
            trailing++;
        }
        if (lastValid + trailing == size) {
            return firstValid != 0 || trailing != 0 ? name.substring(firstValid, lastValid) : name;
        }

        // There are valid chars after 'lastValid' and substring won't do (full normalization is required)
        int pos = 0;
        char[] target = new char[size-firstValid];

        // Copy the chars know to be valid to the new array
        for (int i = firstValid; i < lastValid; i++) {
            target[pos++] = name.charAt(i);
        }

        // Copy valid chars after 'lastValid' to new array
        // Many whitespaces are collapsed into one and trailing spaces are ignored
        for (int i = lastValid + 1; i < size; i++) {
            char c = name.charAt(i);
            if (isValidChar(c)) {
                if (inWhitespace) {
                    target[pos++] = ' ';
                }
                target[pos++] = c;
                inWhitespace = false;
            } else {
                inWhitespace = true;
            }
        }
        return new String(target, 0, pos);
    }

    /**
     * This method defines what characters are valid for the output of normalizeVariableName. Spaces and control characters are invalid.
     * There is a fast-path for well known characters
     */
    private static boolean isValidChar(char c) {
        if ( c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' ) {
            return true;
        }
        return c != ' ' && c != '\u00A0' && !Character.isWhitespace(c);
    }

    public static BigDecimal getBigDecimalOrNull(Object value) {
        if ( value == null ||
                !(value instanceof Number
                || value instanceof String)
                || (value instanceof Double
                && (value.toString().equals("NaN") || value.toString().equals("Infinity") || value.toString().equals("-Infinity"))) ) {
            return null;
        }
        if ( !ClassUtil.isAssignableFrom(BigDecimal.class, value.getClass() ) ) {
            if ( value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte ||
                 value instanceof AtomicLong || value instanceof AtomicInteger ) {
                value = new BigDecimal( ((Number) value).longValue(), MathContext.DECIMAL128 );
            } else if ( value instanceof BigInteger ) {
                value = new BigDecimal( (BigInteger) value, MathContext.DECIMAL128 );
            } else if ( value instanceof String ) {
                try {
                    // we need to remove leading zeros to prevent octal conversion
                    value = new BigDecimal( ((String) value).replaceFirst("^0+(?!$)", ""), MathContext.DECIMAL128 );
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                // doubleValue() sometimes produce rounding errors, so we need to use toString() instead
                // We also need to remove trailing zeros, if there are some so for 10d we get BigDecimal.valueOf(10)
                // instead of BigDecimal.valueOf(10.0).
                value = new BigDecimal( removeTrailingZeros(value.toString()), MathContext.DECIMAL128 );
            }
        }
        return (BigDecimal) value;
    }

    public static Object coerceNumber(Object value) {
        if ( value instanceof Number && !(value instanceof BigDecimal) ) {
            return getBigDecimalOrNull( value );
        } else {
            return value;
        }
    }

    public static Boolean getBooleanOrNull(Object value) {
        if ( value == null || !(value instanceof Boolean) ) {
            return null;
        }
        return (Boolean) value;
    }

    public static String unescapeString(String text) {
        if ( text == null ) {
            return null;
        }
        if ( text.length() >= 2 && text.startsWith( "\"" ) && text.endsWith( "\"" ) ) {
            // remove the quotes
            text = text.substring( 1, text.length() - 1 );
        }
        if ( text.indexOf( '\\' ) >= 0 ) {
            // might require un-escaping
            StringBuilder r = new StringBuilder();
            for ( int i = 0; i < text.length(); i++ ) {
                char c = text.charAt( i );
                if ( c == '\\' ) {
                    if ( text.length() > i + 1 ) {
                        i++;
                        char cn = text.charAt( i );
                        switch ( cn ) {
                            case 'b':
                                r.append( '\b' );
                                break;
                            case 't':
                                r.append( '\t' );
                                break;
                            case 'n':
                                r.append( '\n' );
                                break;
                            case 'f':
                                r.append( '\f' );
                                break;
                            case 'r':
                                r.append( '\r' );
                                break;
                            case '"':
                                r.append( '"' );
                                break;
                            case '\'':
                                r.append( '\'' );
                                break;
                            case '\\':
                                r.append( '\\' );
                                break;
                            case 'u':
                                if ( text.length() >= i + 5 ) {
                                    // escape unicode
                                    String hex = text.substring( i + 1, i + 5 );
                                    char[] chars = Character.toChars( Integer.parseInt( hex, 16 ) );
                                    r.append( chars );
                                    i += 4;
                                } else {
                                    // not really unicode
                                    r.append( "\\" ).append( cn );
                                }
                                break;
                            case 'U':
                                if ( text.length() >= i + 7 ) {
                                    // escape unicode
                                    String hex = text.substring( i + 1, i + 7 );
                                    char[] chars = Character.toChars( Integer.parseInt( hex, 16 ) );
                                    r.append( chars );
                                    i += 6;
                                } else {
                                    // not really unicode
                                    r.append( "\\" ).append( cn );
                                }
                                break;
                            default:
                                r.append( "\\" ).append( cn );
                        }
                    } else {
                        r.append( c );
                    }
                } else {
                    r.append( c );
                }
            }
            text = r.toString();
        }
        return text;
    }

    public static class PropertyValueResult implements FEELPropertyAccessible.AbstractPropertyValueResult {

        private final boolean defined;
        private final Either<Exception, Object> valueResult;

        private PropertyValueResult(boolean isDefined, Either<Exception, Object> value) {
            this.defined = isDefined;
            this.valueResult = value;
        }

        public static PropertyValueResult notDefined() {
            return new PropertyValueResult(false, Either.ofLeft(new UnsupportedOperationException("Property was not defined.")));
        }

        public static PropertyValueResult of(Either<Exception, Object> valueResult) {
            return new PropertyValueResult(true, valueResult);
        }

        public static PropertyValueResult ofValue(Object value) {
            return new PropertyValueResult(true, Either.ofRight(value));
        }

        public boolean isDefined() {
            return defined;
        }

        public Either<Exception, Object> getValueResult() {
            return valueResult;
        }

        @Override
        public Optional<Object> toOptional() {
            return valueResult.cata(l -> Optional.empty(), Optional::ofNullable);
        }

    }

    public static PropertyValueResult getDefinedValue(final Object current, final String property) {
        Object result;
        if ( current == null ) {
            return PropertyValueResult.notDefined();
        } else if ( current instanceof Map ) {
            result = ((Map) current).get(property);
            if (result == null) {
                // most cases "result" will be defined, so checking here only in case null was to signify missing key altogether.
                if (!((Map) current).containsKey(property)) {
                    return PropertyValueResult.notDefined();
                }
            }
        } else if (current instanceof ChronoPeriod) {
            switch ( property ) {
                case "years":
                    result = ((ChronoPeriod) current).get(ChronoUnit.YEARS);
                    break;
                case "months":
                    result = ((ChronoPeriod) current).get(ChronoUnit.MONTHS) % 12;
                    break;
                default:
                    return PropertyValueResult.notDefined();
            }
        } else if ( current instanceof Duration ) {
            switch ( property ) {
                case "days":
                    result = ((Duration) current).toDays();
                    break;
                case "hours":
                    result = ((Duration) current).toHours() % 24;
                    break;
                case "minutes":
                    result = ((Duration) current).toMinutes() % 60;
                    break;
                case "seconds":
                    result = ((Duration) current).getSeconds() % 60;
                    break;
                default:
                    return PropertyValueResult.notDefined();
            }
        } else if (current instanceof TemporalAccessor) {
            switch ( property ) {
                case "year":
                    result = ((TemporalAccessor) current).get(ChronoField.YEAR);
                    break;
                case "month":
                    result = ((TemporalAccessor) current).get(ChronoField.MONTH_OF_YEAR);
                    break;
                case "day":
                    result = ((TemporalAccessor) current).get(ChronoField.DAY_OF_MONTH);
                    break;
                case "hour":
                    result = ((TemporalAccessor) current).get(ChronoField.HOUR_OF_DAY);
                    break;
                case "minute":
                    result = ((TemporalAccessor) current).get(ChronoField.MINUTE_OF_HOUR);
                    break;
                case "second":
                    result = ((TemporalAccessor) current).get(ChronoField.SECOND_OF_MINUTE);
                    break;
                case "time offset":
                    if (((TemporalAccessor) current).isSupported(ChronoField.OFFSET_SECONDS)) {
                        result = Duration.ofSeconds(((TemporalAccessor) current).get(ChronoField.OFFSET_SECONDS));
                    } else {
                        result = null;
                    }
                    break;
                case "timezone":
                    ZoneId zoneId = ((TemporalAccessor) current).query(TemporalQueries.zoneId());
                    if (zoneId != null) {
                        result = TimeZone.getTimeZone(zoneId).getID();
                        break;
                    } else {
                        return PropertyValueResult.notDefined();
                    }
                case "weekday":
                    result = ((TemporalAccessor) current).get(ChronoField.DAY_OF_WEEK);
                    break;
                default:
                    return PropertyValueResult.notDefined();
            }
        } else if (current instanceof Range) {
            switch (property) {
                case "start included":
                    result = ((Range) current).getLowBoundary() == RangeBoundary.CLOSED ? Boolean.TRUE : Boolean.FALSE;
                    break;
                case "start":
                    result = ((Range) current).getLowEndPoint();
                    break;
                case "end":
                    result = ((Range) current).getHighEndPoint();
                    break;
                case "end included":
                    result = ((Range) current).getHighBoundary() == RangeBoundary.CLOSED ? Boolean.TRUE : Boolean.FALSE;
                    break;
                default:
                    return PropertyValueResult.notDefined();
            }
        } else {
            Method getter = getGenericAccessor( current.getClass(), property );
            if ( getter != null ) {
                try {
                    result = getter.invoke(current);
                    if (result instanceof Character) {
                        result = result.toString();
                    } else if ( result instanceof java.util.Date ) {
                        result = java.time.Instant.ofEpochMilli(((java.util.Date) result).getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                    return PropertyValueResult.of(Either.ofLeft(e));
                }
            } else {
                // WORST-CASE: if code reached here, means that "property" is not defined on the "current" object at all.
                return PropertyValueResult.notDefined();
            }
        }

        // before returning, coerce "result" into number.
        result = coerceNumber(result);

        return PropertyValueResult.ofValue(result);
    }

    /**
     * {@link #getDefinedValue(Object, String)} method instead.
     * @deprecated this method cannot distinguish null because: 1. property undefined for current, 2. an error, 3. a properly defined property value valorized to null. 
     * 
     */
    public static Object getValue(final Object current, final String property) {
        return getDefinedValue(current, property).getValueResult().getOrElse(null);
    }

    /**
     * FEEL annotated or else Java accessor.
     * @param clazz
     * @param field
     * @return
     */
    public static Method getGenericAccessor(Class<?> clazz, String field) {
        LOG.trace( "getGenericAccessor({}, {})", clazz, field );

        String accessorQualifiedName = new StringBuilder(clazz.getCanonicalName())
			.append(".").append(field).toString();

        return accessorCache.computeIfAbsent(accessorQualifiedName, key ->
        	Stream.of(ClassUtil.getMethods(clazz) )
            .filter( m -> Optional.ofNullable( m.getAnnotation( FEELProperty.class ) )
                    .map( ann -> ann.value().equals( field ) )
                    .orElse( false )
            )
            .findFirst()
            .orElse( getAccessor( clazz, field ) ));
    }

    public static void clearGenericAccessorCache() {
        accessorCache.clear();
    }

    /**
     * JavaBean -spec compliant accessor.
     * @param clazz
     * @param field
     * @return
     */
    public static Method getAccessor(Class<?> clazz, String field) {
        LOG.trace( "getAccessor({}, {})", clazz, field );
        try {
            return ClassUtil.getMethod(clazz, "get" + ucFirst(field ) );
        } catch ( NoSuchMethodException e ) {
            try {
                return ClassUtil.getMethod(clazz, field );
            } catch ( NoSuchMethodException e1 ) {
                try {
                    return ClassUtil.getMethod(clazz, "is" + ucFirst(field ) );
                } catch ( NoSuchMethodException e2 ) {
                    return null;
                }
            }
        }
    }

    /**
     * Inverse of {@link #getAccessor(Class, String)}
     */
    public static Optional<String> propertyFromAccessor(Method accessor) {
        if ( accessor.getParameterCount() != 0 || accessor.getReturnType().equals( Void.class ) ) {
            return Optional.empty();
        }
        String methodName = accessor.getName();
        if ( methodName.startsWith( "get" ) ) {
            return Optional.of( lcFirst( methodName.substring( 3, methodName.length() ) ) );
        } else if ( methodName.startsWith( "is" ) ) {
            return Optional.of( lcFirst( methodName.substring( 2, methodName.length() ) ) );
        } else {
            return Optional.of( lcFirst( methodName ) );
        }
    }

    public static String ucFirst(final String name) {
        return name.toUpperCase().charAt( 0 ) + name.substring( 1 );
    }

    public static String lcFirst(final String name) {
        return name.toLowerCase().charAt( 0 ) + name.substring( 1 );
    }

    /**
     * Compares left and right operands using the given predicate and returns TRUE/FALSE accordingly
     *
     * @param left
     * @param right
     * @param ctx
     * @param op
     * @return
     */
    public static Boolean compare(Object left, Object right, EvaluationContext ctx, BiPredicate<Comparable, Comparable> op) {
        if ( left == null || right == null ) {
            return null;
        } else if (left instanceof ChronoPeriod && right instanceof ChronoPeriod) {
            // periods have special compare semantics in FEEL as it ignores "days". Only months and years are compared
            Long l = ComparablePeriod.toTotalMonths((ChronoPeriod) left);
            Long r = ComparablePeriod.toTotalMonths((ChronoPeriod) right);
            return op.test( l, r );
        } else if (left instanceof TemporalAccessor && right instanceof TemporalAccessor) {
            // Handle specific cases when both time / datetime
            TemporalAccessor l = (TemporalAccessor) left;
            TemporalAccessor r = (TemporalAccessor) right;
            if (BuiltInType.determineTypeFromInstance(left) == BuiltInType.TIME && BuiltInType.determineTypeFromInstance(right) == BuiltInType.TIME) {
                return op.test(valuet(l), valuet(r));
            } else if (BuiltInType.determineTypeFromInstance(left) == BuiltInType.DATE_TIME && BuiltInType.determineTypeFromInstance(right) == BuiltInType.DATE_TIME) {
                return op.test(valuedt(l, r.query(TemporalQueries.zone())), valuedt(r, l.query(TemporalQueries.zone())));
            } // fallback; continue:
        }
        // last fallback:
        if ((left instanceof String && right instanceof String) ||
            (left instanceof Number && right instanceof Number) ||
            (left instanceof Boolean && right instanceof Boolean) ||
            (left instanceof Comparable && ClassUtil.isAssignableFrom(left.getClass(), right.getClass()))) {
            Comparable l = (Comparable) left;
            Comparable r = (Comparable) right;
            return op.test(l, r);
        }
        return null;
    }

    /**
     * Compares left and right for equality applying FEEL semantics to specific data types
     *
     * @param left
     * @param right
     * @param ctx
     * @return
     */
    public static Boolean isEqual(Object left, Object right, EvaluationContext ctx ) {
        if ( left == null || right == null ) {
            return left == right;
        }

        // spec defines that "a=[a]", i.e., singleton collections should be treated as the single element
        // and vice-versa
        if( left instanceof Collection && !(right instanceof Collection) && ((Collection)left).size() == 1 ) {
            left = ((Collection)left).toArray()[0];
        } else if( right instanceof Collection && !(left instanceof Collection) && ((Collection)right).size()==1 ) {
            right = ((Collection) right).toArray()[0];
        }

        if( left instanceof Range && right instanceof Range ) {
            return isEqual( (Range)left, (Range) right );
        } else if( left instanceof Iterable && right instanceof Iterable ) {
            return isEqual( (Iterable)left, (Iterable) right );
        } else if( left instanceof Map && right instanceof Map ) {
            return isEqual( (Map)left, (Map) right );
        } else if (left instanceof ChronoPeriod && right instanceof ChronoPeriod) {
            // periods have special compare semantics in FEEL as it ignores "days". Only months and years are compared
            Long l = ComparablePeriod.toTotalMonths((ChronoPeriod) left);
            Long r = ComparablePeriod.toTotalMonths((ChronoPeriod) right);
            return isEqual(l, r);
        } else if (left instanceof TemporalAccessor && right instanceof TemporalAccessor) {
            // Handle specific cases when both time / datetime
            TemporalAccessor l = (TemporalAccessor) left;
            TemporalAccessor r = (TemporalAccessor) right;
            if (BuiltInType.determineTypeFromInstance(left) == BuiltInType.TIME && BuiltInType.determineTypeFromInstance(right) == BuiltInType.TIME) {
                return isEqual(valuet(l), valuet(r));
            } else if (BuiltInType.determineTypeFromInstance(left) == BuiltInType.DATE_TIME && BuiltInType.determineTypeFromInstance(right) == BuiltInType.DATE_TIME) {
                return isEqual(valuedt(l, r.query(TemporalQueries.zone())), valuedt(r, l.query(TemporalQueries.zone())));
            } // fallback; continue:
        }
        return compare( left, right, ctx, (l, r) -> l.compareTo( r ) == 0  );
    }

    /**
     * DMNv1.2 10.3.2.3.6 date-time, valuedt(date and time), for use in this {@link EvalHelper#compare(Object, Object, EvaluationContext, BiPredicate)}
     * DMNv1.3 also used for equality DMN13-35
     */
    private static long valuedt(TemporalAccessor datetime, ZoneId otherTimezoneOffset) {
        ZoneId alternativeTZ = Optional.ofNullable(otherTimezoneOffset).orElse(ZoneOffset.UTC);
        if (datetime instanceof LocalDateTime) {
            return ((LocalDateTime) datetime).atZone(alternativeTZ).toEpochSecond();
        } else if (datetime instanceof ZonedDateTime) {
            return ((ZonedDateTime) datetime).toEpochSecond();
        } else if (datetime instanceof OffsetDateTime) {
            return ((OffsetDateTime) datetime).toEpochSecond();
        } else {
            throw new RuntimeException("valuedt() for " + datetime + " but is not a FEEL date and time " + datetime.getClass());
        }
    }

    /**
     * DMNv1.2 10.3.2.3.4 time, valuet(time), for use in this {@link EvalHelper#compare(Object, Object, EvaluationContext, BiPredicate)}
     * DMNv1.3 also used for equality DMN13-35
     */
    private static long valuet(TemporalAccessor time) {
        long result = 0;
        result += time.get(ChronoField.HOUR_OF_DAY) * (60 * 60);
        result += time.get(ChronoField.MINUTE_OF_HOUR) * (60);
        result += time.get(ChronoField.SECOND_OF_MINUTE);
        return result;
    }

    /**
     * DMNv1.2 Table 48: Specific semantics of equality
     * DMNv1.3 Table 71: Semantic of date and time functions
     */
    public static Boolean isEqualDateTimeInSemanticD(TemporalAccessor left, TemporalAccessor right) {
        boolean result = true;
        Optional<Integer> lY = Optional.ofNullable(left.isSupported(ChronoField.YEAR) ? left.get(ChronoField.YEAR) : null);
        Optional<Integer> rY = Optional.ofNullable(right.isSupported(ChronoField.YEAR) ? right.get(ChronoField.YEAR) : null);
        result &= lY.equals(rY);
        Optional<Integer> lM = Optional.ofNullable(left.isSupported(ChronoField.MONTH_OF_YEAR) ? left.get(ChronoField.MONTH_OF_YEAR) : null);
        Optional<Integer> rM = Optional.ofNullable(right.isSupported(ChronoField.MONTH_OF_YEAR) ? right.get(ChronoField.MONTH_OF_YEAR) : null);
        result &= lM.equals(rM);
        Optional<Integer> lD = Optional.ofNullable(left.isSupported(ChronoField.DAY_OF_MONTH) ? left.get(ChronoField.DAY_OF_MONTH) : null);
        Optional<Integer> rD = Optional.ofNullable(right.isSupported(ChronoField.DAY_OF_MONTH) ? right.get(ChronoField.DAY_OF_MONTH) : null);
        result &= lD.equals(rD);
        result &= isEqualTimeInSemanticD(left, right);
        return result;
    }

    /**
     * DMNv1.2 Table 48: Specific semantics of equality
     * DMNv1.3 Table 71: Semantic of date and time functions
     */
    public static Boolean isEqualTimeInSemanticD(TemporalAccessor left, TemporalAccessor right) {
        boolean result = true;
        Optional<Integer> lH = Optional.ofNullable(left.isSupported(ChronoField.HOUR_OF_DAY) ? left.get(ChronoField.HOUR_OF_DAY) : null);
        Optional<Integer> rH = Optional.ofNullable(right.isSupported(ChronoField.HOUR_OF_DAY) ? right.get(ChronoField.HOUR_OF_DAY) : null);
        result &= lH.equals(rH);
        Optional<Integer> lM = Optional.ofNullable(left.isSupported(ChronoField.MINUTE_OF_HOUR) ? left.get(ChronoField.MINUTE_OF_HOUR) : null);
        Optional<Integer> rM = Optional.ofNullable(right.isSupported(ChronoField.MINUTE_OF_HOUR) ? right.get(ChronoField.MINUTE_OF_HOUR) : null);
        result &= lM.equals(rM);
        Optional<Integer> lS = Optional.ofNullable(left.isSupported(ChronoField.SECOND_OF_MINUTE) ? left.get(ChronoField.SECOND_OF_MINUTE) : null);
        Optional<Integer> rS = Optional.ofNullable(right.isSupported(ChronoField.SECOND_OF_MINUTE) ? right.get(ChronoField.SECOND_OF_MINUTE) : null);
        result &= lS.equals(rS);
        Optional<ZoneId> lTZ = Optional.ofNullable(left.query(TemporalQueries.zone()));
        Optional<ZoneId> rTZ = Optional.ofNullable(right.query(TemporalQueries.zone()));
        result &= lTZ.equals(rTZ);
        return result;
    }

    private static Boolean isEqual(Range left, Range right) {
        return left.equals( right );
    }

    private static Boolean isEqual(Iterable left, Iterable right) {
        Iterator li = left.iterator();
        Iterator ri = right.iterator();
        while( li.hasNext() && ri.hasNext() ) {
            Object l = li.next();
            Object r = ri.next();
            if ( !isEqual( l, r ) ) return false;
        }
        return li.hasNext() == ri.hasNext();
    }

    private static Boolean isEqual(Map<?,?> left, Map<?,?> right) {
        if( left.size() != right.size() ) {
            return false;
        }
        for( Map.Entry le : left.entrySet() ) {
            Object l = le.getValue();
            Object r = right.get( le.getKey() );
            if ( !isEqual( l, r ) ) return false;
        }
        return true;
    }

    private static Boolean isEqual(Object l, Object r) {
        if( l instanceof Iterable && r instanceof Iterable && !isEqual( (Iterable) l, (Iterable) r ) ) {
            return false;
        } else if( l instanceof Map && r instanceof Map && !isEqual( (Map) l, (Map) r ) ) {
            return false;
        } else if( l != null && r != null && !l.equals( r ) ) {
            return false;
        } else if( ( l == null || r == null ) && l != r ) {
            return false;
        }
        return true;
    }

    private static String removeTrailingZeros(final String stringNumber) {
        if(stringNumber.contains("E")) {
            return stringNumber;
        }
        final String stringWithoutZeros = stringNumber.replaceAll("0*$", "");
        if (Character.isDigit(stringWithoutZeros.charAt(stringWithoutZeros.length() - 1))) {
            return stringWithoutZeros;
        } else {
            return stringWithoutZeros.substring(0, stringWithoutZeros.length() - 1);
        }
    }
}
