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

package org.drools.mvel.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.drools.compiler.compiler.AnalysisResult;
import org.drools.compiler.compiler.BoundIdentifiers;
import org.drools.compiler.compiler.DescrBuildError;
import org.drools.compiler.lang.descr.RuleDescr;
import org.drools.compiler.rule.builder.ConsequenceBuilder;
import org.drools.compiler.rule.builder.RuleBuildContext;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.RuleTerminalNode.SortDeclarations;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.TypeDeclaration;
import org.drools.core.spi.DeclarationScopeResolver;
import org.drools.core.spi.KnowledgeHelper;
import org.drools.core.util.bitmask.BitMask;
import org.drools.mvel.MVELDialectRuntimeData;
import org.drools.mvel.expr.MVELCompilationUnit;
import org.drools.mvel.expr.MVELConsequence;
import org.mvel2.Macro;
import org.mvel2.MacroProcessor;

import static org.drools.core.reteoo.PropertySpecificUtil.allSetButTraitBitMask;
import static org.drools.core.reteoo.PropertySpecificUtil.getEmptyPropertyReactiveMask;
import static org.drools.core.reteoo.PropertySpecificUtil.setPropertyOnMask;
import static org.drools.core.util.StringUtils.codeAwareSplitOnChar;
import static org.drools.core.util.StringUtils.findEndOfBlockIndex;
import static org.drools.core.util.StringUtils.findEndOfMethodArgsIndex;
import static org.drools.core.util.StringUtils.splitStatements;
import static org.drools.mvel.asm.AsmUtil.copyErrorLocation;

