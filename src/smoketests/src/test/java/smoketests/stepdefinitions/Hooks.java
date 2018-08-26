package smoketests.stepdefinitions;

import cucumber.api.java.Before;
import net.serenitybdd.cucumber.suiteslicing.SerenityTags;

public class Hooks {

    @Before
    public void before() {
        SerenityTags.create().tagScenarioWithBatchingInfo();
    }

}
