package net.serenitybdd.cucumber.integration;

import cucumber.api.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;


@RunWith(CucumberWithSerenity.class)
@CucumberOptions(features="src/test/resources/samples/simple_tagged_pending_feature.feature")
public class SimpleTaggedPendingFeature {}
