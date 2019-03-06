Feature: Using Background Steps

  @do_something_before
  Scenario: Running a scenario with a Before clause
    When I prepare for work

  @do_something_after
  Scenario: Running a scenario with an After clause
    When I prepare for work
