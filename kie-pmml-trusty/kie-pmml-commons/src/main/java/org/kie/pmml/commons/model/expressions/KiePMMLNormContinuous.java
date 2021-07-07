/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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
package org.kie.pmml.commons.model.expressions;

import java.util.List;

import org.kie.pmml.api.enums.OUTLIER_TREATMENT_METHOD;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.commons.model.KiePMMLExtension;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.commons.model.abstracts.AbstractKiePMMLComponent;

import static org.kie.pmml.commons.model.expressions.ExpressionsUtils.getFromPossibleSources;

/**
 * @see <a href=http://dmg.org/pmml/v4-4-1/Transformations.html#xsdElement_NormContinuous>NormContinuous</a>
 */
public class KiePMMLNormContinuous extends AbstractKiePMMLComponent implements KiePMMLExpression {

    private static final long serialVersionUID = -7935602676734880795L;
    protected final List<KiePMMLLinearNorm> linearNorms;
    private final KiePMMLLinearNorm firstLinearNorm;
    private final KiePMMLLinearNorm lastLinearNorm;
    private final OUTLIER_TREATMENT_METHOD outlierTreatmentMethod;
    private final Number mapMissingTo;

    public KiePMMLNormContinuous(final String name,
                                    final List<KiePMMLExtension> extensions,
                                    final List<KiePMMLLinearNorm> linearNorms,
                                    final OUTLIER_TREATMENT_METHOD outlierTreatmentMethod,
                                    final Number mapMissingTo) {
        super(name, extensions);
        sortLinearNorms(linearNorms);
        this.linearNorms = linearNorms;
        firstLinearNorm = linearNorms.get(0);
        lastLinearNorm = linearNorms.get(linearNorms.size() - 1);
        this.outlierTreatmentMethod = outlierTreatmentMethod;
        this.mapMissingTo = mapMissingTo;
    }

    static void sortLinearNorms(final List<KiePMMLLinearNorm> toSort) {
        toSort.sort((o1, o2) -> (int) (o1.getOrig() - o2.getOrig()));
    }

    @Override
    public Object evaluate(final ProcessingDTO processingDTO) {
        Number input = (Number) getFromPossibleSources(name, processingDTO)
                .orElse(mapMissingTo);
        if (input == null) {
            throw new KiePMMLException("Failed to retrieve input number for " + name);
        }
        return evaluate(input);
    }

    Number evaluate(final Number input) {
        if (input.doubleValue() >= firstLinearNorm.getOrig() && input.doubleValue() <= lastLinearNorm.getOrig()) {
            return evaluateExpectedValue(input);
        } else {
            return evaluateOutlierValue(input);
        }
    }

    Number evaluateExpectedValue(final Number input) {
        KiePMMLLinearNorm[] limitLinearNorms = getLimitExpectedValue(input);
        return evaluate(input, limitLinearNorms);
    }

    Number evaluateOutlierValue(final Number input) {
        switch (outlierTreatmentMethod) {
            case AS_IS:
                KiePMMLLinearNorm[] limitLinearNorms;
                if (input.doubleValue() < firstLinearNorm.getOrig()) {
                    limitLinearNorms = linearNorms.subList(0, 2).toArray(new KiePMMLLinearNorm[0]);
                } else {
                    limitLinearNorms = linearNorms.subList(linearNorms.size() -2, linearNorms.size()).toArray(new KiePMMLLinearNorm[0]);
                }
                return evaluate(input, limitLinearNorms);
            case AS_MISSING_VALUES:
                return mapMissingTo;
            case AS_EXTREME_VALUES:
                return input.doubleValue() < firstLinearNorm.getOrig() ? firstLinearNorm.getNorm() : lastLinearNorm.getNorm();
            default:
                throw new KiePMMLException("Unknown outlierTreatmentMethod " + outlierTreatmentMethod);
        }
    }

    KiePMMLLinearNorm[] getLimitExpectedValue(final Number input) {
        int counter = 0;
        KiePMMLLinearNorm linearNorm = linearNorms.get(counter);
        KiePMMLLinearNorm startLinearNorm = null;
        while (linearNorm.getOrig() <= input.doubleValue() && counter < linearNorms.size() -1) {
            startLinearNorm = linearNorm;
            counter ++;
            linearNorm = linearNorms.get(counter);
        }
        int startIndex = linearNorms.indexOf(startLinearNorm);
        counter = linearNorms.size() -1;
        linearNorm = linearNorms.get(counter);
        KiePMMLLinearNorm endLinearNorm = null;
        while (linearNorm.getOrig() >= input.doubleValue() && counter > startIndex) {
            endLinearNorm = linearNorm;
            counter --;
            linearNorm = linearNorms.get(counter);
        }
        return new KiePMMLLinearNorm[]{startLinearNorm, endLinearNorm};
    }

    static Number evaluate(final Number input, final KiePMMLLinearNorm[] limitLinearNorms) {
        KiePMMLLinearNorm startLinearNorm = limitLinearNorms[0];
        KiePMMLLinearNorm endLinearNorm = limitLinearNorms[1];
        return startLinearNorm.getNorm() +
                ((input.doubleValue() - startLinearNorm.getOrig()) / (endLinearNorm.getOrig() - startLinearNorm.getOrig())) *
                        (endLinearNorm.getNorm() - startLinearNorm.getNorm());
    }
}
