package net.serenitybdd.cucumber.suiteslicing;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CucumberScenarioLoaderTest {

    private TestStatistics testStatistics;

    @Before
    public void setup() {
        testStatistics = new DummyStatsOfWeightingOne();
    }

    @Test
    public void shouldEnsureThatFeaturesWithBackgroundsDontCountThemAsScenarios() {
        WeightedCucumberScenarios weightedCucumberScenarios = new CucumberScenarioLoader(newArrayList("classpath:samples/simple_table_based_scenario.feature"), testStatistics).load();
        assertThat(weightedCucumberScenarios.scenarios, containsInAnyOrder(MatchingCucumberScenario.with()
                                                                               .featurePath("simple_table_based_scenario.feature")
                                                                               .feature("Buying things - with tables")
                                                                               .scenario("Buying lots of widgets"),
                                                                           MatchingCucumberScenario.with()
                                                                               .featurePath("simple_table_based_scenario.feature")
                                                                               .feature("Buying things - with tables")
                                                                               .scenario("Buying more widgets")));
    }

    @Test
    public void shouldLoadFeatureAndScenarioTagsOntoCorrectScenarios() {
        WeightedCucumberScenarios weightedCucumberScenarios = new CucumberScenarioLoader(newArrayList("classpath:samples/simple_table_based_scenario.feature"), testStatistics).load();

        assertThat(weightedCucumberScenarios.scenarios, contains(MatchingCucumberScenario.with()
                                                                     .featurePath("simple_table_based_scenario.feature")
                                                                     .feature("Buying things - with tables")
                                                                     .scenario("Buying lots of widgets")
                                                                     .tags("@shouldPass"),
                                                                 MatchingCucumberScenario.with()
                                                                     .featurePath("simple_table_based_scenario.feature")
                                                                     .feature("Buying things - with tables")
                                                                     .scenario("Buying more widgets")
                                                                     .tags()));
    }


}
