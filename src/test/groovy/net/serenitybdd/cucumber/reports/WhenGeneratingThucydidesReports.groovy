package net.serenitybdd.cucumber.reports

import com.github.goldin.spock.extensions.tempdir.TempDir
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import net.serenitybdd.cucumber.integration.SimpleScenario
import spock.lang.Specification

import static net.serenitybdd.cucumber.util.CucumberRunner.serenityRunnerForCucumberTestRunner

/**
 * Created by john on 23/07/2014.
 */
class WhenGeneratingThucydidesReports extends Specification {

    @TempDir
    File outputDirectory

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