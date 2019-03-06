/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.drools.constraint.parser.ast.visitor;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.GenericVisitor;
import org.drools.constraint.parser.ast.expr.BigDecimalLiteralExpr;
import org.drools.constraint.parser.ast.expr.BigIntegerLiteralExpr;
import org.drools.constraint.parser.ast.expr.CommaSeparatedMethodCallExpr;
import org.drools.constraint.parser.ast.expr.DrlNameExpr;
import org.drools.constraint.parser.ast.expr.DrlxExpression;
import org.drools.constraint.parser.ast.expr.HalfBinaryExpr;
import org.drools.constraint.parser.ast.expr.HalfPointFreeExpr;
import org.drools.constraint.parser.ast.expr.InlineCastExpr;
import org.drools.constraint.parser.ast.expr.MapCreationLiteralExpression;
import org.drools.constraint.parser.ast.expr.MapCreationLiteralExpressionKeyValuePair;
import org.drools.constraint.parser.ast.expr.NullSafeFieldAccessExpr;
import org.drools.constraint.parser.ast.expr.NullSafeMethodCallExpr;
import org.drools.constraint.parser.ast.expr.OOPathChunk;
import org.drools.constraint.parser.ast.expr.OOPathExpr;
import org.drools.constraint.parser.ast.expr.PointFreeExpr;
import org.drools.constraint.parser.ast.expr.RuleBody;
import org.drools.constraint.parser.ast.expr.RuleConsequence;
import org.drools.constraint.parser.ast.expr.RuleDeclaration;
import org.drools.constraint.parser.ast.expr.RulePattern;
import org.drools.constraint.parser.ast.expr.TemporalLiteralChunkExpr;
import org.drools.constraint.parser.ast.expr.TemporalLiteralExpr;
import org.drools.constraint.parser.ast.expr.TemporalLiteralInfiniteChunkExpr;

public interface DrlGenericVisitor<R, A> extends GenericVisitor<R,A> {
    default R defaultMethod(Node n, A a) { return null; }

    default R visit(RuleDeclaration ruleDeclaration, A arg) { return defaultMethod(ruleDeclaration, arg); }

    default R visit(RuleBody ruleBody, A arg) { return null; }

    default R visit(RulePattern rulePattern, A arg) { return null; }

    default R visit(DrlxExpression expr, A arg) { return null; }

    default R visit(OOPathExpr expr, A arg) { return null; }

    default R visit(OOPathChunk chunk, A arg) { return null; }

    default R visit(RuleConsequence ruleConsequence, A arg) { return null; }

    default R visit(InlineCastExpr inlineCastExpr, A arg) { return null; }

    default R visit(NullSafeFieldAccessExpr nullSafeFieldAccessExpr, A arg) { return null; }

    default R visit(NullSafeMethodCallExpr nullSafeMethodCallExpr, A arg) { return null; }

    default R visit(PointFreeExpr pointFreeExpr, A arg) { return null; }

    default R visit(TemporalLiteralExpr temporalLiteralExpr, A arg) { return null; }

    default R visit(TemporalLiteralChunkExpr temporalLiteralChunkExpr, A arg) { return null; }

    default R visit(HalfBinaryExpr n, A arg) { return null; }

    default R visit(HalfPointFreeExpr n, A arg) { return null; }

    default R visit(BigDecimalLiteralExpr bigDecimalLiteralExpr, A arg) { return null; }

    default R visit(BigIntegerLiteralExpr bigIntegerLiteralExpr, A arg) { return null; }

    default R visit(TemporalLiteralInfiniteChunkExpr temporalLiteralInfiniteChunkExpr, A arg) { return null; }

    default R visit(CommaSeparatedMethodCallExpr commaSeparatedMethodCallExpr, A arg) { throw new UnsupportedOperationException(); }

    default R visit(DrlNameExpr drlNameExpr, A arg) { return null; };

    @Override
    default R visit(CompilationUnit n, A arg) {
        return null;
    }

