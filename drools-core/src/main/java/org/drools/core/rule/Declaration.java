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

package org.drools.core.rule;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;

import org.drools.core.base.ClassFieldReader;
import org.drools.core.base.ValueType;
import org.drools.core.common.DroolsObjectInputStream;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.spi.AcceptsReadAccessor;
import org.drools.core.spi.InternalReadAccessor;

import static org.drools.core.util.ClassUtils.canonicalName;
import static org.drools.core.util.ClassUtils.convertFromPrimitiveType;

public class Declaration
    implements
    Externalizable,
    AcceptsReadAccessor,
    Cloneable {
    // ------------------------------------------------------------
    // Instance members
    // ------------------------------------------------------------

    private static final long    serialVersionUID = 510l;

    /** The identifier for the variable. */
    private String               identifier;

    private String               bindingName;

    private InternalReadAccessor readAccessor;

    private Pattern              pattern;

    private boolean              internalFact;

    private transient Class<?>   declarationClass;

    private int xPathOffset = 0;

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------
    public Declaration() {
        this( null,
              null,
              null );
    }

    /**
     * Construct.
     *
     * @param identifier
     *            The name of the variable.
     * @param pattern
     *            The pattern this variable is declared in
     */
    public Declaration(final String identifier,
                       final Pattern pattern) {
        this( identifier,
              null,
              pattern,
              false );
    }

    /**
     * Construct.
     *
     * @param identifier
     *            The name of the variable.
     * @param extractor
     *            The extractor for this variable
     * @param pattern
     *            The pattern this variable is declared in
     */
    public Declaration(final String identifier,
                       final InternalReadAccessor extractor,
                       final Pattern pattern) {
        this( identifier,
              extractor,
              pattern,
              false );
    }

    /**
     * Construct.
     *
     * @param identifier
     *            The name of the variable.
     * @param identifier
     *            The name of the variable.
     * @param extractor
     *            The extractor for this variable
     * @param internalFact
     *            True if this is an internal fact created by the engine, like a collection result
     *            of a collect CE
     */
    public Declaration(final String identifier,
                       final InternalReadAccessor extractor,
                       final Pattern pattern,
                       final boolean internalFact) {
        this.identifier = identifier;
        this.readAccessor = extractor;
        this.pattern = pattern;
        this.internalFact = internalFact;
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        identifier = (String) in.readObject();
        ( (DroolsObjectInputStream) in ).readExtractor( this::setReadAccessor );
        pattern = (Pattern) in.readObject();
        internalFact = in.readBoolean();
        bindingName = (String) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( identifier );

        if (readAccessor instanceof ClassFieldReader ) {
            out.writeObject( ( (ClassFieldReader) readAccessor ).getAccessorKey() );
        } else {
            out.writeObject( readAccessor );
        }

        out.writeObject( pattern );
        out.writeBoolean( internalFact );
        out.writeObject( bindingName );
    }

    // ------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------

    /**
     * Retrieve the variable's identifier.
     *
     * @return The variable's identifier.
     */
    public String getIdentifier() {
        return this.identifier;
    }

    public String getBindingName() {
        return bindingName != null ? bindingName : identifier;
    }

    public void setBindingName(String bindingName) {
        this.bindingName = bindingName;
    }

    /**
     * Retrieve the <code>ValueType</code>.
     *
     * @return The ValueType.
     */
    public ValueType getValueType() {
        return this.readAccessor.getValueType();
    }

    /**
     * Returns the index of the pattern
     *
     * @return the pattern
     */
    public Pattern getPattern() {
        return this.pattern;
    }

    public void setPattern(final Pattern pattern) {
        this.pattern = pattern;
    }

    public int getOffset() {
        return pattern.getOffset() + xPathOffset;
    }

    public void setxPathOffset( int xPathOffset ) {
        this.xPathOffset = xPathOffset;
    }

    public boolean isFromXpathChunk() {
        return xPathOffset >= 1;
    }

    /**
     * Returns true if this declaration is a pattern declaration
     */
    public boolean isPatternDeclaration() {
        return ( this.pattern != null && this.pattern.getDeclaration() == this ) || this.getIdentifier().equals( "this" ) ;
    }

    public void setReadAccessor(InternalReadAccessor readAccessor) {
        this.readAccessor = readAccessor;
    }

    /**
     * Returns the Extractor expression
     */
    public InternalReadAccessor getExtractor() {
        return this.readAccessor;
    }

    public Class<?> getDeclarationClass() {
        if (declarationClass == null) {
            declarationClass = readAccessor != null ? readAccessor.getExtractToClass() : null;
        }
        return declarationClass;
    }

    public void setDeclarationClass( Class<?> declarationClass ) {
        this.declarationClass = declarationClass;
    }

    public Object getValue(InternalWorkingMemory workingMemory,
                           final Object object) {
        return this.readAccessor.getValue( workingMemory, object );
    }

    public char getCharValue(InternalWorkingMemory workingMemory,
                             final Object object) {
        return this.readAccessor.getCharValue(workingMemory, object);
    }

    public int getIntValue(InternalWorkingMemory workingMemory,
                           final Object object) {
        return this.readAccessor.getIntValue(workingMemory, object);
    }

    public byte getByteValue(InternalWorkingMemory workingMemory,
                             final Object object) {
        return this.readAccessor.getByteValue(workingMemory, object);
    }

    public short getShortValue(InternalWorkingMemory workingMemory,
                               final Object object) {
        return this.readAccessor.getShortValue(workingMemory, object);
    }

    public long getLongValue(InternalWorkingMemory workingMemory,
                             final Object object) {
        return this.readAccessor.getLongValue(workingMemory, object);
    }

    public float getFloatValue(InternalWorkingMemory workingMemory,
                               final Object object) {
        return this.readAccessor.getFloatValue(workingMemory, object);
    }

    public double getDoubleValue(InternalWorkingMemory workingMemory,
                                 final Object object) {
        return this.readAccessor.getDoubleValue(workingMemory, object);
    }

    public boolean getBooleanValue(InternalWorkingMemory workingMemory,
                                   final Object object) {
        return this.readAccessor.getBooleanValue(workingMemory, object);
    }

    public int getHashCode(InternalWorkingMemory workingMemory,
                           final Object object) {
        return this.readAccessor.getHashCode(workingMemory, object);
    }

    public boolean isGlobal() {
        return this.readAccessor != null && this.readAccessor.isGlobal();
    }

    public Method getNativeReadMethod() {
        if ( this.readAccessor != null ) {
            return this.readAccessor.getNativeReadMethod();
        } else {
            // This only happens if there was an error else where, such as building the initial declaration binding
            // return getValue to avoid null pointers, so rest of drl can attempt to build
            try {
                return this.getClass().getDeclaredMethod( "getValue",
                                                          new Class[]{InternalWorkingMemory.class, Object.class} );
            } catch ( final Exception e ) {
                throw new RuntimeException( "This is a bug. Please report to development team: " + e.getMessage(),
                                            e );
            }
        }
    }

    public String getNativeReadMethodName() {
        return readAccessor != null ? readAccessor.getNativeReadMethodName() : "getValue";
    }

    private transient String cachedTypeName;
    public String getTypeName() {
        if (cachedTypeName == null) {
        // we assume that null extractor errors are reported else where
            cachedTypeName = ( getExtractor() != null && getDeclarationClass() != null )
                             ? canonicalName(getDeclarationClass())
                             : "java.lang.Object";
        }
        return cachedTypeName;
    }

    private transient String cachedBoxedTypeName;
    public String getBoxedTypeName() {
        if (cachedBoxedTypeName == null) {
            // we assume that null extractor errors are reported else where
            cachedBoxedTypeName = getExtractor() != null ? canonicalName(convertFromPrimitiveType(getDeclarationClass())) : "java.lang.Object";
        }
        return cachedBoxedTypeName;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public String toString() {
        if (readAccessor instanceof ClassFieldReader && !((ClassFieldReader) readAccessor).hasReadAccessor()) {
            return "(null) " + this.identifier;
        } else {
            return "(" + this.readAccessor.getValueType() + ") " + this.identifier;
        }
    }

    public int hashCode() {
        int result = 29 * this.pattern.getOffset();
        result += 31 * this.readAccessor.hashCode();
        result += 37 * this.identifier.hashCode();
        return result;
    }

    public boolean equals(final Object object) {
        if ( this == object ) {
            return true;
        }

        if ( object == null || getClass() != object.getClass() ) {
            return false;
        }

        final Declaration other = (Declaration) object;

        return this.pattern.getOffset() == other.pattern.getOffset() && this.identifier.equals( other.identifier ) && this.readAccessor.equals( other.readAccessor );
    }

    public boolean isInternalFact() {
        return internalFact;
    }

    public Declaration clone() {
        return new Declaration( this.identifier,
                                this.readAccessor,
                                this.pattern );
    }

    public Declaration cloneWithPattern() {
        return new Declaration( this.identifier,
                                this.readAccessor,
                                new Pattern( this.pattern.getIndex(),
                                             this.pattern.getOffset(),
                                             this.pattern.getObjectType(),
                                             getIdentifier(),
                                             isInternalFact()) );
    }
}
