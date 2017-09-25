package net.serenitybdd.cucumber.smoketests;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.core.pages.WebElementFacade;
import net.thucydides.core.annotations.DefaultUrl;
import net.thucydides.core.annotations.Step;
import net.thucydides.core.annotations.Steps;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.support.FindBy;

public class SkippingScenariosStepDefinitions {
    @DefaultUrl("https://duckduckgo.com")
    public static class DuckDuckGoSearchPage extends PageObject {

        @FindBy(id="search_form_input_homepage")
        WebElementFacade searchField;

        @FindBy(id="search_button_homepage")
        WebElementFacade searchButton;

        public void enterSearchTerm(String searchTerm) {
            searchField.type(searchTerm);
        }

        public void requestSearch() {
            searchButton.click();
        }
    }

    public static class CuriousSurfer {

        DuckDuckGoSearchPage searchPage;

        @Step
        public void opensTheSearchApp() {
            searchPage.open();
        }

        @Step
        public void searchesFor(String searchTerm) {
            searchPage.enterSearchTerm(searchTerm);
            searchPage.requestSearch();
        }

        @Step
        public void shouldSeeTitle(String title) {
            Assertions.assertThat(searchPage.getTitle()).contains(title);
        }
    }

    @Steps
    CuriousSurfer connor;

    @Given("I want to search for something")
    public void givenIWantToSearchForFruit() {
        connor.opensTheSearchApp();
    }

    @When("I lookup (.*)")
    public void whenILookup(String searchTerm) {
        connor.searchesFor(searchTerm);
    }

    @Then("I should see \"(.*)\" in the page title")
    public void thenIShouldSeeTitle(String title) {
        connor.shouldSeeTitle(title);
    }

}
