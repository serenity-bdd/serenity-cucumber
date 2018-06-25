package net.serenitybdd.cucumber;

import cucumber.api.Plugin;
import cucumber.api.event.TestRunFinished;
import cucumber.api.formatter.Formatter;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.formatter.SerenityReporter;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.junit.Assertions;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.junit.JUnitOptions;
import cucumber.runtime.junit.JUnitReporter;
import cucumber.runtime.model.CucumberFeature;
import net.serenitybdd.cucumber.util.Splitter;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Glue code for running Cucumber via Serenity.
 * Sets up Serenity reporting and instrumentation.
 *
 * Because the {@link cucumber.api.junit.Cucumber} class does not allow programmatically
 * registering the plugins, in order to avoid registering the {@link SerenityReporter}
 * with each feature file, this class embeds the implementation of the {@link cucumber.api.junit.Cucumber}
 * as it is in the version 3.0.2
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class CucumberWithSerenity extends ParentRunner<FeatureRunner> {

    private final JUnitReporter jUnitReporter;
    private final List<FeatureRunner> children = new ArrayList<FeatureRunner>();
    private final Runtime runtime;
    private final Formatter formatter;

    private static RuntimeOptions RUNTIME_OPTIONS;

    public static void setRuntimeOptions(RuntimeOptions runtimeOptions) {
        RUNTIME_OPTIONS = runtimeOptions;
    }

    public CucumberWithSerenity(Class clazz) throws InitializationError
    {
        super(clazz);
        ClassLoader classLoader = clazz.getClassLoader();
        Assertions.assertNoCucumberAnnotatedMethods(clazz);

        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz);
        RuntimeOptions runtimeOptions = runtimeOptionsFactory.create();
        runtimeOptions.getTagFilters().addAll(environmentSpecifiedTags(runtimeOptions.getTagFilters()));

        RUNTIME_OPTIONS = runtimeOptions;

        ResourceLoader resourceLoader = new MultiLoader(classLoader);

        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        runtime = new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
        formatter = runtimeOptions.formatter(classLoader);
        final JUnitOptions junitOptions = new JUnitOptions(runtimeOptions.getJunitOptions());
        final List<CucumberFeature> cucumberFeatures = runtimeOptions.cucumberFeatures(resourceLoader, runtime.getEventBus());
        Configuration systemConfiguration = Injectors.getInjector().getInstance(Configuration.class);
        SerenityReporter reporter = new SerenityReporter(systemConfiguration, resourceLoader);
        addSerenityReporterPlugin(runtimeOptions,reporter);

        jUnitReporter = new JUnitReporter(runtime.getEventBus(), runtimeOptions.isStrict(), junitOptions);
        addChildren(cucumberFeatures);

    }

    public static RuntimeOptions currentRuntimeOptions() {
        return RUNTIME_OPTIONS;
    }

    private static Collection<String> environmentSpecifiedTags(List<?> existingTags) {
        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        String tagsExpression = ThucydidesSystemProperty.TAGS.from(environmentVariables,"");
        List<String> existingTagsValues = existingTags.stream().map(Object::toString).collect(Collectors.toList());
        return Splitter.on(",").trimResults().omitEmptyStrings().splitToList(tagsExpression).stream()
                .map(CucumberWithSerenity::toCucumberTag).filter(t -> !existingTagsValues.contains(t)).collect(Collectors.toList());
    }

    private static String toCucumberTag(String from) {
        String tag = from.replaceAll(":","=");
        if (tag.startsWith("~@") || tag.startsWith("@")) { return tag; }
        if (tag.startsWith("~")) { return "~@" + tag.substring(1); }

        return "@" + tag;
    }

    public static Runtime createSerenityEnabledRuntime(ResourceLoader resourceLoader,
                                                       ClassLoader classLoader,
                                                       RuntimeOptions runtimeOptions,
                                                       Configuration systemConfiguration) {
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        runtimeOptions.getTagFilters().addAll(environmentSpecifiedTags(runtimeOptions.getTagFilters()));
        RUNTIME_OPTIONS = runtimeOptions;
        Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
        //the order here is important, add plugin after the runtime is created
        SerenityReporter reporter = new SerenityReporter(systemConfiguration, resourceLoader);
        addSerenityReporterPlugin(runtimeOptions,reporter);
        runtimeOptions.addPlugin(reporter);
        return runtime;
    }

    private static void addSerenityReporterPlugin(RuntimeOptions runtimeOptions, SerenityReporter plugin)
    {
        List<Plugin> plugins = runtimeOptions.getPlugins();
        for(Plugin currentPlugin :plugins){
            if (currentPlugin instanceof SerenityReporter) {
                return;
            }
        }
        plugins.add(plugin);
    }

    @Override
    public List<FeatureRunner> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(FeatureRunner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(FeatureRunner child, RunNotifier notifier) {
        child.run(notifier);
    }

    @Override
    protected Statement childrenInvoker(RunNotifier notifier) {
        final Statement features = super.childrenInvoker(notifier);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                features.evaluate();
                runtime.getEventBus().send(new TestRunFinished(runtime.getEventBus().getTime()));
            }
        };
    }

    private void addChildren(List<CucumberFeature> cucumberFeatures) throws InitializationError {
        for (CucumberFeature cucumberFeature : cucumberFeatures) {
            FeatureRunner featureRunner = new FeatureRunner(cucumberFeature, runtime, jUnitReporter);
            if (!featureRunner.isEmpty()) {
                children.add(featureRunner);
            }
        }
    }
}
