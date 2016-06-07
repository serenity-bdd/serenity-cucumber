package net.serenitybdd.cucumber.service;



import net.thucydides.core.statistics.service.TagProvider;
import net.thucydides.core.statistics.service.TagProviderStrategy;
import net.thucydides.core.steps.StepEventBus;

import java.util.ServiceLoader;

public class CucumberTagProviderStrategy implements TagProviderStrategy {

    @Override
    public boolean canHandleTestSource(String testType) {
        return StepEventBus.TEST_SOURCE_CUCUMBER.equals(testType);
    }

    @Override
    public Iterable<TagProvider> getTagProviders() {
        return ServiceLoader.load(TagProvider.class);
    }

}
