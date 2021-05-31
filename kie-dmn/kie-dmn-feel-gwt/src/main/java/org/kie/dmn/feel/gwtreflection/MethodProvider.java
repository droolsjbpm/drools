package org.kie.dmn.feel.gwtreflection;

import java.lang.reflect.Method;

import org.kie.dmn.feel.gwt.functions.api.FunctionOverrideVariation;

public interface MethodProvider {

    Method get(FunctionOverrideVariation name);
}
