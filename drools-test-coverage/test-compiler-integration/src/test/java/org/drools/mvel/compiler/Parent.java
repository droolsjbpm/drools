/*
 * Copyright 2006 Red Hat, Inc. and/or its affiliates.
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

package org.drools.mvel.compiler;

public class Parent extends GrandParent {
    
    private GrandParent grandParent;

    public Parent() {
    }

    public Parent(final String name) {
        super( name );
    }

    /**
     * @return the parent
     */
    public GrandParent getGrandParent() {
        return grandParent;
    }

    /**
     * @param parent the parent to set
     */
    public void setGrandParent(GrandParent parent) {
        this.grandParent = parent;
    }

}
