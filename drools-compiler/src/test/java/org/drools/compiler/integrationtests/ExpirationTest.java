/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.compiler.integrationtests;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.drools.compiler.integrationtests.facts.BasicEvent;
import org.drools.core.ClassObjectFilter;
import org.drools.core.ClockType;
import org.drools.core.SessionConfigurationImpl;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.utils.KieHelper;

import static org.junit.Assert.assertEquals;
import static org.kie.api.definition.type.Expires.Policy.TIME_SOFT;

public class ExpirationTest {

    @Test
    public void testAlpha() {
        String drl = "import " + A.class.getCanonicalName() + "\n" +
                     "declare A @role( event ) @expires(11ms) end\n" +
                     "global java.util.concurrent.atomic.AtomicInteger counter;\n" +
                     "rule R0 when\n" +
                     "  $a: A( $Aid: id > 0 )\n" +
                     "then\n" +
                     "  System.out.println(\"[\" + $a + \"]\");" +
                     "  counter.incrementAndGet();\n" +
                     "end";

        KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        KieBase kbase = helper.build( EventProcessingOption.STREAM );
        KieSession ksession = kbase.newKieSession( sessionConfig, null );

        PseudoClockScheduler sessionClock = ksession.getSessionClock();

        AtomicInteger counter = new AtomicInteger( 0 );
        ksession.setGlobal( "counter", counter );

        ksession.insert( new A(1) );

        sessionClock.advanceTime( 10, TimeUnit.MILLISECONDS );
        ksession.insert( new A(2) );

        sessionClock.advanceTime( 10, TimeUnit.MILLISECONDS );
        ksession.fireAllRules();
        assertEquals(2, counter.get());
    }

    @Test
    public void testBeta() {
        // DROOLS-1329
        String drl = "import " + A.class.getCanonicalName() + "\n" +
                     "import " + B.class.getCanonicalName() + "\n" +
                     "declare A @role( event ) @expires(11ms) end\n" +
                     "declare B @role( event ) @expires(11ms) end\n" +
                     "global java.util.concurrent.atomic.AtomicInteger counter;\n" +
                     "rule R0 when\n" +
                     "  $a: A( $Aid: id > 0 )\n" +
                     "  $b: B( ($Bid: id <= $Aid) && (id > ($Aid - 1 )))\n" +
                     "then\n" +
                     "  System.out.println(\"[\" + $a + \",\" + $b + \"]\");" +
                     "  counter.incrementAndGet();\n" +
                     "end";

        KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        KieBase kbase = helper.build( EventProcessingOption.STREAM );
        KieSession ksession = kbase.newKieSession( sessionConfig, null );

        PseudoClockScheduler sessionClock = ksession.getSessionClock();

        AtomicInteger counter = new AtomicInteger( 0 );
        ksession.setGlobal( "counter", counter );

        ksession.insert( new A(1) );
        ksession.insert( new B(1) );

        sessionClock.advanceTime( 10, TimeUnit.MILLISECONDS );
        ksession.insert( new A(2) );
        ksession.insert( new B(2) );

        sessionClock.advanceTime( 10, TimeUnit.MILLISECONDS );

        ksession.fireAllRules();
        assertEquals(2, counter.get());
    }

    @Test
    public void testBetaRightExpired() {
        // DROOLS-1329
        String drl = "import " + A.class.getCanonicalName() + "\n" +
                     "import " + B.class.getCanonicalName() + "\n" +
                     "declare A @role( event ) @expires(11ms) end\n" +
                     "declare B @role( event ) @expires(11ms) end\n" +
                     "global java.util.concurrent.atomic.AtomicInteger counter;\n" +
                     "rule R0 when\n" +
                     "  $a: A( $Aid: id > 0 )\n" +
                     "  $b: B( id == $Aid )\n" +
                     "then\n" +
                     "  System.out.println(\"[\" + $a + \",\" + $b + \"]\");" +
                     "  counter.incrementAndGet();\n" +
                     "end";

        KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        KieBase kbase = helper.build( EventProcessingOption.STREAM );
        KieSession ksession = kbase.newKieSession( sessionConfig, null );

        PseudoClockScheduler sessionClock = ksession.getSessionClock();

        AtomicInteger counter = new AtomicInteger( 0 );
        ksession.setGlobal( "counter", counter );

        ksession.insert( new A(1) );

        sessionClock.advanceTime( 20, TimeUnit.MILLISECONDS );
        ksession.insert( new B(1) );

        ksession.fireAllRules();
        assertEquals(0, counter.get());
    }

