@TS-06 @IAM
Feature: User Sign-Up
  As a prospective user
  I want to register an account
  So that I can sign in and use the platform

  # TS-06 is covered in steps: the reserved-prefix rule and the plain 201 are here; the
  # remaining TS-06 scenarios (duplicate registration 409, invalid data 400) are added below.

  Scenario: Signing up with a username that uses the reserved service prefix is rejected
    When a client signs up with username "fuelbridge-intruso" and password "Intruso!Clave2026"
    Then the sign-up response status code should be 400
    And the sign-up error should be on field "username" with message containing "reserved"
    And no user named "fuelbridge-intruso" is stored

  Scenario: Signing up with the reserved prefix in capital letters is rejected
    When a client signs up with username "FUELBRIDGE-Intruso" and password "Intruso!Clave2026"
    Then the sign-up response status code should be 400
    And no user named "FUELBRIDGE-Intruso" is stored

  Scenario: Signing up with an ordinary username succeeds
    When a client signs up with username "registro.normal.ts06" and password "Normal!Clave2026" for company 880201
    Then the sign-up response status code should be 201
    And the sign-up response should show the username "registro.normal.ts06" and company 880201
    And a user named "registro.normal.ts06" is stored
