@Payment @TS-20
Feature: Payment registration
  As a buyer company
  I want to register the payment of a fuel order
  So that the provider can confirm it and the order can be settled

  Scenario: Register the payment of an order
    When buyer company 101 registers a payment of 4800.0 by "BANK_TRANSFER" for order 5101
    Then the payment service responds with status 201
    And the returned payment is "PENDING" for order 5101 with an amount of 4800.0

  Scenario: An order cannot be paid twice
    Given buyer company 101 has registered a payment of 4800.0 for order 5101
    When buyer company 101 registers a payment of 4800.0 by "CREDIT_CARD" for order 5101
    Then the payment service responds with status 409
    And the error code is "PAYMENT_CONFLICT"
