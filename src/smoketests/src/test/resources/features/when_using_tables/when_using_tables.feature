Feature: Serenity automatically instantiates step libraries

  @tables
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

  @tables
  @expected-outcome:success
  Scenario Outline: Doing more maths
    Given I have a calculator
    When I add <a>
    And I add <b>
    Then the total should be <c>

    @single
    Examples: Single digits
      | a | b | c |
      | 1 | 1 | 2 |
      | 1 | 2 | 3 |
      | 2 | 3 | 5 |

    @double
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
    Examples:
      | a | b | c |
      | 1 | 1 | 2 |
      | 1 | 2 | 3 |
      | 2 | 3 | 5 |