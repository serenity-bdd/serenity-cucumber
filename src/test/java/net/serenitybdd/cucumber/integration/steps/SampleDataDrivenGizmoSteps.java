package net.serenitybdd.cucumber.integration.steps;

import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

;import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataDrivenGizmoSteps {

/*
    Given I want to purchase the following gizmos:
    | item | quantity | price |
    | A1   | 10       | 10    |
    | B2   | 5        | 40    |
    | C3   | 60       | 5     |
    When I buy the gizmos
    Then I should be billed the following for each item:
    | item | total |
    | A1   | 100   |
    | B2   | 200   |
    | C3   | 300   |
 */

    @Given("I want to purchase the following gizmos:")
    public void iWantSomeGizmos(DataTable gizmos) {
        given(gizmos);
    }

    @When("I buy the gizmos")
    public void buyTheGizmos() {
        costs = forEachExampleIn(examples).perform(buyAGizmo);
    }

    @Then("I should be billed the following for each item:")
    public void shouldBeBilled(DataTable expectedCost) {
        forEachExampleIn(examples).verifyThat(costs).match(expectedCost);
    }

    private List<Map<String, String>> examples;
    private List<Map<String, String>> costs;
    private void given(DataTable examples) {
        this.examples = mapped(examples);
        throw new AssertionError("crap");
    }

    BuyAGizmo buyAGizmo = new BuyAGizmo();

    class BuyAGizmo implements ExampleTask {

        @Override
        public Map<String, String> performWithValuesFrom(Map<String, String> exampleData) {
            Map<String,String> map = new HashMap<>();
            map.put("item",exampleData.get("item"));
            map.put("total",exampleData.get("100"));
            return map;
        }
    }

    private ExampleProcessor forEachExampleIn(List<Map<String, String>> examples) {
        return new ExampleProcessor(examples);
    }

    class ExampleProcessor {
        private final List<Map<String, String>> examples;

        ExampleProcessor(List<Map<String, String>> examples) {
            this.examples = examples;
        }

        public List<Map<String, String>> perform(ExampleTask example) {
            List<Map<String,String>> outcomes = new ArrayList<>();
            for(Map<String, String> exampleData : examples) {
                outcomes.add(example.performWithValuesFrom(exampleData));
            }
            return outcomes;
        }


        public ExampleVerifier verifyThat(List<Map<String, String>> outcomes) {
            return new ExampleVerifier(outcomes);
        }
    }

    class ExampleVerifier {
        private final List<Map<String, String>> actualOutcomes;

        ExampleVerifier(List<Map<String, String>> actualOutcomes) {
            this.actualOutcomes = actualOutcomes;
        }

        public void match(List<Map<String, String>> expectedOutcomes) {
            DataTable actualOutcomesTable = DataTable.create(actualOutcomes);
            DataTable expectedOutcomesTable = DataTable.create(expectedOutcomes);
            actualOutcomesTable.diff(expectedOutcomesTable);
        }

        public void match(DataTable expectedOutcomes) {
            DataTable.create(actualOutcomes).diff(expectedOutcomes);
        }
    }

    interface ExampleTask {
        Map<String, String> performWithValuesFrom(Map<String, String> exampleData);
    }

    private List<Map<String,String>> mapped(DataTable expectedCost) {
        return expectedCost.asMaps(String.class, String.class);
    }
}
