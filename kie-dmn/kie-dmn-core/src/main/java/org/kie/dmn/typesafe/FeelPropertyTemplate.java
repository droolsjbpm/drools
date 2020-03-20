package org.kie.dmn.typesafe;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import org.drools.modelcompiler.builder.generator.declaredtype.api.FieldDefinition;
import org.drools.modelcompiler.builder.generator.declaredtype.api.MethodDefinition;
import org.drools.modelcompiler.builder.generator.declaredtype.api.MethodWithStringBody;
import org.kie.dmn.feel.util.EvalHelper;

import static com.github.javaparser.ast.NodeList.nodeList;
import static org.drools.core.util.StringUtils.ucFirst;

public class FeelPropertyTemplate {

    CompilationUnit methodTemplate;

    List<FieldDefinition> fields = new ArrayList<>();

    public FeelPropertyTemplate(List<FieldDefinition> fields) {
        this.fields = fields;
    }

    public List<MethodDefinition> getMethods() {
        List<MethodDefinition> allMethods = new ArrayList<>();

        methodTemplate = getMethodTemplate();

        allMethods.add(getFeelPropertyDefinition());
        allMethods.add(setFeelPropertyDefinition());
        allMethods.add(setAllDefinition());
        allMethods.add(allFeelProperties());

        return allMethods;
    }

    private MethodDefinition getFeelPropertyDefinition() {

        MethodDeclaration getFEELProperty = methodTemplate.findFirst(MethodDeclaration.class, mc -> mc.getNameAsString().equals("getFEELProperty"))
                .orElseThrow(RuntimeException::new)
                .clone();

        SwitchStmt firstSwitch = getFEELProperty.findFirst(SwitchStmt.class).orElseThrow(RuntimeException::new);

        firstSwitch.setComment(null);

        List<SwitchEntry> collect = fields.stream().map(this::toGetPropertySwitchEntry).collect(Collectors.toList());

        SwitchEntry defaultSwitchStmt = firstSwitch.findFirst(SwitchEntry.class, sw -> sw.getLabels().isEmpty()).orElseThrow(RuntimeException::new); // default
        collect.add(defaultSwitchStmt);

        firstSwitch.setEntries(nodeList(collect));

        String body = getFEELProperty.getBody().orElseThrow(RuntimeException::new).toString();
        MethodWithStringBody getFeelProperty = new MethodWithStringBody("getFEELProperty", EvalHelper.PropertyValueResult.class.getCanonicalName(), body);
        getFeelProperty.addParameter(String.class.getCanonicalName(), "property");

        return getFeelProperty;
    }


    private SwitchEntry toGetPropertySwitchEntry(FieldDefinition fieldDefinition) {
        ReturnStmt returnStmt = new ReturnStmt();
        MethodCallExpr mc = StaticJavaParser.parseExpression(EvalHelper.PropertyValueResult.class.getCanonicalName() + ".ofValue()");
        String accessorName = getAccessorName(fieldDefinition, "get");
        mc.addArgument(new MethodCallExpr(new ThisExpr(), accessorName));
        returnStmt.setExpression(mc);
        return new SwitchEntry(nodeList(new StringLiteralExpr(fieldDefinition.getFieldName())), SwitchEntry.Type.STATEMENT_GROUP, nodeList(returnStmt));
    }

    private MethodDefinition setFeelPropertyDefinition() {

        MethodDeclaration setFEELProperty = methodTemplate.findFirst(MethodDeclaration.class, mc -> mc.getNameAsString().equals("setFEELProperty"))
                .orElseThrow(RuntimeException::new)
                .clone();

        SwitchStmt firstSwitch = setFEELProperty.findFirst(SwitchStmt.class).orElseThrow(RuntimeException::new);

        firstSwitch.setComment(null);

        List<SwitchEntry> collect = fields.stream().map(this::toSetPropertySwitchEntry).collect(Collectors.toList());

        firstSwitch.setEntries(nodeList(collect));

        String body = setFEELProperty.getBody().orElseThrow(RuntimeException::new).toString();
        MethodWithStringBody getFeelProperty = new MethodWithStringBody("setFEELProperty", "void", body);
        getFeelProperty.addParameter(String.class.getCanonicalName(), "property");
        getFeelProperty.addParameter(Object.class.getCanonicalName(), "value");

        return getFeelProperty;
    }

    private SwitchEntry toSetPropertySwitchEntry(FieldDefinition fieldDefinition) {
        ExpressionStmt expressionStmt = new ExpressionStmt();
        String accessorName = getAccessorName(fieldDefinition, "set");
        MethodCallExpr mc = new MethodCallExpr(new ThisExpr(), accessorName);
        mc.addArgument(new CastExpr(StaticJavaParser.parseType(fieldDefinition.getObjectType()), new NameExpr("value")));
        expressionStmt.setExpression(mc);
        return new SwitchEntry(nodeList(new StringLiteralExpr(fieldDefinition.getFieldName())), SwitchEntry.Type.STATEMENT_GROUP, nodeList(expressionStmt));
    }

    private MethodDefinition setAllDefinition() {

        String body = " {  } ";
        MethodWithStringBody setFeelProperty = new MethodWithStringBody("setAll", "void", body);
        setFeelProperty.addParameter("java.util.Map<String, Object>", "values");

        return setFeelProperty;
    }

    private CompilationUnit getMethodTemplate() {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/org/kie/dmn/core/impl/DMNTypeSafeTypeTemplate.java");
        CompilationUnit parse = StaticJavaParser.parse(resourceAsStream);
        return parse;
    }

    private MethodWithStringBody allFeelProperties() {

        MethodDeclaration allFeelProperties = methodTemplate.findFirst(MethodDeclaration.class, mc -> mc.getNameAsString().equals("allFEELProperties"))
                .orElseThrow(RuntimeException::new)
                .clone();

        List<Statement> collect = fields.stream().map(this::toGetAllProperty).collect(Collectors.toList());
        BlockStmt newBlockStatement = new BlockStmt(nodeList(collect));

        allFeelProperties.findAll(ExpressionStmt.class,
                                  mc -> mc.getExpression().isMethodCallExpr() &&
                                          mc.getExpression().asMethodCallExpr().getNameAsString().equals("put"))
                .forEach(n -> n.replace(newBlockStatement));

        String body = allFeelProperties.getBody().orElseThrow(RuntimeException::new).toString();

        MethodWithStringBody allFEELProperties = new MethodWithStringBody(
                "allFEELProperties",
                "java.util.Map<String, Object>",
                body
        );

        return allFEELProperties;
    }

    private ExpressionStmt toGetAllProperty(FieldDefinition fieldDefinition) {
        String accessorName = getAccessorName(fieldDefinition, "get");

        MethodCallExpr mc = new MethodCallExpr(new NameExpr("result"), "put");

        mc.addArgument(new StringLiteralExpr(fieldDefinition.getFieldName()));
        mc.addArgument(new MethodCallExpr(new ThisExpr(), accessorName));

        return new ExpressionStmt(mc);
    }

    private String getAccessorName(FieldDefinition fieldDefinition, String get) {
        return get + ucFirst(fieldDefinition.getFieldName());
    }
}
