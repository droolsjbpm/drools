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
package org.kie.pmml.compiler.commons.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dmg.pmml.Array;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Interval;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ParameterField;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;
import org.dmg.pmml.Targets;
import org.dmg.pmml.TransformationDictionary;
import org.dmg.pmml.Value;
import org.kie.pmml.api.enums.CAST_INTEGER;
import org.kie.pmml.api.enums.DATA_TYPE;
import org.kie.pmml.api.enums.FIELD_USAGE_TYPE;
import org.kie.pmml.api.enums.OP_TYPE;
import org.kie.pmml.api.enums.RESULT_FEATURE;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.api.exceptions.KiePMMLInternalException;
import org.kie.pmml.commons.model.KiePMMLTarget;
import org.kie.pmml.commons.model.KiePMMLTargetValue;
import org.kie.pmml.commons.model.tuples.KiePMMLNameOpType;

import static org.kie.pmml.api.utils.PrimitiveBoxedUtils.getKiePMMLPrimitiveBoxed;

/**
 * Class to provide common methods to interact with <code>Model</code>, to convert <b>org.dmn.pmml</b> objects to
 * <b>Kie</b> ones, etc...
 */
public class ModelUtils {

    private ModelUtils() {
    }

    /**
     * Return an <code>Optional</code> with the name of the field whose <b>usageType</b> is <code>TARGET</code> or
     * <code>PREDICTED</code>
     * <p>
     * While the xsd schema does not strictly enforce this, it seems that <b>by convention</b> majority of models has
     * only one target.
     * <p>
     * (see https://github.com/jpmml/jpmml-evaluator/issues/64 discussion)
     * @param model
     * @return
     */
    public static Optional<String> getTargetFieldName(DataDictionary dataDictionary, Model model) {
        return getTargetFields(dataDictionary, model).stream().map(KiePMMLNameOpType::getName).findFirst();
    }

    /**
     * Return the <code>DATA_TYPE</code>> of the field whose <b>usageType</b> is <code>TARGET</code> or
     * <code>PREDICTED</code>.
     * It throws exception if none of such fields are found
     * <p>
     * While the xsd schema does not strictly enforce this, it seems that <b>by convention</b> majority of models has
     * only one target.
     * <p>
     * (see https://github.com/jpmml/jpmml-evaluator/issues/64 discussion)
     * @param dataDictionary
     * @param model
     * @return
     */
    public static DATA_TYPE getTargetFieldType(DataDictionary dataDictionary, Model model) {
        return getTargetFieldsTypeMap(dataDictionary, model).entrySet().iterator().next().getValue();
    }

    /**
     * Return a <code>List&lt;KiePMMLNameOpType&gt;</code> of target fields
     * Please note that only <b>predicted/target</b>
     * <code>MiningField</code> are considered.
     * @param dataDictionary
     * @param model
     * @return
     */
    public static List<KiePMMLNameOpType> getTargetFields(DataDictionary dataDictionary, Model model) {
        List<KiePMMLNameOpType> toReturn = new ArrayList<>();
        if (model.getMiningSchema() != null && model.getMiningSchema().getMiningFields() != null) {
            for (MiningField miningField : model.getMiningSchema().getMiningFields()) {
                if (MiningField.UsageType.TARGET.equals(miningField.getUsageType()) || MiningField.UsageType.PREDICTED.equals(miningField.getUsageType())) {
                    OP_TYPE opType = getOpType(dataDictionary, model, miningField.getName().getValue());
                    toReturn.add(new KiePMMLNameOpType(miningField.getName().getValue(), opType));
                }
            }
        }
        return toReturn;
    }

    /**
     * Returns a <code>Map&lt;String, DATA_TYPE&gt;</code> of target fields, where the key is the name of the field,
     * and the value is the <b>type</b> of the field
     * Please note that only <b>predicted/target</b>
     * <code>MiningField</code> are considered.
     * @param dataDictionary
     * @param model
     * @return
     */
    public static Map<String, DATA_TYPE> getTargetFieldsTypeMap(DataDictionary dataDictionary, Model model) {
        Map<String, DATA_TYPE> toReturn = new LinkedHashMap<>();
        if (model.getMiningSchema() != null && model.getMiningSchema().getMiningFields() != null) {
            for (MiningField miningField : model.getMiningSchema().getMiningFields()) {
                if (MiningField.UsageType.TARGET.equals(miningField.getUsageType()) || MiningField.UsageType.PREDICTED.equals(miningField.getUsageType())) {
                    toReturn.put(miningField.getName().getValue(), getDataType(dataDictionary,
                                                                               miningField.getName().getValue()));
                }
            }
        }
        return toReturn;
    }

