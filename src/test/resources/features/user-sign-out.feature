@TS-03 @IAM
Feature: Sign Out with Token Revocation
  As an authenticated user
  I want to sign out
  So that my JWT can no longer be used even before it expires

  Scenario: Signing out with a valid token succeeds
    Given a registered user with a valid token
    When the user signs out with that token
    Then the sign-out response status code should be 200
    And the revoked token should be stored only as a hash

  Scenario: A revoked token is rejected on protected endpoints
    Given a registered user with a valid token
    And the token is accepted by a protected endpoint
    When the user signs out with that token
    And the user requests a protected endpoint with that token
    Then the protected endpoint response status code should be 401

  Scenario: Signing out again with a revoked token is rejected
    Given a registered user with a valid token
    And the user has signed out with that token
    When the user signs out with that token
    Then the sign-out response status code should be 401

  Scenario: Signing out without a token is rejected
    When a client signs out without a token
    Then the sign-out response status code should be 401

  Scenario: Revoking one token does not affect another user's token
    Given a registered user with a valid token
    And another registered user with a valid token
    When the user signs out with that token
    Then the other user's token is still accepted by a protected endpoint
