@Equipment @TS-23
Feature: Equipment management
  As a buyer company
  I want to register and maintain the equipment that consumes fuel
  So that FuelBridge knows what has to be refilled and where

  Scenario: Register new equipment
    When company 101 registers the equipment "Excavadora Caterpillar 320"
    Then the equipment service responds with status 201
    And the returned equipment is named "Excavadora Caterpillar 320"

  Scenario: Update registered equipment
    Given company 101 owns the equipment "Generador Cummins"
    When the equipment "Generador Cummins" is renamed to "Generador Cummins C150" with 120 liters
    Then the equipment service responds with status 200
    And the returned equipment is named "Generador Cummins C150"
    And the returned equipment holds 120 liters

  Scenario: List all registered equipment
    Given company 101 owns the equipment "Generador Cummins"
    And company 102 owns the equipment "Volquete Volvo FMX"
    When I list all equipment
    Then the equipment service responds with status 200
    And the listed equipment is "Generador Cummins, Volquete Volvo FMX"

  Scenario: Look up equipment by id
    Given company 101 owns the equipment "Generador Cummins"
    When I look up the equipment "Generador Cummins"
    Then the equipment service responds with status 200
    And the returned equipment is named "Generador Cummins"

  Scenario: List the equipment of a company
    Given company 101 owns the equipment "Generador Cummins"
    And company 101 owns the equipment "Excavadora Caterpillar 320"
    And company 102 owns the equipment "Volquete Volvo FMX"
    When I list the equipment of company 101
    Then the equipment service responds with status 200
    And the listed equipment is "Generador Cummins, Excavadora Caterpillar 320"
