package org.kie.dmn.feel.rebind;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.kie.dmn.feel.gwt.functions.api.FunctionOverrideVariation;
import org.kie.dmn.feel.gwtreflection.MethodInvoker;
import org.kie.dmn.feel.runtime.FEELFunction;
import org.kie.dmn.feel.runtime.functions.BuiltInFunctions;
import org.kie.dmn.feel.runtime.functions.SumFunction;
import org.kie.dmn.feel.runtime.functions.extended.KieExtendedDMNFunctions;

public class MethodInvokerFileCreator {

    public static final String GENERATED_CLASS_FQCN = MethodInvoker.class.getSimpleName() + "Impl";

    public static final String PACKAGE_NAME = MethodInvoker.class.getPackage().getName();

    private final TreeLogger logger;

    private final GeneratorContext context;

    public MethodInvokerFileCreator(final GeneratorContext context,
                                    final TreeLogger logger) {
        this.logger = logger;
        this.context = context;
    }

    private Optional<SourceWriter> getSourceWriter(final GeneratorContext context,
                                                   final TreeLogger logger) {

        final ClassSourceFileComposerFactory composerFactory = getClassSourceFileComposerFactory();
        final Optional<PrintWriter> printWriter = Optional.ofNullable(context.tryCreate(logger, PACKAGE_NAME, GENERATED_CLASS_FQCN));

        return printWriter.map(pw -> composerFactory.createSourceWriter(context, pw));
    }

    ClassSourceFileComposerFactory getClassSourceFileComposerFactory() {

        final ClassSourceFileComposerFactory composerFactory = makeComposerFactory();

        composerFactory.addImport(MethodInvoker.class.getCanonicalName());
        composerFactory.addImport(Method.class.getCanonicalName());
        composerFactory.addImport(List.class.getCanonicalName());
        composerFactory.addImport(SumFunction.class.getCanonicalName());
        composerFactory.addImport(FunctionOverrideVariation.class.getCanonicalName());

        return composerFactory;
    }

    public void write() {
        getSourceWriter(context, logger).ifPresent(sourceWriter -> {
            final StringBuilder template = new StringBuilder()
                    .append("public Object invoke(Object obj, Object... args) {\n");

            appendFunctions(template);

            template.append("  return null;}");
            sourceWriter.print(template.toString());
            sourceWriter.commit(logger);
        });
    }

    private void appendFunctions(final StringBuilder template) {
        for (FEELFunction feelFunction : getFeelFunctions()) {
            new FunctionWriter(template,
                               feelFunction).makeFunctionTemplate();
        }
    }

    ClassSourceFileComposerFactory makeComposerFactory() {
        return new ClassSourceFileComposerFactory(PACKAGE_NAME, GENERATED_CLASS_FQCN);
    }

    static List<FEELFunction> getFeelFunctions() {
        final List<FEELFunction> feelFunctions = new ArrayList<>();
        feelFunctions.addAll(Arrays.asList(BuiltInFunctions.getFunctions()));
        feelFunctions.addAll(Arrays.asList(KieExtendedDMNFunctions.getFunctions()));
        return feelFunctions;
    }
}
