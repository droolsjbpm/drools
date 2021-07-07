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

import org.kie.internal.builder.KnowledgeBuilderResult;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A helper class that handles a DSL Mapping file
 */
public abstract class DSLMappingFile {

    private DSLMapping mapping;
    private List       errors;

    public DSLMappingFile() {
        this.mapping = new DefaultDSLMapping();
        this.errors = Collections.emptyList();
    }

    /**
     * Returns the DSL mapping loaded from this file
     * @return
     */
    public DSLMapping getMapping() {
        return this.mapping;
    }

    /**
     * Sets the 
     * @param mapping
     */
    public void setMapping(final DSLMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * Returns the list of parsing errors
     * @return
     */
    public List<KnowledgeBuilderResult> getErrors() {
        return Collections.unmodifiableList( this.errors );
    }

    protected void setErrors(List<? extends KnowledgeBuilderResult> errors) {
        this.errors = errors;
    }

    /**
     * Parses the file. Throws IOException in case there is any problem
     * reading the file;
     * 
     * @return true in case no error was found parsing the file. false 
     *         otherwise. Use getErrors() to check for the actual errors.
     */
    public abstract boolean parseAndLoad(final Reader dsl) throws IOException;

    /**
     * Saves current mapping into a DSL mapping file
     * @param out
     * @throws IOException
     */
    public void saveMapping(final Writer out) throws IOException {
        for ( final Iterator it = this.mapping.getEntries().iterator(); it.hasNext(); ) {
            out.write( it.next().toString() );
            out.write( "\n" );
        }
    }

    /**
     * Saves the given mapping into a DSL mapping file
     * 
     * @param out
     * @param mapping
     * @throws IOException
     */
    public static void saveMapping(final Writer out,
                                   final DSLMapping mapping) throws IOException {
        for (DSLMappingEntry dslMappingEntry : mapping.getEntries()) {
            out.write(dslMappingEntry.toString());
            out.write("\n");
        }
    }

    /**
     * Method to return the current mapping as a String object
     * @return
     */
    public String dumpFile() {
        final StringBuilder buf = new StringBuilder();
        for (DSLMappingEntry dslMappingEntry : this.mapping.getEntries()) {
            buf.append(dslMappingEntry);
            buf.append("\n");
        }
        return buf.toString();
    }

}
