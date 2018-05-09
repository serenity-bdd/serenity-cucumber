Feature: Lifecycle phases can be used to run steps before and after a scenario

  Background:
    Given I have a calculator
    And I add 1

  @current
  @expected-outcome:success
  Scenario: A scenario with before and after phases
    When I add 2
    Then the total should be 3

  @expected-outcome:success
  Scenario: Another scenario with before and after phases
    When I add 3
    Then the total should be 4