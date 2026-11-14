@TS-17 @Fulfillment
Feature: Delivery Completion across Ordering and Equipment
  As a fuel provider
  I want to complete a dispatched delivery
  So that the order moves to payment, the equipment receives the fuel and my fleet is released

  Scenario: Completing a dispatched delivery closes the order and registers the fuel
    Given a dispatched delivery of provider 202 for an order of 300 units for equipment 305
    When the provider completes that delivery with a valid token
    Then the completion response status code should be 200
    And the completed delivery status should be "DELIVERED"
    And the order of that delivery should be "PENDING_PAYMENT" in Ordering
    And equipment 305 should have received 300 units
    And the driver and vehicle of that delivery should be "AVAILABLE"
    And Ordering should have received the provider token

  Scenario: Nothing changes when the Equipment service is unavailable
    Given a dispatched delivery of provider 202 for an order of 300 units for equipment 305
    And the Equipment service is unavailable
    When the provider completes that delivery with a valid token
    Then the completion response status code should be 500
    And the completed delivery status should be "DISPATCHED"
    And the order of that delivery should be "DISPATCHED" in Ordering

  Scenario: A delivery whose order is not dispatched cannot be completed
    Given a dispatched delivery of provider 202 for an order of 300 units for equipment 305
    And the order of that delivery is "PENDING_PAYMENT" in Ordering
    When the provider completes that delivery with a valid token
    Then the completion response status code should be 409
    And the completed delivery status should be "DISPATCHED"
    And equipment 305 should have received 0 units

  Scenario: Completing an unknown delivery returns not found
    When the provider completes delivery 999999 with a valid token
    Then the completion response status code should be 404
