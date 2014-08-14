package net.thucydides.cucumber.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * Created by john on 23/07/2014.
 */
@RunWith(Cucumber.class)
@CucumberOptions(features="src/test/resources/samples/calculator/basic_arithmetic.feature")
public class BasicArithemticScenario {}
