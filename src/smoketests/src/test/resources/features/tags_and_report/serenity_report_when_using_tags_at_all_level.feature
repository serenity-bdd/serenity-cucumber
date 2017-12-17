@tag_test
Feature: Serenity automatically instantiates step libraries

  
  @doing_maths
  Scenario Outline: Doing more maths
    Given I have a calculator
    When I add <a>
    And I add <b>
    Then the total should be <c>

    @single_red
    Examples: Single digits
      | a | b | c |
      | 1 | 1 | 2 |
      | 1 | 2 | 3 |
      | 2 | 3 | 5 |

    @double_blue
    Examples: Double digits
      | a  | b | c  |
      | 10 | 1 | 11 |
      | 10 | 2 | 12 |
      | 20 | 3 | 23 |