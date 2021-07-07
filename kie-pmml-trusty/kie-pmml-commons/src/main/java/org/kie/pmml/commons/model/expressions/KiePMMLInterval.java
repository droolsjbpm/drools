/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.pmml.commons.model.expressions;

import java.io.Serializable;

import org.kie.pmml.api.enums.CLOSURE;

/**
 * KiePMML representation of an <b>Interval</b>
 */
public class KiePMMLInterval implements Serializable {

    private static final long serialVersionUID = -5245266051098683475L;
    private final Number leftMargin;
    private final Number rightMargin;
    private final CLOSURE closure;

    public KiePMMLInterval(Number leftMargin, Number rightMargin, CLOSURE closure) {
        this.leftMargin = leftMargin;
        this.rightMargin = rightMargin;
        this.closure = closure;
    }


    public boolean isIn(Number toEvaluate) {
        switch (closure) {
            case OPEN_OPEN:
                return isInsideOpenOpen(toEvaluate);
            case OPEN_CLOSED:
                return isInsideOpenClosed(toEvaluate);
            case CLOSED_OPEN:
                return isInsideClosedOpen(toEvaluate);
            case CLOSED_CLOSED:
                return isInsideClosedClosed(toEvaluate);
            default:
                throw new IllegalArgumentException("Unexpected closure: " + closure);
        }
    }

    boolean isInsideOpenOpen(Number toEvaluate) {
            if (leftMargin == null) {
                return toEvaluate.doubleValue() < rightMargin.doubleValue();
            } else if (rightMargin == null) {
                return toEvaluate.doubleValue() > leftMargin.doubleValue();
            } else {
                return toEvaluate.doubleValue() > leftMargin.doubleValue() && toEvaluate.doubleValue() < rightMargin.doubleValue();
            }
    }

    boolean isInsideOpenClosed(Number toEvaluate) {
        if (leftMargin == null) {
            return toEvaluate.doubleValue() <= rightMargin.doubleValue();
        } else if (rightMargin == null) {
            return toEvaluate.doubleValue() > leftMargin.doubleValue();
        } else {
            return toEvaluate.doubleValue() > leftMargin.doubleValue() && toEvaluate.doubleValue() <= rightMargin.doubleValue();
        }
    }

    boolean isInsideClosedOpen(Number toEvaluate) {
        if (leftMargin == null) {
            return toEvaluate.doubleValue() < rightMargin.doubleValue();
        } else if (rightMargin == null) {
            return toEvaluate.doubleValue() >= leftMargin.doubleValue();
        } else {
            return toEvaluate.doubleValue() >= leftMargin.doubleValue() && toEvaluate.doubleValue() < rightMargin.doubleValue();
        }
    }

    boolean isInsideClosedClosed(Number toEvaluate) {
        if (leftMargin == null) {
            return toEvaluate.doubleValue() <= rightMargin.doubleValue();
        } else if (rightMargin == null) {
            return toEvaluate.doubleValue() >= leftMargin.doubleValue();
        } else {
            return toEvaluate.doubleValue() >= leftMargin.doubleValue() && toEvaluate.doubleValue() <= rightMargin.doubleValue();
        }
    }
}
