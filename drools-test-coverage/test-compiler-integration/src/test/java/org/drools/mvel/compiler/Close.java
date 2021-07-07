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

public class Close {

    public String label;
    public String label2;
    public Float  time;
    public Float  distance;
    public Float  bearing;

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

    public String toString() {
        return "Close< label: " + this.label + " label2: " + this.label2 + " time: " + this.time + " distance: " + this.distance + " bearing: " + this.bearing + ">";
    }
}
