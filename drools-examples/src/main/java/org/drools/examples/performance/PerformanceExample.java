package org.drools.examples.performance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.definition.type.FactType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.SystemEventListener;


import java.util.ArrayList;

/**
 * Created by Asif Iqbal on 2/2/2016.
 */
public class PerformanceExample {
    public static void main(final String[] args) throws Exception{
        final long numberOfRulesToBuild = 20000;
        boolean useAccumulate = false;
        KieServices ks = KieServices.Factory.get();
        KieRepository kr = ks.getRepository();
        KieFileSystem kfs = ks.newKieFileSystem();
        System.out.println("********* Numbers of rules " + numberOfRulesToBuild + " accumulate " + useAccumulate + " *********");
        long startTime = System.currentTimeMillis();
        //System.out.println(getRules(numberOfRulesToBuild, true));
        kfs.write("src/main/resources/examples/pertest.drl", getRules(numberOfRulesToBuild, useAccumulate));

        KieBuilder kb = ks.newKieBuilder(kfs);

        kb.buildAll(); // kieModule is automatically deployed to KieRepository if successfully built.
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kb.getResults().toString());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Time to build: " + (endTime - startTime) + " ms" );
        startTime = System.currentTimeMillis();
        KieContainer kContainer = ks.newKieContainer(kr.getDefaultReleaseId());
        endTime = System.currentTimeMillis();
        System.out.println("Time to load container: " + (endTime - startTime) + " ms" );
        StatelessKieSession kSession = kContainer.newStatelessKieSession();
        ArrayList output = new ArrayList();
        kSession.setGlobal("mo", output);

        FactType ft = kContainer.getKieBase().getFactType("com.epsilon.types", "TransactionC");
        Object o = ft.newInstance();
        Gson gConverter = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
        Object fo = gConverter.fromJson(getFact(), o.getClass());
        kSession.execute(fo);
        String rulesOutput = gConverter.toJson(output);
        System.out.println(rulesOutput);

    }

    private static String getFact()
    {
        return "{\n" +
                "\"TransactionNumber\": \"88882\",\n" +
                "\"TrackingID\": \"T001\",\n" +
                "\"CurrencyCode\": \"USD\",\n" +
                "\"TransactionNetTotal\" : 100.0,\n" +
                "\"StoreCode\": \"D001\",\n" +
                "\"CardNumber\": \"3614838386\",\n" +
                "\"TransactionDetails\": [\n" +
                "{\n" +
                "\"Quantity\": 25,\n" +
                "\"ItemNumber\": \"SKU1\",\n" +
                "\"BrandID\": \"Nike\",\n" +
                "\"SKU\": \"SKU1\",\n" +
                "\"ProductCategoryCode\" : \"Clothing\"\n" +
                "}]\n" +
                "}";
    }
    private static String getRules(long numberofRules, boolean useAccumulate)
    {
        final long startTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder("package com.epsilon.types;\n");
        sb.append(getImportStatements());
        sb.append("global ArrayList<Outcome> mo;");
        sb.append(getDeclareStatements());
        for (long l =0; l <numberofRules; l++) {
            sb.append(createRule(l, useAccumulate));
        }
        final long endTime = System.currentTimeMillis();
        System.out.println("Time to generate: " + (endTime - startTime) + " ms");
        return sb.toString();
    }

    private static String createRule(long number, boolean useAccumulate)
    {
        return "" +
                "rule \"rule" + number + "\" \n" +
                "when   t : TransactionC() \n" +
                "d: TransactionDetailsC(ItemNumber == \"SKU" + number + "\") from t.TransactionDetails \n" +
                "accumulate($item:TransactionDetailsC(ItemNumber == \"SKU" + number + "\") from t.TransactionDetails, $totQty: collectList($item.getQuantity()))\n" +
                "then \n" +
                "mo.add(new Outcome(\"rule" + number + "\", d.getBrandID()));\n" +
                "end \n" ;
    }

    private static String getDeclareStatements()
    {
        return "" +
                "declare TransactionC \n" +
                "CardNumber : String \n" +
                "StoreCode : String \n" +
                "TrackingID : String \n" +
                "CurrencyCode : String \n" +
                "TransactionNetTotal : Double \n" +
                "TransactionNumber : String \n" +
                "TransactionDetails : TransactionDetailsC[] \n" +
                "end \n" +
                "declare TransactionDetailsC \n" +
                "ItemNumber : String \n" +
                "BrandID : String \n" +
                "SKU : String \n" +
                "ProductCategoryCode : String \n" +
                "Quantity : Double \n" +
                "end\n" +
                "declare Outcome \n" +
                "RuleId : String \n" +
                "OutcomeValue : String \n" +
                "end \n";
    }

    private static String getImportStatements()
    {
        return "import java.util.ArrayList \n" +
                "import java.util.List \n";
    }
}
