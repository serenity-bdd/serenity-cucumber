package cucumber.runtime.junit;

import cucumber.runtime.Runtime;
import cucumber.runtime.model.CucumberExamples;
import cucumber.runtime.model.CucumberScenarioOutline;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;

public class SerenityScenarioOutlineRunner extends Suite {

    public SerenityScenarioOutlineRunner(
            Runtime runtimeValue,
            CucumberScenarioOutline cucumberScenarioOutlineValue,
            JUnitReporter jUnitReporterValue,
            int retryCount) throws InitializationError {
        super(null, buildRunners(runtimeValue, cucumberScenarioOutlineValue, jUnitReporterValue, retryCount));
    }

    private static List<Runner> buildRunners(
            Runtime runtime,
            CucumberScenarioOutline cucumberScenarioOutline,
            JUnitReporter jUnitReporter,
            int retryCount) throws InitializationError {
        List<Runner> runners = new ArrayList<Runner>();
        for (CucumberExamples cucumberExamples : cucumberScenarioOutline.getCucumberExamplesList()) {
            runners.add(new SerenityExamplesRunner(runtime, cucumberExamples, jUnitReporter, retryCount));
        }
        return runners;
    }
}
