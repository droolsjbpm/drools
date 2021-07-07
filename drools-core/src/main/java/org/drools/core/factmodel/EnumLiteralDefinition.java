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


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

/**
 * Declares an enum to be dynamically generated.
 */
public class EnumLiteralDefinition implements Externalizable {

    private String             name             = null;
    private List<String>       constructorArgs  = null;


    public EnumLiteralDefinition() {

    }

    /**
     * Default constructor
     *
     * @param name the literal's name
     * @param args the constructor args (optional)
     */
    public EnumLiteralDefinition(String name,
                                 List<String> args) {
        this.name = name;
        this.constructorArgs = new ArrayList<String>( args );
    }


    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        this.name = (String) in.readObject();
        this.constructorArgs = (List<String>) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( this.name );
        out.writeObject( this.constructorArgs );
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    public List<String> getConstructorArgs() {
        return constructorArgs;
    }

    public void setConstructorArgs(List<String> constructorArgs) {
        this.constructorArgs = constructorArgs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnumLiteralDefinition that = (EnumLiteralDefinition) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }


    @Override
    public String toString() {
        return "EnumLiteralDefinition{" +
                "name='" + name + '\'' +
                ", constructorArgs=" + constructorArgs +
                '}';
    }
}
