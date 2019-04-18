/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.drools.verifier.core.checks.base;

import org.drools.verifier.core.AnalyzerConfigurationMock;
import org.drools.verifier.core.cache.inspectors.RuleInspector;
import org.drools.verifier.core.configuration.CheckConfiguration;
import org.drools.verifier.core.index.Index;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class CheckFactoryTest {

    @Mock
    private Index index;

    @Test
    public void emptyWhiteList() throws
            Exception {

        final AnalyzerConfigurationMock configuration = new AnalyzerConfigurationMock(CheckConfiguration.newEmpty());

        assertTrue(new CheckFactory(configuration).makeSingleChecks(mock(RuleInspector.class))
                           .isEmpty());
        assertFalse(new CheckFactory(configuration).makePairRowCheck(mock(RuleInspector.class),
                                                                     mock(RuleInspector.class))
                            .isPresent());
    }

    @Test
    public void defaultWhiteList() throws
            Exception {

        final AnalyzerConfigurationMock configuration = new AnalyzerConfigurationMock(CheckConfiguration.newDefault());

        assertFalse(new CheckFactory(configuration).makeSingleChecks(mock(RuleInspector.class))
                            .isEmpty());
        RuleInspector ruleInspector = mock(RuleInspector.class);
        doReturn(1).when(ruleInspector).getRowIndex();
        RuleInspector other = mock(RuleInspector.class);
        doReturn(2).when(other).getRowIndex();
        assertTrue(new CheckFactory(configuration).makePairRowCheck(ruleInspector,
                                                                    other)
                           .isPresent());
    }
}