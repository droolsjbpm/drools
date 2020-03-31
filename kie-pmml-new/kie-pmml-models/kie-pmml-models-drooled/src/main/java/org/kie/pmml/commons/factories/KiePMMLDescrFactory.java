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
package org.kie.pmml.commons.factories;

import org.dmg.pmml.SimplePredicate;
import org.drools.compiler.lang.api.DescrFactory;
import org.drools.compiler.lang.api.PackageDescrBuilder;
import org.drools.compiler.lang.descr.PackageDescr;
import org.kie.api.pmml.PMML4Result;
import org.kie.pmml.models.drooled.ast.KiePMMLDrooledAST;
import org.kie.pmml.models.drooled.executor.KiePMMLStatusHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to generate a <b>DROOLS</b> (descr) object out of a<b>TreeModel</b>
 */
public class KiePMMLDescrFactory {

    public static final String PMML4_RESULT = "PMML4Result";
    public static final String PMML4_RESULT_IDENTIFIER = "$pmml4Result";

    private static final Logger logger = LoggerFactory.getLogger(KiePMMLDescrFactory.class.getName());

    private KiePMMLDescrFactory() {
        // Avoid instantiation
    }

    /**
     * Returns the <code>PackageDescr</code> built out of the given <code>KiePMMLDrooledAST</code>.
     * @param kiePMMLDrooledAST
     * @param packageName
     * @return
     */
    public static PackageDescr getBaseDescr(final KiePMMLDrooledAST kiePMMLDrooledAST, String packageName) {
        logger.info("getBaseDescr {} {}", kiePMMLDrooledAST, packageName);
        PackageDescrBuilder builder = DescrFactory.newPackage()
                .name(packageName);
        builder.newImport().target(KiePMMLStatusHolder.class.getName());
        builder.newImport().target(SimplePredicate.class.getName());
        builder.newImport().target(PMML4Result.class.getName());
        builder.newGlobal().identifier(PMML4_RESULT_IDENTIFIER).type(PMML4_RESULT);
        KiePMMLDescrTypesFactory.factory(builder).declareTypes(kiePMMLDrooledAST.getTypes());
        KiePMMLDescrRulesFactory.factory(builder).declareRules(kiePMMLDrooledAST.getRules());
        return builder.getDescr();
    }
}