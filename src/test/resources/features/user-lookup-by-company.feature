@TS-10 @IAM
Feature: User Lookup by Company
  As another FuelBridge service
  I want to find the user that belongs to a buyer company
  So that I can address notifications to that user

  Scenario: Looking up the user of an existing company returns its data
    Given a registered user named "compras.lookup" linked to company 880101
    When the operator requests the user of company 880101
    Then the lookup response status code should be 200
    And the lookup response should show the registered user linked to company 880101

  Scenario: Looking up a company without an associated user returns not found
    Given no user is linked to company 880102
    When the operator requests the user of company 880102
    Then the lookup response status code should be 404

  Scenario: A service authenticated with a service token can look up the user of a company
    Given a registered user named "compras.servicio" linked to company 880103
    When the service "fuelbridge-ordering-service" requests the user of company 880103 with its service token
    Then the lookup response status code should be 200
    And the lookup response should show the registered user linked to company 880103

  Scenario: Looking up the user of a company without a token is rejected
    Given a registered user named "compras.anonimo" linked to company 880104
    When an anonymous client requests the user of company 880104
    Then the lookup response status code should be 401
