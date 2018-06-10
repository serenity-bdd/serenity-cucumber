package net.serenitybdd.cucumber.integration.steps;

import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class SimpleCalculatorSteps {

    @Given("the number {int} and the number {int}")
    public void theNumberAAndTheNumberB(Integer a, Integer b) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @When("{int} plus {int}")
    public void aPlusB(Integer a, Integer b) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("the result is equals to {int}")
    public void theResultIsEqualsToC(Integer b) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Given("the amount {int} and the amount {int}")
    public void theAmounts(Integer a, Integer b) throws Throwable {
    }

    @When("{int} minus {int}")
    public void aMinusB(Integer a, Integer b) throws Throwable {
    }

    @Then("the result should be {int}")
    public void theResultShouldBe(Integer b) throws Throwable {
    }
}