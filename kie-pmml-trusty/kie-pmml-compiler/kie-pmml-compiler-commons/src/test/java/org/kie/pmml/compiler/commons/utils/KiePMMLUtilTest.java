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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.RandomStringUtils;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.kie.pmml.compiler.commons.utils.KiePMMLUtil.MODELNAME_TEMPLATE;
import static org.kie.pmml.compiler.commons.utils.KiePMMLUtil.SEGMENTID_TEMPLATE;
import static org.kie.pmml.compiler.commons.utils.KiePMMLUtil.SEGMENTMODELNAME_TEMPLATE;
import static org.kie.pmml.compiler.commons.utils.KiePMMLUtil.TARGETFIELD_TEMPLATE;
import static org.kie.pmml.compiler.commons.utils.KiePMMLUtil.getMiningTargetFields;
import static org.kie.test.util.filesystem.FileUtils.getFileInputStream;

public class KiePMMLUtilTest {

    private static final String NO_MODELNAME_SAMPLE_NAME = "NoModelNameSample.pmml";
    private static final String NO_MODELNAME_NO_SEGMENTID_SAMPLE_NAME = "NoModelNameNoSegmentIdSample.pmml";
    private static final String NO_MODELNAME_NO_SEGMENT_ID_NOSEGMENT_TARGET_FIELD_SAMPLE =
            "NoModelNameNoSegmentIdNoSegmentTargetFieldSample.pmml";
    private static final String NO_TARGET_FIELD_SAMPLE = "NoTargetFieldSample.pmml";
    private static final String NO_OUTPUT_FIELD_TARGET_NAME_SAMPLE = "NoOutputFieldTargetNameSample.pmml";
    private static final String MINING_WITH_SAME_NESTED_MODEL_NAMES = "MiningWithSameNestedModelNames.pmml";

    @Test
    public void loadString() throws IOException, JAXBException, SAXException {
        commonLoadString(NO_MODELNAME_SAMPLE_NAME);
        commonLoadString(NO_MODELNAME_NO_SEGMENTID_SAMPLE_NAME);
        commonLoadString(MINING_WITH_SAME_NESTED_MODEL_NAMES);
    }

    @Test
    public void loadFile() throws JAXBException, IOException, SAXException {
        commonLoadFile(NO_MODELNAME_SAMPLE_NAME);
        commonLoadFile(NO_MODELNAME_NO_SEGMENTID_SAMPLE_NAME);
        commonLoadFile(MINING_WITH_SAME_NESTED_MODEL_NAMES);
    }

