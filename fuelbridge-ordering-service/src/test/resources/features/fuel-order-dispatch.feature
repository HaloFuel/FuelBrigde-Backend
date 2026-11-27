
@US-12 @Ordering
Feature: Fuel Order Dispatch
  As a fuel provider
  I want to dispatch a fuel order
  So that the buyer can track the order status

  Scenario: Successfully dispatch a pending fuel order
    Given a fuel order exists with status "PENDING"
    And the provider is authenticated
    When the provider sends a POST request to "/api/v1/fuel-orders/{orderId}/dispatch"
    Then the response status code should be 200
    And the fuel order status should be "DISPATCHED"
