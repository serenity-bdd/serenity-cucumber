package net.thucydides.cucumber.integration;

import cucumber.api.CucumberOptions;
import net.thucydides.cucumber.CucumberWithThucydides;
import org.junit.runner.RunWith;


@RunWith(CucumberWithThucydides.class)
@CucumberOptions(features="src/test/resources/samples/web/aPassingBehaviorWithSelenium.feature")
public class SimpleSeleniumScenario {}
