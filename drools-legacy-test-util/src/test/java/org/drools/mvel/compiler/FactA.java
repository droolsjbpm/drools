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

import java.io.Serializable;

public class FactA implements Serializable {
    String  field1;
    Integer field2;
    Float   field3;
    Integer field4;    
    TestEnum enumVal;

    public FactA() {
    }

    public FactA( String f1 ) {
        this.field1 = f1;
    }
    
    public FactA( final Integer f2 ) {
        this.field2 = f2;
    }

    public FactA(final String f1,
                 final Integer f2,
                 final Float f3) {
        this.field1 = f1;
        this.field2 = f2;
        this.field3 = f3;
    }

    public String getField1() {
        return this.field1;
    }

    public void setField1(final String s) {
        this.field1 = s;
    }

    public Integer getField2() {
        return this.field2;
    }

    public void setField2(final Integer i) {
        this.field2 = i;
    }

    public Float getField3() {
        return this.field3;
    }

    public void setField3(final Float f) {
        this.field3 = f;
    }

    public Integer getField4() {
        return field4;
    }

    public void setField4(Integer field4) {
        this.field4 = field4;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field1 == null) ? 0 : field1.hashCode());
        result = prime * result + ((field2 == null) ? 0 : field2.hashCode());
        result = prime * result + ((field3 == null) ? 0 : field3.hashCode());
        result = prime * result + ((field4 == null) ? 0 : field4.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final FactA other = (FactA) obj;
        if ( field1 == null ) {
            if ( other.field1 != null ) return false;
        } else if ( !field1.equals( other.field1 ) ) return false;
        if ( field2 == null ) {
            if ( other.field2 != null ) return false;
        } else if ( !field2.equals( other.field2 ) ) return false;
        if ( field3 == null ) {
            if ( other.field3 != null ) return false;
        } else if ( !field3.equals( other.field3 ) ) return false;
        if ( field4 == null ) {
            if ( other.field4 != null ) return false;
        } else if ( !field4.equals( other.field4 ) ) return false;        
        return true;
    }

    /**
     * @return the enumVal
     */
    public TestEnum getEnumVal() {
        return enumVal;
    }

    /**
     * @param enumVal the enumVal to set
     */
    public void setEnumVal( TestEnum enumVal ) {
        this.enumVal = enumVal;
    }

    public String toString() {
        return "FactA{" +
               "field1='" + field1 + '\'' +
               ", field2=" + field2 +
               ", field3=" + field3 +
               ", field4=" + field4 +
               ", enumVal=" + enumVal +
               '}';
    }
}
