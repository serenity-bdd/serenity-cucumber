package net.serenitybdd.cucumber;

import com.google.common.collect.Iterables;

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

import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cucumber.api.junit.Cucumber;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.formatter.SerenityReporter;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.junit.FeatureRunner;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_BATCH_COUNT;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_BATCH_NUMBER;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_FORK_COUNT;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_FORK_NUMBER;

/**
 * Glue code for running Cucumber via Serenity.
 * Sets up Serenity reporting and instrumentation.
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class CucumberWithSerenity extends Cucumber {

    private static final Logger LOGGER = LoggerFactory.getLogger(CucumberWithSerenity.class);
    private static RuntimeOptions RUNTIME_OPTIONS;
    public static void setRuntimeOptions(RuntimeOptions runtimeOptions) {
        RUNTIME_OPTIONS = runtimeOptions;
    }

    public CucumberWithSerenity(Class clazz) throws InitializationError, IOException {
        super(clazz);
    }

    public static RuntimeOptions currentRuntimeOptions() {
        return RUNTIME_OPTIONS;
    }

    /**
     * Create the Runtime. Sets the Serenity runtime.
     */
    @Override
    protected Runtime createRuntime(ResourceLoader resourceLoader,
                                                     ClassLoader classLoader,
                                                     RuntimeOptions runtimeOptions) throws InitializationError, IOException {
        runtimeOptions.getTagFilters().addAll(environmentSpecifiedTags(runtimeOptions.getTagFilters()));
        RUNTIME_OPTIONS = runtimeOptions;
        return CucumberWithSerenityRuntime.using(resourceLoader, classLoader, runtimeOptions);
    }

    private Collection<String> environmentSpecifiedTags(List<? extends Object> existingTags) {
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

    public static Runtime createSerenityEnabledRuntime(ResourceLoader resourceLoader,
                                                       ClassLoader classLoader,
                                                       RuntimeOptions runtimeOptions,
                                                       Configuration systemConfiguration) {
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        RUNTIME_OPTIONS = runtimeOptions;
        Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
        //the order here is important, add plugin after the runtime is created
        SerenityReporter reporter = new SerenityReporter(systemConfiguration, resourceLoader);
        runtimeOptions.addPlugin(reporter);
        return runtime;
    }

    @Override
    public List<FeatureRunner> getChildren() {
        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        try {
            int batchNumber = environmentVariables.getPropertyAsInteger(SERENITY_BATCH_NUMBER, 1);
            int batchCount = environmentVariables.getPropertyAsInteger(SERENITY_BATCH_COUNT, 1);
            int forkNumber = environmentVariables.getPropertyAsInteger(SERENITY_FORK_NUMBER, 1);
            int forkCount = environmentVariables.getPropertyAsInteger(SERENITY_FORK_COUNT, 1);
            if (batchCount <= 1) {
                return super.getChildren();
            } else {
                RuntimeOptions runtimeOptions = currentRuntimeOptions();
                List<String> tagFilters = runtimeOptions.getTagFilters();

                List<String> featurePaths = runtimeOptions.getFeaturePaths();
                LOGGER.info("Running slice {} of {} using fork {} of {} from feature root {}", batchNumber, batchCount, forkNumber, forkCount, featurePaths);

                WeightedCucumberScenarios weightedCucumberScenarios = new CucumberSuiteSlicer(featurePaths, TestStatistics.from(environmentVariables, featurePaths))
                    .scenarios(batchNumber, batchCount, forkNumber, forkCount, tagFilters);

                List<FeatureRunner> children = super.getChildren();
                AtomicInteger filteredInScenarioCount = new AtomicInteger();
                List<FeatureRunner> filteredChildren = children.stream()
                    .filter(featureRunner -> {
                        String featureName = FeatureRunnerExtractors.extractFeatureName(featureRunner);
                        String featurePath = Iterables.getLast(asList(FeatureRunnerExtractors.featurePathFor(featureRunner).split("/")));
                        boolean matches = weightedCucumberScenarios.scenarios.stream().anyMatch(scenario -> featurePath.equals(scenario.featurePath));
                        LOGGER.debug("{} in filtering '{}' in {}", matches ? "Including" : "Not including", featureName, featurePath);
                        return matches;
                    })
                    .map(featureRunner -> {
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
                            return Optional.<FeatureRunner>empty();
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toList());

                if (filteredInScenarioCount.get() != weightedCucumberScenarios.totalScenarioCount()) {
                    throw new IllegalStateException(
                        String.format("Expected filtered scenarios [%s] and slice scenarios size [%s] to match, but they do not.", filteredInScenarioCount.get(),
                                      weightedCucumberScenarios.scenarios.size()));
                }

                LOGGER.info("Running {} of {} features", filteredChildren.size(), children.size());
                return filteredChildren;
            }
        } catch (Exception e) {
            LOGGER.error("Test failed to start", e);
            throw e;
        }
    }

}
