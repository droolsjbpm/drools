/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.models.commons.backend.rule;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.drools.compiler.compiler.DrlParser;
import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.lang.descr.AccumulateDescr;
import org.drools.compiler.lang.descr.AndDescr;
import org.drools.compiler.lang.descr.AnnotationDescr;
import org.drools.compiler.lang.descr.AttributeDescr;
import org.drools.compiler.lang.descr.BaseDescr;
import org.drools.compiler.lang.descr.BehaviorDescr;
import org.drools.compiler.lang.descr.CollectDescr;
import org.drools.compiler.lang.descr.ConditionalElementDescr;
import org.drools.compiler.lang.descr.EntryPointDescr;
import org.drools.compiler.lang.descr.EvalDescr;
import org.drools.compiler.lang.descr.ExistsDescr;
import org.drools.compiler.lang.descr.ExprConstraintDescr;
import org.drools.compiler.lang.descr.FromDescr;
import org.drools.compiler.lang.descr.GlobalDescr;
import org.drools.compiler.lang.descr.NotDescr;
import org.drools.compiler.lang.descr.OrDescr;
import org.drools.compiler.lang.descr.PackageDescr;
import org.drools.compiler.lang.descr.PatternDescr;
import org.drools.compiler.lang.descr.PatternSourceDescr;
import org.drools.compiler.lang.descr.RuleDescr;
import org.drools.core.base.evaluators.EvaluatorRegistry;
import org.drools.core.base.evaluators.Operator;
import org.drools.core.util.DateUtils;
import org.drools.core.util.ReflectiveVisitor;
import org.drools.core.util.StringUtils;
import org.drools.workbench.models.commons.backend.rule.context.LHSGeneratorContext;
import org.drools.workbench.models.commons.backend.rule.context.LHSGeneratorContextFactory;
import org.drools.workbench.models.commons.backend.rule.context.RHSGeneratorContext;
import org.drools.workbench.models.commons.backend.rule.context.RHSGeneratorContextFactory;
import org.drools.workbench.models.commons.backend.rule.exception.RuleModelDRLPersistenceException;
import org.drools.workbench.models.datamodel.rule.ActionCallMethod;
import org.drools.workbench.models.datamodel.rule.ActionExecuteWorkItem;
import org.drools.workbench.models.datamodel.rule.ActionFieldFunction;
import org.drools.workbench.models.datamodel.rule.ActionFieldList;
import org.drools.workbench.models.datamodel.rule.ActionFieldValue;
import org.drools.workbench.models.datamodel.rule.ActionGlobalCollectionAdd;
import org.drools.workbench.models.datamodel.rule.ActionInsertFact;
import org.drools.workbench.models.datamodel.rule.ActionInsertLogicalFact;
import org.drools.workbench.models.datamodel.rule.ActionRetractFact;
import org.drools.workbench.models.datamodel.rule.ActionSetField;
import org.drools.workbench.models.datamodel.rule.ActionUpdateField;
import org.drools.workbench.models.datamodel.rule.ActionWorkItemFieldValue;
import org.drools.workbench.models.datamodel.rule.BaseSingleFieldConstraint;
import org.drools.workbench.models.datamodel.rule.CEPWindow;
import org.drools.workbench.models.datamodel.rule.CompositeFactPattern;
import org.drools.workbench.models.datamodel.rule.CompositeFieldConstraint;
import org.drools.workbench.models.datamodel.rule.ConnectiveConstraint;
import org.drools.workbench.models.datamodel.rule.DSLSentence;
import org.drools.workbench.models.datamodel.rule.ExpressionCollection;
import org.drools.workbench.models.datamodel.rule.ExpressionField;
import org.drools.workbench.models.datamodel.rule.ExpressionFormLine;
import org.drools.workbench.models.datamodel.rule.ExpressionMethod;
import org.drools.workbench.models.datamodel.rule.ExpressionMethodParameter;
import org.drools.workbench.models.datamodel.rule.ExpressionPart;
import org.drools.workbench.models.datamodel.rule.ExpressionText;
import org.drools.workbench.models.datamodel.rule.ExpressionUnboundFact;
import org.drools.workbench.models.datamodel.rule.ExpressionVariable;
import org.drools.workbench.models.datamodel.rule.FactPattern;
import org.drools.workbench.models.datamodel.rule.FieldConstraint;
import org.drools.workbench.models.datamodel.rule.FieldNature;
import org.drools.workbench.models.datamodel.rule.FieldNatureType;
import org.drools.workbench.models.datamodel.rule.FreeFormLine;
import org.drools.workbench.models.datamodel.rule.FromAccumulateCompositeFactPattern;
import org.drools.workbench.models.datamodel.rule.FromCollectCompositeFactPattern;
import org.drools.workbench.models.datamodel.rule.FromCompositeFactPattern;
import org.drools.workbench.models.datamodel.rule.FromEntryPointFactPattern;
import org.drools.workbench.models.datamodel.rule.IAction;
import org.drools.workbench.models.datamodel.rule.IFactPattern;
import org.drools.workbench.models.datamodel.rule.IPattern;
import org.drools.workbench.models.datamodel.rule.PluggableIAction;
import org.drools.workbench.models.datamodel.rule.RuleAttribute;
import org.drools.workbench.models.datamodel.rule.RuleMetadata;
import org.drools.workbench.models.datamodel.rule.RuleModel;
import org.drools.workbench.models.datamodel.rule.SingleFieldConstraint;
import org.drools.workbench.models.datamodel.rule.SingleFieldConstraintEBLeftSide;
import org.drools.workbench.models.datamodel.rule.builder.DRLConstraintValueBuilder;
import org.drools.workbench.models.datamodel.rule.visitors.ToStringExpressionVisitor;
import org.drools.workbench.models.datamodel.workitems.HasBinding;
import org.drools.workbench.models.datamodel.workitems.PortableBooleanParameterDefinition;
import org.drools.workbench.models.datamodel.workitems.PortableFloatParameterDefinition;
import org.drools.workbench.models.datamodel.workitems.PortableIntegerParameterDefinition;
import org.drools.workbench.models.datamodel.workitems.PortableObjectParameterDefinition;
import org.drools.workbench.models.datamodel.workitems.PortableParameterDefinition;
import org.drools.workbench.models.datamodel.workitems.PortableStringParameterDefinition;
import org.drools.workbench.models.datamodel.workitems.PortableWorkDefinition;
import org.kie.soup.commons.util.ListSplitter;
import org.kie.soup.commons.validation.PortablePreconditions;
import org.kie.soup.project.datamodel.commons.imports.ImportsParser;
import org.kie.soup.project.datamodel.commons.imports.ImportsWriter;
import org.kie.soup.project.datamodel.commons.packages.PackageNameParser;
import org.kie.soup.project.datamodel.commons.packages.PackageNameWriter;
import org.kie.soup.project.datamodel.imports.Import;
import org.kie.soup.project.datamodel.imports.Imports;
import org.kie.soup.project.datamodel.oracle.DataType;
import org.kie.soup.project.datamodel.oracle.MethodInfo;
import org.kie.soup.project.datamodel.oracle.ModelField;
import org.kie.soup.project.datamodel.oracle.OperatorsOracle;
import org.kie.soup.project.datamodel.oracle.PackageDataModelOracle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.core.util.StringUtils.splitArgumentsList;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.adjustParam;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.findField;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.findFields;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.findMethodInfo;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.getMethodInfosForType;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.getSimpleFactType;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.inferDataTypeFromAction;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.inferFieldNature;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.parseExpressionParameters;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.removeNumericSuffix;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.unwrapParenthesis;
import static org.drools.workbench.models.commons.backend.rule.RuleModelPersistenceHelper.unwrapTemplateKey;

/**
 * This class persists the rule model to DRL and back
 */
