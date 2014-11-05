## Serenity Cucumber Integration

This module lets you produce Serenity reports using Cucumber. You run your tests as normal, 
but using the *CucumberWithSerenity* runner, e.g.:

```java
@RunWith(CucumberWithSerenity.class)
@CucumberOptions(features="src/test/resources/samples/myfeature.feature")
public class SimpleTableScenario {}
```



