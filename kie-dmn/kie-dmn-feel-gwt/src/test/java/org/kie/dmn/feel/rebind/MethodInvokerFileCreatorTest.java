package org.kie.dmn.feel.rebind;

import org.junit.Test;
import org.kie.dmn.feel.runtime.FEELFunction;
import org.kie.dmn.feel.runtime.functions.BuiltInFunctions;
import org.kie.dmn.feel.runtime.functions.StringFunction;
import org.kie.dmn.feel.runtime.functions.SumFunction;

public class MethodInvokerFileCreatorTest {

    @Test
    public void name() {
        StringBuilder template = new StringBuilder();
//        for (FEELFunction function : BuiltInFunctions.getFunctions()) {
//
//            new FunctionWriter(template,
//                               function).makeFunctionTemplate();
//        }
        new FunctionWriter(template,
                           new SumFunction()).makeFunctionTemplate();

        System.out.println(template.toString());
        Object obj =null;
        Object[] args = null;
        // TODO manage arrays

    }
//    private Object gg(){
//        Object obj = null;
//        Object[] args = null;
//    }
}