    @Test
    public void testBetaLeftExpired() {
        // DROOLS-1329
        String drl = "import " + A.class.getCanonicalName() + "\n" +
                     "import " + B.class.getCanonicalName() + "\n" +
                     "declare A @role( event ) @expires(11ms) end\n" +
                     "declare B @role( event ) @expires(11ms) end\n" +
                     "global java.util.concurrent.atomic.AtomicInteger counter;\n" +
                     "rule R0 when\n" +
                     "  $a: A( $Aid: id > 0 )\n" +
                     "  $b: B( id == $Aid )\n" +
                     "then\n" +
                     "  System.out.println(\"[\" + $a + \",\" + $b + \"]\");" +
                     "  counter.incrementAndGet();\n" +
                     "end";

        KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        KieBase kbase = helper.build( EventProcessingOption.STREAM );
        KieSession ksession = kbase.newKieSession( sessionConfig, null );

        PseudoClockScheduler sessionClock = ksession.getSessionClock();

        AtomicInteger counter = new AtomicInteger( 0 );
        ksession.setGlobal( "counter", counter );

        ksession.insert( new B(1) );

        sessionClock.advanceTime( 20, TimeUnit.MILLISECONDS );
        ksession.insert( new A(1) );

        ksession.fireAllRules();
        assertEquals(0, counter.get());
    }

    @Test
    public void testBetaLeftExpired2() {
        // DROOLS-1329
        String drl = "import " + A.class.getCanonicalName() + "\n" +
                     "import " + B.class.getCanonicalName() + "\n" +
                     "import " + C.class.getCanonicalName() + "\n" +
                     "declare A @role( event ) @expires(31ms) end\n" +
                     "declare B @role( event ) @expires(11ms) end\n" +
                     "declare C @role( event ) @expires(31ms) end\n" +
                     "global java.util.concurrent.atomic.AtomicInteger counter;\n" +
                     "rule R0 when\n" +
                     "  $a: A( $Aid: id > 0 )\n" +
                     "  $b: B( $Bid: id == $Aid )\n" +
                     "  $c: C( id == $Bid )\n" +
                     "then\n" +
                     "  System.out.println(\"[\" + $a + \",\" + $b + \",\" + $c + \"]\");" +
                     "  counter.incrementAndGet();\n" +
                     "end";

        KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        KieBase kbase = helper.build( EventProcessingOption.STREAM );
        KieSession ksession = kbase.newKieSession( sessionConfig, null );

        PseudoClockScheduler sessionClock = ksession.getSessionClock();

        AtomicInteger counter = new AtomicInteger( 0 );
        ksession.setGlobal( "counter", counter );

        ksession.insert( new A(1) );
        ksession.insert( new B(1) );

        sessionClock.advanceTime( 20, TimeUnit.MILLISECONDS );
        ksession.insert( new C(1) );

        ksession.fireAllRules();
        assertEquals(0, counter.get());
    }

    public class A {
        private final int id;

