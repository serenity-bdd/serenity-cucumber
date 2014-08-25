package net.thucydides.cucumber.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import net.thucydides.cucumber.CucumberWithThucydides;
import org.junit.runner.RunWith;

/**
 * Created by lcarausu on 09.08.14.
 */
@RunWith(CucumberWithThucydides.class)
@CucumberOptions(features="src/test/resources/samples/web/aPassingBehaviorWithSeleniumAndFirefox.feature")
public class SimpleSeleniumTestWithASpecifiedBrowser {
}
