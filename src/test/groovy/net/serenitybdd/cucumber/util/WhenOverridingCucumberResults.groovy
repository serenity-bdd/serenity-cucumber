package net.serenitybdd.cucumber.util

import gherkin.formatter.model.Result
import net.thucydides.core.model.TestResult
import net.thucydides.core.model.TestStep
import spock.lang.Specification
import spock.lang.Unroll

class WhenOverridingCucumberResults  extends Specification {

    def "A failure in a nested step can override the Cucumber result"() {
        given:
            Result cucumberResult = new Result("passed",0,null)
        and:
            TestStep failingStep = new TestStep();
            failingStep.failedWith(new AssertionError("Oh bother!"))
        when:
            Result overridenResult = SerenityResultOverride.override(cucumberResult, failingStep)
        then:
            overridenResult.status == "failed"
            overridenResult.errorMessage.contains "Oh bother!"
    }

    def "An error in a nested step can override the Cucumber result"() {
        given:
        Result cucumberResult = new Result("passed",0,null)
        and:
        TestStep failingStep = new TestStep();
        failingStep.failedWith(new NullPointerException("Oh bother!"))
        when:
        Result overridenResult = SerenityResultOverride.override(cucumberResult, failingStep)
        then:
        overridenResult.status == "failed"
        overridenResult.errorMessage.contains "Oh bother!"
    }

    @Unroll
    def "A pending in a nested step can override the Cucumber result"() {
        given:
            Result initialCucumberResult = new Result(cucumberResult,0,null)
        and:
            TestStep nestedStep = new TestStep()
            nestedStep.setResult(nestedResult)
        when:
            Result overridenResult = SerenityResultOverride.override(initialCucumberResult, nestedStep)
        then:
            overridenResult.status == expectedCucumberResult
        where:
        cucumberResult | nestedResult       | expectedCucumberResult
        "passed"       | TestResult.PENDING | "skipped"
        "passed"       | TestResult.IGNORED | "skipped"
        "passed"       | TestResult.SKIPPED | "skipped"
    }
}
