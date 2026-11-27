@TS-25 @Equipment
Feature: Equipment deletion
  As a buyer company
  I want to delete equipment I no longer operate
  So that my fleet only shows equipment that can receive fuel

  Background:
    Given the buyer is authenticated
    And company 101 has registered equipment "Generador Cummins" with a 300 liter tank at 50 liters

  Scenario: Delete registered equipment
    When the buyer deletes the equipment
    Then the response status code should be 204
    And the equipment should no longer exist

  Scenario: Deleting equipment that does not exist is rejected
    When the buyer deletes equipment 999999
    Then the response status code should be 404
