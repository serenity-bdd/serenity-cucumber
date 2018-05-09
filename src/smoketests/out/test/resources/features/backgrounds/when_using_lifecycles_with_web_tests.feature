Feature: Hooks can be used with web tests as well

  @web
  @expected-outcome:success
  Scenario: A simple passing scenario
    Given I want to search for something
    When I lookup pear
    Then I should see "pear at DuckDuckGo" in the page title

  Scenario: Another simple passing scenario
    Given I want to search for something
    When I lookup pear
    Then I should see "pear at DuckDuckGo" in the page title