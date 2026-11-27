@TS-08 @BuyerCompany
Feature: Buyer Company Listing
  As a platform operator
  I want to list the registered buyer companies
  So that I can review which companies request fuel on the platform

  Scenario: Listing buyer companies when companies are registered returns them
    Given a registered buyer company named "Transportes Huallaga SAC"
    And a registered buyer company named "Agroindustrial Pacasmayo SAA"
    When the operator requests the list of buyer companies
    Then the buyer company list response status code should be 200
    And the buyer company list should contain a company named "Transportes Huallaga SAC"
    And the buyer company list should contain a company named "Agroindustrial Pacasmayo SAA"

  Scenario: Listing buyer companies when none are registered returns an empty list
    Given there are no buyer companies registered
    When the operator requests the list of buyer companies
    Then the buyer company list response status code should be 200
    And the buyer company list should be empty
