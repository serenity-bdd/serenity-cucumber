package net.serenitybdd.cucumber.suiteslicing;


import java.util.List;
import java.util.stream.Collectors;

public class CucumberSuiteSlicer {

    private final List<String> featurePaths;
    private final TestStatistics statistics;

    public CucumberSuiteSlicer(List<String> featurePaths, TestStatistics statistics) {
        this.featurePaths = featurePaths;
        this.statistics = statistics;
    }

    public WeightedCucumberScenarios scenarios(int batchNumber, int batchCount, int forkNumber, int forkCount, List<String> tagFilters) {
        return new CucumberScenarioLoader(featurePaths, statistics).load()
            .filter(cucumberScenario -> tagFilters.isEmpty() || !cucumberScenario.tags.stream()
                .filter(tagFilters::contains)
                .collect(Collectors.toList()).isEmpty())
            .slice(batchNumber).of(batchCount).slice(forkNumber).of(forkCount);
    }

}