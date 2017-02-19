package cucumber.runtime.junit;

import cucumber.runtime.Runtime;
import cucumber.runtime.model.CucumberExamples;
import cucumber.runtime.model.CucumberScenario;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;

public class SerenityExamplesRunner extends Suite {

    private int retryCount;
    private Runtime runtime;

    protected SerenityExamplesRunner(
            Runtime runtime,
            CucumberExamples cucumberExamplesValue,
            JUnitReporter jUnitReporter,
            int retryCount) throws InitializationError {
        super(SerenityExamplesRunner.class,
                buildRunners(runtime, cucumberExamplesValue, jUnitReporter));
        this.runtime = runtime;
        this.retryCount = retryCount;
    }

    private static List<Runner> buildRunners(
            Runtime runtime,
            CucumberExamples cucumberExamples,
            JUnitReporter jUnitReporter) {
        List<Runner> runners = new ArrayList<>();
        List<CucumberScenario> exampleScenarios = cucumberExamples.createExampleScenarios();
        for (CucumberScenario scenario : exampleScenarios) {
            try {
                ExecutionUnitRunner exampleScenarioRunner
                        = new ExecutionUnitRunner(runtime, scenario, jUnitReporter);
                runners.add(exampleScenarioRunner);
            } catch (InitializationError initializationError) {
                initializationError.printStackTrace();
            }
        }
        return runners;
    }

    @Override
    protected void runChild(Runner runner, RunNotifier notifier) {
        for (int i = 0; i <= this.retryCount; i++) {
            runner.run(notifier);
            if(runtime.exitStatus() == 0) {
                break;
            } else {
                runtime.getErrors().clear();
            }
        }
    }
}
