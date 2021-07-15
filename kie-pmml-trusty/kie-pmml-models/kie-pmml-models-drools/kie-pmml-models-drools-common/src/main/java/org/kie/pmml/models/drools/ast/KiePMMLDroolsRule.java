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
package org.kie.pmml.models.drools.ast;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.dmg.pmml.OutputField;
import org.kie.pmml.api.enums.ResultCode;
import org.kie.pmml.models.drools.tuples.KiePMMLReasonCodeAndValue;

/**
 * Data-class used to store information needed to generate a <b>Drools rule</b>
 */
public class KiePMMLDroolsRule {

    // Rule name
    private final String name;
    // RHS
    private final String statusToSet;
    private final List<OutputField> outputFields;
    // LHS
    private String agendaGroup;
    private String activationGroup;
    private String statusConstraint;
    // Constraints put in and
    private List<KiePMMLFieldOperatorValue> andConstraints;
    // Constraints put in or
    private List<KiePMMLFieldOperatorValue> orConstraints;
    // Constraints put in xor
    private List<KiePMMLFieldOperatorValue> xorConstraints;
    // Constraints put in not
    private List<KiePMMLFieldOperatorValue> notConstraints;
    // Constraints put in "in"
    private Map<String, List<Object>> inConstraints;
    // Constraints put in "notIn""
    private Map<String, List<Object>> notInConstraints;
    // Used to manage compound predicates
    private String ifBreakField;
    private String ifBreakOperator;
    private Object ifBreakValue;
    // RHS
    private String focusedAgendaGroup;
    private KiePMMLReasonCodeAndValue reasonCodeAndValue;
    private ResultCode resultCode;
    private Object result;
    private Double toAccumulate;
    private boolean accumulationResult = false;

    private KiePMMLDroolsRule(String name, String statusToSet, List<OutputField> outputFields) {
        this.name = name;
        this.statusToSet = statusToSet;
        this.outputFields = outputFields;
    }

    /**
     * @param name The rule name
     * @param statusToSet The status to set in the rhs' <b>default</b> <code>then</code>; e.g.:
     * <p><code>then</code></p>
     * <p><code>$statusHolder.setStatus(<i>statusToSet</i>);</code></p>
     * </p><code>update($statusHolder);</code></p>
     * <p><b>If</b> there is a <b>break</b> statement in the lhs, then the <i>statusToSet</i>
     * will be applied differently if the node is a <i>final/leaf</i> or not.</p>
     * <p>If the node is a <i>final/leaf</i>, the <i>statusToSet</i> will be applied in the
     * default <code>then</code> directive,
     * while in the default <code>then[match]</code> it will be set as <b>"DONE"</b></p>; e.g.:
     * <p><code>then</code></p>
     * <p><code>$statusHolder.setStatus(<i>statusToSet</i>);</code></p>
     * <p><code>update($statusHolder);</code></p>
     *
     * <p><code>then[match]</code></p>
     * <p><code>$statusHolder.setStatus("DONE");</code></p>
     * <p><code>update($statusHolder);</code></p>
     *
     * <p>If the node is a <b>not</b> final/leaf</b>, the <i>statusToSet</i> will be set in the
     * <code>then[match]</code> directive,
     * while in the default <code>then</code> it will be set as <b>"DONE"</b></p>; e.g.:
     * <p><code>then</code></p>
     * <p><code>$statusHolder.setStatus("DONE");</code></p>
     * <p><code>update($statusHolder);</code></p>
     *
     * <p><code>then[match]</code></p>
     * <p><code>$statusHolder.setStatus(<i>statusToSet</i>);</code></p>
     * <p><code>update($statusHolder);</code></p>
     * @return
     */
    public static Builder builder(String name, String statusToSet, List<OutputField> outputFields) {
        return new Builder(name, statusToSet, outputFields);
    }

    public String getName() {
        return name;
    }

    /**
     * The status to set in the rhs
     * (<code>$statusHolder.setStatus("DONE");</code>
     * <code>update($statusHolder);</code>)
     * @return
     */
    public String getStatusToSet() {
        return statusToSet;
    }

    public List<OutputField> getOutputFields() {
        return outputFields;
    }

    public String getAgendaGroup() {
        return agendaGroup;
    }

    public String getActivationGroup() {
        return activationGroup;
    }

    public String getFocusedAgendaGroup() {
        return focusedAgendaGroup;
    }

    public String getStatusConstraint() {
        return statusConstraint;
    }

    public List<KiePMMLFieldOperatorValue> getAndConstraints() {
        return andConstraints != null ? Collections.unmodifiableList(andConstraints) : null;
    }

