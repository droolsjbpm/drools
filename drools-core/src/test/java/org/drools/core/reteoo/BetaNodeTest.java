/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
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

package org.drools.core.reteoo;

import org.drools.core.common.EmptyBetaConstraints;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.reteoo.builder.BuildContext;
import org.junit.Test;
import org.drools.core.impl.KnowledgeBaseFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BetaNodeTest {

    @Test
    public void testEqualsObject() {
        InternalKnowledgeBase kBase = (InternalKnowledgeBase) KnowledgeBaseFactory.newKnowledgeBase();

        BuildContext buildContext = new BuildContext( kBase );

        final LeftTupleSource ts = new MockTupleSource( 1, buildContext );
        final ObjectSource os = new MockObjectSource( 2, buildContext );
        
        final BetaNode j1 = new JoinNode( 1,
                                          ts,
                                          os,
                                          EmptyBetaConstraints.getInstance(),
                                          buildContext );
        final BetaNode j2 = new JoinNode( 2,
                                          ts,
                                          os,
                                          EmptyBetaConstraints.getInstance(),
                                          buildContext );
        final BetaNode n1 = new NotNode( 3,
                                         ts,
                                         os,
                                         EmptyBetaConstraints.getInstance(),
                                         buildContext );
        final BetaNode n2 = new NotNode( 4,
                                         ts,
                                         os,
                                         EmptyBetaConstraints.getInstance(),
                                         buildContext );

        assertEquals( j1,
                      j1 );
        assertEquals( j2,
                      j2 );
        assertEquals( j1,
                      j2 );
        assertEquals( n1,
                      n1 );
        assertEquals( n2,
                      n2 );
        assertEquals( n1,
                      n2 );

        assertFalse( j1.equals( n1 ) );
        assertFalse( j1.equals( n2 ) );
        assertFalse( n1.equals( j1 ) );
        assertFalse( n1.equals( j2 ) );
    }

}
