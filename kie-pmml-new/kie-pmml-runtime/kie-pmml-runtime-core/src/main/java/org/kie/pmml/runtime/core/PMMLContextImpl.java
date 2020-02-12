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
package org.kie.pmml.runtime.core;

import org.drools.core.command.impl.ContextImpl;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.pmml.runtime.api.executor.PMMLContext;

public class PMMLContextImpl extends ContextImpl implements PMMLContext {

    private static final String PMML_REQUEST_DATA = "PMML_REQUEST_DATA";

    public PMMLContextImpl(PMMLRequestData pmmlRequestData) {
        super();
        set(PMML_REQUEST_DATA, pmmlRequestData);
    }

    @Override
    public PMMLRequestData getRequestData() {
        return (PMMLRequestData) get(PMML_REQUEST_DATA);
    }
}