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

import java.util.Collection;
import java.util.List;

import org.drools.compiler.lang.dsl.DSLMappingEntry.Section;

/**
 * An interface that represents a DSL Mapping 
 */
public interface DSLMapping {

    /**
     * Returns the string identifier for this mapping
     * @return
     */
    public String getIdentifier();

    /**
     * Sets the identifier for this mapping
     * @param identifier
     */
    public void setIdentifier(String identifier);

    /**
     * Returns a String description of this mapping
     * @return
     */
    public String getDescription();

    /**
     * Sets the description for this mapping
     * @param description
     */
    public void setDescription(String description);

    /**
     * Returns the list of entries in this mapping
     * @return
     */
    public List<DSLMappingEntry> getEntries();

    /**
     * Add one entry to the list of the entries
     * @param entry
     */
    public void addEntry(DSLMappingEntry entry);

    /**
     * Adds all entries in the given list to this DSL Mapping
     * @param entries
     */
    public void addEntries(List<DSLMappingEntry> entries);

    /**
     * Removes the given entry from the list of entries
     * @param entry
     */
    public void removeEntry(DSLMappingEntry entry);

    /**
     * Returns the list of mappings for the given section 
     * @param section
     * @return
     */
    public List<DSLMappingEntry> getEntries(Section section);

    /**
     * Sets an expansion option.
     * @param option
     */
    public void setOptions( Collection<String> option );
    
    /**
     * Retrieves an an expansion option.
     * @param option
     * @return true if option is set.
     */
    public boolean getOption( String option );
    
    
    
}
