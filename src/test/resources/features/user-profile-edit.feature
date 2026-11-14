@TS-10 @IAM
Feature: User Profile Editing
  As an authenticated user
  I want to edit my own profile
  So that my contact data stays up to date

  Scenario: Editing my own profile succeeds
    Given a registered user named "rosa.medina"
    When the user "rosa.medina" updates the profile of the registered user with full name "Rosa Medina Paredes" and phone "+51987654321"
    Then the profile response status code should be 200
    And the profile response should show full name "Rosa Medina Paredes" and phone "+51987654321"
    And the stored profile of the registered user should have full name "Rosa Medina Paredes" and phone "+51987654321"

  Scenario: Editing the profile of a user that does not exist returns not found
    When the user "rosa.medina" updates the profile of user 999999 with full name "Rosa Medina Paredes" and phone "+51987654321"
    Then the profile response status code should be 404

  Scenario: Editing the profile of another user is forbidden
    Given a registered user named "carlos.vega"
    When the user "intruso" updates the profile of the registered user with full name "Nombre Cambiado" and phone "+51911111111"
    Then the profile response status code should be 403
    And the stored profile of the registered user should have no full name

  Scenario: Editing my profile with a blank full name is rejected
    Given a registered user named "lucia.rios"
    When the user "lucia.rios" updates the profile of the registered user with full name " " and phone "+51922222222"
    Then the profile response status code should be 400
    And the stored profile of the registered user should have no full name
