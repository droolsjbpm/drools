/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.kie.dmn.feel.gwt.functions.rebind;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.kie.dmn.feel.FEEL;
import org.kie.dmn.feel.lang.FEELProfile;
import org.kie.dmn.feel.parser.feel11.profiles.KieExtendedFEELProfile;
import org.kie.dmn.feel.runtime.FEELFunction;
import org.kie.dmn.feel.runtime.functions.BuiltInFunctions;
import org.kie.dmn.feel.runtime.functions.extended.KieExtendedDMNFunctions;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class MethodTemplatesTest {

    private static final List<FEELProfile> profiles = new ArrayList<>();

    static {
        profiles.add(new KieExtendedFEELProfile());
    }

    @Test
    public void testTemplate() {

        final String template = MethodTemplates.getTemplate();
        final String[] templateLines = template.split("\n");

        assertEquals("public List<FunctionOverrideVariation> getDefinitions() {", templateLines[0]);
        assertEquals("    ArrayList definitions = new ArrayList();", templateLines[1]);
        assertTemplateBody(templateLines);
        assertEquals("    return definitions;", templateLines[templateLines.length - 2]);
        assertEquals("}", templateLines[templateLines.length - 1]);
    }

    @Test
    public void testTemplatedFunctionsCanBeEvaluated() {
        final FEEL feel = FEEL.newInstance(profiles);

        final List<FEELFunction> templatedFunctions = MethodTemplates.getFeelFunctions();
        assertTrue(templatedFunctions.stream().allMatch(fn -> feel.evaluate(fn.getName()) instanceof FEELFunction));
    }

    @Test
    public void testTemplatedFunctionsIncludeBuiltInAndKieExtendedFunctions() {
        final List<FEELFunction> templatedFunctions = MethodTemplates.getFeelFunctions();

        assertTrue(templatedFunctions.containsAll(asList(BuiltInFunctions.getFunctions())));
        assertTrue(templatedFunctions.containsAll(asList(KieExtendedDMNFunctions.getFunctions())));
    }

    private void assertTemplateBody(final String[] templateLines) {

        final List<String> lines = asList(templateLines);

        assertLine(lines, "BuiltInType.DATE, \"date\", new Parameter( \"year\", BuiltInType.NUMBER ), new Parameter( \"month\", BuiltInType.NUMBER ), new Parameter( \"day\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.DATE, \"date\", new Parameter( \"from\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.DATE, \"date\", new Parameter( \"from\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.TIME, \"time\", new Parameter( \"from\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.TIME, \"time\", new Parameter( \"hour\", BuiltInType.NUMBER ), new Parameter( \"minute\", BuiltInType.NUMBER ), new Parameter( \"second\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.TIME, \"time\", new Parameter( \"from\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.TIME, \"time\", new Parameter( \"hour\", BuiltInType.NUMBER ), new Parameter( \"minute\", BuiltInType.NUMBER ), new Parameter( \"second\", BuiltInType.NUMBER ), new Parameter( \"offset\", BuiltInType.DURATION ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"date and time\", new Parameter( \"year\", BuiltInType.NUMBER ), new Parameter( \"month\", BuiltInType.NUMBER ), new Parameter( \"day\", BuiltInType.NUMBER ), new Parameter( \"hour\", BuiltInType.NUMBER ), new Parameter( \"minute\", BuiltInType.NUMBER ), new Parameter( \"second\", BuiltInType.NUMBER ), new Parameter( \"hour offset\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"date and time\", new Parameter( \"year\", BuiltInType.NUMBER ), new Parameter( \"month\", BuiltInType.NUMBER ), new Parameter( \"day\", BuiltInType.NUMBER ), new Parameter( \"hour\", BuiltInType.NUMBER ), new Parameter( \"minute\", BuiltInType.NUMBER ), new Parameter( \"second\", BuiltInType.NUMBER ), new Parameter( \"timezone\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"date and time\", new Parameter( \"date\", BuiltInType.DATE_TIME ), new Parameter( \"time\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"date and time\", new Parameter( \"from\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"date and time\", new Parameter( \"year\", BuiltInType.NUMBER ), new Parameter( \"month\", BuiltInType.NUMBER ), new Parameter( \"day\", BuiltInType.NUMBER ), new Parameter( \"hour\", BuiltInType.NUMBER ), new Parameter( \"minute\", BuiltInType.NUMBER ), new Parameter( \"second\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.DURATION, \"duration\", new Parameter( \"from\", BuiltInType.DURATION ) )");
        assertLine(lines, "BuiltInType.DURATION, \"duration\", new Parameter( \"from\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.DURATION, \"years and months duration\", new Parameter( \"from\", BuiltInType.DATE_TIME ), new Parameter( \"to\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.STRING, \"string\", new Parameter( \"from\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.STRING, \"string\", new Parameter( \"mask\", BuiltInType.STRING ), new Parameter( \"p\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"number\", new Parameter( \"from\", BuiltInType.STRING ), new Parameter( \"grouping separator\", BuiltInType.STRING ), new Parameter( \"decimal separator\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.STRING, \"substring\", new Parameter( \"string\", BuiltInType.STRING ), new Parameter( \"start position\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.STRING, \"substring\", new Parameter( \"string\", BuiltInType.STRING ), new Parameter( \"start position\", BuiltInType.NUMBER ), new Parameter( \"length\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.STRING, \"substring before\", new Parameter( \"string\", BuiltInType.STRING ), new Parameter( \"match\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.STRING, \"substring after\", new Parameter( \"string\", BuiltInType.STRING ), new Parameter( \"match\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"string length\", new Parameter( \"string\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.STRING, \"upper case\", new Parameter( \"string\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.STRING, \"lower case\", new Parameter( \"string\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"contains\", new Parameter( \"string\", BuiltInType.STRING ), new Parameter( \"match\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"starts with\", new Parameter( \"string\", BuiltInType.STRING ), new Parameter( \"match\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"ends with\", new Parameter( \"string\", BuiltInType.STRING ), new Parameter( \"match\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"matches\", new Parameter( \"input\", BuiltInType.STRING ), new Parameter( \"pattern\", BuiltInType.STRING ), new Parameter( \"flags\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"matches\", new Parameter( \"input\", BuiltInType.STRING ), new Parameter( \"pattern\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.STRING, \"replace\", new Parameter( \"input\", BuiltInType.STRING ), new Parameter( \"pattern\", BuiltInType.STRING ), new Parameter( \"replacement\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.STRING, \"replace\", new Parameter( \"input\", BuiltInType.STRING ), new Parameter( \"pattern\", BuiltInType.STRING ), new Parameter( \"replacement\", BuiltInType.STRING ), new Parameter( \"flags\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"list contains\", new Parameter( \"list\", BuiltInType.LIST ), new Parameter( \"element\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"count\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"count\", new Parameter( \"c\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"min\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"min\", new Parameter( \"c\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"max\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"max\", new Parameter( \"c\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"sum\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"sum\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"sum\", new Parameter( \"list\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"mean\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"mean\", new Parameter( \"list\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"mean\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.LIST, \"sublist\", new Parameter( \"list\", BuiltInType.LIST ), new Parameter( \"start position\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.LIST, \"sublist\", new Parameter( \"list\", BuiltInType.LIST ), new Parameter( \"start position\", BuiltInType.NUMBER ), new Parameter( \"length\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.LIST, \"append\", new Parameter( \"list\", BuiltInType.LIST ), new Parameter( \"item\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"append\", new Parameter( \"list\", BuiltInType.UNKNOWN ), new Parameter( \"item\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"concatenate\", new Parameter( \"list\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"insert before\", new Parameter( \"list\", BuiltInType.LIST ), new Parameter( \"position\", BuiltInType.NUMBER ), new Parameter( \"newItem\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"remove\", new Parameter( \"list\", BuiltInType.LIST ), new Parameter( \"position\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.LIST, \"reverse\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.LIST, \"index of\", new Parameter( \"list\", BuiltInType.LIST ), new Parameter( \"match\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"union\", new Parameter( \"list\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"distinct values\", new Parameter( \"list\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"flatten\", new Parameter( \"list\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"decimal\", new Parameter( \"n\", BuiltInType.NUMBER ), new Parameter( \"scale\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"floor\", new Parameter( \"n\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"ceiling\", new Parameter( \"n\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"decision table\", new Parameter( \"ctx\", BuiltInType.UNKNOWN ), new Parameter( \"outputs\", BuiltInType.UNKNOWN ), new Parameter( \"input expression list\", BuiltInType.UNKNOWN ), new Parameter( \"input values list\", BuiltInType.LIST ), new Parameter( \"output values\", BuiltInType.UNKNOWN ), new Parameter( \"rule list\", BuiltInType.LIST ), new Parameter( \"hit policy\", BuiltInType.STRING ), new Parameter( \"default output value\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"not\", new Parameter( \"negand\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"sort\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.LIST, \"sort\", new Parameter( \"ctx\", BuiltInType.UNKNOWN ), new Parameter( \"list\", BuiltInType.LIST ), new Parameter( \"precedes\", BuiltInType.FUNCTION ) )");
        assertLine(lines, "BuiltInType.LIST, \"get entries\", new Parameter( \"m\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"get value\", new Parameter( \"m\", BuiltInType.UNKNOWN ), new Parameter( \"key\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"all\", new Parameter( \"b\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"all\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"all\", new Parameter( \"list\", BuiltInType.BOOLEAN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"any\", new Parameter( \"b\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"any\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"any\", new Parameter( \"list\", BuiltInType.BOOLEAN ) )");
        assertLine(lines, "BuiltInType.DURATION, \"abs\", new Parameter( \"n\", BuiltInType.DURATION ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"abs\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"abs\", new Parameter( \"n\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.DURATION, \"abs\", new Parameter( \"n\", BuiltInType.DURATION ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"modulo\", new Parameter( \"dividend\", BuiltInType.NUMBER ), new Parameter( \"divisor\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"product\", new Parameter( \"list\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"product\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"product\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"split\", new Parameter( \"string\", BuiltInType.STRING ), new Parameter( \"delimiter\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.LIST, \"split\", new Parameter( \"string\", BuiltInType.STRING ), new Parameter( \"delimiter\", BuiltInType.STRING ), new Parameter( \"flags\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"stddev\", new Parameter( \"list\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"stddev\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"stddev\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.LIST, \"mode\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.LIST, \"mode\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"sqrt\", new Parameter( \"number\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"log\", new Parameter( \"number\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"exp\", new Parameter( \"number\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"even\", new Parameter( \"number\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"odd\", new Parameter( \"number\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"median\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"median\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.STRING, \"day of week\", new Parameter( \"date\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"day of year\", new Parameter( \"date\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.STRING, \"month of year\", new Parameter( \"date\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"week of year\", new Parameter( \"date\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"is\", new Parameter( \"value1\", BuiltInType.UNKNOWN ), new Parameter( \"value2\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"after\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"after\", new Parameter( \"point\", BuiltInType.UNKNOWN ), new Parameter( \"range\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"after\", new Parameter( \"point1\", BuiltInType.UNKNOWN ), new Parameter( \"point2\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"after\", new Parameter( \"range\", BuiltInType.RANGE ), new Parameter( \"point\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"before\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"before\", new Parameter( \"point\", BuiltInType.UNKNOWN ), new Parameter( \"range\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"before\", new Parameter( \"point1\", BuiltInType.UNKNOWN ), new Parameter( \"point2\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"before\", new Parameter( \"range\", BuiltInType.RANGE ), new Parameter( \"point\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"coincides\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"coincides\", new Parameter( \"point1\", BuiltInType.UNKNOWN ), new Parameter( \"point2\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"starts\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"starts\", new Parameter( \"point\", BuiltInType.UNKNOWN ), new Parameter( \"range\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"started by\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"started by\", new Parameter( \"range\", BuiltInType.RANGE ), new Parameter( \"point\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"finishes\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"finishes\", new Parameter( \"point\", BuiltInType.UNKNOWN ), new Parameter( \"range\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"finished by\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"finished by\", new Parameter( \"range\", BuiltInType.RANGE ), new Parameter( \"point\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"during\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"during\", new Parameter( \"point\", BuiltInType.UNKNOWN ), new Parameter( \"range\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"includes\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"includes\", new Parameter( \"range\", BuiltInType.RANGE ), new Parameter( \"point\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"overlaps\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"overlaps before\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"overlaps after\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"meets\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"met by\", new Parameter( \"range1\", BuiltInType.RANGE ), new Parameter( \"range2\", BuiltInType.RANGE ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"time\", new Parameter( \"from\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"time\", new Parameter( \"hour\", BuiltInType.NUMBER ), new Parameter( \"minute\", BuiltInType.NUMBER ), new Parameter( \"second\", BuiltInType.NUMBER ), new Parameter( \"offset\", BuiltInType.DURATION ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"time\", new Parameter( \"hour\", BuiltInType.NUMBER ), new Parameter( \"minute\", BuiltInType.NUMBER ), new Parameter( \"second\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"time\", new Parameter( \"from\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"date\", new Parameter( \"from\", BuiltInType.DATE_TIME ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"date\", new Parameter( \"year\", BuiltInType.NUMBER ), new Parameter( \"month\", BuiltInType.NUMBER ), new Parameter( \"day\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"date\", new Parameter( \"from\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.DURATION, \"duration\", new Parameter( \"from\", BuiltInType.DURATION ) )");
        assertLine(lines, "BuiltInType.DURATION, \"duration\", new Parameter( \"from\", BuiltInType.STRING ) )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"now\" )");
        assertLine(lines, "BuiltInType.DATE_TIME, \"today\" )");
        assertLine(lines, "BuiltInType.STRING, \"code\", new Parameter( \"value\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"invoke\", new Parameter( \"ctx\", BuiltInType.UNKNOWN ), new Parameter( \"namespace\", BuiltInType.STRING ), new Parameter( \"model name\", BuiltInType.STRING ), new Parameter( \"decision name\", BuiltInType.STRING ), new Parameter( \"parameters\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"nn any\", new Parameter( \"list\", BuiltInType.BOOLEAN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"nn any\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"nn any\", new Parameter( \"b\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"nn all\", new Parameter( \"list\", BuiltInType.BOOLEAN ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"nn all\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.BOOLEAN, \"nn all\", new Parameter( \"b\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn count\", new Parameter( \"c\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn count\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"nn max\", new Parameter( \"c\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"nn max\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn mean\", new Parameter( \"list\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn mean\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn mean\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn median\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn median\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"nn min\", new Parameter( \"c\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.UNKNOWN, \"nn min\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.LIST, \"nn mode\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.LIST, \"nn mode\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn stddev\", new Parameter( \"list\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn stddev\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn stddev\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn sum\", new Parameter( \"list\", BuiltInType.NUMBER ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn sum\", new Parameter( \"list\", BuiltInType.LIST ) )");
        assertLine(lines, "BuiltInType.NUMBER, \"nn sum\", new Parameter( \"n\", BuiltInType.UNKNOWN ) )");
    }

    private void assertLine(final List<String> lines,
                            final String line) {
        assertTrue(lines.contains(String.format("definitions.add( new FunctionOverrideVariation( %s );", line)));
    }
}
