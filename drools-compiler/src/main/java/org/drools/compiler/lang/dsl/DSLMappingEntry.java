/*
 * Copyright 2006 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.compiler.lang.dsl;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A single entry in a DSL mapping file
 */
public interface DSLMappingEntry {

    public static final Section  KEYWORD        = Section.KEYWORD;
    public static final Section  CONDITION      = Section.CONDITION;
    public static final Section  CONSEQUENCE    = Section.CONSEQUENCE;
    public static final Section  ANY            = Section.ANY;

    public static final MetaData EMPTY_METADATA = new DefaultDSLEntryMetaData( "" );

    /**
     * Returns the section this mapping entry refers to
     * 
     * @return
     */
    public DSLMappingEntry.Section getSection();

    /**
     * Returns the meta data info about this mapping entry
     * 
     * @return
     */
    public DSLMappingEntry.MetaData getMetaData();

    /**
     * Returns the key of this mapping, i.e., the source
     * that needs to be translated
     * 
     * @return
     */
    public String getMappingKey();

    /**
     * Returns the result of the translation
     * 
     * @return
     */
    public String getMappingValue();

    /**
     * Returns the compiled pattern based on the given MappingKey
     * @return the keyPattern
     */
    public Pattern getKeyPattern();

    /**
     * Returns the transformed mapping value using place holders for variables 
     * @return the valuePattern
     */
    public String getValuePattern();

    /**
     * Returns the list of variables found in the given pattern key 
     * in the same order they were found
     * 
     * @return the variables
     */
    public Map<String, Integer> getVariables();

    /**
     * @param key the key to set
     */
    public void setMappingKey(String key);

    /**
     * @param section the section to set
     */
    public void setSection(Section section);

    /**
     * @param value the value to set
     */
    public void setMappingValue(String value);

    /**
     * @param metadata the metadata to set
     */
    public void setMetaData(MetaData metadata);

    /**
     * Returns a list of errors found in this mapping
     * @return
     */
    public List getErrors();

    /**
     * An enum for the sections
     */
    public enum Section {
        KEYWORD("[keyword]"), 
        CONDITION("[condition]"), 
        CONSEQUENCE("[consequence]"), 
        ANY("[*]");

        private String symbol;

        private Section(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return this.symbol;
        }
    }

    /**
     * An inner interface to represent any metadata
     * associated with this entry. It is obviously
     * implementation dependent.
     */
    public static interface MetaData
        extends
        Comparable<MetaData> {
        public String toString();

        public String getMetaData();
    }

    public static class DefaultDSLEntryMetaData
        implements
        DSLMappingEntry.MetaData {

        private String metadata;

        public DefaultDSLEntryMetaData(final String metadata) {
            this.metadata = metadata;
        }

        public String getMetaData() {
            return this.metadata;
        }

        public String toString() {
            return (this.metadata == null) ? "" : this.metadata;
        }

        public int compareTo(final MetaData arg0) {
            return this.toString().compareTo( arg0.toString() );
        }
    }

}
