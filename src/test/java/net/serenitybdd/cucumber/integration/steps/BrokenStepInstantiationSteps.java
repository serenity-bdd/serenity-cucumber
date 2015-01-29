package net.serenitybdd.cucumber.integration.steps;

import cucumber.api.java.en.Given;

import static org.assertj.core.api.Assertions.assertThat;;

/**
 * Created by john on 15/07/2014.
 */
public class BrokenStepInstantiationSteps {

    public BrokenStepInstantiationSteps() throws InstantiationException {
        throw new InstantiationException("Oh crap!");
    }

    @Given("I have a step library that fails to instantiate")
    public void featureFileContainsStepsFields() {
    }

}
