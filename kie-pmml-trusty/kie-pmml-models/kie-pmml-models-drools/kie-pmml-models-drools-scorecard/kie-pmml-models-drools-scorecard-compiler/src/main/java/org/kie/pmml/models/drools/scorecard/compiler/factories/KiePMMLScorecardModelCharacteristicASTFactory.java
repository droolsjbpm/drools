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
package org.kie.pmml.models.drools.scorecard.compiler.factories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.False;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.scorecard.Attribute;
import org.dmg.pmml.scorecard.Characteristic;
import org.dmg.pmml.scorecard.Characteristics;
import org.drools.core.util.StringUtils;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.REASONCODE_ALGORITHM;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.models.drools.ast.KiePMMLDroolsRule;
import org.kie.pmml.models.drools.ast.factories.KiePMMLAbstractModelASTFactory;
import org.kie.pmml.models.drools.ast.factories.KiePMMLPredicateASTFactory;
import org.kie.pmml.models.drools.ast.factories.PredicateASTFactoryData;
import org.kie.pmml.models.drools.tuples.KiePMMLOriginalTypeGeneratedType;
import org.kie.pmml.models.drools.tuples.KiePMMLReasonCodeAndValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.commons.Constants.DONE;
import static org.kie.pmml.models.drools.ast.factories.KiePMMLAbstractModelASTFactory.PATH_PATTERN;

/**
 * Class used to generate <code>KiePMMLDroolsRule</code>s out of a <code>Characteristic</code>
 */
