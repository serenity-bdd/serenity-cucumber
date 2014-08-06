package net.thucydides.cucumber.web

import com.github.goldin.spock.extensions.tempdir.TempDir;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.reports.OutcomeFormat;
import net.thucydides.core.reports.TestOutcomeLoader
import net.thucydides.cucumber.integration.SimpleSeleniumDifferentBrowserScenario
import net.thucydides.cucumber.integration.SimpleSeleniumFailingAndPassingScenario
import net.thucydides.cucumber.integration.SimpleSeleniumPageObjects;
import net.thucydides.cucumber.integration.SimpleTableScenario;
import net.thucydides.cucumber.integration.SimpleSeleniumScenario;
import net.thucydides.cucumber.integration.SimpleSeleniumFailingScenario;
import org.junit.Before;
import org.junit.Test;
import spock.lang.Specification;

import java.util.List;

import static net.thucydides.core.model.TestResult.SUCCESS;
import static net.thucydides.cucumber.util.CucumberRunner.thucydidesRunnerForCucumberTestRunner;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class WhenRunningWebCucumberStories extends Specification {

    @TempDir
    File outputDirectory

    /*@Before
    public void reset_driver() {
        environmentVariables.setProperty("webdriver.driver", "phantomjs");
    } */

   /* @Test
    public void a_test_should_have_storywide_tags_defined_by_the_tag_meta_field() throws Throwable {

        // Given
        ThucydidesJUnitStories story = newStory("aPassingBehaviorWithSelenium.story");

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
    }*/

    def "should run table-driven scenarios successfully"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleSeleniumScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.title == "A scenario that uses selenium"

        and: "there should be one step for each row in the table"
        testOutcome.stepCount == 2
    }


    def "a_failing_story_should_generate_failure_test_outcome"() throws Throwable {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleSeleniumFailingScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]


        then:
        testOutcome.title == "A failing scenario that uses selenium"
        testOutcome.isFailure();

        and: "there should be one step for each row in the table"
        testOutcome.stepCount == 2
    }



    /*def "a_test_should_use_a_different_browser_if_requested"()  {

        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleSeleniumDifferentBrowserScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]


        then:
        testOutcome.title == "A failing scenario that uses selenium"
        testOutcome.isSuccess();

        and: "there should be one step for each row in the table"
        testOutcome.stepCount == 2
    } */


   def "a_cucumber_step_library_can_use_page_objects_directly"()  {

        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleSeleniumPageObjects.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]


        then:
        testOutcome.title == "A scenario that uses selenium"
        testOutcome.isSuccess();

        and: "there should be one step for each row in the table"
        testOutcome.stepCount == 2
    }


    def "stories_with_errors_in_one_scenario_should_still_run_subsequent_scenarios"()  {

        given:
        def runtimeProperties =  new Properties();
        runtimeProperties.setProperty("restart.browser.each.scenario","true");
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleSeleniumFailingAndPassingScenario.class, outputDirectory, runtimeProperties);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        //def testOutcome = recordedTestOutcomes[0]


        then:
        //testOutcome.title == "A failing scenario that uses selenium"
        assertThat(recordedTestOutcomes.size(), is(2));
        assertThat(recordedTestOutcomes.get(0).getResult(), is(TestResult.FAILURE));
        assertThat(recordedTestOutcomes.get(1).getResult(), is(TestResult.SUCCESS));

    }

    /*@Test
    public void should_be_able_to_specify_the_browser_in_the_base_test() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new APassingWebTestSampleWithASpecifiedBrowser();
        story.setEnvironmentVariables(environmentVariables);

        System.out.println("Output dir = " + outputDirectory.getAbsolutePath());
        // When
        run(story);

        // Then
        System.out.println("Loading from output dir = " + outputDirectory.getAbsolutePath());
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
    }

    @Test
    public void should_be_able_to_set_thucydides_properties_in_the_base_test() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new APassingWebTestSampleWithThucydidesPropertiesDefined(systemConfiguration);
        story.setEnvironmentVariables(environmentVariables);

        // When
        run(story);

        // Then

        assertThat(story.getSystemConfiguration().getBaseUrl(), is("some-base-url"));
        assertThat(story.getSystemConfiguration().getElementTimeout(), is(5));
        assertThat(story.getSystemConfiguration().getUseUniqueBrowser(), is(true));
    }


    @Test
    public void data_driven_steps_should_appear_as_nested_steps() throws Throwable {

        // Given
        ThucydidesJUnitStories story = newStory("dataDrivenBehavior.story");

        // When
        run(story);

        // Then
        List<TestOutcome> allOutcomes = loadTestOutcomes();
        assertThat(allOutcomes.size(), is(1));

        List<TestStep> topLevelSteps = allOutcomes.get(0).getTestSteps();
        assertThat(topLevelSteps.size(), is(3));

        List<TestStep> nestedDataDrivenSteps = topLevelSteps.get(2).getChildren().get(0).getChildren();
        assertThat(nestedDataDrivenSteps.size(), is(3));

    }

    @Test
    public void browser_should_not_closed_between_given_stories_and_scenario_steps() throws Throwable {

        // Given
        ThucydidesJUnitStories story = newStory("aBehaviorWithGivenStories.story");

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
    }

    @Test
    public void two_scenarii_using_the_same_given_story_should_return_two_test_outcomes() throws Throwable {
        ThucydidesJUnitStories story = newStory("LookupADefinitionSuite.story");
        run(story);
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(2));
    } */
}
