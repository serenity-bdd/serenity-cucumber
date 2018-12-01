package net.serenitybdd.cucumber.suiteslicing;

import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.FeatureLoader;
import gherkin.ast.Background;
import gherkin.ast.Scenario;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import net.thucydides.core.util.Inflector;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class ScenarioLineCountStatistics implements TestStatistics {

    private final List<String> featurePaths;
    private final List<TestScenarioResult> results;

    private ScenarioLineCountStatistics(List<String> featurePaths) {
        this.featurePaths = featurePaths;
        ResourceLoader resourceLoader = new MultiLoader(CucumberSuiteSlicer.class.getClassLoader());
        this.results = new FeatureLoader(resourceLoader).load(featurePaths).stream()
            .map(featureToScenarios())
            .flatMap(List::stream)
            .collect(toList());
    }

    public static ScenarioLineCountStatistics fromFeaturePath(String featurePaths) {
        return fromFeaturePaths(asList(featurePaths));
    }

    public static ScenarioLineCountStatistics fromFeaturePaths(List<String> featurePaths) {
        return new ScenarioLineCountStatistics(featurePaths);
    }

    private Function<CucumberFeature, List<TestScenarioResult>> featureToScenarios() {
        return cucumberFeature -> {
            try {
                return (cucumberFeature.getGherkinFeature().getFeature() == null) ? Collections.emptyList() : cucumberFeature.getGherkinFeature().getFeature().getChildren()
                    .stream()
                    .filter(child -> asList(ScenarioOutline.class, Scenario.class).contains(child.getClass()))
                    .map(scenarioToResult(cucumberFeature))
                    .collect(toList());
            } catch (Exception e) {
                throw new IllegalStateException(String.format("Could not extract scenarios from %s", cucumberFeature.getUri()), e);
            }
        };
    }

    private Function<ScenarioDefinition, TestScenarioResult> scenarioToResult(CucumberFeature feature) {
        return scenarioDefinition -> {
            try {
                return new TestScenarioResult(
                    feature.getGherkinFeature().getFeature().getName(),
                    scenarioDefinition.getName(),
                    scenarioStepCountFor(backgroundStepCountFor(feature), scenarioDefinition));
            } catch (Exception e) {
                throw new IllegalStateException(String.format("Could not determine step count for scenario '%s'", scenarioDefinition.getDescription()), e);
            }
        };
    }

    private BigDecimal scenarioStepCountFor(int backgroundStepCount, ScenarioDefinition scenarioDefinition) {
        final int stepCount;
        if (scenarioDefinition instanceof ScenarioOutline) {
            ScenarioOutline outline = (ScenarioOutline) scenarioDefinition;
            Integer exampleCount = outline.getExamples().stream().map(examples -> examples.getTableBody().size()).mapToInt(Integer::intValue).sum();
            stepCount = exampleCount * (backgroundStepCount + outline.getSteps().size());
        } else {
            stepCount = backgroundStepCount + scenarioDefinition.getSteps().size();
        }
        return BigDecimal.valueOf(stepCount);
    }

    private int backgroundStepCountFor(CucumberFeature feature) {
        ScenarioDefinition scenarioDefinition = feature.getGherkinFeature().getFeature().getChildren().get(0);
        if (scenarioDefinition instanceof Background) {
            return scenarioDefinition.getSteps().size();
        } else {
            return 0;
        }
    }

    @Override
    public BigDecimal scenarioWeightFor(String feature, String scenario) {
        return results.stream()
            .filter(record -> record.feature.equals(feature) && record.scenario.equals(scenario))
            .map(TestScenarioResult::duration)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("no result found for scenario '%s' in feature '%s'", scenario, feature)));
    }

    @Override
    public List<TestScenarioResult> records() {
        return results;
    }

    public String toString() {
        return Inflector.getInstance().kebabCase(this.getClass().getSimpleName());
    }
}
