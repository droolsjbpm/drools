/*
 * Copyright 2005 Red Hat, Inc. and/or its affiliates.
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

package org.drools.mvel.compiler;

import java.io.Serializable;

/**
 * IndexedNumber
 *
 * Created: 22/06/2006
 *
 * @version $Id$
 */

public class IndexedNumber implements Serializable {

    private int     number = 0;

    private int     index  = 0;

    private boolean printed;

    public IndexedNumber() {
    }

    /**
     * @param pNb
     * @param pIndex
     */
    public IndexedNumber(final int pNb,
                         final int pIndex) {
        this.number = pNb;
        this.index = pIndex;
    }

    /**
     * @return le/la/les index.
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * @param pIndex le/la/les index sauvegarder.
     */
    public void setIndex(final int pIndex) {
        this.index = pIndex;
    }

    /**
     * @return le/la/les nb.
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * @param pNb le/la/les nb sauvegarder.
     */
    public void setNumber(final int pNb) {
        this.number = pNb;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "IndexedNumber[ " + this.number + ", " + this.index + " ]";
    }

    public boolean isPrinted() {
        return this.printed;
    }

    public void setPrinted(final boolean printed) {
        this.printed = printed;
    }

}
