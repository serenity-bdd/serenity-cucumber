Feature: Add two numbers

  @manual
  Scenario Outline: This scenario should be marked as pending

    Given the amount <a> and the amount <b>
    When <a> minus <b>
    Then the result should be <c>


    Examples:
      | a | b | c |
      | 1 | 4 | 5 |
      | 2 | 7 | 9 |