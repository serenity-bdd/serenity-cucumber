package cucumber.runtime.junit;

import cucumber.runtime.CucumberException;
import cucumber.runtime.Runtime;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberScenario;
import cucumber.runtime.model.CucumberScenarioOutline;
import cucumber.runtime.model.CucumberTagStatement;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;

public class SerenityFeatureRunner extends FeatureRunner {
    private final List<ParentRunner> children = new ArrayList<ParentRunner>();

    private int maxRetryCount = 0;
    private int retries = 0;
    private int scenarioCount = 0;
    private Runtime runtime;
    private CucumberFeature cucumberFeature;
    private JUnitReporter jUnitReporter;

    public SerenityFeatureRunner(
            CucumberFeature cucumberFeature,
            Runtime runtime,
            JUnitReporter jUnitReporter,
            int maxRetryCount)
            throws InitializationError {
        super(cucumberFeature, runtime, jUnitReporter);
        this.cucumberFeature = cucumberFeature;
        this.runtime = runtime;
        this.jUnitReporter = jUnitReporter;
        this.maxRetryCount = maxRetryCount;
        buildFeatureElementRunners();
    }

    private void buildFeatureElementRunners() {
        for (CucumberTagStatement cucumberTagStatement : cucumberFeature.getFeatureElements()) {
            try {
                ParentRunner featureElementRunner;
                if (cucumberTagStatement instanceof CucumberScenario) {
                    featureElementRunner = new ExecutionUnitRunner(
                            runtime, (CucumberScenario) cucumberTagStatement, jUnitReporter);
                } else {
                    featureElementRunner = new SerenityScenarioOutlineRunner(
                            runtime, (CucumberScenarioOutline) cucumberTagStatement, jUnitReporter, maxRetryCount);
                }
                children.add(featureElementRunner);
            } catch (InitializationError e) {
                throw new CucumberException("Failed to create scenario runner", e);
            }
        }
    }

    @Override
    protected void runChild(ParentRunner child, RunNotifier notifier) {
        child.run(notifier);
        if(runtime.exitStatus() != 0) {
            retry(notifier, child);
        }
        this.setScenarioCount(this.getScenarioCount() + 1);
        retries = 0;
    }

    private CucumberScenario getCurrentScenario() {
        CucumberTagStatement cucumberTagStatement
                = this.cucumberFeature.getFeatureElements().get(this.getScenarioCount());
        if (cucumberTagStatement instanceof CucumberScenarioOutline) {
            return null;
        }
        return (CucumberScenario) cucumberTagStatement;
    }

    public void retry(RunNotifier notifier, ParentRunner child) {
        ParentRunner featureElementRunner = null;
        CucumberScenario scenario = getCurrentScenario();
        if (scenario == null) {
            return;
        }
        while (retries < maxRetryCount) {
            try {
                featureElementRunner = new ExecutionUnitRunner(runtime, scenario, jUnitReporter);
            } catch (InitializationError e) {
                throw new CucumberException("Failed to create scenario runner", e);
            }
            try {
                featureElementRunner.run(notifier);
            } catch(Throwable thr) {
                thr.printStackTrace();
            }
            if(runtime.exitStatus() == 0) {
                break;
            } else {
                retries = retries + 1;
                runtime.getErrors().clear();
            }
        }
    }

    @Override
    protected List<ParentRunner> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(ParentRunner child) {
        return child.getDescription();
    }

    public int getScenarioCount() {
        return scenarioCount;
    }

    public void setScenarioCount(int scenarioCountValue) {
        this.scenarioCount = scenarioCountValue;
    }
}
