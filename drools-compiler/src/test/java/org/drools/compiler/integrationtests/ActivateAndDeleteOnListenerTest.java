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

package org.drools.compiler.integrationtests;

import org.drools.compiler.Alarm;
import org.drools.compiler.CommonTestMethodBase;
import org.drools.compiler.Sensor;
import org.drools.core.event.DefaultAgendaEventListener;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.definition.rule.Rule;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.runtime.conf.ForceEagerActivationFilter;
import org.kie.internal.runtime.conf.ForceEagerActivationOption;
import org.kie.internal.utils.KieHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * see JBPM-4764
 */
public class ActivateAndDeleteOnListenerTest extends CommonTestMethodBase {

    @Test
    public void testActivateOnMatchAndDelete() throws Exception {

        String str = "";
        str += "package org.drools.compiler.integrationtests \n";
        str += "import " + Alarm.class.getCanonicalName() + " \n";
        str += "import " + Sensor.class.getCanonicalName() + " \n";
        str += "rule StringRule  @Propagation(EAGER) ruleflow-group \"DROOLS_SYSTEM\"\n";
        str += " when \n";
        str += " $a : Alarm() \n";
        str += " $s : Sensor() \n";
        str += " then \n";
        str += "end \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ForceEagerActivationOption.YES );

        KieSession ksession = new KieHelper().addContent( str, ResourceType.DRL )
                                             .build()
                                             .newKieSession(conf, null);

        ksession.addEventListener(new DefaultAgendaEventListener(){
            @Override
            public void matchCreated(MatchCreatedEvent event) {
                Collection<? extends FactHandle> alarms = event.getKieRuntime().getFactHandles(new ClassObjectFilter(Alarm.class));
                for (FactHandle alarm : alarms) {
                    event.getKieRuntime().delete(alarm);
                }
            }
        });

        // go !
        Alarm alarm = new Alarm();
        alarm.setMessage("test");
        alarm.setNumber(123);

        ksession.insert(alarm);

        Sensor sensor = new Sensor();
        sensor.setPressure(1);
        sensor.setTemperature(25);

        ksession.insert(sensor);
        ksession.fireAllRules();
    }

    @Test
    public void testActivateOnMatchAndUpdate() throws Exception {

        String str = "";
        str += "package org.drools.compiler.integrationtests \n";
        str += "import " + Alarm.class.getCanonicalName() + " \n";
        str += "import " + Sensor.class.getCanonicalName() + " \n";
        str += "rule StringRule  @Propagation(EAGER) ruleflow-group \"DROOLS_SYSTEM\"\n";
        str += " when \n";
        str += " $a : Alarm() \n";
        str += " $s : Sensor() \n";
        str += " then \n";
        str += "end \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ForceEagerActivationOption.YES );

        KieSession ksession = new KieHelper().addContent( str, ResourceType.DRL )
                                             .build()
                                             .newKieSession( conf, null );

        ksession.addEventListener(new DefaultAgendaEventListener(){
            @Override
            public void matchCreated(MatchCreatedEvent event) {
                Collection<? extends FactHandle> alarms = event.getKieRuntime().getFactHandles(new ClassObjectFilter(Alarm.class));
                for (FactHandle alarm : alarms) {
                    event.getKieRuntime().update(alarm, new Alarm());
                }
            }
        });

        // go !
        Alarm alarm = new Alarm();
        alarm.setMessage("test");
        alarm.setNumber(123);

        ksession.insert(alarm);

        Sensor sensor = new Sensor();
        sensor.setPressure(1);
        sensor.setTemperature(25);

        ksession.insert(sensor);
    }

    @Test
    public void testEagerEvaluationWith2Paths() throws Exception {
        String str =
                "package org.simple \n" +
                "rule xxx \n" +
                "when \n" +
                "  $s : String()\n" +
                "  $i : Integer()\n" +
                "then \n" +
                "end  \n" +
                "rule yyy \n" +
                "when \n" +
                "  $s : String()\n" +
                "  $i : Integer()\n" +
                "then \n" +
                "end  \n";

        KieServices ks = KieServices.Factory.get();

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption(ForceEagerActivationOption.YES);

        KieSession ksession = new KieHelper()
                .addContent(str, ResourceType.DRL)
                .build()
                .newKieSession(conf, null);

        final List list = new ArrayList();

        AgendaEventListener agendaEventListener = new org.kie.api.event.rule.DefaultAgendaEventListener() {
            public void matchCreated(org.kie.api.event.rule.MatchCreatedEvent event) {
                list.add("activated");
            }
        };
        ksession.addEventListener(agendaEventListener);

        ksession.insert("test");
        assertEquals(0, list.size());

        ksession.insert(1);
        assertEquals(2, list.size());
    }

    @Test
    public void testEagerEvaluationWith2SubPaths() throws Exception {
        String str =
                "package org.simple \n" +
                "rule xxx \n" +
                "when \n" +
                "  $s : String()\n" +
                "  exists( Integer() or Long() )\n" +
                "then \n" +
                "end  \n" +
                "rule yyy \n" +
                "when \n" +
                "  $s : String()\n" +
                "  exists( Integer() or Long() )\n" +
                "then \n" +
                "end  \n";

        KieServices ks = KieServices.Factory.get();

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption(ForceEagerActivationOption.YES);

        KieSession ksession = new KieHelper()
                .addContent(str, ResourceType.DRL)
                .build()
                .newKieSession(conf, null);

        final List list = new ArrayList();

        AgendaEventListener agendaEventListener = new org.kie.api.event.rule.DefaultAgendaEventListener() {
            public void matchCreated(org.kie.api.event.rule.MatchCreatedEvent event) {
                list.add("activated");
            }
        };
        ksession.addEventListener(agendaEventListener);

        ksession.insert("test");
        assertEquals(0, list.size());

        ksession.insert(1);
        assertEquals(2, list.size());
    }

    @Test
    public void testOneLazyAndOneImmediateSubPath() throws Exception {
        String str =
                "package org.simple \n" +
                "rule xxx \n" +
                "when \n" +
                "  $s : String()\n" +
                "  exists( Integer() or Long() )\n" +
                "then \n" +
                "end  \n" +
                "rule yyy \n" +
                "when \n" +
                "  $s : String()\n" +
                "  exists( Integer() or Long() )\n" +
                "then \n" +
                "end  \n";

        KieServices ks = KieServices.Factory.get();

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( new ForceEagerActivationOption.FILTERED( new ForceEagerActivationFilter() {
            @Override
            public boolean accept(Rule rule ) {
                return rule.getName().equals("yyy");
            }
        }));

        KieSession ksession = new KieHelper()
                .addContent(str, ResourceType.DRL)
                .build()
                .newKieSession(conf, null);

        final List list = new ArrayList();

        AgendaEventListener agendaEventListener = new org.kie.api.event.rule.DefaultAgendaEventListener() {
            public void matchCreated(org.kie.api.event.rule.MatchCreatedEvent event) {
                list.add(event.getMatch().getRule().getName());
            }
        };
        ksession.addEventListener(agendaEventListener);

        ksession.insert("test");
        assertEquals(0, list.size());

        ksession.insert(1);
        assertEquals(1, list.size());
        assertEquals("yyy", list.get(0));
    }
}
