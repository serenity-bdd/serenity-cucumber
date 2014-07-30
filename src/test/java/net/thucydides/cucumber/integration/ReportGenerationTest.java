package net.thucydides.cucumber.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.thucydides.ThucydidesCucumberRunner;
import cucumber.runtime.CucumberException;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.webdriver.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

/**
 *  Test generation of reports
 *
 *  @author L.Carausu (liviu.carausu@gmail.com)
 */
public class ReportGenerationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File outputDirectory = null;

    @Before
    public void initTempFolder() throws IOException
    {
        temporaryFolder.create();
        outputDirectory = temporaryFolder.newFolder("cucumberTest");
    }

    public ReportGenerationTest() {
    }


    @Test
    public void testGenerateThucydidesReport() throws Throwable {
        Class clazz = CucumberTests.class;
        ClassLoader classLoader = clazz.getClassLoader();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz, new Class[]{CucumberOptions.class});
        RuntimeOptions runtimeOptions = runtimeOptionsFactory.create();
        Configuration systemConfiguration = Injectors.getInjector().getInstance(Configuration.class);
        systemConfiguration.setOutputDirectory(outputDirectory);
        cucumber.runtime.Runtime runtime =  ThucydidesCucumberRunner.doCreateRuntime(resourceLoader, classLoader, runtimeOptions,systemConfiguration);
        try {
            runtime.run();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        if (!runtime.getErrors().isEmpty()) {
            throw new CucumberException(runtime.getErrors().get(0));
        } else if (runtime.exitStatus() != 0x00) {
            throw new CucumberException("There are pending or undefined steps.");
        }
        File[] files = outputDirectory.listFiles();
        System.out.println("Outputdirectory " + outputDirectory.getCanonicalPath() + " files " + files.length);
        int xmlCount = 0;
        int jsonCount = 0;
        int htmlCount = 0;
        for(File f : files) {
            System.out.println("File " + f.getAbsolutePath());
            if(f.getAbsolutePath().endsWith(".html")) {
                htmlCount++;
            } else if(f.getAbsolutePath().endsWith(".json")) {
                jsonCount++;
            }else if(f.getAbsolutePath().endsWith(".xml")) {
                xmlCount++;
            }
        }
        assert(xmlCount == 3);
        assert(jsonCount == 3);
        assert(htmlCount == 3);
    }
}
