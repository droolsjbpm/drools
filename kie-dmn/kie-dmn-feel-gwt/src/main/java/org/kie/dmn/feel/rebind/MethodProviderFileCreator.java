package org.kie.dmn.feel.rebind;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Optional;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.kie.dmn.feel.gwt.functions.api.FunctionOverrideVariation;
import org.kie.dmn.feel.gwtreflection.MethodProvider;

public class MethodProviderFileCreator {

    public static final String GENERATED_CLASS_FQCN = MethodProvider.class.getSimpleName() + "Impl";

    public static final String PACKAGE_NAME = MethodProvider.class.getPackage().getName();

    private final TreeLogger logger;

    private final GeneratorContext context;

    public MethodProviderFileCreator(final GeneratorContext context,
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

        composerFactory.addImport(MethodProvider.class.getCanonicalName());
        composerFactory.addImport(Method.class.getCanonicalName());
        composerFactory.addImport(FunctionOverrideVariation.class.getCanonicalName());

        return composerFactory;
    }

    public void write() {
        getSourceWriter(context, logger).ifPresent(sourceWriter -> {
            final String template = new StringBuilder()
                    .append("public Method get(FunctionOverrideVariation variation) {")
                    .append("   return new Method( variation );")
                    .append("}")
                    .toString();
            sourceWriter.print(template);
            sourceWriter.commit(logger);
        });
    }

    ClassSourceFileComposerFactory makeComposerFactory() {
        return new ClassSourceFileComposerFactory(PACKAGE_NAME, GENERATED_CLASS_FQCN);
    }
}
