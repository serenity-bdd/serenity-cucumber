Feature: Buying things

  @shouldPass
  Scenario Outline: Buying lots of widgets
    Given I want to purchase <amount> widgets
    And a widget costs $<cost>
    When I buy the widgets
    Then I should be billed $<total>
  Examples:
  | amount | cost | total |
  | 0      | 10   | 0     |
  | 1      | 10   | 10    |
  | 2      | 10   | 50    |
  | 2      | 0    | 0     |