public class KiePMMLScorecardModelCharacteristicASTFactory {

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLScorecardModelCharacteristicASTFactory.class.getName());

    private final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap;
    private final List<OutputField> outputFields;
    private final DATA_TYPE targetType;
    private boolean useReasonCodes = false;
    private Number baselineScore;
    private REASONCODE_ALGORITHM reasonCodeAlgorithm;

    private KiePMMLScorecardModelCharacteristicASTFactory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap, final List<OutputField> outputFields, final DATA_TYPE targetType) {
        this.fieldTypeMap = fieldTypeMap;
        this.outputFields = outputFields;
        this.targetType = targetType;
    }

    public static KiePMMLScorecardModelCharacteristicASTFactory factory(final Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap, final List<OutputField> outputFields, final DATA_TYPE targetType) {
        return new KiePMMLScorecardModelCharacteristicASTFactory(fieldTypeMap, outputFields, targetType);
    }

    public KiePMMLScorecardModelCharacteristicASTFactory withReasonCodes(Number baselineScore, REASONCODE_ALGORITHM reasonCodeAlgorithm) {
        this.useReasonCodes = true;
        this.baselineScore = baselineScore;
        this.reasonCodeAlgorithm = reasonCodeAlgorithm;
        return this;
    }

    public List<KiePMMLDroolsRule> declareRulesFromCharacteristics(final Characteristics characteristics, final String parentPath, Number initialScore) {
        logger.trace("declareRulesFromCharacteristics {} {} {}", characteristics, parentPath, initialScore);
        final List<KiePMMLDroolsRule> toReturn = new ArrayList<>();
        final List<Characteristic> characteristicList = characteristics.getCharacteristics();
        for (int i = 0; i < characteristicList.size(); i++) {
            final Characteristic characteristic = characteristicList.get(i);
            if (i == 0) {
                String statusConstraint = StringUtils.isEmpty(parentPath) ? KiePMMLAbstractModelASTFactory.STATUS_NULL : String.format(KiePMMLAbstractModelASTFactory.STATUS_PATTERN, parentPath);
                String currentRule = String.format(PATH_PATTERN, parentPath, characteristic.getName());
                KiePMMLDroolsRule.Builder builder = KiePMMLDroolsRule.builder(currentRule, currentRule, null)
                        .withStatusConstraint(statusConstraint);
                if (initialScore != null) {
                    builder = builder.withAccumulation(initialScore);
                }
                toReturn.add(builder.build());
            }
            boolean isLastCharacteristic = (i == characteristicList.size() - 1);
            String statusToSet = isLastCharacteristic ? DONE : String.format(PATH_PATTERN, parentPath, characteristicList.get(i + 1).getName());
            declareRuleFromCharacteristic(characteristic, parentPath, toReturn, statusToSet, isLastCharacteristic);
        }
        return toReturn;
    }

    protected void declareRuleFromCharacteristic(final Characteristic characteristic, final String parentPath,
                                                 final List<KiePMMLDroolsRule> rules,
                                                 final String statusToSet,
                                                 final boolean isLastCharacteristic) {
        logger.trace("declareRuleFromCharacteristic {} {}", characteristic, parentPath);
        String currentRule = String.format(PATH_PATTERN, parentPath, characteristic.getName());
        final List<Attribute> attributes = characteristic.getAttributes();
        for (int i = 0; i < attributes.size(); i++) {
            declareRuleFromAttribute(attributes.get(i), currentRule, i, rules, statusToSet, characteristic.getReasonCode(), characteristic.getBaselineScore(), isLastCharacteristic);
        }
    }

    protected void declareRuleFromAttribute(final Attribute attribute, final String parentPath,
                                            final int attributeIndex,
                                            final List<KiePMMLDroolsRule> rules,
                                            final String statusToSet,
                                            final String characteristicReasonCode,
                                            final Number characteristicBaselineScore,
                                            final boolean isLastCharacteristic) {
        logger.trace("declareRuleFromAttribute {} {}", attribute, parentPath);
        final Predicate predicate = attribute.getPredicate();
        // This means the rule should not be created at all.
        // Different semantics has to be implemented if the "False"/"True" predicates are declared inside
        // an XOR compound predicate
        if (predicate instanceof False) {
            return;
        }
        String currentRule = String.format(PATH_PATTERN, parentPath, attributeIndex);
        KiePMMLReasonCodeAndValue reasonCodeAndValue = getKiePMMLReasonCodeAndValue(attribute, characteristicReasonCode, characteristicBaselineScore);
        PredicateASTFactoryData predicateASTFactoryData = new PredicateASTFactoryData(predicate, outputFields, rules, parentPath, currentRule, fieldTypeMap);
        KiePMMLPredicateASTFactory.factory(predicateASTFactoryData).declareRuleFromPredicate(attribute.getPartialScore(), statusToSet, reasonCodeAndValue, isLastCharacteristic);
    }

    protected KiePMMLReasonCodeAndValue getKiePMMLReasonCodeAndValue(final Attribute attribute,
                                                                     final String characteristicReasonCode,
                                                                     final Number characteristicBaselineScore) {
        if (!useReasonCodes) {
            return null;
        }
        if (characteristicBaselineScore == null && baselineScore == null) {
            throw new KiePMMLException("Missing default and characteristic defined baselineScore needed for useReasonCodes == true");
        }
        if (reasonCodeAlgorithm == null) {
            throw new KiePMMLException("Missing reasonCodeAlgorithm needed for useReasonCodes == true");
        }
        String reasonCode = attribute.getReasonCode() != null && !(attribute.getReasonCode().isEmpty()) ? attribute.getReasonCode() : characteristicReasonCode;
        if (reasonCode == null || reasonCode.isEmpty()) {
            throw new KiePMMLException("Missing reasonCode needed for useReasonCodes == true");
        }
        double baseLineScoreToUse = characteristicBaselineScore != null ? characteristicBaselineScore.doubleValue() : baselineScore.doubleValue();
        double partialScoreToUse = attribute.getPartialScore() != null ? attribute.getPartialScore().doubleValue() : 0.0;
        if (REASONCODE_ALGORITHM.POINTS_BELOW.equals(reasonCodeAlgorithm)) {
            baseLineScoreToUse = baseLineScoreToUse - partialScoreToUse;
        } else {
            baseLineScoreToUse = partialScoreToUse - baseLineScoreToUse;
        }
        return new KiePMMLReasonCodeAndValue(reasonCode, baseLineScoreToUse);
    }
}
