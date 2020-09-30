/*
 * Copyright 2020 JBoss Inc
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

package org.drools.modelcompiler;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.drools.core.addon.AlphaNodeOrderingStrategy;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.reteoo.AlphaNode;
import org.drools.core.rule.constraint.MvelConstraint;
import org.drools.core.spi.AlphaNodeFieldConstraint;
import org.drools.core.spi.ObjectType;
import org.drools.modelcompiler.domain.Address;
import org.drools.modelcompiler.domain.FactASuper;
import org.drools.modelcompiler.domain.FactBSub;
import org.drools.modelcompiler.domain.FactCSub;
import org.drools.modelcompiler.domain.Person;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.conf.AlphaNodeOrderingOption;
import org.kie.api.runtime.KieSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AlphaNodeOrderingTest extends BaseModelTest {

    public AlphaNodeOrderingTest(RUN_TYPE testRunType) {
        super(testRunType);
    }

    @Test
    public void testCountBasedAlphaNodeOrdering() {
        String str =
                "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1 when\n" +
                     "  $p : Person(age != 0, age != 1, age != 2, age != 3)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R2 when\n" +
                     "  $p : Person(age != 1, age != 2, age != 3)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R3 when\n" +
                     "  $p : Person(age != 2, age != 3)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R4 when\n" +
                     "  $p : Person(age != 3)\n" +
                     "then\n" +
                     "end\n";

        KieModuleModel model = KieServices.get().newKieModuleModel();
        model.newKieBaseModel("kb")
             .setDefault(true)
             .setAlphaNodeOrdering(AlphaNodeOrderingOption.COUNT)
             .newKieSessionModel("ks")
             .setDefault(true);

        KieSession ksession = getKieSession(model, str);

        List<AlphaNode> alphaNodes = ReteDumper.collectNodes(ksession)
                                               .stream()
                                               .filter(AlphaNode.class::isInstance)
                                               .map(node -> (AlphaNode) node)
                                               .collect(Collectors.toList());
        assertEquals(4, alphaNodes.size());

        if (testRunType == RUN_TYPE.STANDARD_FROM_DRL) {
            // Not found a good way to assert constraints in case of externalized LambdaConstraint
            // Anyway alphaNodes.size() == 4 means that alpha nodes are reordered and effectively shared
            Optional<AlphaNode> optAlphaNot3 = alphaNodes.stream()
                                                         .filter(node -> ((MvelConstraint) node.getConstraint()).getExpression().equals("age != 3"))
                                                         .findFirst();
            assertTrue(optAlphaNot3.isPresent());

            AlphaNode alphaNot3 = optAlphaNot3.get();

            AlphaNode alphaNot2 = getNextAlphaNode(alphaNot3);
            assertEquals("age != 2", ((MvelConstraint) alphaNot2.getConstraint()).getExpression());

            AlphaNode alphaNot1 = getNextAlphaNode(alphaNot2);
            assertEquals("age != 1", ((MvelConstraint) alphaNot1.getConstraint()).getExpression());

            AlphaNode alphaNot0 = getNextAlphaNode(alphaNot1);
            assertEquals("age != 0", ((MvelConstraint) alphaNot0.getConstraint()).getExpression());
        }

        ksession.insert(new Person("Mario", 1));
        assertEquals(2, ksession.fireAllRules());
    }

    private AlphaNode getNextAlphaNode(AlphaNode current) {
        Optional<AlphaNode> optNextAlpha = Arrays.stream(current.getSinks())
                                                 .filter(AlphaNode.class::isInstance)
                                                 .map(AlphaNode.class::cast)
                                                 .findFirst();
        assertTrue(optNextAlpha.isPresent());
        AlphaNode nextAlpha = optNextAlpha.get();
        return nextAlpha;
    }

    @Test
    public void testNoopAlphaNodeOrdering() {
        String str =
                "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1 when\n" +
                     "  $p : Person(age != 0, age != 1, age != 2, age != 3)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R2 when\n" +
                     "  $p : Person(age != 1, age != 2, age != 3)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R3 when\n" +
                     "  $p : Person(age != 2, age != 3)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R4 when\n" +
                     "  $p : Person(age != 3)\n" +
                     "then\n" +
                     "end\n";

        KieModuleModel model = KieServices.get().newKieModuleModel();
        model.newKieBaseModel("kb")
             .setDefault(true)
             .setAlphaNodeOrdering(AlphaNodeOrderingOption.NONE)
             .newKieSessionModel("ks")
             .setDefault(true);

        KieSession ksession = getKieSession(model, str);

        List<AlphaNode> alphaNodes = ReteDumper.collectNodes(ksession)
                                               .stream()
                                               .filter(AlphaNode.class::isInstance)
                                               .map(node -> (AlphaNode) node)
                                               .collect(Collectors.toList());
        assertEquals(10, alphaNodes.size());

        ksession.insert(new Person("Mario", 1));
        assertEquals(2, ksession.fireAllRules());
    }

    @Test
    public void testNullCheck() {

        // address.street == "ABC street" has larger usage count.
        // So reordering results in [address.street == "ABC street", address != null] for R1
        //   -> NullPointerException

        String str =
                "import " + Person.class.getCanonicalName() + "\n" +
                     "import " + Address.class.getCanonicalName() + "\n" +
                     "rule R1 when\n" +
                     "  $p : Person(address != null, address.street == \"ABC street\")\n" +
                     "then\n" +
                     "end\n" +
                     "rule R2 when\n" +
                     "  $a : Address(street != null)" +
                     "  $p : Person(address == $a, name != \"Mario\", address.street == \"ABC street\")\n" +
                     "then\n" +
                     "end\n";

        KieModuleModel model = KieServices.get().newKieModuleModel();
        model
             //.setConfigurationProperty("drools.externaliseCanonicalModelLambda", Boolean.FALSE.toString())
             .newKieBaseModel("kb")
             .setDefault(true)
             .setAlphaNodeOrdering(AlphaNodeOrderingOption.COUNT) // Fails with COUNT
             .newKieSessionModel("ks")
             .setDefault(true);

        KieSession ksession = getKieSession(model, str);

        List<AlphaNode> alphaNodes = ReteDumper.collectNodes(ksession)
                                               .stream()
                                               .filter(AlphaNode.class::isInstance)
                                               .map(node -> (AlphaNode) node)
                                               .collect(Collectors.toList());
        assertEquals(4, alphaNodes.size()); // 5 in case of AlphaNodeOrderingOption.NONE

        ksession.insert(new Person("Mario"));
        assertEquals(0, ksession.fireAllRules());
    }

    @Test
    public void testInstanceof() {

        // name != "Paul" has larger usage count.
        // So reordering results in [name != "Paul", this instanceof FactBSub] for R1
        //   -> FactASuper.getName() throws UnsupportedOperationException 

        String str =
                "import " + FactASuper.class.getCanonicalName() + "\n" +
                     "import " + FactBSub.class.getCanonicalName() + "\n" +
                     "import " + FactCSub.class.getCanonicalName() + "\n" +
                     "rule R1 when\n" +
                     "  $p : FactASuper((this instanceof FactBSub), name != \"Paul\")\n" +
                     "then\n" +
                     "end\n" +
                     "rule R2 when\n" +
                     "  $p : FactASuper((this instanceof FactCSub), name != \"Paul\")\n" +
                     "then\n" +
                     "end\n";

        KieModuleModel model = KieServices.get().newKieModuleModel();
        model
             //.setConfigurationProperty("drools.externaliseCanonicalModelLambda", Boolean.FALSE.toString())
             .newKieBaseModel("kb")
             .setDefault(true)
             .setAlphaNodeOrdering(AlphaNodeOrderingOption.COUNT) // Fails with COUNT
             .newKieSessionModel("ks")
             .setDefault(true);

        KieSession ksession = getKieSession(model, str);

        List<AlphaNode> alphaNodes = ReteDumper.collectNodes(ksession)
                                               .stream()
                                               .filter(AlphaNode.class::isInstance)
                                               .map(node -> (AlphaNode) node)
                                               .collect(Collectors.toList());
        assertEquals(3, alphaNodes.size()); // 4 in case of AlphaNodeOrderingOption.NONE

        ksession.insert(new FactASuper());
        assertEquals(0, ksession.fireAllRules());
    }

    @Test
    public void testAccumulate() {
        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  accumulate ( $p1: Person ( name == \"John\", age > 10);\n" +
                     "                $averageAge : average($p1.getAge())\n" +
                     "             )\n" +
                     "  accumulate ( $p2: Person ( age > 10, name == \"John\");\n" +
                     "                $averageMoney : average($p2.getMoney())\n" +
                     "             )\n" +
                     "then\n" +
                     "end\n" +
                     "rule R2\n" +
                     "when\n" +
                     "  Person ( name == \"John\");\n" +
                     "then\n" +
                     "end\n";

        KieModuleModel model = KieServices.get().newKieModuleModel();
        model.newKieBaseModel("kb")
             .setDefault(true)
             .setAlphaNodeOrdering(AlphaNodeOrderingOption.COUNT)
             .newKieSessionModel("ks")
             .setDefault(true);

        KieSession ksession = getKieSession(model, str);

        List<AlphaNode> alphaNodes = ReteDumper.collectNodes(ksession)
                                               .stream()
                                               .filter(AlphaNode.class::isInstance)
                                               .map(node -> (AlphaNode) node)
                                               .collect(Collectors.toList());
        assertEquals(2, alphaNodes.size()); // 4 in case of AlphaNodeOrderingOption.NONE

        Person p1 = new Person("John", 42);
        p1.setMoney(new BigDecimal("1000"));
        Person p2 = new Person("John", 40);
        p2.setMoney(new BigDecimal("1600"));
        Person p3 = new Person("John", 38);
        p3.setMoney(new BigDecimal("1000"));

        ksession.insert(p1);
        ksession.insert(p2);
        ksession.insert(p3);

        assertEquals(4, ksession.fireAllRules());
    }

    @Test
    public void testFromAccumulate() {
        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "import " + BigDecimal.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  $averageAge : Double() from accumulate ( $p1: Person ( name == \"John\", age > 10);\n" +
                     "                average($p1.getAge())\n" +
                     "             )\n" +
                     "  $averageMoney : BigDecimal() from accumulate ( $p2: Person ( age > 10, name == \"John\");\n" +
                     "                average($p2.getMoney())\n" +
                     "             )\n" +
                     "then\n" +
                     "end\n" +
                     "rule R2\n" +
                     "when\n" +
                     "  Person ( name == \"John\");\n" +
                     "then\n" +
                     "end\n";

        KieModuleModel model = KieServices.get().newKieModuleModel();
        model.newKieBaseModel("kb")
             .setDefault(true)
             .setAlphaNodeOrdering(AlphaNodeOrderingOption.COUNT)
             .newKieSessionModel("ks")
             .setDefault(true);

        KieSession ksession = getKieSession(model, str);

        List<AlphaNode> alphaNodes = ReteDumper.collectNodes(ksession)
                                               .stream()
                                               .filter(AlphaNode.class::isInstance)
                                               .map(node -> (AlphaNode) node)
                                               .collect(Collectors.toList());
        assertEquals(2, alphaNodes.size()); // 4 in case of AlphaNodeOrderingOption.NONE

        Person p1 = new Person("John", 42);
        p1.setMoney(new BigDecimal("1000"));
        Person p2 = new Person("John", 40);
        p2.setMoney(new BigDecimal("1600"));
        Person p3 = new Person("John", 38);
        p3.setMoney(new BigDecimal("1000"));

        ksession.insert(p1);
        ksession.insert(p2);
        ksession.insert(p3);

        assertEquals(4, ksession.fireAllRules());
    }

    @Test
    public void testNot() {
        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  not Person(name == \"John\", age > 10)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R2\n" +
                     "when\n" +
                     "  not Person(age > 10, employed == true, name == \"John\")\n" +
                     "then\n" +
                     "end\n" +
                     "rule R3\n" +
                     "when\n" +
                     "  Person(name == \"John\")\n" +
                     "then\n" +
                     "end\n";

        KieModuleModel model = KieServices.get().newKieModuleModel();
        model.newKieBaseModel("kb")
             .setDefault(true)
             .setAlphaNodeOrdering(AlphaNodeOrderingOption.COUNT)
             .newKieSessionModel("ks")
             .setDefault(true);

        KieSession ksession = getKieSession(model, str);

        List<AlphaNode> alphaNodes = ReteDumper.collectNodes(ksession)
                                               .stream()
                                               .filter(AlphaNode.class::isInstance)
                                               .map(node -> (AlphaNode) node)
                                               .collect(Collectors.toList());
        assertEquals(3, alphaNodes.size()); // 5 in case of AlphaNodeOrderingOption.NONE

        Person p1 = new Person("John", 42);
        p1.setEmployed(false);

        ksession.insert(p1);

        assertEquals(2, ksession.fireAllRules());
    }

    @Test
    public void testExists() {
        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  exists Person(name == \"John\", age > 10)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R2\n" +
                     "when\n" +
                     "  exists Person(age > 10, employed == true, name == \"John\")\n" +
                     "then\n" +
                     "end\n" +
                     "rule R3\n" +
                     "when\n" +
                     "  Person(name == \"John\")\n" +
                     "then\n" +
                     "end\n";

        KieModuleModel model = KieServices.get().newKieModuleModel();
        model.newKieBaseModel("kb")
             .setDefault(true)
             .setAlphaNodeOrdering(AlphaNodeOrderingOption.COUNT)
             .newKieSessionModel("ks")
             .setDefault(true);

        KieSession ksession = getKieSession(model, str);

        List<AlphaNode> alphaNodes = ReteDumper.collectNodes(ksession)
                                               .stream()
                                               .filter(AlphaNode.class::isInstance)
                                               .map(node -> (AlphaNode) node)
                                               .collect(Collectors.toList());
        assertEquals(3, alphaNodes.size()); // 5 in case of AlphaNodeOrderingOption.NONE

        Person p1 = new Person("John", 42);
        p1.setEmployed(false);

        ksession.insert(p1);

        assertEquals(2, ksession.fireAllRules());
    }

    @Test
    public void testCustomAlphaNodeOrdering() {

        System.setProperty(AlphaNodeOrderingOption.CUSTOM_CLASS_PROPERTY_NAME, "org.drools.modelcompiler.AlphaNodeOrderingTest$MyCustomStrategy");

        try {
            String str =
                    "import " + Person.class.getCanonicalName() + "\n" +
                         "rule R1 when\n" +
                         "  $p : Person(age != 0, age != 1, age != 2, age != 3)\n" +
                         "then\n" +
                         "end\n" +
                         "rule R2 when\n" +
                         "  $p : Person(age != 1, age != 2, age != 3)\n" +
                         "then\n" +
                         "end\n" +
                         "rule R3 when\n" +
                         "  $p : Person(age != 2, age != 3)\n" +
                         "then\n" +
                         "end\n" +
                         "rule R4 when\n" +
                         "  $p : Person(age != 3)\n" +
                         "then\n" +
                         "end\n";

            KieModuleModel model = KieServices.get().newKieModuleModel();
            model.newKieBaseModel("kb")
                 .setDefault(true)
                 .setAlphaNodeOrdering(AlphaNodeOrderingOption.CUSTOM)
                 .newKieSessionModel("ks")
                 .setDefault(true);

            KieSession ksession = getKieSession(model, str);

            List<AlphaNode> alphaNodes = ReteDumper.collectNodes(ksession)
                                                   .stream()
                                                   .filter(AlphaNode.class::isInstance)
                                                   .map(node -> (AlphaNode) node)
                                                   .collect(Collectors.toList());
            assertEquals(10, alphaNodes.size());

            ksession.insert(new Person("Mario", 1));
            assertEquals(2, ksession.fireAllRules());

            assertTrue(MyCustomStrategy.counter > 0);
        } finally {
            System.clearProperty(AlphaNodeOrderingOption.CUSTOM_CLASS_PROPERTY_NAME);
        }
    }

    public static class MyCustomStrategy implements AlphaNodeOrderingStrategy {

        public static int counter = 0;

        @Override
        public void analyzeAlphaConstraints(Map<String, InternalKnowledgePackage> pkgs, Collection<InternalKnowledgePackage> newPkgs) {
            counter++;
        }

        @Override
        public void reorderAlphaConstraints(List<AlphaNodeFieldConstraint> alphaConstraints, ObjectType objectType) {
            counter++;
        }

    }
}
