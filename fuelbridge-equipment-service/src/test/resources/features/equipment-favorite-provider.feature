@Equipment @TS-24
Feature: Favorite provider for equipment
  As a buyer company
  I want to choose a favorite provider for each piece of equipment
  So that refills are requested from a provider I trust

  Background:
    Given company 101 owns the equipment "Generador Cummins"

  Scenario: Assign an existing provider as favorite
    When provider 202 is set as favorite for the equipment "Generador Cummins"
    Then the equipment service responds with status 200
    And the favorite provider of "Generador Cummins" is 202

  Scenario: A provider unknown to IAM cannot be assigned
    When provider 99 is set as favorite for the equipment "Generador Cummins"
    Then the equipment service responds with status 404
    And the error code is "PROVIDER_NOT_FOUND"
    And "Generador Cummins" has no favorite provider

  Scenario: A favorite provider cannot be assigned to unknown equipment
    When provider 202 is set as favorite for equipment 999999
    Then the equipment service responds with status 404
    And the error code is "EQUIPMENT_NOT_FOUND"
