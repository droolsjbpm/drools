/*
 * Copyright 2011 Red Hat, Inc. and/or its affiliates.
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

package org.drools.core.factmodel;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Declares an enum class to be dynamically created
 */
public class EnumClassDefinition
    extends ClassDefinition {

    private List<EnumLiteralDefinition>     enumLiterals = Collections.emptyList();

    public EnumClassDefinition() { }

    public EnumClassDefinition(String className, String fullSuperType, String[] interfax ) {
        super( className,
               fullSuperType,
               interfax );
    }



    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        super.readExternal( in );
        this.enumLiterals = (List<EnumLiteralDefinition>) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal( out );
        out.writeObject( enumLiterals );
    }

    public List<EnumLiteralDefinition> getEnumLiterals() {
        return enumLiterals;
    }

    public void setEnumLiterals(List<EnumLiteralDefinition> enumLiterals) {
        this.enumLiterals = enumLiterals;
    }

    public void addLiteral(EnumLiteralDefinition enumLiteralDefinition) {
        if ( enumLiterals == Collections.EMPTY_LIST ) {
            enumLiterals = new ArrayList<EnumLiteralDefinition>();
        }
        enumLiterals.add( enumLiteralDefinition );
    }
}
