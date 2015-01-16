package net.serenitybdd.cucumber.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * Created by john on 23/07/2014.
 */
@RunWith(Cucumber.class)
@CucumberOptions(features="src/test/resources/samples/scenario_with_table_in_background_steps.feature")
public class ScenariosWithTableInBackgroundSteps {}
