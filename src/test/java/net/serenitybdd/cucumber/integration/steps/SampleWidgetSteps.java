package net.serenitybdd.cucumber.integration.steps;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import javassist.tools.reflect.Sample;
import net.serenitybdd.cucumber.integration.steps.thucydides.WidgetSteps;
import net.thucydides.core.annotations.Steps;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by john on 23/07/2014.
 */
public class SampleWidgetSteps {

    private int quantity;
    private int cost;
    private int billedPrice;

    @Steps
    WidgetSteps widgetSteps;

    @Given("I have \\$(\\d+)")
    public void iHaveMoney(int money) {

    }

    @Given("I want to purchase (\\d+) widgets")
    public void wantToPurchaseWidgets(int quantity) {
        this.quantity = quantity;
    }

    @Given("a widget costs \\$(\\d+)")
    public void widgetsCost(int cost) {
        this.cost = cost;
    }

    @When("I buy the widgets")
    public void buyWidgets() {
        billedPrice = cost * quantity;
    }

    @Then("I should be billed \\$(\\d+)")
    public void shouldBeBilled(int totalPrice) {
        widgetSteps.shouldBeBilled(billedPrice, totalPrice);

    }
}
