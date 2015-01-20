package net.serenitybdd.cucumber.util;

import gherkin.formatter.model.Result;
import net.thucydides.core.model.FailureCause;
import net.thucydides.core.model.TestStep;

public class SerenityResultOverride {
    public static Result override(Result cucumberResult, TestStep testStep) {
        switch (testStep.getResult()) {
            case ERROR:
            case FAILURE:
                return failingCucumberResult(cucumberResult, testStep.getException());
            case PENDING:
            case IGNORED:
            case SKIPPED:
                return Result.SKIPPED;
            case UNDEFINED:
                return Result.UNDEFINED;

        }
        return cucumberResult;
    }

    private static Result failingCucumberResult(Result cucumberResult, FailureCause exception) {
        return new Result(Result.FAILED, cucumberResult.getDuration(), exception.toException(), null);
    }
}
