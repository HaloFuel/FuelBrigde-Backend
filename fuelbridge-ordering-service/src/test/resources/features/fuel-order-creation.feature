@TS-04 @Ordering
Feature: Fuel Order Creation
  As a buyer company
  I want to create a fuel order through the Ordering API
  So that the provider receives it with the current product price

  Scenario: Creating an order with complete data returns its identifier
    Given the fuel product 303 of provider 202 costs 4.8 per unit
    When a buyer creates a fuel order for product 303 of provider 202 with 1000 units
    Then the ordering response status should be 201
    And the response contains the new order id with status "PENDING" and total price 4800
    And Inventory was called with the ordering service token

  Scenario: Creating an order without required data is rejected
    Given the fuel product 303 of provider 202 costs 4.8 per unit
    When a buyer creates a fuel order for product 303 of provider 202 without the delivery address
    Then the ordering response status should be 400
    And the ordering error code should be "VALIDATION_ERROR"
    And no fuel order is stored

  Scenario: Creating an order for an unknown product returns not found
    When a buyer creates a fuel order for product 999 of provider 202 with 1000 units
    Then the ordering response status should be 404
    And the ordering error code should be "FUELPRODUCT_NOT_FOUND"
    And no fuel order is stored
