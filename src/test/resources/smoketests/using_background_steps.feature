@driver:phantomjs
Feature: Using Background Steps

  Background:
    Given I want to search for something

  Scenario: Skipping a scenario
    When I lookup apple
    Then I should see "apple at DuckDuckGo" in the page title

  Scenario: Running a scenario
    When I lookup pear
    Then I should see "pear at DuckDuckGo" in the page title
