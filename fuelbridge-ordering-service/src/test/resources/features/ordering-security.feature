@Ordering @Security
Feature: Ordering service validates the caller's JWT
  As the FuelBridge platform
  I want the ordering service to validate the JWT locally with the shared secret
  So that only authenticated users and FuelBridge services can read or change orders

  Scenario: A request without a token is rejected
    When the fuel orders are requested without a token
    Then the ordering request is answered with status 401

  Scenario: A request with a tampered token is rejected
    When the fuel orders are requested with a tampered token
    Then the ordering request is answered with status 401

  Scenario: A request with a malformed token is rejected
    When the fuel orders are requested with the token "not-a-jwt"
    Then the ordering request is answered with status 401

  Scenario: A user token issued by Identity & Access is accepted
    When the fuel orders are requested with a user token
    Then the ordering request is answered with status 200

  Scenario: The service token of another FuelBridge service is accepted
    When the fuel orders are requested with the service token of "fuelbridge-payment-service"
    Then the ordering request is answered with status 200
