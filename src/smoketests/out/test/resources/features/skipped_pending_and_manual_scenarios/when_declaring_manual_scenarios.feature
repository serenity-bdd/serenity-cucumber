Feature: Marking scenarios as 'manual' using metadata

  @expected-outcome:pending
  @manual
  Scenario: A manual scenario
    Given I want to indicate that a scenario should be performed manually
    When I tag it as @manual
    Then it should be reported as manual pending

  @expected-outcome:skip
  @manual
  @skip
  Scenario: A skipped manual scenario
    Given I want to indicate that a scenario should be performed manually
    And I also want it appearing in the skipped scenarios
    When I tag it as @manual and @skipped
    Then it should be reported as manual skipped


  @expected-outcome:pending
  @manual
  Scenario: A manual scenario scenario with undefined steps
    Given I want to indicate that a scenario should be performed manually
    When I tag it as @manual
    And the steps are undefined
    Then it should be reported as manual pending

