/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.mvel.compiler;

public class Approach {

    public String label;
    public String label2;
    public Float  time;
    public Float  time2;
    public Float  distance;
    public Float  distance2;
    public Float  bearing;
    public Float  bearing2;

    public Float getDistance2() {
        return this.distance2;
    }

    public void setDistance2(final Float distance2) {
        this.distance2 = distance2;
    }

    public Float getTime2() {
        return this.time2;
    }

    public void setTime2(final Float time2) {
        this.time2 = time2;
    }

    public Float getBearing() {
        return this.bearing;
    }

    public void setBearing(final Float bearing) {
        this.bearing = bearing;
    }

    public Float getDistance() {
        return this.distance;
    }

    public void setDistance(final Float distance) {
        this.distance = distance;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getLabel2() {
        return this.label2;
    }

    public void setLabel2(final String label2) {
        this.label2 = label2;
    }

    public Float getTime() {
        return this.time;
    }

    public void setTime(final Float time) {
        this.time = time;
    }

    public Float getBearing2() {
        return this.bearing2;
    }

    public void setBearing2(final Float bearing2) {
        this.bearing2 = bearing2;
    }

    public String toString() {
        return "Approach< label: " + this.label + " label2: " + this.label2 + " time: " + this.time + " time2: " + this.time2 + " distance: " + this.distance + " distance2: " + this.distance2 + " bearing: " + this.bearing + " bearing2: " + this.bearing2 + " >";
    }
}
