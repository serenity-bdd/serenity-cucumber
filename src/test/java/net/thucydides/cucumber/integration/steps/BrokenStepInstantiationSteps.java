package net.thucydides.cucumber.integration.steps;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import net.thucydides.core.annotations.Steps;
import net.thucydides.cucumber.integration.steps.thucydides.SampleSteps;

import static org.fest.assertions.Assertions.assertThat;

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
