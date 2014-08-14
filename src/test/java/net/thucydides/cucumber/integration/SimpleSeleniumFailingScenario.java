package net.thucydides.cucumber.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import net.thucydides.cucumber.CucumberWithThucydides;
import org.junit.runner.RunWith;


@RunWith(CucumberWithThucydides.class)
@CucumberOptions(features="src/test/resources/samples/web/aFailingBehaviorWithSelenium.feature")
public class SimpleSeleniumFailingScenario {}
