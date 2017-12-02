Feature: Serenity automatically instantiates step libraries

  Background:
    Given I have a calculator

  @expected-outcome:success
  Scenario Outline: Doing maths
    When I add <a>
    And I add <b>
    Then the total should be <c>
    Examples:
      | a | b | c |
      | 1 | 1 | 2 |
      | 1 | 2 | 3 |
      | 2 | 3 | 5 |

  @expected-outcome:failure
  Scenario Outline: Doing maths
    When I add <a>
    And I add <b>
    Then the total should be <c>
    Examples:
      | a | b | c |
      | 1 | 1 | 2 |
      | 1 | 2 | 3 |
      | 2 | 3 | 5 |