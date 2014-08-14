package net.thucydides.cucumber.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * Created by lcarausu on 09.08.14.
 */
@RunWith(Cucumber.class)
@CucumberOptions(features="src/test/resources/samples/web/aPassingBehaviorWithSeleniumAndFirefox.feature")
public class SimpleSeleniumTestWithASpecifiedBrowser {
}
