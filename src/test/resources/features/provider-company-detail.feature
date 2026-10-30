@TS-09 @ProviderCompany
Feature: Provider Company Detail with Order History
  As a platform operator
  I want to retrieve a provider company's profile along with its order history
  So that I can review the provider's activity on the platform

  Scenario: Retrieving detail of an existing provider with orders returns company data and order history
    Given a provider company exists with name "Combustibles Andina SAC"
    And the provider has 1 fuel order registered
    When the operator requests the detail of the provider
    Then the provider company response status code should be 200
    And the response should include the provider name "Combustibles Andina SAC"
    And the response should include 1 order in the provider history

  Scenario: Retrieving detail of an existing provider with no orders returns empty order list
    Given a provider company exists with name "Distribuidora Lima Norte SAC"
    And the provider has no fuel orders registered
    When the operator requests the detail of the provider
    Then the provider company response status code should be 200
    And the response should include the provider name "Distribuidora Lima Norte SAC"
    And the response should include 0 orders in the provider history

  Scenario: Retrieving detail of a non-existing provider returns 404
    Given no provider company exists with id 9999
    When the operator requests the detail of the provider with id 9999
    Then the provider company response status code should be 404