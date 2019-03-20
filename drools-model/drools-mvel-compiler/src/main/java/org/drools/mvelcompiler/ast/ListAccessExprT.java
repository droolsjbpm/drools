package org.drools.mvelcompiler.ast;

import java.lang.reflect.Type;
import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class ListAccessExprT extends TypedExpression {

    private final TypedExpression name;
    private final Expression index;
    private final Type type;

    public ListAccessExprT(ArrayAccessExpr n, TypedExpression name, Expression index, Type type) {
        super(n);
        this.name = name;
        this.index = index;
        this.type = type;
    }

    @Override
    public Optional<Type> getType() {
        return Optional.of(type);
    }

    @Override
    public Node toJavaExpression() {
        return new MethodCallExpr((Expression)name.toJavaExpression(), "get", NodeList.nodeList(index));
    }
}
