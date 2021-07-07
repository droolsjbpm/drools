/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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
package java.util;

import java.time.ZoneId;

public class TimeZone {

    private String timezone;

    private TimeZone(final String timezone) {
        this.timezone = timezone;
    }

    public static TimeZone getTimeZone(final String timezone) {
        return new TimeZone(timezone);
    }

    public static TimeZone getTimeZone(final ZoneId zoneId) {
        return new TimeZone(zoneId.getId());
    }

    public ZoneId toZoneId() {
        return ZoneId.of(timezone);
    }

    public String getID(){
        return timezone;
    }
}