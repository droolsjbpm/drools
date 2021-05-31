package org.kie.dmn.feel.rebind;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.kie.dmn.feel.runtime.FEELFunction;

public class FunctionWriter {

    private final StringBuilder template;
    private final FEELFunction feelFunction;
    private boolean hasArray = false;
    private boolean hasList = false;

    public FunctionWriter(final StringBuilder template,
                          final FEELFunction feelFunction) {
        this.template = template;
        this.feelFunction = feelFunction;
    }

    public void makeFunctionTemplate() {
        template.append(String.format("if (obj instanceof %s) {\n", feelFunction.getClass().getName()));

        for (final Method declaredMethod : getInvokeMethods(feelFunction)) {

            final Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
            template.append("   if (");
            template.append(String.format("args.length == %d", parameterTypes.length));
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i].isArray()) {
                    template.append(" && args[" + i + "].getClass().isArray()");
                } else {
                    template.append(" && args[" + i + "] instanceof " + parameterTypes[i].getName());
                }
            }

            template.append(") {\n");
            template.append("       return ((" + feelFunction.getClass().getName() + ") obj).invoke(");
            for (int i = 0; i < parameterTypes.length; i++) {

                template.append("(" + getSafeName(parameterTypes[i]) + ") args[" + i + "] ");
                if (i < parameterTypes.length - 1) {
                    template.append(", ");
                }
            }

            template.append(");\n");
            template.append("   }\n");
        }

        if (hasArray && hasList) {

            template.append("   if (args.length == 1 && args[0].getClass().isArray()) {\n");
            template.append("       Object[] var = (Object[]) args[0];\n");
            template.append("       if (var[0] instanceof java.util.List) {\n");
            template.append("           return ((" + feelFunction.getClass().getName() + ") obj).invoke((java.util.List) var[0]);\n");
            template.append("       } else {\n");
            template.append("           return ((" + feelFunction.getClass().getName() + ") obj).invoke(var);\n");
            template.append("       }\n");
            template.append("   }\n");
        } else if (hasList) {
            // TODO test String function
            //TODO if
            template.append("   return ((" + feelFunction.getClass().getName() + ") obj).invoke((java.util.List) args[0]);\n");
        } else if (hasArray) {
            //TODO if
            template.append("   return ((" + feelFunction.getClass().getName() + ") obj).invoke(args);\n");

        }
//            template.append("            if (args.length == 1 && args[0] instanceof Number) {\n" +
//                                    "                return ((SumFunction) obj).invoke((Number) args[0]);\n" +
//                                    "            } else if (args.length == 1 && args[0].getClass().isArray()) {\n" +
//                                    "                Object[] var = (Object[]) args[0];\n" +
//                                    "                if (var[0] instanceof Number) {\n" +
//                                    "                    return ((SumFunction) obj).invoke(var);\n" +
//                                    "                } else if (var[0] instanceof List) {\n" +
//                                    "                    return ((SumFunction) obj).invoke((List) var[0]);\n" +
//                                    "                }\n" +
//                                    "            }\n");
        template.append("}\n");
    }

    private String getSafeName(final Class<?> parameterType) {
        if (parameterType.isArray()) {
            return parameterType.getComponentType().getName() + "[]";
        } else {
            return parameterType.getName();
        }
    }

    private Method[] getInvokeMethods(final FEELFunction feelFunction) {
        final ArrayList<Method> result = new ArrayList();

        for (final Method declaredMethod : feelFunction.getClass().getDeclaredMethods()) {
            if (Modifier.isPublic(declaredMethod.getModifiers()) && declaredMethod.getName().equals("invoke")) {

                if (declaredMethod.getParameterTypes().length == 1
                        && declaredMethod.getParameterTypes()[0].isAssignableFrom(List.class)) {
                    hasList = true;
                } else if (declaredMethod.getParameterTypes().length == 1
                        && declaredMethod.getParameterTypes()[0].isArray()) {
                    hasArray = true;
                } else {
                    result.add(declaredMethod);
                }
            }
        }

        return result.toArray(new Method[result.size()]);
    }
}
