/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kie.dmn.feel.lang.types.impl.ComparablePeriod;
import org.kie.dmn.feel.runtime.Range;
import org.kie.dmn.feel.runtime.functions.DateAndTimeFunction;
import org.kie.dmn.feel.runtime.functions.DateFunction;
import org.kie.dmn.feel.runtime.functions.TimeFunction;

public final class TypeUtil {

    private static final long SECONDS_IN_A_MINUTE = 60;
    private static final long SECONDS_IN_AN_HOUR = 60 * SECONDS_IN_A_MINUTE;
    private static final long SECONDS_IN_A_DAY = 24 * SECONDS_IN_AN_HOUR;
    private static final long NANOSECONDS_PER_SECOND = 1000000000;

    public static boolean isCollectionTypeHomogenous(final Collection collection) {
        if (collection.isEmpty()) {
            return true;
        } else {
            return isCollectionTypeHomogenous(collection, collection.iterator().next().getClass());
        }
    }

    public static boolean isCollectionTypeHomogenous(final Collection collection, final Class expectedType) {
        for (final Object object : collection) {
            if (object == null) {
                continue;
            } else if (!ClassUtil.isAssignableFrom(expectedType, object.getClass())) {
                return false;
            }
        }
        return true;
    }

    public static String formatValue(final Object val, final boolean wrapForCodeUsage) {
        if (val instanceof String) {
            return formatString(val.toString(), wrapForCodeUsage);
        } else if (val instanceof LocalDate) {
            return formatDate((LocalDate) val, wrapForCodeUsage);
        } else if (val instanceof LocalTime || val instanceof OffsetTime) {
            return formatTimeString(TimeFunction.FEEL_TIME.format((TemporalAccessor) val), wrapForCodeUsage);
        } else if (val instanceof LocalDateTime || val instanceof OffsetDateTime) {
            return formatDateTimeString(DateAndTimeFunction.FEEL_DATE_TIME.format((TemporalAccessor) val), wrapForCodeUsage);
        } else if (val instanceof ZonedDateTime) {
            TemporalAccessor ta = (TemporalAccessor) val;
            ZoneId zone = ta.query(TemporalQueries.zone());
            if (!(zone instanceof ZoneOffset)) {
                // it is a ZoneRegion
                return formatDateTimeString(DateAndTimeFunction.REGION_DATETIME_FORMATTER.format((TemporalAccessor) val), wrapForCodeUsage);
            } else {
                return formatDateTimeString(DateAndTimeFunction.FEEL_DATE_TIME.format((TemporalAccessor) val), wrapForCodeUsage);
            }
        } else if (val instanceof Duration) {
            return formatDuration((Duration) val, wrapForCodeUsage);
        } else if (val instanceof ChronoPeriod) {
            return formatPeriod((ChronoPeriod) val, wrapForCodeUsage);
        } else if (val instanceof TemporalAccessor) {
            TemporalAccessor ta = (TemporalAccessor) val;
            if (ta.query(TemporalQueries.localDate()) == null && ta.query(TemporalQueries.localTime()) != null && ta.query(TemporalQueries.zoneId()) != null) {
                return formatTimeString(TimeFunction.FEEL_TIME.format((TemporalAccessor) val), wrapForCodeUsage);
            } else {
                return String.valueOf(val);
            }
        } else if (val instanceof List) {
            return formatList((List) val, wrapForCodeUsage);
        } else if (val instanceof Range) {
            return formatRange((Range) val, wrapForCodeUsage);
        } else if (val instanceof Map) {
            return formatContext((Map) val, wrapForCodeUsage);
        } else {
            return String.valueOf(val);
        }
    }

    public static String formatDateTimeString(final String dateTimeString, final boolean wrapForCodeUsage) {
        if (wrapForCodeUsage) {
            return "date and time( \"" + dateTimeString + "\" )";
        } else {
            return dateTimeString;
        }
    }

    public static String formatTimeString(final String timeString, final boolean wrapForCodeUsage) {
        if (wrapForCodeUsage) {
            return "time( \"" + timeString + "\" )";
        } else {
            return timeString;
        }
    }

    public static String formatDate(final LocalDate date, final boolean wrapForCodeUsage) {
        if (wrapForCodeUsage) {
            return "date( \"" + DateFunction.FEEL_DATE.format(date) + "\" )";
        } else {
            return DateFunction.FEEL_DATE.format(date);
        }
    }

    public static String formatString(final String value, final boolean wrapForCodeUsage) {
        if (wrapForCodeUsage) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }

