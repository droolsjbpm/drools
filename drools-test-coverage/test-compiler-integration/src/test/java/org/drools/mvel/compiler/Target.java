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

public class Target {

    public String label;
    public Float  lat;
    public Float  lon;
    public Float  course;
    public Float  speed;
    public Float  time;

    public Float getCourse() {
        return this.course;
    }

    public void setCourse(final Float course) {
        this.course = course;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public Float getLat() {
        return this.lat;
    }

    public void setLat(final Float lat) {
        this.lat = lat;
    }

    public Float getLon() {
        return this.lon;
    }

    public void setLon(final Float lon) {
        this.lon = lon;
    }

    public Float getSpeed() {
        return this.speed;
    }

    public void setSpeed(final Float speed) {
        this.speed = speed;
    }

    public Float getTime() {
        return this.time;
    }

    public void setTime(final Float time) {
        this.time = time;
    }

    public String toString() {
        return "Target< label: " + this.label + " lat: " + this.lat + " lon: " + this.lon + " course: " + this.course + " speed: " + this.speed + " time: " + this.time + ">";
    }
}
