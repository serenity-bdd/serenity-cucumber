package smoketests;

import cucumber.api.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

/**
 * Created by Ramanathan Raghunathan on 18/12/2014.
 */
@RunWith(CucumberWithSerenity.class)
@CucumberOptions(features="src/test/resources/features",
glue= {"smoketests.stepdefinitions"},tags = {"@tag_test"})
public class WhenRootFolderOfFeaturesAloneProvided {}
