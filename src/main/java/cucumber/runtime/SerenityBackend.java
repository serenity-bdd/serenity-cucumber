package cucumber.runtime;

import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.snippets.FunctionNameGenerator;
import gherkin.pickles.PickleStep;
import net.thucydides.core.steps.StepEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SerenityBackend implements Backend {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerenityBackend.class);

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
        if (StepEventBus.getEventBus().isBaseStepListenerRegistered()) {
            StepEventBus.getEventBus().testFinished();
        } else {
            LOGGER.warn("It looks like you are running a feature using @RunWith(Cucumber.class) instead of @RunWith(CucumberWithSerenity.class). Are you sure this is what you meant to do?");
        }
    }

    @Override
    public String getSnippet(PickleStep step, String keyword, FunctionNameGenerator functionNameGenerator) {
        return "";
    }

}
