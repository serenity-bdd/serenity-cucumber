package net.serenitybdd.cucumber;

import net.serenitybdd.cucumber.suiteslicing.CucumberSuiteSlicer;
import net.serenitybdd.cucumber.suiteslicing.ScenarioFilter;
import net.serenitybdd.cucumber.suiteslicing.TestStatistics;
import net.serenitybdd.cucumber.suiteslicing.WeightedCucumberScenarios;
import net.serenitybdd.cucumber.util.FeatureRunnerExtractors;
import net.serenitybdd.cucumber.util.TagParser;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;

import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import cucumber.api.junit.Cucumber;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.formatter.SerenityReporter;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.junit.FeatureRunner;

import static java.util.stream.Collectors.toList;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_BATCH_COUNT;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_BATCH_NUMBER;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_FORK_COUNT;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_FORK_NUMBER;

/**
 * Glue code for running Cucumber via Serenity.
 * Sets up Serenity reporting and instrumentation.
 */
public class CucumberWithSerenity extends Cucumber {

    private static final Logger LOGGER = LoggerFactory.getLogger(CucumberWithSerenity.class);
    private static ThreadLocal<RuntimeOptions> RUNTIME_OPTIONS = new ThreadLocal<>();
    public static void setRuntimeOptions(RuntimeOptions runtimeOptions) {
        RUNTIME_OPTIONS.set(runtimeOptions);
    }

    public CucumberWithSerenity(Class clazz) throws InitializationError, IOException {
        super(clazz);
    }

    public static RuntimeOptions currentRuntimeOptions() {
        return RUNTIME_OPTIONS.get();
    }

    /**
     * Create the Runtime. Sets the Serenity runtime.
     */
    @Override
    protected Runtime createRuntime(ResourceLoader resourceLoader,
                                    ClassLoader classLoader,
                                    RuntimeOptions runtimeOptions) {

        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);

        runtimeOptions.getTagFilters().addAll(TagParser.additionalTagsSuppliedFrom(environmentVariables, runtimeOptions.getTagFilters()));
        setRuntimeOptions(runtimeOptions);
        return CucumberWithSerenityRuntime.using(resourceLoader, classLoader, runtimeOptions);
    }

    public static Runtime createSerenityEnabledRuntime(ResourceLoader resourceLoader,
                                                       ClassLoader classLoader,
                                                       RuntimeOptions runtimeOptions,
                                                       Configuration systemConfiguration) {
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        setRuntimeOptions(runtimeOptions);
        Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
        //the order here is important, add plugin after the runtime is created
        SerenityReporter reporter = new SerenityReporter(systemConfiguration, resourceLoader);
        runtimeOptions.addPlugin(reporter);
        return runtime;
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
                return super.getChildren();
            } else {
                LOGGER.info("Running slice {} of {} using fork {} of {} from feature paths {}", batchNumber, batchCount, forkNumber, forkCount, featurePaths);

                WeightedCucumberScenarios weightedCucumberScenarios = new CucumberSuiteSlicer(featurePaths, TestStatistics.from(environmentVariables, featurePaths))
                        .scenarios(batchNumber, batchCount, forkNumber, forkCount, tagFilters);

                List<FeatureRunner> unfilteredChildren = super.getChildren();
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
