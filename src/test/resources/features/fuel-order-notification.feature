@US-30 @Notification
Feature: Fuel Order Dispatch Notification
  As a fuel buyer
  I want to receive a notification when my order is dispatched
  So that I can track the delivery status

  Scenario: Notify buyer when a fuel order is dispatched
    Given a buyer exists for company 101
    And a fuel order exists for company 101 with status "PENDING"
    When the fuel order is dispatched
    Then a "FuelOrderDispatchedEvent" should be published
    And an "ORDER_DISPATCHED" notification should be created for the buyer
