package net.thucydides.cucumber.reports

import com.github.goldin.spock.extensions.tempdir.TempDir
import net.thucydides.core.model.TestResult
import net.thucydides.core.model.TestStep
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import net.thucydides.cucumber.integration.FailingScenario
import net.thucydides.cucumber.integration.SimpleScenario
import spock.lang.Specification

import static net.thucydides.cucumber.util.CucumberRunner.thucydidesRunnerForCucumberTestRunner

/**
 * Created by john on 23/07/2014.
 */
class WhenGeneratingThucydidesReports extends Specification {

    @TempDir
    File outputDirectory

    def "should generate a Thucydides report for each executed Cucumber scenario"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(SimpleScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);

        then:
        runtime.getErrors().isEmpty()

        and:
        !recordedTestOutcomes.isEmpty()
    }

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
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]
        List<TestStep> stepResults = testOutcome.testSteps.collect { step -> step.result }

        then:
        testOutcome.result == TestResult.FAILURE
        and:
        stepResults == [TestResult.SUCCESS,TestResult.SUCCESS,TestResult.SUCCESS,TestResult.FAILURE]
        and:
        stepResults[3].errorMessage.contains("expected:<[2]0> but was:<[1]0>")
    }
}