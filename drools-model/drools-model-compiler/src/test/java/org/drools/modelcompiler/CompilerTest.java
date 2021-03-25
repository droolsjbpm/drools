/*
 * Copyright 2005 JBoss Inc
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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.drools.modelcompiler.domain.Address;
import org.drools.modelcompiler.domain.Adult;
import org.drools.modelcompiler.domain.Child;
import org.drools.modelcompiler.domain.Man;
import org.drools.modelcompiler.domain.Overloaded;
import org.drools.modelcompiler.domain.Person;
import org.drools.modelcompiler.domain.Pet;
import org.drools.modelcompiler.domain.Result;
import org.drools.modelcompiler.domain.StockTick;
import org.drools.modelcompiler.domain.Toy;
import org.drools.modelcompiler.domain.Woman;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kie.api.definition.type.FactType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessContext;
import org.kie.api.runtime.rule.FactHandle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CompilerTest extends BaseModelTest {

    public CompilerTest( RUN_TYPE testRunType ) {
        super( testRunType );
    }

    @Test(timeout = 5000)
    public void testPropertyReactivity() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person(name == \"Mario\")\n" +
                "then\n" +
                "  modify($p) { setAge($p.getAge()+1) }\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person me = new Person( "Mario", 40 );
        ksession.insert( me );
        ksession.fireAllRules();

        assertEquals( 41, me.getAge() );
    }

    @Test
    public void testPropertyReactivityWithArguments() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R \n" +
                "when\n" +
                "    $p: Person()\n" +
                "then\n" +
                "    modify($p) { setLikes( String.valueOf(($p.getAddress().getStreet() + $p.getAddress().getCity()))) };\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person me = new Person( "Mario", 40 );
        me.setAddress(new Address("street1", 2, "city1"));
        ksession.insert( me );
        ksession.fireAllRules();

        assertEquals( "street1city1", me.getLikes() );
    }

    @Test
    public void testPropertyReactvityOnFinalField() throws Exception {
        String str =
                "rule R when\n" +
                "    $a : String( length > 3 )\n" +
                "then\n" +
                "  System.out.println($a);\n" +
                "end\n";

        KieSession ksession = getKieSession(str);

        ksession.insert( "abcd" );
        ksession.insert( "xy" );

        assertEquals(1, ksession.fireAllRules());
    }

    @Test
    public void testEqualityCheckOnNull() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "import " + Result.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  Person($name : name == \"Mario\")\n" +
                "then\n" +
                "  insert(new Result($name));\n" +
                "end";

        KieSession ksession = getKieSession( str );

        final Person mario = new Person("Mario", 40);
        final Person luca = new Person(null, 33);

        ksession.insert(mario);
        ksession.insert(luca);

        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "Mario", results.iterator().next().getValue() );
    }

    @Test
    public void testOrWithFixedLeftOperand() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "import " + Result.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person(name == \"Mario\" || == \"Luca\" || == \"Edoardo\")\n" +
                "then\n" +
                "  insert(new Result($p));\n" +
                "end";

        KieSession ksession = getKieSession( str );

        final Person mario = new Person("Mario", 40);
        final Person luca = new Person("Luca", 33);
        final Person edoardo = new Person("Edoardo", 31);
        final Person matteo = new Person("Matteo", 36);

        ksession.insert(mario);
        ksession.insert(luca);
        ksession.insert(edoardo);
        ksession.insert(matteo);

        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 3, results.size() );
        assertThat(results.stream().map(r -> r.getValue())).containsExactlyInAnyOrder(mario, luca, edoardo);
    }

    @Test
    public void testCapitalLetterProperty() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "import " + Result.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person( ParentP!.name == \"Luca\")\n" +
                "then\n" +
                "  insert(new Result($p));\n" +
                "end";

        KieSession ksession = getKieSession( str );

        final Person luca = new Person("Luca", 35);
        final Person leonardo = new Person("Leonardo", 2).setParentP(luca);
        final Person edoardo = new Person("Edoardo", 31);

        ksession.insert(leonardo);
        ksession.insert(luca);
        ksession.insert(edoardo);

        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertThat(results.stream().map(Result::getValue)).containsExactlyInAnyOrder(leonardo);
    }

    @Test
    public void testBeta() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $r : Result()\n" +
                "  $markV : Person(name == \"Mark\")\n" +
                "  $olderV : Person(name != \"Mark\", age > $markV.age)\n" +
                "then\n" +
                "  $r.setValue($olderV.getName() + \" is older than \" + $markV.getName());\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        Person mark = new Person("Mark", 37);
        Person edson = new Person("Edson", 35);
        Person mario = new Person("Mario", 40);

        FactHandle markFH = ksession.insert(mark);
        FactHandle edsonFH = ksession.insert(edson);
        FactHandle marioFH = ksession.insert(mario);

        ksession.fireAllRules();
        assertEquals("Mario is older than Mark", result.getValue());

        result.setValue( null );
        ksession.delete( marioFH );
        ksession.fireAllRules();
        assertNull(result.getValue());

        mark.setAge( 34 );
        ksession.update( markFH, mark, "age" );

        ksession.fireAllRules();
        assertEquals("Edson is older than Mark", result.getValue());
    }

    @Test
    public void testRuleExtends() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R1 when\n" +
                "  $r : Result()\n" +
                "then\n" +
                "end\n" +
                "rule R2 extends R1 when\n" +
                "  $p1 : Person(name == \"Mark\")\n" +
                "then\n" +
                "end\n" +
                "rule R3 extends R2 when\n" +
                "  $p2 : Person(name != \"Mark\", age > $p1.age)\n" +
                "then\n" +
                "  $r.setValue($p2.getName() + \" is older than \" + $p1.getName());\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );
        assertEquals( 1, ksession.fireAllRules() );

        ksession.insert(new Person("Edson", 35));
        ksession.insert(new Person("Mario", 40));
        assertEquals( 0, ksession.fireAllRules() );

        ksession.insert(new Person("Mark", 37));
        assertEquals( 2, ksession.fireAllRules() );

        assertEquals("Mario is older than Mark", result.getValue());
    }

    @Test
    public void testBetaWithDeclaration() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $r : Result()\n" +
                "  $p1 : Person(name == \"Mark\", $markAge : age)\n" +
                "  $p2 : Person(name != \"Mark\", age > $markAge)\n" +
                "then\n" +
                "  $r.setValue($p2.getName() + \" is older than \" + $p1.getName());\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        Person mark = new Person("Mark", 37);
        Person edson = new Person("Edson", 35);
        Person mario = new Person("Mario", 40);

        FactHandle markFH = ksession.insert(mark);
        FactHandle edsonFH = ksession.insert(edson);
        FactHandle marioFH = ksession.insert(mario);

        ksession.fireAllRules();
        assertEquals("Mario is older than Mark", result.getValue());

        result.setValue( null );
        ksession.delete( marioFH );
        ksession.fireAllRules();
        assertNull(result.getValue());

        mark.setAge( 34 );
        ksession.update( markFH, mark, "age" );

        ksession.fireAllRules();
        assertEquals("Edson is older than Mark", result.getValue());
    }

    @Test
    public void test3Patterns() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $mark : Person(name == \"Mark\")\n" +
                "  $p : Person(age > $mark.age)\n" +
                "  $s: String(this == $p.name)\n" +
                "then\n" +
                "  System.out.println(\"Found: \" + $s);\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( "Mario" );
        ksession.insert( new Person( "Mark", 37 ) );
        ksession.insert( new Person( "Edson", 35 ) );
        ksession.insert( new Person( "Mario", 40 ) );
        ksession.fireAllRules();
    }

    @Test
    public void testSimpleInsert() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person( name.length == 4 )\n" +
                "then\n" +
                "  Result r = new Result($p.getName());" +
                "  insert(r);\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( new Person( "Mark", 37 ) );
        ksession.insert( new Person( "Mario", 40 ) );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "Mark", results.iterator().next().getValue() );
    }

    @Test
    public void testSimpleInsertWithProperties() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person( address.city.startsWith(\"M\"))\n" +
                "then\n" +
                "  Result r = new Result($p.getName());" +
                "  insert(r);\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( new Person( "Mark", 37, new Address("London")) );
        ksession.insert( new Person( "Luca", 32 , new Address("Milan")) );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "Luca", results.iterator().next().getValue() );
    }

    @Test
    public void testSimpleDelete() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person( name.length == 4 )\n" +
                "then\n" +
                "  Result r = new Result($p.getName());" +
                "  insert(r);\n" +
                "  delete($p);\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( new Person( "Mark", 37 ) );
        ksession.insert( new Person( "Mario", 40 ) );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "Mark", results.iterator().next().getValue() );
        assertEquals( 1, getObjectsIntoList( ksession, Person.class ).size() );
    }

    @Test
    public void testSimpleInsertDeleteExplicitScope() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person( name.length == 4 )\n" +
                "then\n" +
                "  Result r = new Result($p.getName());" +
                "  drools.insert(r);\n" +
                "  drools.delete($p);\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( new Person( "Mark", 37 ) );
        ksession.insert( new Person( "Mario", 40 ) );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "Mark", results.iterator().next().getValue() );
        assertEquals( 1, getObjectsIntoList( ksession, Person.class ).size() );
    }

    @Test
    public void testSimpleUpdate() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person( name.length == 4 )\n" +
                "then\n" +
                "  $p.setAge($p.getAge()+1);" +
                "  update($p);\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person mark = new Person( "Mark", 37 );
        Person mario = new Person( "Mario", 40 );

        ksession.insert( mark );
        ksession.insert( mario );
        ksession.fireAllRules();

        assertEquals( 38, mark.getAge() );
        assertEquals( 40, mario.getAge() );
    }

    @Test
    public void testSimpleModify() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person( name.length == 4 )\n" +
                "then\n" +
                "  modify($p) { setAge($p.getAge()+1) }\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person mark = new Person( "Mark", 37 );
        Person mario = new Person( "Mario", 40 );

        ksession.insert( mark );
        ksession.insert( mario );
        ksession.fireAllRules();

        assertEquals( 38, mark.getAge() );
        assertEquals( 40, mario.getAge() );
    }

    @Test
    public void testEmptyPattern() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "import " + Result.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  Person() \n" +
                "then\n" +
                "  insert(new Result(\"ok\"));\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person mario = new Person( "Mario", 40 );

        ksession.insert( mario );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "ok", results.iterator().next().getValue() );
    }

    @Test
    public void testEmptyPatternWithBinding() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "import " + Result.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person() \n" +
                "then\n" +
                "  insert(new Result($p.getName()));\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person mario = new Person( "Mario", 40 );

        ksession.insert( mario );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "Mario", results.iterator().next().getValue() );
    }

    @Test
    public void testFrom() {
        String str =
                "import " + Result.class.getCanonicalName() + ";\n" +
                "import " + Adult.class.getCanonicalName() + ";\n" +
                "import " + Child.class.getCanonicalName() + ";\n" +
                "rule R when\n" +
                "  $r : Result()\n" +
                "  $a : Adult()\n" +
                "  $c : Child( age > 8 ) from $a.children\n" +
                "then\n" +
                "  $r.setValue($c.getName());\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        Adult dad = new Adult( "dad", 40 );
        dad.addChild( new Child( "Alan", 10 ) );
        dad.addChild( new Child( "Betty", 7 ) );
        ksession.insert( dad );
        ksession.fireAllRules();

        assertEquals("Alan", result.getValue());
    }

    @Test
    public void testConcatenatedFrom() {
        checkConcatenatedFrom(true);
    }

    @Test
    public void testConcatenatedFromWithCondition() {
        checkConcatenatedFrom(false);
    }

    private void checkConcatenatedFrom(boolean withCondition) {
        String str =
                "import " + Result.class.getCanonicalName() + ";\n" +
                "import " + Man.class.getCanonicalName() + ";\n" +
                "import " + Woman.class.getCanonicalName() + ";\n" +
                "import " + Child.class.getCanonicalName() + ";\n" +
                "import " + Toy.class.getCanonicalName() + ";\n" +
                "global java.util.List list;\n" +
                "rule R when\n" +
                "  $m : Man(" + (withCondition ? "age > 0" : "") + ")\n" +
                "  $w : Woman() from $m.wife\n" +
                "  $c : Child( age > 10 ) from $w.children\n" +
                "  $t : Toy() from $c.toys\n" +
                "then\n" +
                "  list.add($t.getName());\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        final List<String> list = new ArrayList<>();
        ksession.setGlobal( "list", list );

        final Woman alice = new Woman( "Alice", 38 );
        final Man bob = new Man( "Bob", 40 );
        bob.setWife( alice );

        final Child charlie = new Child( "Charles", 12 );
        final Child debbie = new Child( "Debbie", 10 );
        alice.addChild( charlie );
        alice.addChild( debbie );

        charlie.addToy( new Toy( "car" ) );
        charlie.addToy( new Toy( "ball" ) );
        debbie.addToy( new Toy( "doll" ) );

        ksession.insert( bob );
        ksession.fireAllRules();

        assertThat(list).containsExactlyInAnyOrder("car", "ball");
    }

    @Test
    public void testAgeWithSum() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person( personAge : age )\n" +
                "  $plusTwo : Person(age == personAge + 2 )\n" +
                "then\n" +
                "  insert(new Result($plusTwo.getName()));\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( new Person( "Mario", 40 ) );
        ksession.insert( new Person( "Mark", 37 ) );
        ksession.insert( new Person( "Edson", 35 ) );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "Mark", results.iterator().next().getValue() );
    }

    @Test
    public void testAgeWithSumUsing2DeclarationInBeta() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                     "import " + Person.class.getCanonicalName() + ";" +
                     "rule R when\n" +
                     "  $p : Person( personAge : age )\n" +
                     "  $plusTwo : Person(age == personAge + 2 + $p.age - $p.age )\n" +
                     "then\n" +
                     "  insert(new Result($plusTwo.getName()));\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        ksession.insert(new Person("Mario", 40));
        ksession.insert(new Person("Mark", 37));
        ksession.insert(new Person("Edson", 35));
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList(ksession, Result.class);
        System.out.println(results);
        assertEquals(1, results.size());
        assertEquals("Mark", results.iterator().next().getValue());
    }

    @Test
    public void testFunction3() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "function Boolean isFortyYearsOld(Person p, Boolean booleanParameter) {\n" +
                "    return p.getAge() == 40; \n"+
                "}" +
                "rule R when\n" +
                "  $p : Person(isFortyYearsOld(this, true))\n" +
                "then\n" +
                "  insert(new Result($p.getName()));\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( new Person( "Mario", 40 ) );
        ksession.insert( new Person( "Mark", 37 ) );
        ksession.insert( new Person( "Edson", 35 ) );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList( ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "Mario", results.iterator().next().getValue() );
    }

    @Test
    public void testInsertLogical() {
        String str = "rule R when\n" +
                     "  Integer()" +
                     "then\n" +
                     "  insertLogical(\"Hello World\");\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        FactHandle fh_47 = ksession.insert(47);
        ksession.fireAllRules();

        Collection<String> results = getObjectsIntoList(ksession, String.class);
        assertTrue(results.contains("Hello World"));

        ksession.delete(fh_47);
        ksession.fireAllRules();

        results = getObjectsIntoList(ksession, String.class);
        assertFalse(results.contains("Hello World"));
    }

    @Test
    public void testModifyRewriteAvoidTwiceThePreceeding() {
        String str = "import " + Person.class.getCanonicalName() + ";" +
                     "global java.util.List globalA \n" +
                     "global java.util.List globalB \n" +
                     "rule R \n" +
                     "when\n" +
                     "  $p : Person()" +
                     "then\n" +
                     "  globalA.add(\"A\");\n" +
                     "  modify( $p ) { setAge(47); }\n" +
                     "  globalB.add(\"B\");\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        List globalA = new ArrayList<>();
        List globalB = new ArrayList<>();
        ksession.setGlobal("globalA", globalA);
        ksession.setGlobal("globalB", globalB);

        ksession.insert(new Person("person1"));
        ksession.fireAllRules();

        assertEquals(1, globalA.size());
        assertEquals(1, globalB.size());
    }

    @Test
    public void testEmptyModifyRewrite() {
        String str = "rule R \n" +
                     "no-loop \n" +
                     "when\n" +
                     "  $s : String()" +
                     "then\n" +
                     "  System.out.println(\"intentional empty modify on $s\" + $s);" +
                     "  modify( $s ) { }\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        ksession.insert("Hello World");
        int fired = ksession.fireAllRules();

        assertEquals(1, fired);
    }

    @Test()
    public void testModifyRewriteWithComments() {
        String str = "import " + Person.class.getCanonicalName() + ";" +
                     "global java.util.List globalA \n" +
                     "global java.util.List globalB \n" +
                     "rule R \n" +
                     "when\n" +
                     "  $p : Person()" +
                     "then\n" +
                     "  globalA.add(\"A\");\n" +
                     "  modify( $p ) {\n" +
                     "    // modify ; something\n" +
                     "    /* modify ; something */\n" +
                     "    setAge(47)\n" +
                     "  }\n" +
                     "  globalB.add(\"B\");\n" +
                     "  // modify ; something\n" +
                     "  /* modify ; something */\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        List globalA = new ArrayList<>();
        List globalB = new ArrayList<>();
        ksession.setGlobal("globalA", globalA);
        ksession.setGlobal("globalB", globalB);

        Person person1 = new Person("person1");
        ksession.insert(person1);
        ksession.fireAllRules();

        assertEquals(1, globalA.size());
        assertEquals(1, globalB.size());
        assertEquals(47, person1.getAge());
    }

    @Test()
    @Ignore("fails for exec model, is not recognizing properly start/ends of modify block")
    public void testModifyRewriteWithCommentsAbsurd() {
        String str = "import " + Person.class.getCanonicalName() + ";" +
                     "global java.util.List globalA \n" +
                     "global java.util.List globalB \n" +
                     "rule R \n" +
                     "when\n" +
                     "  $p : Person()" +
                     "then\n" +
                     "  globalA.add(\"A\");\n" +
                     "  modify( $p ) {\n" +
                     "    // modify( $p ) { setAge(1) } \n" +
                     "    /* modify( $p ) { setAge(2) } */\n" +
                     "    setAge(47)\n" +
                     "  }\n" +
                     "  globalB.add(\"B\");\n" +
                     "  // modify( $p ) { setAge(1) }\n" +
                     "  /* modify( $p ) { setAge(2) } */\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        List globalA = new ArrayList<>();
        List globalB = new ArrayList<>();
        ksession.setGlobal("globalA", globalA);
        ksession.setGlobal("globalB", globalB);

        Person person1 = new Person("person1");
        ksession.insert(person1);
        ksession.fireAllRules();

        assertEquals(1, globalA.size());
        assertEquals(1, globalB.size());
        assertEquals(47, person1.getAge());
    }

    @Test
    public void testConstraintContainingAMethodCallWithParams() {
        String str = "import " + Overloaded.class.getCanonicalName() + ";" +
                     "rule OverloadedMethods\n" +
                     "when\n" +
                     "  o : Overloaded( method(5, 9, \"x\") == 15 )\n" +
                     "then\n" +
                     "  insert(\"matched\");\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        ksession.insert(new Overloaded());
        ksession.fireAllRules();

        Collection<String> results = getObjectsIntoList(ksession, String.class);
        assertEquals(1, results.size());
    }

    @Test
    public void testSimpleModifyUsingNameRefFollowedByMethodCall() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                     "rule R when\n" +
                     "  $p : Person( name.length() == 4 )\n" +
                     "then\n" +
                     "  modify($p) { setAge($p.getAge()+1) }\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        Person mark = new Person("Mark", 37);
        Person mario = new Person("Mario", 40);

        ksession.insert(mark);
        ksession.insert(mario);
        ksession.fireAllRules();

        assertEquals(38, mark.getAge());
        assertEquals(40, mario.getAge());
    }

    @Test
    public void testChainOfMethodCallInConstraint() {
        String str = "import " + Person.class.getCanonicalName() + ";" +
                     "rule R when\n" +
                     "  $p : Person( getAddress().getCity().length() == 5 )\n" +
                     "then\n" +
                     "  insert(\"matched\");\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        Person john = new Person("John", 47);
        Address a = new Address("Italy");
        john.setAddress(a);

        ksession.insert(john);
        ksession.fireAllRules();

        Collection<String> results = getObjectsIntoList(ksession, String.class);
        assertEquals(1, results.size());
    }

    @Test
    public void testChainOfMethodCallInConstraintSub() {
        String str = "import " + Person.class.getCanonicalName() + ";" +
                     "rule R when\n" +
                     "  $p : Person( address.(city.startsWith(\"I\") &&  city.length() == 5  ) )\n" + // DRL feature "Grouped accessors for nested objects" is addressed by the RuleDescr directly.
                     "then\n" +
                     "  insert(\"matched\");\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        Person john = new Person("John", 47);
        Address a = new Address("Italy");
        john.setAddress(a);

        ksession.insert(john);
        ksession.fireAllRules();

        Collection<String> results = getObjectsIntoList(ksession, String.class);
        assertEquals(1, results.size());
    }

    @Test
    public void testChainFieldAccessorsAndMethodCall() {
        String str = "import " + Person.class.getCanonicalName() + ";" +
                     "rule R when\n" +
                     "  $p : Person( address.getCity().length == 5 )\n" +
                     "then\n" +
                     "  insert(\"matched\");\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        Person john = new Person("John", 47);
        Address a = new Address("Italy");
        john.setAddress(a);

        ksession.insert(john);
        ksession.fireAllRules();

        Collection<String> results = getObjectsIntoList(ksession, String.class);
        assertEquals(1, results.size());
    }

    @Test
    public void testInnerBindingWithOr() {
        String str =
                "global java.util.List list\n" +
                "\n" +
                "rule R when\n" +
                " s: String( s.toString() == \"x\" || s.toString() == \"y\" )\n" +
                "then\n" +
                " list.add(s);\n" +
                "end";

        KieSession ksession = getKieSession(str);

        List<String> list = new ArrayList<String>();
        ksession.setGlobal("list", list);

        ksession.insert("y");
        ksession.fireAllRules();

        assertEquals( 1, list.size() );
        assertEquals( "y", list.get(0) );
    }

    @Test
    public void testRHS() {
        String str =
                "rule R when\n" +
                "        //conditions\n" +
                "    then\n" +
                "        drools.halt();\n" +
                "        drools.getWorkingMemory();\n" +
                "        drools.setFocus(\"agenda-group\");\n" +
                "        drools.getRule();\n" +
                "        drools.getTuple();\n" +
                "        System.out.println(kcontext);\n" +
                "        kcontext.getKnowledgeRuntime();\n" +
                "end";

        KieSession ksession = getKieSession(str);
    }

    @Test
    public void testBindWith2Arguments() {
        String str =
                "import " + Adult.class.getCanonicalName() + ";\n" +
                        "import " + Child.class.getCanonicalName() + ";\n" +
                        "import " + Result.class.getCanonicalName() + ";\n" +
                        "rule R when\n" +
                        "  $y : Adult( $sum : (name.length + age) )\n" +
                        "then\n" +
                        "  insert(new Result($sum));\n" +
                        "end";

        KieSession ksession = getKieSession( str );

        Adult a = new Adult( "Mario", 43 );
        ksession.insert( a );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList(ksession, Result.class);
        assertEquals(((Number)results.iterator().next().getValue()).intValue(), 48);
    }

    @Test
    public void testLockOnActiveWithModify() {
        String str =
                "package org.drools.test; \n" +
                "import " + Person.class.getCanonicalName() + ";\n" +
                "rule \"Rule1\" \n" +
                "@Propagation(EAGER) \n" +
                "salience 1 \n" +
                "lock-on-active true\n" +
                "when\n" +
                "  $p: Person()\n" +
                "then\n" +
                "  System.out.println( \"Rule1\" ); \n" +
                "  modify( $p ) { setAge( 44 ); }\n" +
                "end;\n" +
                "\n" +
                "rule \"Rule2\"\n" +
                "@Propagation(EAGER) \n" +
                "lock-on-active true\n" +
                "when\n" +
                "  $p: Person() \n" +
                "  String() from $p.getName() \n" +
                "then\n" +
                "  System.out.println( \"Rule2\" + $p ); " +
                "  modify ( $p ) { setName( \"john\" ); } \n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.fireAllRules();

        Person p = new Person( "mark", 76 );
        ksession.insert( p );
        ksession.fireAllRules();

        assertEquals( 44, p.getAge() );
        assertEquals( "john", p.getName() );
    }

    @Test
    public void testAlphaConstraintOn2Properties() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person( age > name.length )\n" +
                "then\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person me = new Person( "Mario", 40 );
        ksession.insert( me );
        ksession.fireAllRules();
    }

    @Test
    public void testAlphaNull() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                        "import " + Result.class.getCanonicalName() + ";" +
                        "rule R1 when\n" +
                        "  $p : Person( name == null)\n" +
                        "then\n" +
                        "  insert(new Result($p.getName()));\n" +

                        "end\n" +
                        "rule R2 when\n" +
                        "  $p : Person(  name == \"Luca\")\n" +
                        "then\n" +
                        "  insert(new Result($p.getName()));\n" +
                        "end\n" +
                        "rule R3 when\n" +
                        "  $p : Person(  name == \"Pippo\")\n" +
                        "then\n" +
                        "  insert(new Result($p.getName()));\n" +
                        "end";

        KieSession ksession = getKieSession(str);

        Person first = new Person(null, 40);
        Person second = new Person("Luca", 40);
        Person third = new Person("Mario", 40);
        ksession.insert(first);
        ksession.insert(second);
        ksession.insert(third);
        ksession.fireAllRules();

        List<Object> results = getObjectsIntoList(ksession, Result.class)
                .stream().map(Result::getValue).collect(Collectors.toList());
        assertEquals(2, results.size());

        assertThat(results).containsExactlyInAnyOrder("Luca", null);
    }

    @Test
    public void testAlphaNullBoolean() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                        "import " + Result.class.getCanonicalName() + ";" +
                        "rule R1 when\n" +
                        "  $p : Person( employed == true)\n" +
                        "then\n" +
                        "  insert(new Result($p.getName()));\n" +
                        "end\n";

        KieSession ksession = getKieSession(str);

        Person first = new Person("First", 40);
        first.setEmployed(null);
        Person second = new Person("Second", 40);
        second.setEmployed(true);
        ksession.insert(first);
        ksession.insert(second);
        ksession.fireAllRules();

        List<Object> results = getObjectsIntoList(ksession, Result.class)
                .stream().map(Result::getValue).collect(Collectors.toList());
        assertEquals(1, results.size());

        assertThat(results).containsExactlyInAnyOrder("Second");
    }

    @Test
    public void testStringValueOf() {
        String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "import " + Result.class.getCanonicalName() + ";\n" +
                "rule R when\n" +
                "  Integer( $i : intValue )\n" +
                "  Person( name == (String.valueOf($i)) )\n" +
                "then\n" +
                "  insert(new Result($i));\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( 44 );
        ksession.insert( new Person( "44", 44 ) );
        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList(ksession, Result.class);
        assertEquals(((Number)results.iterator().next().getValue()).intValue(), 44);
    }

    @Test
    public void testBigDecimalBigIntegerCoercion() {
        String str = "import " + Person.class.getCanonicalName() + ";\n" +
                "import " + BigInteger.class.getCanonicalName() + ";\n" +
                "rule \"rule1\"\n" +
                "when\n" +
                "    Person( money == new BigInteger( \"1\" ) )\n" +
                "then\n" +
                "end\n" +
                "rule \"rule2\"\n" +
                "when\n" +
                "    Person( money == new BigInteger( \"2\" ) )\n" +
                "then\n" +
                "end\n" +
                "rule \"rule3\"\n" +
                "when\n" +
                "    Person( money == new BigInteger( \"3\" ) )\n" +
                "then\n" +
                "end\n";


        KieSession ksession1 = getKieSession(str);

        Person p1 = new Person();
        p1.setMoney( new BigDecimal(1 ) );
        ksession1.insert( p1 );
        assertEquals( 1, ksession1.fireAllRules() );

    }

    @Test
    public void testSingleQuoteString() {
        String str =
                "rule R1 when\n" +
                "  String( this == 'x' )\n" +
                "then\n" +
                "end\n" +
                "rule R2 when\n" +
                "  String( this == 'xx' )\n" +
                "then\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( "x" );
        ksession.insert( "xx" );
        assertEquals(2, ksession.fireAllRules());
    }

    @Test
    public void testIntToLongComparison() {
        String str =
                "rule R when\n" +
                "    $i : Integer()\n" +
                "    $l : Long( this > $i )\n" +
                "then\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( 1 );
        ksession.insert( 2L );
        assertEquals(1, ksession.fireAllRules());
    }


    @Test
    public void testUseGlobalInLHS() {
        // DROOLS-1025
        final String drl1 =
                "import " + Result.class.getCanonicalName() + ";\n" +
                        "global java.util.concurrent.atomic.AtomicInteger globalInt\n" +
                        "rule R1 when\n" +
                        "	 exists Integer() from globalInt.get()\n" +
                        "then\n" +
                        "  insert(new Result(\"match\"));\n" +
                        "end\n";

        KieSession ksession = getKieSession( drl1 );

        ksession.setGlobal("globalInt", new AtomicInteger(0));

        ksession.fireAllRules();

        Collection<Result> results = getObjectsIntoList(ksession, Result.class);
        assertEquals(results.iterator().next().getValue().toString(), "match");
    }

    @Test
    public void testMapAccess() {
        final String drl1 =
                "import java.util.Map;\n" +
                "rule R1 when\n" +
                "	 Map(this['type'] == 'Goods' )\n" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Map<String, Object> map = new HashMap<>();
        map.put("type", "Goods");

        ksession.insert( map );
        assertEquals( 1, ksession.fireAllRules() );
    }

    @Test
    public void testMapAccessProperty() {
        final String drl1 =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "import java.util.Map;\n" +
                "rule R1 when\n" +
                "   Person( items[1] == 2000 )" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 2000);
        map.put(2, 2000);

        final Person luca = new Person("Luca");
        luca.setItems(map);
        ksession.insert(luca);

        final Person mario = new Person("Mario");
        ksession.insert(mario);

        assertEquals( 1, ksession.fireAllRules() );
    }

    @Test
    public void testMapInitialization() {
        final String drl1 =
                "import " + Person.class.getCanonicalName() + ";\n" +
                        "import java.util.Map;\n" +
                        "import static " + Person.class.getName() + ".countItems;\n" +
                        "rule R1 when\n" +
                        "   Person( numberOfItems == countItems([123 : 456, 789 : name]))" +
                        "then\n" +
                        "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Person luca = new Person("Luca");
        luca.setNumberOfItems(2);
        ksession.insert(luca);

        final Person mario = new Person("Mario");
        ksession.insert(mario);

        assertEquals( 1, ksession.fireAllRules() );
    }


    @Test
    public void testErrorTwoPatterns() {
        // DROOLS-3850
        final String drl1 =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "import java.util.Map;\n" +
                "import static " + Person.class.getName() + ".countItems;\n" +
                "import static " + Person.class.getName() + ".evaluate;\n" +
                "rule R1 when\n" +
                "   Person( evaluate([123 : 456, 789 : name]), numberOfItems == countItems([123 : 456, 789 : name]))" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Person luca = new Person("Luca");
        luca.setNumberOfItems(2);
        ksession.insert(luca);

        final Person mario = new Person("Mario");
        ksession.insert(mario);

        assertEquals( 1, ksession.fireAllRules() );
    }


    @Test
    public void testMapWithBinding() {
        // DROOLS-3558
        final String drl1 = "package org.drools.compiler\n" +
                "import " + Person.class.getCanonicalName() + ";\n" +
                "import " + Address.class.getCanonicalName() + ";\n" +
                "rule R1\n" +
                "    when\n" +
                "        $p : Person()\n" +
                "        $a : Address( number == $p.items[1] )\n" +
                "    then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Person john = new Person("John");
        HashMap<Integer, Integer> items = new HashMap<Integer, Integer>();
        items.put(1, 20);
        john.setItems(items);

        items.values().iterator().next();
        ksession.insert(john);

        final Address address = new Address("Tasman", 20, "Nelson");
        ksession.insert(address);

        assertEquals( 1, ksession.fireAllRules() );
    }

    @Test
    public void testMapAccessPropertyWithCast() {
        final String drl1 =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "import java.util.Map;\n" +
                "rule R1 when\n" +
                "   Person( items[(Integer) 1] == 2000 )" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 2000);
        map.put(2, 2000);

        final Person luca = new Person("Luca");
        luca.setItems(map);
        ksession.insert(luca);

        final Person mario = new Person("Mario");
        ksession.insert(mario);

        assertEquals( 1, ksession.fireAllRules() );
    }

    @Test
    public void testArrayAccess() {
        final String drl =
                "package org.drools.compiler.test\n" +
                        "import " + Person.class.getCanonicalName() + "\n" +
                        "global java.util.List list\n" +
                        "rule test1\n" +
                        "when\n" +
                        "   $p1  : Person($name : name )\n" +
                        "   Person(addresses[0].street == $p1.name)\n" +  // indexed
                        "then\n" +
                        "end\n";

        KieSession ksession = getKieSession(drl);

        assertEquals(0, ksession.fireAllRules());
    }

    @Test
    public void testInOperators() {
        final String drl1 = "package org.drools.compiler\n" +
                "import " + Person.class.getCanonicalName() + ";\n" +
                "rule \"eval rewrite with 'in'\"\n" +
                "    when\n" +
                "        $p : Person( age in ( 1, (1 + 1) ))\n" +
                "    then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Person luca = new Person("Luca");
        luca.setAge(2);
        ksession.insert(luca);

        final Person mario = new Person("Mario");
        mario.setAge(12);
        ksession.insert(mario);

        assertEquals( 1, ksession.fireAllRules() );
    }

    public static class TestFact {
        private String aBcde;

        public TestFact(String aBcde) {
            this.aBcde = aBcde;
        }

        public String getaBcde() {
            return aBcde;
        }

        public void setaBcde(String aBcde) {
            this.aBcde = aBcde;
        }
    }

    @Test
    public void testGetterSetterCase() {
        // DROOLS-2724
        final String drl =
                "import " + TestFact.class.getCanonicalName() + ";\n" +
                "import java.util.List;\n" +
                "rule R1\n" +
                "when \n" +
                "   TestFact(aBcde == \"test\")\n" +
                "then end";
        KieSession kieSession = getKieSession(drl);
        kieSession.insert(new TestFact("test"));
        assertEquals(1, kieSession.fireAllRules());
    }

    @Test
    public void testCommaInModify() {
        // DROOLS-3505
        final String drl =
                "import " + Person.class.getCanonicalName() + "\n" +
                "dialect \"java\"\n" +

                "rule R1 when\n" +
                "   $p : Person( name == \"John\" )\n" +
                "then\n" +
                "   modify($p) { setAge(1), setLikes(\"bread\"); }\n" +
                "end\n";
        KieSession kieSession = getKieSession(drl);
        Person john = new Person("John", 24);
        kieSession.insert(john);
        assertEquals(1, kieSession.fireAllRules());

        assertEquals(john.getAge(), 1);
        assertEquals(john.getLikes(), "bread");
    }

    public static class Message {
        public static final int HELLO = 0;
        public static final int GOODBYE = 1;

        private String message;

        private int status;

        public String getMessage() {
            return this.message;
        }

        public void setMessage( String message ) {
            this.message = message;
        }

        public int getStatus() {
            return this.status;
        }

        public void setStatus( int status ) {
            this.status = status;
        }
    }

    @Test
    public void testStaticFieldClashingWithClassName() {
        // DROOLS-3560
        final String drl =
                "import " + Message.class.getCanonicalName() + "\n" +
                "rule \"Hello World\"\n" +
                "    when\n" +
                "        m : Message( status == Message.HELLO, myMessage : message ) \n" +
                "    then\n" +
                "        System.out.println( myMessage );\n" +
                "        m.setMessage( \"Goodbye cruel world\" ); \n" +
                "        m.setStatus( Message.GOODBYE ); \n" +
                "        update( m );\n" +
                "end\n" +
                "\n" +
                "rule \"GoodBye\"\n" +
                "    when\n" +
                "        Message( status == Message.GOODBYE, myMessage : message ) \n" +
                "    then\n" +
                "        System.out.println( myMessage );\n" +
                "end\n";

        KieSession kieSession = getKieSession(drl);
        Message message = new Message();
        message.setMessage( "Hi" );
        message.setStatus( Message.HELLO );
        kieSession.insert(message);
        assertEquals(2, kieSession.fireAllRules());
    }

    @Test
    public void testDoubleModify() {
        // DROOLS-3560
        final String drl =
                "import " + Message.class.getCanonicalName() + "\n" +
                "rule \"Hello World\"\n" +
                "    when\n" +
                "        m : Message( status == Message.HELLO, myMessage : message ) \n" +
                "    then\n" +
                "        System.out.println( myMessage );\n" +
                "        m.setMessage( \"Goodbye cruel world\" );" +
                "        update(m); \n" +
                "        m.setStatus( Message.GOODBYE ); \n" +
                "        update( m );\n" +
                "end\n" +
                "\n" +
                "rule \"GoodBye\"\n" +
                "    when\n" +
                "        Message( status == Message.GOODBYE, myMessage : message ) \n" +
                "    then\n" +
                "        System.out.println( myMessage );\n" +
                "end\n";

        KieSession kieSession = getKieSession(drl);
        Message message = new Message();
        message.setMessage( "Hi" );
        message.setStatus( Message.HELLO );
        kieSession.insert(message);
        assertEquals(2, kieSession.fireAllRules());
    }

    @Test
    public void testPrettyPrinterCrashing() {
        final String drl = "" +
                "package org.drools.compiler.test  \n" +

                "import java.util.List\n" +
                "import java.util.ArrayList\n" +
                "import " + Person.class.getCanonicalName() + "\n" +

                "global List list\n" +

                "dialect \"mvel\"\n" +

                "declare Location\n" +
                "    thing : String \n" +
                "    location : String \n" +
                "end" +
                "\n" +
                "query isContainedIn( String x, String y ) \n" +
                "    Location(x, y;)\n" +
                "    or \n" +
                "    ( Location(z, y;) and isContainedIn(x, z;) )\n" +
                "end\n" +
                "\n" +
                "rule look when \n" +
                "    Person( $l : likes ) \n" +
                "    isContainedIn( $l, 'office'; )\n" +
                "then\n" +
                "   insertLogical( 'blah' );" +
                "end\n" +
                "\n" +
                "rule go1 when \n" +
                "    String( this == 'go1') \n" +
                "then\n" +
                "        list.add( drools.getRule().getName() ); \n" +
                "        insert( new Location('lamp', 'desk') );\n" +
                "end\n" +
                "\n";

        KieSession ksession = getKieSession(drl);
        try {
            final List<String> list = new ArrayList<>();
            ksession.setGlobal("list", list);

            final Person p = new Person();
            p.setLikes("lamp");
            ksession.insert(p);
            ksession.fireAllRules();

        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testBetaJoinBigInteger() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                        "import " + Person.class.getCanonicalName() + ";" +
                        "rule R when\n" +
                        "  $r : Result()\n" +
                        "  $markV : Person(name == \"Mark\", $markAgeInSeconds : ageInSeconds)\n" +
                        "  $olderV : Person(name != \"Mark\", ageInSeconds > $markAgeInSeconds)\n" +
                        "then\n" +
                        "  $r.setValue($olderV.getName() + \" is older than \" + $markV.getName());\n" +
                        "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        Person mark = new Person("Mark", 37).setAgeInSeconds(BigInteger.valueOf(12341234));
        Person edson = new Person("Edson", 35).setAgeInSeconds(BigInteger.valueOf(1234));
        Person mario = new Person("Mario", 40).setAgeInSeconds(BigInteger.valueOf(123412341234L));

        ksession.insert(mark);
        ksession.insert(edson);
        ksession.insert(mario);

        ksession.fireAllRules();
        assertEquals("Mario is older than Mark", result.getValue());
    }

    @Test
    public void testBetaJoinBigDecimal() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                        "import " + Person.class.getCanonicalName() + ";" +
                        "rule R when\n" +
                        "  $r : Result()\n" +
                        "  $markV : Person(name == \"Mark\", $markMoney : money)\n" +
                        "  $richerV : Person(name != \"Mark\", money > $markMoney)\n" +
                        "then\n" +
                        "  $r.setValue($richerV.getName() + \" is richer than \" + $markV.getName());\n" +
                        "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        Person mark = new Person("Mark", 37).setMoney(BigDecimal.valueOf(1_000_000));
        Person edson = new Person("Edson", 35).setMoney(BigDecimal.valueOf(1_000));
        Person mario = new Person("Mario", 40).setMoney(BigDecimal.valueOf(1_000_000_000));

        ksession.insert(mark);
        ksession.insert(edson);
        ksession.insert(mario);

        ksession.fireAllRules();
        assertEquals("Mario is richer than Mark", result.getValue());
    }

    @Test
    public void testBetaCast() {
        String str =
                    "import " + Result.class.getCanonicalName() + ";" +
                        "import " + Person.class.getCanonicalName() + ";" +
                        "import " + Address.class.getCanonicalName() + ";" +
                        "rule R when\n" +
                        "  $r : Result()\n" +
                        "  $p : Person($intField : age)\n" +
                        "  $a : Address(shortNumber == (short)$intField)\n" +
                        "then\n" +
                        "  $r.setValue($a.getCity() + \" number has the same value of \" + $p.getName() + \" age\");\n" +
                        "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        Person mark = new Person("Mark", 37);
        Person edson = new Person("Edson", 35);
        Person mario = new Person("Mario", 40);

        Address a = new Address("Milan");
        a.setShortNumber((short)37);

        Address b = new Address("Rome");
        b.setShortNumber((short)1);

        ksession.insert(mark);
        ksession.insert(edson);
        ksession.insert(mario);

        ksession.insert(a);
        ksession.insert(b);

        ksession.fireAllRules();
        assertEquals("Milan number has the same value of Mark age", result.getValue());
    }

    @Test
    public void testNumericLimitsLiteral() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                        "import " + Person.class.getCanonicalName() + ";" +
                        "rule R when\n" +
                        "  $r : Result()\n" +
                        "  $p : Person(ageLong > 9223372036854775806L)\n" + // MAX_LONG - 1
                        "then\n" +
                        "  $r.setValue($p.getName() + \" is very old\");\n" +
                        "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        Person mark = new Person("Mark").setAgeLong(37);
        Person edson = new Person("Edson").setAgeLong(35);
        Person mario = new Person("Mario").setAgeLong(Long.MAX_VALUE);

        ksession.insert(mark);
        ksession.insert(edson);
        ksession.insert(mario);

        ksession.fireAllRules();

        assertEquals("Mario is very old", result.getValue());

    }



    @Test
    public void testBetaCastGreaterThan() {
        String str =
                    "import " + Result.class.getCanonicalName() + ";" +
                        "import " + Person.class.getCanonicalName() + ";" +
                        "import " + Address.class.getCanonicalName() + ";" +
                        "rule R when\n" +
                        "  $r : Result()\n" +
                        "  $p : Person($intField : age)\n" +
                        "  $a : Address(shortNumber > (short)$intField)\n" +
                        "then\n" +
                            "  $r.setValue($a.getCity() + \" number is greater than \" + $p.getName() + \" age\");\n" +
                        "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        Person mark = new Person("Mark", 37);
        Person edson = new Person("Edson", 35);
        Person mario = new Person("Mario", 40);

        Address a = new Address("Milan");
        a.setShortNumber((short)37);

        Address b = new Address("Naples");
        b.setShortNumber((short)1);

        Address c = new Address("Rome");
        c.setShortNumber((short)38);

        ksession.insert(mark);
        ksession.insert(edson);
        ksession.insert(mario);

        ksession.insert(a);
        ksession.insert(b);
        ksession.insert(c);

        ksession.fireAllRules();

        assertEquals("Rome number is greater than Mark age", result.getValue());

    }

    @Test
    public void testNumericLimits() {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                        "import " + Person.class.getCanonicalName() + ";" +
                        "rule R when\n" +
                        "  $r : Result()\n" +
                        "  $p : Person(ageLong > 9223372036854775806)\n" + // MAX_LONG - 1
                        "then\n" +
                        "  $r.setValue($p.getName() + \" is very old\");\n" +
                        "end";

        KieSession ksession = getKieSession( str );

        Result result = new Result();
        ksession.insert( result );

        Person mark = new Person("Mark").setAgeLong(37);
        Person edson = new Person("Edson").setAgeLong(35);
        Person mario = new Person("Mario").setAgeLong(Long.MAX_VALUE);

        ksession.insert(mark);
        ksession.insert(edson);
        ksession.insert(mario);

        ksession.fireAllRules();

        assertEquals("Mario is very old", result.getValue());

    }

    @Test
    public void testMapAbbreviatedComparison() {
        final String drl1 =
                "import java.util.Map;\n" +
                "rule R1 when\n" +
                "    Map(this['money'] >= 65 && <= 75)\n" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Map<String, Object> map = new HashMap<>();
        map.put("money", new BigDecimal(70));

        ksession.insert( map );
        assertEquals( 1, ksession.fireAllRules() );
    }

    @Test
    public void testHalfBinary() {
        final String drl1 =
                "rule R1 when\n" +
                "    Integer(this > 2 && < 5)\n" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        ksession.insert( 3 );
        ksession.insert( 4 );
        ksession.insert( 6 );
        assertEquals( 2, ksession.fireAllRules() );
    }

    @Test
    public void testHalfBinaryWithParenthesis() {
        // DROOLS-6006
        final String drl1 =
                "rule R1 when\n" +
                "    Integer(intValue (> 2 && < 5))\n" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        ksession.insert( 3 );
        ksession.insert( 4 );
        ksession.insert( 6 );
        assertEquals( 2, ksession.fireAllRules() );
    }

    @Test
    public void testComplexHalfBinary() {
        // DROOLS-6006
        final String drl1 =
                "rule R1 when\n" +
                "    Integer(intValue ((> 2 && < 4) || (> 5 && < 7)) )\n" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        ksession.insert( 3 );
        ksession.insert( 4 );
        ksession.insert( 6 );
        assertEquals( 2, ksession.fireAllRules() );
    }

    @Test
    public void testMapPrimitiveComparison() {
        final String drl1 =
                "import java.util.Map;\n" +
                "import " + Person.class.getCanonicalName() + ";\n" +
                "rule R1 when\n" +
                "    $m : Map()\n" +
                "    Person(age == $m['age'] )\n" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Map<String, Object> map = new HashMap<>();
        map.put("age", 20);
        Person john = new Person("John", 20);

        ksession.insert( map );
        ksession.insert( john );
        assertEquals( 1, ksession.fireAllRules() );
    }

    public static final int CONSTANT = 1;

    @Test
    public void testMapCheckForExistence() {
        final String drl1 =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "import " + Result.class.getCanonicalName() + ";" +
                "rule R1 when\n" +
                "    $p : Person(getItems().get( org.drools.modelcompiler.CompilerTest.CONSTANT) == null )\n" +
                "then\n" +
                "  insert(new Result($p.getName()));\n" +
                "end\n";

        KieSession ksession = getKieSession( drl1 );

        final Map<Integer, Integer> items = new HashMap<>();
        items.put(CONSTANT, 2000);

        final Person luca = new Person("Luca");
        luca.setItems(items);

        final Person mario = new Person("Mario");

        ksession.insert( luca );
        ksession.insert( mario );
        assertEquals( 1, ksession.fireAllRules() );

        Collection<Result> results = getObjectsIntoList(ksession, Result.class );
        assertEquals( 1, results.size() );
        assertEquals( "Mario", results.iterator().next().getValue() );
    }

    @Test
    public void testBigDecimalIntCoercion() {
        String str = "import " + Result.class.getCanonicalName() + ";\n" +
                "rule \"rule1\"\n" +
                "when\n" +
                "    $r : Result( value <= 20 )\n" +
                "then\n" +
                "end\n";

        KieSession ksession1 = getKieSession(str);

        Result fact = new Result();
        fact.setValue( new BigDecimal(10) );
        ksession1.insert( fact );
        assertEquals( 1, ksession1.fireAllRules() );
    }

    @Test
    public void testBooleanCoercion() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                        "import " + Result.class.getCanonicalName() + ";" +
                        "rule R1 when\n" +
                        "  $p : Person(employed == \"true\")\n" +
                        "then\n" +
                        "end\n";

        KieSession ksession = getKieSession(str);

        Person first = new Person("First", 40);
        first.setEmployed(true);
        ksession.insert(first);
        assertThat(ksession.fireAllRules()).isEqualTo(1);;
    }

    @Test
    public void testUseMatch() {
        // DROOLS-4579
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "    $p: Person()\n" +
                "then\n" +
                "    if ($p != drools.getMatch().getObjects().get(0)) throw new RuntimeException();\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person me = new Person( "Mario", 40 );
        ksession.insert( me );
        assertEquals(1, ksession.fireAllRules());
    }

    @Test
    public void testMultilinePattern() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                        "rule R1 when\n" +
                        "  $p : Person(age == 30\n" +
                        "    || employed == true)\n" +
                        "then\n" +
                        "end\n";

        KieSession ksession = getKieSession(str);

        Person first = new Person("John", 40);
        first.setEmployed(true);
        ksession.insert(first);
        assertThat(ksession.fireAllRules()).isEqualTo(1);;
    }

    @Test
    public void testAccumulateWithMax() {
        String str =
                "import " + StockTick.class.getCanonicalName() + ";" +
                        "import " + StockTick.class.getCanonicalName() + ";" +
                        "rule AccumulateMaxDate when\n" +
                        "  $max1 : Number() from accumulate(\n" +
                        "    StockTick($time : getTimeFieldAsDate());\n" +
                        "    max($time.getTime()))\n" +
                        "then\n" +
                        "end\n";

        KieSession ksession = getKieSession(str);

        StockTick st = new StockTick("RHT");
        st.setTimeField(new Date().getTime());
        ksession.insert(st);
        assertThat(ksession.fireAllRules()).isEqualTo(1);;
    }

    @Test()
    public void testMultipleModify() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p1 : Person(name == \"John\")\n" +
                "  $p2 : Person(name == \"Paul\")\n" +
                "then\n" +
                "  modify($p1) { setAge($p1.getAge()+1) }\n" +
                "  modify($p2) { setAge($p2.getAge()+5) }\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person p1 = new Person( "John", 40 );
        Person p2 = new Person( "Paul", 38 );

        ksession.insert( p1 );
        ksession.insert( p2 );
        ksession.fireAllRules();

        assertEquals( 41, p1.getAge() );
        assertEquals( 43, p2.getAge() );
    }

    @Test()
    public void testMultipleModifyWithDifferentFacts() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "import " + Pet.class.getCanonicalName() + ";" +
                "rule R1 when\n" +
                "  $person : Person(name == \"John\")\n" +
                "  $pet : Pet(owner == $person)\n" +
                "then\n" +
                "  modify($person) { setName(\"George\") }\n" +
                "  modify($pet) { setAge($pet.getAge()+1) }\n" +
                "end\n" +
                "rule R2 when\n" +
                "  $person : Person(name == \"George\")\n" +
                "  $pet : Pet(owner == $person)\n" +
                "then\n" +
                "  modify($pet) { setAge($pet.getAge()+1) }\n" +
                "end";

        KieSession ksession = getKieSession( str );

        Person person = new Person( "John", 40 );
        Pet pet = new Pet( Pet.PetType.dog, 3 );
        pet.setOwner(person);

        ksession.insert( person );
        ksession.insert( pet );
        ksession.fireAllRules();

        assertEquals( "George", person.getName() );
        assertEquals( 5, pet.getAge() );
    }

    public static class IntegerToShort {

        private Boolean testBoolean;
        private int testInt;
        private Short testShort;
        private double testDouble;

        public IntegerToShort(Boolean testBoolean, int testInt, Short testShort, Double testDouble) {
            this.testBoolean = testBoolean;
            this.testInt = testInt;
            this.testShort = testShort;
            this.testDouble = testDouble;
        }

        public void setTestBoolean(Boolean testBoolean) {
            this.testBoolean = testBoolean;
        }

        public void setTestInt(int testInt) {
            this.testInt = testInt;
        }

        public void setTestShort(Short testShort) {
            this.testShort = testShort;
        }

        public Boolean getTestBoolean() {
            return testBoolean;
        }

        public int getTestInt() {
            return testInt;
        }

        public Short getTestShort() {
            return testShort;
        }

        public Double getTestDouble() {
            return testDouble;
        }

        public void setTestDouble(Double testShort) {
            this.testDouble = testDouble;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IntegerToShort that = (IntegerToShort) o;
            return testInt == that.testInt && Objects.equals(testBoolean, that.testBoolean) && Objects.equals(testShort, that.testShort) && Objects.equals(testDouble, that.testDouble);
        }

        @Override
        public int hashCode() {
            return Objects.hash(testBoolean, testInt, testShort, testDouble);
        }

        @Override
        public String toString() {
            return "IntegerToShort{" +
                    "testBoolean=" + testBoolean +
                    ", testInt=" + testInt +
                    ", testShort=" + testShort +
                    ", testDouble=" + testDouble +
                    '}';
        }
    }

    @Test // DROOLS-5007
    public void testIntToShortCast() {
        String str = "import " + Address.class.getCanonicalName() + ";\n" +
                "rule \"rule1\"\n" +
                "when\n" +
                "    $address : Address( shortNumber == null ||  shortNumber == 0, \n" +
                "                           $interimVar : number)\n" +
                "then\n" +
                "    $address.setShortNumber((short)$interimVar);\n" +
                "    update($address);\n" +
                "end\n";

        KieSession ksession = getKieSession(str);

        Address address = new Address();
        address.setNumber(1);
        ksession.insert( address );
        assertEquals( 1, ksession.fireAllRules() );
    }


    @Test // DROOLS-5709 // DROOLS-5768
    public void testCastingIntegerToShort() {
        String str =
                "import " + IntegerToShort.class.getCanonicalName() + ";\n " +
                        "rule \"test_rule\"\n" +
                        "dialect \"java\"\n" +
                        "when\n" +
                        "   $integerToShort : IntegerToShort( " +
                        "           $testInt : testInt, " +
                        "           testBoolean != null, " +
                        "           testBoolean == false" +
                        ") \n" +
                        "then\n" +
                        "   $integerToShort.setTestShort((short)(12)); \n" +
                        "   $integerToShort.setTestShort((short)($testInt)); \n" +
                        "   $integerToShort.setTestBoolean(true);\n" +
                        "   update($integerToShort);\n" +
                        "end";

        KieSession ksession = getKieSession(str);
        IntegerToShort integerToShort = new IntegerToShort(false, Short.MAX_VALUE, (short)0, (double)0);

        ksession.insert(integerToShort);
        int rulesFired = ksession.fireAllRules();

        Assert.assertEquals(1, rulesFired);
        assertThat(integerToShort).isEqualTo(new IntegerToShort(true, Short.MAX_VALUE, Short.MAX_VALUE, (double)0));
    }

    @Test // DROOLS-5998
    public void testCastingIntegerToShortWithNegativeNumbers() {
        String str =
                "import " + IntegerToShort.class.getCanonicalName() + ";\n " +
                        "rule \"test_rule\"\n" +
                        "dialect \"java\"\n" +
                        "when\n" +
                        "   $integerToShort : IntegerToShort( " +
                        "           $testInt : testInt, " +
                        "           testBoolean != null, " +
                        "           testBoolean == false" +
                        ") \n" +
                        "then\n" +
                        "   $integerToShort.setTestShort((short)(-12)); \n" +
                        "   $integerToShort.setTestBoolean(true);\n" +
                        "   update($integerToShort);\n" +
                        "end";

        KieSession ksession = getKieSession(str);
        IntegerToShort integerToShort = new IntegerToShort(false, Short.MAX_VALUE, (short)0, (double)0);

        ksession.insert(integerToShort);
        int rulesFired = ksession.fireAllRules();

        Assert.assertEquals(1, rulesFired);
        assertThat(integerToShort).isEqualTo(new IntegerToShort(true, Short.MAX_VALUE, (short)-12, (double)0));
    }

    @Test // RHDM-1644
    public void testCastingIntegerToShortWithDoubleVar() {
        String str =
                "import " + IntegerToShort.class.getCanonicalName() + ";\n " +
                        "rule \"test_rule\"\n" +
                        "dialect \"java\"\n" +
                        "when\n" +
                        "   $integerToShort : IntegerToShort( " +
                        "           $testDouble : testDouble, " +
                        "           $testInt : testInt, " +
                        "           testBoolean != null, " +
                        "           testBoolean == false" +
                        ") \n" +
                        "then\n" +
                        "   $integerToShort.setTestShort((short)((16 + $testDouble))); \n" +
                        "   $integerToShort.setTestBoolean(true);\n" +
                        "   update($integerToShort);\n" +
                        "end";

        KieSession ksession = getKieSession(str);
        IntegerToShort integerToShort = new IntegerToShort(false, Short.MAX_VALUE, (short)0, (double)1);

        ksession.insert(integerToShort);
        int rulesFired = ksession.fireAllRules();

        Assert.assertEquals(1, rulesFired);
        assertThat(integerToShort).isEqualTo(new IntegerToShort(true, Short.MAX_VALUE, (short)17, (double)1));
    }

    @Test
    public void testConsequenceGetContext() throws Exception {
        String str =
                "import " + ProcessContext.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Object()\n" +
                "then\n" +
                "  ProcessContext clazz = drools.getContext(ProcessContext.class);\n" +
                "end";

        KieSession ksession = getKieSession( str );
        assertNotNull( ksession);
    }

    @Test // DROOLS-6034
    public void testConsequenceInsertThenUpdate() throws Exception {
        String str =
                "global java.util.List children;\n" +
                        "import " + Person.class.getCanonicalName() + ";" +
                        "rule \"a new baby is born\" when\n" +
                        "  " + // No pattern
                        "then\n" +
                        "  Person andrea = new Person();\n" +
                        "  insert(andrea);\n" +
                        "  andrea.setName(\"Andrea\");\n" +
                        "  update(andrea);\n" +
                        "  children.add(andrea.getName());\n" +
                        "end";

        KieSession kSession = getKieSession( str );

        ArrayList<String> children = new ArrayList<>();
        kSession.setGlobal("children", children);

        Person luca = new Person( "Luca", 36 );

        kSession.insert( luca );
        assertEquals(1, kSession.fireAllRules());

        Assertions.assertThat(children).containsOnly("Andrea");
    }

    @Test // DROOLS-6034
    public void testConsequenceInsertThenModify() throws Exception {
        String str =
                "global java.util.List children;\n" +
                        "import " + Person.class.getCanonicalName() + ";" +
                        "dialect \"mvel\"" +
                        "rule \"a new baby is born\" when\n" +
                        "  " + // No pattern
                        "then\n" +
                        "  Person andrea = new Person();\n" +
                        "  insert(andrea);\n" +
                        "  modify(andrea) {" +
                        "       name = \"Andrea\" " +
                        "  }" +
                        "  children.add(andrea.getName());\n" +
                        "end";

        KieSession kSession = getKieSession( str );

        ArrayList<String> children = new ArrayList<>();
        kSession.setGlobal("children", children);

        Person luca = new Person( "Luca", 36 );

        kSession.insert( luca );
        assertEquals(1, kSession.fireAllRules());

        Assertions.assertThat(children).containsOnly("Andrea");
    }

    @Test // DROOLS-6034
    public void testConsequenceInsertThenUpdateWithPatternInitializer() throws Exception {
        String str =
                "global java.util.List result;\n" +
                "import " + Person.class.getCanonicalName() + ";" +
                "import static " + Person.class.getName() + ".identityFunction;\n" +
                "no-loop true\n" +
                "rule R1 when\n" +
                "  $p : Person($name : name)\n" +
                "then\n" +
                "  Person fromMethodCall = identityFunction($p);" +
                "  Person fromNameExpr = fromMethodCall;" +
                "  Person fromNameExprTwice = fromNameExpr;\n" +
                "  insert(fromNameExprTwice);\n" +
                "  update(fromNameExprTwice);\n" +
                "  result.add(fromNameExprTwice.getName());\n" +
                "end";

        KieSession kSession = getKieSession( str );

        ArrayList<String> children = new ArrayList<>();
        kSession.setGlobal("result", children);

        Person luca = new Person( "Luca", 36 );

        kSession.insert( luca );
        assertEquals(1, kSession.fireAllRules());

        Assertions.assertThat(children).containsOnly("Luca");
    }

    @Test
    public void testExtraParenthes() throws Exception {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $p : Person((age > 30))\n" +
                "then\n" +
                "end";

        KieSession ksession = getKieSession(str);

        Person person = new Person( "John", 20 );

        ksession.insert( person );
        assertEquals(0, ksession.fireAllRules());
    }

    @Test
    public void testNegateBigDecimal() throws Exception {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                     "global java.util.List list;\n" +
                     "rule R when\n" +
                     "  $p : Person(!(money > 20))\n" +
                     "then\n" +
                     "  list.add($p.getName());" +
                     "end";

        KieSession ksession = getKieSession(str);
        final List<String> list = new ArrayList<>();
        ksession.setGlobal("list", list);

        Person p1 = new Person("John");
        p1.setMoney(new BigDecimal("10.0"));
        Person p2 = new Person("Paul");
        p2.setMoney(new BigDecimal("30.0"));

        ksession.insert(p1);
        ksession.insert(p2);

        assertEquals(1, ksession.fireAllRules());
        assertThat(list).containsExactly("John");
    }

    @Test
    public void testNegateJoin() throws Exception {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                     "import " + Address.class.getCanonicalName() + ";" +
                     "rule R when\n" +
                     "  $a : Address()\n" +
                     "  $p : Person(!(address == $a))\n" +
                     "then\n" +
                     "end";

        KieSession ksession = getKieSession(str);

        Address a = new Address("Milan");
        Person p = new Person("Toshiya");
        p.setAddress(new Address("Tokyo"));

        ksession.insert(a);
        ksession.insert(p);

        assertEquals(1, ksession.fireAllRules());
    }

    @Test
    public void testNegateComplex() throws Exception {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                     "global java.util.List list;\n" +
                     "rule R when\n" +
                     "  $p : Person(!(money > 20 && money < 40))\n" +
                     "then\n" +
                     "  list.add($p.getName());" +
                     "end";

        KieSession ksession = getKieSession(str);
        final List<String> list = new ArrayList<>();
        ksession.setGlobal("list", list);

        Person p1 = new Person("John");
        p1.setMoney(new BigDecimal("10.0"));
        Person p2 = new Person("Paul");
        p2.setMoney(new BigDecimal("30.0"));
        Person p3 = new Person("George");
        p3.setMoney(new BigDecimal("50.0"));

        ksession.insert(p1);
        ksession.insert(p2);
        ksession.insert(p3);

        assertEquals(2, ksession.fireAllRules());
        assertThat(list).containsExactlyInAnyOrder("John", "George");
    }

    @Test
    public void testMapStringProp() throws Exception {
        final String str =
                "package org.drools.test;\n" +
                           "import " + Person.class.getCanonicalName() + ";\n" +
                           "rule R1 when \n" +
                           "  Person(\"XXX\" == itemsString[\"AAA\"])\n" +
                           "then\n" +
                           "end";

        final KieSession ksession = getKieSession(str);

        final Person p = new Person("Toshiya");
        p.getItemsString().put("AAA", "XXX");

        ksession.insert(p);
        assertEquals(1, ksession.fireAllRules());
    }

    @Test
    public void testMapString() throws Exception {
        final String str =
                "package org.drools.test;\n" +
                           "import " + Map.class.getCanonicalName() + ";\n" +
                           "rule R1 when \n" +
                           "  Map(\"XXX\" == this[\"AAA\"])\n" +
                           "then\n" +
                           "end";

        final KieSession ksession = getKieSession(str);

        Map<String, String> map = new HashMap<>();
        map.put("AAA", "XXX");

        ksession.insert(map);
        assertEquals(1, ksession.fireAllRules());
    }

    @Test
    public void testHashSet() throws Exception {
        final String str =
                "package org.drools.test;\n" +
                           "import " + HashSet.class.getCanonicalName() + ";\n" +
                           "import " + Set.class.getCanonicalName() + ";\n" +
                           "declare Application\n" +
                           "    categories : Set = new HashSet()" +
                           "end\n" +
                           "rule R1\n" +
                           "no-loop true\n" +
                           "when \n" +
                           "  $a : Application()\n" +
                           "then\n" +
                           "  modify ($a) { getCategories().add(\"hello\") };\n" +
                           "end\n" +
                           "rule R2\n" +
                           "when \n" +
                           "  $a : Application(categories contains \"hello\")\n" +
                           "then\n" +
                           "end";

        final KieSession ksession = getKieSession(str);

        FactType appType = ksession.getKieBase().getFactType("org.drools.test", "Application");
        Object appObj = appType.newInstance();
        Set<String> categories = new HashSet<>();
        appType.set(appObj, "categories", categories);

        ksession.insert(appObj);
        assertEquals(2, ksession.fireAllRules());
    }

    @Test()
    public void testRhsOrderWithModify() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "global java.util.List list;\n" +
                "rule R when\n" +
                "  $p1 : Person(name == \"John\")\n" +
                "  $p2 : Person(name == \"Paul\")\n" +
                "then\n" +
                "  list.add($p1.getAge());\n" +
                "  list.add($p2.getAge());\n" +
                "  modify($p1) { setAge($p1.getAge()+1) }\n" +
                "  list.add($p1.getAge());\n" +
                "  list.add($p2.getAge());\n" +
                "  modify($p2) { setAge($p2.getAge()+5) }\n" +
                "  list.add($p1.getAge());\n" +
                "  list.add($p2.getAge());\n" +
                "end";

        KieSession ksession = getKieSession( str );
        final List<Integer> list = new ArrayList<>();
        ksession.setGlobal("list", list);

        Person p1 = new Person( "John", 40 );
        Person p2 = new Person( "Paul", 38 );

        ksession.insert( p1 );
        ksession.insert( p2 );
        ksession.fireAllRules();

        assertThat(list).containsExactlyInAnyOrder(40, 38, 41, 38, 41, 43);
    }

    @Test()
    public void testStringRelationalComparison() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "global java.util.List list;\n" +
                "rule R when\n" +
                "  $p : Person(name > \"Bob\" && name < \"Ken\")\n" +
                "then\n" +
                "  list.add($p.getName());" +
                "end";

        KieSession ksession = getKieSession(str);
        final List<String> list = new ArrayList<>();
        ksession.setGlobal("list", list);

        ksession.insert(new Person("John"));
        ksession.insert(new Person("Ann"));
        ksession.fireAllRules();

        assertThat(list).containsExactly("John");
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testNPEOnConstraint() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage(equalTo("Error evaluating constraint 'money < salary * 20' in [Rule \"R\" in r0.drl]"));

        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                        "rule R when\n" +
                        "  $p : Person(money < salary * 20 )\n" +
                        "then\n" +
                        "end";

        KieSession ksession = getKieSession( str );

        Person me = new Person( "Luca");
        me.setMoney(null);
        ksession.insert( me );
        ksession.fireAllRules();
    }

    @Test
    public void testWithQuotedStringConcatenationOnConstraint() {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                        "rule R when\n" +
                        "  $p : Person(name == \"Luca\" + \" II\"  )\n" +
                        "then\n" +
                        "end";

        KieSession ksession = getKieSession( str );

        Person me = new Person( "Luca II");
        me.setMoney(null);
        ksession.insert( me );
        int rulesFired = ksession.fireAllRules();
        assertEquals(rulesFired, 1);
    }

    @Test
    public void testNegatedConstraint() {
        // DROOLS-5791
        String str =
                "rule R when\n" +
                "  $i : Integer()\n" +
                "  String( !($i.intValue > length) )\n" +
                "then\n" +
                "end";

        KieSession ksession = getKieSession( str );

        ksession.insert( 5 );
        ksession.insert( "test" );
        assertEquals(0, ksession.fireAllRules());
    }

    @Test
    public void testMethodCallWithClass() {
        final String str = "package org.drools.mvel.compiler\n" +
                "import " + FactWithMethod.class.getCanonicalName() + ";" +
                "rule r1\n" +
                "when\n" +
                "    FactWithMethod( checkClass( java.lang.String.class ) )\n" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession( str );
        final FactWithMethod fact = new FactWithMethod();
        ksession.insert(fact);
        final int rules = ksession.fireAllRules();
        assertEquals(1, rules);
    }

    public static class FactWithMethod {

        public FactWithMethod() {}

        public boolean checkClass(Class<?> clazz) {
            return clazz.equals(String.class);
        }
    }
}
