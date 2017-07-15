package net.serenitybdd.cucumber.reports

import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import net.serenitybdd.cucumber.integration.SimpleScenario
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static net.serenitybdd.cucumber.util.CucumberRunner.serenityRunnerForCucumberTestRunner

/**
 * Created by john on 23/07/2014.
 */
class WhenGeneratingThucydidesReports extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    File outputDirectory

    def setup() {
        outputDirectory = temporaryFolder.newFolder()
    }

    def "should generate a Thucydides report for each executed Cucumber scenario"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);

        then:
        runtime.getErrors().isEmpty()

        and:
        !recordedTestOutcomes.isEmpty()
    }


}