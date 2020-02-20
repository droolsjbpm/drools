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

import java.io.Serializable;
import java.util.List;

/**
 * @see <a href=http://dmg.org/pmml/v4-4/GeneralStructure.html#xsdElement_Extension>Extension</a>
 */
public class KiePMMLExtension implements Serializable {

    private static final long serialVersionUID = -5895956619311871049L;
    private String extender;
    private String name;
    private String value;
    private List<Object> content;

    public KiePMMLExtension() {
        // Serialization
    }

    public KiePMMLExtension(String extender, String name, String value, List<Object> content) {
        this.extender = extender;
        this.name = name;
        this.value = value;
        this.content = content;
    }

    public String getExtender() {
        return extender;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public List<Object> getContent() {
        return content;
    }
}
