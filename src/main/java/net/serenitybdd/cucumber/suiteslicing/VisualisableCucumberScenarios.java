package net.serenitybdd.cucumber.suiteslicing;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;

public class VisualisableCucumberScenarios extends WeightedCucumberScenarios {

    public final Integer slice;
    public final Integer forkNumber;

    private VisualisableCucumberScenarios(Integer slice, Integer forkNumber, WeightedCucumberScenarios WeightedCucumberScenarios) {
        super(WeightedCucumberScenarios.scenarios);
        this.forkNumber = forkNumber;
        this.slice = slice;
    }

    public static VisualisableCucumberScenarios create(Integer slice, Integer forkNumber, WeightedCucumberScenarios WeightedCucumberScenarios) {
        return new VisualisableCucumberScenarios(slice, forkNumber, WeightedCucumberScenarios);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static List<VisualisableCucumberScenarios> visualise(int forkCount, List<WeightedCucumberScenarios> slices) {
        return slices.stream()
            .map(slice -> IntStream.rangeClosed(1, forkCount).mapToObj(forkNumber -> create(slices.indexOf(slice) + 1, forkNumber, slice.slice(forkNumber).of(forkCount)))
                .collect(toList())).flatMap(List::stream).collect(toList());
    }

}
