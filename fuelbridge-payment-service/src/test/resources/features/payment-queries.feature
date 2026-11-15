@Payment @TS-21
Feature: Payment queries
  As a buyer or provider company
  I want to look up registered payments
  So that I can reconcile what has been paid

  Background:
    Given buyer company 101 has registered a payment of 4800.0 for order 5101
    And buyer company 101 has registered a payment of 1500.0 for order 5102
    And buyer company 102 has registered a payment of 900.0 for order 5201

  Scenario: List every payment
    When all payments are requested
    Then the payment service responds with status 200
    And the listed payments belong to orders "5101, 5102, 5201"

  Scenario: Get a payment by its id
    When the payment of order 5102 is requested by its id
    Then the payment service responds with status 200
    And the returned payment is "PENDING" for order 5102 with an amount of 1500.0

  Scenario: Requesting an unknown payment is rejected
    When payment 999999 is requested
    Then the payment service responds with status 404

  Scenario: Get the payment of an order
    When the payment of order 5201 is requested by order
    Then the payment service responds with status 200
    And the returned payment is "PENDING" for order 5201 with an amount of 900.0

  Scenario: List the payments of a company
    When the payments of company 101 are requested
    Then the payment service responds with status 200
    And the listed payments belong to orders "5101, 5102"
