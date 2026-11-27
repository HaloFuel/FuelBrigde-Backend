@T10.04b @Ordering @Outbox
Feature: Transactional Outbox for Fuel Order Events
  As the Ordering service
  I want to store every order event in an outbox in the same transaction as the order
  So that no event is lost when the Notification service or Identity & Access are down

  Scenario: A dispatch stores its event in the outbox before anything is sent
    Given a buyer exists for company 101
    And a fuel order exists for company 101 with status "PENDING"
    When the fuel order is dispatched
    Then the outbox holds a pending "FuelOrderDispatched" event for the order
    And no notification should be sent

  Scenario: The relay delivers the pending event to the buyer
    Given a buyer exists for company 101
    And a fuel order exists for company 101 with status "PENDING"
    When the fuel order is dispatched
    And the outbox relay runs
    Then an "ORDER_DISPATCHED" notification should be created for the buyer
    And the notification carries the outbox event identifier
    And the outbox event is published

  Scenario: An unavailable Notification service keeps the event for a later retry
    Given a buyer exists for company 101
    And a fuel order exists for company 101 with status "PENDING"
    And the notification service is unavailable
    When the fuel order is dispatched
    And the outbox relay runs
    Then the fuel order status remains "DISPATCHED"
    And no notification should be sent
    And the outbox event is "PENDING" after 1 failed attempt

  Scenario: The event is delivered once the Notification service is back
    Given a buyer exists for company 101
    And a fuel order exists for company 101 with status "PENDING"
    And the notification service is unavailable
    When the fuel order is dispatched
    And the outbox relay runs
    And the notification service is available again
    And the outbox relay runs
    Then an "ORDER_DISPATCHED" notification should be created for the buyer
    And the outbox event is published

  Scenario: A rejected transition stores no event
    Given a buyer exists for company 101
    And a fuel order exists for company 101 with status "DISPATCHED"
    When the buyer tries to cancel the fuel order
    Then the fuel order status remains "DISPATCHED"
    And the outbox is empty
