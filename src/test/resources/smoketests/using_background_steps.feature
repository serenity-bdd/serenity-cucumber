@driver:phantomjs
@driver-options:--headless
Feature: Using Background Steps

  Background:
    Given I want to search for something

  Scenario: Skipping a scenario
    When I view the home page details
    Then I should see "DuckDuckGo" in the page title

  Scenario: Running a scenario
    When I view the home page details
    Then I should see "DuckDuckGo" in the page title
