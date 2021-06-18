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

package org.drools.compiler.integrationtests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.api.Assertions;
import org.drools.core.base.UndefinedCalendarExcption;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.drools.core.util.DateUtils;
import org.drools.testcoverage.common.model.Alarm;
import org.drools.testcoverage.common.model.Cheese;
import org.drools.testcoverage.common.model.FactA;
import org.drools.testcoverage.common.model.FactB;
import org.drools.testcoverage.common.model.Person;
import org.drools.testcoverage.common.model.Pet;
import org.drools.testcoverage.common.model.StockTick;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.KieSessionTestConfiguration;
import org.drools.testcoverage.common.util.KieUtil;
import org.drools.testcoverage.common.util.TestParametersUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieBase;
import org.kie.api.builder.KieBuilder;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.TimedRuleExecutionOption;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.time.Calendar;
import org.kie.api.time.SessionClock;
import org.kie.api.time.SessionPseudoClock;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class TimerAndCalendarTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public TimerAndCalendarTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
        this.kieBaseTestConfiguration = kieBaseTestConfiguration;
    }

    @Parameterized.Parameters(name = "KieBase type={0}")
    public static Collection<Object[]> getParameters() {
        return TestParametersUtil.getKieBaseStreamConfigurations(true);
    }

    @Test(timeout = 15000)
    public void testDuration() throws Exception {
        final String drl = "package org.drools.compiler.test;\n" +
                "\n" +
                "import " + Cheese.class.getCanonicalName() + ";\n" +
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "global java.util.List list;\n" +
                "\n" +
                "rule delayed\n" +
                "    duration 100\n" +
                "    when\n" +
                "        cheese : Cheese( )\n" +
                "    then\n" +
                "        list.add( cheese );\n" +
                "end ";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            final List list = new ArrayList();
            ksession.setGlobal("list", list);

            final Cheese brie = new Cheese("brie", 12);
            ksession.insert(brie);

            ksession.fireAllRules();
            // now check for update
            assertEquals(0, list.size());
            // sleep for 500ms
            Thread.sleep(500);
            ksession.fireAllRules();
            // now check for update
            assertEquals(1, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testDurationWithNoLoop() throws Exception {
        final String drl = "package org.drools.compiler.test;\n" +
                "\n" +
                "import " + Cheese.class.getCanonicalName() + ";\n" +
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "global java.util.List list;\n" +
                "\n" +
                "rule delayed\n" +
                "    timer 100\n" +
                "    no-loop true\n" +
                "    when\n" +
                "        cheese : Cheese( )\n" +
                "    then\n" +
                "        list.add( cheese );\n" +
                "end";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            final List list = new ArrayList();
            ksession.setGlobal("list", list);

            final Cheese brie = new Cheese("brie", 12);
            ksession.insert(brie);
            ksession.fireAllRules();

            // now check for update
            assertEquals(0, list.size());

            // sleep for 300ms
            Thread.sleep(300);

            ksession.fireAllRules();
            // now check for update
            assertEquals(1, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testDurationMemoryLeakonRepeatedUpdate() {
        final String drl =
            "package org.drools.compiler.test\n" +
            "import " + Alarm.class.getCanonicalName() + "\n" +
            "global java.util.List list;" +
            "rule \"COMPTEUR\"\n" +
            "  timer (int: 50s)\n" +
            "  when\n" +
            "    $alarm : Alarm( number < 5 )\n" +
            "  then\n" +
            "    $alarm.incrementNumber();\n" +
            "    list.add( $alarm );\n" +
            "    update($alarm);\n" + 
            "end\n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            final List list = new ArrayList();
            ksession.setGlobal("list", list);
            ksession.insert(new Alarm());

            ksession.fireAllRules();

            for (int i = 0; i < 6; i++) {
                timeService.advanceTime(55, TimeUnit.SECONDS);
                ksession.fireAllRules();
            }

            assertEquals(5, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testFireRuleAfterDuration() throws Exception {
        final String drl = "package org.drools.compiler.test;\n" +
                "import " + Cheese.class.getCanonicalName() + ";\n" +
                "global java.util.List list;\n" +
                "\n" +
                "rule delayed\n" +
                "    duration 100\n" +
                "    when\n" +
                "        cheese : Cheese( $type:type == \"brie\" )\n" +
                "    then\n" +
                "        list.add( cheese );\n" +
                "        insert(new Cheese(\"stilton\", 42));\n" +
                "        delete(cheese);\n" +
                "end\n" +
                "\n" +
                "rule after_delayed\n" +
                "    when\n" +
                "        cheese : Cheese( $type:type == \"stilton\" )\n" +
                "    then\n" +
                "        list.add( cheese );\n" +
                "end";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            final List list = new ArrayList();
            ksession.setGlobal("list", list);

            final Cheese brie = new Cheese("brie", 12);
            ksession.insert(brie);
            ksession.fireAllRules();

            // now check for update
            assertEquals(0, list.size());

            // sleep for 300ms
            Thread.sleep(300);

            ksession.fireAllRules();

            // now check for update
            assertEquals(2, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testNoProtocolIntervalTimer() {
        testIntervalTimer(true);
    }

    @Test(timeout = 10000)
    public void testIntervalTimer() {
        testIntervalTimer(false);
    }

    private void testIntervalTimer(final boolean noProtocol) {
        final String drl =
            "package org.simple \n" +
            "global java.util.List list \n" +
            "rule xxx \n" +
                    (noProtocol ? "  duration (30s 10s) " : "  timer (int:30s 10s) ") +
            "when \n" +
            "then \n" +
            "  list.add(\"fired\"); \n" +
            "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(15, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(3, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(2, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testIntervalTimerWithoutFire() {
        final String drl =
                "package org.simple \n" +
                        "global java.util.List list \n" +
                        "rule xxx \n" +
                        "  timer (int:30s 10s) " +
                        "when \n" +
                        "then \n" +
                        "  list.add(\"fired\"); \n" +
                        "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSessionConfiguration kieSessionConfiguration = KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration();
        kieSessionConfiguration.setOption(TimedRuleExecutionOption.YES);
        final KieSession ksession = kbase.newKieSession(kieSessionConfiguration, null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(35, TimeUnit.SECONDS);
            assertEquals(1, list.size());

            timeService.advanceTime(10, TimeUnit.SECONDS);
            assertEquals(2, list.size());

            timeService.advanceTime(10, TimeUnit.SECONDS);
            assertEquals(3, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testExprIntervalTimerRaceCondition() {
        final String drl =
            "package org.simple \n" +
            "global java.util.List list \n" +
            "rule xxx \n" +
            "  timer (expr: $i, $i) \n" +
            "when \n" +
            "   $i : Long() \n" +
            "then \n" +
            "  list.add(\"fired\"); \n" +
            "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            ksession.setGlobal("list", list);
            final FactHandle fh = ksession.insert(10000L);

            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(17, TimeUnit.SECONDS);
            ksession.update(fh, 5000L);
            ksession.fireAllRules();
            assertEquals(2, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testUnknownProtocol() {
        wrongTimerExpression("xyz:30");
    }

    @Test(timeout = 10000)
    public void testMissingColon() {
        wrongTimerExpression("int 30");
    }

    @Test(timeout = 10000)
    public void testMalformedExpression() {
        wrongTimerExpression("30s s30");
    }

    @Test(timeout = 10000)
    public void testMalformedIntExpression() {
        wrongTimerExpression("int 30s");
    }

    @Test(timeout = 10000)
    public void testMalformedCronExpression() {
        wrongTimerExpression("cron: 0/30 * * * * *");
    }

    private void wrongTimerExpression(final String timer) {
        final String drl =
            "package org.simple \n" +
            "rule xxx \n" +
            "  timer (" + timer + ") " +
            "when \n" +
            "then \n" +
            "end  \n";

        final KieBuilder kieBuilder = KieUtil.getKieBuilderFromDrls(kieBaseTestConfiguration,
                                                                    false,
                                                                    drl);
        Assertions.assertThat(kieBuilder.getResults().getMessages()).isNotEmpty();
    }

    @Test(timeout = 10000)
    public void testCronTimer() throws Exception {
        final String drl =
            "package org.simple \n" +
            "global java.util.List list \n" +
            "rule xxx \n" +
            "  timer (cron:15 * * * * ?) " +
            "when \n" +
            "then \n" +
            "  list.add(\"fired\"); \n" +
            "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(30, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(30, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testCalendarNormalRuleSingleCalendar() throws Exception {
        final String drl =
            "package org.simple \n" +
            "global java.util.List list \n" +
            "rule xxx \n" +
            "  calendars \"cal1\"\n" +
            "when \n" +
            "  String()\n" +
            "then \n" +
            "  list.add(\"fired\"); \n" +
            "end  \n";

        final Calendar calFalse = timestamp -> false;
        final Calendar calTrue = timestamp -> true;

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            ksession.getCalendars().set("cal1", calTrue);

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
            ksession.setGlobal("list", list);
            ksession.insert("o1");
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.insert("o2");
            ksession.fireAllRules();
            assertEquals(2, list.size());

            ksession.getCalendars().set("cal1", calFalse);
            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.insert("o3");
            ksession.fireAllRules();
            assertEquals(2, list.size());

            ksession.getCalendars().set("cal1", calTrue);
            timeService.advanceTime(30, TimeUnit.SECONDS);
            ksession.insert("o4");
            ksession.fireAllRules();
            assertEquals(3, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testUndefinedCalendar() {
        final String drl =
            "rule xxx \n" +
            "  calendars \"cal1\"\n" +
            "when \n" +
            "then \n" +
            "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            try {
                ksession.fireAllRules();
                fail("should throw UndefinedCalendarExcption");
            } catch (final UndefinedCalendarExcption ignored) {
            }
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testCalendarNormalRuleMultipleCalendars() throws Exception {
        final String drl =
            "package org.simple \n" +
            "global java.util.List list \n" +
            "rule xxx \n" +
            "  calendars \"cal1\", \"cal2\"\n" +
            "when \n" +
            "  String()\n" +
            "then \n" +
            "  list.add(\"fired\"); \n" +
            "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final Calendar calFalse = timestamp -> false;

            final Calendar calTrue = timestamp -> true;

            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            ksession.getCalendars().set("cal1", calTrue);
            ksession.getCalendars().set("cal2", calTrue);

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
            ksession.setGlobal("list", list);
            ksession.insert("o1");
            ksession.fireAllRules();
            assertEquals(1, list.size());

            ksession.getCalendars().set("cal2", calFalse);
            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.insert("o2");
            ksession.fireAllRules();
            assertEquals(1, list.size());

            ksession.getCalendars().set("cal1", calFalse);
            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.insert("o3");
            ksession.fireAllRules();
            assertEquals(1, list.size());

            ksession.getCalendars().set("cal1", calTrue);
            ksession.getCalendars().set("cal2", calTrue);
            timeService.advanceTime(30, TimeUnit.SECONDS);
            ksession.insert("o4");
            ksession.fireAllRules();
            assertEquals(2, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testCalendarsWithCron() throws Exception {
        final String drl =
            "package org.simple \n" +
            "global java.util.List list \n" +
            "rule xxx \n" +
            "  calendars \"cal1\", \"cal2\"\n" +
            "  timer (cron:15 * * * * ?) " +
            "when \n" +
            "then \n" +
            "  list.add(\"fired\"); \n" +
            "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);

            final Date date1 = new Date(date.getTime() + (15 * 1000));
            final Date date2 = new Date(date1.getTime() + (60 * 1000));
            final Date date3 = new Date(date2.getTime() + (60 * 1000));
            final Date date4 = new Date(date3.getTime() + (60 * 1000));

            final Calendar cal1 = timestamp -> {
                if (timestamp == date1.getTime()) {
                    return true;
                } else {
                    return timestamp != date4.getTime();
                }
            };

            final Calendar cal2 = timestamp -> {
                if (timestamp == date2.getTime()) {
                    return false;
                } else if (timestamp == date3.getTime()) {
                    return true;
                } else {
                    return true;
                }
            };

            ksession.getCalendars().set("cal1", cal1);
            ksession.getCalendars().set("cal2", cal2);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(4, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testCalendarsWithIntervals() throws Exception {
        final String drl =
            "package org.simple \n" +
            "global java.util.List list \n" +
            "rule xxx \n" +
            "  calendars \"cal1\", \"cal2\"\n" +
            "  timer (15s 60s) " + //int: protocol is assume
            "when \n" +
            "then \n" +
            "  list.add(\"fired\"); \n" +
            "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);

            final Date date1 = new Date(date.getTime() + (15 * 1000));
            final Date date2 = new Date(date1.getTime() + (60 * 1000));
            final Date date3 = new Date(date2.getTime() + (60 * 1000));
            final Date date4 = new Date(date3.getTime() + (60 * 1000));

            final Calendar cal1 = timestamp -> {
                if (timestamp == date1.getTime()) {
                    return true;
                } else {
                    return timestamp != date4.getTime();
                }
            };

            final Calendar cal2 = timestamp -> {
                if (timestamp == date2.getTime()) {
                    return false;
                } else if (timestamp == date3.getTime()) {
                    return true;
                } else {
                    return true;
                }
            };

            ksession.getCalendars().set("cal1", cal1);
            ksession.getCalendars().set("cal2", cal2);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(4, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testCalendarsWithIntervalsAndStartAndEnd() throws Exception {
        final String drl =
            "package org.simple \n" +
            "global java.util.List list \n" +
            "rule xxx \n" +
            "  calendars \"cal1\"\n" +
            "  timer (0d 1d; start=3-JAN-2010, end=5-JAN-2010) " + //int: protocol is assume
            "when \n" +
            "then \n" +
            "  list.add(\"fired\"); \n" +
            "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
            final Date date = df.parse("1-JAN-2010");

            final Calendar cal1 = timestamp -> true;

            final long oneDay = 60 * 60 * 24;
            ksession.getCalendars().set("cal1", cal1);
            ksession.setGlobal("list", list);

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);  // day 3
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);   // day 5
            ksession.fireAllRules();
            assertEquals(3, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testCalendarsWithIntervalsAndStartAndLimit() throws Exception {
        final String drl =
            "package org.simple \n" +
            "global java.util.List list \n" +
            "rule xxx \n" +
            "  calendars \"cal1\"\n" +
            "  timer (0d 1d; start=3-JAN-2010, repeat-limit=4) " + //int: protocol is assume
            "when \n" +
            "then \n" +
            "  list.add(\"fired\"); \n" +
            "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
            final Date date = df.parse("1-JAN-2010");

            final Calendar cal1 = timestamp -> true;

            final long oneDay = 60 * 60 * 24;
            ksession.getCalendars().set("cal1", cal1);
            ksession.setGlobal("list", list);

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 3
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);   // day 5
            ksession.fireAllRules();
            assertEquals(3, list.size());

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testCalendarsWithCronAndStartAndEnd() throws Exception {
        final Locale defaultLoc = Locale.getDefault();
        try {
            Locale.setDefault(Locale.UK); // Because of the date strings in the DRL, fixable with JBRULES-3444
            final String drl =
                    "package org.simple \n" +
                            "global java.util.List list \n" +
                            "rule xxx \n" +
                            "  date-effective \"2-JAN-2010\"\n" +
                            "  date-expires \"6-JAN-2010\"\n" +
                            "  calendars \"cal1\"\n" +
                            "  timer (cron: 0 0 0 * * ?) " +
                            "when \n" +
                            "then \n" +
                            "  list.add(\"fired\"); \n" +
                            "end  \n";

            final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
            final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
            try {
                final List list = new ArrayList();
                final PseudoClockScheduler timeService = ksession.getSessionClock();
                final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
                final Date date = df.parse("1-JAN-2010");

                final Calendar cal1 = timestamp -> true;

                final long oneDay = 60 * 60 * 24;
                ksession.getCalendars().set("cal1", cal1);
                ksession.setGlobal("list", list);

                timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
                ksession.fireAllRules();
                assertEquals(0, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertEquals(0, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 3
                ksession.fireAllRules();
                assertEquals(1, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertEquals(2, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);   // day 5
                ksession.fireAllRules();
                assertEquals(3, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertEquals(3, list.size());
            } finally {
                ksession.dispose();
            }
        } finally {
            Locale.setDefault(defaultLoc);
        }
    }

    @Test(timeout = 10000)
    public void testCalendarsWithCronAndStartAndLimit() throws Exception {
        final Locale defaultLoc = Locale.getDefault();
        try {
            Locale.setDefault(Locale.UK); // Because of the date strings in the DRL, fixable with JBRULES-3444
            final String drl =
                    "package org.simple \n" +
                            "global java.util.List list \n" +
                            "rule xxx \n" +
                            "  date-effective \"2-JAN-2010\"\n" +
                            "  calendars \"cal1\"\n" +
                            // FIXME: I have to set the repeate-limit to 6 instead of 4 becuase
                            // it is incremented regardless of the effective date
                            "  timer (cron: 0 0 0 * * ?; repeat-limit=6) " +
                            "when \n" +
                            "then \n" +
                            "  list.add(\"fired\"); \n" +
                            "end  \n";

            final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
            final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
            try {
                final List list = new ArrayList();
                final PseudoClockScheduler timeService = ksession.getSessionClock();
                final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
                final Date date = df.parse("1-JAN-2010");

                final Calendar cal1 = timestamp -> true;

                final long oneDay = 60 * 60 * 24;
                ksession.getCalendars().set("cal1", cal1);
                ksession.setGlobal("list", list);

                timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
                ksession.fireAllRules();
                assertEquals(0, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertEquals(0, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 3
                ksession.fireAllRules();
                assertEquals(1, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertEquals(2, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);   // day 5
                ksession.fireAllRules();
                assertEquals(3, list.size());

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertEquals(4, list.size());
            } finally {
                ksession.dispose();
            }
        } finally {
            Locale.setDefault(defaultLoc);
        }
    }

    @Test(timeout = 10000)
    public void testTimerWithNot() throws Exception {

        final KieBase kbase = KieBaseUtil.getKieBaseFromClasspathResources("timer-and-calendar-test", kieBaseTestConfiguration,
                                                                           "org/drools/compiler/integrationtests/test_Timer_With_Not.drl");
        final KieSession ksession = kbase.newKieSession();
        try {
            System.out.println("+++ First fire ");
            ksession.fireAllRules();
            Thread.sleep(1000);
            System.out.println("+++ Waited 200ms, second fire ");
            ksession.fireAllRules(); // fires insert A
            Thread.sleep(2000);
            System.out.println("+++ Waited 2000ms, Third fire fire ");
            ksession.fireAllRules(); // fires Wrap A
            // now check that rule "wrap A" fired once, creating one B
            assertEquals(2, ksession.getFactCount());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testHaltWithTimer() throws Exception {
        final String drl = "// fire once, for a String, create an Integer, halt!\n" +
                "rule x\n" +
                "timer(int:0 1000)\n" +
                "when\n" +
                "    $s: String( this == \"halt\" )\n" +
                "then\n" +
                "    insert( new Integer(1) );\n" +
                "    drools.halt();\n" +
                "end";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        new Thread(ksession::fireUntilHalt).start();
        try {
            Thread.sleep(1000);
            final FactHandle handle = ksession.insert("halt");
            Thread.sleep(2000);

            // now check that rule "halt" fired once, creating one Integer
            assertEquals(2, ksession.getFactCount());
            ksession.delete(handle);
        } finally {
            ksession.halt();
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testTimerRemoval() throws InterruptedException {
        final String drl = "package org.drools.compiler.test\n" +
                "import " + TimeUnit.class.getName() + "\n" +
                "global java.util.List list \n" +
                "global " + CountDownLatch.class.getName() + " latch\n" +
                "rule TimerRule \n" +
                "   timer (int:100 50) \n" +
                "when \n" +
                "then \n" +
                "        //forces it to pause until main thread is ready\n" +
                "        latch.await(10, TimeUnit.MINUTES); \n" +
                "        list.add(list.size()); \n" +
                " end";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final List<Integer> list = Collections.synchronizedList(new ArrayList<>());
            ksession.setGlobal("list", list);
            ksession.setGlobal("latch", latch);

            ksession.fireAllRules();
            Thread.sleep(500); // this makes sure it actually enters a rule
            kbase.removeRule("org.drools.compiler.test", "TimerRule");
            ksession.fireAllRules();
            latch.countDown();
            Thread.sleep(500); // allow the last rule, if we were in the middle of one to actually fire, before clearing
            ksession.fireAllRules();
            list.clear();
            Thread.sleep(500); // now wait to see if any more fire, they shouldn't
            ksession.fireAllRules();
            assertEquals(0, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testIntervalTimerWithLongExpressions() {
        final String drl = "package org.simple;\n" +
                "global java.util.List list;\n" +
                "\n" +
                "declare Bean\n" +
                "  delay   : long = 30000\n" +
                "  period  : long = 10000\n" +
                "end\n" +
                "\n" +
                "rule init \n" +
                "when \n" +
                "then \n" +
                " insert( new Bean() );\n" +
                "end \n" +
                "\n" +
                "rule xxx\n" +
                "  salience ($d) \n" +
                "  timer( expr: $d, $p; start=3-JAN-2010 )\n" +
                "when\n" +
                "  Bean( $d : delay, $p : period )\n" +
                "then\n" +
                "  list.add( \"fired\" );\n" +
                "end";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.setStartupTime(DateUtils.parseDate("3-JAN-2010").getTime());

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(15, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(3, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(2, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testIntervalTimerAfterEnd() {
        // DROOLS-5908
        final String drl = "package org.simple;\n" +
                "global java.util.List list;\n" +
                "\n" +
                "declare Bean\n" +
                "  delay   : long = 30000\n" +
                "  period  : long = 10000\n" +
                "end\n" +
                "\n" +
                "rule init \n" +
                "when \n" +
                "then \n" +
                " insert( new Bean() );\n" +
                "end \n" +
                "\n" +
                "rule xxx\n" +
                "  salience ($d) \n" +
                "  timer( expr: $d, $p; end=3-JAN-2010 )\n" +
                "when\n" +
                "  Bean( $d : delay, $p : period )\n" +
                "then\n" +
                "  list.add( \"fired\" );\n" +
                "end";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.setStartupTime(DateUtils.parseDate("3-JAN-2010").getTime());

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertEquals(0, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testIntervalTimerWithStringExpressions() {
        checkIntervalTimerWithStringExpressions(false, "3-JAN-2010");
    }

    @Test(timeout = 10000)
    public void testIntervalTimerWithAllExpressions() {
        checkIntervalTimerWithStringExpressions(true, "3-JAN-2010");
    }

    @Test(timeout = 10000)
    public void testIntervalTimerWithStringExpressionsAfterStart() {
        checkIntervalTimerWithStringExpressions(false, "3-FEB-2010");
    }

    @Test(timeout = 10000)
    public void testIntervalTimerWithAllExpressionsAfterStart() {
        checkIntervalTimerWithStringExpressions(true, "3-FEB-2010");
    }

    private void checkIntervalTimerWithStringExpressions(final boolean useExprForStart, final String startTime) {
        final String drl = "package org.simple;\n" +
                "global java.util.List list;\n" +
                "\n" +
                "declare Bean\n" +
                "  delay   : String = \"30s\"\n" +
                "  period  : long = 60000\n" +
                "  start   : String = \"3-JAN-2010\"\n" +
                "end\n" +
                "\n" +
                "rule init \n" +
                "when \n" +
                "then \n" +
                " insert( new Bean() );\n" +
                "end \n" +
                "\n" +
                "rule xxx\n" +
                "  salience ($d) \n" +
                "  timer( expr: $d, $p; start=" + (useExprForStart ? "$s" : "3-JAN-2010") + " )\n" +
                "when\n" +
                "  Bean( $d : delay, $p : period, $s : start )\n" +
                "then\n" +
                "  list.add( \"fired\" );\n" +
                "end";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.setStartupTime(DateUtils.parseDate(startTime).getTime());

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(40, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());

            // simulate a pause in the use of the engine by advancing the system clock
            timeService.setStartupTime(DateUtils.parseDate("3-MAR-2010").getTime());
            list.clear();

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size()); // fires once to recover from missing activation

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(40, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(4, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testIntervalTimerExpressionWithOr() {
        final String drl = "package org.kie.test\n"
                + "global java.util.List list\n"
                + "import " + FactA.class.getCanonicalName() + "\n"
                + "import " + FactB.class.getCanonicalName() + "\n"
                + "import " + Pet.class.getCanonicalName() + "\n"
                + "rule r1 timer (expr: f1.field2, f1.field2; repeat-limit=3)\n"
                + "when\n"
                + "    foo: FactB()\n"
                + "    ( Pet()  and f1 : FactA( field1 == 'f1') ) or \n"
                + "    f1 : FactA(field1 == 'f2') \n"
                + "then\n"
                + "    list.add( f1 );\n"
                + "    foo.setF1( 'xxx' );\n"
                + "end\n" + "\n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            final List list = new ArrayList();
            ksession.setGlobal("list", list);
            ksession.insert(new FactB());
            ksession.insert(new Pet("cinchilla"));

            final FactA fact1 = new FactA();
            fact1.setField1("f1");
            fact1.setField2(250);

            final FactA fact3 = new FactA();
            fact3.setField1("f2");
            fact3.setField2(1000);

            ksession.insert(fact1);
            ksession.insert(fact3);

            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());
            Assert.assertEquals(fact1, list.get(0));

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());
            Assert.assertEquals(fact1, list.get(1));

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size()); // did not change, repeat-limit kicked in

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());
            Assert.assertEquals(fact3, list.get(2));

            timeService.advanceTime(1000, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(4, list.size());
            Assert.assertEquals(fact3, list.get(3));

            timeService.advanceTime(1000, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(4, list.size()); // did not change, repeat-limit kicked in
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testExprTimeRescheduled() {
        final String drl = "package org.kie.test\n"
                + "global java.util.List list\n"
                + "import " + FactA.class.getCanonicalName() + "\n"
                + "rule r1 timer (expr: f1.field2, f1.field4)\n"
                + "when\n"
                + "    f1 : FactA() \n"
                + "then\n"
                + "    list.add( f1 );\n"
                + "end\n" + "\n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            final List list = new ArrayList();
            ksession.setGlobal("list", list);

            final FactA fact1 = new FactA();
            fact1.setField1("f1");
            fact1.setField2(500);
            fact1.setField4(1000);
            final FactHandle fh = ksession.insert(fact1);

            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(1100, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());
            Assert.assertEquals(fact1, list.get(0));

            timeService.advanceTime(1100, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());
            Assert.assertEquals(fact1, list.get(1));

            timeService.advanceTime(400, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());
            Assert.assertEquals(fact1, list.get(2));
            list.clear();

            // the activation state of the rule is not changed so the timer isn't reset
            // since the timer alredy fired it will only use only the period that now will be set to 2000
            fact1.setField2(300);
            fact1.setField4(2000);
            ksession.update(fh, fact1);
            ksession.fireAllRules();

            // 100 has passed of the 1000, from the previous schedule
            // so that should be deducted from the 2000 period above, meaning
            //  we only need to increment another 1950
            timeService.advanceTime(1950, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());
            Assert.assertEquals(fact1, list.get(0));
            list.clear();

            timeService.advanceTime(1000, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(700, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    @Ignore
    public void testHaltAfterSomeTimeThenRestart() throws Exception {
        final String drl = "package org.kie.test;" +
                "global java.util.List list; \n" +
                "\n" +
                "\n" +
                "rule FireAtWill\n" +
                "timer(int:0 100)\n" +
                "when  \n" +
                "then \n" +
                "  list.add( 0 );\n" +
                "end\n" +
                "\n" +
                "rule ImDone\n" +
                "when\n" +
                "  String( this == \"halt\" )\n" +
                "then\n" +
                "  drools.halt();\n" +
                "end\n" +
                "\n" +
                "rule Hi \n" +
                "salience 10 \n" +
                "when \n" +
                "  String( this == \"trigger\" ) \n" +
                "then \n " +
                "  list.add( 5 ); \n" +
                "end \n" +
                "\n" +
                "rule Lo \n" +
                "salience -5 \n" +
                "when \n" +
                "  String( this == \"trigger\" ) \n" +
                "then \n " +
                "  list.add( -5 ); \n" +
                "end \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            final List list = new ArrayList();
            ksession.setGlobal("list", list);

            new Thread(ksession::fireUntilHalt).start();
            Thread.sleep(250);

            assertEquals(asList(0, 0, 0), list);

            ksession.insert("halt");
            ksession.insert("trigger");
            Thread.sleep(300);
            assertEquals(asList(0, 0, 0), list);

            new Thread(ksession::fireUntilHalt).start();
            Thread.sleep(200);

            assertEquals(asList(0, 0, 0, 5, 0, -5, 0, 0), list);
        } finally {
            ksession.halt();
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    public void testHaltAfterSomeTimeThenRestartButNoLongerHolding() throws Exception {
        final String drl = "package org.kie.test;" +
                "global java.util.List list; \n" +
                "\n" +
                "\n" +
                "rule FireAtWill\n" +
                "   timer(int:0 200)\n" +
                "when  \n" +
                "  eval(true)" +
                "  String( this == \"trigger\" )" +
                "then \n" +
                "  System.out.println(\"--- FireAtWill, adding 0 to list\");\n" +
                "  list.add( 0 );\n" +
                "  System.out.println(\"--- List is now: \" + list);\n" +
                "end\n" +
                "\n" +
                "rule ImDone\n" +
                "when\n" +
                "  String( this == \"halt\" )\n" +
                "then\n" +
                "  System.out.println(\"--- ImDone drools halt\");\n" +
                "  drools.halt();\n" +
                "end\n" +
                "\n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();

        final List list = new ArrayList();
        ksession.setGlobal("list", list);

        final FactHandle handle = ksession.insert("trigger");

        System.out.println("--- Starting fireUntilHalt");
        new Thread(ksession::fireUntilHalt).start();
        try {
            System.out.println("--- Sleeping for 350ms");
            Thread.sleep(350);
            System.out.println("--- Slept for 350ms");
            System.out.println("--- Assert1 List " + list + " size is " + list.size());
            assertEquals(2, list.size()); // delay 0, repeat after 100
            assertEquals(asList(0, 0), list);

            ksession.insert("halt");

            System.out.println("--- Sleeping for 200ms");
            Thread.sleep(200);
            System.out.println("--- Slept for 200ms");
            ksession.delete(handle);
            System.out.println("--- Deleted trigger");
            System.out.println("--- List " + list + " size is " + list.size());
            assertEquals(2, list.size()); // halted, no more rule firing
            System.out.println("--- Assert1 List " + list + " size is " + list.size());

            System.out.println("--- Starting fireUntilHalt new thread");
            new Thread(ksession::fireUntilHalt).start();
            try {
                System.out.println("--- Sleeping for 200ms");
                Thread.sleep(200);
                System.out.println("--- Slept for 200ms");

                assertEquals(2, list.size());
                assertEquals(asList(0, 0), list);
            } finally {
                ksession.halt();
            }
        } finally {
            ksession.halt();
            ksession.dispose();
        }
    }

    @Test
    public void testExpiredPropagations() {
        // DROOLS-244
        final String drl = "package org.drools.test;\n" +
                "\n" +
                "import " + StockTick.class.getCanonicalName() + ";\n" +
                "global java.util.List list;\n" +
                "\n" +
                "declare StockTick\n" +
                "    @role( event )\n" +
                "    @timestamp( time )\n" +
                "end\n" +
                "\n" +
                "declare window ATicks\n" +
                " StockTick( company == \"AAA\" ) over window:time( 1s ) " +
                " from entry-point \"AAA\"\n" +
                "end\n" +
                "\n" +
                "declare window BTicks\n" +
                " StockTick( company == \"BBB\" ) over window:time( 1s ) " +
                " from entry-point \"BBB\"\n" +
                "end\n" +
                "\n" +
                "rule Ticks \n" +
                " when\n" +
                " String()\n" +
                " accumulate( $x : StockTick() from window ATicks, $a : count( $x ) )\n" +
                " accumulate( $y : StockTick() from window BTicks, $b : count( $y ) )\n" +
                " accumulate( $z : StockTick() over window:time( 1s ), $c : count( $z ) )\n" +
                " then\n" +
                " list.add( $a );\n" +
                " list.add( $b );\n" +
                " list.add( $c );\n" +
                "end";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final ArrayList list = new ArrayList();
            ksession.setGlobal("list", list);

            final SessionPseudoClock clock = ksession.getSessionClock();

            clock.advanceTime(1100, TimeUnit.MILLISECONDS);

            final StockTick tick = new StockTick(0, "AAA", 1.0, 0);
            final StockTick tock = new StockTick(1, "BBB", 1.0, 2500);
            final StockTick tack = new StockTick(1, "CCC", 1.0, 2700);

            final EntryPoint epa = ksession.getEntryPoint("AAA");
            final EntryPoint epb = ksession.getEntryPoint("BBB");

            epa.insert(tick);
            epb.insert(tock);
            ksession.insert(tack);

            FactHandle handle = ksession.insert("go1");
            ksession.fireAllRules();
            assertEquals(asList(0L, 1L, 1L), list);
            list.clear();
            ksession.delete(handle);

            clock.advanceTime(2550, TimeUnit.MILLISECONDS);

            handle = ksession.insert("go2");
            ksession.fireAllRules();
            assertEquals(asList(0L, 0L, 1L), list);
            list.clear();
            ksession.delete(handle);

            clock.advanceTime(500, TimeUnit.MILLISECONDS);

            handle = ksession.insert("go3");
            ksession.fireAllRules();
            assertEquals(asList(0L, 0L, 0L), list);
            list.clear();
            ksession.delete(handle);
        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testCronFire() {
        // BZ-1059372
        final String drl = "package test.drools\n" +
                "rule TestRule " +
                "  timer (cron:* * * * * ?) " +
                "when\n" +
                "    String() " +
                "    Integer() " +
                "then\n" +
                "end\n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            final int repetitions = 10000;
            for (int j = 0; j < repetitions; j++) {
                ksession.insert(j);
            }

            ksession.insert("go");
            ksession.fireAllRules();
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000)
    @Ignore("the listener callback holds some locks so blocking in it is not safe")
    public void testRaceConditionWithTimedRuleExectionOption() throws Exception {
        // BZ-1073880
        final String drl = "package org.simple \n" +
                "global java.util.List list \n" +
                "rule xxx @Propagation(EAGER)\n" +
                "  timer (int:30s 10s) "
                + "when \n" +
                "  $s: String()\n" +
                "then \n" +
                "  list.add($s); \n" +
                "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSessionConfiguration kieSessionConfiguration = KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration();
        kieSessionConfiguration.setOption(TimedRuleExecutionOption.YES);
        final KieSession ksession = kbase.newKieSession(kieSessionConfiguration, null);
        try {
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicBoolean aBool = new AtomicBoolean(true);
            final AgendaEventListener agendaEventListener = new DefaultAgendaEventListener() {
                public void afterMatchFired(final org.kie.api.event.rule.AfterMatchFiredEvent event) {
                    try {
                        if (aBool.get()) {
                            barrier.await();
                            aBool.set(false);
                        }
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            ksession.addEventListener(agendaEventListener);

            final List list = new ArrayList();
            ksession.setGlobal("list", list);

            // Using the Pseudo Clock.
            final SessionClock clock = ksession.getSessionClock();
            final SessionPseudoClock pseudoClock = (SessionPseudoClock) clock;

            // Insert the event.
            final String eventOne = "one";
            ksession.insert(eventOne);

            // Advance the time .... so the timer will fire.
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);

            // Rule doesn't fire in PHREAK. This is because you need to call 'fireAllRules' after you've inserted the fact, otherwise the timer
            // job is not created.

            ksession.fireAllRules();

            // Rule still doesn't fire, because the DefaultTimerJob is created now, and now we need to advance the timer again.

            pseudoClock.advanceTime(30000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            final String eventTwo = "two";
            ksession.insert(eventTwo);
            ksession.fireAllRules();

            // 60
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            // 70
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            //From here, the second rule should fire.
            //phaser.register();
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            // Now 2 rules have fired, and those will now fire every 10 seconds.
            pseudoClock.advanceTime(20000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();

            pseudoClock.advanceTime(20000, TimeUnit.MILLISECONDS);
            aBool.set(true);
            barrier.await();
            barrier.reset();

            pseudoClock.advanceTime(20000, TimeUnit.MILLISECONDS);
            aBool.set(true);
            barrier.await();
            barrier.reset();
        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testSharedTimers() {
        // DROOLS-451
        final String drl = "package org.simple \n" +
                "global java.util.List list \n" +
                "rule R1\n" +
                "  timer (int:30s 10s) " +
                "when \n" +
                "  $i: Integer()\n" +
                "then \n" +
                "  list.add(\"1\"); \n" +
                "end  \n" +
                "rule R2\n" +
                "  timer (int:30s 10s) " +
                "when \n" +
                "  $i: Integer()\n" +
                "then \n" +
                "  list.add(\"2\"); \n" +
                "end  \n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSessionConfiguration kieSessionConfiguration = KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration();
        kieSessionConfiguration.setOption(TimedRuleExecutionOption.YES);
        final KieSession ksession = kbase.newKieSession(kieSessionConfiguration, null);
        try {
            final List<String> list = new ArrayList<>();
            ksession.setGlobal("list", list);

            final SessionClock clock = ksession.getSessionClock();
            final SessionPseudoClock pseudoClock = (SessionPseudoClock) clock;

            ksession.insert(1);
            ksession.fireAllRules();
            pseudoClock.advanceTime(35, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());
            assertTrue(list.containsAll(asList("1", "2")));
        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testIntervalRuleInsertion() throws Exception {
        // DROOLS-620
        // Does not fail when using pseudo clock due to the subsequent call to fireAllRules
        final String drl =
                "package org.simple\n" +
                        "global java.util.List list\n" +
                        "import " + Alarm.class.getCanonicalName() + "\n" +
                        "rule \"Interval Alarm\"\n" +
                        "timer(int: 1s 1s)\n" +
                        "when " +
                        "    not Alarm()\n" +
                        "then\n" +
                        "    insert(new Alarm());\n" +
                        "    list.add(\"fired\"); \n" +
                        "end\n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSessionConfiguration kieSessionConfiguration = KieSessionTestConfiguration.STATEFUL_REALTIME.getKieSessionConfiguration();
        kieSessionConfiguration.setOption(TimedRuleExecutionOption.YES);
        final KieSession ksession = kbase.newKieSession(kieSessionConfiguration, null);
        try {
            final List list = new ArrayList();
            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertEquals(0, list.size());
            Thread.sleep(900);
            assertEquals(0, list.size());
            Thread.sleep(500);
            assertEquals(1, list.size());
        } finally {
            ksession.dispose();
        }
    }
}
