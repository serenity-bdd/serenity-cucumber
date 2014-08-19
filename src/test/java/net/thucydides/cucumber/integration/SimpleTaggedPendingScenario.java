package net.thucydides.cucumber.integration;

import cucumber.api.CucumberOptions;
import net.thucydides.cucumber.CucumberWithThucydides;
import org.junit.runner.RunWith;


@RunWith(CucumberWithThucydides.class)
@CucumberOptions(features="src/test/resources/samples/simple_tagged_pending_scenario.feature")
public class SimpleTaggedPendingScenario {}
