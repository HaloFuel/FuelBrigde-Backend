@Fulfillment @TS-19
Feature: Vehicle management
  As a provider company
  I want to look up, update and remove the vehicles of my fleet
  So that deliveries are only scheduled with vehicles I still operate

  Background:
    Given provider 202 has registered the vehicle with plate "ABC-202"
    And provider 202 has registered the vehicle with plate "DEF-202"
    And provider 303 has registered the vehicle with plate "GHI-303"

  Scenario: List the vehicles of a provider
    When the vehicles of provider 202 are listed
    Then the fleet service responds with status 200
    And the listed vehicles have plates "ABC-202, DEF-202"

  Scenario: Get a vehicle by its id
    When the vehicle with plate "DEF-202" is requested
    Then the fleet service responds with status 200
    And the returned vehicle has plate "DEF-202"

  Scenario: Requesting an unknown vehicle is rejected
    When vehicle 999999 is requested
    Then the fleet service responds with status 404

  Scenario: Update a vehicle's capacity
    When the vehicle with plate "ABC-202" changes its capacity to 12000.0 liters
    Then the fleet service responds with status 200
    And the returned vehicle has a capacity of 12000.0 liters

  Scenario: Delete a vehicle
    When the vehicle with plate "GHI-303" is deleted
    Then the fleet service responds with status 204
    And the vehicle with plate "GHI-303" no longer exists

  Scenario: Deleting an unknown vehicle is rejected
    When vehicle 999999 is deleted
    Then the fleet service responds with status 404
