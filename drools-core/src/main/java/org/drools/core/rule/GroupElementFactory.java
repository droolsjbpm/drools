/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.rule;

/**
 * A simple factory for GroupElements
 */
public class GroupElementFactory {

    private GroupElementFactory() {
    }

    public static GroupElement newAndInstance() {
        return new GroupElement( GroupElement.AND );
    }

    public static GroupElement newOrInstance() {
        return new GroupElement( GroupElement.OR );
    }

    public static GroupElement newNotInstance() {
        return new GroupElement( GroupElement.NOT );
    }

    public static GroupElement newExistsInstance() {
        return new GroupElement( GroupElement.EXISTS );
    }

}
