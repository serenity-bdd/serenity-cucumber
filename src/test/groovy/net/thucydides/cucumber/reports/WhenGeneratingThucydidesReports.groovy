package net.thucydides.cucumber.reports

import net.thucydides.cucumber.integration.samples.SimpleScenarioFeature
import net.thucydides.cucumber.util.CucumberRunner
import spock.lang.Specification

/**
 * Created by john on 23/07/2014.
 */
class WhenGeneratingThucydidesReports extends Specification {

    def "should generate a Thucydides report for each executed Cucumber scenario"() {
        when:
            CucumberRunner.run(SimpleScenarioFeature)
        and:
            def outputDirContents = new File("target/site/thucydides").list()
            def htmlReports = outputDirContents.findAll {file -> file.endsWith(".html")}
            def jsonReports = outputDirContents.findAll {file -> file.endsWith(".json")}
        then:
            !htmlReports.isEmpty()
        and:
            !jsonReports.isEmpty()
    }
}