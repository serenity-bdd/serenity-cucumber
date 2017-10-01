## Serenity Cucumber Smoke tests

This sub-project contains a range of self-documenting tests that illustrate Serenity-JBehave features.

Running the tests as follows should always pass:
```
$ mvn clean verify
```
or
```
$ gradle clean test aggregate
```

Running in the failure mode will intentionally fail with self-documenting errors.
```
$ mvn clean verify -PincludingFailures
```
or
```
gradle clean test aggregate -DincludingFailures
```
