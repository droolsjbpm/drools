/*
 * Copyright 2008 Red Hat, Inc. and/or its affiliates.
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

package org.drools.core.factmodel;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.base.ClassFieldAccessor;
import org.drools.core.base.CoreComponentsBuilder;
import org.drools.core.factmodel.traits.Alias;
import org.drools.core.util.StringUtils;
import org.kie.api.definition.type.Annotation;
import org.kie.api.definition.type.FactField;

/**
 * Declares a field to be dynamically generated.
 */
public class FieldDefinition
        implements
        FactField,
        Comparable<FieldDefinition> {

    private String             name       = null;
    private GenericTypeDefinition type;
    private boolean            key        = false;
    private boolean            inherited  = false;
    private String             overriding = null;
    private int                index      = -1;
    private int                priority   = -1;
    private int                declIndex  = -1;
    private String             initExpr   = null;
    private boolean            recursive  = false;
    private Map<String,Object> metaData;
	private String             getterName = null;
	private String             setterName = null;

    private List<AnnotationDefinition> annotations;

    private ClassFieldAccessor accessor   = null;

    public FieldDefinition() {
    }

    public FieldDefinition(String name, String type) {
        this( name, new GenericTypeDefinition( type ) );
    }

    public FieldDefinition(String name, GenericTypeDefinition type) {
        this( name, type, false );
    }

    public FieldDefinition(String name, String type, boolean key) {
        this( name, new GenericTypeDefinition( type ), key );
    }

    public FieldDefinition(String name, GenericTypeDefinition type, boolean key) {
        this.name = name;
        this.type = type;
        this.key = key;
    }

    public void setReadWriteAccessor(ClassFieldAccessor accessor) {
        this.accessor = accessor;
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        this.name = (String) in.readObject();
        this.type = (GenericTypeDefinition) in.readObject();
        this.key = in.readBoolean();
        this.accessor = (ClassFieldAccessor) in.readObject();
        this.annotations = (List<AnnotationDefinition>) in.readObject();
        this.inherited = in.readBoolean();
        this.overriding = (String) in.readObject();
        this.index = in.readInt();
        this.declIndex = in.readInt();
        this.priority = in.readInt();
        this.initExpr = (String) in.readObject();
        this.metaData = (Map<String, Object>) in.readObject();
        this.getterName = ( String ) in.readObject();
        this.setterName = ( String ) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( this.name );
        out.writeObject( this.type );
        out.writeBoolean( this.key );
        out.writeObject( this.accessor );
        out.writeObject( this.annotations );
        out.writeBoolean( this.inherited );
        out.writeObject( this.overriding );
        out.writeInt( this.index );
        out.writeInt( this.declIndex );
        out.writeInt( this.priority );
        out.writeObject( this.initExpr );
        out.writeObject( this.metaData );
        out.writeObject( this.getterName );
        out.writeObject( this.setterName );
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

    public GenericTypeDefinition getGenericType() {
        return this.type;
    }

    /**
     * @return Returns the fully qualified type.
     */
    public String getTypeName() {
        return this.type.getRawType();
    }

    /**
     * @param type The fully qualified type to set.
     */
    public void setTypeName(String type) {
        this.type = new GenericTypeDefinition(type);
    }

    public Class< ? > getType() {
        return this.accessor.getFieldType();
    }

    /**
     * @return Returns the key.
     */
    public boolean isKey() {
        return key;
    }

    /**
     * @param key The key to set.
     */
    public void setKey(boolean key) {
        this.key = key;
    }

    /**
     * Creates the String name for the get method for a field with the given name and type
     * @return
     */
    public String getReadMethod() {
    	if ( getterName != null ) {
    		return getterName;
	    }
        String prefix;
        if ( "boolean".equals( this.type.getRawType() ) ) {
            prefix = "is";
        } else {
            prefix = "get";
        }
        return prefix + this.name.substring( 0,
                1 ).toUpperCase() + this.name.substring( 1 );
    }

    /**
     * Creates the String name for the set method for a field with the given name and type
     *
     * @return
     */
    public String getWriteMethod() {
        return setterName != null ? setterName :
		        "set" + this.name.substring( 0, 1 ).toUpperCase() + this.name.substring( 1 );
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object o) {
        return (o != null) && this.getName().equals( ((FieldDefinition) o).getName() );
    }

    /**
     * @return Returns the field extractor
     */
    public ClassFieldAccessor getFieldAccessor() {
        return this.accessor;
    }

    /**
     * @inheritDoc
     */
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * Sets the value of this attribute in the target
     * bean instance
     *
     * @param bean the target bean instance where the attribute shall be set
     * @param value the value to set the attribute to
     */
    public void setValue(Object bean,
                         Object value) {
        this.accessor.setValue( bean,
                value );
    }

    /**
     * Returns the value of this attribute in the target bean instance
     *
     * @param bean the target bean instance
     *
     * @return target bean instance attribute value
     */
    public Object getValue(Object bean) {
        return this.accessor.getValue( bean );
    }

    public Object get(Object bean) {
        return this.accessor.getValue( bean );
    }

    public void set(Object bean,
                    Object value) {
        this.accessor.setValue( bean,
                value );
    }


    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getDeclIndex() {
        return declIndex;
    }

    public void setDeclIndex( int declIndex ) {
        this.declIndex = declIndex;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority( int priority ) {
        this.priority = priority;
    }

    public String getInitExpr() {
        return initExpr;
    }

    public void setInitExpr(String initExpr) {
        this.initExpr = initExpr;
    }


    public int compareTo(FieldDefinition other) {
        return (this.priority - other.priority);
    }



    public void addAnnotation(AnnotationDefinition annotationDefinition) {
        if (this.annotations == null) {
            this.annotations = new ArrayList<AnnotationDefinition>();
        }
        this.annotations.add(annotationDefinition);
    }

    public List<AnnotationDefinition> getAnnotations() {
        return annotations;
    }

    public List<Annotation> getFieldAnnotations() {
        return Collections.unmodifiableList( new ArrayList( annotations ) );
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public void addMetaData( String key, Object value ) {
        if ( this.metaData == null ) {
            metaData = new HashMap<String,Object>();
        }
        metaData.put( key, value );
    }

    public String getDefaultValueAsString() {
        return CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, String.class );
    }


    public Boolean getDefaultValueAsBoolean( ) {
        if ( StringUtils.isEmpty( initExpr ) ) {
            return false;
        } else {
            if ( "true".equalsIgnoreCase( initExpr ) ) {
                return true;
            } else if ( "false".equalsIgnoreCase( initExpr ) ) {
                return false;
            } else {
                return CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Boolean.class );
            }
        }
    }
    public Byte getDefaultValueAsByte( ) {
        try {
            return initExpr == null ? 0 : Byte.parseByte(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0 : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Byte.class );
        }
    }
    public Character getDefaultValueAsChar() {
        if ( StringUtils.isEmpty( initExpr ) ) {
            return '\u0000';
        } else {
            if ( initExpr.length() == 1 ) {
                return initExpr.charAt(0);
            } else {
                return CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Character.class );
            }
        }
    }
    public Double getDefaultValueAsDouble( ) {
        try {
            return initExpr == null ? 0.0 : Double.parseDouble(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0.0 : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Double.class );
        }
    }
    public Float getDefaultValueAsFloat( ) {
        try {
            return initExpr == null ? 0.0f : Float.parseFloat(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0.0f : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Float.class );
        }
    }
    public Integer getDefaultValueAsInt( ) {
        try {
            return initExpr == null ? 0 : Integer.parseInt(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0 : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Integer.class );
        }
    }
    public Long getDefaultValueAsLong( ) {
        try {
            return initExpr == null ? 0L : Long.parseLong(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0L : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Long.class );
        }
    }
    public Short getDefaultValueAsShort( ) {
        try {
            return initExpr == null ? 0 : Short.parseShort(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0 : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Short.class );
        }
    }


    public boolean getDefaultValueAs_boolean() {
        if ( StringUtils.isEmpty( initExpr ) ) {
            return false;
        } else {
            if ( "true".equalsIgnoreCase( initExpr ) ) {
                return true;
            } else if ( "false".equalsIgnoreCase( initExpr ) ) {
                return false;
            } else {
                return CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Boolean.class );
            }
        }
    }
    public byte getDefaultValueAs_byte() {
        try {
            return initExpr == null ? 0 : Byte.parseByte(initExpr);
        } catch (NumberFormatException nfe) {
             return StringUtils.isEmpty( initExpr ) ? 0 : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Byte.class );
        }
    }
    public char getDefaultValueAs_char() {
        if ( StringUtils.isEmpty( initExpr ) ) {
            return '\u0000';
        } else {
            if ( initExpr.length() == 1 ) {
                return initExpr.charAt(0);
            } else {
                return CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Character.class );
            }
        }
    }
    public double getDefaultValueAs_double() {
        try {
            return initExpr == null ? 0.0 : Double.parseDouble(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0.0 : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Double.class );
        }
    }
    public float getDefaultValueAs_float() {
        try {
            return initExpr == null ? 0.0f : Float.parseFloat(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0.0f : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Float.class );
        }
    }
    public int getDefaultValueAs_int() {
        try {
            return initExpr == null ? 0 : Integer.parseInt(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0 : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Integer.class );
        }
    }
    public long getDefaultValueAs_long() {
        try {
            return initExpr == null ? 0L : Long.parseLong(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0L : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Long.class );
        }
    }
    public short getDefaultValueAs_short() {
        try {
            return initExpr == null ? 0 : Short.parseShort(initExpr);
        } catch (NumberFormatException nfe) {
            return StringUtils.isEmpty( initExpr ) ? 0 : CoreComponentsBuilder.get().getMVELExecutor().eval( initExpr, Short.class );
        }
    }


    public String toString() {
        return "FieldDefinition{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", key=" + key +
                ", inherited=" + inherited +
                ", index=" + index +
                ", initExpr='" + initExpr + '\'' +
                ", annotations=" + annotations +
                ", accessor=" + accessor +
                '}';
    }

    public String resolveAlias( ) {
        if ( getAnnotations() != null ) {
            for ( AnnotationDefinition def : getAnnotations() ) {

                if ( def.getName().equals( Alias.class.getName() ) ) {
                    String alias =  (String) def.getValues().get( "value" ).getValue();
                    return alias;
                }
            }
        }
        return getName();
    }

    public String getAlias() {
        if ( getAnnotations() != null ) {
            for ( AnnotationDefinition def : getAnnotations() ) {
                if ( def.getName().equals( Alias.class.getName() ) ) {
                    return (String) def.getValues().get( "value" ).getValue();
                }
            }
        }
        return getName();
    }

    public boolean hasAlias() {
        if ( getAnnotations() == null ) {
            return false;
        }
        for ( AnnotationDefinition def : getAnnotations() ) {
            if ( def.getName().equals( Alias.class.getName() ) ) {
                return true;
            }
        }
        return false;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public boolean hasOverride() {
        return overriding != null;
    }

    public String getOverriding() {
        return overriding;
    }

    public void setOverriding( String overriding ) {
        this.overriding = overriding;
    }

	public String getGetterName() {
		return getterName;
	}

	public void setGetterName( String getterName ) {
		this.getterName = getterName;
	}

	public String getSetterName() {
		return setterName;
	}

	public void setSetterName( String setterName ) {
		this.setterName = setterName;
	}
}
