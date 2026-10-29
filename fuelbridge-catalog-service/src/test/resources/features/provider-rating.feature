@Catalog
Feature: Provider Rating
  As a buyer company
  I want to rate a provider after doing business with them
  So that other buyers can see the provider's reputation

  Scenario: Buyer successfully rates a provider for the first time
    Given buyer company 101 and provider company 202 are known to IAM
    And the buyer is authenticated
    When the buyer rates the provider with 5
    Then the response status code should be 201
    And the stored rating should be 5

  Scenario: Buyer cannot rate the same provider twice
    Given buyer company 101 and provider company 202 are known to IAM
    And the buyer is authenticated
    And the buyer has already rated the provider with 4
    When the buyer rates the provider with 5
    Then the response status code should be 409
