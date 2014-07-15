package net.thucydides.cucumber.e2e.steps;

import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class SimpleExampleSteps {

    public SimpleExampleSteps() {
    }

    @Given("^I have a Cucumber feature file$")
    public void I_have_a_Cucumber_feature_file() throws Throwable {

    }

    @Then("^I should obtain a Thucydides report$")
    public void I_should_obtain_a_Thucydides_report() throws Throwable {
        throw new PendingException();
    }

}
