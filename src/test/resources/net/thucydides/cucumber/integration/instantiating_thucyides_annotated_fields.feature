Feature: Using Cucumber with Thucydides

  Scenario: Running a Cucumber scenario with Thucydides
    Given I have a Cucumber feature file containing Thucydides @Steps fields
    When I run it using Thucydides
    Then the step fields should be instantiated

  Scenario: Running a Cucumber scenario with Thucydides
    Given I have a Cucumber feature file containing Thucydides @Steps fields
    When I run it using Thucydides
    Then the nested pages objects should be instantiated

