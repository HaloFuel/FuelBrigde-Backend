@TS-02 @IAM
Feature: Password Recovery and Reset
  As a user who forgot the password
  I want to request a reset token and choose a new password with it
  So that I can sign in again

  Scenario: Requesting a password reset succeeds
    Given a registered user with password "ClaveAntigua!1"
    When the user requests a password reset by username
    Then the password reset response status code should be 200
    And the response should contain a reset token valid for one hour
    And the reset token should be stored only as a hash

  Scenario: Requesting a password reset by email field succeeds
    Given a registered user with password "ClaveAntigua!1"
    When the user requests a password reset by email
    Then the password reset response status code should be 200
    And the response should contain a reset token valid for one hour

  Scenario: Requesting a password reset for an unknown user returns not found
    When someone requests a password reset for "nadie@desconocido.pe"
    Then the password reset response status code should be 404

  Scenario: Resetting the password with a valid token succeeds
    Given a registered user with password "ClaveAntigua!1"
    And the user requested a password reset
    When the user resets the password to "NuevaClave!2026" with that token
    Then the password reset response status code should be 200
    And the user can sign in with "NuevaClave!2026"
    And the user cannot sign in with "ClaveAntigua!1"
    And the reset token should be marked as used

  Scenario: Resetting the password with an expired token is rejected
    Given a registered user with password "ClaveAntigua!1"
    And the user has an expired reset token
    When the user resets the password to "NuevaClave!2026" with that token
    Then the password reset response status code should be 400
    And the user can sign in with "ClaveAntigua!1"

  Scenario: Resetting the password with an already used token is rejected
    Given a registered user with password "ClaveAntigua!1"
    And the user requested a password reset
    And the user already reset the password to "NuevaClave!2026" with that token
    When the user resets the password to "OtraClave!2027" with that token
    Then the password reset response status code should be 400
    And the user can sign in with "NuevaClave!2026"

  Scenario: Resetting the password with an unknown token is rejected
    When someone resets the password to "NuevaClave!2026" with token "00000000-0000-0000-0000-000000000000"
    Then the password reset response status code should be 400

  Scenario: A weak new password is rejected and the token stays usable
    Given a registered user with password "ClaveAntigua!1"
    And the user requested a password reset
    When the user resets the password to "corta" with that token
    Then the password reset response status code should be 400
    And the user can sign in with "ClaveAntigua!1"
    And the reset token should not be marked as used

  Scenario: A new request supersedes the previous reset token
    Given a registered user with password "ClaveAntigua!1"
    And the user requested a password reset
    And the user requested a password reset again
    When the user resets the password to "NuevaClave!2026" with the first token
    Then the password reset response status code should be 400
