package smoketests.stepdefinitions;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.thucydides.core.annotations.Step;
import net.thucydides.core.annotations.Steps;

import static org.assertj.core.api.Assertions.assertThat;

public class LifecycleStepDefinitions {

    static class Calculations {

        int total = 0;

        @Step
        public void add(int amount) {
            total += amount;
        }

        @Step
        public void substract(int amount) {
            total -= amount;
        }

        public int getTotal() { return total; }

    }

    @Steps
    Calculations calculations;

    @Given("I have a calculator")
    public void givenIHaveACalculator() {
    }

    @Given("I add (.*)")
    public void givenIAdd(int amount) {
        calculations.add(amount);
    }

    @When("I substract (.*)")
    public void whenISubstract(int amount) {
        calculations.substract(amount);
    }

    @Then("the total should be (.*)")
    public void thenTheTotalShouldBe(int total) {
        assertThat(calculations.total).isEqualTo(total);
    }

    @Then("the total should not be zero")
    public void thenTheTotalShouldNotBeZero() {
        assertThat(calculations.total).isNotZero();
    }

}
