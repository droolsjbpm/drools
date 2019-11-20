package org.drools.modelcompiler.util.lambdareplace;

import org.junit.Test;

import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.*;

public class MaterializedLambdaConsequenceTest {

    @Test
    public void createConsequence() {
        CreatedClass aClass = new MaterializedLambdaConsequence("org.drools.modelcompiler.util.lambdareplace", "rulename")
                .create("(org.drools.modelcompiler.domain.Person p1, org.drools.modelcompiler.domain.Person p2) -> result.setValue( p1.getName() + \" is older than \" + p2.getName())");

        //language=JAVA
        String expectedResult = "" +
                "package org.drools.modelcompiler.util.lambdareplace;\n" +
                "import static rulename.*; " +
                "" +
                "public enum LambdaConsequence2FABA51719447471EAF65717A3CB3A94 implements org.drools.model.functions.Block2<org.drools.modelcompiler.domain.Person, org.drools.modelcompiler.domain.Person>  {\n" +
                "INSTANCE;\n" +
                "        @Override()\n" +
                "        public void execute(org.drools.modelcompiler.domain.Person p1, org.drools.modelcompiler.domain.Person p2) {\n" +
                "            result.setValue(p1.getName() + \" is older than \" + p2.getName());\n" +
                "        }\n" +
                "    }\n";

        assertThat(aClass.getCompilationUnitAsString(), equalToIgnoringWhiteSpace(expectedResult));

    }
}