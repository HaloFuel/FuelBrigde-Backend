@Fulfillment @TS-18
Feature: Driver management
  As a provider company
  I want to look up, update and remove the drivers of my fleet
  So that only active drivers can be assigned to deliveries

  Background:
    Given provider 202 has registered the driver with license "LIC-202-A"
    And provider 202 has registered the driver with license "LIC-202-B"
    And provider 303 has registered the driver with license "LIC-303-A"

  Scenario: List the drivers of a provider
    When the drivers of provider 202 are listed
    Then the fleet service responds with status 200
    And the listed drivers have licenses "LIC-202-A, LIC-202-B"

  Scenario: Get a driver by its id
    When the driver with license "LIC-202-B" is requested
    Then the fleet service responds with status 200
    And the returned driver has license "LIC-202-B"

  Scenario: Requesting an unknown driver is rejected
    When driver 999999 is requested
    Then the fleet service responds with status 404

  Scenario: Update a driver's contact data
    When the driver with license "LIC-202-A" changes the phone number to "+51988777666"
    Then the fleet service responds with status 200
    And the returned driver has the phone number "+51988777666"

  Scenario: Delete a driver
    When the driver with license "LIC-303-A" is deleted
    Then the fleet service responds with status 204
    And the driver with license "LIC-303-A" no longer exists

  Scenario: Deleting an unknown driver is rejected
    When driver 999999 is deleted
    Then the fleet service responds with status 404
