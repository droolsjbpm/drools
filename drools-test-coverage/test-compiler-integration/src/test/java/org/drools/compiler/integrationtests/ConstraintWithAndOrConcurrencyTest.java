/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.drools.compiler.integrationtests;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestParametersUtil;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.test.testcategory.TurtleTestCategory;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@Category(TurtleTestCategory.class)
public class ConstraintWithAndOrConcurrencyTest {

    private static int LOOP = 500;

    private static int THREADS = 32;
    private static int REQUESTS = 32;

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    public ConstraintWithAndOrConcurrencyTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
        this.kieBaseTestConfiguration = kieBaseTestConfiguration;
    }

    @Parameterized.Parameters(name = "KieBase type={0}")
    public static Collection<Object[]> getParameters() {
        return TestParametersUtil.getKieBaseCloudConfigurations(false);
    }

    @Test(timeout = 300000)
    public void testConstraintConcurrency() {
        final String drl =
                "package com.example.reproducer\n" +
                           "import " + Bus.class.getCanonicalName() + ";\n" +
                           "import static " + ConstraintWithAndOrConcurrencyTest.class.getCanonicalName() + ".TOSTRING;\n" +
                           "dialect \"mvel\"\n" +
                           "rule \"rule_mt_1a\"\n" +
                           "    when\n" +
                           "        $bus : Bus( $check: \"GAMMA RAY\",\n" +
                           "                    $title: \"POWER PLANT\",\n" +
                           "                    karaoke.dvd[$title] != null,\n" +
                           "                    TOSTRING(karaoke.dvd[$title].artist) == \"BBB\" || TOSTRING(karaoke.dvd[$title].artist) >= \"01\" && TOSTRING(karaoke.dvd[$title].artist) <= \"39\" )\n" +
                           "    then\n" +
                           "end";

        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < LOOP; i++) {
            final KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("accumulate-test", kieBaseTestConfiguration, drl);

            // 1st run
            KieSession kSession1 = kieBase.newKieSession();
            Bus bus1 = new Bus("red", 30);
            bus1.getKaraoke().getDvd().put("POWER PLANT", new Album("POWER PLANT", "BBB")); // match the 1st condition -> short circuit 
            bus1.getKaraoke().getDvd().put("Somewhere Out In Space", new Album("Somewhere Out In Space", "GAMMA RAY"));
            kSession1.insert(bus1);
            kSession1.fireAllRules();
            kSession1.dispose();

            // 2nd run
            KieSession kSession2 = kieBase.newKieSession();
            Bus bus2 = new Bus("red", 30);
            bus2.getKaraoke().getDvd().put("POWER PLANT", new Album("POWER PLANT", "00")); // not match the 1st condition, not match the 2nd condition -> short circuit 
            bus2.getKaraoke().getDvd().put("Somewhere Out In Space", new Album("Somewhere Out In Space", "GAMMA RAY"));
            kSession2.insert(bus2);
            kSession2.fireAllRules();
            kSession2.dispose();

            ExecutorService executor = Executors.newFixedThreadPool(THREADS);
            CountDownLatch latch = new CountDownLatch(THREADS);
            for (int j = 0; j < REQUESTS; j++) {
                executor.execute(new Runnable() {

                    public void run() {
                        KieSession kSession = kieBase.newKieSession();

                        Bus bus1 = new Bus("red", 30);
                        bus1.getKaraoke().getDvd().put("POWER PLANT", new Album("POWER PLANT", "10")); // not match the 1st condition, match the 2nd & 3rd condition
                        bus1.getKaraoke().getDvd().put("Somewhere Out In Space", new Album("Somewhere Out In Space", "GAMMA RAY"));
                        kSession.insert(bus1);

                        try {
                            latch.countDown();
                            latch.await();
                        } catch (InterruptedException e) {
                            // ignore
                        }

                        try {
                            kSession.fireAllRules();
                        } catch (Exception e) {
                            //e.printStackTrace();
                            exceptions.add(e);
                        } finally {
                            kSession.dispose();
                        }
                    }
                });
            }

            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        assertEquals(0, exceptions.size());
    }

    public static class Bus {

        private String name;
        private int capacity;
        private BigDecimal weight;
        private Karaoke karaoke = new Karaoke();

        public Bus(String name, int capacity) {
            this.name = name;
            this.capacity = capacity;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPerson() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }

        public Karaoke getKaraoke() {
            return karaoke;
        }
    }

    public static class Karaoke {

        private Map<String, Album> dvd = new HashMap<>();

        public Map<String, Album> getDvd() {
            return dvd;
        }

        public void fix() {
            dvd = Collections.unmodifiableMap(dvd);
        }
    }

    public static class Album {

        private String title;
        private String artist;

        public Album(String title, String artist) {
            this.title = title;
            this.artist = artist;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }
    }

    public static String TOSTRING(String s) {
        return s == null ? "null" : s;
    }

    public static String TOSTRING(Object o) {
        return o == null ? "null" : o.toString();
    }
}
