@TS-16 @Ordering
Feature: Fuel Request Detail
  As a buyer or provider company
  I want to consult a fuel request by its identifier
  So that I can review its product, quantity, price and status before acting on it

  Scenario: Consult an existing fuel request
    Given a pending fuel request from buyer 101 to provider 202 for 1000 liters
    When the user requests that fuel request by its identifier
    Then the fuel request detail response status should be 200
    And the fuel request detail should show status "PENDING" and 1000 liters

  Scenario: Consult a fuel request that does not exist
    When the user requests the fuel request with id 99999999
    Then the fuel request detail response status should be 404
