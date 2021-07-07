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
package org.kie.pmml.commons.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.kie.pmml.api.enums.MINING_FUNCTION;
import org.kie.pmml.api.enums.PMML_MODEL;
import org.kie.pmml.api.models.MiningField;
import org.kie.pmml.api.models.OutputField;
import org.kie.pmml.api.models.PMMLModel;
import org.kie.pmml.commons.model.abstracts.AbstractKiePMMLComponent;
import org.kie.pmml.commons.transformations.KiePMMLLocalTransformations;
import org.kie.pmml.commons.transformations.KiePMMLTransformationDictionary;

/**
 * KIE representation of PMML model
 */
public abstract class KiePMMLModel extends AbstractKiePMMLComponent implements PMMLModel {

    private static final long serialVersionUID = 759750766311061701L;

    protected PMML_MODEL pmmlMODEL;
    protected MINING_FUNCTION miningFunction;
    protected String targetField;
    protected Map<String, Object> outputFieldsMap = new HashMap<>();
    protected Map<String, Object> missingValueReplacementMap = new HashMap<>();
    protected List<MiningField> miningFields = new ArrayList<>();
    protected List<OutputField> outputFields = new ArrayList<>();
    protected List<KiePMMLOutputField> kiePMMLOutputFields = new ArrayList<>();
    protected List<KiePMMLTarget> kiePMMLTargets = new ArrayList<>();
    protected KiePMMLTransformationDictionary transformationDictionary;
    protected KiePMMLLocalTransformations localTransformations;


    protected KiePMMLModel(String name, List<KiePMMLExtension> extensions) {
        super(name, extensions);
    }

    public PMML_MODEL getPmmlMODEL() {
        return pmmlMODEL;
    }

    public MINING_FUNCTION getMiningFunction() {
        return miningFunction;
    }

    public String getTargetField() {
        return targetField;
    }

    public Map<String, Object> getOutputFieldsMap() {
        return Collections.unmodifiableMap(outputFieldsMap);
    }

    public Map<String, Object> getMissingValueReplacementMap() {
        return Collections.unmodifiableMap(missingValueReplacementMap);
    }

    /**
     * Method to retrieve the <b>package</b> name to be used inside kiebase/package attribute of
     * kmodule.xml and to use for package creation inside PMMLAssemblerService
     * By default returns the package name of the current instance
     * To be eventually overridden.
     * @return
     */
    public String getKModulePackageName() {
        String className = this.getClass().getCanonicalName();
        return className.substring(0, className.lastIndexOf('.'));
    }

    @Override
    public List<MiningField> getMiningFields() {
        return miningFields;
    }

    public void setMiningFields(final List<MiningField> miningFields) {
        this.miningFields = Collections.unmodifiableList(miningFields);
    }

    @Override
    public List<OutputField> getOutputFields() {
        return outputFields;
    }

    public void setOutputFields(List<OutputField> outputFields) {
        this.outputFields = Collections.unmodifiableList(outputFields);
    }

    public List<KiePMMLTarget> getKiePMMLTargets() {
        return kiePMMLTargets;
    }

    public void setKiePMMLTargets(List<KiePMMLTarget> kiePMMLTargets) {
        this.kiePMMLTargets = Collections.unmodifiableList(kiePMMLTargets);
    }

    public List<KiePMMLOutputField> getKiePMMLOutputFields() {
        return kiePMMLOutputFields != null  ? Collections.unmodifiableList(kiePMMLOutputFields) : Collections.emptyList();
    }

    public KiePMMLTransformationDictionary getTransformationDictionary() {
        return transformationDictionary;
    }

    public KiePMMLLocalTransformations getLocalTransformations() {
        return localTransformations;
    }

    /**
     * @param knowledgeBase the knowledgeBase we are working on. Add as <code>Object</code> to avoid direct
     * dependency. It is needed only by <b>Drools-dependent</b>
     * models, so it may be <b>ignored</b> by others
     * @param requestData
     * @return
     */
    public abstract Object evaluate(final Object knowledgeBase, final Map<String, Object> requestData);

    public abstract static class Builder<T extends KiePMMLModel> extends AbstractKiePMMLComponent.Builder<T> {

        protected Builder(String prefix, PMML_MODEL pmmlMODEL, MINING_FUNCTION miningFunction, Supplier<T> supplier) {
            super(prefix, supplier);
            toBuild.pmmlMODEL = pmmlMODEL;
            toBuild.miningFunction = miningFunction;
        }

        public Builder<T> withTargetField(String targetField) {
            toBuild.targetField = targetField;
            return this;
        }

        public Builder<T> withOutputFieldsMap(Map<String, Object> outputFieldsMap) {
            toBuild.outputFieldsMap.putAll(outputFieldsMap);
            return this;
        }

        public Builder<T> withMissingValueReplacementMap(Map<String, Object> missingValueReplacementMap) {
            toBuild.missingValueReplacementMap.putAll(missingValueReplacementMap);
            return this;
        }
    }
}
