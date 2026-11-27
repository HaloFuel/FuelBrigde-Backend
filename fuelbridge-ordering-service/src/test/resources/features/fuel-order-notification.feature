@US-30 @Notification
Feature: Fuel Order Dispatch Notification
  As a fuel buyer
  I want to receive a notification when my order is dispatched
  So that I can track the delivery status

  Scenario: Notify buyer when a fuel order is dispatched
    Given a buyer exists for company 101
    And a fuel order exists for company 101 with status "PENDING"
    When the fuel order is dispatched
    And the outbox relay runs
    Then a "FuelOrderDispatchedEvent" should be published
    And an "ORDER_DISPATCHED" notification should be created for the buyer

  Scenario: The dispatch is kept when the buyer company has no user in Identity & Access
    Given a fuel order exists for company 909 with status "PENDING"
    When the fuel order is dispatched
    And the outbox relay runs
    Then no notification should be sent
    And the outbox has 1 "PUBLISHED" event
