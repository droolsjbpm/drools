package org.drools.modelcompiler.builder.generator.visitor.accumulate;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.drools.compiler.compiler.Dialect;
import org.drools.compiler.compiler.DialectCompiletimeRegistry;
import org.drools.compiler.compiler.PackageRegistry;
import org.drools.compiler.lang.descr.AccumulateDescr;
import org.drools.compiler.lang.descr.PatternDescr;
import org.drools.compiler.lang.descr.RuleDescr;
import org.drools.compiler.rule.builder.PatternBuilder;
import org.drools.compiler.rule.builder.RuleBuildContext;
import org.drools.compiler.rule.builder.dialect.java.JavaAccumulateBuilder;
import org.drools.compiler.rule.builder.dialect.java.JavaRuleClassBuilder;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.rule.Pattern;
import org.drools.core.rule.RuleConditionElement;
import org.drools.javaparser.ast.CompilationUnit;
import org.drools.javaparser.ast.Node;
import org.drools.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.drools.modelcompiler.builder.GeneratedClassWithPackage;
import org.drools.modelcompiler.builder.PackageModel;
import org.drools.modelcompiler.builder.generator.RuleContext;

public class LegacyAccumulate {

    private final AccumulateDescr descr;
    private final PatternDescr basePattern;
    private final RuleBuildContext ruleBuildContext;
    private final JavaAccumulateBuilder javaAccumulateBuilder = new JavaAccumulateBuilder();
    private final RuleContext context;

    public LegacyAccumulate(RuleContext context, AccumulateDescr descr, PatternDescr basePattern) {
        this.descr = descr;
        this.basePattern = basePattern;
        this.context = context;

        final PackageModel packageModel = context.getPackageModel();

        final PackageRegistry pkgRegistry = packageModel.getPkgRegistry();
        final DialectCompiletimeRegistry dialectCompiletimeRegistry = pkgRegistry.getDialectCompiletimeRegistry();
        final Dialect defaultDialect = dialectCompiletimeRegistry.getDialect("java");
        final InternalKnowledgePackage pkg = packageModel.getPkg();
        final RuleDescr ruleDescr = context.getRuleDescr();

        ruleBuildContext = new RuleBuildContext(context.getKbuilder(), ruleDescr, dialectCompiletimeRegistry, pkg, defaultDialect);
    }

    public List<String> build() {

        final Pattern pattern = (Pattern) new PatternBuilder().build(ruleBuildContext, basePattern);
        javaAccumulateBuilder.build(ruleBuildContext, descr, pattern);

        final List<String> methods = ruleBuildContext.getMethods();

        final String s1 = new JavaRuleClassBuilder().buildRule(ruleBuildContext);

        final CompilationUnit cu = org.drools.javaparser.JavaParser.parse(s1);

        final Set<String> imports = ruleBuildContext.getPkg().getImports().keySet();

        GeneratedClassWithPackage generatedClassWithPackage = new GeneratedClassWithPackage(
                (ClassOrInterfaceDeclaration) cu.getType(0), ruleBuildContext.getPkg().getName(), imports
        );

        context.getPackageModel().addGeneratedAccumulateClasses(generatedClassWithPackage);

        // Let's assume the first one is the correct invokers (there are two)
        final String invoker = ruleBuildContext.getInvokers().values().iterator().next();
        final CompilationUnit cuInvoker = org.drools.javaparser.JavaParser.parse(invoker);

        Set<String> imports2 = new HashSet<>();
        imports2.addAll(imports);
        imports2.addAll(cuInvoker.getImports().stream().map(importDeclaration -> {
            return importDeclaration.getName().toString();
        }).collect(Collectors.toList()));

        GeneratedClassWithPackage invokerGenerated = new GeneratedClassWithPackage(
                (ClassOrInterfaceDeclaration) cuInvoker.getType(0), ruleBuildContext.getPkg().getName(), imports2
        );

        context.getPackageModel().addGeneratedAccumulateClasses(invokerGenerated);



        context.addAccumulateClasses(methods);

        return methods;
    }
}
