package net.serenitybdd.cucumber.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;


@RunWith(Cucumber.class)
@CucumberOptions(features="src/test/resources/samples/calculator/basic_arithmetic_more_issues.feature")
public class FeatureWithMoreIssuesTag {}
