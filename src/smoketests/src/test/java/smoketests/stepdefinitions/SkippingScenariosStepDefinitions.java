package smoketests.stepdefinitions;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.core.pages.WebElementFacade;
import net.thucydides.core.annotations.DefaultUrl;
import net.thucydides.core.annotations.Step;
import net.thucydides.core.annotations.Steps;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.steps.StepEventBus;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SkippingScenariosStepDefinitions {

    @DefaultUrl("https://duckduckgo.com")
    public static class DuckDuckGoSearchPage extends PageObject {

        @FindBy(id="search_form_input_homepage")
        WebElementFacade searchField;

        @FindBy(id="search_button_homepage")
        WebElementFacade searchButton;

        @FindBy(className = "result__title")
        List<WebElementFacade> results;

        public void enterSearchTerm(String searchTerm) {
            searchField.type(searchTerm);
        }

        public void requestSearch() {
            searchButton.click();
        }

        public List<String> getResults() {
            return results.stream().map(element -> element.getAttribute("textContent")).collect(Collectors.toList());
        }
    }

    @DefaultUrl("https://html5demos.com/drag/")
    public static class HTMLDragAndDrop extends PageObject {

        public void tryDragAndDrop() {
            withAction().dragAndDrop($("#one"), $("#bin")).perform();
            StepEventBus.getEventBus().suspendTest(TestResult.SUCCESS);
            StepEventBus.getEventBus().temporarilySuspendWebdriverCalls();
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
            assertThat(searchPage.getTitle()).contains(title);
        }

        @Step
        public void shouldSeeAListOfResults() {
            assertThat(searchPage.getResults().size()).isGreaterThan(0);
        }
    }

    @Steps
    CuriousSurfer connor;

    HTMLDragAndDrop dragAndDrop;

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

    @After("@web")
    public void checkBrowserAfterTheTest() {
        connor.shouldSeeAListOfResults();
    }

    @Then("I should see search results")
    public void thenIShouldSeeSearchResults() {
        connor.shouldSeeAListOfResults();
    }

    @Then("subsequent steps should be ignored")
    public void ignoreSubsequentSteps() {}

    @Then("steps should be ignored")
    public void ignoreSteps() {}


    @Given("I want to indicate that a scenario should be performed manually")
    public void manual() {}

    @Given("I also want it appearing in the skipped scenarios")
    public void appear_as_skipped() {}

    @When("I tag it as (.*)")
    public void tag_as(String tags) {}

    @Then("it should be reported as (.*)")
    public void reported_as(String states) {}
}
