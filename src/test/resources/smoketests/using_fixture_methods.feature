Feature: Using Background Steps

  @do_something_before
  Scenario: Running a scenario with a Before clause
    When I lookup apple
    Then I should see "DuckDuckGo" in the page title

  @do_something_after
  Scenario: Running a scenario with an After clause
    Given I want to search for something
    When I lookup pear
    Then I should see "DuckDuckGo" in the page title