    public static String formatList(final List list, final boolean wrapDateTimeValuesInFunctions) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        int count = 0;
        for (final Object val : list) {
            if (count > 0) {
                sb.append(", ");
            }
            sb.append(formatValue(val, wrapDateTimeValuesInFunctions));
            count++;
        }
        if (!list.isEmpty()) {
            sb.append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String formatContext(final Map context, final boolean wrapDateTimeValuesInFunctions) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        int count = 0;
        for (final Map.Entry<Object, Object> val : (Set<Map.Entry<Object, Object>>) context.entrySet()) {
            if (count > 0) {
                sb.append(", ");
            }
            // keys should always be strings, so do not call recursivelly to avoid the "
            sb.append(val.getKey());
            sb.append(" : ");
            sb.append(formatValue(val.getValue(), wrapDateTimeValuesInFunctions));
            count++;
        }
        if (!context.isEmpty()) {
            sb.append(" ");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String formatRange(final Range val, final boolean wrapDateTimeValuesInFunctions) {
        final StringBuilder sb = new StringBuilder();
        sb.append(val.getLowBoundary() == Range.RangeBoundary.OPEN ? "( " : "[ ");
        sb.append(formatValue(val.getLowEndPoint(), wrapDateTimeValuesInFunctions));
        sb.append(" .. ");
        sb.append(formatValue(val.getHighEndPoint(), wrapDateTimeValuesInFunctions));
        sb.append(val.getHighBoundary() == Range.RangeBoundary.OPEN ? " )" : " ]");
        return sb.toString();
    }

    public static String formatPeriod(final ChronoPeriod period, final boolean wrapInDurationFunction) {
        final long totalMonths = ComparablePeriod.toTotalMonths(period);
        if (totalMonths == 0) {
            if (wrapInDurationFunction) {
                return "duration( \"P0M\" )";
            } else {
                return "P0M";
            }
        }
        final StringBuilder sb = new StringBuilder();
        if (wrapInDurationFunction) {
            sb.append("duration( \"");
        }
        if (totalMonths < 0) {
            sb.append("-P");
        } else {
            sb.append('P');
        }

        final long years = Math.abs(totalMonths / 12);
        if (years != 0) {
            sb.append(years).append('Y');
        }

        final long months = Math.abs(totalMonths % 12);
        if (months != 0) {
            sb.append(months).append('M');
        }

        if (wrapInDurationFunction) {
            sb.append("\" )");
        }

        return sb.toString();
    }

    public static String formatDuration(final Duration duration, final boolean wrapInDurationFunction) {
        if (duration.getSeconds() == 0 && duration.getNano() == 0) {
            if (wrapInDurationFunction) {
                return "duration( \"PT0S\" )";
            } else {
                return "PT0S";
            }
        }
        final long days = duration.getSeconds() / SECONDS_IN_A_DAY;
        final long hours = (duration.getSeconds() % SECONDS_IN_A_DAY) / SECONDS_IN_AN_HOUR;
        final long minutes = (duration.getSeconds() % SECONDS_IN_AN_HOUR) / SECONDS_IN_A_MINUTE;
        final long seconds = duration.getSeconds() % SECONDS_IN_A_MINUTE;

        final StringBuilder sb = new StringBuilder();
        if (wrapInDurationFunction) {
            sb.append("duration( \"");
        }
        if (duration.isNegative()) {
            sb.append("-");
        }
        sb.append("P");
        if (days != 0) {
            appendToDurationString(sb, days, "D");
        }
        if (hours != 0 || minutes != 0 || seconds != 0 || duration.getNano() != 0) {
            sb.append("T");
            if (hours != 0) {
                appendToDurationString(sb, hours, "H");
            }
            if (minutes != 0) {
                appendToDurationString(sb, minutes, "M");
            }
            if (seconds != 0 || duration.getNano() != 0) {
                appendSecondsToDurationString(sb, seconds, duration.getNano());
            }
        }
        if (wrapInDurationFunction) {
            sb.append("\" )");
        }
        return sb.toString();
    }

    private static void appendToDurationString(final StringBuilder sb, final long days, final String timeSegmentChar) {
        sb.append(Math.abs(days));
        sb.append(timeSegmentChar);
    }

    private static void appendSecondsToDurationString(final StringBuilder sb, final long seconds, final long nanoseconds) {
        if (seconds < 0 && nanoseconds > 0) {
            if (seconds == -1) {
                sb.append("0");
            } else {
                sb.append(Math.abs(seconds + 1));
            }
        } else {
            sb.append(Math.abs(seconds));
        }
        if (nanoseconds > 0) {
            final int pos = sb.length();
            if (seconds < 0) {
                sb.append(2 * NANOSECONDS_PER_SECOND - nanoseconds);
            } else {
                sb.append(nanoseconds + NANOSECONDS_PER_SECOND);
            }
            eliminateTrailingZeros(sb);
            sb.setCharAt(pos, '.');
        }
        sb.append('S');
    }

    private static void eliminateTrailingZeros(final StringBuilder sb) {
        while (sb.charAt(sb.length() - 1) == '0') {
            // eliminates trailing zeros in the nanoseconds
            sb.setLength(sb.length() - 1);
        }
    }

    private TypeUtil() {
        // Not allowed for util classes.
    }
}
