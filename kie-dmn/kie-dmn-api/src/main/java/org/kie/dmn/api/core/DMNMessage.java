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

package org.kie.dmn.api.core;

import org.kie.api.builder.Message;
import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.internal.builder.InternalMessage;

/**
 * A general message interface for all DMN related messages
 * raised during compilation and execution.
 */
public interface DMNMessage extends InternalMessage {

    enum Severity {
        /**
         * @deprecated use {@link #INFO} level.
         */
        @Deprecated
        TRACE,
        INFO,
        WARN,
        ERROR;
    }

    /**
     * Returns the severity of the message. Either TRACE, INFO, WARN or ERROR
     *
     * @deprecated use {@link Message#getLevel()} instead.
     */
    @Deprecated
    Severity getSeverity();

    /**
     * Returns a human readable text with the explanation of the event that
     * raised the message.
     *
     * @deprecated use {@link Message#getText()} instead.
     */
    @Deprecated
    String getMessage();
    
    /**
     * Returns a classification of the event that raised the message.
     *
     * @return
     */
    DMNMessageType getMessageType();

    /**
     * Returns the ID of the model element to which this message relates to
     * or null if this message does not refer to a specific model element.
     *
     * @return
     */
    String getSourceId();

    /**
     * Returns the actual model element reference to which this message relates to
     * or null if this message does not refer to a specific model element.
     *
     * @return
     */
    Object getSourceReference();

    /**
     * If this message relates to a FEEL compilation or runtime event, this method
     * returns the reference to the actual FEEL event.
     *
     * @return
     */
    FEELEvent getFeelEvent();

    /**
     * If this message relates to a java exception, this method returns a reference
     * to the actual Throwable object.
     *
     * @return
     */
    Throwable getException();


}
