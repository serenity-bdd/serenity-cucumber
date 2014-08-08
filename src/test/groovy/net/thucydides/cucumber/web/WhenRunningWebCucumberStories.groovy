package net.thucydides.cucumber.web

import com.github.goldin.spock.extensions.tempdir.TempDir
import net.thucydides.core.model.TestResult
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import net.thucydides.core.util.MockEnvironmentVariables
import net.thucydides.cucumber.integration.SimpleSeleniumFailingAndPassingScenario
import net.thucydides.cucumber.integration.SimpleSeleniumFailingScenario
import net.thucydides.cucumber.integration.SimpleSeleniumPageObjects
import net.thucydides.cucumber.integration.SimpleSeleniumScenario
import spock.lang.Specification

import static net.thucydides.cucumber.util.CucumberRunner.thucydidesRunnerForCucumberTestRunner

public class WhenRunningWebCucumberStories extends Specification {

    @TempDir
    File outputDirectory

    /*@Before
    public void reset  driver() {
        environmentVariables.setProperty("webdriver.driver", "phantomjs");
    } */

   /* @Test
    public void a  test  should  have  storywide  tags  defined  by  the  tag  meta  field() throws Throwable {

        // Given
        ThucydidesJUnitStories story = newStory("aPassingBehaviorWithSelenium.story");

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
    }*/

    def environmentVariables = new MockEnvironmentVariables()

    def setup() {
        environmentVariables.setProperty("webdriver.driver", "phantomjs");
    }

    def "should run table-driven scenarios successfully"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleSeleniumScenario.class, outputDirectory, environmentVariables);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.title == "A scenario that uses selenium"

        and: "there should be one step for each row in the table"
        testOutcome.stepCount == 2
    }


    def "a failing story should generate failure test outcome"() throws Throwable {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleSeleniumFailingScenario.class, outputDirectory, environmentVariables);

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



    /*def "a  test  should  use  a  different  browser  if  requested"()  {

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


   def "a  cucumber  step  library  can  use  page  objects  directly"()  {

        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleSeleniumPageObjects.class, outputDirectory, environmentVariables);

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


    def "stories with errors in one scenario should still run subsequent scenarios"()  {

        given:
        environmentVariables.setProperty("restart.browser.each.scenario","true");
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleSeleniumFailingAndPassingScenario, outputDirectory, environmentVariables);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);

        then:
        recordedTestOutcomes.size() == 2
        recordedTestOutcomes[0].result == TestResult.FAILURE
        recordedTestOutcomes[1].result == TestResult.SUCCESS

    }

    /*@Test
    public void should be  able  to  specify  the  browser  in  the  base  test() throws Throwable {

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
    public void should  be  able  to  set  thucydides  properties  in  the  base  test() throws Throwable {

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
    public void data  driven  steps  should  appear  as  nested  steps() throws Throwable {

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
    public void browser  should  not  closed  between  given  stories  and  scenario  steps() throws Throwable {

        // Given
        ThucydidesJUnitStories story = newStory("aBehaviorWithGivenStories.story");

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
    }

    @Test
    public void two  scenarii  using  the  same  given  story  should  return  two  test  outcomes() throws Throwable {
        ThucydidesJUnitStories story = newStory("LookupADefinitionSuite.story");
        run(story);
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(2));
    } */
}
