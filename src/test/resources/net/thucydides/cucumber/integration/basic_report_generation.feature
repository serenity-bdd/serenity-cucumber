Feature: Generating Thucydides Reports

  Scenario: Running a Cucumber scenario with Thucydides
    Given I have a Cucumber feature file
    When I run it using Thucydides
    Then I should obtain a Thucydides report

