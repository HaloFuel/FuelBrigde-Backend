@TS-11 @US-43 @Inventory
Feature: Fuel Product Inventory Management
  As a fuel provider
  I want to manage fuel products and their available stock
  So that inventory information remains accurate

  Scenario: Register a fuel product successfully
    Given a valid new fuel product for inventory testing
    When the provider registers the fuel product
    Then the inventory creation response status should be 201
    And the created inventory product should have 1000 units of stock

  Scenario: Update fuel product stock successfully
    Given a registered inventory product with 1000 units of stock
    When the provider updates the inventory stock to 1500 units
    Then the inventory stock response status should be 200
    And the inventory product should retain 1500 units of stock

  Scenario: Reject a negative stock update
    Given a registered inventory product with 1000 units of stock
    When the provider updates the inventory stock to -100 units
    Then the inventory stock response status should be 400
    And the inventory product should retain 1000 units of stock



  Scenario: Reject a non-numeric stock update
    Given a registered inventory product with 1000 units of stock
    When the provider updates the inventory stock with a non-numeric value
    Then the inventory stock response status should be 400
    And the inventory product should retain 1000 units of stock



  Scenario: Reject updating a non-existing fuel product
    Given inventory product with id 99999999 does not exist
    When the provider updates a missing inventory product with id 99999999
    Then the inventory missing product response status should be 404

  Scenario: Reject deleting a non-existing fuel product
    Given inventory product with id 99999999 does not exist
    When the provider deletes a missing inventory product with id 99999999
    Then the inventory missing product response status should be 404




  Scenario: Retrieve an existing inventory product by ID
    Given a registered inventory product with 1000 units of stock
    When the provider requests the registered inventory product by ID
    Then the inventory lookup response status should be 200
    And the inventory lookup should match the registered product

  Scenario: Filter inventory products by provider
    Given a registered inventory product with 1000 units of stock
    And another inventory product belongs to provider 303
    When the provider requests inventory products for provider 202
    Then the inventory provider lookup response status should be 200
    And the inventory provider lookup should contain only provider 202
    And the inventory provider lookup should include the registered product

  Scenario: Retrieve a non-existing inventory product
    Given inventory product with id 99999999 does not exist
    When the provider requests inventory product with id 99999999
    Then the inventory lookup response status should be 404




  Scenario: List all registered fuel products
    Given a registered inventory product with 1000 units of stock
    When the provider requests the complete inventory product list
    Then the inventory CRUD response status should be 200
    And the inventory list should include its registered product

  Scenario: Update an existing fuel product
    Given a registered inventory product with 1000 units of stock
    When the provider updates the complete inventory product
    Then the inventory CRUD response status should be 200
    And the inventory product details should be updated

  Scenario: Delete an existing fuel product
    Given a registered inventory product with 1000 units of stock
    When the provider deletes the registered inventory product
    Then the inventory CRUD response status should be 204
    And the deleted inventory product should no longer exist
