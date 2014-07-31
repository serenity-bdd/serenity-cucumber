package net.thucydides.cucumber.reports

import com.github.goldin.spock.extensions.tempdir.TempDir
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
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


}