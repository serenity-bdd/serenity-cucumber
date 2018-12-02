package net.serenitybdd.cucumber;

import cucumber.api.Plugin;
import cucumber.api.StepDefinitionReporter;
import cucumber.api.event.TestRunFinished;
import cucumber.api.event.TestRunStarted;
import cucumber.runner.EventBus;
import cucumber.runner.ThreadLocalRunnerSupplier;
import cucumber.runner.TimeService;
import cucumber.runner.TimeServiceEventBus;
import cucumber.runtime.*;
import cucumber.runtime.Runtime;
import cucumber.runtime.filter.Filters;
import cucumber.runtime.filter.RerunFilters;
import cucumber.runtime.formatter.PluginFactory;
import cucumber.runtime.formatter.Plugins;
import cucumber.runtime.formatter.SerenityReporter;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.junit.Assertions;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.junit.JUnitOptions;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.FeatureLoader;
import net.serenitybdd.cucumber.suiteslicing.CucumberSuiteSlicer;
import net.serenitybdd.cucumber.suiteslicing.ScenarioFilter;
import net.serenitybdd.cucumber.suiteslicing.TestStatistics;
import net.serenitybdd.cucumber.suiteslicing.WeightedCucumberScenarios;
import net.serenitybdd.cucumber.util.FeatureRunnerExtractors;
import net.serenitybdd.cucumber.util.Splitter;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.junit.runner.Description;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_BATCH_COUNT;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_BATCH_NUMBER;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_FORK_COUNT;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_FORK_NUMBER;

/**
 * Glue code for running Cucumber via Serenity.
 * Sets up Serenity reporting and instrumentation.
 */
