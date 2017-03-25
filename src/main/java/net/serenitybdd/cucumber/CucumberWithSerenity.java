package net.serenitybdd.cucumber;

import ch.lambdaj.Lambda;
import ch.lambdaj.function.convert.Converter;
import com.google.common.base.Splitter;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.junit.JUnitOptions;
import cucumber.runtime.junit.JUnitReporter;
import cucumber.runtime.junit.SerenityFeatureRunner;
import cucumber.runtime.model.CucumberFeature;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ch.lambdaj.Lambda.on;
import static cucumber.runtime.junit.Assertions.assertNoCucumberAnnotatedMethods;
import static net.thucydides.core.ThucydidesSystemProperty.TEST_RETRY_COUNT_CUCUMBER;


/**
 * Glue code for running Cucumber via Thucydides.
 * Sets the Thucydides reporter.
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class CucumberWithSerenity extends ParentRunner<SerenityFeatureRunner> {

    private JUnitReporter jUnitReporter;
    private List<SerenityFeatureRunner> children = new ArrayList<>();
    private Runtime runtime;
    private List<CucumberFeature> cucumberFeatures;
    private int maxRetryCount = 0;

    public CucumberWithSerenity(Class clazz) throws InitializationError, IOException {
        super(clazz);
        initialize(clazz,Injectors.getInjector().getInstance(EnvironmentVariables.class));
    }

    public CucumberWithSerenity(Class clazz, EnvironmentVariables environmentVariables) throws InitializationError, IOException {
        super(clazz);
        initialize(clazz, environmentVariables);
    }

    private void initialize(Class clazz,EnvironmentVariables environmentVariables) throws InitializationError, IOException {
        maxRetryCount = TEST_RETRY_COUNT_CUCUMBER.integerFrom(environmentVariables, 0);
        ClassLoader classLoader = clazz.getClassLoader();
        assertNoCucumberAnnotatedMethods(clazz);
        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz);
        RuntimeOptions runtimeOptions = runtimeOptionsFactory.create();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        runtime = createRuntime(resourceLoader, classLoader, runtimeOptions);

        RUNTIME_OPTIONS.set(runtimeOptions);

        final JUnitOptions junitOptions = new JUnitOptions(runtimeOptions.getJunitOptions());
        cucumberFeatures = runtimeOptions.cucumberFeatures(resourceLoader);
        jUnitReporter = new JUnitReporter(runtimeOptions.reporter(classLoader), runtimeOptions.formatter(classLoader), runtimeOptions.isStrict(), junitOptions);
        addChildren(cucumberFeatures,maxRetryCount);
    }

    public CucumberWithSerenity withOutputDirectory(File outputDirectory)
    {
        Configuration systemConfiguration = Injectors.getInjector().getInstance(Configuration.class);
        systemConfiguration.setOutputDirectory(outputDirectory);
        return this;
    }

    private static ThreadLocal<RuntimeOptions> RUNTIME_OPTIONS = new ThreadLocal<>();

    public static RuntimeOptions currentRuntimeOptions() {
        return RUNTIME_OPTIONS.get();
    }

    /**
     * Creates the Runtime. Sets the Serenity runtime.
     * @param resourceLoader
     * @param classLoader
     * @param runtimeOptions
     * @return cucumber Runtime
     * @throws InitializationError
     * @throws IOException
     */
    protected cucumber.runtime.Runtime createRuntime(ResourceLoader resourceLoader,
                                                     ClassLoader classLoader,
                                                     RuntimeOptions runtimeOptions) throws InitializationError, IOException {
        runtimeOptions.getFilters().addAll(environmentSpecifiedTags(runtimeOptions.getFilters()));
        RUNTIME_OPTIONS.set(runtimeOptions);
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
        return createSerenityEnabledRuntime(resourceLoader, classLoader, runtimeOptions, systemConfiguration,maxRetryCount);
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

    public static Runtime createSerenityEnabledRuntime(ResourceLoader resourceLoader,
                                                       ClassLoader classLoader,
                                                       RuntimeOptions runtimeOptions,
                                                       Configuration systemConfiguration,
                                                       int maxRetryCount) {
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        SerenityReporter reporter = new SerenityReporter(systemConfiguration);
        reporter.setMaxRetryCount(maxRetryCount);
        runtimeOptions.addPlugin(reporter);

        RUNTIME_OPTIONS.set(runtimeOptions);

        return new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }

    @Override
    protected Description describeChild(SerenityFeatureRunner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(SerenityFeatureRunner child, RunNotifier notifier) {
        child.run(notifier);
    }

    private void addChildren(List<CucumberFeature> cucumberFeatures,int retryCount) throws InitializationError {
        children.clear();
        for (CucumberFeature cucumberFeature : cucumberFeatures) {
            children.add(new SerenityFeatureRunner(cucumberFeature, runtime, jUnitReporter,retryCount));
        }
    }

    @Override
    public List<SerenityFeatureRunner> getChildren() {
        return children;
    }

    public Runtime getRuntime() {
        return runtime;
    }

}
