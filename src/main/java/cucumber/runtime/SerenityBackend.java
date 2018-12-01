package cucumber.runtime;

import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.snippets.FunctionNameGenerator;
import gherkin.pickles.PickleStep;
import io.cucumber.stepexpression.TypeRegistry;
import net.thucydides.core.steps.StepEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SerenityBackend implements Backend {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerenityBackend.class);

    private final ResourceLoader resourceLoader;
    private final TypeRegistry typeRegistry;

    public SerenityBackend(ResourceLoader resourceLoader, TypeRegistry typeRegistry) {
        this.resourceLoader = resourceLoader;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public void loadGlue(Glue glue, List<String> gluePaths) {

    }

    @Override
    public void buildWorld() {

    }

    @Override
    public void disposeWorld() {
        if (!StepEventBus.getEventBus().isBaseStepListenerRegistered()) {
            LOGGER.warn("It looks like you are running a feature using @RunWith(Cucumber.class) instead of @RunWith(CucumberWithSerenity.class). Are you sure this is what you meant to do?");
        }
    }

    @Override
    public  List<String>  getSnippet(PickleStep step, String keyword, FunctionNameGenerator functionNameGenerator) {
        return new ArrayList<>();
    }

}
