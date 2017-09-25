@driver:chrome
Feature: Skipping Scenarios

  @skip
  Scenario: 1 Skipping a scenario
    Given I want to search for something
    When I lookup apple
    Then I should see "apple at DuckDuckGo" in the page title

  Scenario: 2 Running a scenario
    Given I want to search for something
    When I lookup pear
    Then I should see "pear at DuckDuckGo" in the page title

  @manual
  Scenario: 3 A manual scenario
    Given I want to search for something
    When I lookup apple
    Then I should see "apple at DuckDuckGo" in the page title

  Scenario: 4 Running another scenario
    Given I want to search for something
    When I lookup orange
    Then I should see "orange at DuckDuckGo" in the page title

  @ignore
  Scenario: 5 An ignored scenario
    Given I want to search for something
    When I lookup apple
    Then I should see "apple at DuckDuckGo" in the page title
