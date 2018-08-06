package net.serenitybdd.cucumber.formatting;

import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import gherkin.ast.TableCell;
import gherkin.ast.TableRow;

import java.util.stream.Collectors;

public class ScenarioOutlineDescription {
    private final ScenarioDefinition scenario;

    public ScenarioOutlineDescription(ScenarioDefinition scenario) {
        this.scenario = scenario;
    }

    public static ScenarioOutlineDescription from(ScenarioDefinition scenario) {
        return new ScenarioOutlineDescription(scenario);
    }

    public String getDescription() {
        return scenario.getSteps().stream().map(
                step -> stepToString(step)
        ).collect(Collectors.joining(System.lineSeparator()));
    }

    private String stepToString(Step step) {
        String phrase = step.getKeyword() + step.getText();

        if ((step.getArgument() != null) && (step.getArgument().getClass().isAssignableFrom(gherkin.ast.DataTable.class))) {
            gherkin.ast.DataTable table = (gherkin.ast.DataTable) step.getArgument();
            String tableAsString = "";
            for (TableRow row : table.getRows()) {
                tableAsString += "|";
                tableAsString += row.getCells().stream()
                        .map(TableCell::getValue)
                        .collect(Collectors.joining(" | "));
                tableAsString += "|" + System.lineSeparator();
            }

            phrase = phrase + System.lineSeparator() + tableAsString.trim();
        }

        return phrase;
    }
}
