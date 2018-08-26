package smoketests;

import cucumber.api.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(glue = "smoketests.stepdefinitions", features = "classpath:features")
public class SlicedTestRunner {

    /*

    Experimental test runner where parameters can changed in order to run specific portions of the test suite. For instance create a run configuration and paste the following into the VM Options:

    -Dserenity.batch.count=3 -Dserenity.batch.number=2 -Dserenity.fork.number=1 -Dserenity.fork.count=2 -Dserenity.test.statistics.dir=/statistics

    And you should see the following logged in the console:

    16:49:18.551 [main] INFO  n.s.cucumber.CucumberWithSerenity - Running slice 2 of 3 using fork 1 of 2 from feature paths [classpath:smoketests]

    The Test output should show some features selected and some scenarios run and some not run. This is expected!

    */

    /*

    Alternatively, the tests can be run in maven with or without test forking as follows:

    clean verify -Dserenity.batch.count=1 -Dserenity.batch.number=1 -P dontUseTheForks
    clean verify -Dserenity.batch.count=1 -Dserenity.batch.number=1 -Dserenity.test.statistics.dir=/statistics -P useTheForks

    */

}
