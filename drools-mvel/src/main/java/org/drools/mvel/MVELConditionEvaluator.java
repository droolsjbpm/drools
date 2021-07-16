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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.drools.core.base.EvaluatorWrapper;
import org.drools.mvel.expr.MVELCompilationUnit;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.constraint.ConditionEvaluator;
import org.drools.core.spi.Tuple;
import org.drools.mvel.expr.MvelEvaluator;
import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.ast.ASTNode;
import org.mvel2.ast.And;
import org.mvel2.ast.BinaryOperation;
import org.mvel2.ast.BooleanNode;
import org.mvel2.ast.Contains;
import org.mvel2.ast.LineLabel;
import org.mvel2.ast.Negation;
import org.mvel2.ast.Or;
import org.mvel2.ast.Substatement;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExecutableAccessor;
import org.mvel2.compiler.ExecutableLiteral;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ASTLinkedList;

import static org.drools.core.rule.constraint.EvaluatorHelper.valuesAsMap;
import static org.drools.mvel.expr.MvelEvaluator.EvaluatorType.SYNCHRONIZED_TILL_EVALUATED;
import static org.drools.mvel.expr.MvelEvaluator.createMvelEvaluator;

public class MVELConditionEvaluator implements ConditionEvaluator {

    private final Declaration[] declarations;
    private final EvaluatorWrapper[] operators;

    private final String conditionClass;
    private final ParserConfiguration parserConfiguration;
    private final ExecutableStatement executableStatement;
    private final MVELCompilationUnit compilationUnit;

    private final MvelEvaluator<Boolean> evaluator;

    private boolean evaluated = false;

    public MVELConditionEvaluator( ParserConfiguration configuration,
                                   String expression,
                                   Declaration[] declarations,
                                   EvaluatorWrapper[] operators,
                                   String conditionClass) {
        this(null,
             configuration,
             (ExecutableStatement)MVEL.compileExpression(expression, new ParserContext(configuration)),
             declarations,
             operators,
             conditionClass);
    }

    public MVELConditionEvaluator( MVELCompilationUnit compilationUnit,
                                   ParserConfiguration parserConfiguration,
                                   ExecutableStatement executableStatement,
                                   Declaration[] declarations,
                                   EvaluatorWrapper[] operators,
                                   String conditionClass) {
        this.declarations = declarations;
        this.operators = operators;
        this.conditionClass = conditionClass;
        this.compilationUnit = compilationUnit;
        this.parserConfiguration = parserConfiguration;
        this.executableStatement = executableStatement;
        this.evaluator = createMvelEvaluator(SYNCHRONIZED_TILL_EVALUATED, executableStatement );
    }

    public boolean evaluate(InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple) {
        return evaluate(evaluator, handle, workingMemory, tuple);
    }

    private boolean evaluate(MvelEvaluator<Boolean> evaluator, InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple) {
        if (compilationUnit == null) {
            Map<String, Object> vars = valuesAsMap(handle.getObject(), workingMemory, tuple, declarations);
            if (operators.length > 0) {
                if (vars == null) {
                    vars = new HashMap<String, Object>();
                }
                InternalFactHandle[] handles = tuple != null ? tuple.toFactHandles() : new InternalFactHandle[0];
                for (EvaluatorWrapper operator : operators) {
                    vars.put( operator.getBindingName(), operator );
                    operator.loadHandles(handles, handle);
                }
            }
            return evaluate(evaluator, handle.getObject(), vars);
        }

        VariableResolverFactory factory = compilationUnit.createFactory();
        compilationUnit.updateFactory( handle, tuple, null, workingMemory,
                                       workingMemory.getGlobalResolver(),
                                       factory );

        return evaluator.evaluate( handle.getObject(), factory );
    }

    private boolean evaluate(MvelEvaluator<Boolean> evaluator, Object object, Map<String, Object> vars) {
        return vars == null ?
                evaluator.evaluate( object ) :
                evaluator.evaluate( object, vars );
    }

    ConditionAnalyzer.Condition getAnalyzedCondition( InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple leftTuple) {
        ensureCompleteEvaluation(handle, workingMemory, leftTuple);
        return new ConditionAnalyzer(executableStatement, declarations, operators, conditionClass).analyzeCondition();
    }

    private void ensureCompleteEvaluation(InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple) {
        if (!evaluated) {
            ASTNode rootNode = getRootNode(executableStatement);
            if (rootNode != null) {
                ensureCompleteEvaluation(rootNode, handle, workingMemory, tuple);
            }
            evaluated = true;
        }
    }

    private void ensureCompleteEvaluation(ASTNode node, InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple) {
        node = unwrap(node);
        if (!(node instanceof And || node instanceof Or)) {
            evaluateIfNecessary(handle, workingMemory, tuple, node);
            return;
        }
        ensureBranchEvaluation(handle, workingMemory, tuple, ((BooleanNode)node).getLeft());
        ensureBranchEvaluation(handle, workingMemory, tuple, ((BooleanNode)node).getRight());
    }

    private void ensureBranchEvaluation(InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple, ASTNode node) {
        evaluateIfNecessary( handle, workingMemory, tuple, node );
        ensureCompleteEvaluation(node, handle, workingMemory, tuple);
    }

    private void evaluateIfNecessary( InternalFactHandle handle, InternalWorkingMemory workingMemory, Tuple tuple, ASTNode node ) {
        if (!isEvaluated(node)) {
            ASTNode next = node.nextASTNode;
            node.nextASTNode = null;
            evaluate( createMvelEvaluator(SYNCHRONIZED_TILL_EVALUATED, asCompiledExpression(node) ), handle, workingMemory, tuple);
            node.nextASTNode = next;
        }
    }

    private CompiledExpression asCompiledExpression(ASTNode node) {
        return new CompiledExpression(new ASTLinkedList(node), null, Object.class, parserConfiguration, false);
    }

    public static boolean isFullyEvaluated(Serializable executableStatement) {
        return isEvaluated( getRootNode( executableStatement ) );
    }

    private static boolean isEvaluated(ASTNode node) {
        node = unwrapSubstatement(node);
        if (node instanceof Contains) {
            return ((Contains)node).getFirstStatement().getAccessor() != null;
        }
        return node instanceof BinaryOperation ? ((BooleanNode) node).getLeft().getAccessor() != null : node.getAccessor() != null;
    }

    private static ASTNode getRootNode(Serializable executableStatement) {
        if (executableStatement instanceof ExecutableLiteral) {
            return null;
        }
        return executableStatement instanceof CompiledExpression ? ((CompiledExpression) executableStatement).getFirstNode() : ((ExecutableAccessor) executableStatement).getNode();
    }

    private static ASTNode unwrap(ASTNode node) {
        while (node instanceof Negation || node instanceof LineLabel || node instanceof Substatement) {
            node = unwrapNegation(node);
            node = unwrapSubstatement(node);
        }
        return node;
    }

    private static ASTNode unwrapNegation(ASTNode node) {
        if (node instanceof Negation) {
            ExecutableStatement statement = ((Negation)node).getStatement();
            return statement instanceof ExecutableAccessor ? ((ExecutableAccessor)statement).getNode() : null;
        }
        return node;
    }

    private static ASTNode unwrapSubstatement(ASTNode node) {
        if (node instanceof LineLabel) {
            return node.nextASTNode;
        }
        return node instanceof Substatement ? ((ExecutableAccessor)((Substatement)node).getStatement()).getNode() : node;
    }
}
