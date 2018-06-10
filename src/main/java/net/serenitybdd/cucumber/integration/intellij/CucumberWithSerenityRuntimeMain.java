package net.serenitybdd.cucumber.integration.intellij;

import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.webdriver.Configuration;

import java.io.IOException;

import static java.util.Arrays.asList;

/**
 * A test runner that allows you to run feature files directly from IntelliJ.
 * This avoids having to write specific runners for each feature file.
 * Contributed by Vladimir Ivanov
 * Deprecated: Replaced with cucumber.runtime.cli.Main
 */
@Deprecated
public class CucumberWithSerenityRuntimeMain {
    public static void main(String[] argv) throws Throwable {
        byte exitStatus = run(argv, Thread.currentThread().getContextClassLoader());
        System.exit(exitStatus);
    }

    /**
     * Launches the Cucumber-JVM command line
     * @param argv runtime options. See details in the {@code cucumber.api.cli.Usage.txt} resource
     * @param classLoader classloader used to load the runtime
     * @return 0 if execution was successful, 1 if not (there were test failures)
     * @throws IOException if resources couldn't be loaded during execution
     */
    public static byte run(String[] argv, ClassLoader classLoader) throws IOException {
        RuntimeOptions runtimeOptions = new RuntimeOptions(asList(argv));

        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        Configuration systemConfiguration = Injectors.getInjector().getInstance(Configuration.class);
        Runtime seleniumRuntime = CucumberWithSerenity.createSerenityEnabledRuntime(resourceLoader,
                                                                                    classLoader,
                                                                                    runtimeOptions,
                                                                                    systemConfiguration);

        seleniumRuntime.run();

        return seleniumRuntime.exitStatus();
    }
}
