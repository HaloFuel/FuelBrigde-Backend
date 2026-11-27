@TS-19 @Fulfillment
Feature: Vehicle Registration with Unique License Plate
  As a fuel provider
  I want the platform to reject vehicles whose license plate is already registered
  So that my fleet has no duplicated tanker trucks

  Scenario: Registering a vehicle with a new license plate succeeds
    Given provider 202 prepares a vehicle with an unused license plate
    When the provider submits the vehicle registration
    Then the vehicle registration response status code should be 201
    And the registered vehicle status should be "AVAILABLE"

  Scenario: Registering a vehicle with a duplicated license plate is rejected
    Given provider 202 prepares a vehicle with an unused license plate
    And a vehicle with that license plate is already registered
    When the provider submits the vehicle registration
    Then the vehicle registration response status code should be 409
    And the vehicle registration error code should be "VEHICLE_CONFLICT"

  Scenario: Updating a vehicle to a license plate used by another vehicle is rejected
    Given provider 202 prepares a vehicle with an unused license plate
    And a vehicle with that license plate is already registered
    And another vehicle of the provider is registered
    When the provider changes that other vehicle to the duplicated license plate
    Then the vehicle registration response status code should be 409
    And the vehicle registration error code should be "VEHICLE_CONFLICT"
