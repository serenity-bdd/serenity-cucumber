package net.serenitybdd.cucumber.suiteslicing;

import com.google.common.collect.FluentIterable;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.FeatureLoader;
import gherkin.ast.Scenario;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Reads cucumber feature files and breaks them down into a collection of scenarios (WeightedCucumberScenarios).
 */
public class CucumberScenarioLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CucumberScenarioLoader.class);
    private final List<String> featurePaths;
    private final TestStatistics statistics;

    public CucumberScenarioLoader(List<String> featurePaths, TestStatistics statistics) {
        this.featurePaths = featurePaths;
        this.statistics = statistics;
    }

    public WeightedCucumberScenarios load() {
        LOGGER.debug("Feature paths are {}", featurePaths);
        ResourceLoader resourceLoader = new MultiLoader(CucumberSuiteSlicer.class.getClassLoader());
        List<WeightedCucumberScenario> weightedCucumberScenarios = new FeatureLoader(resourceLoader).load(featurePaths).stream()
            .map(getScenarios())
            .flatMap(List::stream)
            .collect(toList());

        return new WeightedCucumberScenarios(weightedCucumberScenarios);
    }

    private Function<CucumberFeature, List<WeightedCucumberScenario>> getScenarios() {
        return cucumberFeature -> {
            try {
                return (cucumberFeature.getGherkinFeature().getFeature() == null) ? Collections.emptyList() : cucumberFeature.getGherkinFeature().getFeature().getChildren()
                    .stream()
                    .filter(child -> asList(ScenarioOutline.class, Scenario.class).contains(child.getClass()))
                    .map(scenarioDefinition -> new WeightedCucumberScenario(
                        new File(cucumberFeature.getUri()).getName(),
                        cucumberFeature.getGherkinFeature().getFeature().getName(),
                        scenarioDefinition.getName(),
                        scenarioWeightFor(cucumberFeature, scenarioDefinition),
                        tagsFor(cucumberFeature, scenarioDefinition),
                        scenarioCountFor(scenarioDefinition)))
                    .collect(toList());
            } catch (Exception e) {
                throw new IllegalStateException(String.format("Could not extract scenarios from %s", cucumberFeature.getUri()), e);
            }
        };
    }

    private int scenarioCountFor(ScenarioDefinition scenarioDefinition) {
        if (scenarioDefinition instanceof ScenarioOutline) {
            return ((ScenarioOutline) scenarioDefinition).getExamples().stream().map(examples -> examples.getTableBody().size()).mapToInt(Integer::intValue).sum();
        } else {
            return 1;
        }
    }

    private Set<String> tagsFor(CucumberFeature feature, ScenarioDefinition scenarioDefinition) {
        return FluentIterable.concat(feature.getGherkinFeature().getFeature().getTags(), scenarioTags(scenarioDefinition)).stream().map(Tag::getName).collect(toSet());
    }

    private List<Tag> scenarioTags(ScenarioDefinition scenario) {
        if (Scenario.class.isAssignableFrom(scenario.getClass())) {
            return ((Scenario) scenario).getTags();
        } else {
            return ((ScenarioOutline) scenario).getTags();
        }
    }

    private BigDecimal scenarioWeightFor(CucumberFeature feature, ScenarioDefinition scenarioDefinition) {
        return statistics.scenarioWeightFor(feature.getGherkinFeature().getFeature().getName(), scenarioDefinition.getName());
    }

}
