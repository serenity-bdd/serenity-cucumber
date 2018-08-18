package net.serenitybdd.cucumber.util;

import com.google.common.collect.Iterables;

import java.lang.reflect.Field;

import cucumber.runtime.junit.FeatureRunner;

import static java.util.Arrays.asList;

public class FeatureRunnerExtractors {

    public static String extractFeatureName(FeatureRunner runner) {
        return Iterables.getLast(asList(runner.getDescription().getDisplayName().split(":"))).trim();
    }

    public static String featurePathFor(FeatureRunner featureRunner) {
        try {
            Field field = featureRunner.getDescription().getClass().getDeclaredField("fUniqueId");
            field.setAccessible(true);
            return field.get(featureRunner.getDescription()).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
