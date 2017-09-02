package net.serenitybdd.cucumber;

import com.google.common.base.Splitter;
import cucumber.api.junit.Cucumber;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Glue code for running Cucumber via Serenity.
 * Sets up Serenity reporting and instrumentation.
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class CucumberWithSerenity extends Cucumber {

    public static void main(String[] argv) throws Throwable {
        byte exitstatus = run(argv, Thread.currentThread().getContextClassLoader());
        System.exit(exitstatus);

//        net.serenitybdd.cucumber.CucumberWithSerenity.main

    }

    public static byte run(String[] argv, ClassLoader classLoader) throws IOException {
        RuntimeOptions runtimeOptions = new RuntimeOptions(new ArrayList(Arrays.asList(argv)));
        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);

        Runtime runtime =  CucumberWithSerenityRuntime.using(resourceLoader, classLoader, classFinder, runtimeOptions);

        RUNTIME_OPTIONS.set(runtimeOptions);
        runtime.run();
        return runtime.exitStatus();
    }

    ////

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
    @Override
    protected cucumber.runtime.Runtime createRuntime(ResourceLoader resourceLoader,
                                                     ClassLoader classLoader,
                                                     RuntimeOptions runtimeOptions) throws InitializationError, IOException {
        runtimeOptions.getFilters().addAll(environmentSpecifiedTags(runtimeOptions.getFilters()));
        RUNTIME_OPTIONS.set(runtimeOptions);
        return CucumberWithSerenityRuntime.using(resourceLoader, classLoader, runtimeOptions);
    }

    private Collection<String> environmentSpecifiedTags(List<Object> existingTags) {
        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        String tagsExpression = ThucydidesSystemProperty.TAGS.from(environmentVariables,"");
        List<String> existingTagsValues = existingTags.stream().map(Object::toString).collect(Collectors.toList());
        return Splitter.on(",").trimResults().omitEmptyStrings().splitToList(tagsExpression).stream()
                .map(this::toCucumberTag).filter(t -> !existingTagsValues.contains(t)).collect(Collectors.toList());
    }

    private String toCucumberTag(String from) {
        String tag = from.replaceAll(":","=");
        if (tag.startsWith("~@") || tag.startsWith("@")) { return tag; }
        if (tag.startsWith("~")) { return "~@" + tag.substring(1); }

        return "@" + tag;
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
