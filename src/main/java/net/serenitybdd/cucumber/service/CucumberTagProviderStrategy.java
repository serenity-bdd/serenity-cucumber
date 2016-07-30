package net.serenitybdd.cucumber.service;


import com.beust.jcommander.internal.Lists;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.requirements.FileSystemRequirementsTagProvider;
import net.thucydides.core.statistics.service.TagProvider;
import net.thucydides.core.statistics.service.TagProviderStrategy;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.util.EnvironmentVariables;

public class CucumberTagProviderStrategy implements TagProviderStrategy {

    private final EnvironmentVariables environmentVariables;

    public CucumberTagProviderStrategy(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public CucumberTagProviderStrategy() {
        this(Injectors.getInjector().getInstance(EnvironmentVariables.class));
    }

    @Override
    public boolean canHandleTestSource(String testType) {
        return StepEventBus.TEST_SOURCE_CUCUMBER.equalsIgnoreCase(testType);
    }

    @Override
    public Iterable<TagProvider> getTagProviders() {
        String rootDirectory = ThucydidesSystemProperty.THUCYDIDES_REQUIREMENTS_DIR.from(environmentVariables,"features");
        return Lists.newArrayList((TagProvider) new FileSystemRequirementsTagProvider(environmentVariables,rootDirectory));
    }

}
