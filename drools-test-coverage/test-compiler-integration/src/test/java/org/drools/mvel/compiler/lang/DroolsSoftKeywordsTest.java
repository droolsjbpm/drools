/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.mvel.compiler.lang;

import org.drools.compiler.lang.DroolsSoftKeywords;
import org.drools.core.base.evaluators.EvaluatorRegistry;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DroolsSoftKeywordsTest {

    /**
     * Test method for {@link org.kie.lang.DroolsSoftKeywords#isOperator(java.lang.String, boolean)}.
     */
    @Test
    public void testIsOperator() {
        // initializes the registry
        new EvaluatorRegistry();

        // test the registry
        assertTrue( DroolsSoftKeywords.isOperator("matches", false) );
        assertTrue( DroolsSoftKeywords.isOperator("matches", true) );
        assertTrue( DroolsSoftKeywords.isOperator("contains", false) );
        assertTrue( DroolsSoftKeywords.isOperator("contains", true) );
        assertTrue( DroolsSoftKeywords.isOperator("after", false) );
        assertTrue( DroolsSoftKeywords.isOperator("after", true) );
        assertTrue( DroolsSoftKeywords.isOperator("before", false) );
        assertTrue( DroolsSoftKeywords.isOperator("before", true) );
        assertTrue( DroolsSoftKeywords.isOperator("finishes", false) );
        assertTrue( DroolsSoftKeywords.isOperator("finishes", true) );
        assertTrue( DroolsSoftKeywords.isOperator("overlappedby", false) );
        assertTrue( DroolsSoftKeywords.isOperator("overlappedby", true) );

        assertFalse( DroolsSoftKeywords.isOperator("xyz", false) );
        assertFalse( DroolsSoftKeywords.isOperator("xyz", true) );

    }

}
