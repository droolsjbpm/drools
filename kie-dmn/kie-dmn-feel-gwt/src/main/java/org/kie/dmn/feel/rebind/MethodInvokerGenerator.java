package org.kie.dmn.feel.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import org.kie.dmn.feel.gwtreflection.MethodInvoker;

public class MethodInvokerGenerator extends Generator {

    @Override
    public String generate(final TreeLogger logger,
                           final GeneratorContext context,
                           final String requestedClass) {
        try {
            final TypeOracle typeOracle = context.getTypeOracle();
            final JClassType functionType = typeOracle.findType(requestedClass);

            assertMethodInvokerClass(functionType);
            getFileCreator(logger, context).write();

            return MethodInvokerFileCreator.PACKAGE_NAME + "." + MethodInvokerFileCreator.GENERATED_CLASS_FQCN;
        } catch (final Exception e) {
            return null;
        }
    }

    MethodInvokerFileCreator getFileCreator(final TreeLogger logger,
                                             final GeneratorContext context) {
        return new MethodInvokerFileCreator(context, logger);
    }
    @SuppressWarnings("ConstantConditions")
    void assertMethodInvokerClass(final JClassType functionType) {
        // JClassType#getClass cannot be mocked (GWT API)
        assert MethodInvoker.class.equals(functionType.getClass());
    }
}
