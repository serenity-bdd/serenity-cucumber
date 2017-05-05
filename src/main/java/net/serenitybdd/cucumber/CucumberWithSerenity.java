package net.serenitybdd.cucumber;

import ch.lambdaj.Lambda;
import ch.lambdaj.function.convert.Converter;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import cucumber.api.junit.Cucumber;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static ch.lambdaj.Lambda.on;

/**
 * Glue code for running Cucumber via Thucydides.
 * Sets the Thucydides reporter.
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class CucumberWithSerenity extends Cucumber {

    private static final List<String> DEFAULT_FEATURE_PATHS = ImmutableList.of("src/test/resources/features");

    public CucumberWithSerenity(Class clazz) throws InitializationError, IOException
    {
        super(clazz);
        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz);
        RUNTIME_OPTIONS.set(runtimeOptionsFactory.create());
    }

    private static ThreadLocal<RuntimeOptions> RUNTIME_OPTIONS = new ThreadLocal<>();

    public static RuntimeOptions currentRuntimeOptions() {
        return RUNTIME_OPTIONS.get();
    }

    /**
     * Create the Runtime. Sets the Serenity runtime.
     */
    protected cucumber.runtime.Runtime createRuntime(ResourceLoader resourceLoader,
                                                     ClassLoader classLoader,
                                                     RuntimeOptions runtimeOptions) throws InitializationError, IOException {
        runtimeOptions.getFilters().addAll(environmentSpecifiedTags(runtimeOptions.getFilters()));
        RUNTIME_OPTIONS.set(runtimeOptions);
        System.out.println("Setting runtime options in createRuntime()");
        return createSerenityEnabledRuntime(resourceLoader, classLoader, runtimeOptions);
    }

    private Collection<String> environmentSpecifiedTags(List<Object> existingTags) {
        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        String tagsExpression = ThucydidesSystemProperty.TAGS.from(environmentVariables,"");
        List<String> newTags  = Lambda.convert(Splitter.on(",").trimResults().omitEmptyStrings().splitToList(tagsExpression),
                                               toCucumberTags());
        newTags.removeAll(stringVersionOf(existingTags));
        return newTags;
    }

    private Collection<String> stringVersionOf(List<Object> existingTags) {
        return Lambda.extract(existingTags, on(Object.class).toString());
    }

    private Converter<String, String> toCucumberTags() {
        return new Converter<String, String>() {

            @Override
            public String convert(String from) {
                from = from.replaceAll(":","=");
                if (from.startsWith("~@")) { return from; }
                if (from.startsWith("@")) { return from; }
                if (from.startsWith("~")) { return "~@" + from.substring(1); }

                return "@" + from;
            }
        };
    }

    private Runtime createSerenityEnabledRuntime(ResourceLoader resourceLoader,
                                                 ClassLoader classLoader,
                                                 RuntimeOptions runtimeOptions) {
        Configuration systemConfiguration = Injectors.getInjector().getInstance(Configuration.class);
        return createSerenityEnabledRuntime(resourceLoader, classLoader, runtimeOptions, systemConfiguration);
    }

    public static Runtime createSerenityEnabledRuntime(ResourceLoader resourceLoader,
                                                       ClassLoader classLoader,
                                                       RuntimeOptions runtimeOptions,
                                                       Configuration systemConfiguration) {
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        SerenityReporter reporter = new SerenityReporter(systemConfiguration);
        runtimeOptions.addPlugin(reporter);
        RUNTIME_OPTIONS.set(runtimeOptions);
        return new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }

    public static List<String> getFeaturePaths() {
        return RUNTIME_OPTIONS.get().getFeaturePaths();
    }
}