public class MVELConsequenceBuilder
    implements
    ConsequenceBuilder {

    public static final Map<String, Macro> macros = new HashMap<String,Macro>( 10 );
    static {
        macros.put( "insert",
                    new Macro() {
                        public String doMacro() {
                            return "drools.insert";
                        }
                    } );

        macros.put( "insertLogical",
                    new Macro() {
                        public String doMacro() {
                            return "drools.insertLogical";
                        }
                    } );

        macros.put( "bolster",
                    new Macro() {
                        public String doMacro() {
                            return "drools.bolster";
                        }
                    } );

        macros.put( "modify",
                    new Macro() {
                        public String doMacro() {
                            return "@Modify with";
                        }
                    } );

        macros.put( "update",
                    new Macro() {
                        public String doMacro() {
                            return "drools.update";
                        }
                    } );

        macros.put( "retract",
                    new Macro() {
                        public String doMacro() {
                            return "drools.retract";
                        }
                    } );
        macros.put( "entryPoints",
                    new Macro() {
                        public String doMacro() {
                            return "drools.entryPoints";
                        }
                    } );
        macros.put( "exitPoints",
                    new Macro() {
                        public String doMacro() {
                            return "drools.exitPoints";
                        }
                    } );

        macros.put( "don",
                    new Macro() {
                        public String doMacro() {
                            return "drools.don";
                        }
                    } );

        macros.put( "shed",
                    new Macro() {
                        public String doMacro() {
                            return "drools.shed";
                        }
                    } );
    }

    public MVELConsequenceBuilder() {

    }

    public void build(final RuleBuildContext context, String consequenceName) {

        // pushing consequence LHS into the stack for variable resolution
        context.getDeclarationResolver().pushOnBuildStack( context.getRule().getLhs() );

        try {
            MVELDialect dialect = (MVELDialect) context.getDialect( "mvel" );
            
            final RuleDescr ruleDescr = context.getRuleDescr();
            
            String text = ( RuleImpl.DEFAULT_CONSEQUENCE_NAME.equals(consequenceName) ) ?
                    (String) ruleDescr.getConsequence() :
                    (String) ruleDescr.getNamedConsequences().get( consequenceName );

            text = processMacros( text );
            text = rewriteModify( text );

            Map<String, Declaration> decls = context.getDeclarationResolver().getDeclarations(context.getRule());
            
            AnalysisResult analysis = dialect.analyzeBlock( context,
                                                            text,
                                                            new BoundIdentifiers( DeclarationScopeResolver.getDeclarationClasses(decls),
                                                                                  context,
                                                                                  Collections.EMPTY_MAP,
                                                                                  KnowledgeHelper.class),
                                                            null,
                                                            "drools",
                                                            KnowledgeHelper.class );
            
            if ( analysis == null ) {
                // something bad happened, issue already logged in errors
                return;
            }

            text = rewriteUpdates( context, analysis, text );

            final BoundIdentifiers usedIdentifiers = analysis.getBoundIdentifiers();
            
            final Declaration[] declarations =  new Declaration[usedIdentifiers.getDeclrClasses().size()];
            String[] declrStr = new String[declarations.length];
            int j = 0;
            for (String str : usedIdentifiers.getDeclrClasses().keySet() ) {
                declrStr[j] = str;
                declarations[j++] = decls.get( str );
            }
            Arrays.sort( declarations, SortDeclarations.instance  );
            for ( int i = 0; i < declrStr.length; i++) {
                declrStr[i] = declarations[i].getIdentifier();
            }
            context.getRule().setRequiredDeclarationsForConsequence(consequenceName, declrStr);
            MVELCompilationUnit unit = dialect.getMVELCompilationUnit( text,
                                                                       analysis,
                                                                       declarations,
                                                                       null,
                                                                       null,
                                                                       context,
                                                                       "drools",
                                                                       KnowledgeHelper.class,
                                                                       false,
                                                                       MVELCompilationUnit.Scope.CONSEQUENCE );

            MVELConsequence expr = new MVELConsequence( unit,
                                                        dialect.getId(),
                                                        consequenceName );
            
            if ( RuleImpl.DEFAULT_CONSEQUENCE_NAME.equals( consequenceName ) ) {
                context.getRule().setConsequence( expr );
            } else {
                context.getRule().addNamedConsequence(consequenceName, expr);
            }
            
            MVELDialectRuntimeData data = ( MVELDialectRuntimeData ) context.getPkg().getDialectRuntimeRegistry().getDialectData( "mvel" );
            data.addCompileable( context.getRule(),
                                 expr );
            
            expr.compile( data, context.getRule() );
        } catch ( final Exception e ) {
            copyErrorLocation(e, context.getRuleDescr());
            context.addError(new DescrBuildError(context.getParentDescr(),
                    context.getRuleDescr(),
                    null,
                    "Unable to build expression for 'consequence': " + e.getMessage() + " '" + context.getRuleDescr().getConsequence() + "'"));
        }
    }

    private static String rewriteModify( String text ) {
        int modifyPos = text.indexOf( "@Modify with" );
        if (modifyPos < 0) {
            return text;
        }

        int modifyArgStart = text.indexOf( '(', modifyPos );
        int modifyArgEnd = text.indexOf( ')', modifyPos );
        String modified = text.substring( modifyArgStart+1, modifyArgEnd ).trim();

        int modifyBlockStart = text.indexOf( '{', modifyArgEnd );
        int modifyBlockEnd = findEndOfBlockIndex(text, modifyBlockStart);
        String modifyBlock = text.substring( modifyBlockStart+1, modifyBlockEnd ).trim();

        StringBuilder sb = new StringBuilder();
        for (String statement : codeAwareSplitOnChar(modifyBlock, true, ';', ',', '\n')) {
            sb.append( modified ).append( "." ).append( statement.trim() ).append( ";\n" );
        }
        sb.append( "drools.update(" ).append( modified ).append( ");\n" );

        return rewriteModify( text.substring( 0, modifyPos ) + sb + text.substring( modifyBlockEnd+1 ) );
    }

    private static String rewriteUpdates( RuleBuildContext context, AnalysisResult analysis, String text ) {
        return rewriteUpdates( analysis.getBoundIdentifiers()::resolveType,
                c -> {
                    TypeDeclaration typeDeclaration = context.getKnowledgeBuilder().getTypeDeclaration(c);
                    if (typeDeclaration != null && typeDeclaration.isPropertyReactive()) {
                        typeDeclaration.setTypeClass(c);
                        return typeDeclaration.getAccessibleProperties();
                    }
                    return Collections.emptyList();
                },
                text );
    }

    public static String rewriteUpdates( Function<String, Class<?>> classResolver, Function<Class<?>, List<String>> propsResolver, String text ) {
        int start = 0;
        while (true) {
            int updatePos = text.indexOf( "drools.update(", start );
            if (updatePos < 0) {
                break;
            }
            start = updatePos + "drools.update(".length();
            int end = text.indexOf( ')', start );
            String identifier = text.substring( start, end ).trim();
            Class<?> updatedType = classResolver.apply( identifier );
            if (updatedType == null) {
                continue;
            }

            List<String> settableProperties = propsResolver.apply(updatedType);
            if (settableProperties.isEmpty()) {
                continue;
            }
            BitMask modificationMask = getEmptyPropertyReactiveMask(settableProperties.size());

            for (String expr : splitStatements(text)) {
                if (expr.startsWith( identifier + "." )) {
                    int fieldEnd = identifier.length()+1;
                    while (fieldEnd < expr.length() && Character.isJavaIdentifierPart( expr.charAt( fieldEnd ) )) fieldEnd++;
                    String propertyName = expr.substring( identifier.length()+1, fieldEnd );
                    boolean ignoreThisProp = false;
                    if (propertyName.length() > 3) {
                        if (propertyName.startsWith("set")) {
                            propertyName = Character.toLowerCase(propertyName.charAt(3)) + propertyName.substring(4);
                        } else if (propertyName.startsWith("get")) {
                            int parenthesisIndex = expr.indexOf('(');
                            int endMethodIndex;
                            if (parenthesisIndex != -1) {
                                endMethodIndex = findEndOfMethodArgsIndex(expr, parenthesisIndex);
                            } else {
                                endMethodIndex = fieldEnd;
                            }
                            if (expr.length() > endMethodIndex + 1 && expr.substring(endMethodIndex + 1).trim().startsWith(".")) {
                                propertyName = Character.toLowerCase(propertyName.charAt(3)) + propertyName.substring(4); // assume the property is modified
                            } else {
                                ignoreThisProp = true; // just a getter
                            }
                        }
                    }

                    int index = settableProperties.indexOf(propertyName);
                    if (index >= 0) {
                        modificationMask = setPropertyOnMask(modificationMask, index);
                    } else if (!ignoreThisProp) {
                        // I'm property reactive, but I was unable to infer which properties was modified, setting all bit in bitmask
                        modificationMask = allSetButTraitBitMask();
                        break;
                    }
                }
            }

            String updateArgs = ", " + modificationMask.getInstancingStatement() + ", " + updatedType.getCanonicalName() + ".class";
            text = text.substring( 0, end ) + updateArgs + text.substring( end );
            start = end + updateArgs.length();
        }

        return text;
    }

    public static String processMacros(String consequence) {
        MacroProcessor macroProcessor = new MacroProcessor();
        macroProcessor.setMacros( macros );
        return macroProcessor.parse( delimitExpressions( consequence ) );
    }

    /**
     * Allows newlines to demarcate expressions, as per MVEL command line.
     * If expression spans multiple lines (ie inside an unbalanced bracket) then
     * it is left alone.
     * Uses character based iteration which is at least an order of magnitude faster then a single
     * simple regex.
     */
    public static String delimitExpressions(String s) {

        StringBuilder result = new StringBuilder();
        char[] cs = s.toCharArray();
        int brace = 0;
        int sqre = 0;
        int crly = 0;
        int skippedNewLines = 0;
        boolean inString = false;
        char lastNonWhite = ';';
        for ( int i = 0; i < cs.length; i++ ) {
            char c = cs[i];
            switch ( c ) {
                case ' ' :
                case '\t' :
                    if (!inString && lookAhead(cs, i+1) == '.') {
                        continue;
                    }
                    break;
                case '\"' :
                    if ( i == 0 || cs[i-1] != '\\' ) {
                        inString = !inString;
                    }
                    break;
                case '/' :
                    if( i < cs.length-1 && cs[i+1] == '*' && !inString ) {
                        // multi-line comment
                        int start = i;
                        i+=2; // skip the /*
                        for( ; i < cs.length; i++ ) {
                            if( cs[i] == '*' && i < cs.length-1 && cs[i+1] == '/' ) {
                                i++; // skip the */
                                break;
                            } else if( cs[i] == '\n' || cs[i] == '\r' ) {
                                lastNonWhite = checkAndAddSemiColon( result,
                                                                     inString,
                                                                     brace,
                                                                     sqre,
                                                                     crly,
                                                                     lastNonWhite );
                            }
                        }
                        result.append( cs, start, i-start );
                        break;
                    } else if( i < cs.length-1 && cs[i+1] != '/' ) {
                        // not a line comment
                        break;
                    }
                    // otherwise handle it in the same way as #
                case '#' :
                    // line comment
                    lastNonWhite = checkAndAddSemiColon( result,
                                                         inString,
                                                         brace,
                                                         sqre,
                                                         crly,
                                                         lastNonWhite );
                    if (inString) {
                        result.append( c );
                    } else {
                        i = processLineComment( cs, i, result );
                    }
                    continue;
                case '(' :
                    brace++;
                    break;
                case '{' :
                    crly++;
                    break;
                case '[' :
                    sqre++;
                    break;
                case ')' :
                    brace--;
                    break;
                case '}' :
                    crly--;
                    break;
                case ']' :
                    sqre--;
                    break;
                default :
                    break;
            }

            if ( c == '\n' || c == '\r' ) {
                // line break
                if ( brace == 0 && sqre == 0 && crly == 0 &&
                     lastNonWhite != '.' && lookAhead(cs, i+1) != '.' ) {
                    if ( lastNonWhite != ';' ) {
                        result.append( ';' );
                        lastNonWhite = ';';
                    }
                    for (int j = 0; j < skippedNewLines; j++) {
                        result.append("\n");
                    }
                    skippedNewLines = 0;
                } else {
                    skippedNewLines++;
                    continue;
                }
            } else if ( !Character.isWhitespace( c ) ) {
                lastNonWhite = c;
            }
            result.append( c );
        }
        for (int i = 0; i < skippedNewLines; i++) {
            result.append("\n");
        }
        return result.toString();
    }

    private static char lookAhead(char[] cs, int pos) {
        for (int i = pos; i < cs.length; i++) {
            if ( !Character.isWhitespace( cs[i] ) ) {
                return cs[i];
            }
        }
        return ' ';
    }

    private static int processLineComment(char[] cs,
                                          int i,
                                          StringBuilder result) {
        for( ; i < cs.length; i++ ) {
            result.append( cs[i] );
            if( cs[i] == '\n' || cs[i] == '\r' ) {
                break;
            }
        }
        return i;
    }

    private static char checkAndAddSemiColon(StringBuilder result,
                                             boolean inString,
                                             int brace,
                                             int sqre,
                                             int crly,
                                             char lastNonWhite) {
        if ( !inString && brace == 0 && sqre == 0 && crly == 0 ){
            if ( lastNonWhite != ';' ) {
                result.append( ';' );
                lastNonWhite = ';';
            }
        }
        return lastNonWhite;
    }

}
