package net.serenitybdd.cucumber.reports

import net.serenitybdd.cucumber.integration.SimpleTableScenarioWithLineFilters
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static net.serenitybdd.cucumber.util.CucumberRunner.serenityRunnerForCucumberTestRunner

class WhenGeneratingReportsForLineFilteredScenarioOutline extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    File outputDirectory

    def setup() {
        outputDirectory = temporaryFolder.newFolder()
    }

    def "should generate a Thucydides report for each executed Cucumber scenario"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleTableScenarioWithLineFilters.class, outputDirectory)

        when:
        runtime.run()
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:
        runtime.getErrors().isEmpty()

        and:
        !recordedTestOutcomes.isEmpty()
    }


}