Feature: Serenity automatically instantiates step libraries

  @expected-outcome:success
  Scenario: Serenity automatically instantiates step library fields in step definition classes
    Given I want to use a step library
    When I use a step library field annotated with @Steps
    Then Serenity should instantiate the field

  @expected-outcome:success
  Scenario: Serenity instantiates different step libraries for each field by default
    Given I want to use several step library fields of the same type
    When I use a step library fields to each of them
    Then Serenity should instantiate a different library for each field

  @expected-outcome:success
  Scenario: Serenity creates new step library instances for each new scenario
    Given I have a Serenity step library
    When I start a new scenario
    Then the step library should be reinitialised

  @expected-outcome:success
  Scenario: You can share step library instances using @Steps with shared=true
    Given I have two Serenity step libraries
    When they are annotated with @Steps(shared=true)
    Then both should refer to the same instance

  @expected-outcome:success
  Scenario: You can share step library instances using @Shared
    Given I have two @Shared Serenity step libraries
    When they are annotated with @Shared
    Then they should be reset between scenarios

  @expected-outcome:success
  Scenario: Shared scenarios should be reset between scenarios
    Given I have two @Shared Serenity step libraries
    When they are annotated with @Shared
    Then they should be reset between scenarios

  @expected-outcome:success
  Scenario Outline: You can share step library instances using @Shared in scenario outlines
    Given I have two @Shared Serenity step libraries
    When they are annotated with @Shared
    Then they should be reset between scenario examples
    Examples:
      | example |
      | 1       |
      | 2       |
      | 3       |