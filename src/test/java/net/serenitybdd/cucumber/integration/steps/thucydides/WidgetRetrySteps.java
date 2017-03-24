package net.serenitybdd.cucumber.integration.steps.thucydides;

import net.thucydides.core.annotations.Step;

import static org.assertj.core.api.Assertions.assertThat;


public class WidgetRetrySteps {

    static int testCount = 1;

    @Step
    public void aStepThatFailsOnMultipleOfFourTries() {
        boolean shouldPass = testCount++ % 4 == 0;
        assertThat(shouldPass).isTrue();
    }
}
