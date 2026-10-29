@TS-17 @Fulfillment
Feature: Joint Driver and Vehicle Dispatch Assignment
  As a fuel provider
  I want to assign a driver and a vehicle to an order in a single operation
  So that the delivery, order, driver, vehicle and stock stay consistent

  Scenario: Scheduling a delivery assigns the driver and vehicle together
    Given an available driver and an available vehicle for provider 202
    And a pending fuel order for provider 202 requesting 300 units
    And the fuel product has 1000 units of available stock
    When the provider schedules a delivery assigning that driver and vehicle
    Then the delivery response status code should be 201
    And the delivery status should be "DISPATCHED"
    And the assigned driver status should be "ASSIGNED"
    And the assigned vehicle status should be "IN_ROUTE"
    And the available stock should be 700 units

  Scenario: Scheduling a delivery is rejected when the vehicle is not available
    Given an available driver and an unavailable vehicle for provider 202
    And a pending fuel order for provider 202 requesting 300 units
    And the fuel product has 1000 units of available stock
    When the provider schedules a delivery assigning that driver and vehicle
    Then the delivery response status code should be 409

  Scenario: An order cannot be assigned a second delivery
    Given an available driver and an available vehicle for provider 202
    And a pending fuel order for provider 202 requesting 300 units
    And the fuel product has 1000 units of available stock
    And that order already has a scheduled delivery
    When the provider schedules a delivery assigning that driver and vehicle
    Then the delivery response status code should be 409