public class RuleModelDRLPersistenceImpl
        implements
        RuleModelPersistence {

    private static final String WORKITEM_PREFIX = "wi";

    private static final RuleModelPersistence INSTANCE = new RuleModelDRLPersistenceImpl();

    private static final Logger log = LoggerFactory.getLogger(RuleModelDRLPersistenceImpl.class);

    //This is the default dialect for rules not specifying one explicitly
    protected DRLConstraintValueBuilder constraintValueBuilder = DRLConstraintValueBuilder.getBuilder(DRLConstraintValueBuilder.DEFAULT_DIALECT);

    //Keep a record of all variable bindings for Actions that depend on them
    protected Map<String, IFactPattern> bindingsPatterns;
    protected Map<String, FieldConstraint> bindingsFields;

    protected RuleModelDRLPersistenceImpl() {
        // register custom evaluators
        new EvaluatorRegistry(getClass().getClassLoader());
    }

    public static RuleModelPersistence getInstance() {
        return INSTANCE;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.drools.ide.common.server.util.RuleModelPersistence#marshal(org.drools.guvnor
     * .client.modeldriven.brl.RuleModel)
     */
    @Override
    public String marshal(final RuleModel model) {
        return marshalRule(model);
    }

    protected String marshalRule(final RuleModel model) {
        boolean isDSLEnhanced = model.hasDSLSentences();
        bindingsPatterns = new HashMap<String, IFactPattern>();
        bindingsFields = new HashMap<String, FieldConstraint>();

        fixActionInsertFactBindings(model.rhs);

        StringBuilder buf = new StringBuilder();

        //Build rule
        this.marshalPackageHeader(model,
                                  buf);
        this.marshalRuleHeader(model,
                               buf);
        this.marshalMetadata(buf,
                             model);
        this.marshalAttributes(buf,
                               model);

        buf.append("\twhen\n");
        this.marshalLHS(buf,
                        model,
                        isDSLEnhanced,
                        new LHSGeneratorContextFactory());
        buf.append("\tthen\n");
        this.marshalRHS(buf,
                        model,
                        isDSLEnhanced,
                        new RHSGeneratorContextFactory());
        this.marshalFooter(buf);
        return buf.toString();
    }

    protected void fixActionInsertFactBindings(final IAction[] rhs) {
        final Set<String> existingBindings = extractExistingActionBindings(rhs);
        for (IAction action : rhs) {
            if (action instanceof ActionInsertFact) {
                final ActionInsertFact aif = (ActionInsertFact) action;
                if (aif.getFieldValues().length > 0 && aif.getBoundName() == null) {
                    int idx = 0;
                    String binding = "fact" + idx;
                    while (existingBindings.contains(binding)) {
                        idx++;
                        binding = "fact" + idx;
                    }
                    existingBindings.add(binding);
                    aif.setBoundName(binding);
                }
            }
        }
    }

    private Set<String> extractExistingActionBindings(final IAction[] rhs) {
        final Set<String> bindings = new HashSet<String>();
        for (IAction action : rhs) {
            if (action instanceof ActionInsertFact) {
                final ActionInsertFact aif = (ActionInsertFact) action;
                if (aif.getBoundName() != null) {
                    bindings.add(aif.getBoundName());
                }
            }
        }
        return bindings;
    }

    protected void marshalFooter(final StringBuilder buf) {
        buf.append("end\n");
    }

    //Append package name and imports to DRL
    protected void marshalPackageHeader(final RuleModel model,
                                        final StringBuilder buf) {
        PackageNameWriter.write(buf,
                                model);
        ImportsWriter.write(buf,
                            model);
    }

    //Append rule header
    protected void marshalRuleHeader(final RuleModel model,
                                     final StringBuilder buf) {
        buf.append("rule \"" + marshalRuleName(model) + "\"");
        if (null != model.parentName && model.parentName.length() > 0) {
            buf.append(" extends \"" + model.parentName + "\"\n");
        } else {
            buf.append('\n');
        }
    }

    protected String marshalRuleName(final RuleModel model) {
        return model.name;
    }

    /**
     * Marshal model attributes
     *
     * @param buf
     * @param model
     */
    protected void marshalAttributes(final StringBuilder buf,
                                     final RuleModel model) {
        boolean hasDialect = false;
        for (int i = 0; i < model.attributes.length; i++) {
            RuleAttribute attr = model.attributes[i];

            buf.append("\t");
            buf.append(attr);

            buf.append("\n");
            if (attr.getAttributeName().equals("dialect")) {
                constraintValueBuilder = DRLConstraintValueBuilder.getBuilder(attr.getValue());
                hasDialect = true;
            }
        }
        // Un comment below for mvel
        if (!hasDialect) {
            RuleAttribute attr = new RuleAttribute("dialect",
                                                   DRLConstraintValueBuilder.DEFAULT_DIALECT);
            buf.append("\t");
            buf.append(attr);
            buf.append("\n");
        }
    }

    /**
     * Marshal model metadata
     *
     * @param buf
     * @param model
     */
    protected void marshalMetadata(final StringBuilder buf,
                                   final RuleModel model) {
        if (model.metadataList != null) {
            for (int i = 0; i < model.metadataList.length; i++) {
                buf.append("\t").append(model.metadataList[i]).append("\n");
            }
        }
    }

    /**
     * Marshal LHS patterns
     *
     * @param buf
     * @param model
     */
    protected void marshalLHS(final StringBuilder buf,
                              final RuleModel model,
                              final boolean isDSLEnhanced,
                              final LHSGeneratorContextFactory generatorContextFactory) {
        String indentation = "\t\t";
        String nestedIndentation = indentation;
        boolean isNegated = model.isNegated();

        if (model.lhs != null) {
            if (isNegated) {
                nestedIndentation += "\t";
                buf.append(indentation);
                buf.append("not (\n");
            }
            LHSPatternVisitor visitor = getLHSPatternVisitor(isDSLEnhanced,
                                                             buf,
                                                             nestedIndentation,
                                                             isNegated,
                                                             generatorContextFactory);
            for (IPattern cond : model.lhs) {
                visitor.visit(cond);
            }
            if (model.isNegated()) {
                //Delete the spurious " and ", added by LHSPatternVisitor.visitFactPattern, when the rule is negated
                buf.delete(buf.length() - 5,
                           buf.length());
                buf.append("\n");
                buf.append(indentation);
                buf.append(")\n");
            }
        }
    }

    protected LHSPatternVisitor getLHSPatternVisitor(final boolean isDSLEnhanced,
                                                     final StringBuilder buf,
                                                     final String nestedIndentation,
                                                     final boolean isNegated,
                                                     final LHSGeneratorContextFactory generatorContextFactory) {
        return new LHSPatternVisitor(isDSLEnhanced,
                                     bindingsPatterns,
                                     bindingsFields,
                                     constraintValueBuilder,
                                     generatorContextFactory,
                                     buf,
                                     nestedIndentation,
                                     isNegated);
    }

    protected void marshalRHS(final StringBuilder buf,
                              final RuleModel model,
                              final boolean isDSLEnhanced,
                              final RHSGeneratorContextFactory generatorContextFactory) {
        String indentation = "\t\t";
        if (model.rhs != null) {
            //Add boiler-plate for actions operating on Dates
            Map<String, List<ActionFieldValue>> classes = getRHSClassDependencies(model);
            if (classes.containsKey(DataType.TYPE_DATE)) {
                buf.append(indentation);
                if (isDSLEnhanced) {
                    buf.append(">");
                }
                buf.append("java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(\"" + DateUtils.getDateFormatMask() + "\");\n");
            }

            if (classes.containsKey(DataType.TYPE_LOCAL_DATE)) {
                buf.append(indentation);
                if (isDSLEnhanced) {
                    buf.append(">");
                }
                buf.append("java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern(\"" + DateUtils.getDateFormatMask() + "\");\n");
            }

            //Add boiler-plate for actions operating on WorkItems
            if (!getRHSWorkItemDependencies(model).isEmpty()) {
                buf.append(indentation);
                buf.append("org.drools.core.process.instance.WorkItemManager wim = (org.drools.core.process.instance.WorkItemManager) drools.getWorkingMemory().getWorkItemManager();\n");
            }

            //Marshall the model itself
            RHSActionVisitor actionVisitor = getRHSActionVisitor(isDSLEnhanced,
                                                                 buf,
                                                                 indentation,
                                                                 generatorContextFactory);

            //Reconcile ActionSetField and ActionUpdateField calls
            final List<IAction> actions = new ArrayList<IAction>();
            for (IAction action : model.rhs) {
                if (action instanceof ActionCallMethod) {
                    actions.add(action);
                } else if (action instanceof ActionSetField) {
                    final ActionSetField asf = (ActionSetField) action;
                    final ActionSetFieldWrapper afw = findExistingAction(asf,
                                                                         actions);
                    if (afw == null) {
                        actions.add(new ActionSetFieldWrapper(asf,
                                                              (asf instanceof ActionUpdateField)));
                    } else {
                        final List<ActionFieldValue> existingActionFieldValue = new ArrayList<ActionFieldValue>(Arrays.asList(afw.getAction().getFieldValues()));
                        for (ActionFieldValue afv : asf.getFieldValues()) {
                            existingActionFieldValue.add(afv);
                        }
                        final ActionFieldValue[] temp = new ActionFieldValue[existingActionFieldValue.size()];
                        afw.getAction().setFieldValues(existingActionFieldValue.toArray(temp));
                    }
                } else {
                    actions.add(action);
                }
            }
            model.rhs = new IAction[actions.size()];
            for (int i = 0; i < actions.size(); i++) {
                final IAction action = actions.get(i);
                if (action instanceof ActionSetFieldWrapper) {
                    model.rhs[i] = ((ActionSetFieldWrapper) action).getAction();
                } else {
                    model.rhs[i] = action;
                }
            }

            for (IAction action : model.rhs) {
                if (action instanceof PluggableIAction) {
                    PluggableIAction processedIAction = (PluggableIAction) actionVisitor.preProcessIActionForExtensions(action);
                    buf.append(indentation)
                            .append(processedIAction.getStringRepresentation())
                            .append(";\n");
                } else {
                    actionVisitor.visit(action);
                }
            }
        }
    }

    private ActionSetFieldWrapper findExistingAction(final ActionSetField asf,
                                                     final List<IAction> actions) {
        for (IAction action : actions) {
            if (action instanceof ActionSetFieldWrapper) {
                final ActionSetFieldWrapper afw = (ActionSetFieldWrapper) action;
                if (asf.getVariable().equals(afw.getAction().getVariable()) && (asf instanceof ActionUpdateField) == afw.isUpdate()) {
                    return afw;
                }
            }
        }
        return null;
    }

    private static class ActionSetFieldWrapper implements IAction {

        private final ActionSetField action;
        private final boolean isUpdate;

        private ActionSetFieldWrapper(final ActionSetField action,
                                      final boolean isUpdate) {
            this.action = clone(action);
            this.isUpdate = isUpdate;
        }

        private ActionSetField getAction() {
            return action;
        }

        private boolean isUpdate() {
            return isUpdate;
        }

        private ActionSetField clone(final ActionSetField action) {
            if (action instanceof ActionUpdateField) {
                final ActionUpdateField auf = (ActionUpdateField) action;
                final ActionUpdateField clone = new ActionUpdateField(auf.getVariable());
                clone.setFieldValues(auf.getFieldValues());
                return clone;
            } else if (action instanceof ActionCallMethod) {
                final ActionCallMethod acm = (ActionCallMethod) action;
                final ActionCallMethod clone = new ActionCallMethod(acm.getVariable());
                clone.setState(acm.getState());
                clone.setMethodName(acm.getMethodName());
                clone.setFieldValues(acm.getFieldValues());
                return clone;
            } else if (action instanceof ActionSetField) {
                final ActionSetField clone = new ActionSetField(action.getVariable());
                clone.setFieldValues(action.getFieldValues());
                return clone;
            } else {
                return action;
            }
        }
    }

    protected RHSActionVisitor getRHSActionVisitor(final boolean isDSLEnhanced,
                                                   final StringBuilder buf,
                                                   final String indentation,
                                                   final RHSGeneratorContextFactory generatorContextFactory) {
        return new RHSActionVisitor(isDSLEnhanced,
                                    bindingsPatterns,
                                    bindingsFields,
                                    constraintValueBuilder,
                                    generatorContextFactory,
                                    buf,
                                    indentation);
    }

    private Map<String, List<ActionFieldValue>> getRHSClassDependencies(final RuleModel model) {
        if (model != null) {
            RHSClassDependencyVisitor dependencyVisitor = new RHSClassDependencyVisitor();
            for (IAction action : model.rhs) {
                dependencyVisitor.visit(action);
            }
            return dependencyVisitor.getRHSClasses();
        }

        Map<String, List<ActionFieldValue>> empty = Collections.emptyMap();
        return empty;
    }

    private List<PortableWorkDefinition> getRHSWorkItemDependencies(final RuleModel model) {
        if (model != null) {
            List<PortableWorkDefinition> workItems = new ArrayList<PortableWorkDefinition>();
            for (IAction action : model.rhs) {
                if (action instanceof ActionExecuteWorkItem) {
                    workItems.add(((ActionExecuteWorkItem) action).getWorkDefinition());
                }
            }
            return workItems;
        }

        List<PortableWorkDefinition> empty = Collections.emptyList();
        return empty;
    }

    public static class LHSPatternVisitor extends ReflectiveVisitor {

        protected StringBuilder buf;
        private boolean isDSLEnhanced;
        private boolean isPatternNegated;
        private String indentation;
        private Map<String, IFactPattern> bindingsPatterns;
        private Map<String, FieldConstraint> bindingsFields;
        protected DRLConstraintValueBuilder constraintValueBuilder;
        protected LHSGeneratorContextFactory generatorContextFactory;

        protected final LHSGeneratorContext rootContext;

        public LHSPatternVisitor(final boolean isDSLEnhanced,
                                 final Map<String, IFactPattern> bindingsPatterns,
                                 final Map<String, FieldConstraint> bindingsFields,
                                 final DRLConstraintValueBuilder constraintValueBuilder,
                                 final LHSGeneratorContextFactory generatorContextFactory,
                                 final StringBuilder b,
                                 final String indentation,
                                 final boolean isPatternNegated) {
            this.isDSLEnhanced = isDSLEnhanced;
            this.bindingsPatterns = bindingsPatterns;
            this.bindingsFields = bindingsFields;
            this.constraintValueBuilder = constraintValueBuilder;
            this.generatorContextFactory = generatorContextFactory;
            this.rootContext = generatorContextFactory.newGeneratorContext();
            this.indentation = indentation;
            this.isPatternNegated = isPatternNegated;
            this.buf = b;
        }

        protected void preGeneratePattern(final LHSGeneratorContext gctx) {
            // empty, overridden by rule templates
        }

        protected void postGeneratePattern(final LHSGeneratorContext gctx) {
            // empty, overridden by rule templates
        }

        protected void preGenerateNestedConnector(final LHSGeneratorContext gctx) {
            // empty, overridden by rule templates
        }

        protected void postGenerateNestedConnector(final LHSGeneratorContext gctx) {
            // empty, overridden by rule templates
        }

        protected void preGenerateNestedConstraint(final LHSGeneratorContext gctx) {
            // empty, overridden by rule templates
        }

        protected void postGenerateNestedConstraint(final LHSGeneratorContext gctx) {
            // empty, overridden by rule templates
        }

        public void visitFactPattern(final FactPattern pattern) {
            visitFactPattern(pattern,
                             rootContext);
        }

        protected void visitFactPattern(final FactPattern pattern,
                                        final LHSGeneratorContext parentContext) {
            final LHSGeneratorContext gctx = generatorContextFactory.newPeerGeneratorContext(parentContext, pattern);
            final boolean isSubPattern = gctx.getDepth() > 0;

            if (!isSubPattern) {
                buf.append(indentation);
            }
            if (!isSubPattern && isDSLEnhanced) {
                // adding passthrough markup
                buf.append(">");
            }

            preGeneratePattern(gctx);

            generateFactPattern(pattern,
                                gctx);
            if (isPatternNegated) {
                buf.append(" and ");
            }

            postGeneratePattern(gctx);
            if (!isSubPattern) {
                buf.append("\n");
            }
        }

        public void visitFreeFormLine(final FreeFormLine ffl) {
            visitFreeFormLine(ffl,
                              rootContext);
        }

        protected void visitFreeFormLine(final FreeFormLine ffl,
                                         final LHSGeneratorContext parentContext) {
            if (ffl.getText() == null) {
                return;
            }

            final LHSGeneratorContext gctx = generatorContextFactory.newPeerGeneratorContext(parentContext, ffl);
            final boolean isSubPattern = gctx.getDepth() > 0;

            String[] lines = ffl.getText().split("\\n|\\r\\n");
            for (String line : lines) {
                this.buf.append(indentation);
                if (!isSubPattern && isDSLEnhanced) {
                    buf.append(">");
                }
                this.buf.append(line + "\n");
            }
        }

        public void visitCompositeFactPattern(final CompositeFactPattern pattern) {
            visitCompositeFactPattern(pattern,
                                      rootContext);
        }

        protected void visitCompositeFactPattern(final CompositeFactPattern pattern,
                                                 final LHSGeneratorContext parentContext) {
            final LHSGeneratorContext gctx = generatorContextFactory.newPeerGeneratorContext(parentContext, pattern);
            final boolean isSubPattern = gctx.getDepth() > 0;

            buf.append(indentation);
            if (!isSubPattern && isDSLEnhanced) {
                // adding passthrough markup
                buf.append(">");
            }
            if (CompositeFactPattern.COMPOSITE_TYPE_EXISTS.equals(pattern.getType())) {
                renderCompositeFOL(pattern, gctx);
            } else if (CompositeFactPattern.COMPOSITE_TYPE_NOT.equals(pattern.getType())) {
                renderCompositeFOL(pattern, gctx);
            } else if (CompositeFactPattern.COMPOSITE_TYPE_OR.equals(pattern.getType())) {
                buf.append("( ");
                if (pattern.getPatterns() != null) {
                    for (int i = 0; i < pattern.getPatterns().length; i++) {
                        if (i > 0) {
                            buf.append(" ");
                            buf.append(pattern.getType());
                            buf.append(" ");
                        }
                        renderSubPattern(pattern,
                                         gctx,
                                         i);
                    }
                }
                buf.append(" )\n");
            }
        }

        public void visitFromCompositeFactPattern(final FromCompositeFactPattern pattern) {
            visitFromCompositeFactPattern(pattern,
                                          rootContext);
        }

        protected void visitFromCompositeFactPattern(final FromCompositeFactPattern pattern,
                                                     final LHSGeneratorContext parentContext) {
            final LHSGeneratorContext gctx = generatorContextFactory.newPeerGeneratorContext(parentContext, pattern.getFactPattern());
            final boolean isSubPattern = gctx.getDepth() > 0;

            buf.append(indentation);
            if (!isSubPattern && isDSLEnhanced) {
                // adding passthrough markup
                buf.append(">");
            }

            if (pattern.getFactPattern() != null) {

                // DROOLS-1308 - wraps from pattern in parenthesis
                if (!isSubPattern) {
                    buf.append("(");
                }
                generateFactPattern(pattern.getFactPattern(),
                                    gctx);

                buf.append(" from ");
                renderExpression(pattern.getExpression());
                if (!isSubPattern) {
                    buf.append(")");
                }
                buf.append("\n");
            }
        }

        public void visitFromCollectCompositeFactPattern(final FromCollectCompositeFactPattern pattern) {
            visitFromCollectCompositeFactPattern(pattern,
                                                 rootContext);
        }

        protected void visitFromCollectCompositeFactPattern(final FromCollectCompositeFactPattern pattern,
                                                            final LHSGeneratorContext parentContext) {
            final LHSGeneratorContext gctx = generatorContextFactory.newPeerGeneratorContext(parentContext, pattern.getFactPattern());
            final boolean isSubPattern = gctx.getDepth() > 0;

            buf.append(indentation);
            if (!isSubPattern && isDSLEnhanced) {
                // adding passthrough markup
                buf.append(">");
            }

            if (pattern.getFactPattern() != null) {
                generateFactPattern(pattern.getFactPattern(),
                                    gctx);

                buf.append(" from collect ( ");

                if (pattern.getRightPattern() != null) {
                    final LHSGeneratorContext childContext = generatorContextFactory.newChildGeneratorContext(gctx, pattern.getFactPattern());
                    if (pattern.getRightPattern() instanceof FactPattern) {
                        visitFactPattern((FactPattern) pattern.getRightPattern(),
                                         childContext);
                    } else if (pattern.getRightPattern() instanceof FromAccumulateCompositeFactPattern) {
                        visitFromAccumulateCompositeFactPattern((FromAccumulateCompositeFactPattern) pattern.getRightPattern(),
                                                                childContext);
                    } else if (pattern.getRightPattern() instanceof FromCollectCompositeFactPattern) {
                        visitFromCollectCompositeFactPattern((FromCollectCompositeFactPattern) pattern.getRightPattern(),
                                                             childContext);
                    } else if (pattern.getRightPattern() instanceof FromEntryPointFactPattern) {
                        visitFromEntryPointFactPattern((FromEntryPointFactPattern) pattern.getRightPattern(),
                                                       childContext);
                    } else if (pattern.getRightPattern() instanceof FromCompositeFactPattern) {
                        visitFromCompositeFactPattern((FromCompositeFactPattern) pattern.getRightPattern(),
                                                      childContext);
                    } else if (pattern.getRightPattern() instanceof FreeFormLine) {
                        visitFreeFormLine((FreeFormLine) pattern.getRightPattern(),
                                          childContext);
                    } else {
                        throw new IllegalArgumentException("Unsupported pattern " + pattern.getRightPattern() + " for FROM COLLECT");
                    }
                }
                if (!isSubPattern && isDSLEnhanced) {
                    buf.append("\n"); // Just in case we add a row. Not sure what the methods above append.
                    buf.append(indentation);
                    buf.append(">");
                }
                buf.append(") \n");
            }
        }

        public void visitFromAccumulateCompositeFactPattern(final FromAccumulateCompositeFactPattern pattern) {
            visitFromAccumulateCompositeFactPattern(pattern,
                                                    rootContext);
        }

        protected void visitFromAccumulateCompositeFactPattern(final FromAccumulateCompositeFactPattern pattern,
                                                               final LHSGeneratorContext parentContext) {
            final LHSGeneratorContext gctx = generatorContextFactory.newPeerGeneratorContext(parentContext, pattern.getFactPattern());
            final boolean isSubPattern = gctx.getDepth() > 0;

            buf.append(indentation);
            if (!isSubPattern && isDSLEnhanced) {
                // adding passthrough markup
                buf.append(">");
            }

            if (pattern.getFactPattern() != null) {
                generateFactPattern(pattern.getFactPattern(),
                                    gctx);

                buf.append(" from accumulate ( ");
                if (pattern.getSourcePattern() != null) {
                    final LHSGeneratorContext childContext = generatorContextFactory.newChildGeneratorContext(gctx, pattern.getFactPattern());
                    if (pattern.getSourcePattern() instanceof FactPattern) {
                        generateFactPattern((FactPattern) pattern.getSourcePattern(),
                                            childContext);
                    } else if (pattern.getSourcePattern() instanceof FromAccumulateCompositeFactPattern) {
                        visitFromAccumulateCompositeFactPattern((FromAccumulateCompositeFactPattern) pattern.getSourcePattern(),
                                                                childContext);
                    } else if (pattern.getSourcePattern() instanceof FromCollectCompositeFactPattern) {
                        visitFromCollectCompositeFactPattern((FromCollectCompositeFactPattern) pattern.getSourcePattern(),
                                                             childContext);
                    } else if (pattern.getSourcePattern() instanceof FromEntryPointFactPattern) {
                        visitFromEntryPointFactPattern((FromEntryPointFactPattern) pattern.getSourcePattern(),
                                                       childContext);
                    } else if (pattern.getSourcePattern() instanceof FromCompositeFactPattern) {
                        visitFromCompositeFactPattern((FromCompositeFactPattern) pattern.getSourcePattern(),
                                                      childContext);
                    } else {
                        throw new IllegalArgumentException("Unsupported pattern " + pattern.getSourcePattern() + " for FROM ACCUMULATE");
                    }
                }
                buf.append(",\n");

                if (pattern.useFunctionOrCode().equals(FromAccumulateCompositeFactPattern.USE_FUNCTION)) {
                    if (!isSubPattern && isDSLEnhanced) {
                        buf.append(">");
                    }
                    buf.append(indentation + "\t");
                    buf.append(pattern.getFunction());
                } else {
                    if (!isSubPattern && isDSLEnhanced) {
                        buf.append(">");
                    }
                    buf.append(indentation + "\tinit( ");
                    buf.append(pattern.getInitCode());
                    buf.append(" ),\n");
                    if (!isSubPattern && isDSLEnhanced) {
                        buf.append(">");
                    }
                    buf.append(indentation + "\taction( ");
                    buf.append(pattern.getActionCode());
                    buf.append(" ),\n");
                    if (pattern.getReverseCode() != null && !pattern.getReverseCode().trim().equals("")) {
                        if (!isSubPattern && isDSLEnhanced) {
                            buf.append(">");
                        }
                        buf.append(indentation + "\treverse( ");
                        buf.append(pattern.getReverseCode());
                        buf.append(" ),\n");
                    }
                    if (!isSubPattern && isDSLEnhanced) {
                        buf.append(">");
                    }
                    buf.append(indentation + "\tresult( ");
                    buf.append(pattern.getResultCode());
                    buf.append(" )\n");
                }
                buf.append(") \n");
            }
        }

        public void visitFromEntryPointFactPattern(final FromEntryPointFactPattern pattern) {
            visitFromEntryPointFactPattern(pattern,
                                           rootContext);
        }

        protected void visitFromEntryPointFactPattern(final FromEntryPointFactPattern pattern,
                                                      final LHSGeneratorContext parentContext) {
            if (pattern.getFactPattern() != null) {
                final LHSGeneratorContext gctx = generatorContextFactory.newPeerGeneratorContext(parentContext, pattern.getFactPattern());
                final boolean isSubPattern = gctx.getDepth() > 0;

                buf.append(indentation);
                if (!isSubPattern && isDSLEnhanced) {
                    // adding passthrough markup
                    buf.append(">");
                }

                generateFactPattern(pattern.getFactPattern(),
                                    gctx);
                buf.append(" from entry-point \"" + pattern.getEntryPointName() + "\"\n");
            }
        }

        private void renderCompositeFOL(final CompositeFactPattern pattern,
                                        final LHSGeneratorContext parentContext) {
            buf.append(pattern.getType());
            if (pattern.getPatterns() != null) {
                buf.append(" (");
                for (int i = 0; i < pattern.getPatterns().length; i++) {
                    final LHSGeneratorContext childContext = generatorContextFactory.newChildGeneratorContext(parentContext, pattern);
                    renderSubPattern(pattern,
                                     childContext,
                                     i);
                    if (i != pattern.getPatterns().length - 1) {
                        buf.append(" and ");
                    }
                }
                buf.append(") \n");
            }
        }

        private void renderSubPattern(final CompositeFactPattern pattern,
                                      final LHSGeneratorContext parentContext,
                                      final int subIndex) {
            if (pattern.getPatterns() == null || pattern.getPatterns().length == 0) {
                return;
            }
            IFactPattern subPattern = pattern.getPatterns()[subIndex];
            if (subPattern instanceof FactPattern) {
                this.generateFactPattern((FactPattern) subPattern,
                                         parentContext);
            } else if (subPattern instanceof FromAccumulateCompositeFactPattern) {
                this.visitFromAccumulateCompositeFactPattern((FromAccumulateCompositeFactPattern) subPattern,
                                                             parentContext);
            } else if (subPattern instanceof FromCollectCompositeFactPattern) {
                this.visitFromCollectCompositeFactPattern((FromCollectCompositeFactPattern) subPattern,
                                                          parentContext);
            } else if (subPattern instanceof FromCompositeFactPattern) {
                this.visitFromCompositeFactPattern((FromCompositeFactPattern) subPattern,
                                                   parentContext);
            } else {
                throw new IllegalStateException("Unsupported Pattern: " + subPattern.getClass().getName());
            }
        }

        private void renderExpression(final ExpressionFormLine expression) {
            final ToStringExpressionVisitor visitor = new ToStringExpressionVisitor(constraintValueBuilder);
            buf.append(expression.getText(visitor));
        }

        public void visitDSLSentence(final DSLSentence sentence) {
            buf.append(indentation);
            buf.append(sentence.interpolate());
            buf.append("\n");
        }

        private void generateFactPattern(final FactPattern pattern,
                                         final LHSGeneratorContext gctx) {
            if (pattern.isNegated()) {
                buf.append("not ");
            } else if (pattern.isBound()) {
                bindingsPatterns.put(pattern.getBoundName(),
                                     pattern);
                buf.append(pattern.getBoundName());
                buf.append(" : ");
            }
            if (pattern.getFactType() != null) {
                buf.append(pattern.getFactType());
            }
            buf.append("( ");

            // top level constraints
            if (pattern.getConstraintList() != null) {
                generateConstraints(pattern,
                                    gctx);
            }
            buf.append(")");

            //Add CEP window definition
            CEPWindow window = pattern.getWindow();
            if (window.isDefined()) {
                buf.append(" ");
                buf.append(window.getOperator());
                buf.append(buildOperatorParameterDRL(window.getParameters()));
            }
        }

        private void generateConstraints(final FactPattern pattern,
                                         final LHSGeneratorContext parentContext) {
            LHSGeneratorContext gctx = null;
            for (int constraintIndex = 0; constraintIndex < pattern.getFieldConstraints().length; constraintIndex++) {
                FieldConstraint constr = pattern.getConstraintList().getConstraints()[constraintIndex];

                if (constraintIndex == 0) {
                    gctx = generatorContextFactory.newChildGeneratorContext(parentContext,
                                                                            constr);
                } else {
                    gctx = generatorContextFactory.newPeerGeneratorContext(gctx,
                                                                           constr);
                }

                generateConstraint(constr,
                                   gctx);
            }
        }

        public void generateSeparator(final FieldConstraint constr,
                                      final LHSGeneratorContext gctx) {
            if (!doesPeerHaveOutput(gctx)) {
                return;
            }
            if (gctx.getParent().getFieldConstraint() instanceof CompositeFieldConstraint) {
                CompositeFieldConstraint cconstr = (CompositeFieldConstraint) gctx.getParent().getFieldConstraint();
                buf.append(cconstr.getCompositeJunctionType() + " ");
            } else {
                if (buf.length() > 2 && !(buf.charAt(buf.length() - 2) == ',')) {
                    buf.append(", ");
                }
            }
        }

        protected boolean doesPeerHaveOutput(final LHSGeneratorContext gctx) {
            final List<LHSGeneratorContext> peers = generatorContextFactory.getPeers(gctx);
            for (LHSGeneratorContext c : peers) {
                if (c.isHasOutput()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Recursively process the nested constraints. It will only put brackets
         * in for the ones that aren't at top level. This makes for more
         * readable DRL in the most common cases.
         */
        protected void generateConstraint(final FieldConstraint con,
                                          final LHSGeneratorContext parentContext) {
            generateSeparator(con,
                              parentContext);
            if (con instanceof CompositeFieldConstraint) {
                CompositeFieldConstraint cfc = (CompositeFieldConstraint) con;
                FieldConstraint[] nestedConstraints = cfc.getConstraints();
                if (nestedConstraints != null) {
                    LHSGeneratorContext nestedGctx = generatorContextFactory.newChildGeneratorContext(parentContext,
                                                                                                      con);
                    preGenerateNestedConstraint(nestedGctx);
                    if (parentContext.getParent().getFieldConstraint() instanceof CompositeFieldConstraint) {
                        buf.append("( ");
                    }
                    LHSGeneratorContext gctx = null;
                    for (int nestedConstraintIndex = 0; nestedConstraintIndex < nestedConstraints.length; nestedConstraintIndex++) {
                        FieldConstraint nestedConstr = nestedConstraints[nestedConstraintIndex];

                        if (nestedConstraintIndex == 0) {
                            gctx = generatorContextFactory.newChildGeneratorContext(nestedGctx,
                                                                                    nestedConstr);
                        } else {
                            gctx = generatorContextFactory.newPeerGeneratorContext(gctx,
                                                                                   nestedConstr);
                        }

                        generateConstraint(nestedConstr,
                                           gctx);
                    }
                    if (parentContext.getParent().getFieldConstraint() instanceof CompositeFieldConstraint) {
                        buf.append(")");
                    }
                    postGenerateNestedConstraint(parentContext);
                }
            } else {
                generateSingleFieldConstraint((SingleFieldConstraint) con,
                                              parentContext);
            }
        }

        private void generateSingleFieldConstraint(final SingleFieldConstraint constr,
                                                   final LHSGeneratorContext gctx) {
            if (constr.getConstraintValueType() == BaseSingleFieldConstraint.TYPE_PREDICATE) {
                generatePredicateSingleFieldConstraint(constr,
                                                       gctx);
            } else {
                if (constr.isBound()) {
                    bindingsFields.put(constr.getFieldBinding(),
                                       constr);
                    buf.append(constr.getFieldBinding());
                    buf.append(" : ");
                }

                assertConstraintValue(constr);

                if (isConstraintComplete(constr)) {
                    if (constr instanceof SingleFieldConstraintEBLeftSide) {
                        final SingleFieldConstraintEBLeftSide sfcexp = ((SingleFieldConstraintEBLeftSide) constr);
                        final ToStringExpressionVisitor visitor = new ToStringExpressionVisitor(constraintValueBuilder);
                        buf.append(sfcexp.getExpressionLeftSide().getText(visitor));
                    } else {
                        SingleFieldConstraint parent = (SingleFieldConstraint) constr.getParent();
                        StringBuilder parentBuf = new StringBuilder();
                        while (parent != null) {
                            String fieldName = parent.getFieldName();
                            parentBuf.insert(0,
                                             fieldName + ".");
                            parent = (SingleFieldConstraint) parent.getParent();
                        }
                        buf.append(parentBuf);
                        String fieldName = constr.getFieldName();
                        buf.append(fieldName);
                    }

                    final Map<String, String> parameters = constr.getParameters();
                    if (constr.getConnectives() == null) {
                        generateNormalFieldRestriction(constr,
                                                       parameters);
                    } else {
                        generateConnectiveFieldRestriction(constr,
                                                           parameters,
                                                           gctx);
                    }
                    gctx.setHasOutput(true);
                }
            }
        }

        protected void generatePredicateSingleFieldConstraint(final SingleFieldConstraint constr,
                                                              final LHSGeneratorContext gctx) {
            buf.append("eval( ");
            buf.append(constr.getValue());
            buf.append(" )");
            gctx.setHasOutput(true);
        }

        private void generateNormalFieldRestriction(final SingleFieldConstraint constr,
                                                    final Map<String, String> parameters) {
            if (constr instanceof SingleFieldConstraintEBLeftSide) {
                SingleFieldConstraintEBLeftSide sfexp = (SingleFieldConstraintEBLeftSide) constr;
                addFieldRestriction(buf,
                                    sfexp.getConstraintValueType(),
                                    sfexp.getExpressionLeftSide().getGenericType(),
                                    sfexp.getOperator(),
                                    parameters,
                                    sfexp.getValue(),
                                    sfexp.getExpressionValue(),
                                    true);
            } else {
                addFieldRestriction(buf,
                                    constr.getConstraintValueType(),
                                    constr.getFieldType(),
                                    constr.getOperator(),
                                    parameters,
                                    constr.getValue(),
                                    constr.getExpressionValue(),
                                    true);
            }
        }

        private void generateConnectiveFieldRestriction(final SingleFieldConstraint constr,
                                                        final Map<String, String> parameters,
                                                        final LHSGeneratorContext gctx) {
            LHSGeneratorContext cctx = generatorContextFactory.newChildGeneratorContext(gctx,
                                                                                        constr);
            if (constr instanceof SingleFieldConstraintEBLeftSide) {
                SingleFieldConstraintEBLeftSide sfexp = (SingleFieldConstraintEBLeftSide) constr;
                addConnectiveFieldRestriction(buf,
                                              sfexp.getConstraintValueType(),
                                              sfexp.getExpressionLeftSide().getGenericType(),
                                              sfexp.getOperator(),
                                              parameters,
                                              sfexp.getValue(),
                                              sfexp.getExpressionValue(),
                                              cctx,
                                              true);
            } else {
                addConnectiveFieldRestriction(buf,
                                              constr.getConstraintValueType(),
                                              constr.getFieldType(),
                                              constr.getOperator(),
                                              parameters,
                                              constr.getValue(),
                                              constr.getExpressionValue(),
                                              cctx,
                                              true);
            }

            for (int j = 0; j < constr.getConnectives().length; j++) {
                final ConnectiveConstraint conn = constr.getConnectives()[j];
                final Map<String, String> connectiveParameters = conn.getParameters();

                addConnectiveFieldRestriction(buf,
                                              conn.getConstraintValueType(),
                                              conn.getFieldType(),
                                              conn.getOperator(),
                                              connectiveParameters,
                                              conn.getValue(),
                                              conn.getExpressionValue(),
                                              cctx,
                                              true);
            }
        }

        private void assertConstraintValue(final SingleFieldConstraint sfc) {
            if (DataType.TYPE_STRING.equals(sfc.getFieldType())) {
                if (sfc.getValue() == null) {
                    sfc.setValue("");
                }
            }
        }

        private boolean isConstraintComplete(final SingleFieldConstraint constr) {
            if (constr.getConstraintValueType() == BaseSingleFieldConstraint.TYPE_EXPR_BUILDER_VALUE) {
                return true;
            } else if (constr instanceof SingleFieldConstraintEBLeftSide) {
                return true;
            } else if (constr.getFieldBinding() != null) {
                return true;
            }
            final String operator = constr.getOperator();
            final String fieldType = constr.getFieldType();
            final String fieldValue = constr.getValue();
            if (operator == null) {
                return false;
            }
            if (operator.equals("== null") || operator.equals("!= null")) {
                return true;
            }
            if (DataType.TYPE_STRING.equals(fieldType)) {
                return true;
            }

            return !(fieldValue == null || fieldValue.isEmpty());
        }

        protected void addConnectiveFieldRestriction(final StringBuilder buf,
                                                     final int type,
                                                     final String fieldType,
                                                     final String operator,
                                                     final Map<String, String> parameters,
                                                     final String value,
                                                     final ExpressionFormLine expression,
                                                     LHSGeneratorContext gctx,
                                                     final boolean spaceBeforeOperator) {
            addFieldRestriction(buf,
                                type,
                                fieldType,
                                operator,
                                parameters,
                                value,
                                expression,
                                spaceBeforeOperator);
        }

        private void addFieldRestriction(final StringBuilder buf,
                                         final int type,
                                         final String fieldType,
                                         final String operator,
                                         final Map<String, String> parameters,
                                         final String value,
                                         final ExpressionFormLine expression,
                                         final boolean spaceBeforeOperator) {
            if (operator == null) {
                return;
            }

            if (spaceBeforeOperator) {
                buf.append(" ");
            }
            buf.append(operator);

            if (parameters != null && parameters.size() > 0) {
                buf.append(buildOperatorParameterDRL(parameters));
            }

            switch (type) {
                case BaseSingleFieldConstraint.TYPE_RET_VALUE:
                    buildReturnValueFieldValue(value,
                                               buf);
                    break;
                case BaseSingleFieldConstraint.TYPE_LITERAL:
                    buildLiteralFieldValue(operator,
                                           type,
                                           fieldType,
                                           value,
                                           buf);
                    break;
                case BaseSingleFieldConstraint.TYPE_EXPR_BUILDER_VALUE:
                    buildExpressionFieldValue(expression,
                                              buf);
                    break;
                case BaseSingleFieldConstraint.TYPE_TEMPLATE:
                    buildTemplateFieldValue(operator,
                                            type,
                                            fieldType,
                                            value,
                                            buf);
                    break;
                case BaseSingleFieldConstraint.TYPE_ENUM:
                    buildEnumFieldValue(operator,
                                        type,
                                        fieldType,
                                        value,
                                        buf);
                    break;
                default:
                    buildDefaultFieldValue(operator,
                                           value,
                                           buf);
            }
        }

        protected void buildReturnValueFieldValue(final String value,
                                                  final StringBuilder buf) {
            buf.append(" ");
            buf.append("( ");
            buf.append(value);
            buf.append(" )");
            buf.append(" ");
        }

        protected StringBuilder buildOperatorParameterDRL(final Map<String, String> parameters) {
            String className = parameters.get(SharedConstants.OPERATOR_PARAMETER_GENERATOR);
            if (className == null) {
                throw new IllegalStateException("Implementation of 'org.kie.guvnor.guided.server.util.OperatorParameterDRLBuilder' undefined. Unable to build Operator Parameter DRL.");
            }

            try {
                OperatorParameterDRLBuilder builder = (OperatorParameterDRLBuilder) Class.forName(className).newInstance();
                return builder.buildDRL(parameters);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalStateException("Unable to generate Operator DRL using class '" + className + "'.",
                                                cnfe);
            } catch (IllegalAccessException iae) {
                throw new IllegalStateException("Unable to generate Operator DRL using class '" + className + "'.",
                                                iae);
            } catch (InstantiationException ie) {
                throw new IllegalStateException("Unable to generate Operator DRL using class '" + className + "'.",
                                                ie);
            }
        }

        protected void buildLiteralFieldValue(final String operator,
                                              final int type,
                                              final String fieldType,
                                              final String value,
                                              final StringBuilder buf) {
            if (OperatorsOracle.operatorRequiresList(operator)) {
                populateValueList(buf,
                                  type,
                                  fieldType,
                                  value);
            } else {
                if (!operator.equals("== null") && !operator.equals("!= null")) {
                    buf.append(" ");
                    constraintValueBuilder.buildLHSFieldValue(buf,
                                                              type,
                                                              fieldType,
                                                              value);
                }
            }
            buf.append(" ");
        }

        protected void populateValueList(final StringBuilder buf,
                                         final int type,
                                         final String fieldType,
                                         final String value) {
            String workingValue = value.trim();
            if (workingValue.startsWith("(") && workingValue.endsWith(")")) {
                workingValue = workingValue.substring(1,
                                                      workingValue.length() - 1);
            }

            final String[] values = ListSplitter.split("\"", true, workingValue);
            buf.append(" ( ");
            for (String v : values) {
                constraintValueBuilder.buildLHSFieldValue(buf,
                                                          type,
                                                          fieldType,
                                                          v);
                buf.append(", ");
            }
            buf.delete(buf.length() - 2,
                       buf.length());
            buf.append(" )");
        }

        protected void buildExpressionFieldValue(final ExpressionFormLine expression,
                                                 final StringBuilder buf) {
            if (expression != null) {
                buf.append(" ");
                final ToStringExpressionVisitor visitor = new ToStringExpressionVisitor(constraintValueBuilder);
                buf.append(expression.getText(visitor));
                buf.append(" ");
            }
        }

        protected void buildTemplateFieldValue(final String operator,
                                               final int type,
                                               final String fieldType,
                                               final String value,
                                               final StringBuilder buf) {
            if (OperatorsOracle.operatorRequiresList(operator)) {
                buf.append(" ");
                constraintValueBuilder.buildLHSFieldValue(buf,
                                                          type,
                                                          DataType.TYPE_COLLECTION,
                                                          "@{makeValueList(" + value + "," + DataType.isNumeric(fieldType) + ")}");
                buf.append(" ");
            } else {
                buf.append(" ");
                constraintValueBuilder.buildLHSFieldValue(buf,
                                                          type,
                                                          fieldType,
                                                          "@{removeDelimitingQuotes(" + value + ")}");
                buf.append(" ");
            }
        }

        private void buildEnumFieldValue(final String operator,
                                         final int type,
                                         final String fieldType,
                                         final String value,
                                         final StringBuilder buf) {

            if (OperatorsOracle.operatorRequiresList(operator)) {
                populateValueList(buf,
                                  type,
                                  fieldType,
                                  value);
            } else {
                if (!operator.equals("== null") && !operator.equals("!= null")) {
                    buf.append(" ");
                    constraintValueBuilder.buildLHSFieldValue(buf,
                                                              type,
                                                              fieldType,
                                                              value);
                }
            }
            buf.append(" ");
        }

        protected void buildDefaultFieldValue(final String operator,
                                              final String value,
                                              final StringBuilder buf) {
            if (!operator.equals("== null") && !operator.equals("!= null")) {
                buf.append(" ");
                buf.append(value);
            }
            buf.append(" ");
        }
    }

    public static class RHSActionVisitor extends ReflectiveVisitor {

        protected StringBuilder buf;
        private boolean isDSLEnhanced;
        private String indentation;
        private Map<String, IFactPattern> bindingsPatterns;
        private Map<String, FieldConstraint> bindingsFields;
        protected DRLConstraintValueBuilder constraintValueBuilder;
        protected RHSGeneratorContextFactory generatorContextFactory;
        //        protected Collection<RuleModelIActionPersistenceExtension> extensions;

        protected final RHSGeneratorContext rootContext;

        //Keep a record of Work Items that are instantiated for Actions that depend on them
        private Set<String> instantiatedWorkItems;

        public RHSActionVisitor(final boolean isDSLEnhanced,
                                final Map<String, IFactPattern> bindingsPatterns,
                                final Map<String, FieldConstraint> bindingsFields,
                                final DRLConstraintValueBuilder constraintValueBuilder,
                                final RHSGeneratorContextFactory generatorContextFactory,
                                final StringBuilder b,
                                final String indentation) {
            this.isDSLEnhanced = isDSLEnhanced;
            this.bindingsPatterns = bindingsPatterns;
            this.bindingsFields = bindingsFields;
            this.constraintValueBuilder = constraintValueBuilder;
            this.generatorContextFactory = generatorContextFactory;
            this.rootContext = generatorContextFactory.newGeneratorContext();
            this.indentation = indentation;
            this.instantiatedWorkItems = new HashSet<String>();
            this.buf = b;
        }

        protected void preGenerateAction(final RHSGeneratorContext gctx) {
            // empty, overridden by rule templates
        }

        protected void postGenerateAction(final RHSGeneratorContext gctx) {
            // empty, overridden by rule templates
        }

        protected void preGenerateSetMethodCallParameterValue(final RHSGeneratorContext gctx,
                                                              final ActionFieldValue fieldValue) {
            gctx.setHasOutput(true);
        }

        protected IAction preProcessIActionForExtensions(final IAction iAction) {
            // return parameter value by default, override by child classes where necessary
            return iAction;
        }

        public void visitActionInsertFact(final ActionInsertFact action) {
            this.generateInsertCall(action,
                                    false);
        }

        public void visitActionInsertLogicalFact(final ActionInsertLogicalFact action) {
            this.generateInsertCall(action,
                                    true);
        }

        public void visitFreeFormLine(final FreeFormLine ffl) {
            if (ffl.getText() == null) {
                return;
            }
            String[] lines = ffl.getText().split("\\n|\\r\\n");
            for (String line : lines) {
                this.buf.append(indentation);
                if (isDSLEnhanced) {
                    buf.append(">");
                }
                this.buf.append(line + "\n");
            }
        }

        private void generateInsertCall(final ActionInsertFact action,
                                        final boolean isLogic) {
            buf.append(indentation);
            if (isDSLEnhanced) {
                buf.append(">");
            }
            final String binding = action.getBoundName();
            if (action.getFieldValues().length == 0 && binding == null) {
                buf.append((isLogic) ? "insertLogical( new " : "insert( new ");

                buf.append(action.getFactType());
                buf.append("() );\n");
            } else {
                buf.append(action.getFactType());
                buf.append(" " + binding);
                buf.append(" = new ");
                buf.append(action.getFactType());
                buf.append("();\n");
                generateSetMethodCalls(binding,
                                       action.getFieldValues());

                buf.append(indentation);
                if (isDSLEnhanced) {
                    buf.append(">");
                }
                if (isLogic) {
                    buf.append("insertLogical( ");
                    buf.append(binding);
                    buf.append(" );\n");
                } else {
                    buf.append("insert( ");
                    buf.append(binding);

                    buf.append(" );\n");
                }
            }
        }

        public void visitActionUpdateField(final ActionUpdateField action) {
            final RHSGeneratorContext gctx = generatorContextFactory.newChildGeneratorContext(rootContext,
                                                                                              action);
            preGenerateAction(gctx);

            buf.append(indentation);
            if (isDSLEnhanced) {
                buf.append(">");
            }
            buf.append("modify( ").append(action.getVariable()).append(" ) {\n");
            this.generateModifyMethodCalls(action.getFieldValues(),
                                           gctx);
            buf.append("\n").append(indentation);
            if (isDSLEnhanced) {
                buf.append(">");
            }
            buf.append("}\n");

            postGenerateAction(gctx);
        }

        public void visitActionGlobalCollectionAdd(final ActionGlobalCollectionAdd add) {
            buf.append(indentation);
            if (isDSLEnhanced) {
                buf.append(">");
            }
            buf.append(add.getGlobalName() + ".add( " + add.getFactName() + " );\n");
        }

        public void visitActionRetractFact(final ActionRetractFact action) {
            buf.append(indentation);
            if (isDSLEnhanced) {
                buf.append(">");
            }
            buf.append("retract( ");
            buf.append(action.getVariableName());
            buf.append(" );\n");
        }

        public void visitDSLSentence(final DSLSentence sentence) {
            buf.append(indentation);
            buf.append(sentence.interpolate());
            buf.append("\n");
        }

        public void visitActionExecuteWorkItem(final ActionExecuteWorkItem action) {
            String wiName = action.getWorkDefinition().getName();
            String wiImplName = WORKITEM_PREFIX + wiName;

            instantiatedWorkItems.add(wiName);

            buf.append(indentation);
            buf.append("org.drools.core.process.instance.impl.WorkItemImpl ");
            buf.append(wiImplName);
            buf.append(" = new org.drools.core.process.instance.impl.WorkItemImpl();\n");
            buf.append(indentation);
            buf.append(wiImplName);
            buf.append(".setName( \"");
            buf.append(wiName);
            buf.append("\" );\n");
            for (PortableParameterDefinition ppd : action.getWorkDefinition().getParameters()) {
                makeWorkItemParameterDRL(ppd,
                                         wiImplName);
            }
            buf.append(indentation);
            buf.append("wim.internalExecuteWorkItem( ");
            buf.append(wiImplName);
            buf.append(" );\n");
        }

        private void makeWorkItemParameterDRL(final PortableParameterDefinition ppd,
                                              final String wiImplName) {
            boolean makeParameter = true;

            //Only add bound parameters if their binding exists (i.e. the corresponding column has a value or - for Limited Entry - is true)
            if (ppd instanceof HasBinding) {
                HasBinding hb = (HasBinding) ppd;
                if (hb.isBound()) {
                    String binding = hb.getBinding();
                    makeParameter = isBindingValid(binding);
                }
            }
            if (makeParameter) {
                buf.append(indentation);
                buf.append(wiImplName);
                buf.append(".getParameters().put( \"");
                buf.append(ppd.getName());
                buf.append("\", ");
                buf.append(ppd.asString());
                buf.append(" );\n");
            }
        }

        private boolean isBindingValid(final String binding) {
            if (bindingsPatterns.containsKey(binding)) {
                return true;
            }
            if (bindingsFields.containsKey(binding)) {
                return true;
            }
            return false;
        }

        public void visitActionSetField(final ActionSetField action) {
            if (action instanceof ActionCallMethod) {
                visitActionCallMethod((ActionCallMethod)action);
            } else {
                this.generateSetMethodCalls(action.getVariable(),
                                            action.getFieldValues());
            }
        }

        public void visitActionCallMethod(final ActionCallMethod action) {
            final RHSGeneratorContext gctx = generatorContextFactory.newChildGeneratorContext(rootContext,
                                                                                              action);
            preGenerateAction(gctx);

            this.generateActionCallMethod(action,
                                          action.getFieldValues());

            postGenerateAction(gctx);
        }

        private void generateSetMethodCalls(final String variableName,
                                            final ActionFieldValue[] fieldValues) {
            for (int i = 0; i < fieldValues.length; i++) {
                generateSetMethodCall(variableName,
                                      fieldValues[i]);
            }
        }

        protected void generateSetMethodCall(final String variableName,
                                             final ActionFieldValue fieldValue) {
            buf.append(indentation);
            if (isDSLEnhanced) {
                buf.append(">");
            }
            buf.append(variableName);

            if (fieldValue instanceof ActionFieldFunction) {
                buf.append(".");
                buf.append(fieldValue.getField());
            } else {
                buf.append(".set");
                buf.append(formatFieldFirstCharacterToFitDroolsCoreStandards(fieldValue.getField()));
                buf.append(fieldValue.getField().substring(1));
            }
            buf.append("( ");
            generateSetMethodCallParameterValue(buf,
                                                fieldValue);
            buf.append(" );\n");
        }

        private void generateSetMethodCallParameterValue(final StringBuilder buf,
                                                         final ActionFieldValue fieldValue) {
            if (fieldValue.isFormula()) {
                buildFormulaFieldValue(fieldValue,
                                       buf);
            } else if (fieldValue.getNature() == FieldNatureType.TYPE_VARIABLE) {
                buildVariableFieldValue(fieldValue,
                                        buf);
            } else if (fieldValue.getNature() == FieldNatureType.TYPE_TEMPLATE) {
                buildTemplateFieldValue(fieldValue,
                                        buf);
            } else if (fieldValue instanceof ActionWorkItemFieldValue) {
                buildWorkItemFieldValue((ActionWorkItemFieldValue) fieldValue,
                                        buf);
            } else {
                buildDefaultFieldValue(fieldValue,
                                       buf);
            }
        }

        private void generateModifyMethodCalls(final ActionFieldValue[] fieldValues,
                                               final RHSGeneratorContext parentContext) {
            RHSGeneratorContext gctx = null;
            for (int index = 0; index < fieldValues.length; index++) {
                final ActionFieldValue fieldValue = fieldValues[index];
                if (index == 0) {
                    gctx = generatorContextFactory.newChildGeneratorContext(parentContext,
                                                                            fieldValue);
                } else {
                    gctx = generatorContextFactory.newPeerGeneratorContext(gctx,
                                                                           fieldValue);
                }

                preGenerateSetMethodCallParameterValue(gctx,
                                                       fieldValue);
                generateModifyMethodSeparator(gctx,
                                              fieldValue);
                generateModifyMethodCall(gctx,
                                         fieldValue);
            }
        }

        protected void generateModifyMethodCall(final RHSGeneratorContext gctx,
                                                final ActionFieldValue fieldValue) {
            buf.append(indentation).append(indentation);
            if (isDSLEnhanced) {
                buf.append(">");
            }

            if (fieldValue instanceof ActionFieldFunction) {
                buf.append(fieldValue.getField());
            } else {
                buf.append("set");
                buf.append(formatFieldFirstCharacterToFitDroolsCoreStandards(fieldValue.getField()));
                buf.append(fieldValue.getField().substring(1));
            }
            buf.append("( ");
            generateSetMethodCallParameterValue(buf,
                                                fieldValue);
            buf.append(" )");
        }

        protected void generateModifyMethodSeparator(final RHSGeneratorContext gctx,
                                                     final ActionFieldValue fieldValue) {
            if (doesPeerHaveOutput(gctx)) {
                buf.append(", \n");
            }
        }

        private char formatFieldFirstCharacterToFitDroolsCoreStandards(final String fieldName) {
            if (fieldName.length() > 1 && Character.isLowerCase(fieldName.charAt(0)) && Character.isUpperCase(fieldName.charAt(1))) {
                return fieldName.charAt(0);
            } else {
                return Character.toUpperCase(fieldName.charAt(0));
            }
        }

        private boolean doesPeerHaveOutput(final RHSGeneratorContext gctx) {
            final List<RHSGeneratorContext> peers = generatorContextFactory.getPeers(gctx);
            for (RHSGeneratorContext c : peers) {
                if (c.isHasOutput()) {
                    return true;
                }
            }
            return false;
        }

        protected void buildFormulaFieldValue(final ActionFieldValue fieldValue,
                                              final StringBuilder buf) {
            buf.append(fieldValue.getValue());
        }

        protected void buildVariableFieldValue(final ActionFieldValue fieldValue,
                                               final StringBuilder buf) {
            buf.append(fieldValue.getValue().substring(1));
        }

        protected void buildTemplateFieldValue(final ActionFieldValue fieldValue,
                                               final StringBuilder buf) {
            constraintValueBuilder.buildRHSFieldValue(buf,
                                                      fieldValue.getType(),
                                                      "@{removeDelimitingQuotes(" + fieldValue.getValue() + ")}");
        }

        protected void buildWorkItemFieldValue(final ActionWorkItemFieldValue afv,
                                               final StringBuilder buf) {
            if (instantiatedWorkItems.contains(afv.getWorkItemName())) {
                buf.append("(");
                buf.append(afv.getWorkItemParameterClassName());
                buf.append(") ");
                buf.append(WORKITEM_PREFIX);
                buf.append(afv.getWorkItemName());
                buf.append(".getResult( \"");
                buf.append(afv.getWorkItemParameterName());
                buf.append("\" )");
            } else {
                buf.append("null");
            }
        }

        protected void buildDefaultFieldValue(final ActionFieldValue fieldValue,
                                              final StringBuilder buf) {
            constraintValueBuilder.buildRHSFieldValue(buf,
                                                      fieldValue.getType(),
                                                      fieldValue.getValue());
        }

        private void generateActionCallMethod(final ActionCallMethod action,
                                              final FieldNature[] fieldValues) {
            buf.append(indentation);
            if (isDSLEnhanced) {
                buf.append(">");
            }
            buf.append(action.getVariable());
            buf.append(".");

            buf.append(action.getMethodName());

            buf.append("( ");
            boolean isFirst = true;
            for (int i = 0; i < fieldValues.length; i++) {
                ActionFieldFunction valueFunction = (ActionFieldFunction) fieldValues[i];
                if (isFirst == true) {
                    isFirst = false;
                } else {
                    buf.append(", ");
                }

                if (valueFunction.isFormula()) {
                    buf.append(valueFunction.getValue());
                } else if (valueFunction.getNature() == FieldNatureType.TYPE_VARIABLE) {
                    buf.append(valueFunction.getValue());
                } else if (valueFunction.getNature() == FieldNatureType.TYPE_TEMPLATE) {
                    buf.append("@{");
                    buf.append(valueFunction.getValue());
                    buf.append("}");
                } else {
                    buildDefaultFieldValue(valueFunction,
                                           buf);
                }
            }
            buf.append(" );\n");
        }
    }

    public static class RHSClassDependencyVisitor {

        private Map<String, List<ActionFieldValue>> classes = new HashMap<>();

        public void visit(final IAction iAction) {
            if (iAction instanceof ActionFieldList) {
                visit((ActionFieldList) iAction);
            }
        }

        private void visit(final ActionFieldList actionFieldList) {
            addClasses(actionFieldList.getFieldValues());
        }

        public Map<String, List<ActionFieldValue>> getRHSClasses() {
            return classes;
        }

        private void addClasses(ActionFieldValue[] fieldValues) {
            for (ActionFieldValue afv : fieldValues) {
                String type = afv.getType();
                List<ActionFieldValue> afvs = classes.get(type);
                if (afvs == null) {
                    afvs = new ArrayList<ActionFieldValue>();
                    classes.put(type,
                                afvs);
                }
                afvs.add(afv);
            }
        }
    }

    /**
     * @see RuleModelPersistence#unmarshal(String, List, PackageDataModelOracle)
     */
    @Override
    public RuleModel unmarshal(final String str,
                               final List<String> globals,
                               final PackageDataModelOracle dmo) {

        return unmarshal(str,
                         globals,
                         dmo,
                         Collections.emptyList());
    }

    @Override
    public RuleModel unmarshal(final String str,
                               final List<String> globals,
                               final PackageDataModelOracle dmo,
                               final boolean splitEvals) {
        return unmarshal(str,
                         globals,
                         dmo,
                         Collections.emptyList(),
                         splitEvals);
    }

    @Override
    public RuleModel unmarshal(final String str,
                               final List<String> globals,
                               final PackageDataModelOracle dmo,
                               final Collection<RuleModelIActionPersistenceExtension> extensions) {
        return unmarshal(str,
                         globals,
                         dmo,
                         extensions,
                         false);
    }

    public RuleModel unmarshal(final String str,
                               final List<String> globals,
                               final PackageDataModelOracle dmo,
                               final Collection<RuleModelIActionPersistenceExtension> extensions,
                               final boolean splitEvals) {
        PortablePreconditions.checkNotNull("extensions",
                                           extensions);

        if (str == null || str.isEmpty()) {
            return new RuleModel();
        }
        try {
            return getRuleModel(preprocessDRL(str,
                                              false).registerGlobals(dmo,
                                                                     globals),
                                dmo,
                                extensions,
                                splitEvals);
        } catch (RuleModelDRLPersistenceException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.info("Unable to parse source Drl. Falling back to simple parser. \n" + str);
            return getSimpleRuleModel(str);
        }
    }

    public RuleModel unmarshalUsingDSL(final String str,
                                       final List<String> globals,
                                       final PackageDataModelOracle dmo,
                                       final String... dsls) {
        return unmarshalUsingDSL(str,
                                 globals,
                                 dmo,
                                 Collections.emptyList(),
                                 dsls);
    }

    @Override
    public RuleModel unmarshalUsingDSL(String str,
                                       List<String> globals,
                                       PackageDataModelOracle dmo,
                                       Collection<RuleModelIActionPersistenceExtension> extensions,
                                       String... dsls) {
        if (str == null || str.isEmpty()) {
            return new RuleModel();
        }
        try {
            return getRuleModel(parseDSLs(preprocessDRL(str,
                                                        true),
                                          dsls).registerGlobals(dmo,
                                                                globals),
                                dmo,
                                extensions,
                                false);
        } catch (RuleModelDRLPersistenceException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.info("Unable to parse source Drl. Falling back to simple parser. \n" + str);
            return getSimpleRuleModel(str);
        }
    }

    private ExpandedDRLInfo parseDSLs(final ExpandedDRLInfo expandedDRLInfo,
                                      final String[] dsls) {
        for (String dsl : dsls) {
            for (String line : dsl.split("\n")) {
                String dslPattern = line.trim();
                if (dslPattern.length() > 0) {
                    if (dslPattern.startsWith("[when]")) {
                        final String dslDefinition = dslPattern.substring("[when]".length());
                        expandedDRLInfo.lhsDslPatterns.add(new SimpleDSLSentence(extractDslPattern(dslDefinition),
                                                                                 extractDslDrl(dslDefinition)));
                    } else if (dslPattern.startsWith("[then]")) {
                        final String dslDefinition = dslPattern.substring("[then]".length());
                        expandedDRLInfo.rhsDslPatterns.add(new SimpleDSLSentence(extractDslPattern(dslDefinition),
                                                                                 extractDslDrl(dslDefinition)));
                    } else if (dslPattern.startsWith("[")) {
                        final String dslDefinition = removeDslTopics(dslPattern);
                        expandedDRLInfo.lhsDslPatterns.add(new SimpleDSLSentence(extractDslPattern(dslDefinition),
                                                                                 extractDslDrl(dslDefinition)));
                        expandedDRLInfo.rhsDslPatterns.add(new SimpleDSLSentence(extractDslPattern(dslDefinition),
                                                                                 extractDslDrl(dslDefinition)));
                    }
                }
            }
        }
        return expandedDRLInfo;
    }

    private String removeDslTopics(final String line) {
        int lastClosedSquare = -1;
        boolean lookForOpen = true;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (lookForOpen) {
                if (ch == '[') {
                    lookForOpen = false;
                } else if (!Character.isWhitespace(ch)) {
                    break;
                }
            } else {
                if (ch == ']') {
                    lastClosedSquare = i;
                    lookForOpen = true;
                }
            }
        }
        return line.substring(lastClosedSquare + 1);
    }

    private String extractDslPattern(final String line) {
        return line.substring(0,
                              line.indexOf('=')).trim();
    }

    private String extractDslDrl(final String line) {
        return line.substring(line.indexOf('=') + 1).trim();
    }

    private RuleModel getRuleModel(final ExpandedDRLInfo expandedDRLInfo,
                                   final PackageDataModelOracle dmo,
                                   final Collection<RuleModelIActionPersistenceExtension> extensions,
                                   final boolean splitEvals) throws RuleModelDRLPersistenceException {
        //De-serialize model
        RuleDescr ruleDescr = parseDrl(expandedDRLInfo);
        RuleModel model = new RuleModel();
        model.name = ruleDescr.getName();
        model.parentName = ruleDescr.getParentName();

        for (AnnotationDescr annotation : ruleDescr.getAnnotations()) {
            model.addMetadata(new RuleMetadata(annotation.getName(),
                                               annotation.getValuesAsString()));
        }

        //De-serialize Package name
        final String packageName = PackageNameParser.parsePackageName(expandedDRLInfo.plainDrl);
        model.setPackageName(packageName);

        //De-serialize imports
        final Imports imports = ImportsParser.parseImports(expandedDRLInfo.plainDrl);
        for (Import item : imports.getImports()) {
            model.getImports().addImport(item);
        }

        boolean isJavaDialect = parseAttributes(model,
                                                ruleDescr.getAttributes());
        Map<String, String> boundParams = parseLhs(model,
                                                   ruleDescr.getLhs(),
                                                   isJavaDialect,
                                                   expandedDRLInfo,
                                                   dmo);
        parseRhs(model,
                 expandedDRLInfo.consequence != null ? expandedDRLInfo.consequence : (String) ruleDescr.getConsequence(),
                 isJavaDialect,
                 boundParams,
                 expandedDRLInfo,
                 dmo,
                 extensions,
                 splitEvals);
        return model;
    }

    private ExpandedDRLInfo preprocessDRL(final String str,
                                          final boolean hasDsl) {
        StringBuilder drl = new StringBuilder();
        String thenLine = "";
        List<String> lhsStatements = new ArrayList<String>();
        List<String> rhsStatements = new ArrayList<String>();

        String[] lines = str.split("\n");
        RuleSection ruleSection = RuleSection.HEADER;
        int lhsParenthesisBalance = 0;

        for (String line : lines) {
            if (line.trim().length() == 0) {
                continue;
            }
            if (ruleSection == RuleSection.HEADER) {
                drl.append(line).append("\n");
                if (isLHSStartMarker(line)) {
                    ruleSection = RuleSection.LHS;
                }
                continue;
            }
            if (ruleSection == RuleSection.LHS && isRHSStartMarker(line)) {
                thenLine = line;
                ruleSection = RuleSection.RHS;
                continue;
            }
            if (ruleSection == RuleSection.LHS) {
                if (lhsParenthesisBalance == 0) {
                    lhsStatements.add(line);
                } else {
                    String newLine = line.trim();
                    final String oldLine = lhsStatements.remove(lhsStatements.size() - 1);

                    if (hasDsl && newLine.startsWith(">")) {
                        newLine = newLine.substring(1);
                    }

                    lhsStatements.add(oldLine + " " + newLine);
                }
                lhsParenthesisBalance += parenthesisBalance(line);
            } else {
                rhsStatements.add(line);
            }
        }

        return createExpandedDRLInfo(hasDsl,
                                     drl,
                                     thenLine,
                                     lhsStatements,
                                     rhsStatements);
    }

    private boolean isLHSStartMarker(final String line) {
        final String lhsMarker = line.trim() + " ";
        return lhsMarker.startsWith("when ");
    }

    private boolean isRHSStartMarker(final String line) {
        final String rhsMarker = line.trim() + " ";
        return rhsMarker.startsWith("then ");
    }

    private int parenthesisBalance(String str) {
        int balance = 0;
        for (char ch : str.toCharArray()) {
            if (ch == '(') {
                balance++;
            } else if (ch == ')') {
                balance--;
            }
        }
        return balance;
    }

    private ExpandedDRLInfo createExpandedDRLInfo(final boolean hasDsl,
                                                  final StringBuilder drl,
                                                  final String thenLine,
                                                  final List<String> lhsStatements,
                                                  final List<String> rhsStatements) {
        if (!hasDsl) {
            return processFreeFormStatement(drl,
                                            thenLine,
                                            lhsStatements,
                                            rhsStatements);
        }

        ExpandedDRLInfo expandedDRLInfo = new ExpandedDRLInfo(hasDsl);
        int lineCounter = -1;
        for (String statement : lhsStatements) {
            lineCounter++;
            String trimmed = statement.trim();
            if (hasDsl && trimmed.startsWith(">")) {
                if (isValidLHSStatement(trimmed)) {
                    drl.append(trimmed.substring(1)).append("\n");
                } else {
                    expandedDRLInfo.freeFormStatementsInLhs.put(lineCounter,
                                                                trimmed.substring(1));
                }
            } else {
                expandedDRLInfo.dslStatementsInLhs.put(lineCounter,
                                                       trimmed);
            }
        }

        drl.append(thenLine).append("\n");

        lineCounter = -1;
        for (String statement : rhsStatements) {
            lineCounter++;
            String trimmed = statement.trim();
            if (trimmed.endsWith("end")) {
                trimmed = trimmed.substring(0,
                                            trimmed.length() - 3).trim();
            }
            if (trimmed.length() > 0) {
                if (hasDsl && trimmed.startsWith(">")) {
                    drl.append(trimmed.substring(1)).append("\n");
                } else {
                    expandedDRLInfo.dslStatementsInRhs.put(lineCounter,
                                                           trimmed);
                }
            } else {
                drl.append("end");
            }
        }

        expandedDRLInfo.plainDrl = drl.toString();

        return expandedDRLInfo;
    }

    private ExpandedDRLInfo processFreeFormStatement(final StringBuilder drl,
                                                     final String thenLine,
                                                     final List<String> lhsStatements,
                                                     final List<String> rhsStatements) {
        ExpandedDRLInfo expandedDRLInfo = new ExpandedDRLInfo(false);

        int lineCounter = -1;
        for (String statement : lhsStatements) {
            lineCounter++;
            String trimmed = statement.trim();
            if (isValidLHSStatement(trimmed)) {
                drl.append(trimmed).append("\n");
            } else {
                expandedDRLInfo.freeFormStatementsInLhs.put(lineCounter,
                                                            trimmed);
            }
        }

        drl.append(thenLine).append("\n");

        expandedDRLInfo.consequence = "";
        for (String statement : rhsStatements) {
            String trimmed = statement.trim();
            if (trimmed.endsWith("end")) {
                trimmed = trimmed.substring(0,
                                            trimmed.length() - 3).trim();
            }
            if (trimmed.length() > 0) {
                expandedDRLInfo.consequence += (trimmed + "\n");
            }
            drl.append(statement).append("\n");
        }

        expandedDRLInfo.plainDrl = drl.toString();
        return expandedDRLInfo;
    }

    private boolean isValidLHSStatement(final String lhs) {
        // TODO: How to identify a non valid (free form) lhs statement?
        return (lhs.indexOf('(') >= 0 || lhs.indexOf(':') >= 0) && !containsComment(lhs);
    }

    private boolean containsComment(final String lhs) {
        //Check if there are any comments not inside Strings. Comments are ignored by the DrlParser
        //and hence lines containing comments will be "lost" unless we handle such lines as
        //FreeFormLines when unmarshalling DRL.
        String _lhs = lhs;
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(lhs);
        while (matcher.find()) {
            final String text = matcher.group(1);
            final int index = _lhs.indexOf(text);
            _lhs = _lhs.substring(0,
                                  index) + _lhs.substring(index + text.length());
        }
        return _lhs.indexOf("//") > -1;
    }

    private enum RuleSection {
        HEADER,
        LHS,
        RHS
    }

    private static class ExpandedDRLInfo {

        private final boolean hasDsl;
        private String plainDrl;
        private String consequence;

        private Map<Integer, String> dslStatementsInLhs;
        private Map<Integer, String> dslStatementsInRhs;

        private Map<Integer, String> freeFormStatementsInLhs;

        private List<SimpleDSLSentence> lhsDslPatterns;
        private List<SimpleDSLSentence> rhsDslPatterns;

        private Set<String> globals = new HashSet<String>();

        private ExpandedDRLInfo(final boolean hasDsl) {
            this.hasDsl = hasDsl;
            dslStatementsInLhs = new HashMap<Integer, String>();
            dslStatementsInRhs = new HashMap<Integer, String>();
            freeFormStatementsInLhs = new HashMap<Integer, String>();
            lhsDslPatterns = new ArrayList<SimpleDSLSentence>();
            rhsDslPatterns = new ArrayList<SimpleDSLSentence>();
        }

        public boolean hasGlobal(final String name) {
            return globals.contains(name);
        }

        public ExpandedDRLInfo registerGlobals(final PackageDataModelOracle dmo,
                                               final List<String> globalStatements) {
            if (globalStatements != null) {
                for (String globalStatement : globalStatements) {
                    String identifier = getIdentifier(globalStatement);
                    if (identifier != null) {
                        globals.add(identifier);
                    }
                }
            }
            Map<String, String> globalsFromDmo = dmo != null ? dmo.getPackageGlobals() : null;
            if (globalsFromDmo != null) {
                globals.addAll(globalsFromDmo.keySet());
            }
            return this;
        }

        private String getIdentifier(String globalStatement) {
            globalStatement = globalStatement.trim();
            if (!globalStatement.startsWith("global")) {
                return null;
            }
            int lastSpace = globalStatement.lastIndexOf(' ');
            if (lastSpace < 0) {
                return null;
            }
            String identifier = globalStatement.substring(lastSpace + 1);
            if (identifier.endsWith(";")) {
                identifier = identifier.substring(0,
                                                  identifier.length() - 1);
            }
            return identifier;
        }

        public ExpandedDRLInfo registerGlobalDescrs(final List<GlobalDescr> globalDescrs) {
            if (globalDescrs != null) {
                for (GlobalDescr globalDescr : globalDescrs) {
                    globals.add(globalDescr.getIdentifier());
                }
            }
            return this;
        }
    }

    private RuleDescr parseDrl(final ExpandedDRLInfo expandedDRLInfo) {
        DrlParser drlParser = new DrlParser();
        PackageDescr packageDescr;
        try {
            packageDescr = drlParser.parse(true,
                                           expandedDRLInfo.plainDrl);
            if (drlParser.hasErrors()) {
                throw new RuleModelUnmarshallingException();
            }
        } catch (DroolsParserException e) {
            throw new RuntimeException(e);
        }
        expandedDRLInfo.registerGlobalDescrs(packageDescr.getGlobals());
        return packageDescr.getRules().get(0);
    }

    private RuleDescr parseDrl(final String drl) throws DroolsParserException {
        final DrlParser drlParser = new DrlParser();
        final PackageDescr packageDescr = drlParser.parse(true, drl);
        if (drlParser.hasErrors()) {
            throw new RuleModelUnmarshallingException();
        }

        return packageDescr.getRules().get(0);
    }

    private boolean parseAttributes(final RuleModel m,
                                    final Map<String, AttributeDescr> attributes) {
        boolean isJavaDialect = false;
        for (Map.Entry<String, AttributeDescr> entry : attributes.entrySet()) {
            String name = entry.getKey();
            String value = normalizeAttributeValue(entry.getValue().getValue().trim());
            RuleAttribute ruleAttribute = new RuleAttribute(name,
                                                            value);
            m.addAttribute(ruleAttribute);
            isJavaDialect |= name.equals("dialect") && value.equals("java");
        }
        return isJavaDialect;
    }

    private String normalizeAttributeValue(String value) {
        if (value.startsWith("[")) {
            value = value.substring(1,
                                    value.length() - 1).trim();
        }
        if (value.startsWith("\"")) {
            StringBuilder sb = new StringBuilder();
            String[] split = value.split(",");
            sb.append(stripQuotes(split[0].trim()));
            for (int i = 1; i < split.length; i++) {
                sb.append(", ").append(stripQuotes(split[i].trim()));
            }
            value = sb.toString();
        }
        return value;
    }

    private String stripQuotes(String value) {
        if (value.startsWith("\"")) {
            value = value.substring(1,
                                    value.length() - 1).trim();
        }
        return value;
    }

    private Map<String, String> parseLhs(final RuleModel m,
                                         final AndDescr lhs,
                                         final boolean isJavaDialect,
                                         final ExpandedDRLInfo expandedDRLInfo,
                                         final PackageDataModelOracle dmo) {
        Map<String, String> boundParams = new HashMap<String, String>();
        int lineCounter = -1;
        for (BaseDescr descr : lhs.getDescrs()) {
            lineCounter = parseNonDrlInLhs(m,
                                           expandedDRLInfo,
                                           lineCounter);
            IPattern pattern = parseBaseDescr(m,
                                              descr,
                                              isJavaDialect,
                                              boundParams,
                                              dmo);
            if (pattern != null) {
                m.addLhsItem(pattern);
            }
        }
        parseNonDrlInLhs(m,
                         expandedDRLInfo,
                         lineCounter);
        return boundParams;
    }

    private int parseNonDrlInLhs(final RuleModel m,
                                 final ExpandedDRLInfo expandedDRLInfo,
                                 int lineCounter) {
        lineCounter++;
        lineCounter = parseDslInLhs(m,
                                    expandedDRLInfo,
                                    lineCounter);
        lineCounter = parseFreeForm(m,
                                    expandedDRLInfo,
                                    lineCounter);
        return lineCounter;
    }

    private int parseDslInLhs(final RuleModel m,
                              final ExpandedDRLInfo expandedDRLInfo,
                              int lineCounter) {
        if (expandedDRLInfo.hasDsl) {
            String dslLine = expandedDRLInfo.dslStatementsInLhs.get(lineCounter);
            while (dslLine != null) {
                m.addLhsItem(toDSLSentence(expandedDRLInfo.lhsDslPatterns,
                                           dslLine));
                dslLine = expandedDRLInfo.dslStatementsInLhs.get(++lineCounter);
            }
        }
        return lineCounter;
    }

    private int parseFreeForm(final RuleModel m,
                              final ExpandedDRLInfo expandedDRLInfo,
                              int lineCounter) {
        String freeForm = expandedDRLInfo.freeFormStatementsInLhs.get(lineCounter);
        while (freeForm != null) {
            FreeFormLine ffl = new FreeFormLine();
            ffl.setText(freeForm);
            m.addLhsItem(ffl);
            freeForm = expandedDRLInfo.freeFormStatementsInLhs.get(++lineCounter);
        }
        return lineCounter;
    }

    private IPattern parseBaseDescr(final RuleModel m,
                                    final BaseDescr descr,
                                    final boolean isJavaDialect,
                                    final Map<String, String> boundParams,
                                    final PackageDataModelOracle dmo) {
        if (descr instanceof PatternDescr) {
            return parsePatternDescr(m,
                                     (PatternDescr) descr,
                                     isJavaDialect,
                                     boundParams,
                                     dmo);
        } else if (descr instanceof AndDescr) {
            AndDescr andDescr = (AndDescr) descr;
            return parseBaseDescr(m,
                                  andDescr.getDescrs().get(0),
                                  isJavaDialect,
                                  boundParams,
                                  dmo);
        } else if (descr instanceof EvalDescr) {
            FreeFormLine freeFormLine = new FreeFormLine();
            freeFormLine.setText("eval( " + ((EvalDescr) descr).getContent() + " )");
            return freeFormLine;
        } else if (descr instanceof ConditionalElementDescr) {
            return parseExistentialElementDescr(m,
                                                (ConditionalElementDescr) descr,
                                                isJavaDialect,
                                                boundParams,
                                                dmo);
        }
        return null;
    }

    private IFactPattern parsePatternDescr(final RuleModel m,
                                           final PatternDescr pattern,
                                           final boolean isJavaDialect,
                                           final Map<String, String> boundParams,
                                           final PackageDataModelOracle dmo) {
        if (pattern.getSource() != null) {
            return parsePatternSource(m,
                                      pattern,
                                      pattern.getSource(),
                                      isJavaDialect,
                                      boundParams,
                                      dmo);
        }
        return getFactPattern(m,
                              pattern,
                              isJavaDialect,
                              boundParams,
                              dmo);
    }

    private FactPattern getFactPattern(final RuleModel m,
                                       final PatternDescr pattern,
                                       final boolean isJavaDialect,
                                       final Map<String, String> boundParams,
                                       final PackageDataModelOracle dmo) {
        String type = pattern.getObjectType();
        FactPattern factPattern = new FactPattern(getSimpleFactType(type,
                                                                    dmo));
        if (pattern.getIdentifier() != null) {
            String identifier = pattern.getIdentifier();
            factPattern.setBoundName(identifier);
            boundParams.put(identifier,
                            type);
        }

        parseConstraint(m,
                        factPattern,
                        pattern.getConstraint(),
                        isJavaDialect,
                        boundParams,
                        dmo);

        for (BehaviorDescr behavior : pattern.getBehaviors()) {
            if (behavior.getText().equals("window")) {
                CEPWindow window = new CEPWindow();
                window.setOperator("over window:" + behavior.getSubType());
                window.setParameter("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator",
                                    "org.drools.workbench.models.commons.backend.rule.CEPWindowOperatorParameterDRLBuilder");
                List<String> params = behavior.getParameters();
                if (params != null) {
                    int i = 1;
                    for (String param : params) {
                        window.setParameter("" + i++,
                                            param);
                    }
                }
                factPattern.setWindow(window);
            }
        }
        return factPattern;
    }

    private IFactPattern parsePatternSource(final RuleModel m,
                                            final PatternDescr pattern,
                                            final PatternSourceDescr patternSource,
                                            final boolean isJavaDialect,
                                            final Map<String, String> boundParams,
                                            final PackageDataModelOracle dmo) {
        if (pattern.getIdentifier() != null) {
            boundParams.put(pattern.getIdentifier(),
                            pattern.getObjectType());
        }
        if (patternSource instanceof AccumulateDescr) {
            AccumulateDescr accumulate = (AccumulateDescr) patternSource;
            FromAccumulateCompositeFactPattern fac = new FromAccumulateCompositeFactPattern();
            fac.setSourcePattern(parseBaseDescr(m,
                                                accumulate.getInput(),
                                                isJavaDialect,
                                                boundParams,
                                                dmo));
            fac.setInitCode(accumulate.getInitCode());
            fac.setActionCode(accumulate.getActionCode());
            fac.setReverseCode(accumulate.getReverseCode());
            fac.setResultCode(accumulate.getResultCode());

            FactPattern factPattern = new FactPattern(pattern.getObjectType());
            factPattern.setBoundName(pattern.getIdentifier());

            parseConstraint(m,
                            factPattern,
                            pattern.getConstraint(),
                            isJavaDialect,
                            boundParams,
                            dmo);

            fac.setFactPattern(factPattern);
            for (AccumulateDescr.AccumulateFunctionCallDescr func : accumulate.getFunctions()) {
                String funcName = func.getFunction();
                boolean first = true;
                StringBuilder sb = new StringBuilder();
                for (String param : func.getParams()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(param);
                }
                fac.setFunction(funcName + "(" + sb + ")");
            }
            return fac;
        } else if (patternSource instanceof CollectDescr) {
            CollectDescr collect = (CollectDescr) patternSource;
            FromCollectCompositeFactPattern fac = new FromCollectCompositeFactPattern();
            fac.setRightPattern(parseBaseDescr(m,
                                               collect.getInputPattern(),
                                               isJavaDialect,
                                               boundParams,
                                               dmo));
            fac.setFactPattern(getFactPattern(m,
                                              pattern,
                                              isJavaDialect,
                                              boundParams,
                                              dmo));
            return fac;
        } else if (patternSource instanceof EntryPointDescr) {
            EntryPointDescr entryPoint = (EntryPointDescr) patternSource;
            FromEntryPointFactPattern fep = new FromEntryPointFactPattern();
            fep.setEntryPointName(entryPoint.getText());
            fep.setFactPattern(getFactPattern(m,
                                              pattern,
                                              isJavaDialect,
                                              boundParams,
                                              dmo));
            return fep;
        } else if (patternSource instanceof FromDescr) {
            FromDescr from = (FromDescr) patternSource;
            FromCompositeFactPattern fcfp = new FromCompositeFactPattern();
            FactPattern factPattern = new FactPattern(pattern.getObjectType());
            factPattern.setBoundName(pattern.getIdentifier());
            parseConstraint(m,
                            factPattern,
                            pattern.getConstraint(),
                            isJavaDialect,
                            boundParams,
                            dmo);

            fcfp.setFactPattern(factPattern);
            ExpressionFormLine expression = new ExpressionFormLine();
            fcfp.setExpression(expression);

            String dataSource = from.getDataSource().toString();
            String[] splitSource = dataSource.split("\\.");
            ModelField[] fields = null;
            for (int i = 0; i < splitSource.length; i++) {
                String sourcePart = splitSource[i];
                if (i == 0) {
                    String type = boundParams.get(sourcePart);
                    expression.appendPart(new ExpressionVariable(sourcePart,
                                                                 type,
                                                                 DataType.TYPE_NUMERIC));
                    fields = findFields(m,
                                        dmo,
                                        type);
                } else {
                    ModelField modelField = null;
                    if (fields != null) {
                        for (ModelField field : fields) {
                            if (field.getName().equals(sourcePart)) {
                                modelField = field;
                                break;
                            }
                        }
                    }
                    if (modelField == null) {
                        final String previousClassName = expression.getClassType();
                        final List<MethodInfo> mis = dmo.getModuleMethodInformation().get(previousClassName);
                        boolean isMethod = false;
                        if (mis != null) {
                            for (MethodInfo mi : mis) {
                                if (mi.getName().equals(sourcePart)) {
                                    expression.appendPart(new ExpressionMethod(mi.getName(),
                                                                               mi.getReturnClassType(),
                                                                               mi.getGenericType(),
                                                                               mi.getParametricReturnType()));
                                    isMethod = true;
                                    break;
                                }
                            }
                        }
                        if (isMethod == false) {
                            expression.appendPart(new ExpressionText(sourcePart));
                        }
                    } else {
                        expression.appendPart(new ExpressionField(sourcePart,
                                                                  modelField.getClassName(),
                                                                  modelField.getType()));
                        fields = findFields(m,
                                            dmo,
                                            modelField.getClassName());
                    }
                }
            }

            return fcfp;
        }
        throw new RuntimeException("Unknown pattern source " + patternSource);
    }

    private CompositeFactPattern parseExistentialElementDescr(final RuleModel m,
                                                              final ConditionalElementDescr conditionalDescr,
                                                              final boolean isJavaDialect,
                                                              final Map<String, String> boundParams,
                                                              final PackageDataModelOracle dmo) {
        if (conditionalDescr.getDescrs().stream().anyMatch(d -> d instanceof ConditionalElementDescr)) {
            throw new RuleModelUnmarshallingException();
        }

        CompositeFactPattern comp;
        if (conditionalDescr instanceof NotDescr) {
            comp = new CompositeFactPattern(CompositeFactPattern.COMPOSITE_TYPE_NOT);
        } else if (conditionalDescr instanceof OrDescr) {
            comp = new CompositeFactPattern(CompositeFactPattern.COMPOSITE_TYPE_OR);
        } else if (conditionalDescr instanceof ExistsDescr) {
            comp = new CompositeFactPattern(CompositeFactPattern.COMPOSITE_TYPE_EXISTS);
        } else {
            throw new IllegalArgumentException("Unknown conditional descr type: " + conditionalDescr);
        }

        addPatternToComposite(m,
                              conditionalDescr,
                              comp,
                              isJavaDialect,
                              boundParams,
                              dmo);
        IFactPattern[] patterns = comp.getPatterns();
        return patterns != null && patterns.length > 0 ? comp : null;
    }

    private void addPatternToComposite(final RuleModel m,
                                       final ConditionalElementDescr conditionalDescr,
                                       final CompositeFactPattern comp,
                                       final boolean isJavaDialect,
                                       final Map<String, String> boundParams,
                                       final PackageDataModelOracle dmo) {
        for (Object descr : conditionalDescr.getDescrs()) {
            if (descr instanceof PatternDescr) {
                comp.addFactPattern(parsePatternDescr(m,
                                                      (PatternDescr) descr,
                                                      isJavaDialect,
                                                      boundParams,
                                                      dmo));
            } else if (descr instanceof ConditionalElementDescr) {
                addPatternToComposite(m,
                                      (ConditionalElementDescr) descr,
                                      comp,
                                      isJavaDialect,
                                      boundParams,
                                      dmo);
            }
        }
    }

    private void parseConstraint(final RuleModel m,
                                 final FactPattern factPattern,
                                 final ConditionalElementDescr constraint,
                                 final boolean isJavaDialect,
                                 final Map<String, String> boundParams,
                                 final PackageDataModelOracle dmo) {
        for (BaseDescr descr : constraint.getDescrs()) {
            if (descr instanceof ExprConstraintDescr) {
                ExprConstraintDescr exprConstraint = (ExprConstraintDescr) descr;
                Expr expr = parseExpr(exprConstraint.getExpression(),
                                      isJavaDialect,
                                      boundParams,
                                      dmo);
                factPattern.addConstraint(expr.asFieldConstraint(m,
                                                                 factPattern));
            }
        }
    }

    private static String findOperator(String expr) {
        //ConnectiveConstraints are handled SimpleExpr.setOperatorAndValueOnConstraint(). Therefore we
        //only need to try to find the first operator before the ConnectiveConstraint separator.
        if (expr.contains("&&")) {
            expr = expr.substring(0,
                                  expr.indexOf("&&")).trim();
        }
        if (expr.contains("||")) {
            expr = expr.substring(0,
                                  expr.indexOf("||")).trim();
        }

        final Set<String> potentialOperators = new HashSet<String>();
        for (Operator op : Operator.getAllOperators()) {
            String opString = op.getOperatorString();
            if (op.isNegated()) {
                if (expr.contains(" not " + opString)) {
                    return "not " + opString;
                }
            }
            int opPos = expr.indexOf(opString);
            if (opPos >= 0 &&
                    isNotMethodName(expr, opString, opPos) &&
                    !isInQuote(expr, opPos) &&
                    !(Character.isLetter(opString.charAt(0)) &&
                            (expr.length() == opPos + opString.length() || Character.isLetter(expr.charAt(opPos + opString.length())) ||
                                    (opPos > 0 && Character.isLetter(expr.charAt(opPos - 1)))))) {
                potentialOperators.add(opString);
            }
        }
        String operator = "";
        if (!potentialOperators.isEmpty()) {
            for (String potentialOperator : potentialOperators) {
                if (potentialOperator.length() > operator.length()) {
                    operator = potentialOperator;
                }
            }
        }
        if (!operator.isEmpty()) {
            return operator;
        }

        if (expr.contains(" not in ")) {
            return " not in ";
        }
        if (expr.contains(" in ")) {
            return " in ";
        }
        return null;
    }

    private static boolean isNotMethodName(final String expression,
                                           final String potentialOperator,
                                           final int operatorPosition) {
        if (Objects.equals(potentialOperator, Operator.EQUAL.getOperatorString()) ||
                Objects.equals(potentialOperator, Operator.NOT_EQUAL.getOperatorString()) ||
                Objects.equals(potentialOperator, Operator.LESS.getOperatorString()) ||
                Objects.equals(potentialOperator, Operator.LESS_OR_EQUAL.getOperatorString()) ||
                Objects.equals(potentialOperator, Operator.GREATER.getOperatorString()) ||
                Objects.equals(potentialOperator, Operator.GREATER_OR_EQUAL.getOperatorString())) {
            return true;
        }
        return operatorPosition == 0 || expression.charAt(operatorPosition - 1) == ' ';
    }

    private static boolean isInQuote(final String expr,
                                     final int pos) {
        boolean isInQuote = false;
        for (int i = pos - 1; i >= 0; i--) {
            if (expr.charAt(i) == '"') {
                isInQuote = !isInQuote;
            }
        }
        return isInQuote;
    }

    private static final String[] NULL_OPERATORS = new String[]{"== null", "!= null"};

    private static String findNullOrNotNullOperator(final String expr) {
        for (String op : NULL_OPERATORS) {
            if (expr.contains(op)) {
                return op;
            }
        }
        return null;
    }

    private void parseRhs(final RuleModel m,
                          final String rhs,
                          final boolean isJavaDialect,
                          final Map<String, String> boundParams,
                          final ExpandedDRLInfo expandedDRLInfo,
                          final PackageDataModelOracle dmo,
                          final Collection<RuleModelIActionPersistenceExtension> extensions,
                          final boolean splitEvals) throws RuleModelDRLPersistenceException {
        PortableWorkDefinition pwd = null;
        Map<String, List<String>> setStatements = new HashMap<String, List<String>>();
        Map<String, Integer> setStatementsPosition = new HashMap<String, Integer>();
        Map<String, String> factsType = new HashMap<String, String>();

        String modifiedVariable = null;
        String modifiers = null;

        int lineCounter = -1;
        String[] lines = rhs.split("\n");
        for (String line : lines) {
            lineCounter++;
            line = line.trim();

            List<RuleModelIActionPersistenceExtension> matchingExtensions = getMatchingExtensionsForLine(line,
                                                                                                         extensions);
            if (matchingExtensions.isEmpty()) {
                // Continue with hardcoded parsers
            } else if (matchingExtensions.size() > 1) {
                throw new RuleModelDRLPersistenceException("Ambiguous RuleModelIActionPersistenceExtension implementations (" + matchingExtensions + ") found for line " + line);
            } else {
                unmarshalUsingExtension(m,
                                        matchingExtensions.get(0),
                                        line);
                continue;
            }

            if (expandedDRLInfo.hasDsl) {
                String dslLine = expandedDRLInfo.dslStatementsInRhs.get(lineCounter);
                while (dslLine != null) {
                    List<RuleModelIActionPersistenceExtension> matchingExtensionsDslLine = getMatchingExtensionsForLine(dslLine,
                                                                                                                        extensions);
                    if (matchingExtensionsDslLine.isEmpty()) {
                        m.addRhsItem(toDSLSentence(expandedDRLInfo.rhsDslPatterns,
                                                   dslLine));
                    } else if (matchingExtensionsDslLine.size() > 1) {
                        throw new RuleModelDRLPersistenceException("Ambiguous RuleModelIActionPersistenceExtension implementations (" + matchingExtensionsDslLine + ") found for line " + line);
                    } else {
                        unmarshalUsingExtension(m,
                                                matchingExtensionsDslLine.get(0),
                                                dslLine);
                    }

                    dslLine = expandedDRLInfo.dslStatementsInRhs.get(++lineCounter);
                }
            }

            if (modifiedVariable != null) {
                int modifyBlockEnd = line.lastIndexOf('}');
                if (modifiers == null) {
                    modifiers = modifyBlockEnd > 0 ?
                            line.substring(line.indexOf('{') + 1,
                                           modifyBlockEnd).trim() :
                            line.substring(line.indexOf('{') + 1).trim();
                } else if (modifyBlockEnd != 0) {
                    modifiers += modifyBlockEnd > 0 ?
                            line.substring(0,
                                           modifyBlockEnd).trim() :
                            line;
                }
                if (modifyBlockEnd >= 0) {
                    ActionUpdateField action = new ActionUpdateField();
                    action.setVariable(modifiedVariable);
                    m.addRhsItem(action);
                    addModifiersToAction(modifiers,
                                         action,
                                         modifiedVariable,
                                         boundParams,
                                         dmo,
                                         m,
                                         isJavaDialect);
                    modifiedVariable = null;
                    modifiers = null;
                }
            } else if (line.startsWith("insertLogical")) {
                String fact = unwrapParenthesis(line);
                String type = getStatementType(fact,
                                               factsType);
                if (type != null) {
                    boundParams.put(fact,
                                    type);
                    ActionInsertLogicalFact action = new ActionInsertLogicalFact(type);
                    m.addRhsItem(action);
                    if (factsType.containsKey(fact)) {
                        action.setBoundName(fact);
                        addSettersToAction(setStatements,
                                           fact,
                                           action,
                                           boundParams,
                                           dmo,
                                           m,
                                           isJavaDialect);
                    }
                }
            } else if (line.startsWith("insert")) {
                String fact = unwrapParenthesis(line);
                String type = getStatementType(fact,
                                               factsType);
                if (type != null) {
                    boundParams.put(fact,
                                    type);
                    ActionInsertFact action = new ActionInsertFact(type);
                    m.addRhsItem(action);
                    if (factsType.containsKey(fact)) {
                        action.setBoundName(fact);
                        addSettersToAction(setStatements,
                                           fact,
                                           action,
                                           boundParams,
                                           dmo,
                                           m,
                                           isJavaDialect);
                    }
                } else {
                    FreeFormLine ffl = new FreeFormLine();
                    ffl.setText(line);
                    m.addRhsItem(ffl);
                }
            } else if (line.startsWith("update")) {
                String variable = unwrapParenthesis(line);
                ActionUpdateField action = new ActionUpdateField();
                action.setVariable(variable);
                m.addRhsItem(action);
                addSettersToAction(setStatements,
                                   variable,
                                   action,
                                   boundParams,
                                   dmo,
                                   m,
                                   isJavaDialect);
            } else if (line.startsWith("modify")) {
                int modifyBlockEnd = line.lastIndexOf('}');
                if (modifyBlockEnd > 0) {
                    String variable = line.substring(line.indexOf('(') + 1,
                                                     line.indexOf(')')).trim();
                    ActionUpdateField action = new ActionUpdateField();
                    action.setVariable(variable);
                    m.addRhsItem(action);
                    addModifiersToAction(line.substring(line.indexOf('{') + 1,
                                                        modifyBlockEnd).trim(),
                                         action,
                                         variable,
                                         boundParams,
                                         dmo,
                                         m,
                                         isJavaDialect);
                } else {
                    modifiedVariable = line.substring(line.indexOf('(') + 1,
                                                      line.indexOf(')')).trim();
                    int modifyBlockStart = line.indexOf('{');
                    if (modifyBlockStart > 0) {
                        modifiers = line.substring(modifyBlockStart + 1).trim();
                    }
                }
            } else if (line.startsWith("retract") || line.startsWith("delete")) {
                String variable = unwrapParenthesis(line);
                m.addRhsItem(new ActionRetractFact(variable));
            } else if (line.startsWith("org.drools.core.process.instance.impl.WorkItemImpl wiWorkItem")) {
                ActionExecuteWorkItem awi = new ActionExecuteWorkItem();
                pwd = new PortableWorkDefinition();
                pwd.setName("WorkItem");
                awi.setWorkDefinition(pwd);
                m.addRhsItem(awi);
            } else if (line.startsWith("wiWorkItem.getParameters().put")) {
                String statement = line.substring("wiWorkItem.getParameters().put".length());
                statement = unwrapParenthesis(statement);
                int commaPos = statement.indexOf(',');
                String name = statement.substring(0,
                                                  commaPos).trim();
                String value = statement.substring(commaPos + 1).trim();
                pwd.addParameter(buildPortableParameterDefinition(name,
                                                                  value,
                                                                  boundParams));
            } else if (line.startsWith("wim.internalExecuteWorkItem") || line.startsWith("wiWorkItem.setName")) {
                // ignore

            } else {
                int dotPos = line.indexOf('.');
                int argStart = line.indexOf('(');
                if (dotPos > 0 && argStart > dotPos) {
                    String variable = line.substring(0,
                                                     dotPos).trim();

                    if (boundParams.containsKey(variable) || factsType.containsKey(variable) || expandedDRLInfo.hasGlobal(variable)) {
                        if (isJavaIdentifier(variable)) {
                            String methodName = line.substring(dotPos + 1,
                                                               argStart).trim();
                            if (isJavaIdentifier(methodName)) {
                                if (getSettedField(m,
                                                   methodName,
                                                   boundParams.get(variable),
                                                   dmo) != null) {
                                    List<String> setters = setStatements.get(variable);
                                    if (setters == null) {
                                        setters = new ArrayList<String>();
                                        setStatements.put(variable,
                                                          setters);
                                    }
                                    if (!setStatementsPosition.containsKey(variable)) {
                                        setStatementsPosition.put(variable,
                                                                  lineCounter);
                                    }
                                    setters.add(line);
                                } else if (methodName.equals("add") && expandedDRLInfo.hasGlobal(variable)) {
                                    String factName = line.substring(argStart + 1,
                                                                     line.lastIndexOf(')')).trim();
                                    ActionGlobalCollectionAdd actionGlobalCollectionAdd = new ActionGlobalCollectionAdd();
                                    actionGlobalCollectionAdd.setGlobalName(variable);
                                    actionGlobalCollectionAdd.setFactName(factName);
                                    m.addRhsItem(actionGlobalCollectionAdd);
                                } else {
                                    m.addRhsItem(getActionCallMethod(m,
                                                                     isJavaDialect,
                                                                     boundParams,
                                                                     dmo,
                                                                     line,
                                                                     variable,
                                                                     methodName));
                                }
                                continue;
                            }
                        }
                    }
                }

                int eqPos = line.indexOf('=');
                boolean addFreeFormLine = line.trim().length() > 0;
                if (eqPos > 0) {
                    String field = line.substring(0,
                                                  eqPos).trim();
                    if ("java.text.SimpleDateFormat sdf".equals(field) ||
                            "java.time.format.DateTimeFormatter dtf".equals(field) ||
                            "org.drools.core.process.instance.WorkItemManager wim".equals(field)) {
                        addFreeFormLine = false;
                    }
                    String[] split = field.split(" ");
                    if (split.length == 2) {
                        factsType.put(split[1],
                                      split[0]);
                        addFreeFormLine &= !isInsertedFact(lines,
                                                           lineCounter,
                                                           split[1]);
                    }
                }
                if (addFreeFormLine) {
                    FreeFormLine ffl = new FreeFormLine();
                    ffl.setText(line);
                    m.addRhsItem(ffl);
                }
            }
        }

        //The "setStatements" variable, at this point, contains a record of unmatched "set" calls. Unmatched means that they do not
        //have a relationship with a resolved "insert", "insertLogical", "update" or "modify" action. Resolved means the action had been
        //identified as an explicit operation above; normally where the RHS line began with such a call. Therefore it is likely the
        // variable they are modifying was recorded as Free Format DRL and hence the "sets" need to be Free Format DRL too.
        for (Map.Entry<String, List<String>> entry : setStatements.entrySet()) {
            if (boundParams.containsKey(entry.getKey()) && SetStatementValidator.validate(entry.getValue())) {
                final ActionSetField action = new ActionSetField(entry.getKey());
                final List<String> setters = entry.getValue();
                if (setters != null) {
                    for (final String statement : setters) {
                        addSetterToAction(action,
                                          entry.getKey(),
                                          boundParams,
                                          dmo,
                                          m,
                                          isJavaDialect,
                                          statement);
                    }
                }
                m.addRhsItem(action,
                             setStatementsPosition.get(entry.getKey()));
            } else {

                if (splitEvals) {

                    for (final String drl : entry.getValue()) {

                        final FreeFormLine action = new FreeFormLine();
                        action.setText(drl);
                        m.addRhsItem(action,
                                     setStatementsPosition.get(entry.getKey()));
                    }
                } else {

                    final StringBuilder drl = new StringBuilder();
                    for (final String setter : entry.getValue()) {
                        drl.append(setter).append("\n");
                    }

                    final FreeFormLine action = new FreeFormLine();
                    action.setText(drl.toString());
                    m.addRhsItem(action,
                                 setStatementsPosition.get(entry.getKey()));
                }

            }
        }

        if (expandedDRLInfo.hasDsl) {
            String dslLine = expandedDRLInfo.dslStatementsInRhs.get(++lineCounter);
            while (dslLine != null) {
                m.addRhsItem(toDSLSentence(expandedDRLInfo.rhsDslPatterns,
                                           dslLine));
                dslLine = expandedDRLInfo.dslStatementsInRhs.get(++lineCounter);
            }
        }
    }

    private void unmarshalUsingExtension(final RuleModel ruleModel,
                                         final RuleModelIActionPersistenceExtension extension,
                                         final String line) {
        try {
            ruleModel.addRhsItem(extension.unmarshal(line));
        } catch (RuleModelDRLPersistenceException e) {
            FreeFormLine freeFormLine = new FreeFormLine();
            freeFormLine.setText(line);
            ruleModel.addRhsItem(freeFormLine);
        }
    }

    private List<RuleModelIActionPersistenceExtension> getMatchingExtensionsForLine(final String line,
                                                                                    final Collection<RuleModelIActionPersistenceExtension> extensions) {
        return extensions.stream().filter(e -> e.accept(line)).collect(Collectors.toList());
    }

    private IAction getActionCallMethod(final RuleModel model,
                                        final boolean isJavaDialect,
                                        final Map<String, String> boundParams,
                                        final PackageDataModelOracle dmo,
                                        final String line,
                                        final String variable,
                                        final String methodName) {
        final ActionCallMethodBuilder builder = new ActionCallMethodBuilder(model,
                                                                            dmo,
                                                                            isJavaDialect,
                                                                            boundParams);
        if (!builder.supports(line)) {
            final FreeFormLine ffl = new FreeFormLine();
            ffl.setText(line);
            return ffl;
        }
        return builder.get(variable,
                           methodName,
                           unwrapParenthesis(line));
    }

    private boolean isInsertedFact(final String[] lines,
                                   final int lineCounter,
                                   final String fact) {
        for (int i = lineCounter; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("insert")) {
                if (fact.equals(unwrapParenthesis(line))) {
                    return true;
                }
            }
        }
        return false;
    }

    private DSLSentence toDSLSentence(final List<SimpleDSLSentence> simpleDslSentences,
                                      final String dslLine) {
        DSLSentence dslSentence = new DSLSentence();
        for (SimpleDSLSentence simpleDslSentence : simpleDslSentences) {
            String dslPattern = simpleDslSentence.getDsl();
            // Dollar breaks the matcher, need to escape them.
            dslPattern = dslPattern.replace("$",
                                            "\\$");
            //A DSL Pattern can contain Regex itself, for example "When the ages is less than {num:1?[0-9]?[0-9]}"
            String regex = dslPattern.replaceAll("\\{.*?\\}",
                                                 "(.*)");
            Matcher matcher = Pattern.compile(regex).matcher(dslLine);
            if (matcher.matches()) {
                dslPattern = dslPattern.replace("\\$",
                                                "$");
                dslSentence.setDrl(simpleDslSentence.getDrl());
                dslSentence.setDefinition(dslPattern);
                for (int i = 0; i < matcher.groupCount(); i++) {
                    dslSentence.getValues().get(i).setValue(matcher.group(i + 1));
                }
                return dslSentence;
            }
        }
        dslSentence.setDefinition(dslLine);
        return dslSentence;
    }

    private PortableParameterDefinition buildPortableParameterDefinition(final String name,
                                                                         final String value,
                                                                         final Map<String, String> boundParams) {
        PortableParameterDefinition paramDef;
        String type = boundParams.get(value);
        if (type != null) {
            if (type.equals("Boolean")) {
                paramDef = new PortableBooleanParameterDefinition();
            } else if (type.equals("String")) {
                paramDef = new PortableStringParameterDefinition();
            } else if (type.equals("Float")) {
                paramDef = new PortableBooleanParameterDefinition();
            } else if (type.equals("Integer")) {
                paramDef = new PortableIntegerParameterDefinition();
            } else {
                paramDef = new PortableObjectParameterDefinition();
            }
            ((HasBinding) paramDef).setBinding(value);
        } else if (value.equals("true") || value.equals("false") || value.equals("Boolean.TRUE") || value.equals("Boolean.FALSE")) {
            paramDef = new PortableBooleanParameterDefinition();
            boolean b = value.equals("true") || value.equals("Boolean.TRUE");
            ((PortableBooleanParameterDefinition) paramDef).setValue(b);
        } else if (value.startsWith("\"")) {
            paramDef = new PortableStringParameterDefinition();
            ((PortableStringParameterDefinition) paramDef).setValue(value.substring(1,
                                                                                    value.length() - 1));
        } else if (Character.isDigit(value.charAt(0))) {
            if (value.endsWith("f")) {
                paramDef = new PortableFloatParameterDefinition();
                ((PortableFloatParameterDefinition) paramDef).setValue(Float.parseFloat(value));
            } else {
                paramDef = new PortableIntegerParameterDefinition();
                ((PortableIntegerParameterDefinition) paramDef).setValue(Integer.parseInt(value));
            }
        } else {
            throw new RuntimeException("Unknown parameter " + value);
        }
        paramDef.setName(name.substring(1,
                                        name.length() - 1));
        return paramDef;
    }

    private void addSettersToAction(final Map<String, List<String>> setStatements,
                                    final String variable,
                                    final ActionFieldList action,
                                    final Map<String, String> boundParams,
                                    final PackageDataModelOracle dmo,
                                    final RuleModel model,
                                    final boolean isJavaDialect) {
        addSettersToAction(setStatements.remove(variable),
                           action,
                           variable,
                           boundParams,
                           dmo,
                           model,
                           isJavaDialect);
    }

    private void addSettersToAction(final List<String> setters,
                                    final ActionFieldList action,
                                    final String variable,
                                    final Map<String, String> boundParams,
                                    final PackageDataModelOracle dmo,
                                    final RuleModel model,
                                    final boolean isJavaDialect) {
        if (setters != null) {
            for (String statement : setters) {
                addSetterToAction(action,
                                  variable,
                                  boundParams,
                                  dmo,
                                  model,
                                  isJavaDialect,
                                  statement);
            }
        }
    }

    private void addSetterToAction(final ActionFieldList action,
                                   final String variable,
                                   final Map<String, String> boundParams,
                                   final PackageDataModelOracle dmo,
                                   final RuleModel model,
                                   final boolean isJavaDialect,
                                   final String statement) {
        final int dotPos = statement.indexOf('.');
        final int argStart = statement.indexOf('(');
        final String methodName = statement.substring(dotPos + 1,
                                                      argStart).trim();
        addSetterToAction(action,
                          variable,
                          boundParams,
                          dmo,
                          model,
                          isJavaDialect,
                          statement,
                          methodName);
    }

    private void addModifiersToAction(final String modifiers,
                                      final ActionFieldList action,
                                      final String variable,
                                      final Map<String, String> boundParams,
                                      final PackageDataModelOracle dmo,
                                      final RuleModel model,
                                      final boolean isJavaDialect) {
        for (String statement : splitArgumentsList(modifiers)) {
            int argStart = statement.indexOf('(');
            String methodName = statement.substring(0,
                                                    argStart).trim();
            addSetterToAction(action,
                              variable,
                              boundParams,
                              dmo,
                              model,
                              isJavaDialect,
                              statement,
                              methodName);
        }
    }

    private void addSetterToAction(final ActionFieldList action,
                                   final String variable,
                                   final Map<String, String> boundParams,
                                   final PackageDataModelOracle dmo,
                                   final RuleModel model,
                                   final boolean isJavaDialect,
                                   final String statement,
                                   final String methodName) {
        String field = getSettedField(model,
                                      methodName,
                                      boundParams.get(variable),
                                      dmo);
        String value = unwrapParenthesis(statement);
        String dataType = inferDataTypeFromAction(action,
                                                  field,
                                                  value,
                                                  isJavaDialect,
                                                  boundParams,
                                                  dmo,
                                                  model.getImports());

        action.addFieldValue(buildFieldValue(isJavaDialect,
                                             field,
                                             value,
                                             dataType,
                                             boundParams));
    }

    private ActionFieldValue buildFieldValue(final boolean isJavaDialect,
                                             String field,
                                             String value,
                                             final String dataType,
                                             final Map<String, String> boundParams) {
        if (value.contains("wiWorkItem.getResult")) {
            field = field.substring(0,
                                    1).toUpperCase() + field.substring(1);
            String wiParam = field.substring("Results".length());
            if (wiParam.equals("BooleanResult")) {
                return new ActionWorkItemFieldValue(field,
                                                    DataType.TYPE_BOOLEAN,
                                                    "WorkItem",
                                                    wiParam,
                                                    Boolean.class.getName());
            } else if (wiParam.equals("StringResult")) {
                return new ActionWorkItemFieldValue(field,
                                                    DataType.TYPE_STRING,
                                                    "WorkItem",
                                                    wiParam,
                                                    String.class.getName());
            } else if (wiParam.equals("IntegerResult")) {
                return new ActionWorkItemFieldValue(field,
                                                    DataType.TYPE_NUMERIC_INTEGER,
                                                    "WorkItem",
                                                    wiParam,
                                                    Integer.class.getName());
            } else if (wiParam.equals("FloatResult")) {
                return new ActionWorkItemFieldValue(field,
                                                    DataType.TYPE_NUMERIC_FLOAT,
                                                    "WorkItem",
                                                    wiParam,
                                                    Float.class.getName());
            }
        }

        value = removeNumericSuffix(value,
                                    dataType);
        final int fieldNature = inferFieldNature(dataType,
                                                 value,
                                                 boundParams,
                                                 isJavaDialect);

        //If the field is a formula don't adjust the param value
        String paramValue = value;
        switch (fieldNature) {
            case FieldNatureType.TYPE_FORMULA:
                break;
            case FieldNatureType.TYPE_VARIABLE:
                paramValue = "=" + paramValue;
                break;
            case FieldNatureType.TYPE_TEMPLATE:
                paramValue = unwrapTemplateKey(value);
                break;
            default:
                paramValue = adjustParam(dataType,
                                         value,
                                         isJavaDialect);
        }
        ActionFieldValue fieldValue = new ActionFieldValue(field,
                                                           paramValue,
                                                           dataType);
        fieldValue.setNature(fieldNature);
        return fieldValue;
    }

    private boolean isJavaIdentifier(final String name) {
        if (name == null || name.length() == 0 || !Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String getSettedField(final RuleModel model,
                                  final String methodName,
                                  final String variableType,
                                  final PackageDataModelOracle dmo) {
        //Check if method is MethodInformation as multiple parameter "setters" are handled as methods and not field mutators
        List<MethodInfo> mis = RuleModelPersistenceHelper.getMethodInfosForType(model,
                                                                                dmo,
                                                                                variableType);
        if (mis != null) {
            for (MethodInfo mi : mis) {
                if (mi.getName().equals(methodName)) {
                    return null;
                }
            }
        }

        //Check if method is a field mutator
        if (methodName.length() > 3 && methodName.startsWith("set")) {
            String field = methodName.substring(3);
            if (Character.isUpperCase(field.charAt(0))) {
                return field.substring(0,
                                       1).toLowerCase() + field.substring(1);
            } else {
                return field;
            }
        }
        return null;
    }

    private String getStatementType(final String fact,
                                    final Map<String, String> factsType) {
        String type = null;
        if (fact.startsWith("new ")) {
            String inserted = fact.substring(4).trim();
            if (inserted.endsWith("()")) {
                type = inserted.substring(0,
                                          inserted.length() - 2).trim();
            }
        } else {
            type = factsType.get(fact);
        }
        return type;
    }

    private Expr parseExpr(final String expr,
                           final boolean isJavaDialect,
                           final Map<String, String> boundParams,
                           final PackageDataModelOracle dmo) {
        if (isSingleEval(expr)) {
            return new EvalExpr(unwrapParenthesis(expr));
        }
        List<String> splittedExpr = splitExpression(expr);
        if (splittedExpr.size() == 1) {
            String singleExpr = splittedExpr.get(0);
            if (singleExpr.startsWith("(")) {
                return parseExpr(singleExpr.substring(1),
                                 isJavaDialect,
                                 boundParams,
                                 dmo);
            } else if (isSingleEval(singleExpr)) {
                return new EvalExpr(unwrapParenthesis(singleExpr));
            } else {
                return new SimpleExpr(singleExpr,
                                      isJavaDialect,
                                      boundParams,
                                      dmo);
            }
        }
        if (isCompositeFieldConstraint(splittedExpr)) {
            ComplexExpr complexExpr = new ComplexExpr(splittedExpr.get(1),
                                                      expr);
            for (int i = 0; i < splittedExpr.size(); i += 2) {
                complexExpr.subExprs.add(parseExpr(splittedExpr.get(i),
                                                   isJavaDialect,
                                                   boundParams,
                                                   dmo));
            }
            return complexExpr;
        } else {
            //The expression parts cannot be represented by a CompositeFieldConstraint as it
            //contains different operators in between the component parts. Therefore we have
            //to revert to simple unmarshalling of the rule.
            throw new RuleModelUnmarshallingException();
        }
    }

    private boolean isCompositeFieldConstraint(final List<String> splittedExpr) {
        if (splittedExpr.size() < 2) {
            return false;
        }
        String operator = splittedExpr.get(1);
        for (int i = 1; i < splittedExpr.size(); i += 2) {
            if (!splittedExpr.get(i).equals(operator)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSingleEval(final String expr) {
        if (!expr.startsWith("eval(")) {
            return false;
        }
        int nestingLevel = 0;
        final char[] characters = expr.substring(4).trim().toCharArray();
        for (int i = 0; i < characters.length; i++) {
            final char ch = characters[i];
            if (ch == '(') {
                nestingLevel++;
            } else if (ch == ')') {
                nestingLevel--;
                if (nestingLevel == 0) {
                    if (i < characters.length - 1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private enum SplitterState {
        START,
        EXPR,
        PIPE,
        OR,
        AMPERSAND,
        AND,
        NESTED,
        EVAL
    }

    private List<String> splitExpression(final String expr) {
        List<String> splittedExpr = new ArrayList<String>();
        int nestingLevel = 0;
        int evalNestingLevel = nestingLevel;
        SplitterState status = SplitterState.START;

        StringBuilder sb = new StringBuilder();
        for (char ch : expr.toCharArray()) {
            switch (status) {
                case START:
                    if (ch == '(') {
                        status = SplitterState.NESTED;
                        sb.append(ch);
                        nestingLevel++;
                    } else {
                        status = SplitterState.EXPR;
                        sb.append(ch);
                    }
                    break;
                case EXPR:
                    if (sb.toString().equals("eval(")) {
                        status = SplitterState.EVAL;
                        evalNestingLevel = nestingLevel;
                        nestingLevel++;
                        sb.append(ch);
                    } else if (ch == '|') {
                        status = SplitterState.PIPE;
                    } else if (ch == '&') {
                        status = SplitterState.AMPERSAND;
                    } else {
                        sb.append(ch);
                    }
                    break;
                case EVAL:
                    if (ch == '(') {
                        nestingLevel++;
                        sb.append(ch);
                    } else if (ch == ')') {
                        nestingLevel--;
                        if (nestingLevel == evalNestingLevel) {
                            String currentExpr = sb.toString().trim();
                            if (currentExpr.length() > 0) {
                                splittedExpr.add(currentExpr + ")");
                            }
                            status = SplitterState.EXPR;
                            sb = new StringBuilder();
                        } else {
                            sb.append(ch);
                        }
                    } else {
                        sb.append(ch);
                    }
                    break;

                case PIPE:
                    if (ch == '|') {
                        status = SplitterState.OR;
                    } else {
                        status = SplitterState.EXPR;
                        sb.append('|').append(ch);
                    }
                    break;
                case AMPERSAND:
                    if (ch == '&') {
                        status = SplitterState.AND;
                    } else {
                        status = SplitterState.EXPR;
                        sb.append('&').append(ch);
                    }
                    break;
                case OR:
                case AND:
                    if (ch == '=' || ch == '!' || ch == '<' || ch == '>') {
                        sb.append(status == SplitterState.AND ? "&& " : "|| ");
                        sb.append(ch);
                        status = SplitterState.EXPR;
                    } else if (Character.isJavaIdentifierStart(ch)) {
                        String currentExpr = sb.toString().trim();
                        if (currentExpr.length() > 0) {
                            splittedExpr.add(currentExpr);
                        }
                        splittedExpr.add(status == SplitterState.AND ? "&&" : "||");
                        status = SplitterState.EXPR;
                        sb = new StringBuilder();
                        sb.append(ch);
                    } else if (ch == '(') {
                        String currentExpr = sb.toString().trim();
                        if (currentExpr.length() > 0) {
                            splittedExpr.add(currentExpr);
                        }
                        splittedExpr.add(status == SplitterState.AND ? "&&" : "||");
                        status = SplitterState.NESTED;
                        nestingLevel++;
                        sb = new StringBuilder();
                        sb.append(ch);
                    }
                    break;
                case NESTED:
                    if (ch == '(') {
                        nestingLevel++;
                        sb.append(ch);
                    } else if (ch == ')') {
                        nestingLevel--;
                        if (nestingLevel == 0) {
                            String currentExpr = sb.toString().trim();
                            if (currentExpr.length() > 0) {
                                splittedExpr.add("(" + currentExpr);
                            }
                            status = SplitterState.EXPR;
                            sb = new StringBuilder();
                        } else {
                            sb.append(ch);
                        }
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        String currentExpr = sb.toString().trim();
        if (currentExpr.length() > 0) {
            splittedExpr.add(currentExpr);
        }
        return splittedExpr;
    }

    private interface Expr {

        FieldConstraint asFieldConstraint(final RuleModel m,
                                          final FactPattern factPattern);
    }

    private static class SimpleExpr implements Expr {

        private final String expr;
        private final boolean isJavaDialect;
        private final Map<String, String> boundParams;
        private final PackageDataModelOracle dmo;

        private SimpleExpr(final String expr,
                           final boolean isJavaDialect,
                           final Map<String, String> boundParams,
                           final PackageDataModelOracle dmo) {
            this.expr = expr;
            this.isJavaDialect = isJavaDialect;
            this.boundParams = boundParams;
            this.dmo = dmo;
        }

        public FieldConstraint asFieldConstraint(final RuleModel m,
                                                 final FactPattern factPattern) {
            String fieldName = expr;

            String value = null;
            String operator = findNullOrNotNullOperator(expr);
            if (operator != null) {
                int opPos = expr.indexOf(operator);
                fieldName = expr.substring(0,
                                           opPos).trim();
            } else {
                operator = findOperator(expr);
            }
            if (operator != null) {
                int opPos = expr.indexOf(operator);
                fieldName = expr.substring(0,
                                           opPos).trim();
                value = expr.substring(opPos + operator.length(),
                                       expr.length()).trim();
            }

            boolean isExpression = fieldName.contains(".") || fieldName.endsWith("()");
            return createFieldConstraint(m,
                                         factPattern,
                                         fieldName,
                                         value,
                                         operator == null ? null : operator.trim(),
                                         isExpression);
        }

        private SingleFieldConstraint createNullCheckFieldConstraint(final RuleModel m,
                                                                     final FactPattern factPattern,
                                                                     final String fieldName) {
            return createFieldConstraint(m,
                                         factPattern,
                                         fieldName,
                                         null,
                                         null,
                                         true);
        }

        private SingleFieldConstraint createFieldConstraint(final RuleModel m,
                                                            final FactPattern factPattern,
                                                            final String fieldName,
                                                            String value,
                                                            final String operator,
                                                            final boolean isExpression) {
            String operatorParams = null;
            if (value != null && value.startsWith("[")) {
                int endSquare = value.indexOf(']');
                operatorParams = value.substring(1,
                                                 endSquare).trim();
                value = value.substring(endSquare + 1).trim();
            }

            SingleFieldConstraint fieldConstraint = isExpression ?
                    createExpressionBuilderConstraint(m,
                                                      factPattern,
                                                      fieldName,
                                                      operator,
                                                      value) :
                    createSingleFieldConstraint(m,
                                                factPattern,
                                                fieldName,
                                                operator,
                                                value);

            if (operatorParams != null) {
                int i = 0;
                for (String param : operatorParams.split(",")) {
                    fieldConstraint.setParameter("" + i++,
                                                 param.trim());
                }
                fieldConstraint.setParameter("org.drools.workbench.models.commons.backend.rule.visibleParameterSet",
                                             "" + i);
                fieldConstraint.setParameter("org.drools.workbench.models.commons.backend.rule.operatorParameterGenerator",
                                             "org.drools.workbench.models.commons.backend.rule.CEPOperatorParameterDRLBuilder");
            }

            if (fieldName.equals("this") && (operator == null || operator.equals("!= null"))) {
                fieldConstraint.setFieldType(DataType.TYPE_THIS);
            }
            fieldConstraint.setFactType(factPattern.getFactType());

            ModelField field = findField(findFields(m,
                                                    dmo,
                                                    factPattern.getFactType()),
                                         fieldConstraint.getFieldName());

            if (field != null && (fieldConstraint.getFieldType() == null || fieldConstraint.getFieldType().trim().length() == 0)) {
                fieldConstraint.setFieldType(getSimpleFactType(field.getType(),
                                                               dmo));
            }
            return fieldConstraint;
        }

        private SingleFieldConstraint createExpressionBuilderConstraint(final RuleModel m,
                                                                        final FactPattern factPattern,
                                                                        final String fieldName,
                                                                        final String operator,
                                                                        final String value) {
            // TODO: we should find a way to know when the expression uses a getter and in this case create a plain SingleFieldConstraint
            //int dotPos = fieldName.lastIndexOf('.');
            //SingleFieldConstraint con = createSingleFieldConstraint(dotPos > 0 ? fieldName.substring(dotPos+1) : fieldName, operator, value);

            SingleFieldConstraint con = createSingleFieldConstraintEBLeftSide(m,
                                                                              factPattern,
                                                                              fieldName,
                                                                              operator,
                                                                              value);

            return con;
        }

        private SingleFieldConstraint createSingleFieldConstraint(final RuleModel m,
                                                                  final FactPattern factPattern,
                                                                  String fieldName,
                                                                  final String operator,
                                                                  final String value) {
            SingleFieldConstraint con = new SingleFieldConstraint();
            fieldName = setFieldBindingOnContraint(factPattern.getFactType(),
                                                   fieldName,
                                                   m,
                                                   con,
                                                   boundParams);
            con.setFieldName(fieldName);
            setOperatorAndValueOnConstraint(m,
                                            operator,
                                            value,
                                            factPattern,
                                            con);

            //Setup parent relationships for SingleFieldConstraints
            for (FieldConstraint fieldConstraint : factPattern.getFieldConstraints()) {
                if (fieldConstraint instanceof SingleFieldConstraint) {
                    SingleFieldConstraint sfc = (SingleFieldConstraint) fieldConstraint;
                    if (sfc.getOperator() != null && sfc.getOperator().equals("!= null")) {
                        int parentPos = fieldName.indexOf(sfc.getFieldName() + ".");
                        if (parentPos >= 0 && !fieldName.substring(parentPos + sfc.getFieldName().length() + 1).contains(".")) {
                            con.setParent(sfc);
                            break;
                        }
                    }
                }
            }

            if (con.getParent() == null) {
                con.setParent(createParentFor(m,
                                              factPattern,
                                              fieldName));
            }

            return con;
        }

        private SingleFieldConstraintEBLeftSide createSingleFieldConstraintEBLeftSide(final RuleModel m,
                                                                                      final FactPattern factPattern,
                                                                                      String fieldName,
                                                                                      final String operator,
                                                                                      final String value) {
            SingleFieldConstraintEBLeftSide con = new SingleFieldConstraintEBLeftSide();

            fieldName = setFieldBindingOnContraint(factPattern.getFactType(),
                                                   fieldName,
                                                   m,
                                                   con,
                                                   boundParams);
            String classType = RuleModelPersistenceHelper.getFQFactType(m,
                                                                        factPattern.getFactType(),
                                                                        dmo);
            con.getExpressionLeftSide().appendPart(new ExpressionUnboundFact(factPattern.getFactType()));

            parseExpression(m,
                            classType,
                            fieldName,
                            con.getExpressionLeftSide());

            setOperatorAndValueOnConstraint(m,
                                            operator,
                                            value,
                                            factPattern,
                                            con);

            return con;
        }

        private ExpressionFormLine parseExpression(final RuleModel m,
                                                   String factType,
                                                   final String fieldName,
                                                   final ExpressionFormLine expression) {
            String[] splits = fieldName.split("\\.");

            boolean isBoundParam = false;
            if (factType == null) {
                factType = RuleModelPersistenceHelper.getFQFactType(m,
                                                                    boundParams.get(splits[0].trim()),
                                                                    dmo);
                isBoundParam = true;
            }

            //An ExpressionPart can be a Field or a Method
            ModelField[] typeFields = findFields(m,
                                                 dmo,
                                                 factType);
            List<MethodInfo> methodInfos = getMethodInfosForType(m,
                                                                 dmo,
                                                                 factType);

            //Handle all but last expression part
            for (int i = 0; i < splits.length - 1; i++) {
                String expressionPart = splits[i];

                //The first part of the expression may be a bound variable
                if (boundParams.containsKey(expressionPart)) {
                    factType = RuleModelPersistenceHelper.getFQFactType(m,
                                                                        boundParams.get(expressionPart),
                                                                        dmo);
                    isBoundParam = true;

                    typeFields = findFields(m,
                                            dmo,
                                            factType);
                    methodInfos = getMethodInfosForType(m,
                                                        dmo,
                                                        factType);
                }
                if ("this".equals(expressionPart)) {
                    expression.appendPart(new ExpressionField(expressionPart,
                                                              factType,
                                                              DataType.TYPE_THIS));
                } else if (isBoundParam) {
                    ModelField currentFact = findFact(dmo.getModuleModelFields(),
                                                      factType);

                    expression.appendPart(getExpressionPart(expressionPart,
                                                            currentFact));
                    isBoundParam = false;
                } else {
                    //An ExpressionPart can be a Field or a Method
                    String currentClassName = null;
                    ModelField currentField = findField(typeFields,
                                                        expressionPart);
                    if (currentField != null) {
                        currentClassName = currentField.getClassName();
                    }
                    MethodInfo currentMethodInfo = findMethodInfo(methodInfos,
                                                                  expressionPart);
                    if (currentMethodInfo != null) {
                        currentClassName = currentMethodInfo.getReturnClassType();
                    }

                    processExpressionPart(m,
                                          factType,
                                          currentField,
                                          currentMethodInfo,
                                          expression,
                                          expressionPart);

                    //Refresh field and method information based on current expression part
                    typeFields = findFields(m,
                                            dmo,
                                            currentClassName);
                    methodInfos = getMethodInfosForType(m,
                                                        dmo,
                                                        currentClassName);
                }
            }

            //Handle last expression part
            String expressionPart = splits[splits.length - 1];
            ModelField currentField = findField(typeFields,
                                                expressionPart);
            MethodInfo currentMethodInfo = findMethodInfo(methodInfos,
                                                          expressionPart);

            processExpressionPart(m,
                                  factType,
                                  currentField,
                                  currentMethodInfo,
                                  expression,
                                  expressionPart);

            return expression;
        }

        private void processExpressionPart(final RuleModel m,
                                           final String factType,
                                           final ModelField currentField,
                                           final MethodInfo currentMethodInfo,
                                           final ExpressionFormLine expression,
                                           final String expressionPart) {
            if (currentField == null) {
                boolean isMethod = currentMethodInfo != null;
                if (isMethod) {
                    final ExpressionMethod em = new ExpressionMethod(currentMethodInfo.getName(),
                                                                     currentMethodInfo.getReturnClassType(),
                                                                     currentMethodInfo.getGenericType(),
                                                                     currentMethodInfo.getParametricReturnType());
                    //Add applicable parameter values
                    final List<String> parameters = parseExpressionParameters(expressionPart);
                    for (int index = 0; index < currentMethodInfo.getParams().size(); index++) {
                        final String paramDataType = currentMethodInfo.getParams().get(index);
                        final String paramValue = getParameterValue(paramDataType,
                                                                    parameters,
                                                                    index);
                        if (paramValue != null) {
                            final ExpressionFormLine param = new ExpressionFormLine(index);
                            param.appendPart(new ExpressionMethodParameter(paramValue,
                                                                           paramDataType,
                                                                           paramDataType));
                            em.putParam(paramDataType,
                                        param);
                        }
                    }
                    expression.appendPart(em);
                } else {
                    expression.appendPart(new ExpressionText(expressionPart));
                }
            } else if ("Collection".equals(currentField.getType())) {
                expression.appendPart(new ExpressionCollection(expressionPart,
                                                               currentField.getClassName(),
                                                               currentField.getType(),
                                                               dmo.getModuleFieldParametersType().get(factType + "#" + expressionPart))
                );
            } else {
                expression.appendPart(new ExpressionField(expressionPart,
                                                          currentField.getClassName(),
                                                          currentField.getType()));
            }
        }

        private String getParameterValue(final String paramDataType,
                                         final List<String> parameters,
                                         final int index) {
            if (parameters == null || parameters.isEmpty()) {
                return null;
            }
            if (index < 0 || index > parameters.size() - 1) {
                return null;
            }

            return RuleModelPersistenceHelper.adjustParam(paramDataType,
                                                          parameters.get(index).trim(),
                                                          isJavaDialect);
        }

        private ModelField findFact(final Map<String, ModelField[]> modelFields,
                                    final String factType) {
            final ModelField[] typeFields = modelFields.get(factType);
            if (typeFields == null) {
                return null;
            }
            for (ModelField typeField : typeFields) {
                if (typeField.getName().equals(DataType.TYPE_THIS)) {
                    return typeField;
                }
            }
            return null;
        }

        private SingleFieldConstraint createParentFor(final RuleModel m,
                                                      final FactPattern factPattern,
                                                      final String fieldName) {
            int dotPos = fieldName.lastIndexOf('.');
            if (dotPos > 0) {
                SingleFieldConstraint constraint = createNullCheckFieldConstraint(m,
                                                                                  factPattern,
                                                                                  fieldName.substring(0,
                                                                                                      dotPos));
                factPattern.addConstraint(constraint);
                return constraint;
            }
            return null;
        }

        private String setFieldBindingOnContraint(final String factType,
                                                  String fieldName,
                                                  final RuleModel model,
                                                  final SingleFieldConstraint con,
                                                  final Map<String, String> boundParams) {
            int colonPos = fieldName.indexOf(':');
            if (colonPos > 0) {
                String fieldBinding = fieldName.substring(0,
                                                          colonPos).trim();
                con.setFieldBinding(fieldBinding);
                fieldName = fieldName.substring(colonPos + 1).trim();

                ModelField[] fields = findFields(model,
                                                 dmo,
                                                 factType);
                if (fields != null) {
                    for (ModelField field : fields) {
                        if (field.getName().equals(fieldName)) {
                            boundParams.put(fieldBinding,
                                            field.getClassName());
                        }
                    }
                }
            }
            return fieldName;
        }

        private String setOperatorAndValueOnConstraint(final RuleModel m,
                                                       final String operator,
                                                       final String value,
                                                       final FactPattern factPattern,
                                                       final SingleFieldConstraint con) {
            con.setOperator(operator);
            String type = null;
            boolean isAnd = false;
            String[] splittedValue = new String[0];
            if (!(value == null || value.isEmpty())) {
                isAnd = value.contains("&&");
                splittedValue = isAnd ? value.split("\\&\\&") : value.split("\\|\\|");
                if (!splittedValue[0].trim().isEmpty()) {
                    type = setValueOnConstraint(m,
                                                operator,
                                                factPattern,
                                                con,
                                                splittedValue[0].trim());
                }
            }

            if (splittedValue.length > 1) {
                ConnectiveConstraint[] connectiveConstraints = new ConnectiveConstraint[splittedValue.length - 1];
                for (int i = 0; i < connectiveConstraints.length; i++) {
                    String constraint = splittedValue[i + 1].trim();
                    String connectiveOperator = findOperator(constraint);
                    String connectiveValue = constraint.substring(connectiveOperator == null ? 0 : connectiveOperator.length()).trim();

                    connectiveConstraints[i] = new ConnectiveConstraint();
                    connectiveConstraints[i].setOperator((isAnd ? "&& " : "|| ") + (connectiveOperator == null ? null : connectiveOperator.trim()));
                    connectiveConstraints[i].setFactType(factPattern.getFactType());
                    connectiveConstraints[i].setFieldName(con.getFieldName());
                    connectiveConstraints[i].setFieldType(con.getFieldType());
                    setValueOnConstraint(m,
                                         operator,
                                         factPattern,
                                         connectiveConstraints[i],
                                         connectiveValue);
                }
                con.setConnectives(connectiveConstraints);
            }
            return type;
        }

        private String setValueOnConstraint(final RuleModel m,
                                            final String operator,
                                            final FactPattern factPattern,
                                            final BaseSingleFieldConstraint con,
                                            String value) {
            if (value.contains("@{")) {
                con.setConstraintValueType(BaseSingleFieldConstraint.TYPE_TEMPLATE);
                con.setValue(unwrapTemplateKey(value));
            } else if (value.startsWith("\"")) {
                con.setConstraintValueType(SingleFieldConstraint.TYPE_LITERAL);
                con.setValue(value.substring(1,
                                             value.length() - 1));
            } else if (value.startsWith("(")) {
                if (operator != null && (Objects.equals("in", operator) || Objects.equals("not in", operator))) {
                    con.setConstraintValueType(SingleFieldConstraint.TYPE_LITERAL);
                    String[] split = ListSplitter.splitPreserveQuotes("\"", true, unwrapParenthesis(value));
                    con.setValue(String.join(", ", split));
                } else {
                    con.setConstraintValueType(SingleFieldConstraint.TYPE_RET_VALUE);
                    con.setValue(unwrapParenthesis(value));
                }
            } else {
                if (!Character.isDigit(value.charAt(0))) {
                    if (value.equals("true") || value.equals("false")) {
                        con.setConstraintValueType(BaseSingleFieldConstraint.TYPE_ENUM);
                    } else if (RuleModelPersistenceHelper.isEnumerationValue(m,
                                                                             factPattern,
                                                                             con,
                                                                             dmo)) {
                        con.setConstraintValueType(SingleFieldConstraint.TYPE_ENUM);
                    } else if (value.indexOf('.') > 0 && boundParams.containsKey(value.substring(0,
                                                                                                 value.indexOf('.')).trim())) {
                        con.setExpressionValue(parseExpression(m,
                                                               null,
                                                               value,
                                                               new ExpressionFormLine()));
                        con.setConstraintValueType(BaseSingleFieldConstraint.TYPE_EXPR_BUILDER_VALUE);
                        value = "";
                    } else if (boundParams.containsKey(value)) {
                        con.setConstraintValueType(SingleFieldConstraint.TYPE_VARIABLE);
                    } else {
                        con.setConstraintValueType(SingleFieldConstraint.TYPE_RET_VALUE);
                    }
                } else {
                    con.setConstraintValueType(SingleFieldConstraint.TYPE_LITERAL);
                }
                if (isNumberThatNeedsToBeTrimmed(value)) {
                    con.setValue(trim(value));
                } else {
                    con.setValue(value);
                }
            }

            final String type = RuleModelPersistenceHelper.inferDataTypeFromConstraint(m,
                                                                                       factPattern,
                                                                                       con,
                                                                                       value,
                                                                                       dmo,
                                                                                       m.getImports());

            if (con instanceof SingleFieldConstraint) {
                ((SingleFieldConstraint) con).setFieldType(type);
            } else if (con instanceof ConnectiveConstraint) {
                ((ConnectiveConstraint) con).setFieldType(type);
            }

            return type;
        }

        private boolean isNumberThatNeedsToBeTrimmed(final String value) {
            if (isBigDecimal(value) || isBigInteger(value)) {
                return true;
            } else {
                return false;
            }
        }

        private boolean isBigDecimal(final String value) {
            if (value.endsWith("B")) {
                try {
                    new BigDecimal(trim(value));
                } catch (NumberFormatException e) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }

        private boolean isBigInteger(final String value) {
            if (value.endsWith("I")) {
                try {
                    new BigInteger(trim(value));
                } catch (NumberFormatException e) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }

        private String trim(final String value) {
            return value.substring(0, value.length() - 1);
        }
    }

    /**
     * If the bound type is not in the DMO it probably hasn't been imported.
     * So we have little option than to fall back to keeping the value as Text.
     */
    private static ExpressionPart getExpressionPart(String expressionPart,
                                                    ModelField currentFact) {
        if (currentFact == null) {
            return new ExpressionText(expressionPart);
        } else {
            return new ExpressionVariable(expressionPart,
                                          currentFact.getClassName(),
                                          currentFact.getType());
        }
    }

    private static class ComplexExpr implements Expr {

        private final List<Expr> subExprs = new ArrayList<Expr>();
        private final String connector;
        private final String expr;

        private ComplexExpr(final String connector,
                            final String expr) {
            this.connector = connector;
            this.expr = expr;
        }

        public FieldConstraint asFieldConstraint(final RuleModel m,
                                                 final FactPattern factPattern) {
            if (!connector.equals("&&") && !connector.equals("||")) {
                return new EvalExpr(expr).asFieldConstraint(m, factPattern);
            } else {

                final CompositeFieldConstraint comp = new CompositeFieldConstraint();
                comp.setCompositeJunctionType(connector.equals("&&") ? CompositeFieldConstraint.COMPOSITE_TYPE_AND : CompositeFieldConstraint.COMPOSITE_TYPE_OR);
                for (final Expr subExpr : subExprs) {
                    comp.addConstraint(subExpr.asFieldConstraint(m,
                                                                 factPattern));
                }
                convertLegacyMatchesToNewFormat(comp);
                return comp;
            }
        }

        /**
         * Fact(something != null && matches "P.*") = was supported before, now is deprecated.
         * It breaks the UI if user has a rule with this case, so we convert to the new format:
         * Fact(something != null && something matches "P.*")
         *                           ^^^^^^^^
         *
         * @param comp The CompositeFieldConstraint with legacy 'matches'.
         */
        private void convertLegacyMatchesToNewFormat(final CompositeFieldConstraint comp) {
            String fieldName = "";
            for (FieldConstraint field : comp.getConstraints()) {
                if (field instanceof SingleFieldConstraint) {
                    SingleFieldConstraint constraint = (SingleFieldConstraint) field;
                    if (StringUtils.isEmpty(constraint.getFieldName())
                            && (Objects.equals(constraint.getOperator(), "matches") || Objects.equals(constraint.getOperator(), "not matches"))) {
                        constraint.setFieldName(fieldName);
                    }
                    fieldName = constraint.getFieldName();
                }
            }
        }
    }

    private static class EvalExpr implements Expr {

        private final String expr;

        private EvalExpr(final String expr) {
            this.expr = expr;
        }

        public FieldConstraint asFieldConstraint(final RuleModel m,
                                                 final FactPattern factPattern) {
            SingleFieldConstraint con = new SingleFieldConstraint();
            con.setConstraintValueType(SingleFieldConstraint.TYPE_PREDICATE);
            con.setValue(expr);
            return con;
        }
    }

    private static class SimpleDSLSentence {

        private String dsl;
        private String drl;

        private SimpleDSLSentence(final String dsl,
                                  final String drl) {
            this.dsl = dsl;
            this.drl = drl;
        }

        private String getDsl() {
            return this.dsl;
        }

        private String getDrl() {
            return this.drl;
        }
    }

    //Exception to indicate the DrlParser encountered a problem parsing the DRL. This fails-fast the unmarshalling.
    public static class RuleModelUnmarshallingException extends RuntimeException {

    }

    //Simple fall-back parser of DRL
    private RuleModel getSimpleRuleModel(final String drl) {
        final RuleModel rm = new RuleModel();
        rm.setPackageName(PackageNameParser.parsePackageName(drl));
        rm.setImports(ImportsParser.parseImports(drl));

        try {
            parseAttributes(rm, parseDrl(drl).getAttributes());
        } catch (Exception e) {
            //Discard. We're unable to retrieve the rule attributes from the DRL
        }

        final Pattern rulePattern = Pattern.compile(".*\\s?rule\\s+\"(.+?)\"\\s+.*",
                                                    Pattern.DOTALL);
        final Pattern lhsPattern = Pattern.compile(".*\\s+when\\s+(.+?)\\s+then.*",
                                                   Pattern.DOTALL);
        final Pattern rhsPattern = Pattern.compile(".*\\s+then\\s+(.+?)\\s+end.*",
                                                   Pattern.DOTALL);

        final Matcher ruleMatcher = rulePattern.matcher(drl);
        if (ruleMatcher.matches()) {
            String name = ruleMatcher.group(1);
            if (name.startsWith("\"")) {
                name = name.substring(1);
            }
            if (name.endsWith("\"")) {
                name = name.substring(0,
                                      name.length() - 1);
            }
            rm.name = name;
        }

        final Matcher lhsMatcher = lhsPattern.matcher(drl);
        if (lhsMatcher.matches()) {
            final FreeFormLine lhs = new FreeFormLine();
            lhs.setText(lhsMatcher.group(1) == null ? "" : lhsMatcher.group(1).trim());
            rm.addLhsItem(lhs);
        }

        final Matcher rhsMatcher = rhsPattern.matcher(drl);
        if (rhsMatcher.matches()) {
            final FreeFormLine rhs = new FreeFormLine();
            rhs.setText(rhsMatcher.group(1) == null ? "" : rhsMatcher.group(1).trim());
            rm.addRhsItem(rhs);
        }

        return rm;
    }
}
