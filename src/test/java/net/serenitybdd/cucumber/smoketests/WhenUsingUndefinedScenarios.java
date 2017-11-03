package net.serenitybdd.cucumber.smoketests;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
//@RunWith(Cucumber.class)
@CucumberOptions(features="src/test/resources/smoketests/undefined_scenarios.feature")
public class WhenUsingUndefinedScenarios {}
