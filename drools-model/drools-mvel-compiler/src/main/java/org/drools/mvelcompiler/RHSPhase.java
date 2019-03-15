package org.drools.mvelcompiler;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Stack;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.drools.constraint.parser.ast.expr.DrlNameExpr;
import org.drools.constraint.parser.ast.visitor.DrlGenericVisitor;
import org.drools.mvelcompiler.ast.FieldAccessTExpr;
import org.drools.mvelcompiler.ast.FieldToAccessorTExpr;
import org.drools.mvelcompiler.ast.IntegerLiteralExpressionT;
import org.drools.mvelcompiler.ast.SimpleNameExpr;
import org.drools.mvelcompiler.ast.SimpleNameTExpr;
import org.drools.mvelcompiler.ast.StringLiteralExpressionT;
import org.drools.mvelcompiler.ast.TypedExpression;
import org.drools.mvelcompiler.context.Declaration;
import org.drools.mvelcompiler.context.MvelCompilerContext;
import org.drools.mvelcompiler.util.OptionalUtils;

import static org.drools.constraint.parser.printer.PrintUtil.printConstraint;
import static org.drools.core.util.ClassUtils.getAccessor;

public class RHSPhase implements DrlGenericVisitor<TypedExpression, RHSPhase.Context> {

    static class Context {

        Stack<TypedExpression> lastTypedExpression = new Stack<>();
    }

    private final MvelCompilerContext mvelCompilerContext;

    public RHSPhase(MvelCompilerContext mvelCompilerContext) {
        this.mvelCompilerContext = mvelCompilerContext;
    }

    public TypedExpression invoke(Statement statement) {
        Context ctx = new Context();

        TypedExpression typedExpression = statement.accept(this, ctx);
        if (typedExpression == null) {
            throw new MvelCompilerException("Type check of " + printConstraint(statement) + " failed.");
        }
        return typedExpression;
    }

    @Override
    public TypedExpression visit(FieldAccessExpr n, Context arg) {
        TypedExpression scope = n.getScope().accept(this, arg);
        TypedExpression name = n.getName().accept(this, arg);
        if(name instanceof FieldToAccessorTExpr) {
            return name;
        } else {
            return new FieldAccessTExpr(n, scope, name);
        }
    }

    @Override
    public TypedExpression visit(DrlNameExpr n, Context arg) {
        return n.getName().accept(this, arg);
    }

    @Override
    public TypedExpression visit(SimpleName n, Context arg) {
        if (arg.lastTypedExpression.isEmpty()) { // first node
            return simpleNameAsFirstNode(n, arg);
        } else {
            return simpleNameAsField(n, arg);
        }
    }

    private SimpleNameTExpr simpleNameAsFirstNode(SimpleName n, Context arg) {
        return tryParseAsDeclaration(n, arg)
                .orElse(new SimpleNameTExpr(n, null));
    }

    private TypedExpression simpleNameAsField(SimpleName n, Context arg) {
        return tryParseItAsPropertyAccessor(n, arg)
                .orElse(new SimpleNameExpr(n));
    }

    private Optional<SimpleNameTExpr> tryParseAsDeclaration(SimpleName n, Context arg) {
        Optional<Declaration> typeFromDeclarations = mvelCompilerContext.findDeclarations(n.asString());
        return typeFromDeclarations.map(d -> {
            Class<?> clazz = d.getClazz();
            SimpleNameTExpr simpleNameTExpr = new SimpleNameTExpr(n, clazz);
            arg.lastTypedExpression.push(simpleNameTExpr);
            return simpleNameTExpr;
        });
    }

    private Optional<TypedExpression> tryParseItAsPropertyAccessor(SimpleName n, Context arg) {
        Optional<TypedExpression> lastTypedExpression = Optional.of(arg.lastTypedExpression.peek());
        Optional<Type> scopeType = lastTypedExpression.flatMap(TypedExpression::getType);
        Optional<Method> optAccessor = scopeType.flatMap(t -> Optional.ofNullable(getAccessor((Class) t, n.asString())));

        return OptionalUtils.map2(lastTypedExpression, optAccessor, (lt, accessor) -> {
            FieldToAccessorTExpr fieldToAccessorTExpr = new FieldToAccessorTExpr(n, lt, accessor);
            arg.lastTypedExpression.push(fieldToAccessorTExpr);
            return fieldToAccessorTExpr;
        });
    }

    @Override
    public TypedExpression visit(MethodCallExpr n, Context arg) {
        TypedExpression last = null;
        for (Node children : n.getChildNodes()) {
            last = children.accept(this, arg);
        }

        return last;
    }

    @Override
    public TypedExpression visit(ExpressionStmt n, Context arg) {
        return n.getExpression().accept(this, arg);
    }

    @Override
    public TypedExpression visit(VariableDeclarationExpr n, Context arg) {
        return n.getVariables().iterator().next().accept(this, arg);
    }

    @Override
    public TypedExpression visit(VariableDeclarator n, Context arg) {
        Optional<TypedExpression> initExpression = n.getInitializer().map(i -> i.accept(this, arg));
        return initExpression.orElse(null);
    }

    @Override
    public TypedExpression visit(AssignExpr n, Context arg) {
        return n.getValue().accept(this, arg);
    }

    @Override
    public TypedExpression visit(StringLiteralExpr n, Context arg) {
        return new StringLiteralExpressionT(n);
    }

    @Override
    public TypedExpression visit(IntegerLiteralExpr n, Context arg) {
        return new IntegerLiteralExpressionT(n);
    }
}

