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
package org.kie.pmml.models.drools.commons.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Supplier;

import org.drools.compiler.lang.descr.PackageDescr;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.pmml.PMML4Result;
import org.kie.pmml.commons.enums.ResultCode;
import org.kie.pmml.commons.model.KiePMMLExtension;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.commons.model.KiePMMLOutputField;
import org.kie.pmml.commons.model.enums.MINING_FUNCTION;
import org.kie.pmml.commons.model.enums.PMML_MODEL;
import org.kie.pmml.models.drools.tuples.KiePMMLOriginalTypeGeneratedType;
import org.kie.pmml.models.drools.utils.KiePMMLSessionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.models.drools.utils.KiePMMLAgendaListenerUtils.getAgendaEventListener;

/**
 * KIE representation of PMML model that use <b>drool</b> for implementation
 */
public abstract class KiePMMLDroolsModel extends KiePMMLModel {

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLDroolsModel.class.getName());

    private static final AgendaEventListener agendaEventListener = getAgendaEventListener(logger);

    protected PackageDescr packageDescr;

    protected List<KiePMMLOutputField> outputFields;

    /**
     * Map between the original field name and the generated type.
     */
    protected Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap;

    protected KiePMMLDroolsModel(String name, List<KiePMMLExtension> extensions) {
        super(name, extensions);
    }

    public PackageDescr getPackageDescr() {
        return packageDescr;
    }

    public Map<String, KiePMMLOriginalTypeGeneratedType> getFieldTypeMap() {
        return fieldTypeMap;
    }

    @Override
    public Object evaluate(Map<String, Object> requestData) {
        final PMML4Result toReturn = getPMML4Result(targetField);
        final KiePMMLSessionUtils kiePMMLSessionUtils = KiePMMLSessionUtils.builder(packageDescr, toReturn)
                .withAgendaEventListener(agendaEventListener)
                .withObjectsInSession(requestData, fieldTypeMap)
                .withOutputFieldsMap(outputFieldsMap)
                .build();
        kiePMMLSessionUtils.fireAllRules();
        return toReturn;
    }

    private PMML4Result getPMML4Result(final String targetField) {
        PMML4Result toReturn = new PMML4Result();
        toReturn.setResultCode(ResultCode.FAIL.getName());
        toReturn.setResultObjectName(targetField);
        return toReturn;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", KiePMMLDroolsModel.class.getSimpleName() + "[", "]")
                .add("packageDescr=" + packageDescr)
                .add("outputFields=" + outputFields)
                .add("fieldTypeMap=" + fieldTypeMap)
                .add("pmmlMODEL=" + pmmlMODEL)
                .add("miningFunction=" + miningFunction)
                .add("targetField='" + targetField + "'")
                .add("outputFieldsMap=" + outputFieldsMap)
                .add("missingValueReplacementMap=" + missingValueReplacementMap)
                .add("name='" + name + "'")
                .add("extensions=" + extensions)
                .add("id='" + id + "'")
                .add("parentId='" + parentId + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        KiePMMLDroolsModel that = (KiePMMLDroolsModel) o;
        return Objects.equals(packageDescr, that.packageDescr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), packageDescr);
    }

    public abstract static class Builder<T extends KiePMMLDroolsModel> extends KiePMMLModel.Builder<T> {

        protected Builder(String prefix, PMML_MODEL pmmlMODEL, MINING_FUNCTION miningFunction, Supplier<T> supplier) {
            super(prefix, pmmlMODEL, miningFunction, supplier);
        }

        public Builder<T> withPackageDescr(PackageDescr packageDescr) {
            toBuild.packageDescr = packageDescr;
            return this;
        }

        public Builder<T> withFieldTypeMap(Map<String, KiePMMLOriginalTypeGeneratedType> fieldTypeMap) {
            toBuild.fieldTypeMap = fieldTypeMap;
            return this;
        }

        public Builder<T> withOutputFields(List<KiePMMLOutputField> outputFields) {
            toBuild.outputFields = outputFields;
            return this;
        }
    }
}
