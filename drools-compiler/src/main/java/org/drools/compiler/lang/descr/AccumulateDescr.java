/*
 * Copyright 2005 Red Hat, Inc. and/or its affiliates.
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

package org.drools.compiler.lang.descr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A descr class for accumulate node
 */
public class AccumulateDescr extends PatternSourceDescr
    implements
    ConditionalElementDescr,
    PatternDestinationDescr,
    MultiPatternDestinationDescr {

    private static final long                 serialVersionUID = 510l;

    private BaseDescr                         input;
    private String                            initCode;
    private String                            actionCode;
    private String                            reverseCode;
    private String                            resultCode;
    private String[]                          declarations;
    private String                            className;
    private List<AccumulateFunctionCallDescr> functions        = null;

    @SuppressWarnings("unchecked")
    public void readExternal( ObjectInput in ) throws IOException,
                                              ClassNotFoundException {
        super.readExternal( in );
        input = (BaseDescr) in.readObject();
        initCode = (String) in.readObject();
        actionCode = (String) in.readObject();
        reverseCode = (String) in.readObject();
        resultCode = (String) in.readObject();
        declarations = (String[]) in.readObject();
        className = (String) in.readObject();
        functions = (List<AccumulateDescr.AccumulateFunctionCallDescr>) in.readObject();
    }

    public void writeExternal( ObjectOutput out ) throws IOException {
        super.writeExternal( out );
        out.writeObject( input );
        out.writeObject( initCode );
        out.writeObject( actionCode );
        out.writeObject( reverseCode );
        out.writeObject( resultCode );
        out.writeObject( declarations );
        out.writeObject( className );
        out.writeObject( functions );
    }

    public int getLine() {
        return this.input.getLine();
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName( final String classMethodName ) {
        this.className = classMethodName;
    }

    public String[] getDeclarations() {
        return this.declarations;
    }

    public void setDeclarations( final String[] declarations ) {
        this.declarations = declarations;
    }

    public String getActionCode() {
        return this.actionCode;
    }

    public void setActionCode( final String actionCode ) {
        this.actionCode = actionCode;
    }

    public String getInitCode() {
        return this.initCode;
    }

    public void setInitCode( final String initCode ) {
        this.initCode = initCode;
    }

    public String getResultCode() {
        return this.resultCode;
    }

    public void setResultCode( final String resultCode ) {
        this.resultCode = resultCode;
    }

    public String toString() {
        return "[Accumulate: input=" + this.input.toString() + "]";
    }

    public void addDescr( final BaseDescr patternDescr ) {
        throw new UnsupportedOperationException( "Can't add descriptors to " + this.getClass().getName() );
    }

    public boolean removeDescr(BaseDescr baseDescr) {
        throw new UnsupportedOperationException("Can't remove descriptors from "+this.getClass().getName());
    }

    public void insertBeforeLast( final Class< ? > clazz,
                                  final BaseDescr baseDescr ) {
        throw new UnsupportedOperationException( "Can't add descriptors to " + this.getClass().getName() );
    }

    public List<BaseDescr> getDescrs() {
        // nothing to do
        return Collections.emptyList();
    }

    public void addOrMerge( BaseDescr baseDescr ) {
        throw new UnsupportedOperationException( "Can't add descriptors to " + this.getClass().getName() );
    }

    public String getReverseCode() {
        return reverseCode;
    }

    public void setReverseCode( String reverseCode ) {
        this.reverseCode = reverseCode;
    }

    public List<AccumulateFunctionCallDescr> getFunctions() {
        if ( functions == null ) {
            return Collections.emptyList();
        }
        return functions;
    }

    public void addFunction( String function,
                             String bind,
                             boolean unify,
                             String[] params ) {
        addFunction( new AccumulateFunctionCallDescr( function,
                                                      bind,
                                                      unify,
                                                      params ) );
    }

    public void addFunction( AccumulateFunctionCallDescr function ) {
        if ( functions == null ) {
            functions = new ArrayList<AccumulateDescr.AccumulateFunctionCallDescr>();
        }
        this.functions.add( function );
    }

    public boolean removeFunction( AccumulateFunctionCallDescr function ) {
        return functions != null && functions.remove(function);
    }

    public boolean isExternalFunction() {
        return functions != null && !functions.isEmpty();
    }

    public PatternDescr getInputPattern() {
        if ( isSinglePattern() ) {
            if( this.input instanceof PatternDescr ) {
                return (PatternDescr) this.input;
            } else {
                BaseDescr firstDescr = ((AndDescr)this.input).getDescrs().get( 0 );
                return firstDescr instanceof PatternDescr ? (PatternDescr) firstDescr : null;
            }
        }
        return null;
    }

    public void setInputPattern( final PatternDescr inputPattern ) {
        this.input = inputPattern;
    }

    public BaseDescr getInput() {
        return input;
    }

    public void setInput( BaseDescr input ) {
        this.input = input;
    }

    public boolean isSinglePattern() {
        return this.input instanceof PatternDescr || (this.input instanceof AndDescr && ((AndDescr) this.input).getDescrs().size() == 1);
    }

    public boolean isMultiPattern() {
        return !isSinglePattern();
    }

    public boolean hasValidInput() {
        // TODO: need to check that there are no OR occurrences
        return this.input != null;
    }

    public boolean isMultiFunction() {
        return functions != null && functions.size() > 1;
    }

    public static class AccumulateFunctionCallDescr
        implements
        Serializable {
        private static final long serialVersionUID = 520l;

        private final String      function;
        private final String      bind;
        private final boolean     unification;
        private final String[]    params;

        public AccumulateFunctionCallDescr(String function,
                                           String bind,
                                           boolean unify,
                                           String[] params) {
            this.function = function;
            this.bind = bind;
            this.unification = unify;
            this.params = params;
        }

        public String getFunction() {
            return function;
        }
        
        public String getBind() {
            return bind;
        }

        public String[] getParams() {
            return params;
        }

        public boolean isUnification() {
            return unification;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((function == null) ? 0 : function.hashCode());
            result = prime * result + ((bind == null) ? 0 : bind.hashCode());
            result = prime * result + Arrays.hashCode( params );
            return result;
        }

        @Override
        public boolean equals( Object obj ) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            AccumulateFunctionCallDescr other = (AccumulateFunctionCallDescr) obj;
            if ( function == null ) {
                if ( other.function != null ) return false;
            } else if ( !function.equals( other.function ) ) return false;


            if ( bind == null ) {
                if ( other.bind != null ) return false;
            } else if ( !bind.equals( other.bind ) ) {
                return false;
            }

            return Arrays.equals( params, other.params );
        }
    }

    public void accept(DescrVisitor visitor) {
        visitor.visit(this);
    }

}