    @Override
    default R visit(PackageDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(TypeParameter n, A arg) {
        return null;
    }

    @Override
    default R visit(LineComment n, A arg) {
        return null;
    }

    @Override
    default R visit(BlockComment n, A arg) {
        return null;
    }

    @Override
    default R visit(ClassOrInterfaceDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(EnumDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(EnumConstantDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(AnnotationDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(AnnotationMemberDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(FieldDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(VariableDeclarator n, A arg) {
        return null;
    }

    @Override
    default R visit(ConstructorDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(MethodDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(Parameter n, A arg) {
        return null;
    }

    @Override
    default R visit(InitializerDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(JavadocComment n, A arg) {
        return null;
    }

    @Override
    default R visit(ClassOrInterfaceType n, A arg) {
        return null;
    }

    @Override
    default R visit(PrimitiveType n, A arg) {
        return null;
    }

    @Override
    default R visit(ArrayType n, A arg) {
        return null;
    }

    @Override
    default R visit(ArrayCreationLevel n, A arg) {
        return null;
    }

    @Override
    default R visit(IntersectionType n, A arg) {
        return null;
    }

    @Override
    default R visit(UnionType n, A arg) {
        return null;
    }

    @Override
    default R visit(VoidType n, A arg) {
        return null;
    }

    @Override
    default R visit(WildcardType n, A arg) {
        return null;
    }

    @Override
    default R visit(UnknownType n, A arg) {
        return null;
    }

    @Override
    default R visit(ArrayAccessExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(ArrayCreationExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(ArrayInitializerExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(AssignExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(BinaryExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(CastExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(ClassExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(ConditionalExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(EnclosedExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(FieldAccessExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(InstanceOfExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(StringLiteralExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(IntegerLiteralExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(LongLiteralExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(CharLiteralExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(DoubleLiteralExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(BooleanLiteralExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(NullLiteralExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(MethodCallExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(NameExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(ObjectCreationExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(ThisExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(SuperExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(UnaryExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(VariableDeclarationExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(MarkerAnnotationExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(SingleMemberAnnotationExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(NormalAnnotationExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(MemberValuePair n, A arg) {
        return null;
    }

    @Override
    default R visit(ExplicitConstructorInvocationStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(LocalClassDeclarationStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(AssertStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(BlockStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(LabeledStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(EmptyStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(ExpressionStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(SwitchStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(SwitchEntryStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(BreakStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(ReturnStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(IfStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(WhileStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(ContinueStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(DoStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(ForEachStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(ForStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(ThrowStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(SynchronizedStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(TryStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(CatchClause n, A arg) {
        return null;
    }

    @Override
    default R visit(LambdaExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(MethodReferenceExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(TypeExpr n, A arg) {
        return null;
    }

    @Override
    default R visit(NodeList n, A arg) {
        return null;
    }

    @Override
    default R visit(Name n, A arg) {
        return null;
    }

    @Override
    default R visit(SimpleName n, A arg) {
        return null;
    }

    @Override
    default R visit(ImportDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(ModuleDeclaration n, A arg) {
        return null;
    }

    @Override
    default R visit(ModuleRequiresDirective n, A arg) {
        return null;
    }

    @Override
    default R visit(ModuleExportsDirective n, A arg) {
        return null;
    }

    @Override
    default R visit(ModuleProvidesDirective n, A arg) {
        return null;
    }

    @Override
    default R visit(ModuleUsesDirective n, A arg) {
        return null;
    }

    @Override
    default R visit(ModuleOpensDirective n, A arg) {
        return null;
    }

    @Override
    default R visit(UnparsableStmt n, A arg) {
        return null;
    }

    @Override
    default R visit(ReceiverParameter n, A arg) {
        return null;
    }

    @Override
    default R visit(VarType n, A arg) {
        return null;
    }

    @Override
    default R visit(Modifier n, A arg) {
        return null;
    }

    @Override
    default R visit(SwitchExpr n, A arg) {
        return null;
    }

    default R visit(MapCreationLiteralExpression n, A arg) { return null; };

    default R visit(MapCreationLiteralExpressionKeyValuePair n, A arg) { return null; };
}
