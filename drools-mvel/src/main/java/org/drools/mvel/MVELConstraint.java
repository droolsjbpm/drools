/*
 * Copyright (c) 2020. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.mvel;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.base.ClassFieldReader;
import org.drools.core.base.DroolsQuery;
import org.drools.core.base.EvaluatorWrapper;
import org.drools.core.common.DroolsObjectInputStream;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.reteoo.PropertySpecificUtil;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.rule.ContextEntry;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.IndexableConstraint;
import org.drools.core.rule.MutableTypeConstraint;
import org.drools.core.rule.constraint.ConditionEvaluator;
import org.drools.core.spi.AcceptsReadAccessor;
import org.drools.core.spi.FieldValue;
import org.drools.core.spi.InternalReadAccessor;
import org.drools.core.spi.ReadAccessor;
import org.drools.core.spi.Tuple;
import org.drools.core.spi.TupleValueExtractor;
import org.drools.core.util.AbstractHashTable.FieldIndex;
import org.drools.core.util.MemoryUtil;
import org.drools.core.util.bitmask.BitMask;
import org.drools.core.util.index.IndexUtil;
import org.drools.mvel.ConditionAnalyzer.CombinedCondition;
import org.drools.mvel.ConditionAnalyzer.Condition;
import org.drools.mvel.ConditionAnalyzer.EvaluatedExpression;
import org.drools.mvel.ConditionAnalyzer.Expression;
import org.drools.mvel.ConditionAnalyzer.FieldAccessInvocation;
import org.drools.mvel.ConditionAnalyzer.Invocation;
import org.drools.mvel.ConditionAnalyzer.MethodInvocation;
import org.drools.mvel.ConditionAnalyzer.SingleCondition;
import org.drools.mvel.expr.MVELCompilationUnit;
import org.drools.mvel.extractors.MVELObjectClassFieldReader;
import org.drools.reflective.classloader.ProjectClassLoader;
import org.kie.api.runtime.rule.Variable;
import org.kie.internal.concurrent.ExecutorProviderFactory;
import org.mvel2.ParserConfiguration;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExecutableStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.core.reteoo.PropertySpecificUtil.allSetBitMask;
import static org.drools.core.reteoo.PropertySpecificUtil.allSetButTraitBitMask;
import static org.drools.core.reteoo.PropertySpecificUtil.getEmptyPropertyReactiveMask;
import static org.drools.core.reteoo.PropertySpecificUtil.setPropertyOnMask;
import static org.drools.core.util.ClassUtils.areNullSafeEquals;
import static org.drools.core.util.ClassUtils.getter2property;
import static org.drools.core.util.Drools.isJmxAvailable;
import static org.drools.core.util.StringUtils.codeAwareIndexOf;
import static org.drools.core.util.StringUtils.equalsIgnoreSpaces;
import static org.drools.core.util.StringUtils.extractFirstIdentifier;
import static org.drools.core.util.StringUtils.skipBlanks;

public class MVELConstraint extends MutableTypeConstraint implements IndexableConstraint, AcceptsReadAccessor {
    protected static final boolean TEST_JITTING = false;

    private static final Logger logger = LoggerFactory.getLogger(MVELConstraint.class);

    protected final transient AtomicInteger invocationCounter = new AtomicInteger(1);
    protected transient volatile boolean jitted = false;

    private Set<String> packageNames;
    protected String expression;
    private IndexUtil.ConstraintType constraintType = IndexUtil.ConstraintType.UNKNOWN;
    private Declaration[] declarations;
    private EvaluatorWrapper[] operators;
    private TupleValueExtractor indexingDeclaration;
    private InternalReadAccessor extractor;
    private boolean isUnification;
    protected boolean isDynamic;
    private FieldValue fieldValue;

    protected MVELCompilationUnit compilationUnit;

    private EvaluationContext evaluationContext = new EvaluationContext();

    protected transient volatile ConditionEvaluator conditionEvaluator;
    private transient volatile Condition analyzedCondition;

    private static final Declaration[] EMPTY_DECLARATIONS = new Declaration[0];
    private static final EvaluatorWrapper[] EMPTY_OPERATORS = new EvaluatorWrapper[0];

    public MVELConstraint() {}

    public MVELConstraint(final String packageName,
                          String expression,
                          MVELCompilationUnit compilationUnit,
                          IndexUtil.ConstraintType constraintType,
                          FieldValue fieldValue,
                          InternalReadAccessor extractor,
                          EvaluatorWrapper[] operators) {
        this.packageNames = new LinkedHashSet<>();
        this.packageNames.add(packageName);
        this.expression = expression;
        this.compilationUnit = compilationUnit;
        this.constraintType = constraintType;
        this.declarations = EMPTY_DECLARATIONS;
        this.operators = operators == null ? EMPTY_OPERATORS : operators;
        this.fieldValue = fieldValue;
        this.extractor = extractor;
    }

    public MVELConstraint(final String packageName,
                          String expression,
                          Declaration[] declarations,
                          EvaluatorWrapper[] operators,
                          MVELCompilationUnit compilationUnit,
                          boolean isDynamic) {
        this.packageNames = new LinkedHashSet<>();
        this.packageNames.add(packageName);
        this.expression = expression;
        this.declarations = declarations == null ? EMPTY_DECLARATIONS : declarations;
        this.operators = operators == null ? EMPTY_OPERATORS : operators;
        this.compilationUnit = compilationUnit;
        this.isDynamic = isDynamic;
    }

    public MVELConstraint(Collection<String> packageNames,
                          String expression,
                          Declaration[] declarations,
                          EvaluatorWrapper[] operators,
                          MVELCompilationUnit compilationUnit,
                          IndexUtil.ConstraintType constraintType,
                          TupleValueExtractor indexingDeclaration,
                          InternalReadAccessor extractor,
                          boolean isUnification) {
        this.packageNames = new LinkedHashSet<>(packageNames);
        this.expression = expression;
        this.compilationUnit = compilationUnit;
        this.constraintType = indexingDeclaration != null ? constraintType : IndexUtil.ConstraintType.UNKNOWN;
        this.declarations = declarations == null ? EMPTY_DECLARATIONS : declarations;
        this.operators = operators == null ? EMPTY_OPERATORS : operators;
        this.indexingDeclaration = indexingDeclaration;
        this.extractor = extractor;
        this.isUnification = isUnification;
    }

    protected String getAccessedClass() {
        return extractor instanceof ClassFieldReader ?
                ((ClassFieldReader) extractor).getClassName() :
                extractor instanceof MVELObjectClassFieldReader ?
                        ((MVELObjectClassFieldReader) extractor).getClassName() :
                        null;
    }

    public void setReadAccessor(InternalReadAccessor readAccessor) {
        this.extractor = readAccessor;
    }

    @Override
    public Collection<String> getPackageNames() {
        return packageNames;
    }

    @Override
    public void addPackageNames(Collection<String> otherPkgs) {
        packageNames.addAll(otherPkgs);
    }

    public String getExpression() {
        return expression;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public boolean isUnification() {
        return isUnification;
    }

    @Override
    public void unsetUnification() {
        isUnification = false;
    }

    public boolean isIndexable(short nodeType, RuleBaseConfiguration config) {
        return getConstraintType().isIndexableForNode(nodeType, this, config);
    }

    public IndexUtil.ConstraintType getConstraintType() {
        return constraintType;
    }

    public FieldValue getField() {
        return fieldValue;
    }

    public boolean isAllowed(InternalFactHandle handle, InternalWorkingMemory workingMemory) {
        if (isUnification) {
            throw new UnsupportedOperationException("Should not be called");
        }

        return evaluate(handle, workingMemory, null);
    }

    public boolean isAllowedCachedLeft(ContextEntry context, InternalFactHandle handle) {
        if (isUnification) {
            if (((UnificationContextEntry) context).getVariable() != null) {
                return true;
            }
            context = ((UnificationContextEntry) context).getContextEntry();
        }

        MvelContextEntry mvelContextEntry = (MvelContextEntry) context;
        return evaluate(handle, mvelContextEntry.workingMemory, mvelContextEntry.tuple);
    }

    public boolean isAllowedCachedRight(Tuple tuple, ContextEntry context) {
        if (isUnification) {
            DroolsQuery query = (DroolsQuery) tuple.get(0).getObject();
            Variable v = query.getVariables()[((UnificationContextEntry) context).getReader().getIndex()];

            if (v != null) {
                return true;
            }
            context = ((UnificationContextEntry) context).getContextEntry();
        }

        MvelContextEntry mvelContextEntry = (MvelContextEntry) context;
        return evaluate(mvelContextEntry.rightHandle, mvelContextEntry.workingMemory, tuple);
    }

    protected boolean evaluate(InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple) {
        if (!jitted) {
            int jittingThreshold = TEST_JITTING ? 0 : workingMemory.getKnowledgeBase().getConfiguration().getJittingThreshold();
            if (conditionEvaluator == null) {
                if (jittingThreshold == 0 && !isDynamic) { // Only for test purposes or when jitting is enforced at first evaluation
                    synchronized (this) {
                        if (conditionEvaluator == null) {
                            conditionEvaluator = forceJitEvaluator(handle, workingMemory, tuple);
                        }
                    }
                } else {
                    conditionEvaluator = createMvelConditionEvaluator(workingMemory);
                }
            }

            if (jittingThreshold != 0 && !isDynamic && invocationCounter.getAndIncrement() == jittingThreshold) {
                jitEvaluator(handle, workingMemory, tuple);
            }
        }
        try {
            return conditionEvaluator.evaluate(handle, workingMemory, tuple);
        } catch (Exception e) {
            throw new ConstraintEvaluationException(expression, evaluationContext, e);
        }
    }

    protected ConditionEvaluator createMvelConditionEvaluator(InternalWorkingMemory workingMemory) {
        if (compilationUnit != null) {
            MVELDialectRuntimeData data = getMVELDialectRuntimeData(workingMemory);
            ExecutableStatement statement = (ExecutableStatement)compilationUnit.getCompiledExpression(data, evaluationContext);
            ParserConfiguration configuration = statement instanceof CompiledExpression ?
                    ((CompiledExpression) statement).getParserConfiguration() :
                    data.getParserConfiguration();
            return new MVELConditionEvaluator(compilationUnit, configuration, statement, declarations, operators, getAccessedClass());
        } else {
            return new MVELConditionEvaluator(getParserConfiguration(workingMemory), expression, declarations, operators, getAccessedClass());
        }
    }

    protected ConditionEvaluator forceJitEvaluator(InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple) {
        ConditionEvaluator mvelEvaluator = createMvelConditionEvaluator(workingMemory);
        try {
            mvelEvaluator.evaluate(handle, workingMemory, tuple);
        } catch (ClassCastException cce) {
        } catch (Exception e) {
            return createMvelConditionEvaluator(workingMemory);
        }
        return executeJitting(handle, workingMemory, tuple, mvelEvaluator);
    }

    protected void jitEvaluator(InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple) {
        jitted = true;
        ExecutorHolder.executor.execute(new ConditionJitter(this, handle, workingMemory, tuple));
    }

    private static class ConditionJitter implements Runnable {
        private MVELConstraint mvelConstraint;
        private InternalFactHandle rightHandle;
        private InternalWorkingMemory workingMemory;
        private Tuple tuple;

        private ConditionJitter( MVELConstraint mvelConstraint, InternalFactHandle rightHandle, InternalWorkingMemory workingMemory, Tuple tuple) {
            this.mvelConstraint = mvelConstraint;
            this.rightHandle = rightHandle;
            this.workingMemory = workingMemory;
            this.tuple = tuple;
        }

        public void run() {
            mvelConstraint.conditionEvaluator = mvelConstraint.executeJitting(rightHandle, workingMemory, tuple, mvelConstraint.conditionEvaluator);
            mvelConstraint = null;
            rightHandle = null;
            workingMemory = null;
            tuple = null;
        }
    }

    private static class ExecutorHolder {
        private static final Executor executor = ExecutorProviderFactory.getExecutorProvider().getExecutor();
    }

    private ConditionEvaluator executeJitting(InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple, ConditionEvaluator mvelEvaluator) {
        InternalKnowledgeBase kBase = workingMemory.getKnowledgeBase();
        if (!isJmxAvailable() && MemoryUtil.permGenStats.isUsageThresholdExceeded(kBase.getConfiguration().getPermGenThreshold())) {
            return mvelEvaluator;
        }

        try {
            if (analyzedCondition == null) {
                analyzedCondition = (( MVELConditionEvaluator ) mvelEvaluator).getAnalyzedCondition(handle, workingMemory, tuple);
            }
            ClassLoader jitClassLoader = kBase.getRootClassLoader() instanceof ProjectClassLoader ?
                    ((ProjectClassLoader) kBase.getRootClassLoader()).getTypesClassLoader() :
                    kBase.getRootClassLoader();
            return ASMConditionEvaluatorJitter.jitEvaluator(expression, analyzedCondition, declarations, operators, jitClassLoader, tuple);
        } catch (Throwable t) {
            if (TEST_JITTING) {
                if (analyzedCondition == null) {
                    logger.error("Unable to analize condition for expression: " + expression, t);
                } else {
                    throw new RuntimeException("Unable to analize condition for expression: " + expression, t);
                }
            } else {
                logger.warn("Exception jitting: {}." +
                             " This is NOT an error and NOT prevent the correct execution since the constraint will be evaluated in intrepreted mode",
                            expression);
            }
        }
        return mvelEvaluator;
    }

    public ContextEntry createContextEntry() {
        if (declarations.length == 0) return null;
        ContextEntry contextEntry = new MvelContextEntry(declarations);
        if (isUnification) {
            contextEntry = new UnificationContextEntry(contextEntry, declarations[0]);
        }
        return contextEntry;
    }

    public FieldIndex getFieldIndex() {
        return new FieldIndex(extractor, indexingDeclaration);
    }

    public InternalReadAccessor getFieldExtractor() {
        return extractor;
    }

    @Override
    public TupleValueExtractor getIndexExtractor() {
        return indexingDeclaration;
    }

    public Declaration[] getRequiredDeclarations() {
        return declarations;
    }

    public EvaluatorWrapper[] getOperators() {
        return operators;
    }

    public void replaceDeclaration(Declaration oldDecl, Declaration newDecl) {
        for (int i = 0; i < declarations.length; i++) {
            if (declarations[i].equals(oldDecl)) {
                if (compilationUnit != null) {
                    compilationUnit.replaceDeclaration(declarations[i], newDecl);
                }
                declarations[i] = newDecl;

                if (indexingDeclaration != null && i == 0) {
                    // indexed MVELConstraints currently only have a single required declaration.
                    // So this is a hack that works for this limited scenario.
                    // It needs to clone first, due to unification otherwise you
                    // might change the pattern incorrectly for other nodes.
                    if (!indexingDeclaration.equals(oldDecl)) {
                        // This is true for synthetic declarations
                        indexingDeclaration = indexingDeclaration.clone();
                        ((Declaration) indexingDeclaration).setPattern(newDecl.getPattern());
                    } else {
                        indexingDeclaration = newDecl;
                    }
                }
                break;
            }
        }
    }

    // Slot specific

    @Override
    public BitMask getListenedPropertyMask(Class modifiedClass, List<String> settableProperties) {
        return analyzedCondition != null ?
                calculateMask(modifiedClass, settableProperties) :
                calculateMaskFromExpression(settableProperties);
    }

    private BitMask calculateMaskFromExpression(List<String> settableProperties) {
        BitMask mask = getEmptyPropertyReactiveMask(settableProperties.size());
        String[] simpleExpressions = expression.split("\\Q&&\\E|\\Q||\\E");

        for (String simpleExpression : simpleExpressions) {
            List<String> properties = getPropertyNamesFromSimpleExpression(simpleExpression);
            if (properties.isEmpty()) {
                return allSetBitMask();
            }
            boolean firstProp = true;
            for (String propertyName : properties) {
                if (propertyName == null || propertyName.equals("this") || propertyName.length() == 0) {
                    return allSetButTraitBitMask();
                }
                int pos = settableProperties.indexOf(propertyName);
                if (pos < 0) {
                    if (Character.isUpperCase(propertyName.charAt(0))) {
                        propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                        pos = settableProperties.indexOf(propertyName);
                    } else {
                        propertyName = findBoundVariable(propertyName);
                        if (propertyName != null) {
                            pos = settableProperties.indexOf(propertyName);
                        }
                    }
                }
                if (pos >= 0) {
                    mask = mask.set(pos + PropertySpecificUtil.CUSTOM_BITS_OFFSET);
                } else {
                    // if it is not able to find the property name it could be a function invocation so property reactivity shouldn't filter anything
                    if (firstProp) {
                        return allSetBitMask();
                    }
                }
                firstProp = false;
            }
        }

        return mask;
    }

    private String findBoundVariable(String variable) {
        for (Declaration declaration : declarations) {
            if (declaration.getIdentifier().equals(variable)) {
                InternalReadAccessor accessor = declaration.getExtractor();
                if (accessor instanceof ClassFieldReader) {
                    return ((ClassFieldReader) accessor).getFieldName();
                }
            }
        }
        return null;
    }

    private List<String> getPropertyNamesFromSimpleExpression(String expression) {
        List<String> names = new ArrayList<>();
        for (int cursor = 0; cursor < expression.length(); cursor = nextPropertyName(expression, names, cursor));
        return names;
    }

    private int nextPropertyName(String expression, List<String> names, int cursor) {
        StringBuilder propertyNameBuilder = new StringBuilder();
        cursor = extractFirstIdentifier(expression, propertyNameBuilder, cursor);
        if (propertyNameBuilder.length() == 0) {
            return cursor;
        }

        boolean isAccessor = false;
        String propertyName = propertyNameBuilder.toString();
        if (propertyName.equals("this")) {
            cursor = skipBlanks(expression, cursor);
            if (cursor >= expression.length() || expression.charAt(cursor) != '.') {
                names.add("this");
                return cursor;
            }
            propertyNameBuilder = new StringBuilder();
            extractFirstIdentifier(expression, propertyNameBuilder, cursor);
            propertyName = propertyNameBuilder.toString();
        } else if (propertyName.equals("null") || propertyName.equals("true") || propertyName.equals("false")) {
            propertyNameBuilder = new StringBuilder();
            extractFirstIdentifier(expression, propertyNameBuilder, cursor);
            propertyName = propertyNameBuilder.toString();
        }

        if (propertyName.startsWith("is") || propertyName.startsWith("get")) {
            int exprPos = expression.indexOf(propertyName);
            int propNameEnd = exprPos + propertyName.length();
            if (expression.length() > propNameEnd + 1 && expression.charAt(propNameEnd) == '(') {
                int argsEnd = expression.indexOf(')', propNameEnd);
                // the getter has to be used for property reactivity only if it's a true getter (doesn't have any argument)
                if (expression.substring(propNameEnd + 1, argsEnd).trim().isEmpty()) {
                    propertyName = getter2property(propertyName);
                    isAccessor = true;
                }
            }
        }

        if (!isAccessor) {
            String lookAhead = lookAheadIgnoringSpaces(expression, cursor);
            boolean isMethodInvocation = lookAhead != null && lookAhead.equals("(");
            if (isMethodInvocation) {
                return nextPropertyName(expression, names, cursor);
            }
        }

        if (propertyName != null && propertyName.length() > 0) {
            names.add(propertyName);
        }
        return skipOperator(expression, cursor);
    }

    private String lookAheadIgnoringSpaces(String expression, int cursor) {
        while (cursor < expression.length()) {
            char c = expression.charAt(cursor);
            if (!Character.isWhitespace(c)) {
                return "" + c;
            }
            cursor++;
        }
        return null;
    }

    private int skipOperator(String expression, int cursor) {
        if (cursor < expression.length() && expression.charAt(cursor) == '.') {
            while (cursor < expression.length() && Character.isWhitespace(expression.charAt(++cursor)));
        }

        boolean namedOperator = false;
        int i = cursor;
        for (; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (Character.isJavaIdentifierStart(ch)) {
                namedOperator = true;
            } else if (Character.isWhitespace(ch)) {
                if (namedOperator) {
                    return i + 1;
                }
            } else if (!Character.isJavaIdentifierPart(ch)) {
                return i + 1;
            }
        }
        return i;
    }

    private BitMask calculateMask(Class modifiedClass, List<String> settableProperties) {
        BitMask mask = getEmptyPropertyReactiveMask(settableProperties.size());
        if (analyzedCondition instanceof SingleCondition) {
            mask = setPropertyOnReactiveMask( modifiedClass, settableProperties, mask, ( SingleCondition ) analyzedCondition );
        } else {
            for (Condition c : ((CombinedCondition) analyzedCondition).getConditions()) {
                mask = setPropertyOnReactiveMask(modifiedClass, settableProperties, mask, (SingleCondition) c);
            }
        }
        return mask;
    }

    private BitMask setPropertyOnReactiveMask( Class modifiedClass, List<String> settableProperties, BitMask mask, SingleCondition c ) {
        String propertyName = getFirstInvokedPropertyName(modifiedClass, c.getLeft());
        return propertyName != null ?
                setPropertyOnMask(modifiedClass, mask, settableProperties, propertyName) :
                allSetBitMask();
    }

    private String getFirstInvokedPropertyName(Class modifiedClass, Expression expression) {
        if (!(expression instanceof EvaluatedExpression)) {
            return null;
        }
        List<Invocation> invocations = ((EvaluatedExpression) expression).invocations;
        Invocation invocation = invocations.get(0);

        if (invocation instanceof MethodInvocation) {
            Method method = ((MethodInvocation) invocation).getMethod();
            if (method == null) {
                if (invocations.size() > 1) {
                    invocation = invocations.get(1);
                    if (invocation instanceof MethodInvocation) {
                        method = ((MethodInvocation) invocation).getMethod();
                    } else if (invocation instanceof FieldAccessInvocation) {
                        return ((FieldAccessInvocation) invocation).getField().getName();
                    }
                } else {
                    return null;
                }
            }
            return method != null && !Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass().isAssignableFrom(modifiedClass)
                    ? getter2property(method.getName()) : null;
        }

        if (invocation instanceof FieldAccessInvocation) {
            return ((FieldAccessInvocation) invocation).getField().getName();
        }

        return null;
    }

    // Externalizable

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(packageNames);
        out.writeObject(expression);

        if (extractor instanceof ClassFieldReader) {
            out.writeObject(((ClassFieldReader) extractor).getAccessorKey());
        } else {
            out.writeObject(extractor);
        }

        out.writeObject(indexingDeclaration);
        out.writeObject(declarations);
        out.writeObject(constraintType);
        out.writeBoolean(isUnification);
        out.writeBoolean(isDynamic);
        out.writeObject(fieldValue);
        out.writeObject(compilationUnit);
        out.writeObject(evaluationContext);
        out.writeObject(operators);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        packageNames = (Set<String>) in.readObject();
        expression = (String) in.readObject();
        ((DroolsObjectInputStream) in).readExtractor(this::setReadAccessor);
        indexingDeclaration = (Declaration) in.readObject();
        declarations = (Declaration[]) in.readObject();
        constraintType = (IndexUtil.ConstraintType) in.readObject();
        isUnification = in.readBoolean();
        isDynamic = in.readBoolean();
        fieldValue = (FieldValue) in.readObject();
        compilationUnit = (MVELCompilationUnit) in.readObject();
        evaluationContext = (EvaluationContext) in.readObject();
        operators = (EvaluatorWrapper[]) in.readObject();
    }

    public boolean isTemporal() {
        return false;
    }

    @Override
    public MVELConstraint cloneIfInUse() {
        MVELConstraint clone = (MVELConstraint) super.cloneIfInUse();
        if (clone != this) {
            clone.conditionEvaluator = null;
        }
        return clone;
    }

    public MVELConstraint clone() {
        Declaration[] clonedDeclarations = new Declaration[declarations.length];
        System.arraycopy(declarations, 0, clonedDeclarations, 0, declarations.length);

        MVELConstraint clone = new MVELConstraint();
        clone.setType(getType());
        clone.packageNames = packageNames;
        clone.expression = expression;
        clone.fieldValue = fieldValue;
        clone.constraintType = constraintType;
        clone.declarations = clonedDeclarations;
        clone.operators = operators;
        if (indexingDeclaration != null) {
            clone.indexingDeclaration = indexingDeclaration.clone();
        }
        clone.extractor = extractor;
        clone.isUnification = isUnification;
        clone.isDynamic = isDynamic;
        clone.conditionEvaluator = conditionEvaluator;
        clone.compilationUnit = compilationUnit != null ? compilationUnit.clone() : null;
        return clone;
    }

    public int hashCode() {
        if (isAlphaHashable()) {
            return 29 * getLeftInExpression(IndexUtil.ConstraintType.EQUAL).hashCode() + 31 * fieldValue.hashCode();
        }
        return expression.hashCode();
    }

    private String getLeftInExpression(IndexUtil.ConstraintType constraint) {
        return expression.substring(0, codeAwareIndexOf(expression, constraint.getOperator())).trim();
    }

    private boolean isAlphaHashable() {
        return fieldValue != null && constraintType == IndexUtil.ConstraintType.EQUAL && getType() == ConstraintType.ALPHA;
    }

    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || object.getClass() != MVELConstraint.class) {
            return false;
        }
        MVELConstraint other = (MVELConstraint) object;
        if (isAlphaHashable()) {
            if (!other.isAlphaHashable() ||
                    !getLeftInExpression(IndexUtil.ConstraintType.EQUAL).equals(other.getLeftInExpression(IndexUtil.ConstraintType.EQUAL)) ||
                    !fieldValue.equals(other.fieldValue)) {
                return false;
            }
        } else {
            if (!equalsIgnoreSpaces(expression, other.expression)) {
                return false;
            }
        }
        if (declarations.length != other.declarations.length) {
            return false;
        }
        for (int i = 0; i < declarations.length; i++) {
            if (!declarations[i].getExtractor().equals(other.declarations[i].getExtractor())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object object, InternalKnowledgeBase kbase) {
        if (!equals(object)) {
            return false;
        }
        String thisPkg = packageNames.iterator().next();
        String otherPkg = (( MVELConstraint ) object).packageNames.iterator().next();
        if (thisPkg.equals( otherPkg )) {
            return true;
        }

        Map<String, Object> thisImports = (( MVELDialectRuntimeData ) kbase.getPackage( thisPkg ).getDialectRuntimeRegistry().getDialectData("mvel")).getImports();
        Map<String, Object> otherImports = (( MVELDialectRuntimeData ) kbase.getPackage( otherPkg ).getDialectRuntimeRegistry().getDialectData("mvel")).getImports();

                    if (fieldValue != null && constraintType.getOperator() != null) {
            return equalsExpressionTokensInBothImports(getLeftInExpression(constraintType), thisImports, otherImports);
                    } else {
                        return equalsExpressionTokensInBothImports(expression, thisImports, otherImports);
                    }
                }

    private boolean equalsExpressionTokensInBothImports(String expression, Map<String, Object> thisImports, Map<String, Object> otherImports) {
        for (String token : splitExpression(expression)) {
            if (!areNullSafeEquals(thisImports.get(token), otherImports.get(token))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Splits the expression in token (words) ignoring everything that is between quotes
     */
    private static List<String> splitExpression(String expression) {
        List<String> tokens = new ArrayList<String>();
        int lastStart = -1;
        boolean isQuoted = false;
        for (int i = 0; i < expression.length(); i++) {
            if ( lastStart == -1 ) {
                if ( !isQuoted && Character.isJavaIdentifierStart( expression.charAt(i) ) ) {
                    lastStart = i;
                }
            } else if ( !Character.isJavaIdentifierPart( expression.charAt(i) ) ) {
                tokens.add(expression.subSequence(lastStart, i).toString());
                lastStart = -1;
            }
            if (expression.charAt(i) == '"' || expression.charAt(i) == '\'') {
                if (i == 0 || expression.charAt(i-1) != '\\') {
                    isQuoted = !isQuoted;
                }
                if (isQuoted) {
                    lastStart = -1;
                }
            }
        }
        if (lastStart != -1) {
            tokens.add( expression.subSequence( lastStart, expression.length() ).toString() );
        }
        return tokens;
    }

    @Override
    public String toString() {
        return expression;
    }

    protected ParserConfiguration getParserConfiguration(InternalWorkingMemory workingMemory) {
        return getMVELDialectRuntimeData(workingMemory).getParserConfiguration();
    }

    protected MVELDialectRuntimeData getMVELDialectRuntimeData(InternalWorkingMemory workingMemory) {
        return getMVELDialectRuntimeData(workingMemory.getKnowledgeBase());
    }

    protected MVELDialectRuntimeData getMVELDialectRuntimeData(InternalKnowledgeBase kbase) {
        for (String packageName : packageNames) {
            InternalKnowledgePackage pkg = kbase.getPackage(packageName);
            if (pkg != null) {
                return ((MVELDialectRuntimeData) pkg.getDialectRuntimeRegistry().getDialectData("mvel"));
            }
        }
        return null;
    }

    // MvelArrayContextEntry

    public static class MvelContextEntry implements ContextEntry {

        protected ContextEntry next;
        protected Tuple tuple;
        protected InternalFactHandle rightHandle;
        protected Declaration[] declarations;

        protected transient InternalWorkingMemory workingMemory;

        public MvelContextEntry() { }

        public MvelContextEntry(Declaration[] declarations) {
            this.declarations = declarations;
        }

        public ContextEntry getNext() {
            return this.next;
        }

        public void setNext(final ContextEntry entry) {
            this.next = entry;
        }

        public void updateFromTuple(InternalWorkingMemory workingMemory, Tuple tuple) {
            this.tuple = tuple;
            this.workingMemory = workingMemory;
        }

        public void updateFromFactHandle(InternalWorkingMemory workingMemory, InternalFactHandle handle) {
            this.workingMemory = workingMemory;
            rightHandle = handle;
        }

        public void resetTuple() {
            tuple = null;
        }

        public void resetFactHandle() {
            workingMemory = null;
            rightHandle = null;
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(tuple);
            out.writeObject(rightHandle);
            out.writeObject(declarations);
            out.writeObject(next);
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            tuple = (Tuple) in.readObject();
            rightHandle = (InternalFactHandle) in.readObject();
            declarations = (Declaration[]) in.readObject();
            next = (ContextEntry) in.readObject();
        }

        public InternalFactHandle getRight() {
            return rightHandle;
        }

        public Declaration[] getDeclarations() {
            return declarations;
        }

        public InternalWorkingMemory getWorkingMemory() {
            return workingMemory;
        }
    }

    public static class UnificationContextEntry implements ContextEntry {
        private ContextEntry contextEntry;
        private Declaration declaration;
        private Variable variable;
        private ReadAccessor reader;

        public UnificationContextEntry() { }

        public UnificationContextEntry(ContextEntry contextEntry,
                                       Declaration declaration) {
            this.contextEntry = contextEntry;
            this.declaration = declaration;
            reader = this.declaration.getExtractor();
        }

        public ContextEntry getContextEntry() {
            return this.contextEntry;
        }

        public ReadAccessor getReader() {
            return reader;
        }

        public ContextEntry getNext() {
            return this.contextEntry.getNext();
        }

        public void resetFactHandle() {
            this.contextEntry.resetFactHandle();
        }

        public void resetTuple() {
            this.contextEntry.resetTuple();
            this.variable = null;
        }

        public void setNext(ContextEntry entry) {
            this.contextEntry.setNext( entry );
        }

        public void updateFromFactHandle(InternalWorkingMemory workingMemory,
                                         InternalFactHandle handle) {
            this.contextEntry.updateFromFactHandle(workingMemory, handle);
        }

        public void updateFromTuple(InternalWorkingMemory workingMemory,
                                    Tuple tuple) {
            DroolsQuery query = (DroolsQuery) tuple.getObject(0);
            this.variable = query.getVariables()[this.reader.getIndex()];
            if (this.variable == null) {
                // if there is no Variable, handle it as a normal constraint
                this.contextEntry.updateFromTuple(workingMemory, tuple);
            }
        }

        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            this.contextEntry = (ContextEntry) in.readObject();
            this.declaration = (Declaration) in.readObject();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(this.contextEntry);
            out.writeObject(this.declaration);
        }

        public Variable getVariable() {
            return this.variable;
        }

    }

    @Override
    public void registerEvaluationContext(BuildContext buildContext) {
        evaluationContext.addContext(buildContext);
    }

    public static class EvaluationContext implements Externalizable {

        private Collection<String> evaluatedRules = new HashSet<String>();

        public void addContext(BuildContext buildContext) {
            evaluatedRules.add(buildContext.getRule().toRuleNameAndPathString());
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(evaluatedRules);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            evaluatedRules = (Collection<String>) in.readObject();
        }

        @Override
        public String toString() {
            return evaluatedRules.toString();
        }
    }
}
