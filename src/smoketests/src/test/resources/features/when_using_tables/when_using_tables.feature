Feature: Serenity automatically instantiates step libraries

  @expected-outcome:success
  Scenario Outline: Doing maths
    Given I have a calculator
    When I add <a>
    And I add <b>
    Then the total should be <c>
    Examples:
      | a | b | c |
      | 1 | 1 | 2 |
      | 1 | 2 | 3 |
      | 2 | 3 | 5 |