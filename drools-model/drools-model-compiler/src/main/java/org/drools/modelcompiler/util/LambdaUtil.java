package org.drools.modelcompiler.util;

import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.ast.NodeList.nodeList;

public class LambdaUtil {

    private static String AND_THEN_CALL = "andThen";

    private LambdaUtil() {

    }

    public static Expression compose(LambdaExpr l1, LambdaExpr l2, String typeA, String typeB) {
        Type type = new ClassOrInterfaceType(null, new SimpleName("Function1"),
                                             nodeList(parseClassOrInterfaceType(typeA), parseClassOrInterfaceType(typeB)));

        Expression castedExpr = new EnclosedExpr(new CastExpr(type, new EnclosedExpr(l1)));
        return new MethodCallExpr(castedExpr, AND_THEN_CALL, nodeList(l2));
    }
}
