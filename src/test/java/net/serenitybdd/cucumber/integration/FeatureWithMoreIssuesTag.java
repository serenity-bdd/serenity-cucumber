package net.serenitybdd.cucumber.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;


@RunWith(Cucumber.class)
@CucumberOptions(features="src/test/resources/samples/multiple_jira_issues.feature")
public class FeatureWithMoreIssuesTag {}
