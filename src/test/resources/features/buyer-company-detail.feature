@TS-09 @BuyerCompany
Feature: Buyer Company Detail with Order History
  As a platform operator
  I want to retrieve a buyer company's profile along with its order history
  So that I can review the company's activity on the platform

  Scenario: Retrieving detail of an existing company with orders returns company data and order history
    Given a buyer company exists with name "Constructora Costa Sur SAC"
    And the company has 1 fuel order registered
    When the operator requests the detail of the company
    Then the buyer company response status code should be 200
    And the response should include the company name "Constructora Costa Sur SAC"
    And the response should include 1 order in the history

  Scenario: Retrieving detail of an existing company with no orders returns empty order list
    Given a buyer company exists with name "Minera Andina SAC"
    And the company has no fuel orders registered
    When the operator requests the detail of the company
    Then the buyer company response status code should be 200
    And the response should include the company name "Minera Andina SAC"
    And the response should include 0 orders in the history

  Scenario: Retrieving detail of a non-existing company returns 404
    Given no buyer company exists with id 9999
    When the operator requests the detail of the company with id 9999
    Then the buyer company response status code should be 404