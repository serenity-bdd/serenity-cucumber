package net.serenitybdd.cucumber.outcomes

import net.serenitybdd.cucumber.integration.BrokenStepLibraryScenario
import net.serenitybdd.cucumber.integration.IllegalStepLibraryScenario
import net.serenitybdd.cucumber.util.CucumberRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created by john on 23/07/2014.
 */
class WhenUsingAnIllegalStepLibrary extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    File outputDirectory

    def setup() {
        outputDirectory = temporaryFolder.newFolder()
    }

    def "should throw a meaningful exception if a step library with no default constructor is used"() {
        given:
        def runtime = CucumberRunner.serenityRunnerForCucumberTestRunner(IllegalStepLibraryScenario.class, outputDirectory);

        when:
        runtime.run();

        then:
        runtime.exitStatus.results

        and:
        runtime.exitStatus.results[0].errorMessage.contains("this class doesn't have an empty or a page enabled constructor")
    }

    def "should throw a meaningful exception if a step library if the step library could not be instantiated"() {
        given:
        def runtime = CucumberRunner.serenityRunnerForCucumberTestRunner(BrokenStepLibraryScenario.class, outputDirectory);

        when:
        runtime.run();

        then:
        runtime.exitStatus.results

        and:
        runtime.exitStatus.results[0].errorMessage.contains("Failed to instantiate class")
    }


}