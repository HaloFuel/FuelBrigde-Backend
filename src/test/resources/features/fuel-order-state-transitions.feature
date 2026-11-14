@T09.01 @Ordering
Feature: Fuel Order State Transitions Requested by Other Services
  As the Payment and Fulfillment services
  I want to request the paid and received transitions of a fuel order through the Ordering API
  So that they no longer write to the Ordering repository directly

  Scenario: Marking an order as paid succeeds
    Given a stored fuel order with status "PENDING_PAYMENT"
    When a service requests the "mark-paid" transition for the stored order
    Then the transition response status code should be 200
    And the stored fuel order status should be "PAID"

  Scenario: Marking a cancelled order as paid is rejected
    Given a stored fuel order with status "CANCELLED"
    When a service requests the "mark-paid" transition for the stored order
    Then the transition response status code should be 409
    And the stored fuel order status should be "CANCELLED"

  Scenario: Receiving a dispatched order succeeds
    Given a stored fuel order with status "DISPATCHED"
    When a service requests the "receive" transition for the stored order
    Then the transition response status code should be 200
    And the stored fuel order status should be "PENDING_PAYMENT"

  Scenario: Receiving an order that was not dispatched is rejected
    Given a stored fuel order with status "PENDING"
    When a service requests the "receive" transition for the stored order
    Then the transition response status code should be 409
    And the stored fuel order status should be "PENDING"

  Scenario: Requesting a transition for an unknown order returns not found
    When a service requests the "receive" transition for order 999999
    Then the transition response status code should be 404
