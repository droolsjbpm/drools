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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.phreak.Reactive;
import org.kie.api.definition.type.Annotation;
import org.kie.api.definition.type.FactField;
import org.kie.api.definition.type.FactType;

/**
 * Declares a class to be dynamically created
 */
public class ClassDefinition
    implements
    FactType {

    public static enum TRAITING_MODE { NONE, BASIC, LOGICAL }

    private String                       className;
    private String                       superClass;
    private String[]                     interfaces;
    private transient Class< ? >         definedClass;
    private TRAITING_MODE                traitable;
    private boolean                      abstrakt       = false;
    private Map<String, Object>          metaData;

    protected LinkedHashMap<String, FieldDefinition> fields = new LinkedHashMap<String, FieldDefinition>();

    private Map<String, AnnotationDefinition> annotations;

    private Map<String, List<String>> modifiedPropsByMethod;

    public ClassDefinition() {
        this( null,
              null,
              null );
    }

    public ClassDefinition( String className ) {
        this( className,
              null,
              null );
    }

    public ClassDefinition( String className,
                            String superClass,
                            String[] interfaces ) {
        this.setClassName( className );
        this.setSuperClass( superClass );
        this.setInterfaces( interfaces );
    }

    public ClassDefinition( Class<?> cls ) {
        this.definedClass = cls;
        this.setClassName( cls.getCanonicalName() );
        this.setSuperClass( cls.getSuperclass() != null ? cls.getSuperclass().getCanonicalName() : null );
        String[] interfaces = new String[cls.getInterfaces().length];
        int i = 0;
        for (Class<?> interfaze : cls.getInterfaces()) {
            interfaces[i++] = interfaze.getCanonicalName();
        }
        this.setInterfaces( interfaces );
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        this.className = (String) in.readObject();
        this.superClass = (String) in.readObject();
        this.interfaces = (String[]) in.readObject();
        this.fields = (LinkedHashMap<String, FieldDefinition>) in.readObject();
        this.annotations = (Map<String, AnnotationDefinition>) in.readObject();
        this.modifiedPropsByMethod = (Map<String, List<String>>) in.readObject();
        this.traitable = (ClassDefinition.TRAITING_MODE) in.readObject();
        this.abstrakt = in.readBoolean();
        this.metaData = (HashMap<String,Object>) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( this.className );
        out.writeObject( this.superClass );
        out.writeObject( this.interfaces );
        out.writeObject( this.fields );
        out.writeObject( this.annotations );
        out.writeObject( this.modifiedPropsByMethod);
        out.writeObject( this.traitable );
        out.writeBoolean( this.abstrakt );
        out.writeObject( this.metaData );
    }

    /**
     * @return Returns the name.
     */
    public final String getClassName() {
        return this.className;
    }

    /**
     * @param className The name to set.
     */
    public final void setClassName(final String className) {
        this.className = className;
    }

    /**
     * @return Returns the className.
     */
    public final Class< ? > getDefinedClass() {
        return definedClass;
    }

    /**
     * @param definedClass The class to set.
     */
    public void setDefinedClass(final Class< ? > definedClass) {
        this.definedClass = definedClass;
    }

    /**
     * Adds a field definition to this class
     * @param attr
     */
    public final void addField(FieldDefinition attr) {
        this.fields.put( attr.getName(),
                         attr );
    }

    /**
     * @return Returns an unmodifiable collection of field definitions
     */
    public final Collection<FieldDefinition> getFieldsDefinitions() {
        return Collections.unmodifiableCollection( this.fields.values() );
    }

    /**
     * Returns the field definition object for the given field name
     *
     * @param fieldName
     * @return
     */
    public FieldDefinition getField(final String fieldName) {
        return this.fields.get( fieldName );
    }

    public FieldDefinition getFieldByAlias( String alias ) {
        for ( FactField factField : getFields() ) {
            FieldDefinition def = (FieldDefinition) factField;
            if ( def.resolveAlias().equals( alias ) ) {
                return def;
            }
        }
        return null;
    }

    /**
     * Returns the field at position index, as defined by the builder using the @position annotation
     * @param index
     * @return    the index-th field
     */
    public FieldDefinition getField(int index) {
        if (index >= fields.size() || index < 0) {
            return null;
        }
        Iterator<FieldDefinition> iter = fields.values().iterator();
        for (int j = 0; j < index ; j++) {
            iter.next();
        }
        return iter.next();
    }


    /**
     * @return Returns the interfaces.
     */
    public final String[] getInterfaces() {
        return interfaces;
    }

    /**
     * @param interfaces The interfaces to set.
     */
    public final void setInterfaces(String[] interfaces) {
        this.interfaces = (interfaces != null) ? interfaces : new String[0];
    }

    public final void addInterface(String interfaze) {
        if (interfaces == null) {
            interfaces = new String[] { interfaze };
        } else {
            String[] i2 = new String[interfaces.length+1];
            System.arraycopy( interfaces, 0, i2, 0, interfaces.length );
            i2[interfaces.length] = interfaze;
            this.interfaces = i2;
        }
    }

    /**
     * @return Returns the superClass.
     */
    public final String getSuperClass() {
        return superClass;
    }

    /**
     * @param superClass The superClass to set.
     */
    public final void setSuperClass(final String superClass) {
        this.superClass = (superClass != null) ? superClass : "java.lang.Object";
    }

    public String getName() {
        return getClassName();
    }

    public String getSimpleName() {
        return getClassName().substring( getClassName().lastIndexOf( '.' ) + 1 );
    }

    public String getPackageName() {
        return getClassName().substring( 0, getClassName().lastIndexOf( '.' ) );
    }

    public Object newInstance() throws InstantiationException,
                               IllegalAccessException {
        return this.definedClass.newInstance();
    }

    public Class< ? > getFactClass() {
        return getDefinedClass();
    }

    public List<FactField> getFields() {
        return new ArrayList<FactField>( fields.values() );
    }

    public Object get(Object bean,
                      String field) {
        FieldDefinition fieldDefinition = getField( field );
        if (fieldDefinition != null) {
            return fieldDefinition.getFieldAccessor().getValue( bean );
        }
        return null;
    }

    public void set(Object bean,
                    String field,
                    Object value) {
        FieldDefinition fieldDefinition = getField( field );
        if (fieldDefinition != null) {
            fieldDefinition.getFieldAccessor().setValue( bean, value );
        }
    }

    public Map<String, Object> getAsMap(Object bean) {
        Map<String, Object> m = new HashMap<String, Object>( fields.size() );
        for (Map.Entry<String, FieldDefinition> ent : this.fields.entrySet()) {
            Object val = ent.getValue().getFieldAccessor().getValue(bean);
            m.put(ent.getKey(),
                    val);
        }
        return m;
    }

    public void setFromMap(Object bean,
                           Map<String, Object> data) {
        for (Map.Entry<String, Object> ent : data.entrySet()) {
            set(bean,
                    ent.getKey(),
                    ent.getValue());
        }
    }

    public void addAnnotation(AnnotationDefinition annotationDefinition) {
        if (this.annotations == null) {
            this.annotations = new HashMap<String, AnnotationDefinition>();
        }
        this.annotations.put( annotationDefinition.getName(), annotationDefinition );
    }

    public Collection<AnnotationDefinition> getAnnotations() {
        return annotations != null ? annotations.values() : Collections.<AnnotationDefinition>emptyList();
    }

    public AnnotationDefinition getAnnotation(Class<?> annotationClass) {
        return annotations != null ? annotations.get(annotationClass.getName()) : null;
    }

    public List<Annotation> getClassAnnotations() {
        return Collections.unmodifiableList( new ArrayList( getAnnotations() ) );
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public Object getMetaData(String name) {
        return metaData != null ? metaData.get(name) : null;
    }

    public void addMetaData( String key, Object value ) {
        if ( this.metaData == null ) {
            metaData = new HashMap<String,Object>();
        }
        metaData.put( key, value );
    }

    public void addModifiedPropsByMethod(Method method, List<String> props) {
        if (modifiedPropsByMethod == null) {
            modifiedPropsByMethod = new HashMap<String, List<String>>();
        }
        String methodName = modifiedPropsByMethodKey(method);
        modifiedPropsByMethod.put(methodName, props);
    }

    public List<String> getModifiedPropsByMethod(Method method) {
        return getModifiedPropsByMethod(method.getName(), method.getParameterTypes().length );
    }

    public List<String> getModifiedPropsByMethod(String methodName, int args) {
        if (modifiedPropsByMethod == null) {
            return null; 
        }
        
        List<String> byExactNumberOfArgs = modifiedPropsByMethod.get( methodName + "_" + args );
        List<String> bestEffortVarArgs = modifiedPropsByMethod.get( methodName + "_*" ); 
        if ( byExactNumberOfArgs != null ) {
            return byExactNumberOfArgs;
        }
        return bestEffortVarArgs; // << indeed maybe null
    }
    
    public static String modifiedPropsByMethodKey(Method method) {
        return method.getName() + "_" + ( method.isVarArgs() ? "*" : method.getParameterTypes().length );
    }

    public boolean isReactive() {
        return getAnnotation( Reactive.class ) != null;
    }

    public boolean isTraitable() {
        return traitable != null && traitable != TRAITING_MODE.NONE;
    }

    public void setTraitable( boolean traitable ) {
        setTraitable( traitable, false );
    }

    public void setTraitable( boolean traitable, boolean enableLogical ) {
        if ( ! traitable ) {
            this.traitable = TRAITING_MODE.NONE;
        } else {
            this.traitable = enableLogical ? TRAITING_MODE.LOGICAL : TRAITING_MODE.BASIC;
        }
    }

    public boolean isFullTraiting() {
        return this.traitable == TRAITING_MODE.LOGICAL;
    }

    public boolean isAbstrakt() {
        return abstrakt;
    }

    public void setAbstrakt(boolean abstrakt) {
        this.abstrakt = abstrakt;
    }

    public String toString() {
        return "ClassDefinition{" +
                "className='" + className + '\'' +
                ", superClass='" + superClass + '\'' +
                ", interfaces=" + (interfaces == null ? null : Arrays.asList(interfaces)) +
                ", definedClass=" + definedClass +
                ", traitable=" + traitable +
                ", abstract=" + abstrakt +
                ", fields=" + fields +
                ", annotations=" + annotations +
                '}';
    }
}
