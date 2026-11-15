@Catalog @Security
Feature: Catalog service rejects unauthenticated requests
  As the FuelBridge platform
  I want the catalog service to validate the JWT issued by Identity & Access
  So that only authenticated users can read or change its data

  Scenario: A request without a token is rejected
    When the provider ratings are requested without a token
    Then the request is rejected with status 401

  Scenario: A request with a tampered token is rejected
    When the provider ratings are requested with a tampered token
    Then the request is rejected with status 401

  Scenario: A request with a malformed token is rejected
    When the provider ratings are requested with the token "not-a-jwt"
    Then the request is rejected with status 401
