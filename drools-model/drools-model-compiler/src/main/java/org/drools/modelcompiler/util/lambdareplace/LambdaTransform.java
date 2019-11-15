package org.drools.modelcompiler.util.lambdareplace;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static org.drools.modelcompiler.util.StringUtil.md5Hash;

public class LambdaTransform {

    private final static String CLASS_NAME_PREFIX = "Lambda";
    private LambdaExpr lambdaExpr;
    private String className;

    private final String packageName;

    private List<LambdaParameter> lambdaParameters = new ArrayList<>();

    public LambdaTransform(String packageName) {
        this.packageName = packageName;
    }

    public CreatedClass createClass(String expressionString, Class<?>... argsType) {
        Expression expression = StaticJavaParser.parseExpression(expressionString);

        if (!expression.isLambdaExpr()) {
            throw new NotLambdaException();
        }

        lambdaExpr = expression.asLambdaExpr();
        className = CLASS_NAME_PREFIX + md5Hash(expressionString);

        parseParameters(argsType);

        CompilationUnit compilationUnit = new CompilationUnit(packageName);
        ClassOrInterfaceDeclaration classDeclaration = createClass(compilationUnit);

        createMethodDeclaration(classDeclaration);

        return new CreatedClass(compilationUnit, className);
    }

    private void parseParameters(Class<?>[] argsType) {
        NodeList<Parameter> parameters = lambdaExpr.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            Parameter p = parameters.get(i);
            Class<?> c = argsType[i];
            lambdaParameters.add(new LambdaParameter(p.getNameAsString(), c));
        }
    }

    private void createMethodDeclaration(ClassOrInterfaceDeclaration classDeclaration) {
        MethodDeclaration methodDeclaration = classDeclaration.addMethod("apply", Modifier.Keyword.PUBLIC);
        methodDeclaration.addAnnotation("Override");
        methodDeclaration.setType(parseClassOrInterfaceType("java.lang.Boolean"));

        setMethodParameter(methodDeclaration);

        ExpressionStmt clone = (ExpressionStmt) lambdaExpr.getBody().clone();
        methodDeclaration.setBody(new BlockStmt(NodeList.nodeList(new ReturnStmt(clone.getExpression()))));
    }

    private void setMethodParameter(MethodDeclaration methodDeclaration) {
        for (LambdaParameter parameter : lambdaParameters) {
            ClassOrInterfaceType type = parseClassOrInterfaceType(parameter.clazz.getCanonicalName());
            methodDeclaration.addParameter(new Parameter(type, parameter.name));
        }
    }

    private ClassOrInterfaceDeclaration createClass(CompilationUnit cu) {
        ClassOrInterfaceDeclaration expression = cu.addClass(className);
        return expression;
    }

    private static class NotLambdaException extends RuntimeException {

    }

    private static class LambdaParameter {

        String name;
        Class<?> clazz;

        LambdaParameter(String name, Class<?> clazz) {
            this.name = name;
            this.clazz = clazz;
        }
    }

    private static class NoFunctionForTypesException extends RuntimeException {

    }
}
