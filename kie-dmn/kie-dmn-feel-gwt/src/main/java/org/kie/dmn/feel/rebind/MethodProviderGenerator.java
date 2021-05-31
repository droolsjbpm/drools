package org.kie.dmn.feel.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import org.kie.dmn.feel.gwtreflection.MethodProvider;

public class MethodProviderGenerator extends Generator {

    @Override
    public String generate(final TreeLogger logger,
                           final GeneratorContext context,
                           final String requestedClass) {
        try {
            final TypeOracle typeOracle = context.getTypeOracle();
            final JClassType functionType = typeOracle.findType(requestedClass);

            assertMethodProviderClass(functionType);
            getFileCreator(logger, context).write();

            return MethodProviderFileCreator.PACKAGE_NAME + "." + MethodProviderFileCreator.GENERATED_CLASS_FQCN;
        } catch (final Exception e) {
            return null;
        }
    }

    MethodProviderFileCreator getFileCreator(final TreeLogger logger,
                                             final GeneratorContext context) {
        return new MethodProviderFileCreator(context, logger);
    }
    @SuppressWarnings("ConstantConditions")
    void assertMethodProviderClass(final JClassType functionType) {
        // JClassType#getClass cannot be mocked (GWT API)
        assert MethodProvider.class.equals(functionType.getClass());
    }
}
