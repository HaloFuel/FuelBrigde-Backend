@Payment @TS-20
Feature: Payment completion
  As a provider company
  I want to confirm a payment with its transaction reference
  So that the order is marked as paid in Ordering

  Scenario: Completing a pending payment marks the order as paid
    Given buyer company 101 has registered a payment of 4800.0 for order 5101
    When the payment of order 5101 is completed with reference "TXN-2026-0001"
    Then the payment service responds with status 200
    And the returned payment is "COMPLETED" with reference "TXN-2026-0001"
    And Ordering was asked to mark order 5101 as paid

  Scenario: The payment stays pending when Ordering rejects the order
    Given buyer company 101 has registered a payment of 4800.0 for order 9409
    When the payment of order 9409 is completed with reference "TXN-2026-0002"
    Then the payment service responds with status 409
    And the payment of order 9409 is still "PENDING"

  Scenario: The payment stays pending when Ordering does not know the order
    Given buyer company 101 has registered a payment of 4800.0 for order 9404
    When the payment of order 9404 is completed with reference "TXN-2026-0003"
    Then the payment service responds with status 404
    And the error code is "FUELORDER_NOT_FOUND"
    And the payment of order 9404 is still "PENDING"

  Scenario: Completing an unknown payment is rejected
    When payment 999999 is completed with reference "TXN-2026-0004"
    Then the payment service responds with status 404
    And the error code is "PAYMENT_NOT_FOUND"
    And Ordering was not asked to mark any order as paid
