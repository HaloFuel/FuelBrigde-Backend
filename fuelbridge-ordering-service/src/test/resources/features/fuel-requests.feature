@TS-15 @Ordering
Feature: Fuel Requests
  As a buyer company and its provider
  I want to create, list, accept and reject fuel requests
  So that an accepted request becomes a fuel order with the current product price

  Scenario: Creating a fuel request with complete data
    Given the fuel product 303 of provider 202 costs 4.8 per unit
    When a buyer creates a fuel request for 1000 units of product 303 of provider 202
    Then the ordering response status should be 201
    And the response contains the new fuel request id with status "PENDING"

  Scenario: Creating a fuel request without a delivery date is rejected
    Given the fuel product 303 of provider 202 costs 4.8 per unit
    When a buyer creates a fuel request for product 303 of provider 202 without a delivery date
    Then the ordering response status should be 400
    And the ordering error code should be "VALIDATION_ERROR"

  Scenario: Creating a fuel request for a product of another provider is rejected
    Given the fuel product 303 of provider 202 costs 4.8 per unit
    When a buyer creates a fuel request for 1000 units of product 303 of provider 999
    Then the ordering response status should be 400

  Scenario: Listing the fuel requests of a buyer company
    Given a pending fuel request from buyer 101 for 1000 units of product 303 of provider 202
    And a pending fuel request from buyer 101 for 500 units of product 303 of provider 202
    And a pending fuel request from buyer 102 for 800 units of product 303 of provider 202
    When the fuel requests of buyer 101 are listed
    Then the ordering response status should be 200
    And the response lists 2 fuel requests

  Scenario: Accepting a pending fuel request creates the order
    Given the fuel product 303 of provider 202 costs 5.0 per unit
    And a pending fuel request from buyer 101 for 1000 units of product 303 of provider 202
    When the provider accepts the fuel request
    Then the ordering response status should be 200
    And the response is a "PENDING" order created from the fuel request with total price 5000
    And the fuel request status should be "APPROVED"

  Scenario: Accepting a fuel request that is no longer pending is rejected
    Given the fuel product 303 of provider 202 costs 5.0 per unit
    And a rejected fuel request from buyer 101 for 1000 units of product 303 of provider 202
    When the provider accepts the fuel request
    Then the ordering response status should be 409
    And the fuel request status should be "REJECTED"
    And no fuel order is stored

  Scenario: Rejecting a pending fuel request keeps the reason
    Given a pending fuel request from buyer 101 for 1000 units of product 303 of provider 202
    When the provider rejects the fuel request with reason "  No hay unidades disponibles para la fecha.  "
    Then the ordering response status should be 200
    And the fuel request status should be "REJECTED"
    And the fuel request keeps the rejection reason "No hay unidades disponibles para la fecha."

  Scenario: Rejecting a fuel request without a reason is rejected
    Given a pending fuel request from buyer 101 for 1000 units of product 303 of provider 202
    When the provider rejects the fuel request with reason " "
    Then the ordering response status should be 400
    And the fuel request status should be "PENDING"
