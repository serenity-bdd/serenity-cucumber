package net.thucydides.cucumber.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import net.thucydides.cucumber.CucumberWithThucydides;
import org.junit.runner.RunWith;

/**
 * Created by john on 23/07/2014.
 */
@RunWith(CucumberWithThucydides.class)
@CucumberOptions(features="src/test/resources/samples/multiple_scenarios.feature")
public class MultipleScenarios {}
