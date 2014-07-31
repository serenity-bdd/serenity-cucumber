package net.thucydides.cucumber.outcomes

import com.github.goldin.spock.extensions.tempdir.TempDir
import cucumber.runtime.CucumberException
import net.thucydides.core.model.TestOutcome
import net.thucydides.core.model.TestResult
import net.thucydides.core.model.TestStep
import net.thucydides.core.model.TestTag
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import net.thucydides.cucumber.integration.BrokenStepLibraryScenario
import net.thucydides.cucumber.integration.FailingScenario
import net.thucydides.cucumber.integration.IllegalStepLibraryScenario
import net.thucydides.cucumber.integration.MultipleScenarios
import net.thucydides.cucumber.integration.PendingScenario
import net.thucydides.cucumber.integration.SimpleScenario
import spock.lang.Specification

import static net.thucydides.cucumber.util.CucumberRunner.thucydidesRunnerForCucumberTestRunner

/**
 * Created by john on 23/07/2014.
 */
class WhenUsingAnIllegalStepLibrary extends Specification {

    @TempDir
    File outputDirectory

    def "should throw a meaningful exception if a step library with no default constructor is used"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(IllegalStepLibraryScenario.class, outputDirectory);

        when:
        runtime.run();

        then:
        runtime.errors

        and:
        runtime.errors[0].message.contains("IllegalStepInstantiationSteps doesn't have an empty constructor")
    }

    def "should throw a meaningful exception if a step library if the step library could not be instantiated"() {
        given:
        def runtime = thucydidesRunnerForCucumberTestRunner(BrokenStepLibraryScenario.class, outputDirectory);

        when:
        runtime.run();

        then:
        runtime.errors

        and:
        runtime.errors[0].message.contains("Failed to instantiate class")
    }


}