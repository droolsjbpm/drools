/*
 * Copyright 2006 Red Hat, Inc. and/or its affiliates.
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

package org.drools.compiler.rule.builder;

import java.util.Optional;

import org.drools.compiler.builder.DroolsAssemblerContext;
import org.drools.compiler.compiler.Dialect;
import org.drools.compiler.compiler.DialectCompiletimeRegistry;
import org.drools.compiler.compiler.RuleBuildError;
import org.drools.compiler.lang.descr.QueryDescr;
import org.drools.compiler.lang.descr.RuleDescr;
import org.drools.core.beliefsystem.abductive.Abductive;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.rule.AbductiveQuery;
import org.drools.core.rule.EntryPointId;
import org.drools.core.rule.Pattern;
import org.drools.core.rule.QueryImpl;
import org.drools.core.spi.DeclarationScopeResolver;
import org.drools.core.util.ClassUtils;
import org.kie.internal.ruleunit.RuleUnitComponentFactory;
import org.kie.internal.ruleunit.RuleUnitDescription;
import org.drools.core.addon.TypeResolver;

/**
 * A context for the current build
 */
public class RuleBuildContext extends PackageBuildContext {

    // current rule
    private final RuleImpl rule;

    // current Rule descriptor
    private final RuleDescr ruleDescr;

    // available declarationResolver 
    private DeclarationScopeResolver declarationResolver;

    // a simple counter for patterns
    private int patternId = -1;

    private final DroolsCompilerComponentFactory compilerFactory;

    private boolean needStreamMode = false;

    private Pattern prefixPattern;

    private boolean inXpath;

    private int xpathChuckNr = 0;

    private int xpathOffsetadjustment = 0;

    /**
     * Default constructor
     */
    public RuleBuildContext(final DroolsAssemblerContext kBuilder,
                            final RuleDescr ruleDescr,
                            final DialectCompiletimeRegistry dialectCompiletimeRegistry,
                            final InternalKnowledgePackage pkg,
                            final Dialect defaultDialect) {
        this.ruleDescr = ruleDescr;

        if (ruleDescr instanceof QueryDescr) {
            Abductive abductive = ruleDescr.getTypedAnnotation(Abductive.class);
            if (abductive == null) {
                this.rule = new QueryImpl(ruleDescr.getName());
            } else {
                this.rule = new AbductiveQuery(ruleDescr.getName());
            }
        } else {
            this.rule = ruleDescr.toRule();
        }
        this.rule.setPackage(pkg.getName());
        this.rule.setDialect(ruleDescr.getDialect());
        this.rule.setLoadOrder(ruleDescr.getLoadOrder());

        init(kBuilder, pkg, ruleDescr, dialectCompiletimeRegistry, defaultDialect, this.rule);

        if (this.rule.getDialect() == null) {
            this.rule.setDialect(getDialect().getId());
        }

        if (ruleDescr.getUnit() != null) {
            rule.setRuleUnitClassName(pkg.getName() + "." + ruleDescr.getUnit().getTarget().replace('.', '$'));
        }

        Dialect dialect = getDialect();
        if (dialect != null) {
            dialect.init(ruleDescr);
        }

        this.compilerFactory = kBuilder.getBuilderConfiguration().getComponentFactory();
        this.declarationResolver = new DeclarationScopeResolver(kBuilder.getGlobals(), getPkg());
    }

    /**
     * Returns the current Rule being built
     */
    public RuleImpl getRule() {
        return this.rule;
    }

    /**
     * Returns the current RuleDescriptor
     */
    public RuleDescr getRuleDescr() {
        return this.ruleDescr;
    }

    /**
     * Returns the available declarationResolver instance
     */
    public DeclarationScopeResolver getDeclarationResolver() {
        return this.declarationResolver;
    }

    /**
     * Sets the available declarationResolver instance
     */
    public void setDeclarationResolver(final DeclarationScopeResolver declarationResolver) {
        this.declarationResolver = declarationResolver;
    }

    public int getNextPatternId() {
        return ++this.patternId;
    }

    public DroolsCompilerComponentFactory getCompilerFactory() {
        return compilerFactory;
    }

    public boolean needsStreamMode() {
        return needStreamMode;
    }

