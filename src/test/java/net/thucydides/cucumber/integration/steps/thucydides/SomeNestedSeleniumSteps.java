package net.thucydides.cucumber.integration.steps.thucydides;

import net.thucydides.core.annotations.Managed;
import net.thucydides.core.annotations.ManagedPages;
import net.thucydides.core.annotations.Step;
import net.thucydides.core.pages.Pages;
import org.openqa.selenium.WebDriver;

public class SomeNestedSeleniumSteps {

    StaticSitePage page;

    @Step
    public void enters_the_first_name(String firstname) {
        page.setFirstName(firstname);
    }

    @Step
    public void enters_the_last_name(String lastname) {
        page.setLastName(lastname);
    }
}
