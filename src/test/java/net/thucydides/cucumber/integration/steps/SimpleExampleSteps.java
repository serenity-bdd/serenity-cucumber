package net.thucydides.cucumber.integration.steps;


import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.webdriver.Configuration;



import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasItem;

import net.thucydides.core.util.FileSystemUtils;
import org.apache.commons.io.FileUtils;

public class SimpleExampleSteps {


    public SimpleExampleSteps() {
    }

    @Given("^I have a Cucumber feature file$")
    public void I_have_a_Cucumber_feature_file() throws Throwable {

    }

    @Then("^I should obtain a Thucydides report$")
    public void I_should_obtain_a_Thucydides_report() throws Throwable {
        Configuration systemConfiguration = Injectors.getInjector().getInstance(net.thucydides.core.webdriver.Configuration.class);
        File outputDirectory = systemConfiguration.getOutputDirectory();
        File[] files = outputDirectory.listFiles();
        int xmlCount = 0;
        int jsonCount = 0;
        int htmlCount = 0;
        for(File f : files) {
           if(f.getAbsolutePath().endsWith(".html")) {
                htmlCount++;
           } else if(f.getAbsolutePath().endsWith(".json")) {
                jsonCount++;
           }else if(f.getAbsolutePath().endsWith(".xml")) {
                xmlCount++;
           }
        }
        assert(xmlCount > 0);
        assert(jsonCount > 0);
        assert(htmlCount > 0);
        assert(xmlCount == jsonCount);
        assert(jsonCount == htmlCount);
    }

}
