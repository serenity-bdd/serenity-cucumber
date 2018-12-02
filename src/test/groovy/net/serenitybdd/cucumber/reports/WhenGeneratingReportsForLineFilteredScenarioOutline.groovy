package net.serenitybdd.cucumber.reports

import cucumber.api.Result
import net.serenitybdd.cucumber.integration.SimpleTableScenarioWithLineFilters
import net.serenitybdd.cucumber.util.CucumberRunner
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class WhenGeneratingReportsForLineFilteredScenarioOutline extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    File outputDirectory

    def setup() {
        outputDirectory = temporaryFolder.newFolder()
    }

    def "should generate a Thucydides report for each executed Cucumber scenario"() {
        given:
        def runtime = CucumberRunner.serenityRunnerForCucumberTestRunner(SimpleTableScenarioWithLineFilters.class, outputDirectory)

        when:
        runtime.run()
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:
        runtime.exitStatus.results[0].is(Result.Type.PASSED)

        and:
        !recordedTestOutcomes.isEmpty()
    }


}