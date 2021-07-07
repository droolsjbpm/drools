/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
 */
package org.kie.pmml.models.drools.commons.factories;

import java.util.List;
import java.util.StringJoiner;

import org.dmg.pmml.OutputField;
import org.dmg.pmml.ResultFeature;
import org.drools.compiler.lang.api.RuleDescrBuilder;
import org.kie.pmml.models.drools.ast.KiePMMLDroolsRule;
import org.kie.pmml.models.drools.tuples.KiePMMLReasonCodeAndValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.commons.Constants.DONE;

/**
 * Class used to generate the <b>rhs</b> of a rule (descr) out of a <b>KiePMMLDroolsRule</b>
 */
public class KiePMMLDescrRhsFactory {

    public static final String SET_PMML4_RESULT_CODE = "\r\n" + KiePMMLDescrFactory.PMML4_RESULT_IDENTIFIER + ".setResultCode(\"%s\");";
    public static final String ADD_PMML4_RESULT_VARIABLE = "\r\n" + KiePMMLDescrFactory.PMML4_RESULT_IDENTIFIER + ".addResultVariable(" + KiePMMLDescrFactory.PMML4_RESULT_IDENTIFIER + ".getResultObjectName()" + ", %s);";
    public static final String ADD_PMML4_OUTPUT_FIELD = "\r\n" + KiePMMLDescrFactory.PMML4_RESULT_IDENTIFIER + ".addResultVariable(\"%s\", %s);";

    public static final String ADD_OUTPUTFIELD_VALUE = "\r\n" + KiePMMLDescrFactory.OUTPUTFIELDS_MAP_IDENTIFIER + ".put(\"%s\", %s);";

    public static final String UPDATE_STATUS_HOLDER_STATUS = "\r\n" + KiePMMLDescrRulesFactory.STATUS_HOLDER + ".setStatus(\"%s\");";
    public static final String UPDATE_STATUS_HOLDER_ACCUMULATE = "\r\n" + KiePMMLDescrRulesFactory.STATUS_HOLDER + ".accumulate(%s);";
    public static final String UPDATE_STATUS_HOLDER = "\r\nupdate(" + KiePMMLDescrRulesFactory.STATUS_HOLDER + ");";
    public static final String RETURN_ACCUMULATION = "\r\n" + KiePMMLDescrFactory.PMML4_RESULT_IDENTIFIER + ".addResultVariable($pmml4Result.getResultObjectName(), $statusHolder.getAccumulator());";

    public static final String FOCUS_AGENDA_GROUP = "\r\nkcontext.getKieRuntime().getAgenda().getAgendaGroup( \"%s\" ).setFocus();";

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLDescrRhsFactory.class.getName());

    final RuleDescrBuilder builder;

    private KiePMMLDescrRhsFactory(final RuleDescrBuilder builder) {
        this.builder = builder;
    }

    public static KiePMMLDescrRhsFactory factory(final RuleDescrBuilder builder) {
        return new KiePMMLDescrRhsFactory(builder);
    }

    public void declareRhs(final KiePMMLDroolsRule rule) {
        logger.trace("declareRhs {}", rule);
        if (rule.getIfBreakField() != null) {
            declareIfThen(rule);
        } else {
            declareDefaultThen(rule);
        }
    }

    protected void declareDefaultThen(final KiePMMLDroolsRule rule) {
        StringJoiner joiner = new StringJoiner("");
        if (rule.getStatusToSet() != null) {
            joiner.add(String.format(UPDATE_STATUS_HOLDER_STATUS, rule.getStatusToSet()));
        }
        if (rule.getStatusToSet() != null || rule.getToAccumulate() != null) {
            joiner.add(UPDATE_STATUS_HOLDER);
        }
        commonDeclareThen(rule, joiner);
        builder.rhs(joiner.toString());
    }

    protected void declareIfThen(final KiePMMLDroolsRule rule) {
        builder.rhs(String.format(UPDATE_STATUS_HOLDER_STATUS, rule.getStatusToSet()));
        StringJoiner joiner = new StringJoiner("");
        joiner.add(String.format(UPDATE_STATUS_HOLDER_STATUS, DONE));
        commonDeclareThen(rule, joiner);
        builder.namedRhs(KiePMMLDescrRulesFactory.BREAK_LABEL, joiner.toString());
    }

    protected void commonDeclareThen(final KiePMMLDroolsRule rule, final StringJoiner joiner) {
        if (rule.getFocusedAgendaGroup() != null) {
            joiner.add(String.format(FOCUS_AGENDA_GROUP, rule.getFocusedAgendaGroup()));
        }
        if (rule.getToAccumulate() != null) {
            joiner.add(String.format(UPDATE_STATUS_HOLDER_ACCUMULATE, rule.getToAccumulate()));
        }
        if (rule.isAccumulationResult()) {
            joiner.add(RETURN_ACCUMULATION);
        }
        if (rule.getResultCode() != null) {
            joiner.add(String.format(SET_PMML4_RESULT_CODE, rule.getResultCode()));
        }
        if (rule.getResult() != null) {
            joiner.add(String.format(ADD_PMML4_RESULT_VARIABLE, rule.getResult()));
        }
        if (rule.getReasonCodeAndValue() != null) {
            final KiePMMLReasonCodeAndValue reasonCodeAndValue = rule.getReasonCodeAndValue();
            joiner.add(String.format(ADD_OUTPUTFIELD_VALUE, reasonCodeAndValue.getReasonCode(), reasonCodeAndValue.getValue()));
        }
        if (rule.getOutputFields() != null) {
            if (rule.getResult() != null) {
                commonDeclareOutputFields(rule.getOutputFields(), rule.getResult(), joiner);
            } else if (rule.isAccumulationResult()) {
                commonDeclareOutputFields(rule.getOutputFields(), "$statusHolder.getAccumulator()", joiner);
            }
        }
    }

    protected void commonDeclareOutputFields(final List<OutputField> outputFields, final Object result, final StringJoiner joiner) {
        outputFields.forEach(kiePMMLOutputField -> {
            if (ResultFeature.PREDICTED_VALUE.equals(kiePMMLOutputField.getResultFeature())) {
                joiner.add(String.format(ADD_PMML4_OUTPUT_FIELD, kiePMMLOutputField.getName(), result));
            }
        });
    }
}