    public List<KiePMMLFieldOperatorValue> getOrConstraints() {
        return orConstraints != null ? Collections.unmodifiableList(orConstraints) : null;
    }

    public List<KiePMMLFieldOperatorValue> getXorConstraints() {
        return xorConstraints != null ? Collections.unmodifiableList(xorConstraints) : null;
    }

    public List<KiePMMLFieldOperatorValue> getNotConstraints() {
        return notConstraints != null ? Collections.unmodifiableList(notConstraints) : null;
    }

    public Map<String, List<Object>> getInConstraints() {
        return inConstraints != null ? Collections.unmodifiableMap(inConstraints) : null;
    }

    public Map<String, List<Object>> getNotInConstraints() {
        return notInConstraints != null ? Collections.unmodifiableMap(notInConstraints) : null;
    }

    public String getIfBreakField() {
        return ifBreakField;
    }

    public String getIfBreakOperator() {
        return ifBreakOperator;
    }

    public Object getIfBreakValue() {
        return ifBreakValue;
    }

    public KiePMMLReasonCodeAndValue getReasonCodeAndValue() {
        return reasonCodeAndValue;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public Object getResult() {
        return result;
    }

    /**
     * The accumulation to set in the rhs
     * (<code>$statusHolder.accumulate(_toAccumulate_);</code>
     * @return
     */
    public Double getToAccumulate() {
        return toAccumulate;
    }

    /**
     * It <code>true</code>, set the overall accumulation as final result
     * @return
     */
    public boolean isAccumulationResult() {
        return accumulationResult;
    }

    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(", ", KiePMMLDroolsRule.class.getSimpleName() + "[", "]");
        if (name != null) {
            stringJoiner.add("name='" + name + "'");
        }
        if (statusToSet != null) {
            stringJoiner.add("statusToSet='" + statusToSet + "'");
        }
        if (outputFields != null && !outputFields.isEmpty()) {
            stringJoiner.add("outputFields='" + outputFields + "'");
        }
        if (agendaGroup != null) {
            stringJoiner.add("agendaGroup='" + agendaGroup + "'");
        }
        if (activationGroup != null) {
            stringJoiner.add("activationGroup='" + activationGroup + "'");
        }
        if (statusConstraint != null) {
            stringJoiner.add("statusConstraint='" + statusConstraint + "'");
        }
        if (andConstraints != null && !andConstraints.isEmpty()) {
            stringJoiner.add("andConstraints='" + andConstraints + "'");
        }
        if (orConstraints != null && !orConstraints.isEmpty()) {
            stringJoiner.add("orConstraints='" + orConstraints + "'");
        }
        if (xorConstraints != null && !xorConstraints.isEmpty()) {
            stringJoiner.add("xorConstraints='" + xorConstraints + "'");
        }
        if (notConstraints != null && !notConstraints.isEmpty()) {
            stringJoiner.add("notConstraints='" + notConstraints + "'");
        }
        if (inConstraints != null && !inConstraints.isEmpty()) {
            stringJoiner.add("inConstraints='" + inConstraints + "'");
        }
        if (notInConstraints != null && !notInConstraints.isEmpty()) {
            stringJoiner.add("notInConstraints='" + notInConstraints + "'");
        }
        if (ifBreakField != null) {
            stringJoiner.add("ifBreakField='" + ifBreakField + "'");
        }
        if (ifBreakOperator != null) {
            stringJoiner.add("ifBreakOperator='" + ifBreakOperator + "'");
        }
        if (ifBreakValue != null) {
            stringJoiner.add("ifBreakValue='" + ifBreakValue + "'");
        }
        if (focusedAgendaGroup != null) {
            stringJoiner.add("focusedAgendaGroup='" + focusedAgendaGroup + "'");
        }
        if (resultCode != null) {
            stringJoiner.add("resultCode='" + resultCode + "'");
        }
        if (result != null) {
            stringJoiner.add("result='" + result + "'");
        }
        if (toAccumulate != null) {
            stringJoiner.add("toAccumulate='" + toAccumulate + "'");
        }
        stringJoiner.add("accumulationResult='" + accumulationResult + "'");
        return stringJoiner.toString();
    }

    public static class Builder {

        protected KiePMMLDroolsRule toBuild;

        /**
         * @param name
         * @param statusToSet
         * @param outputFields
         */
        public Builder(String name, String statusToSet, List<OutputField> outputFields) {
            this.toBuild = new KiePMMLDroolsRule(name, statusToSet, outputFields);
        }