public class CucumberWithSerenity extends ParentRunner<FeatureRunner> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CucumberWithSerenity.class);

    private final List<FeatureRunner> children = new ArrayList<FeatureRunner>();
    private final EventBus bus;
    private final ThreadLocalRunnerSupplier runnerSupplier;
    private final Filters filters;
    private final JUnitOptions junitOptions;
    private static RuntimeOptions RUNTIME_OPTIONS;
    /**
     * Constructor called by JUnit.
     *
     * @param clazz the class with the @RunWith annotation.
     * @throws InitializationError if there is another problem
     */
    public CucumberWithSerenity(Class clazz) throws InitializationError {
        super(clazz);
        ClassLoader classLoader = clazz.getClassLoader();
        Assertions.assertNoCucumberAnnotatedMethods(clazz);

        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz);
        RuntimeOptions runtimeOptions = runtimeOptionsFactory.create();
        runtimeOptions.getTagFilters().addAll(environmentSpecifiedTags(runtimeOptions.getTagFilters()));

        RUNTIME_OPTIONS = runtimeOptions;

        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        FeatureLoader featureLoader = new FeatureLoader(resourceLoader);
        FeaturePathFeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(featureLoader, runtimeOptions);
        // Parse the features early. Don't proceed when there are lexer errors
        final List<CucumberFeature> features = featureSupplier.get();

        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        BackendSupplier backendSupplier = new BackendModuleBackendSupplier(resourceLoader, classFinder, runtimeOptions);
        this.bus = new TimeServiceEventBus(TimeService.SYSTEM);
        Plugins plugins = new Plugins(classLoader, new PluginFactory(), bus, runtimeOptions);

        Configuration systemConfiguration = Injectors.getInjector().getInstance(Configuration.class);
        SerenityReporter reporter = new SerenityReporter(systemConfiguration, resourceLoader);
        addSerenityReporterPlugin(plugins,reporter);

        this.runnerSupplier = new ThreadLocalRunnerSupplier(runtimeOptions, bus, backendSupplier);
        RerunFilters rerunFilters = new RerunFilters(runtimeOptions, featureLoader);
        this.filters = new Filters(runtimeOptions, rerunFilters);
        this.junitOptions = new JUnitOptions(runtimeOptions.isStrict(), runtimeOptions.getJunitOptions());
        final StepDefinitionReporter stepDefinitionReporter = plugins.stepDefinitionReporter();

        // Start the run before reading the features.
        // Allows the test source read events to be broadcast properly
        bus.send(new TestRunStarted(bus.getTime()));
        for (CucumberFeature feature : features) {
            feature.sendTestSourceRead(bus);
        }
        runnerSupplier.get().reportStepDefinitions(stepDefinitionReporter);
        addChildren(features);
    }

    public static void setRuntimeOptions(RuntimeOptions runtimeOptions) {
        RUNTIME_OPTIONS = runtimeOptions;
    }

    public static RuntimeOptions currentRuntimeOptions() {
        return RUNTIME_OPTIONS;
    }

    private static Collection<String> environmentSpecifiedTags(List<?> existingTags) {
        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        String tagsExpression = ThucydidesSystemProperty.TAGS.from(environmentVariables,"");
        List<String> existingTagsValues = existingTags.stream().map(Object::toString).collect(toList());
        return Splitter.on(",").trimResults().omitEmptyStrings().splitToList(tagsExpression).stream()
                .map(CucumberWithSerenity::toCucumberTag).filter(t -> !existingTagsValues.contains(t)).collect(toList());
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

        FeatureLoader featureLoader = new FeatureLoader(resourceLoader);
        FeaturePathFeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(featureLoader, runtimeOptions);
        // Parse the features early. Don't proceed when there are lexer errors
        final List<CucumberFeature> features = featureSupplier.get();
        EventBus bus = new TimeServiceEventBus(TimeService.SYSTEM);
        // Start the run before reading the features.
        // Allows the test source read events to be broadcast properly
        bus.send(new TestRunStarted(bus.getTime()));
        for (CucumberFeature feature : features) {
            feature.sendTestSourceRead(bus);
        }
        SerenityReporter serenityReporter = new SerenityReporter(systemConfiguration, resourceLoader);
        Runtime runtime = Runtime.builder().withResourceLoader(resourceLoader).withClassFinder(classFinder).
                withClassLoader(classLoader).withRuntimeOptions(runtimeOptions).
                withAdditionalPlugins(serenityReporter).
                withEventBus(bus).withFeatureSupplier(featureSupplier).
                build();

        return runtime;
    }

    private static void addSerenityReporterPlugin(Plugins plugins, SerenityReporter plugin)
    {
        for(Plugin currentPlugin : plugins.getPlugins()){
            if (currentPlugin instanceof SerenityReporter) {
                return;
            }
        }
        plugins.addPlugin(plugin);
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
                bus.send(new TestRunFinished(bus.getTime()));
            }
        };
    }

    private void addChildren(List<CucumberFeature> cucumberFeatures) throws InitializationError {
        for (CucumberFeature cucumberFeature : cucumberFeatures) {
            FeatureRunner featureRunner = new FeatureRunner(cucumberFeature, filters, runnerSupplier, junitOptions);
            if (!featureRunner.isEmpty()) {
                children.add(featureRunner);
            }
        }
    }


    @Override
    public List<FeatureRunner> getChildren() {
        try {
            EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
            RuntimeOptions runtimeOptions = currentRuntimeOptions();
            List<String> tagFilters = runtimeOptions.getTagFilters();
            List<String> featurePaths = runtimeOptions.getFeaturePaths();
            int batchNumber = environmentVariables.getPropertyAsInteger(SERENITY_BATCH_NUMBER, 1);
            int batchCount = environmentVariables.getPropertyAsInteger(SERENITY_BATCH_COUNT, 1);
            int forkNumber = environmentVariables.getPropertyAsInteger(SERENITY_FORK_NUMBER, 1);
            int forkCount = environmentVariables.getPropertyAsInteger(SERENITY_FORK_COUNT, 1);
            if ((batchCount == 1) && (forkCount == 1)) {
                return children;
            } else {
                LOGGER.info("Running slice {} of {} using fork {} of {} from feature paths {}", batchNumber, batchCount, forkNumber, forkCount, featurePaths);

                WeightedCucumberScenarios weightedCucumberScenarios = new CucumberSuiteSlicer(featurePaths, TestStatistics.from(environmentVariables, featurePaths))
                    .scenarios(batchNumber, batchCount, forkNumber, forkCount, tagFilters);

                List<FeatureRunner> unfilteredChildren = children;
                AtomicInteger filteredInScenarioCount = new AtomicInteger();
                List<FeatureRunner> filteredChildren = unfilteredChildren.stream()
                    .filter(forIncludedFeatures(weightedCucumberScenarios))
                    .map(toPossibleFeatureRunner(weightedCucumberScenarios, filteredInScenarioCount))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toList());

                if (filteredInScenarioCount.get() != weightedCucumberScenarios.totalScenarioCount()) {
                    LOGGER.warn(
                        "There is a mismatch between the number of scenarios included in this test run ({}) and the expected number of scenarios loaded ({}). This suggests that the scenario filtering is not working correctly or feature file(s) of an unexpected structure are being run",
                        filteredInScenarioCount.get(),
                        weightedCucumberScenarios.scenarios.size());
                }

                LOGGER.info("Running {} of {} features", filteredChildren.size(), unfilteredChildren.size());
                return filteredChildren;
            }
        } catch (Exception e) {
            LOGGER.error("Test failed to start", e);
            throw e;
        }
    }

    private Function<FeatureRunner, Optional<FeatureRunner>> toPossibleFeatureRunner(WeightedCucumberScenarios weightedCucumberScenarios, AtomicInteger filteredInScenarioCount) {
        return featureRunner -> {
            int initialScenarioCount = featureRunner.getDescription().getChildren().size();
            String featureName = FeatureRunnerExtractors.extractFeatureName(featureRunner);
            try {
                ScenarioFilter filter = weightedCucumberScenarios.createFilterContainingScenariosIn(featureName);
                String featurePath = FeatureRunnerExtractors.featurePathFor(featureRunner);
                featureRunner.filter(filter);
                if (!filter.scenariosIncluded().isEmpty()) {
                    LOGGER.info("{} scenario(s) included for '{}' in {}", filter.scenariosIncluded().size(), featureName, featurePath);
                    filter.scenariosIncluded().forEach(scenario -> {
                        LOGGER.info("Included scenario '{}'", scenario);
                        filteredInScenarioCount.getAndIncrement();
                    });
                }
                if (!filter.scenariosExcluded().isEmpty()) {
                    LOGGER.debug("{} scenario(s) excluded for '{}' in {}", filter.scenariosExcluded().size(), featureName, featurePath);
                    filter.scenariosExcluded().forEach(scenario -> LOGGER.debug("Excluded scenario '{}'", scenario));
                }
                return Optional.of(featureRunner);
            } catch (NoTestsRemainException e) {
                LOGGER.info("Filtered out all {} scenarios for feature '{}'", initialScenarioCount, featureName);
                return Optional.empty();
            }
        };
    }

    private Predicate<FeatureRunner> forIncludedFeatures(WeightedCucumberScenarios weightedCucumberScenarios) {
        return featureRunner -> {
            String featureName = FeatureRunnerExtractors.extractFeatureName(featureRunner);
            String featurePath = Paths.get(FeatureRunnerExtractors.featurePathFor(featureRunner)).getFileName().toString();
            boolean matches = weightedCucumberScenarios.scenarios.stream().anyMatch(scenario -> featurePath.equals(scenario.featurePath));
            LOGGER.debug("{} in filtering '{}' in {}", matches ? "Including" : "Not including", featureName, featurePath);
            return matches;
        };
    }

}