    public void setNeedStreamMode() {
        this.needStreamMode = true;
    }

    public void setPrefixPattern(Pattern prefixPattern) {
        this.prefixPattern = prefixPattern;
    }

    public Pattern getPrefixPattern() {
        return prefixPattern;
    }

    public boolean isInXpath() {
        return inXpath;
    }

    public void setInXpath(boolean inXpath) {
        this.inXpath = inXpath;
    }

    public void initRule() {
        initRuleUnitClassName();
        declarationResolver.setRule(rule);
    }

    @Override
    public Class<?> resolveVarType(String identifier) {
        return getDeclarationResolver().resolveVarType(identifier);
    }

    private void initRuleUnitClassName() {
        String ruleUnitClassName = rule.getRuleUnitClassName();
        boolean nameInferredFromResource = false;

        if (ruleUnitClassName == null && rule.getResource() != null && rule.getResource().getSourcePath() != null) {
            // We cannot depend on splitting based on File.separator, because e.g. MemoryFileSystem is "/" based
            // also on Windows => We need to parse the classname based on Java classname allowed characters.
            ruleUnitClassName = extractClassNameFromSourcePath();
            nameInferredFromResource = true;
        }

        if (RuleUnitComponentFactory.get() != null && ruleUnitClassName != null) {
            TypeResolver typeResolver = getPkg().getTypeResolver();
            boolean unitFound = false;
            Class<?> ruleUnitClass = ClassUtils.safeLoadClass(typeResolver.getClassLoader(), ruleUnitClassName);
            if (ruleUnitClass != null) {
                unitFound = RuleUnitComponentFactory.get().isRuleUnitClass( ruleUnitClass );
                if (unitFound && nameInferredFromResource) {
                    rule.setRuleUnitClassName(ruleUnitClassName);
                }
            }

            if (!unitFound && !nameInferredFromResource) {
                addError(new RuleBuildError(rule, getParentDescr(), null,
                                            ruleUnitClassName + " is not a valid RuleUnit class name"));
            }
        }
    }

    public Optional<EntryPointId> getEntryPointId(String name) {
        return getPkg().getRuleUnitDescriptionLoader().getDescription(getRule()).flatMap(ruDescr -> getEntryPointId(ruDescr, name));
    }

    public Optional<EntryPointId> getEntryPointId( RuleUnitDescription ruDescr, String name ) {
        return ruDescr.hasVar( name ) ? Optional.of( new EntryPointId( ruDescr.getEntryPointName(name) ) ) : Optional.empty();
    }

    private String extractClassNameFromSourcePath() {
        String drlPath = rule.getResource().getSourcePath();
        final int fileTypeDotIndex = drlPath.lastIndexOf('.');
        // If '.' is the first character, it may be a path like ./somepath/something/etc,
        // otherwise, if found somewhere, remove it with file type suffix
        if (fileTypeDotIndex > 0) {
            drlPath = drlPath.substring(0, fileTypeDotIndex);
        }

        StringBuilder classNameBuilder = new StringBuilder();
        int actualIndex = drlPath.length() - 1;
        char actualChar = drlPath.charAt(actualIndex);
        while (Character.isJavaIdentifierPart(actualChar) || Character.isJavaIdentifierStart(actualChar)) {
            classNameBuilder.append(actualChar);
            actualIndex--;
            if (actualIndex >= 0) {
                actualChar = drlPath.charAt(actualIndex);
            } else {
                break;
            }
        }

        return rule.getPackage() + "." + classNameBuilder.reverse().toString();
    }

    public void increaseXpathChuckNr() {
        xpathChuckNr++;
    }

    public void resetXpathChuckNr() {
        xpathChuckNr = 0;
    }

    public int getXpathChuckNr() {
        return xpathChuckNr + xpathOffsetadjustment;
    }

    public void setXpathChuckNr(int chunkNbr) {
        this.xpathChuckNr = chunkNbr + xpathOffsetadjustment;
    }

    public void setXpathOffsetadjustment(int xpathOffsetadjustment) {
        this.xpathOffsetadjustment = xpathOffsetadjustment;
    }

    public int getXpathOffsetadjustment() {
        return xpathOffsetadjustment;
    }
}
