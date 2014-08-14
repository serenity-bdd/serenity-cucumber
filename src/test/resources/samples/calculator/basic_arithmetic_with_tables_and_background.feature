@foo
Feature: Basic Arithmetic
  Calculing additions

  Background: A Calculator
    Given a calculator I just turned on

  Scenario Outline: Many additions
    Given the previous entries:
      | first | second | operation |
      | 1     | 1      | +         |
      | 2     | 1      | +         |
    When I press +
    And I add <a> and <b>
    And I press +
    Then the result is <c>

  Examples: Single digits
    | a | b | c  |
    | 1 | 2 | 8  |
    | 2 | 3 | 10 |