        /**
         * The required status to fire the given rule
         * <p>
         * (lhs)
         *
         * <p><code>$statusHolder : KiePMMLStatusHolder( status == "_constraint_" )</code></p>
         * @param constraint
         * @return
         */
        public Builder withStatusConstraint(String constraint) {
            this.toBuild.statusConstraint = constraint;
            return this;
        }

        /**
         * @param constraints The <b>key</b> of the map is the name of the generated type, while the <b>value</b> is the <code>List&lt;KiePMMLOperatorValue&gt;</code>
         * to use for evaluation. Implicitly, the latter is evaluated with the <b>value</b> field of the former
         * (e.g entry <b>"OUTLOOK"/List(KiePMMLOperatorValue("==", "sunny"))</b> generates
         *
         * <p><code>OUTLOOK(value == "sunny")</code></p>)
         * <p>
         * (e.g entry <b>"TEMPERATURE"/List(KiePMMLOperatorValue("<", 90), KiePMMLOperatorValue(">", 50))</b> generates
         *
         * <p><code>TEMPERATURE( value < 90 && value > 50 )</code></p>)
         * @return
         */
        public Builder withAndConstraints(List<KiePMMLFieldOperatorValue> constraints) {
            this.toBuild.andConstraints = constraints;
            return this;
        }

        /**
         * @param constraints The <b>key</b> of the map is the name of the generated type, while the <b>value</b> is the <code>List&lt;KiePMMLOperatorValue&gt;</code>
         * to use for evaluation. Implicitly, the latter is evaluated with the <b>value</b> field of the former
         * (e.g entry <b>"OUTLOOK"/List(KiePMMLOperatorValue("==", "sunny"))</b> generates
         *
         * <p><code>OUTLOOK(value == "sunny")</code></p>)
         * <p>
         * (e.g entry <b>"TEMPERATURE"/List(KiePMMLOperatorValue("<", 90), KiePMMLOperatorValue(">", 50))</b> generates
         *
         * <p><code>TEMPERATURE( value < 90 && value > 50 )</code></p>)
         * @return
         */
        public Builder withOrConstraints(List<KiePMMLFieldOperatorValue> constraints) {
            this.toBuild.orConstraints = constraints;
            return this;
        }

        /**
         * @param constraints The <code>List&lt;KiePMMLOperatorValue&gt;</code>
         * to use for evaluation. Implicitly, the "operator" and the "value" fields are evaluated with the <b>value</b> field of the former
         * (e.g entry <b>KiePMMLOperatorValue("OUTLOOK", "==", "sunny"))</b> generates
         *
         * <p><code>OUTLOOK(value == "sunny")</code></p>)
         * <p>
         * (e.g entry <b>KiePMMLOperatorValue("TEMPERATURE", "<", 90), KiePMMLOperatorValue("TEMPERATURE",">", 50))</b> generates
         *
         * <p><code>TEMPERATURE( value < 90) TEMPERATURE( value > 50 )</code></p>)
         * @return
         */
        public Builder withXorConstraints(List<KiePMMLFieldOperatorValue> constraints) {
            this.toBuild.xorConstraints = constraints;
            return this;
        }

        /**
         * @param constraints The <b>key</b> of the map is the name of the generated type, while the <b>value</b> is the <code>List&lt;KiePMMLOperatorValue&gt;</code>
         * to use for evaluation. Implicitly, the latter is evaluated with the <b>value</b> field of the former
         * (e.g entry <b>"OUTLOOK"/List(KiePMMLOperatorValue("==", "sunny"))</b> generates
         *
         * <p><code>not(OUTLOOK(value == "sunny")</code></p>)
         * <p>
         * (e.g entry <b>"TEMPERATURE"/List(KiePMMLOperatorValue("<", 90), KiePMMLOperatorValue(">", 50))</b> generates
         * <p><code>not(TEMPERATURE( value < 90 && value > 50 )</code></p>)
         * @return
         */
        public Builder withNotConstraints(List<KiePMMLFieldOperatorValue> constraints) {
            this.toBuild.notConstraints = constraints;
            return this;
        }

        /**
         * @param constraints The <b>key</b> of the map is the name of the generated type, while the <b>value</b> is the <code>List&lt;Object&gt;</code>
         * to use for evaluation. Implicitly, the latter is evaluated with the <b>value</b> field of the former
         * (e.g entry <b>"INPUT1"/List(-5,  0.5, 1, 10)</b> generates
         *
         * <p><code>INPUT1(value in (-5,  0.5, 1, 10)</code></p>)
         * @return
         */
        public Builder withInConstraints(Map<String, List<Object>> constraints) {
            this.toBuild.inConstraints = constraints;
            return this;
        }

