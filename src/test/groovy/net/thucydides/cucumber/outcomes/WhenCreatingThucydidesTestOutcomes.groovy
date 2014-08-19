package net.thucydides.cucumber.outcomes

import com.github.goldin.spock.extensions.tempdir.TempDir
import net.thucydides.core.model.TestOutcome
import net.thucydides.core.model.TestResult
import net.thucydides.core.model.TestStep
import net.thucydides.core.model.TestTag
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import net.thucydides.cucumber.integration.BasicArithemticScenario
import net.thucydides.cucumber.integration.FailingScenario
import net.thucydides.cucumber.integration.MultipleScenarios
import net.thucydides.cucumber.integration.PendingScenario
import net.thucydides.cucumber.integration.SimpleScenario
import net.thucydides.cucumber.integration.SimpleScenarioWithNarrativeTexts
import net.thucydides.cucumber.integration.SimpleScenarioWithTags
import net.thucydides.cucumber.integration.SimpleTaggedPendingFeature
import net.thucydides.cucumber.integration.SimpleTaggedPendingScenario
import spock.lang.Specification

import static net.thucydides.cucumber.util.CucumberRunner.thucydidesRunnerForCucumberTestRunner

/**
 * Created by john on 23/07/2014.
 */
class WhenCreatingThucydidesTestOutcomes extends Specification {

    @TempDir
    File outputDirectory

    /*
    Feature: A simple feature

      Scenario: A simple scenario
        Given I want to purchase 2 widgets
        And a widget costs $5
        When I buy the widgets
        Then I should be billed $10
     */
    def "should generate a well-structured Thucydides test outcome for each executed Cucumber scenario"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]
        def steps = testOutcome.testSteps.collect { step -> step.description }

        then:
        testOutcome.title == "A simple scenario"

        and:
        testOutcome.stepCount == 4
        steps == ['Given I want to purchase 2 widgets', 'And a widget costs $5', 'When I buy the widgets', 'Then I should be billed $10']
    }

    def "should record results for each step"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]
        def stepResults = testOutcome.testSteps.collect { step -> step.result }

        then:
        testOutcome.result == TestResult.SUCCESS

        and:
        stepResults == [TestResult.SUCCESS,TestResult.SUCCESS,TestResult.SUCCESS,TestResult.SUCCESS]
    }

    def "should record failures for a failing scenario"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(FailingScenario.class, outputDirectory);

        when:
        runtime.run();
        List<TestOutcome>  recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        TestOutcome testOutcome = recordedTestOutcomes[0]
        List<TestStep> stepResults = testOutcome.testSteps.collect { step -> step.result }

        then:
        testOutcome.result == TestResult.FAILURE
        and:
        stepResults == [TestResult.SUCCESS,TestResult.SUCCESS,TestResult.SUCCESS,TestResult.FAILURE]
        and:
        testOutcome.testSteps[3].errorMessage.contains("expected:<[2]0> but was:<[1]0>")
    }

    def "should record a feature tag based on the name of the feature"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.tags.contains(TestTag.withName("A simple feature").andType("feature"))
    }

/*
@flavor:strawberry
Feature: A simple feature with tags
  This is about selling widgets
  @shouldPass
  @color:red
  @in-progress
  ...
 */
    def "should record any provided tags"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleScenarioWithTags.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.tags.size() == 5
        and:
        testOutcome.tags.contains(TestTag.withName("A simple feature with tags").andType("feature"))
        testOutcome.tags.contains(TestTag.withName("strawberry").andType("flavor"))
        testOutcome.tags.contains(TestTag.withName("red").andType("color"))
        testOutcome.tags.contains(TestTag.withName("shouldPass").andType("tag"))
        testOutcome.tags.contains(TestTag.withName("in-progress").andType("tag"))
    }

    def "should record the narrative text"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleScenarioWithNarrativeTexts.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.userStory.narrative == "This is about selling widgets"
    }

    def "should record the scenario description text for a scenario"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleScenarioWithNarrativeTexts.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.description == """A description of this scenario
It goes for two lines"""
    }

    def "should record pending and skipped steps for a pending scenario"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(PendingScenario.class, outputDirectory);

        when:
        runtime.run();
        List<TestOutcome>  recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        TestOutcome testOutcome = recordedTestOutcomes[0]
        List<TestStep> stepResults = testOutcome.testSteps.collect { step -> step.result }

        then:
        testOutcome.result == TestResult.PENDING
        and:
        stepResults == [TestResult.SUCCESS,TestResult.SUCCESS,TestResult.PENDING,TestResult.IGNORED]
    }

    def "should record a @wip scenario as ignored"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleTaggedPendingScenario.class, outputDirectory);

        when:
        runtime.run();
        List<TestOutcome>  recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        TestOutcome testOutcome = recordedTestOutcomes[0]

        then:
        recordedTestOutcomes[0].result == TestResult.IGNORED
        and:
        recordedTestOutcomes[1].result == TestResult.SUCCESS
    }

    def "should record a @wip feature as ignored"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleTaggedPendingFeature.class, outputDirectory);

        when:
        runtime.run();
        List<TestOutcome>  recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        TestOutcome testOutcome = recordedTestOutcomes[0]

        then:
        then:
        recordedTestOutcomes[0].result == TestResult.IGNORED
        and:
        recordedTestOutcomes[1].result == TestResult.IGNORED
    }

    def "should generate a well-structured Thucydides test outcome for feature files with several Cucumber scenario"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(MultipleScenarios.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:

        recordedTestOutcomes.size() == 2

        def testOutcome1 = recordedTestOutcomes[0]
        def steps1 = testOutcome1.testSteps.collect { step -> step.description }

        def testOutcome2 = recordedTestOutcomes[1]
        def steps2 = testOutcome2.testSteps.collect { step -> step.description }

        and:
        testOutcome1.title == "Simple scenario 1"
        testOutcome1.result == TestResult.FAILURE

        and:
        testOutcome2.title == "Simple scenario 2"
        testOutcome2.result == TestResult.SUCCESS

        and:
        steps1 == ['Given I want to purchase 2 widgets', 'And a widget costs $5', 'When I buy the widgets', 'Then I should be billed $50']
        steps2 == ['Given I want to purchase 4 widgets', 'And a widget costs $3', 'When I buy the widgets', 'Then I should be billed $12']
    }

    def "should generate outcomes for scenarios with a background section"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(BasicArithemticScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:
        recordedTestOutcomes.size() == 2

        and:
        recordedTestOutcomes.collect { it.methodName } == ["Addition", "Another Addition"]

        and:
        recordedTestOutcomes[0].stepCount == 3
        recordedTestOutcomes[1].stepCount == 3
    }

}