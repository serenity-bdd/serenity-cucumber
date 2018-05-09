Feature: Marking scenarios as pending, skipped or ignored

  @expected-outcome:success
  Scenario: A simple passing scenario
    Given I want to search for something
    When I lookup pear
    Then I should see "pear at DuckDuckGo" in the page title

  @skip
  @expected-outcome:skip
  Scenario: Skipping a scenario using the @skip annotation
  Steps in the scenario will be reported as 'ignored'

    Given I want to search for something
    When I lookup apple
    Then I should see "apple at DuckDuckGo" in the page title

  @ignore
  @expected-outcome:ignore
  Scenario: Ignoring a scenario
  You can also ignore an entire scenario, which is a bit like skipping it
    Given I want to search for something
    When I lookup apple
    Then I should see "apple at DuckDuckGo" in the page title

  @pending
  @expected-outcome:pending
  Scenario: You mark a scenario as pending using the @pending annotation
  Pending scenarios are meant to indicate a scenario that has not been completed yet.
    Given I want to search for something
    When I lookup apple
    Then I should see "apple at DuckDuckGo" in the page title

  @expected-outcome:pending
  Scenario: A scenario with no step definitions will be marked as pending by default
    When I use a step that has no step definition
    Then the step without a step definition should be pending
    And subsequent steps should be ignored

  @skip
  @expected-outcome:skip
  Scenario: You can mark a scenario with no step definitions as @skipped
    When I use a step that has no step definition
    And I tag the scenario with @Skip
    Then the overall result should be skipped
    Then the steps without a step definition should be pending
    And subsequent steps should be ignored
