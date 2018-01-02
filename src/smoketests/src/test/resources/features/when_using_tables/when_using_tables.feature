Feature: Serenity and Cucumber let you do cool stuff with tables

  Tables are used for data-driven testing.
  You can use more than one table in a scenario outline, and tables can be tagged individually.

  @expected-outcome:success
  Scenario: Doing basic maths
    Given I have a calculator
    When I add 1
    And I add 2
    Then the total should be 3

  @tables
  @expected-outcome:success
  Scenario Outline: Doing maths
    Given I have a calculator
    When I add <a>
    And I add <b>
    Then the total should be <c>

  @isolated
    Examples:
      | a | b | c |
      | 1 | 1 | 2 |
      | 1 | 2 | 3 |
      | 2 | 3 | 5 |

  @tables
  @expected-outcome:success
  Scenario Outline: Doing more maths
    Given I have a calculator
    When I add <a>
    And I add <b>
    Then the total should be <c>

  @single @red
    Examples: Single digits
      | a | b | c |
      | 1 | 1 | 2 |
      | 1 | 2 | 3 |
      | 2 | 3 | 5 |

  @double @blue
    Examples: Double digits
      | a  | b | c  |
      | 10 | 1 | 11 |
      | 10 | 2 | 12 |
      | 20 | 3 | 23 |

  @tables
  @expected-outcome:failure
  Scenario Outline: Doing maths wrong
    Given I have a calculator
    When I add <a>
    And I add <b>
    Then the total should be <c>
    @isolated
    Examples:
      | a | b | c |
      | 1 | 1 | 2 |
      | 1 | 2 | 3 |
      | 2 | 3 | 5 |