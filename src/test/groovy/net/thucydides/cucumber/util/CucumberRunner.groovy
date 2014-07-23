package net.thucydides.cucumber.util

import org.junit.runner.Computer
import org.junit.runner.JUnitCore

/**
 * Created by john on 23/07/2014.
 */
class CucumberRunner {

    static void run(Class testClass) {
        def jUnitCore = new JUnitCore()
        jUnitCore.run(new Computer(), testClass)
    }
}
