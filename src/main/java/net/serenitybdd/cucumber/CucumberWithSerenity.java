package net.serenitybdd.cucumber;

import cucumber.api.junit.Cucumber;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.webdriver.Configuration;
import org.junit.runners.model.InitializationError;

import java.io.IOException;

/**
 * Glue code for running Cucumber via Thucydides.
 * Sets the Thucydides reporter.
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class CucumberWithSerenity extends Cucumber {


    public CucumberWithSerenity(Class clazz) throws InitializationError, IOException
    {
        super(clazz);
    }

    /**
     * Create the Runtime. Sets the Thucydides runtime.
     */
    protected cucumber.runtime.Runtime createRuntime(ResourceLoader resourceLoader, ClassLoader classLoader,
                                                     RuntimeOptions runtimeOptions) throws InitializationError, IOException {
        return createThucydidesEnabledRuntime(resourceLoader, classLoader, runtimeOptions);
    }

    private Runtime createThucydidesEnabledRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, RuntimeOptions runtimeOptions) {
        Configuration systemConfiguration = Injectors.getInjector().getInstance(Configuration.class);
        return createThucydidesEnabledRuntime(resourceLoader, classLoader, runtimeOptions, systemConfiguration);
    }

    public static Runtime createThucydidesEnabledRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, RuntimeOptions runtimeOptions, Configuration systemConfiguration) {
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        SerenityReporter reporter = new SerenityReporter(systemConfiguration);
        runtimeOptions.addFormatter(reporter);
        return new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }
}