        public A( int id ) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return "A(" + id + ")";
        }
    }

    public class B {
        private final int id;

        public B( int id ) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return "B(" + id + ")";
        }
    }

    public class C {
        private final int id;

        public C( int id ) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return "C(" + id + ")";
        }
    }


    @Role(Role.Type.EVENT)
    @Expires( "10s" )
    public static class ExpiringEventA { }

    @Role(Role.Type.EVENT)
    @Expires( value = "30s", policy = TIME_SOFT )
    public static class ExpiringEventB { }

    @Role(Role.Type.EVENT)
    @Expires( value = "30s", policy = TIME_SOFT )
    public static class ExpiringEventC { }

    @Test
    public void testSoftExpiration() {
        // DROOLS-1483
        String drl = "import " + ExpiringEventA.class.getCanonicalName() + "\n" +
                     "import " + ExpiringEventB.class.getCanonicalName() + "\n" +
                     "import " + ExpiringEventC.class.getCanonicalName() + "\n" +
                     "rule Ra when\n" +
                     "  $e : ExpiringEventA() over window:time(20s)\n" +
                     "then end\n " +
                     "rule Rb when\n" +
                     "  $e : ExpiringEventB() over window:time(20s)\n" +
                     "then end\n " +
                     "rule Rc when\n" +
                     "  $e : ExpiringEventC()\n" +
                     "then end\n";

        KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        KieBase kbase = helper.build( EventProcessingOption.STREAM );
        KieSession ksession = kbase.newKieSession( sessionConfig, null );

        PseudoClockScheduler clock = ksession.getSessionClock();

        ksession.insert( new ExpiringEventA() );
        ksession.insert( new ExpiringEventB() );
        ksession.insert( new ExpiringEventC() );
        ksession.fireAllRules();

        clock.advanceTime( 5, TimeUnit.SECONDS );
        ksession.fireAllRules();

        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( ExpiringEventA.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( ExpiringEventB.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( ExpiringEventC.class ) ).size() );

        clock.advanceTime( 10, TimeUnit.SECONDS );
        ksession.fireAllRules();

        // t=15 -> hard expiration of A
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( ExpiringEventA.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( ExpiringEventB.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( ExpiringEventC.class ) ).size() );

        clock.advanceTime( 10, TimeUnit.SECONDS );
        ksession.fireAllRules();

        // t=25 -> implicit expiration of B
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( ExpiringEventA.class ) ).size() );
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( ExpiringEventB.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( ExpiringEventC.class ) ).size() );

        clock.advanceTime( 10, TimeUnit.SECONDS );
        ksession.fireAllRules();

        // t=35 -> soft expiration of C
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( ExpiringEventA.class ) ).size() );
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( ExpiringEventB.class ) ).size() );
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( ExpiringEventC.class ) ).size() );
    }

    @Test
    public void testSoftExpirationWithDeclaration() {
        // DROOLS-1483
        String drl = "import " + A.class.getCanonicalName() + "\n" +
                     "import " + B.class.getCanonicalName() + "\n" +
                     "import " + C.class.getCanonicalName() + "\n" +
                     "declare A @role( event ) @expires(10s) end\n" +
                     "declare B @role( event ) @expires(value = 30s, policy = TIME_SOFT) end\n" +
                     "declare C @role( event ) @expires(value = 30s, policy = TIME_SOFT) end\n" +
                     "rule Ra when\n" +
                     "  $e : A() over window:time(20s)\n" +
                     "then end\n " +
                     "rule Rb when\n" +
                     "  $e : B() over window:time(20s)\n" +
                     "then end\n " +
                     "rule Rc when\n" +
                     "  $e : C()\n" +
                     "then end\n";

        KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        KieBase kbase = helper.build( EventProcessingOption.STREAM );
        KieSession ksession = kbase.newKieSession( sessionConfig, null );

        PseudoClockScheduler clock = ksession.getSessionClock();

        ksession.insert( new A(1) );
        ksession.insert( new B(2) );
        ksession.insert( new C(3) );
        ksession.fireAllRules();

        clock.advanceTime( 5, TimeUnit.SECONDS );
        ksession.fireAllRules();

        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( A.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( B.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( C.class ) ).size() );

        clock.advanceTime( 10, TimeUnit.SECONDS );
        ksession.fireAllRules();

        // t=15 -> hard expiration of A
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( A.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( B.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( C.class ) ).size() );

        clock.advanceTime( 10, TimeUnit.SECONDS );
        ksession.fireAllRules();

        // t=25 -> implicit expiration of B
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( A.class ) ).size() );
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( B.class ) ).size() );
        assertEquals( 1, ksession.getObjects( new ClassObjectFilter( C.class ) ).size() );

        clock.advanceTime( 10, TimeUnit.SECONDS );
        ksession.fireAllRules();

        // t=35 -> soft expiration of C
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( A.class ) ).size() );
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( B.class ) ).size() );
        assertEquals( 0, ksession.getObjects( new ClassObjectFilter( C.class ) ).size() );
    }

    @Test
    public void testEventsExpiredInThePast() throws InterruptedException {
        final String drl =
                " package org.drools.compiler.integrationtests;\n" +
                " import " + BasicEvent.class.getCanonicalName() + ";\n" +
                " declare BasicEvent\n" +
                "     @role( event )\n" +
                "     @timestamp( eventTimestamp )\n" +
                "     @duration( eventDuration )\n" +
                " end\n" +
                " \n" +
                " rule R1\n" +
                " when\n" +
                "     $A : BasicEvent()\n" +
                "     $B : BasicEvent( this starts $A )\n" +
                " then \n" +
                " end\n";

        testEventsExpiredInThePast(drl);
    }

    @Test
    public void testEventsExpiredInThePastTemporalConstraint() throws InterruptedException {
        final String drl =
                " package org.drools.compiler.integrationtests;\n" +
                " import " + BasicEvent.class.getCanonicalName() + ";\n" +
                " declare BasicEvent\n" +
                "     @role( event )\n" +
                "     @timestamp( eventTimestamp )\n" +
                "     @duration( eventDuration )\n" +
                " end\n" +
                " \n" +
                " rule R1\n" +
                " when\n" +
                "     $A : BasicEvent()\n" +
                "     $B : BasicEvent( this starts[5ms] $A )\n" +
                " then \n" +
                " end\n";

        testEventsExpiredInThePast(drl);
    }

    private void testEventsExpiredInThePast(final String drl) {
        final KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        final KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        final KieBase kieBase = helper.build( EventProcessingOption.STREAM );
        final KieSession kieSession = kieBase.newKieSession( sessionConfig, null );

        PseudoClockScheduler clock = kieSession.getSessionClock();

        final long currentTime = clock.getCurrentTime();

        clock.advanceTime(100, TimeUnit.MILLISECONDS);

        kieSession.insert(new BasicEvent(new Date(currentTime + 20), 10L, "20ms-30ms"));
        clock.advanceTime(1, TimeUnit.MILLISECONDS);
        kieSession.insert(new BasicEvent(new Date(currentTime + 20), 20L, "20ms-40ms"));

        clock.advanceTime(100, TimeUnit.MILLISECONDS);

        Assertions.assertThat(kieSession.fireAllRules()).isEqualTo(1);
        clock.advanceTime(10, TimeUnit.MILLISECONDS);
        Assertions.assertThat(kieSession.getObjects()).isEmpty();
    }

    @Test
    public void testExpiredEventWithIdConstraint() throws InterruptedException {
        // DROOLS-4577
        testEventExpiredContraint(true, true);
    }

    @Test
    public void testExpiredEventWithoutIdConstraint() throws InterruptedException {
        testEventExpiredContraint(true, false);
    }

    @Test
    public void testNotExpiredEventWithIdConstraint() throws InterruptedException {
        testEventExpiredContraint(false, true);
    }

    @Test
    public void testNotExpiredEventWithoutIdConstraint() throws InterruptedException {
        testEventExpiredContraint(false, false);
    }

    private void testEventExpiredContraint(boolean expired, boolean constraint) throws InterruptedException {
        final String drl =
                " package org.drools.compiler.integrationtests;\n" +
                " import " + DummyEvent.class.getCanonicalName() + ";\n" +
                " declare DummyEvent\n" +
                "     @role( event )\n" +
                "     @timestamp( eventTimestamp )\n" +
                "     @expires( 2h )\n" +
                " end\n" +
                " \n" +
                " rule R1 when\n" +
                "     $dummyEvent : DummyEvent(state != \"release\")\n" +
                "     $dummyEventContext : DummyEvent(this != $dummyEvent, this before $dummyEvent, state != \"release\"" + (constraint ? ", idA == $dummyEvent.idA" : "") +")\n" +
                " then \n" +
                "     System.out.println(\"R1\");\n" +
                "     modify($dummyEventContext){setState(\"release\");} \n" +
                " end\n" +
                " rule R2 when\n" +
                "     $dummyEvent : DummyEvent(state != \"release\")\n" +
                "     $dummyEventContext : DummyEvent(this != $dummyEvent, this before $dummyEvent, state !=  \"release\"" + (constraint ? ", idB == $dummyEvent.idB" : "") + ")\n" +
                " then \n" +
                "     System.out.println(\"R2\");\n" +
                "     modify($dummyEventContext){setState(\"release\");} \n" +
                " end\n";

        final KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        final KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        final KieBase kieBase = helper.build( EventProcessingOption.STREAM );
        final KieSession kieSession = kieBase.newKieSession( sessionConfig, null );

        PseudoClockScheduler clock = kieSession.getSessionClock();
        final long currentTime = System.currentTimeMillis();
        clock.setStartupTime(currentTime - 1);

        kieSession.insert(new DummyEvent(1, currentTime));
        kieSession.insert(new DummyEvent(2, (expired ? currentTime - Duration.ofHours(8).toMillis() : currentTime + Duration.ofHours(8).toMillis())));

        Assertions.assertThat(kieSession.fireAllRules()).isEqualTo(1);
    }

    public interface ApplicationEvent { }

    public static class DummyEvent implements ApplicationEvent {
        private int id;

        private long eventTimestamp;

        private String state;

        public DummyEvent(int id, long eventTimestamp) {
            this.id = id;
            this.eventTimestamp = eventTimestamp;
            this.state = "initial";
        }

        public String getState() {
            return state;
        }

        public DummyEvent setState(String state) {
            this.state = state;
            return this;
        }

        public int getId() {
            return id;
        }

        public DummyEvent setId(int id) {
            this.id = id;
            return this;
        }

        public long getEventTimestamp() {
            return eventTimestamp;
        }

        public DummyEvent setEventTimestamp(long eventTimestamp) {
            this.eventTimestamp = eventTimestamp;
            return this;
        }

        public String getIdA() {
            return "A";
        }

        public String getIdB() {
            return "B";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DummyEvent that = (DummyEvent) o;
            return Objects.equals(this.id, that.id) &&
                    Objects.equals(this.eventTimestamp, that.eventTimestamp) &&
                    Objects.equals(this.state, that.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, eventTimestamp, state);
        }

        @Override
        public String toString() {
            return "DummyEvent{" + "id=" + id + ", eventTimestamp=" + eventTimestamp + ", state=" + state + '}';
        }
    }

    public static class OtherEvent implements ApplicationEvent {

        private int id;

        private long eventTimestamp;

        private int dummyEventId;

        public OtherEvent(int id, long eventTimestamp, int dummyEventId) {
            this.id = id;
            this.eventTimestamp = eventTimestamp;
            this.dummyEventId = dummyEventId;
        }

        @Override
        public String toString() {
            return "OtherEvent{" + "id=" + id + ", eventTimestamp=" + eventTimestamp + ", dummyEventId=" + dummyEventId + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OtherEvent that = (OtherEvent) o;
            return id == that.id && eventTimestamp == that.eventTimestamp && dummyEventId == that.dummyEventId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, eventTimestamp, dummyEventId);
        }

        public int getId() {
            return id;
        }

        public long getEventTimestamp() {
            return eventTimestamp;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setEventTimestamp(long eventTimestamp) {
            this.eventTimestamp = eventTimestamp;
        }

        public int getDummyEventId() {
            return dummyEventId;
        }

        public void setDummyEventId(int dummyEventId) {
            this.dummyEventId = dummyEventId;
        }
    }

    @Test
    public void testEventSameContraint() throws InterruptedException {
        // DROOLS-4580
        final String drl =
                " package org.drools.compiler.integrationtests;\n" +
                " import " + DummyEvent.class.getCanonicalName() + ";\n" +
                " declare DummyEvent\n" +
                "     @role( event )\n" +
                "     @timestamp( eventTimestamp )\n" +
                " end\n" +
                " \n" +
                " rule R1 when\n" +
                "     $dummyEvent : DummyEvent(state != \"release\")\n" +
                "     $otherDummyEvent : DummyEvent(this != $dummyEvent, this before $dummyEvent, idA == $dummyEvent.idA)\n" +
                " then \n" +
                "     System.out.println(\"R1\");\n" +
                " end\n" +
                " rule R2 when\n" +
                "     $dummyEvent : DummyEvent(state != \"release\")\n" +
                "     $otherDummyEvent : DummyEvent(this != $dummyEvent, this before $dummyEvent, idA == $dummyEvent.idA)\n" +
                " then \n" +
                "     System.out.println(\"R2\");\n" +
                " end\n";

        final KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        final KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        final KieBase kieBase = helper.build( EventProcessingOption.STREAM );
        final KieSession kieSession = kieBase.newKieSession( sessionConfig, null );

        PseudoClockScheduler clock = kieSession.getSessionClock();
        final long currentTime = System.currentTimeMillis();
        clock.setStartupTime(currentTime - 1);

        kieSession.insert(new DummyEvent(1, currentTime));
        kieSession.insert(new DummyEvent(2, currentTime - Duration.ofHours(8).toMillis()));

        Assertions.assertThat(kieSession.fireAllRules()).isEqualTo(2);
    }

    @Test
    public void testCollectWithExpiredEvent() {
        // DROOLS-4626
        testCollectExpiredEvent(true);
    }

    @Test
    public void testCollectWithoutExpiredEvent() {
        testCollectExpiredEvent(false);
    }

    private void testCollectExpiredEvent(boolean expired) {

        final String drl =
                " package org.drools.compiler.integrationtests;\n" +
                " import " + DummyEvent.class.getCanonicalName() + ";\n" +
                " import java.util.Collection\n" +
                " declare DummyEvent\n" +
                "     @role( event )\n" +
                "     @timestamp( eventTimestamp )\n" +
                "     @expires( 3d )\n" +
                " end\n" +
                " rule R when\n" +
                "     $listEvent: Collection(size >= 2) from collect (DummyEvent())" +
                " then \n" +
                "	  System.out.println(\"R is fired\");  \n" +
                " end\n";

        KieBaseConfiguration baseConfig = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        baseConfig.setOption( EqualityBehaviorOption.EQUALITY);
        baseConfig.setOption(EventProcessingOption.STREAM);

        final KieSessionConfiguration sessionConfig = new SessionConfigurationImpl();
        sessionConfig.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));

        final KieHelper helper = new KieHelper();
        helper.addContent(drl, ResourceType.DRL);
        final KieBase kieBase = helper.build(baseConfig);
        final KieSession kieSession = kieBase.newKieSession(sessionConfig, null);

        //clock init to current time
        PseudoClockScheduler clock = kieSession.getSessionClock();
        long currentTime = System.currentTimeMillis();
        clock.setStartupTime(currentTime);

        kieSession.fireAllRules();

        //facts inserts
        final DummyEvent event1 = new DummyEvent(1, currentTime);

        long timestamp = currentTime;

        if (expired) {
            timestamp = currentTime - Duration.ofDays(8).toMillis();	//8 days in the past...
        }

        final DummyEvent event2 = new DummyEvent(2, timestamp);

        kieSession.insert(event1);
        kieSession.insert(event2);

        Assertions.assertThat(kieSession.fireAllRules()).isEqualTo(1);
    }

    @Test
    public void testSameConstraintAllExpired() {
        // DROOLS-4656
        testMultipleExpiredEvent(2, false);
    }

    @Test
    public void testSameConstraintOneExpired() {
        testMultipleExpiredEvent(1, false);
    }

    @Test
    public void testSameConstraintNoExpiration() {
        testMultipleExpiredEvent(0, false);
    }

     @Test
    public void testDifferentConstraintAllExpired() {
        // DROOLS-4656
         testMultipleExpiredEvent(2, true);
    }

    @Test
    public void testDifferentConstraintOneExpired() {
        testMultipleExpiredEvent(1, true);
    }

    @Test
    public void testDifferentConstraintNoExpiration() {
        testMultipleExpiredEvent(0, true);
    }

    private void testMultipleExpiredEvent(int expiredNumber, boolean differentConstraint) {

        final String drl =
                " package org.drools.compiler.integrationtests;\n" +
                " import " + DummyEvent.class.getCanonicalName() + ";\n" +
                " declare DummyEvent\n" +
                "     @role( event )\n" +
                "     @timestamp( eventTimestamp )\n" +
                "	  @expires( 2h )\n" +
                " end\n" +
                " rule R1 when\n" +
                "     $dummyEvent : DummyEvent()\n" +
                "     $otherDummyEvent : DummyEvent( this before $dummyEvent )\n" +
                " then \n" +
                "    System.out.println(\"R1 Fired\"); \n" +
                " end\n" +
                " rule R2 when\n" +
                "     $dummyEvent : DummyEvent()\n" +
                (differentConstraint ?
                "     $otherDummyEvent : DummyEvent( state == \"initial\", this before $dummyEvent )\n" :
                "     $otherDummyEvent : DummyEvent( this before $dummyEvent )\n" ) +
                " then \n" +
                "    System.out.println(\"R2 Fired\"); \n" +
                " end\n";

        final KieSessionConfiguration sessionConfig = new SessionConfigurationImpl();
        sessionConfig.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));

        KieSession kieSession = new KieHelper()
                .addContent(drl, ResourceType.DRL)
                .build(EventProcessingOption.STREAM)
                .newKieSession(sessionConfig, null);

        //clock init to current time
        PseudoClockScheduler clock = kieSession.getSessionClock();
        long currentTime = System.currentTimeMillis();
        clock.setStartupTime(currentTime);

        kieSession.fireAllRules();

        //facts inserts
        long timeStampEvent1;
        long timeStampEvent2 ;

        switch (expiredNumber){
            case 2:
                timeStampEvent1 = currentTime - Duration.ofHours(9).toMillis();	// oldest, expired
                timeStampEvent2 = currentTime - Duration.ofHours(8).toMillis(); // expired
                break;
            case 1:
                timeStampEvent1 = currentTime - Duration.ofHours(8).toMillis(); //expired
                timeStampEvent2 = currentTime;
                break;
            default:
                timeStampEvent1 = currentTime - Duration.ofHours(1).toMillis(); //oldest, not expired
                timeStampEvent2 = currentTime;
        }

        final DummyEvent event1 = new DummyEvent(1, timeStampEvent1);
        final DummyEvent event2 = new DummyEvent(2, timeStampEvent2);

        kieSession.insert(event1);
        kieSession.insert(event2);

        assertEquals(2, kieSession.fireAllRules());
        assertEquals(2 - expiredNumber, kieSession.getObjects().size());
    }

    @Test
    public void testInstanceofExpired() {
        // DROOLS-4660
        testEvalExpiredEvent(true, false);
    }

    @Test
    public void testInstanceofNotExpired() {
        testEvalExpiredEvent(false, false);
    }

    @Test
    public void testEvalExpired() {
        // DROOLS-4660
        testEvalExpiredEvent(true, true);
    }

    @Test
    public void testEvalNotExpired() {
        testEvalExpiredEvent(false, true);
    }

    private void testEvalExpiredEvent(boolean isExpired, boolean useEval) {

        final String drl =
                " package org.drools.compiler.integrationtests;\n" +
                        " import " + DummyEvent.class.getCanonicalName() + ";\n" +
                        " import " + OtherEvent.class.getCanonicalName() + ";\n" +
                        " import " + ApplicationEvent.class.getCanonicalName() + ";\n" +
                        " declare DummyEvent\n" +
                        "     @role( event )\n" +
                        "     @timestamp( eventTimestamp )\n" +
                        "     @expires( 1s )\n" +
                        " end\n" +
                        " declare OtherEvent\n" +
                        "     @role( event )\n" +
                        "     @timestamp( eventTimestamp )\n" +
                        "     @expires( 1s )\n" +
                        " end\n" +
                        " rule R1 when\n" +
                        "     $dummyEvent : DummyEvent()\n" +
                        "     $otherEvent : OtherEvent()\n" +
                        " then\n"+
                        "     System.out.println(\"R1 is fired\");\n"+
                        " end\n" +
                        " rule R2 when\n"+
                        ( useEval ?
                        "     $evt : ApplicationEvent() \n" +
                        "     eval( !($evt.getClass().getSimpleName().equals(\"DummyEvent\"))) \n" :
                        "     $evt : ApplicationEvent( !(this instanceof DummyEvent))\n" ) +
                        " then\n"+
                        "     System.out.println(\"R2 is fired\");\n"+
                        " end";

        final KieSessionConfiguration sessionConfig = new SessionConfigurationImpl();
        sessionConfig.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));

        KieSession kieSession = new KieHelper()
                .addContent(drl, ResourceType.DRL)
                .build(EventProcessingOption.STREAM)
                .newKieSession(sessionConfig, null);

        //clock init to current time
        PseudoClockScheduler clock = kieSession.getSessionClock();
        long currentTime = System.currentTimeMillis();
        clock.setStartupTime(currentTime);

        kieSession.fireAllRules();

        long timestamp = currentTime;

        //facts inserts
        if(isExpired){
            timestamp = currentTime - Duration.ofDays(8).toMillis();	//8 days in the past...
        }

        final DummyEvent dummyEvent = new DummyEvent(1, timestamp);
        final OtherEvent otherEvent = new OtherEvent(2, timestamp, 1);

        kieSession.insert(dummyEvent);
        kieSession.insert(otherEvent);

        assertEquals(2, kieSession.fireAllRules());
        assertEquals(isExpired ? 0 : 2, kieSession.getObjects().size());
    }

    @Test
    public void testAlphaExpired_1() throws InterruptedException {
        // DROOLS-5050
        final String drl =
                " package org.drools.compiler.integrationtests;\n" +
                        " import " + DummyEvent.class.getCanonicalName() + ";\n" +
                        " import " + ApplicationEvent.class.getCanonicalName() + ";\n" +
                        " declare DummyEvent\n" +
                        "     @role( event )\n" +
                        "     @timestamp( eventTimestamp )\n" +
                        "     @expires( 1s )\n" +
                        " end\n" +
                        " rule R1\n" +
                        " when\n" +
                        "     $evt : DummyEvent()\n" +
                        " then \n" +
                        " end\n" +
                        " rule R2\n" +
                        " when\n" +
                        "     $evt : DummyEvent()\n" +
                        " then \n" +
                        " end\n" +
                        " rule R3\n" +
                        " when\n" +
                        "     $evt : ApplicationEvent()\n" +
                        " then \n" +
                        " end\n";

        final KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        final KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        final KieBase kieBase = helper.build( EventProcessingOption.STREAM );
        final KieSession kieSession = kieBase.newKieSession( sessionConfig, null );

        PseudoClockScheduler clock = kieSession.getSessionClock();
        final long currentTime = clock.getCurrentTime();
        clock.advanceTime(10, TimeUnit.SECONDS);

        kieSession.insert(new DummyEvent(10, currentTime));

        Assertions.assertThat(kieSession.fireAllRules()).isEqualTo(3);
        Assertions.assertThat(kieSession.fireAllRules()).isEqualTo(0);
    }

    @Test
    public void testAlphaExpired_2() throws InterruptedException {
        // DROOLS-5050
        final String drl =
                " package org.drools.compiler.integrationtests;\n" +
                        " import " + DummyEvent.class.getCanonicalName() + ";\n" +
                        " import " + ApplicationEvent.class.getCanonicalName() + ";\n" +
                        " declare DummyEvent\n" +
                        "     @role( event )\n" +
                        "     @timestamp( eventTimestamp )\n" +
                        "     @expires( 1s )\n" +
                        " end\n" +
                        " rule R1\n" +
                        " when\n" +
                        "     $evt : DummyEvent()\n" +
                        " then \n" +
                        " end\n" +
                        " rule R2\n" +
                        " when\n" +
                        "     $evt : ApplicationEvent()\n" +
                        " then \n" +
                        " end\n";

        final KieSessionConfiguration sessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        final KieHelper helper = new KieHelper();
        helper.addContent( drl, ResourceType.DRL );
        final KieBase kieBase = helper.build( EventProcessingOption.STREAM );
        final KieSession kieSession = kieBase.newKieSession( sessionConfig, null );

        PseudoClockScheduler clock = kieSession.getSessionClock();
        final long currentTime = clock.getCurrentTime();
        clock.advanceTime(10, TimeUnit.SECONDS);

        kieSession.insert(new DummyEvent(10, currentTime));

        Assertions.assertThat(kieSession.fireAllRules()).isEqualTo(2);
        Assertions.assertThat(kieSession.fireAllRules()).isEqualTo(0);
    }
}
