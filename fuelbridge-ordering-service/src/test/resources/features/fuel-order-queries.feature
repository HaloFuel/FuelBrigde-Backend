@TS-05 @TS-13 @Ordering
Feature: Fuel Order Queries
  As a buyer or provider company
  I want to consult fuel orders in general, by identifier, by buyer company or by provider
  So that each company can follow its own orders

  Scenario: Listing all fuel orders
    Given company 101 has 2 fuel orders with provider 202
    And company 102 has 1 fuel orders with provider 203
    When all fuel orders are requested
    Then the ordering response status should be 200
    And the response lists 3 fuel orders

  Scenario: Consulting an order by its identifier
    Given company 101 has 1 fuel orders with provider 202
    When the first stored order is requested by its identifier
    Then the ordering response status should be 200
    And the response shows that order with status "PENDING"

  Scenario: Consulting an order that does not exist
    When the fuel order 99999999 is requested
    Then the ordering response status should be 404

  Scenario: A buyer company with orders gets only its own orders
    Given company 101 has 2 fuel orders with provider 202
    And company 102 has 1 fuel orders with provider 202
    When the orders of company 101 are requested
    Then the ordering response status should be 200
    And the response lists 2 fuel orders
    And every listed order belongs to company 101

  Scenario: A buyer company without orders gets an empty list
    Given company 101 has 2 fuel orders with provider 202
    When the orders of company 555 are requested
    Then the ordering response status should be 200
    And the response lists 0 fuel orders

  Scenario: A provider gets the orders addressed to it
    Given company 101 has 2 fuel orders with provider 202
    And company 102 has 1 fuel orders with provider 203
    When the orders of provider 203 are requested
    Then the ordering response status should be 200
    And the response lists 1 fuel orders
    And every listed order belongs to provider 203
