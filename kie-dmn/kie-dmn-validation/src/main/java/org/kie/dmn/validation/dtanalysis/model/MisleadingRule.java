/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.kie.dmn.validation.dtanalysis.model;

import javax.annotation.Generated;

public class MisleadingRule {

    public final int misleadingRule;
    public final int misleadRule;

    public MisleadingRule(int misleadingRule, int misleadRule) {
        super();
        this.misleadingRule = misleadingRule;
        this.misleadRule = misleadRule;
    }

    @Generated(value = "org.eclipse.jdt.internal.corext.codemanipulation")
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + misleadRule;
        result = prime * result + misleadingRule;
        return result;
    }

    @Generated(value = "org.eclipse.jdt.internal.corext.codemanipulation")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MisleadingRule other = (MisleadingRule) obj;
        if (misleadRule != other.misleadRule)
            return false;
        if (misleadingRule != other.misleadingRule)
            return false;
        return true;
    }

}
