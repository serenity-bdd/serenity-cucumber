package net.serenitybdd.cucumber.integration.steps;

import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class SimpleCalculatorSteps {

    @Given("^the number ([0-9]*) and the number ([0-9]*)$")
    public void theNumberAAndTheNumberB(Integer a, Integer b) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @When("^([0-9]*) plus ([0-9]*)$")
    public void aPlusB(Integer a, Integer b) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("^the result is equals to ([0-9]*)$")
    public void theResultIsEqualsToC(Integer b) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

}