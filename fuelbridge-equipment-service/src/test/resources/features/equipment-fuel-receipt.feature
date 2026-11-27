@Equipment
Feature: Equipment fuel receipt
  As a buyer company
  I want each completed delivery to update the fuel level of my equipment
  So that I know how much fuel every piece of equipment has available

  Background:
    Given the buyer is authenticated
    And company 101 has registered equipment "Excavadora CAT 320" with a 400 liter tank at 80 liters

  Scenario: Delivered fuel increases the equipment level
    When 240 liters are received for the equipment
    Then the response status code should be 200
    And the equipment fuel level should be 320

  Scenario: Delivered fuel never exceeds the tank capacity
    When 500 liters are received for the equipment
    Then the response status code should be 200
    And the equipment fuel level should be 400

  Scenario: A non-positive quantity is rejected without changing the level
    When 0 liters are received for the equipment
    Then the response status code should be 400
    And the equipment fuel level should be 80

  Scenario: Fuel cannot be received for unknown equipment
    When 100 liters are received for equipment 999999
    Then the response status code should be 404
