@TS-18 @Fulfillment
Feature: Driver Registration with Unique License Number
  As a fuel provider
  I want the platform to reject drivers whose license number is already registered
  So that each driver of my fleet is registered only once

  Scenario: Registering a driver with a new license number succeeds
    Given provider 202 prepares a driver with an unused license number
    When the provider submits the driver registration
    Then the driver registration response status code should be 201
    And the registered driver status should be "AVAILABLE"

  Scenario: Registering a driver with a duplicated license number is rejected
    Given provider 202 prepares a driver with an unused license number
    And a driver with that license number is already registered
    When the provider submits the driver registration
    Then the driver registration response status code should be 409
    And the driver registration error code should be "DRIVER_CONFLICT"

  Scenario: Updating a driver to a license number used by another driver is rejected
    Given provider 202 prepares a driver with an unused license number
    And a driver with that license number is already registered
    And another driver of the provider is registered
    When the provider changes that other driver to the duplicated license number
    Then the driver registration response status code should be 409
    And the driver registration error code should be "DRIVER_CONFLICT"