        /**
         * @param constraints The <b>key</b> of the map is the name of the generated type, while the <b>value</b> is the <code>List&lt;Object&gt;</code>
         * to use for evaluation. Implicitly, the latter is evaluated with the <b>value</b> field of the former
         * (e.g entry <b>"INPUT2"/List(3, 8.5)</b> generates
         *
         * <p><code>not(INPUT2(value in(3, 8.5))</code></p>)
         * @return
         */
        public Builder withNotInConstraints(Map<String, List<Object>> constraints) {
            this.toBuild.notInConstraints = constraints;
            return this;
        }

        /**
         * Add a <b>break</b> statement to the lhs of the rule
         * (e.g. ifBreakField = "SEPAL_WIDTH"; ifBreakOperator = ">="; ifBreakValue = 5.45 generates</br>
         *
         * <p><code>$inputField: SEPAL_WIDTH()</code></p>
         * <p><code>if ($inputField.getValue() >= 5.45) break[match]</code></p>
         * @param ifBreakField
         * @param ifBreakOperator
         * @param ifBreakValue
         * @return
         */
        public Builder withIfBreak(String ifBreakField, String ifBreakOperator, Object ifBreakValue) {
            this.toBuild.ifBreakField = ifBreakField;
            this.toBuild.ifBreakOperator = ifBreakOperator;
            this.toBuild.ifBreakValue = ifBreakValue;
            return this;
        }

        /**
         * Set the <b>result code</b> to be returned
         * <p>
         * (rhs)
         * <p><code>$pmml4Result.setResultCode(_resultCode_);</code></p>
         * @param resultCode
         * @return
         */
        public Builder withResultCode(ResultCode resultCode) {
            this.toBuild.resultCode = resultCode;
            return this;
        }

        /**
         * Set the <b>result</b> to be returned
         * <p>
         * (rhs)
         * <p><code>$pmml4Result.addResultVariable($pmml4Result.getResultObjectName(), _result_);</code></p>
         * @param result
         * @return
         */
        public Builder withResult(Object result) {
            this.toBuild.result = result;
            return this;
        }

        /**
         * Set the <b>Agenda Group</b> of the rule
         * <p>
         * (lhs)
         * <p><code>agenda-group "_agendaGroup_"</code></p>
         * @param agendaGroup
         * @return
         */
        public Builder withAgendaGroup(String agendaGroup) {
            this.toBuild.agendaGroup = agendaGroup;
            return this;
        }

        /**
         * Set the <b>Activation Group</b> of the rule
         * <p>
         * (lhs)
         * <p><code>activation-group "_activationGroup_"</code></p>
         * @param activationGroup
         * @return
         */
        public Builder withActivationGroup(String activationGroup) {
            this.toBuild.activationGroup = activationGroup;
            return this;
        }

        /**
         * Set the <b>AgendaGroup</b> to be focused
         * <p>
         * (rhs)
         * <p><code>kcontext.getKieRuntime().getAgenda().getAgendaGroup( "_focusedAgendaGroup_").setFocus();</code></p>
         * @param focusedAgendaGroup
         * @return
         */
        public Builder withFocusedAgendaGroup(String focusedAgendaGroup) {
            this.toBuild.focusedAgendaGroup = focusedAgendaGroup;
            return this;
        }

        /**
         * Accumulate the given number to the <code>StatusHolder</code>
         * <p>
         * (rhs)
         * <p><code>$statusHolder.accumulate("_toAccumulate_");</code></p>
         * @param toAccumulate
         * @return
         */
        public Builder withAccumulation(Number toAccumulate) {
            this.toBuild.toAccumulate =  toAccumulate != null ? toAccumulate.doubleValue() : 0;
            return this;
        }

        /**
         * If true, return the result of the overall <b>accumulation</b>
         * <p>
         * (rhs)
         * <p><code>$pmml4Result.addResultVariable($pmml4Result.getResultObjectName(), $statusHolder.getAccumulator());</code></p>
         * @param accumulationResult
         * @return
         */
        public Builder withAccumulationResult(boolean accumulationResult) {
            this.toBuild.accumulationResult = accumulationResult;
            return this;
        }

        /**
         * Add the given <b>reasonCode</b> to the ordered map of matched reason codes.
         * <p>
         * (rhs)
         * <p><code$outputFieldsMap.put("_reasonCodeAndValue.reasonCode_", "__reasonCodeAndValue.value_");</code></p>
         * @param reasonCodeAndValue
         * @return
         */
        public Builder withReasonCodeAndValue(KiePMMLReasonCodeAndValue reasonCodeAndValue) {
            this.toBuild.reasonCodeAndValue = reasonCodeAndValue;
            return this;
        }

        public KiePMMLDroolsRule build() {
            return toBuild;
        }
    }
}
