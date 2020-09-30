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

package org.drools.core.base.mvel;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.drools.core.base.EvaluatorWrapper;
import org.drools.core.base.ModifyInterceptor;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.MVELDialectRuntimeData;
import org.drools.core.spi.GlobalResolver;
import org.drools.core.spi.KnowledgeHelper;
import org.drools.core.spi.Tuple;
import org.kie.api.definition.rule.Rule;
import org.mvel2.DataConversion;
import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.Interceptor;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.util.SimpleVariableSpaceModel;

import static org.drools.core.rule.constraint.EvaluatorHelper.WM_ARGUMENT;

public class MVELCompilationUnit
    implements
    Externalizable,
    Cloneable {

    private static final long                    serialVersionUID = 510l;

    private String                               name;

    private String                               expression;

    private String[]                             globalIdentifiers;
    private EvaluatorWrapper[]                   operators;

    private Declaration[]                        previousDeclarations;
    private Declaration[]                        localDeclarations;
    private String[]                             otherIdentifiers;

    private String[]                             inputIdentifiers;
    private String[]                             inputTypes;

    private boolean                              strictMode;
    
    private boolean                              readLocalsFromTuple;
    
    private SimpleVariableSpaceModel             varModel;

    private int                                  allVarsLength;
        
    private final Object                         mvelCompilerFlagsMutex = new Object();

    public static final Map<String, Interceptor> INTERCEPTORS = new InterceptorMap();

    public enum Scope {
        CONSTRAINT, CONSEQUENCE, EXPRESSION;

        public boolean hasRule() {
            return this != CONSTRAINT;
        }
    }

    static {
        //for handling dates as string literals
        DataConversion.addConversionHandler( Date.class,
                                             new MVELDateCoercion() );
        DataConversion.addConversionHandler( Calendar.class,
                                             new MVELCalendarCoercion() );

        // always use mvel reflective optimizer
        OptimizerFactory.setDefaultOptimizer( OptimizerFactory.SAFE_REFLECTIVE );
    }

    private static final Map<String, Class< ? >> primitivesMap    = new HashMap<String, Class< ? >>();
    static {
        primitivesMap.put( "int",
                           int.class );
        primitivesMap.put( "boolean",
                           boolean.class );
        primitivesMap.put( "float",
                           float.class );
        primitivesMap.put( "long",
                           long.class );
        primitivesMap.put( "short",
                           short.class );
        primitivesMap.put( "byte",
                           byte.class );
        primitivesMap.put( "double",
                           double.class );
        primitivesMap.put( "char",
                           char.class );
    }

    public MVELCompilationUnit() {
    }

    public MVELCompilationUnit(String name,
                               String expression,
                               String[] globalIdentifiers,
                               EvaluatorWrapper[] operators,
                               Declaration[] previousDeclarations,
                               Declaration[] localDeclarations,
                               String[] otherIdentifiers,
                               String[] inputIdentifiers,
                               String[] inputTypes,
                               boolean strictMode,
                               boolean readLocalsFromTuple ) {
        this.name = name;
        this.expression = expression;

        this.globalIdentifiers = globalIdentifiers;
        this.operators = operators;

        this.previousDeclarations = previousDeclarations;
        this.localDeclarations = localDeclarations;
        this.otherIdentifiers = otherIdentifiers;

        this.inputIdentifiers = inputIdentifiers;
        this.inputTypes = inputTypes;

        this.strictMode = strictMode;
        
        this.readLocalsFromTuple = readLocalsFromTuple;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof MVELCompilationUnit)) {
            return false;
        }

        MVELCompilationUnit other = (MVELCompilationUnit) obj;

        return expression.equals( other.expression ) &&
               Arrays.equals(previousDeclarations, other.previousDeclarations) &&
               Arrays.equals(localDeclarations, other.localDeclarations);
    }

    @Override
    public int hashCode() {
        return 23 * expression.hashCode() +
               29 * Arrays.hashCode( previousDeclarations ) +
               31 * Arrays.hashCode( localDeclarations );
    }

    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeUTF( name );

        out.writeUTF( expression );

        out.writeObject( globalIdentifiers );
        out.writeObject( operators );

        out.writeObject( previousDeclarations );
        out.writeObject( localDeclarations );
        out.writeObject( otherIdentifiers );

        out.writeObject( inputIdentifiers );
        out.writeObject( inputTypes );

        out.writeBoolean( strictMode );
        
        out.writeBoolean( readLocalsFromTuple );
    }

    public void readExternal( ObjectInput in ) throws IOException,
                                              ClassNotFoundException {
        name = in.readUTF();
        expression = in.readUTF();

        globalIdentifiers = (String[]) in.readObject();
        operators = (EvaluatorWrapper[]) in.readObject();

        previousDeclarations = (Declaration[]) in.readObject();
        localDeclarations = (Declaration[]) in.readObject();
        otherIdentifiers = (String[]) in.readObject();

        inputIdentifiers = (String[]) in.readObject();
        inputTypes = (String[]) in.readObject();

        strictMode = in.readBoolean();
        
        readLocalsFromTuple = in.readBoolean();
    }    

    public Serializable getCompiledExpression(MVELDialectRuntimeData runtimeData) {
        return getCompiledExpression(runtimeData, null);
    }

    public Serializable getCompiledExpression(ParserConfiguration conf) {
        return getCompiledExpression(conf, null);
    }

    public Serializable getCompiledExpression(MVELDialectRuntimeData runtimeData, Object evaluationContext) {
        return getCompiledExpression(runtimeData.getParserConfiguration(), evaluationContext);
    }

    public Serializable getCompiledExpression(ParserConfiguration conf, Object evaluationContext) {
        final ParserContext parserContext = new ParserContext( conf, evaluationContext );
        if ( MVELDebugHandler.isDebugMode() ) {
            parserContext.setDebugSymbols( true );
        }

        parserContext.setStrictTypeEnforcement( strictMode );
        parserContext.setStrongTyping( strictMode );
        parserContext.setIndexAllocation( true );

        if ( INTERCEPTORS != null ) {
            parserContext.setInterceptors(INTERCEPTORS);
        }

        parserContext.addIndexedInput( inputIdentifiers );

        String identifier = null;
        String type = null;
        try {
            for ( int i = 0, length = inputIdentifiers.length; i < length; i++ ) {
                identifier = inputIdentifiers[i];
                type = inputTypes[i];
                Class< ? > cls = loadClass( conf.getClassLoader(),
                                            inputTypes[i] );
                parserContext.addInput( inputIdentifiers[i],
                                        cls );
            }
        } catch ( ClassNotFoundException e ) {
            throw new RuntimeException( "Unable to resolve class '" + type + "' for identifier '" + identifier );
        }

        parserContext.setSourceFile( name );

        String[] varNames = parserContext.getIndexedVarNames();

        ExecutableStatement stmt = (ExecutableStatement) compile( expression, parserContext );

        Set<String> localNames = parserContext.getVariables().keySet();

        parserContext.addIndexedLocals(localNames);

        String[] locals = localNames.toArray(new String[localNames.size()]);
        String[] allVars = new String[varNames.length + locals.length];

        System.arraycopy(varNames, 0, allVars, 0, varNames.length);
        System.arraycopy(locals, 0, allVars, varNames.length, locals.length);

        this.varModel = new SimpleVariableSpaceModel(allVars);
        this.allVarsLength = allVars.length;

        return stmt;
    }

    public VariableResolverFactory createFactory() {
        Object[] vals = new Object[inputIdentifiers.length];

        VariableResolverFactory factory = varModel.createFactory( vals );
        factory.setNextFactory( new DroolsVarFactory() );
        return factory;
    }
    
    public VariableResolverFactory getFactory(final Object knowledgeHelper,
                                              final Declaration[] prevDecl,
                                              final Rule rule,
                                              final Tuple tuples,
                                              final Object[] otherVars,
                                              final InternalWorkingMemory workingMemory,
                                              final GlobalResolver globals) {
        VariableResolverFactory factory = createFactory();
        updateFactory(knowledgeHelper, prevDecl, rule, null, knowledgeHelper, tuples, otherVars, workingMemory, globals, factory );
        return factory;
    }

    public VariableResolverFactory getFactory(final Object knowledgeHelper,
                                              final Declaration[] prevDecl,
                                              final Rule rule,
                                              final InternalFactHandle rightHandle,
                                              final Tuple tuple,
                                              final Object[] otherVars,
                                              final InternalWorkingMemory workingMemory,
                                              final GlobalResolver globals) {
        VariableResolverFactory factory = createFactory();
        updateFactory(knowledgeHelper, prevDecl, rule, rightHandle, rightHandle != null ? rightHandle.getObject() : null, tuple, otherVars, workingMemory, globals, factory);
        return factory;
    }
    
    public void updateFactory( InternalFactHandle rightHandle,
                               Tuple tuple,
                               Object[] localVars,
                               InternalWorkingMemory workingMemory,
                               GlobalResolver globalResolver,
                               VariableResolverFactory factory ) {
        updateFactory( null, null, null, rightHandle, rightHandle != null ? rightHandle.getObject() : null, tuple, localVars, workingMemory, globalResolver, factory );
    }    
    
    private void updateFactory( Object knowledgeHelper,
                                Declaration[] prevDecl,
                                Rule rule,
                                InternalFactHandle rightHandle,
                                Object rightObject,
                                Tuple tuple,
                                Object[] otherVars,
                                InternalWorkingMemory workingMemory,
                                GlobalResolver globals,
                                VariableResolverFactory factory ) {
        int varLength = inputIdentifiers.length;

        int i = 0;
        if ( "this".equals( inputIdentifiers[0] ) ) {
            factory.getIndexedVariableResolver( i++ ).setValue( rightObject );
        }
        factory.getIndexedVariableResolver( i++ ).setValue( knowledgeHelper );
        factory.getIndexedVariableResolver( i++ ).setValue( knowledgeHelper );
        if (inputIdentifiers.length > i && "rule".equals( inputIdentifiers[i] )) {
            factory.getIndexedVariableResolver( i++ ).setValue( rule );
        }

        if ( globalIdentifiers != null ) {
            for (String globalIdentifier : globalIdentifiers) {
                if (WM_ARGUMENT.equals( globalIdentifier )) {
                    factory.getIndexedVariableResolver( i++ ).setValue( workingMemory );
                } else {
                    factory.getIndexedVariableResolver( i++ ).setValue( globals.resolveGlobal( globalIdentifier ) );
                }
            }
        }

        InternalFactHandle[] handles = tuple instanceof LeftTuple ? ( (LeftTuple) tuple ).toFactHandles() : null;
        if ( operators.length > 0 ) {
            for (EvaluatorWrapper operator : operators) {
                // TODO: need to have one operator per working memory
                factory.getIndexedVariableResolver(i++).setValue(operator);
                operator.loadHandles(handles, rightHandle);
            }
        }

        Object[] objs = null;

        if ( tuple != null ) {
            if (handles == null) {
                objs = tuple.toObjects();
            }
            if ( this.previousDeclarations != null && this.previousDeclarations.length > 0 ) {
                // Consequences with 'or's will have different declaration offsets, so use the one's from the RTN's subrule.
                if ( prevDecl == null ) {
                    // allows the caller to override the member var
                    // used for rules, salience and timers so they work with 'or' CEs
                    prevDecl =  this.previousDeclarations;
                }

                for (Declaration decl : prevDecl) {
                    int offset = decl.getOffset();
                    Object o = decl.getValue(workingMemory, objs != null ? objs[offset] : handles[offset].getObject());
                    factory.getIndexedVariableResolver(i++).setValue(o);
                }
            }
        }

        if ( this.localDeclarations != null && this.localDeclarations.length > 0 ) {
            for ( Declaration decl : this.localDeclarations ) {
                Object value;
                if( readLocalsFromTuple && tuple != null ) {
                    int offset = decl.getOffset();
                    value = decl.getValue( workingMemory,
                                           objs != null ? objs[offset] : handles[offset].getObject() );
                } else {
                    value = decl.getValue( workingMemory,
                                          rightObject ); 
                }
                factory.getIndexedVariableResolver( i++ ).setValue( value );
            }
        }

        int otherVarsPos = 0;
        if ( otherVars != null ) {
            otherVarsPos = i;
            for ( Object o : otherVars ) {
                factory.getIndexedVariableResolver( i++ ).setValue( o );
            }
        }
        int otherVarsLength = i - otherVarsPos;
        
        for ( i = varLength; i < this.allVarsLength; i++ ) {
            // null all local vars
            factory.getIndexedVariableResolver( i ).setValue( null );
        }
        
        DroolsVarFactory df = ( DroolsVarFactory )  factory.getNextFactory();

        df.setOtherVarsPos( otherVarsPos );
        df.setOtherVarsLength( otherVarsLength );
        
        if ( knowledgeHelper instanceof KnowledgeHelper ) {
            KnowledgeHelper kh = ( KnowledgeHelper ) knowledgeHelper;
            df.setKnowledgeHelper( kh );
        }        
    }

    public static InternalFactHandle getFactHandle( Declaration declaration,
                                                    InternalFactHandle[] handles ) {
        return handles != null && handles.length > declaration.getOffset() ? handles[declaration.getOffset()] : null;
    }

    private static Serializable compile( final String text,
                                         final ParserContext parserContext ) {
        if ( !( MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL &&
                MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING &&
                MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION &&
                MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS ) ) {
            synchronized ( mvelCompilerFlagsMutex ) {
                MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;
                MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
                MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION = true;
                MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = true;
            }
        }
        
        if ( MVELDebugHandler.isDebugMode() ) {
            parserContext.setDebugSymbols( true );
        }

        return MVEL.compileExpression( text.trim(),
                                       parserContext );
    }

    public static Class loadClass( ClassLoader classLoader,
                                   String className ) throws ClassNotFoundException {
        Class cls = primitivesMap.get( className );
        if ( cls == null ) {
            cls = classLoader.loadClass( className );
        }
        return cls;
    }

    public void replaceDeclaration( Declaration declaration,
                                    Declaration resolved ) {
        if ( previousDeclarations != null ) {
            for ( int i = 0; i < previousDeclarations.length; i++ ) {
                if ( previousDeclarations[i].equals( declaration ) ) {
                    previousDeclarations[i] = resolved;
                }
            }
        }
        if ( localDeclarations != null ) {
            for ( int i = 0; i < localDeclarations.length; i++ ) {
                if ( localDeclarations[i].equals( declaration ) ) {
                    localDeclarations[i] = resolved;
                }
            }
        }
    }

    @Override
    public MVELCompilationUnit clone() {
        Declaration[] clonedPreviousDeclarations = null;
        if (previousDeclarations != null) {
            clonedPreviousDeclarations = new Declaration[previousDeclarations.length];
            System.arraycopy(previousDeclarations, 0, clonedPreviousDeclarations, 0, previousDeclarations.length);
        }
        Declaration[] clonedLocalDeclarations = null;
        if (localDeclarations != null) {
            clonedLocalDeclarations = new Declaration[localDeclarations.length];
            System.arraycopy(localDeclarations, 0, clonedLocalDeclarations, 0, localDeclarations.length);
        }

        MVELCompilationUnit unit = new MVELCompilationUnit( name,
                                                            expression,
                                                            globalIdentifiers,
                                                            operators,
                                                            clonedPreviousDeclarations,
                                                            clonedLocalDeclarations,
                                                            otherIdentifiers,
                                                            inputIdentifiers,
                                                            inputTypes,
                                                            strictMode,
                                                            readLocalsFromTuple );
        unit.varModel = this.varModel;
        return unit;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getName() {
        return name;
    }
    
    public String[] getGlobalIdentifiers() {
        return globalIdentifiers;
    }

    public Declaration[] getPreviousDeclarations() {
        return previousDeclarations;
    }

    public void setPreviousDeclarations( Declaration[] previousDeclarations ) {
        this.previousDeclarations = previousDeclarations;
    }

    public Declaration[] getLocalDeclarations() {
        return localDeclarations;
    }

    public String[] getOtherIdentifiers() {
        return otherIdentifiers;
    }

    public String[] getInputIdentifiers() {
        return inputIdentifiers;
    }

    public String[] getInputTypes() {
        return inputTypes;
    }
    
    public boolean isStrictMode() {
        return strictMode;
    }

    public static Map getInterceptors() {
        return INTERCEPTORS;
    }

    public static Map<String, Class< ? >> getPrimitivesmap() {
        return primitivesMap;
    }

    public static class DroolsVarFactory implements VariableResolverFactory {
    
        private KnowledgeHelper knowledgeHelper;
    
        private int             otherVarsPos;
        private int             otherVarsLength;
        
        public KnowledgeHelper getKnowledgeHelper() {
            return this.knowledgeHelper ;
        }
    
        public void setKnowledgeHelper(KnowledgeHelper kh) {
            this.knowledgeHelper = kh;
        }
    
        public int getOtherVarsPos() {
            return otherVarsPos;
        }
    
        public void setOtherVarsPos( int otherVarsPos ) {
            this.otherVarsPos = otherVarsPos;
        }
    
        public int getOtherVarsLength() {
            return otherVarsLength;
        }
    
        public void setOtherVarsLength( int otherVarsLength ) {
            this.otherVarsLength = otherVarsLength;
        }

        public VariableResolver createIndexedVariable( int index,
                                                       String name,
                                                       Object value ) {
            throw new UnsupportedOperationException(); 
        }
    
        public VariableResolver getIndexedVariableResolver( int index ) {
            throw new UnsupportedOperationException(); 
        }
    
        public VariableResolver createVariable( String name,
                                                Object value ) {
            throw new UnsupportedOperationException();            
        }
    
        public VariableResolver createVariable( String name,
                                                Object value,
                                                Class< ? > type ) {
            throw new UnsupportedOperationException();            
        }
    
        public VariableResolver getVariableResolver( String name ) {
            return null;
        }
    
        public boolean isResolveable( String name ) {
            return false;
        }
    
        public boolean isTarget( String name ) {
            return false;
        }
    
        public Set<String> getKnownVariables() {
            return Collections.emptySet();
        }
    
        public void clear() { }
    
        public boolean isIndexedFactory() {
            return false;
        }

        public VariableResolver createIndexedVariable(int index,
                                                      String name,
                                                      Object value,
                                                      Class< ? > typee) {
            // TODO Auto-generated method stub
            return null;
        }

        public VariableResolver setIndexedVariableResolver(int index,
                                                           VariableResolver variableResolver) {
            // TODO Auto-generated method stub
            return null;
        }

        public VariableResolverFactory getNextFactory() {
            // TODO Auto-generated method stub
            return null;
        }

        public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory) {
            // TODO Auto-generated method stub
            return null;
        }

        public int variableIndexOf(String name) {
            // TODO Auto-generated method stub
            return 0;
        }

        public boolean tiltFlag() {
            // TODO Auto-generated method stub
            return false;
        }

        public void setTiltFlag(boolean tilt) {
            // TODO Auto-generated method stub
        }
    }
    
    public static class PropertyHandlerFactoryFixer extends PropertyHandlerFactory {
        public static  Map<Class, PropertyHandler> getPropertyHandlerClass() {
            return propertyHandlerClass;
        }
    }

    private static class InterceptorMap implements Map<String, Interceptor>, Serializable {
        public int size() {
            return 1;
        }

        public boolean isEmpty() {
            return false;
        }

        public boolean containsKey(Object key) {
            return "Modify".equals(key);
        }

        public boolean containsValue(Object value) {
            return false;
        }

        public Interceptor get(Object key) {
            return new ModifyInterceptor();
        }

        public Interceptor put(String key, Interceptor value) {
            throw new UnsupportedOperationException();
        }

        public Interceptor remove(Object key) {
            throw new UnsupportedOperationException();
        }

        public void putAll(Map<? extends String, ? extends Interceptor> m) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public Set<String> keySet() {
            return new HashSet<String>() {{
                add("Modify");
            }};
        }

        public Collection<Interceptor> values() {
            return new ArrayList<Interceptor>() {{
                add(new ModifyInterceptor());
            }};
        }

        public Set<Entry<String, Interceptor>> entrySet() {
            return new HashSet<Entry<String, Interceptor>>() {{
                add(new Entry<String, Interceptor>() {
                    public String getKey() {
                        return "Modify";
                    }
                    public Interceptor getValue() {
                        return new ModifyInterceptor();
                    }
                    public Interceptor setValue(Interceptor value) {
                        throw new UnsupportedOperationException();
                    }
                });
            }};
        }
    }

    @Override
    public String toString() {
        return expression;
    }
}
