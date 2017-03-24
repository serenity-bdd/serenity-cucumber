Feature: A simple feature that is instable

  @shouldFail
  Scenario: A simple instable scenario
    Given I want to purchase 2 widgets
    And a widget costs $5
    When I buy the widgets
    Then I should perhaps be billed $10
