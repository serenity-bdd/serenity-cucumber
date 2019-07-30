package net.serenitybdd.cucumber.service;


import net.thucydides.core.steps.service.CleanupMethodAnnotationProvider;

import java.util.ArrayList;
import java.util.List;

public class CucumberCleanupMethodAnnotationProvider implements CleanupMethodAnnotationProvider {

    @Override
    public List<String> getCleanupMethodAnnotations() {
        List<String> cleanupAnnotationTags = new ArrayList<>();
        cleanupAnnotationTags.add("@cucumber.api.java.After()");
        cleanupAnnotationTags.add("@cucumber.api.java.AfterStep()");
        return cleanupAnnotationTags;
    }
}
