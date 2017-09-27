package cucumber.runtime;

import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.snippets.FunctionNameGenerator;
import gherkin.formatter.model.Step;
import net.thucydides.core.steps.StepEventBus;

import java.util.List;

public class SerenityBackend implements Backend {

    private final ResourceLoader resourceLoader;

    public SerenityBackend(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void loadGlue(Glue glue, List<String> gluePaths) {

    }

    @Override
    public void setUnreportedStepExecutor(UnreportedStepExecutor executor) {

    }

    @Override
    public void buildWorld() {

    }

    @Override
    public void disposeWorld() {
        StepEventBus.getEventBus().testFinished();
    }

    @Override
    public String getSnippet(Step step, FunctionNameGenerator functionNameGenerator) {
        return "";
    }
}
