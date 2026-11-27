@TS-17 @Inventory
Feature: Fuel stock reservation for dispatch
  As the Fulfillment service
  I want to reserve and release fuel stock through the Inventory service
  So that a scheduled delivery never takes more fuel than the provider has available

  Scenario: Reserving stock for a delivery reduces the available stock
    Given a fuel product with 1000 units of available stock
    When 300 units are reserved for a delivery
    Then the stock reservation response status should be 200
    And the fuel product should have 700 units of available stock

  Scenario: A reservation larger than the available stock is rejected
    Given a fuel product with 200 units of available stock
    When 300 units are reserved for a delivery
    Then the stock reservation response status should be 409
    And the fuel product should have 200 units of available stock

  Scenario: Releasing a reservation returns the stock
    Given a fuel product with 700 units of available stock
    When 300 units are released from a cancelled reservation
    Then the stock reservation response status should be 200
    And the fuel product should have 1000 units of available stock
