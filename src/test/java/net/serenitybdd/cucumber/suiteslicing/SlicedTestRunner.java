package net.serenitybdd.cucumber.suiteslicing;

import net.serenitybdd.cucumber.CucumberWithSerenity;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(glue = "net.serenitybdd.cucumber.smoketests", features="classpath:smoketests")
public class SlicedTestRunner {

//    @BeforeClass
//    public static void setUp() {
//        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
//        environmentVariables.setProperty(ThucydidesSystemProperty.SERENITY_BATCH_SIZE);
//        ThucydidesSystemProperty.SERENITY_BATCH_SIZE.
//        System.setProperty("BAT_FORK_NUMBER", System.getenv("BAT_ENV_fork_number"));
//        System.setProperty("BAT_TOTAL_FORKS", System.getenv("BAT_ENV_fork_count"));
//        BeforeAll.configureOptions();
//        String bat_fork_number = System.getProperty("BAT_FORK_NUMBER");
//        if ("0".equals(bat_fork_number) && TestEnvironmentConfig.TEST_ENV.isDevOrPreview()) {
//            TimeMachineClient timeMachineClient = new TimeMachineClient();
//            timeMachineClient.resetTime(true);
//            clearDownDb();
//        }
//
//        ensureAgentAccount(bat_fork_number);
//    }

}
