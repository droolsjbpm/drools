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

package org.drools.core.base.field;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.drools.core.spi.FieldValue;

public class BooleanFieldImpl
    implements
    FieldValue, Externalizable {

    private static final long serialVersionUID = 510l;
    private boolean     value;

    public BooleanFieldImpl() {

    }

    public BooleanFieldImpl(final boolean value) {
        this.value = value;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        value   = in.readBoolean();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(value);
    }

    public Serializable getValue() {
        return this.value ? Boolean.TRUE : Boolean.FALSE;
    }

    public String toString() {
        return this.value ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
    }

    public boolean getBooleanValue() {
        return this.value;
    }

    public byte getByteValue() {
        throw new RuntimeException( "Conversion to byte not supported for type boolean" );
    }

    public char getCharValue() {
        throw new RuntimeException( "Conversion to char not supported for type boolean" );
    }

    public double getDoubleValue() {
        throw new RuntimeException( "Conversion to double not supported for type boolean" );
    }

    public float getFloatValue() {
        throw new RuntimeException( "Conversion to float not supported for type boolean" );
    }

    public int getIntValue() {
        throw new RuntimeException( "Conversion to int not supported for type boolean" );
    }

    public long getLongValue() {
        throw new RuntimeException( "Conversion to long not supported for type boolean" );
    }

    public short getShortValue() {
        throw new RuntimeException( "Conversion to short not supported for type boolean" );
    }

    public boolean equals(final Object object) {
        if ( this == object ) {
            return true;
        }
        if ( object == null || !(object instanceof BooleanFieldImpl) ) {
            return false;
        }
        final BooleanFieldImpl other = (BooleanFieldImpl) object;

        return this.value == other.value;
    }

    public int hashCode() {
        return this.value ? 1 : 0;
    }

    public boolean isNull() {
        return false;
    }

    public boolean isBooleanField() {
        return true;
    }

    public boolean isFloatNumberField() {
        return false;
    }

    public boolean isIntegerNumberField() {
        return false;
    }

    public boolean isObjectField() {
        return false;
    }

    public boolean isCollectionField() {
        return false;
    }

    public boolean isStringField() {
        return false;
    }

    public BigDecimal getBigDecimalValue() {
        throw new RuntimeException( "Conversion to BigDecimal not supported for type boolean" );
    }

    public BigInteger getBigIntegerValue() {
        throw new RuntimeException( "Conversion to BigInteger not supported for type boolean" );
    }


}
