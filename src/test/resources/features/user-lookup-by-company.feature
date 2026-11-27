@TS-10 @IAM
Feature: User Lookup by Company
  As another FuelBridge service
  I want to find the user that belongs to a buyer company
  So that I can address notifications to that user
  # The endpoint is reserved for service tokens: a user token is rejected with 403.

  Scenario: A service looking up the user of an existing company receives its data
    Given a registered user named "compras.servicio" linked to company 880101
    When the service "fuelbridge-ordering-service" requests the user of company 880101 with its service token
    Then the lookup response status code should be 200
    And the lookup response should show the registered user linked to company 880101

  Scenario: A service looking up a company without an associated user receives not found
    Given no user is linked to company 880102
    When the service "fuelbridge-notification-service" requests the user of company 880102 with its service token
    Then the lookup response status code should be 404

  Scenario: A buyer user cannot look up the user of their own company
    Given a registered user named "compras.propio" linked to company 880103
    When the registered user requests the user of company 880103 with their own user token
    Then the lookup response status code should be 403

  Scenario: A buyer user gets forbidden for a company without user, so nothing about it is revealed
    Given a registered user named "compras.curioso" linked to company 880104
    And no user is linked to company 880105
    When the registered user requests the user of company 880105 with their own user token
    Then the lookup response status code should be 403

  Scenario: A buyer user cannot look up the user of another company
    Given a registered user named "empresa.a" linked to company 880106
    And a registered user named "empresa.b" linked to company 880107
    When the user linked to company 880106 requests the user of company 880107 with their own user token
    Then the lookup response status code should be 403
    And the lookup response should not reveal the user linked to company 880107

  Scenario: Looking up the user of a company without a token is rejected
    Given a registered user named "compras.anonimo" linked to company 880108
    When an anonymous client requests the user of company 880108
    Then the lookup response status code should be 401