    @Test
    public void populateMissingModelName() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_MODELNAME_SAMPLE_NAME);
        final PMML pmml = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model toPopulate = pmml.getModels().get(0);
        assertNull(toPopulate.getModelName());
        KiePMMLUtil.populateMissingModelName(toPopulate, NO_MODELNAME_SAMPLE_NAME, 0);
        assertNotNull(toPopulate.getModelName());
        String expected = String.format(MODELNAME_TEMPLATE,
                                        NO_MODELNAME_SAMPLE_NAME,
                                        toPopulate.getClass().getSimpleName(),
                                        0);
        assertEquals(expected, toPopulate.getModelName());
    }

    @Test
    public void populateMissingMiningTargetField() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_TARGET_FIELD_SAMPLE);
        final PMML pmml = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model toPopulate = pmml.getModels().get(0);
        List<MiningField> miningTargetFields = getMiningTargetFields(toPopulate.getMiningSchema().getMiningFields());
        assertTrue(miningTargetFields.isEmpty());
        assertNull(toPopulate.getTargets().getTargets().get(0).getField());
        KiePMMLUtil.populateMissingMiningTargetField(toPopulate, pmml.getDataDictionary().getDataFields());
        miningTargetFields = getMiningTargetFields(toPopulate.getMiningSchema().getMiningFields());
        assertEquals(1, miningTargetFields.size());
        final MiningField targetField = miningTargetFields.get(0);
        assertTrue(pmml.getDataDictionary()
                           .getDataFields()
                           .stream()
                           .anyMatch(dataField -> dataField.getName().equals(targetField.getName())));
        assertEquals(targetField.getName(), toPopulate.getTargets().getTargets().get(0).getField());
    }

    @Test
    public void populateMissingPredictedOutputFieldTarget() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_OUTPUT_FIELD_TARGET_NAME_SAMPLE);
        final PMML pmml = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model toPopulate = pmml.getModels().get(0);
        final OutputField outputField = toPopulate.getOutput().getOutputFields().get(0);
        assertEquals(ResultFeature.PREDICTED_VALUE, outputField.getResultFeature());
        assertNull(outputField.getTargetField());
        KiePMMLUtil.populateMissingPredictedOutputFieldTarget(toPopulate);
        final MiningField targetField = getMiningTargetFields(toPopulate.getMiningSchema().getMiningFields()).get(0);
        assertNotNull(outputField.getTargetField());
        assertEquals(targetField.getName(), outputField.getTargetField());
    }

    @Test
    public void getTargetDataField() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_TARGET_FIELD_SAMPLE);
        final PMML pmml = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model model = pmml.getModels().get(0);
        Optional<DataField> optionalDataField = KiePMMLUtil.getTargetDataField(model);
        assertTrue(optionalDataField.isPresent());
        DataField retrieved = optionalDataField.get();
        String expected= String.format(TARGETFIELD_TEMPLATE, "golfing");
        assertEquals(expected, retrieved.getName().getValue());
    }

    @Test
    public void getTargetDataType()  {
        MiningFunction miningFunction = MiningFunction.REGRESSION;
        MathContext mathContext = MathContext.DOUBLE;
        DataType retrieved = KiePMMLUtil.getTargetDataType(miningFunction, mathContext);
        assertEquals(DataType.DOUBLE, retrieved);
        mathContext = MathContext.FLOAT;
        retrieved = KiePMMLUtil.getTargetDataType(miningFunction, mathContext);
        assertEquals(DataType.FLOAT, retrieved);
        miningFunction = MiningFunction.CLASSIFICATION;
        retrieved = KiePMMLUtil.getTargetDataType(miningFunction, mathContext);
        assertEquals(DataType.STRING, retrieved);
        miningFunction = MiningFunction.CLUSTERING;
        retrieved = KiePMMLUtil.getTargetDataType(miningFunction, mathContext);
        assertEquals(DataType.STRING, retrieved);
        List<MiningFunction> notMappedMiningFunctions = Arrays.asList(MiningFunction.ASSOCIATION_RULES,
                                                                      MiningFunction.MIXED,
                                                                      MiningFunction.SEQUENCES,
                                                                      MiningFunction.TIME_SERIES);

        notMappedMiningFunctions.forEach(minFun -> assertNull(KiePMMLUtil.getTargetDataType(minFun, MathContext.DOUBLE)));
    }

    @Test
    public void getTargetOpType()  {
        MiningFunction miningFunction = MiningFunction.REGRESSION;
        OpType retrieved = KiePMMLUtil.getTargetOpType(miningFunction);
        assertEquals(OpType.CONTINUOUS, retrieved);
        miningFunction = MiningFunction.CLASSIFICATION;
        retrieved = KiePMMLUtil.getTargetOpType(miningFunction);
        assertEquals(OpType.CATEGORICAL, retrieved);
        miningFunction = MiningFunction.CLUSTERING;
        retrieved = KiePMMLUtil.getTargetOpType(miningFunction);
        assertEquals(OpType.CATEGORICAL, retrieved);
        List<MiningFunction> notMappedMiningFunctions = Arrays.asList(MiningFunction.ASSOCIATION_RULES,
                                                                      MiningFunction.MIXED,
                                                                      MiningFunction.SEQUENCES,
                                                                      MiningFunction.TIME_SERIES);

        notMappedMiningFunctions.forEach(minFun -> assertNull(KiePMMLUtil.getTargetOpType(minFun)));
    }

    @Test
    public void getTargetMiningField() {
        final DataField dataField = new DataField();
        dataField.setName(FieldName.create("FIELD_NAME"));
        final MiningField retrieved = KiePMMLUtil.getTargetMiningField(dataField);
        assertEquals(dataField.getName().getValue(), retrieved.getName().getValue());
        assertEquals(MiningField.UsageType.TARGET, retrieved.getUsageType());
    }

    @Test
    public void correctTargetFields() {
        final MiningField miningField = new MiningField(FieldName.create("FIELD_NAME"));
        final Targets targets = new Targets();
        final Target namedTarget = new Target();
        String targetName ="TARGET_NAME";
        namedTarget.setField(FieldName.create(targetName));
        final Target unnamedTarget = new Target();
        targets.addTargets(namedTarget, unnamedTarget);
        KiePMMLUtil.correctTargetFields(miningField, targets);
        assertEquals(targetName, namedTarget.getField().getValue());
        assertEquals(miningField.getName(), unnamedTarget.getField());
    }

    @Test
    public void populateCorrectMiningModel() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_MODELNAME_NO_SEGMENT_ID_NOSEGMENT_TARGET_FIELD_SAMPLE);
        final PMML pmml = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model retrieved = pmml.getModels().get(0);
        assertTrue(retrieved instanceof MiningModel);
        MiningModel miningModel = (MiningModel) retrieved;
        miningModel.getSegmentation().getSegments().forEach(segment -> {
            assertNull(segment.getId());
            assertNull(segment.getModel().getModelName());
            assertTrue(getMiningTargetFields(segment.getModel().getMiningSchema()).isEmpty());
        });
        KiePMMLUtil.populateCorrectMiningModel(miningModel);
        miningModel.getSegmentation().getSegments().forEach(segment -> {
            assertNotNull(segment.getId());
            assertNotNull(segment.getModel().getModelName());
            assertFalse(getMiningTargetFields(segment.getModel().getMiningSchema()).isEmpty());
        });
    }

    @Test
    public void populateCorrectSegmentId() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_MODELNAME_NO_SEGMENTID_SAMPLE_NAME);
        final PMML pmml = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model retrieved = pmml.getModels().get(0);
        assertTrue(retrieved instanceof MiningModel);
        MiningModel miningModel = (MiningModel) retrieved;
        Segment toPopulate = miningModel.getSegmentation().getSegments().get(0);
        assertNull(toPopulate.getId());
        String modelName = "MODEL_NAME";
        int i = 0;
        KiePMMLUtil.populateCorrectSegmentId(toPopulate, modelName, i);
        assertNotNull(toPopulate.getId());
        String expected = String.format(SEGMENTID_TEMPLATE,
                                        modelName,
                                        i);
        assertEquals(expected, toPopulate.getId());
    }

    @Test
    public void populateMissingSegmentModelName() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_MODELNAME_NO_SEGMENTID_SAMPLE_NAME);
        final PMML pmml = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model retrieved = pmml.getModels().get(0);
        assertTrue(retrieved instanceof MiningModel);
        MiningModel miningModel = (MiningModel) retrieved;
        Model toPopulate = miningModel.getSegmentation().getSegments().get(0).getModel();
        assertNull(toPopulate.getModelName());
        String segmentId = "SEG_ID";
        KiePMMLUtil.populateMissingSegmentModelName(toPopulate, segmentId);
        assertNotNull(toPopulate.getModelName());
        String expected = String.format(SEGMENTMODELNAME_TEMPLATE,
                                        segmentId,
                                        toPopulate.getClass().getSimpleName());
        assertEquals(expected, toPopulate.getModelName());
    }

    @Test
    public void populateMissingTargetFieldInSegment() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_MODELNAME_NO_SEGMENT_ID_NOSEGMENT_TARGET_FIELD_SAMPLE);
        final PMML pmml = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model retrieved = pmml.getModels().get(0);
        assertTrue(retrieved instanceof MiningModel);
        MiningModel miningModel = (MiningModel) retrieved;
        Model toPopulate = miningModel.getSegmentation().getSegments().get(0).getModel();
        assertTrue(getMiningTargetFields(toPopulate.getMiningSchema()).isEmpty());
        KiePMMLUtil.populateMissingTargetFieldInSegment(retrieved.getMiningSchema(), toPopulate);
        List<MiningField> childrenTargetFields = getMiningTargetFields(toPopulate.getMiningSchema());
        assertFalse(childrenTargetFields.isEmpty());
        getMiningTargetFields(miningModel.getMiningSchema()).forEach(parentTargetField -> assertTrue(childrenTargetFields.contains(parentTargetField)));
    }


    @Test
    public void populateMissingOutputFieldDataType() {
        Random random = new Random();
        List<String> fieldNames = IntStream.range(0, 6)
                .mapToObj(i -> RandomStringUtils.random(6, true, false))
                .collect(Collectors.toList());
        List<DataField> dataFields = fieldNames.stream()
                .map(fieldName -> {
                    DataField toReturn = new DataField();
                    toReturn.setName(FieldName.create(fieldName));
                    DataType dataType = DataType.values()[random.nextInt(DataType.values().length)];
                    toReturn.setDataType(dataType);
                    return toReturn;
                })
                .collect(Collectors.toList());
        List<MiningField> miningFields = IntStream.range(0, dataFields.size() - 1)
                .mapToObj(dataFields::get)
                .map(dataField -> {
                    MiningField toReturn = new MiningField();
                    toReturn.setName(FieldName.create(dataField.getName().getValue()));
                    toReturn.setUsageType(MiningField.UsageType.ACTIVE);
                    return toReturn;
                })
                .collect(Collectors.toList());
        DataField lastDataField = dataFields.get(dataFields.size() - 1);
        MiningField targetMiningField = new MiningField();
        targetMiningField.setName(FieldName.create(lastDataField.getName().getValue()));
        targetMiningField.setUsageType(MiningField.UsageType.TARGET);
        miningFields.add(targetMiningField);
        // Following OutputFields should be populated based on "ResultFeature.PROBABILITY"
        List<OutputField> outputFields = IntStream.range(0, 3)
                .mapToObj(i -> {
                    OutputField toReturn = new OutputField();
                    toReturn.setName(FieldName.create(RandomStringUtils.random(6, true, false)));
                    toReturn.setResultFeature(ResultFeature.PROBABILITY);
                    return toReturn;
                })
                .collect(Collectors.toList());
        // Following OutputField should be populated based on "ResultFeature.PREDICTED_VALUE"
        OutputField targetOutputField = new OutputField();
        targetOutputField.setName(FieldName.create(RandomStringUtils.random(6, true, false)));
        targetOutputField.setResultFeature(ResultFeature.PREDICTED_VALUE);
        outputFields.add(targetOutputField);
        // Following OutputField should be populated based on "TargetField" property
        OutputField targetingOutputField = new OutputField();
        targetingOutputField.setName(FieldName.create(RandomStringUtils.random(6, true, false)));
        targetingOutputField.setTargetField(FieldName.create(targetMiningField.getName().getValue()));
        outputFields.add(targetingOutputField);
        outputFields.forEach(outputField -> assertNull(outputField.getDataType()));
        IntStream.range(0, 2)
                .forEach(i -> {
                    OutputField toAdd = new OutputField();
                    toAdd.setName(FieldName.create(RandomStringUtils.random(6, true, false)));
                    DataType dataType = DataType.values()[random.nextInt(DataType.values().length)];
                    toAdd.setDataType(dataType);
                    outputFields.add(toAdd);
                });
        KiePMMLUtil.populateMissingOutputFieldDataType(outputFields, miningFields, dataFields);
        outputFields.forEach(outputField -> assertNotNull(outputField.getDataType()));
    }

    @Test
    public void getSanitizedId() {
        final String modelName = "MODEL_NAME";
        String id = "2";
        String expected = String.format(SEGMENTID_TEMPLATE, modelName, id);
        String retrieved = KiePMMLUtil.getSanitizedId(id, modelName);
        assertEquals(expected, retrieved);
        id = "34.5";
        expected = String.format(SEGMENTID_TEMPLATE, modelName, id);
        retrieved = KiePMMLUtil.getSanitizedId(id, modelName);
        assertEquals(expected, retrieved);
        id = "3,45";
        expected = String.format(SEGMENTID_TEMPLATE, modelName, id);
        retrieved = KiePMMLUtil.getSanitizedId(id, modelName);
        assertEquals(expected, retrieved);
    }

    @Test
    public void getMiningTargetFieldsFromMiningSchema() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_MODELNAME_SAMPLE_NAME);
        final PMML toPopulate = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model model = toPopulate.getModels().get(0);
        List<MiningField> retrieved = KiePMMLUtil.getMiningTargetFields(model.getMiningSchema());
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        MiningField targetField = retrieved.get(0);
        assertEquals("car_location", targetField.getName().getValue());
        assertEquals("target", targetField.getUsageType().value());
    }

    @Test
    public void getMiningTargetFieldsFromMiningFields() throws Exception {
        final InputStream inputStream = getFileInputStream(NO_MODELNAME_SAMPLE_NAME);
        final PMML toPopulate = org.jpmml.model.PMMLUtil.unmarshal(inputStream);
        final Model model = toPopulate.getModels().get(0);
        List<MiningField> retrieved = KiePMMLUtil.getMiningTargetFields(model.getMiningSchema().getMiningFields());
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        MiningField targetField = retrieved.get(0);
        assertEquals("car_location", targetField.getName().getValue());
        assertEquals("target", targetField.getUsageType().value());
    }

    private void commonLoadString(String fileName) throws IOException, JAXBException, SAXException {
        InputStream inputStream = getFileInputStream(fileName);

        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                                                        (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        PMML retrieved = KiePMMLUtil.load(textBuilder.toString());
        commonValidatePMML(retrieved);
    }

    private void commonLoadFile(String fileName) throws IOException, JAXBException, SAXException {
        PMML retrieved = KiePMMLUtil.load(getFileInputStream(fileName), fileName);
        commonValidatePMML(retrieved);
    }

    private void commonValidatePMML(PMML toValidate) {
        assertNotNull(toValidate);
        for (Model model : toValidate.getModels()) {
            assertNotNull(model.getModelName());
            if (model instanceof MiningModel) {
                commonValidateMiningModel((MiningModel) model);
            }
        }
    }

    private void commonValidateMiningModel(MiningModel toValidate) {
        assertNotNull(toValidate);
        for (Segment segment : toValidate.getSegmentation().getSegments()) {
            assertNotNull(segment.getId());
            Model segmentModel = segment.getModel();
            assertNotNull(segmentModel.getModelName());
            if (segmentModel instanceof MiningModel) {
                commonValidateMiningModel((MiningModel) segmentModel);
            }
        }
        List<String> modelNames = toValidate.getSegmentation().getSegments()
                .stream()
                .map(segment -> segment.getModel().getModelName())
                .collect(Collectors.toList());
        assertEquals(modelNames.size(), modelNames.stream().distinct().count());
    }
}