## Serenity Cucumber Integration

This module lets you produce Serenity reports using Cucumber. You run your tests as normal, 
but using the *CucumberWithSerenity* runner, e.g.:

```java
@RunWith(CucumberWithSerenity.class)
@CucumberOptions(features="src/test/resources/samples/myfeature.feature")
public class SimpleTableScenario {}
```



## Found a bug? Please read this before you raise an issue.

If you have found a defect, we are keen to hear about it! But there are a few things you can do to help us provide a fix sooner:

### Give as much context as possible.

Simply saying "The reports don't get generated" will not help us very much. Give as much context as possible, including:
  - Serenity version (serenity-core and the other serenity libraries, such as serenity-cucummber and serenity-jbehave)
  - If you are using Firefox, firefox and geckodriver version
  - If you are using Chrome, chrome and chromedriver version
  - What Operating System are you using

Also, make sure you try with the latest version of Serenity - your bug may already be fixed, and in any case error messages from the latest version will be more relevant when we try to track down the source of the problem.

### Use living documentation

It is easier for us to fix something we can see breaking. If someone has to volunteer an hour of there time to reproduce a defect, Start of with one of the Serenity started projects (like [this one](https://github.com/serenity-bdd/serenity-cucumber-starter) and add a scenario or test case that both illustrates and describes your issue. If possible, write the test to describe the behaviour you expect, so that it fails when the defect is present, and that it will pass when the defect is fixed.

### Submit a Pull Request

The fastest way to fix a defect is often to dig into the code and to submit a pull request. 

### Ask for commercial support

If you are using Serenity for your company projects, and need faster or more in-depth support, why not ask your company to get some [commercial support](https://johnfergusonsmart.com/serenity-bdd/)? We provide a range of support options including prioritied tickets, custom Serenity work, and remote mentoring/pair programming sessions, depending on your needs.

Take a look at [this article](https://opensource.guide/how-to-contribute/#communicating-effectively) for more information.


