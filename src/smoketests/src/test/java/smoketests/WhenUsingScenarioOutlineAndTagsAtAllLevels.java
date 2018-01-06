package smoketests;

import cucumber.api.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

/**
 * Created by Ramanathan Raghunathan on 18/12/2014.
 */
@RunWith(CucumberWithSerenity.class)
@CucumberOptions(features="src/test/resources/features/tags_and_report/serenity_report_when_using_tags_at_all_level.feature",
glue= {"smoketests.stepdefinitions"},tags = {"(@tag_test or @doing_maths) and @single_red"})
public class WhenUsingScenarioOutlineAndTagsAtAllLevels {}