    /**
     * <code>OP_TYPE</code> may be defined inside <code>DataField</code>, <code>MiningField</code> or both.
     * In the latter case, <code>MiningField</code> override <code>DataField</code> definition
     * @param dataDictionary
     * @param model
     * @param targetFieldName
     * @return
     */
    public static OP_TYPE getOpType(DataDictionary dataDictionary, Model model, String targetFieldName) {
        return Stream.of(getOpTypeFromTargets(model.getTargets(), targetFieldName),
                         getOpTypeFromMiningFields(model.getMiningSchema(), targetFieldName),
                         getOpTypeFromDataDictionary(dataDictionary, targetFieldName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new KiePMMLInternalException(String.format("Failed to find OpType for field" +
                                                                                      " %s", targetFieldName)));
    }

    /**
     * Return <code>Optional&lt;OP_TYPE&gt;</code> of field with given <b>fieldName</b> from <code>DataDictionary</code>
     * @param dataDictionary
     * @param fieldName
     * @return
     */
    public static Optional<OP_TYPE> getOpTypeFromDataDictionary(DataDictionary dataDictionary, String fieldName) {
        if (dataDictionary != null && dataDictionary.getDataFields() != null) {
            return dataDictionary.getDataFields().stream()
                    .filter(dataField -> Objects.equals(fieldName, dataField.getName().getValue()) && dataField.getOpType() != null)
                    .findFirst()
                    .map(dataField -> OP_TYPE.byName(dataField.getOpType().value()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Return <code>Optional&lt;OP_TYPE&gt;</code> of field with given <b>fieldName</b> from <code>MiningSchema</code>
     * @param miningSchema
     * @param fieldName
     * @return
     */
    public static Optional<OP_TYPE> getOpTypeFromMiningFields(MiningSchema miningSchema, String fieldName) {
        if (miningSchema != null && miningSchema.getMiningFields() != null) {
            return miningSchema.getMiningFields().stream()
                    .filter(miningField -> Objects.equals(fieldName, miningField.getName().getValue()) && miningField.getOpType() != null)
                    .findFirst()
                    .map(dataField -> OP_TYPE.byName(dataField.getOpType().value()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Return <code>Optional&lt;OP_TYPE&gt;</code> of field with given <b>fieldName</b> from <code>Targets</code>
     * @param targets
     * @param fieldName
     * @return
     */
    public static Optional<OP_TYPE> getOpTypeFromTargets(Targets targets, String fieldName) {
        if (targets != null && targets.getTargets() != null) {
            return targets.getTargets().stream()
                    .filter(target -> Objects.equals(fieldName, target.getField().getValue()) && target.getOpType() != null)
                    .findFirst()
                    .map(dataField -> OP_TYPE.byName(dataField.getOpType().value()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * <code>DataType</code> of the given <b>field</b>, first looked upon <b>derivedFields</b> and then in
     * <b>dataDictionary</b>
     * @param derivedFields
     * @param dataDictionary
     * @param fieldName
     * @return
     */
    public static DataType getDataType(final List<DerivedField> derivedFields,
                                       final DataDictionary dataDictionary,
                                       final String fieldName) {
        return Stream.of(getDataTypeFromDerivedFields(derivedFields, fieldName),
                         getDataTypeFromDataDictionary(dataDictionary, fieldName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new KiePMMLInternalException(String.format("Failed to find DataType for " +
                                                                                      "field %s",
                                                                              fieldName)));
    }

    /**
     * <code>DATA_TYPE</code> of the given <b>field</b>
     * @param dataDictionary
     * @param targetFieldName
     * @return
     */
    public static DATA_TYPE getDataType(DataDictionary dataDictionary, String targetFieldName) {
        Optional<DATA_TYPE> toReturn = dataDictionary.getDataFields().stream()
                .filter(dataField -> Objects.equals(targetFieldName, dataField.getName().getValue()))
                .findFirst()
                .map(dataField -> DATA_TYPE.byName(dataField.getDataType().value()));
        return toReturn.orElseThrow(() -> new KiePMMLInternalException(String.format("Failed to find DataType for " +
                                                                                             "field %s",
                                                                                     targetFieldName)));
    }

    /**
     * Return <code>List&lt;DerivedField&gt;</code>s from the given <code>TransformationDictionary</code> and
     * <code>LocalTransformations</code>
     * @param transformationDictionary
     * @param localTransformations
     * @return
     */
    public static List<DerivedField> getDerivedFields(final TransformationDictionary transformationDictionary,
                                                      final LocalTransformations localTransformations) {
        final List<DerivedField> toReturn = new ArrayList<>();
        if (transformationDictionary != null && transformationDictionary.getDerivedFields() != null) {
            toReturn.addAll(transformationDictionary.getDerivedFields());
        }
        if (localTransformations != null && localTransformations.getDerivedFields() != null) {
            toReturn.addAll(localTransformations.getDerivedFields());
        }
        return toReturn;
    }


    public static List<Object> getObjectsFromArray(Array source) {
        Array.Type type = source.getType();
        List<Object> toReturn = new ArrayList<>();
        String stringValue = (String) source.getValue();
        String[] valuesArray = stringValue.split(" ");
        for (String s : valuesArray) {
            switch (type) {
                case INT:
                    toReturn.add(Integer.valueOf(s));
                    break;
                case STRING:
                    toReturn.add(s);
                    break;
                case REAL:
                    toReturn.add(Double.valueOf(s));
                    break;
                default:
                    throw new KiePMMLException("Unknown Array " + type);
            }
        }
        return toReturn;
    }

    /**
     * Return a <code>List&lt;org.kie.pmml.api.models.MiningField&glt;</code> out of a <code>org.dmg.pmml
     * .MiningSchema</code> one
     * @param toConvert
     * @param dataDictionary
     * @return
     */
    public static List<org.kie.pmml.api.models.MiningField> convertToKieMiningFieldList(final MiningSchema toConvert,
                                                                                        final DataDictionary dataDictionary) {
        if (toConvert == null) {
            return Collections.emptyList();
        }
        return toConvert.getMiningFields()
                .stream()
                .map(miningField -> {
                    DataField dataField = dataDictionary.getDataFields().stream()
                            .filter(df -> df.getName().equals(miningField.getName()))
                            .findFirst()
                            .orElseThrow(() -> new KiePMMLException("Cannot find " + miningField.getName() + " in " +
                                                                            "DataDictionary"));
                    return convertToKieMiningField(miningField, dataField);
                })
                .collect(Collectors.toList());
    }

    /**
     * Return a <code>org.kie.pmml.api.models.MiningField</code> out of a <code>org.dmg.pmml.MiningField</code> and
     * relative <code>org.dmg.pmml.DataField</code> ones
     * @param toConvert
     * @param dataField
     * @return
     */
    public static org.kie.pmml.api.models.MiningField convertToKieMiningField(final MiningField toConvert,
                                                                              final DataField dataField) {
        final String name = toConvert.getName() != null ? toConvert.getName().getValue() : null;
        final FIELD_USAGE_TYPE fieldUsageType = toConvert.getUsageType() != null ?
                FIELD_USAGE_TYPE.byName(toConvert.getUsageType().value()) : null;
        final OP_TYPE opType = toConvert.getOpType() != null ? OP_TYPE.byName(toConvert.getOpType().value()) : null;
        final DATA_TYPE dataType = dataField.getDataType() != null ?
                DATA_TYPE.byName(dataField.getDataType().value()) : null;
        final String missingValueReplacement = toConvert.getMissingValueReplacement() != null ?
                toConvert.getMissingValueReplacement().toString() : null;
        final List<String> allowedValues = convertDataFieldValues(dataField.getValues());
        final List<org.kie.pmml.api.models.Interval> intervals = convertDataFieldIntervals(dataField.getIntervals());
        return new org.kie.pmml.api.models.MiningField(name,
                                                       fieldUsageType,
                                                       opType,
                                                       dataType,
                                                       missingValueReplacement,
                                                       allowedValues,
                                                       intervals);
    }

    /**
     * Return a <code>List&lt;org.kie.pmml.api.models.OutputField&gt;</code> out of a <code>org.dmg.pmml
     * .Output</code> one
     * @param toConvert
     * @return
     */
    public static List<org.kie.pmml.api.models.OutputField> convertToKieOutputFieldList(final Output toConvert,
                                                                                        final DataDictionary dataDictionary) {
        if (toConvert == null) {
            return Collections.emptyList();
        }
        return toConvert.getOutputFields()
                .stream()
                .map(outputField -> {
                    DataField dataField = dataDictionary.getDataFields().stream()
                            .filter(df -> df.getName().equals(outputField.getTargetField()))
                            .findFirst()
                            .orElse(null); // expected to be null because OutputField.targetField is not mandatory
                    return convertToKieOutputField(outputField, dataField);
                })
                .collect(Collectors.toList());
    }

    /**
     * Return a <code>org.kie.pmml.api.models.OutputField</code> out of a <code>org.dmg.pmml.OutputField</code> one
     * @param toConvert
     * @param dataField - this may be <code>null</code>
     * @return
     */
    public static org.kie.pmml.api.models.OutputField convertToKieOutputField(final OutputField toConvert,
                                                                              final DataField dataField) {
        final String name = toConvert.getName() != null ? toConvert.getName().getValue() : null;
        final OP_TYPE opType = toConvert.getOpType() != null ? OP_TYPE.byName(toConvert.getOpType().value()) : null;
        final DATA_TYPE dataFieldDataType = dataField != null ? DATA_TYPE.byName(dataField.getDataType().value()) :
                null;
        final DATA_TYPE dataType = toConvert.getDataType() != null ?
                DATA_TYPE.byName(toConvert.getDataType().value()) : dataFieldDataType;
        final String targetField = toConvert.getTargetField() != null ? toConvert.getTargetField().getValue() : null;
        final RESULT_FEATURE resultFeature = toConvert.getResultFeature() != null ?
                RESULT_FEATURE.byName(toConvert.getResultFeature().value()) : null;
        final List<String> allowedValues = dataField != null ? convertDataFieldValues(dataField.getValues()) : null;
        return new org.kie.pmml.api.models.OutputField(name,
                                                       opType,
                                                       dataType,
                                                       targetField,
                                                       resultFeature,
                                                       allowedValues);
    }

    /**
     * Return a <code>List&lt;org.kie.pmml.commons.model.KiePMMLTarget&gt;</code> out of a <code>org.dmg.pmml
     * .Targets</code>
     * @param toConvert
     * @return
     */
    public static List<KiePMMLTarget> convertToKiePMMLTargetList(Targets toConvert) {
        if (toConvert == null) {
            return Collections.emptyList();
        }
        return toConvert.getTargets()
                .stream()
                .map(ModelUtils::convertToKiePMMLTarget)
                .collect(Collectors.toList());
    }

    /**
     * Return a <code>org.kie.pmml.commons.model.KiePMMLTarget</code> out of a <code>org.dmg.pmml.Target</code>
     * @param toConvert
     * @return
     */
    public static KiePMMLTarget convertToKiePMMLTarget(final Target toConvert) {
        final KiePMMLTarget.Builder builder = KiePMMLTarget.builder("" + toConvert.hashCode(), Collections.emptyList());
        final List<KiePMMLTargetValue> targetValues = convertToKiePMMLTargetValueList(toConvert.getTargetValues());
        if (!targetValues.isEmpty()) {
            builder.withTargetValues(targetValues);
        }
        final OP_TYPE opType = toConvert.getOpType() != null ? OP_TYPE.byName(toConvert.getOpType().value()) : null;
        if (opType != null) {
            builder.withOpType(opType);
        }
        final String field = toConvert.getField() != null ? toConvert.getField().getValue() : null;
        if (field != null) {
            builder.withField(field);
        }
        final CAST_INTEGER castInteger = toConvert.getCastInteger() != null ?
                CAST_INTEGER.byName(toConvert.getCastInteger().value()) : null;
        if (castInteger != null) {
            builder.withCastInteger(castInteger);
        }
        final Double min = toConvert.getMin() != null ? toConvert.getMin().doubleValue() : null;
        if (min != null) {
            builder.withMin(min);
        }
        final Double max = toConvert.getMax() != null ? toConvert.getMax().doubleValue() : null;
        if (max != null) {
            builder.withMax(max);
        }
        final Double rescaleConstant = toConvert.getRescaleConstant() != null ?
                toConvert.getRescaleConstant().doubleValue() : null;
        if (rescaleConstant != null) {
            builder.withRescaleConstant(rescaleConstant);
        }
        final Double rescaleFactor = toConvert.getRescaleFactor() != null ? toConvert.getRescaleFactor().doubleValue() :
                null;
        if (rescaleFactor != null) {
            builder.withRescaleFactor(rescaleFactor);
        }
        return builder.build();
    }

    /**
     * Return a <code>List&lt;org.kie.pmml.commons.model.KiePMMLTargetValue&gt;</code> out of a
     * <code>List&lt;org.dmg.pmml.TargetValue&gt;</code>
     * @param toConvert
     * @return
     */
    public static List<KiePMMLTargetValue> convertToKiePMMLTargetValueList(List<TargetValue> toConvert) {
        if (toConvert == null) {
            return Collections.emptyList();
        }
        return toConvert
                .stream()
                .map(ModelUtils::convertToKiePMMLTargetValue)
                .collect(Collectors.toList());
    }

    /**
     * Return a <code>org.kie.pmml.commons.model.KiePMMLTargetValue</code> out of a <code>org.dmg.pmml
     * .TargetValue</code>
     * @param toConvert
     * @return
     */
    public static KiePMMLTargetValue convertToKiePMMLTargetValue(final TargetValue toConvert) {
        final String value = toConvert.getValue() != null ? toConvert.getValue().toString() : null;
        final String displayValue = toConvert.getDisplayValue() != null ? toConvert.getDisplayValue() : null;
        final KiePMMLTargetValue.Builder builder = KiePMMLTargetValue.builder("" + toConvert.hashCode(),
                                                                              Collections.emptyList())
                .withValue(value)
                .withDisplayValue(displayValue)
                .withPriorProbability(toConvert.getPriorProbability())
                .withDefaultValue(toConvert.getDefaultValue());
        return builder.build();
    }

    /**
     * Retrieve the <b>mapped</b> class name of the given <code>ParameterField</code>, <b>eventually</b> boxed (for
     * primitive ones)
     * It returns <b>Object</b> <code>ParameterField.getDataType()</code> is null
     * @param parameterField
     * @return
     */
    public static String getBoxedClassName(ParameterField parameterField) {
        return parameterField.getDataType() == null ? Object.class.getName() :
                getBoxedClassName(parameterField.getDataType());
    }

    /**
     * Retrieve the <b>mapped</b> class name of the given <code>DataType</code>, <b>eventually</b> boxed (for
     * primitive ones).
     * It returns <b>Object</b> if null
     * @param dataType
     * @return
     */
    public static String getBoxedClassName(DataType dataType) {
        Class<?> c = dataType == null ? Object.class : DATA_TYPE.byName(dataType.value()).getMappedClass();
        return getKiePMMLPrimitiveBoxed(c).map(primitiveBoxed -> primitiveBoxed.getBoxed().getName()).orElse(c.getName());
    }

    /**
     * <code>Optional&lt;DataType&gt;</code> of the given <b>field</b>
     * @param dataDictionary
     * @param fieldName
     * @return
     */
    static Optional<DataType> getDataTypeFromDataDictionary(DataDictionary dataDictionary, String fieldName) {
        return (dataDictionary != null && dataDictionary.getDataFields() != null) ?
                dataDictionary.getDataFields().stream()
                        .filter(dataField -> Objects.equals(fieldName, dataField.getName().getValue()))
                        .findFirst()
                        .map(DataField::getDataType) : Optional.empty();
    }

    /**
     * <code>Optional&lt;DataType&gt;</code> of the given <b>field</b>
     * @param derivedFields
     * @param fieldName
     * @return
     */
    static Optional<DataType> getDataTypeFromDerivedFields(List<DerivedField> derivedFields, String fieldName) {
        return derivedFields.stream()
                .filter(derivedField -> Objects.equals(fieldName, derivedField.getName().getValue()))
                .map(DerivedField::getDataType)
                .findFirst();
    }

    static List<String> convertDataFieldValues(List<Value> toConvert) {
        return toConvert != null ? toConvert.stream()
                .map(value -> value.getValue().toString())
                .collect(Collectors.toList()) : null;
    }

    static List<org.kie.pmml.api.models.Interval> convertDataFieldIntervals(List<Interval> toConvert) {
        return toConvert != null ? toConvert.stream()
                .map(interval -> new org.kie.pmml.api.models.Interval(interval.getLeftMargin(),
                                                                      interval.getRightMargin()))
                .collect(Collectors.toList()) : null;
    }
}
